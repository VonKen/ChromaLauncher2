package net.kdt.pojavlaunch.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import git.artdeell.mojo.R;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.services.GameService;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Monitors the device temperature while a game is running and progressively
 * throttles the current process (CPU priority + efficiency-core affinity) so the
 * device stays below the temperature at which Android/vendor thermal management
 * kills the app. As a last resort it quits the game gracefully.
 */
public class ThermalManager {
    private static final String TAG = "ThermalManager";

    private static final int THERMAL_STATUS_NONE = 0;
    private static final int THERMAL_STATUS_LIGHT = 1;
    private static final int THERMAL_STATUS_MODERATE = 2;
    private static final int THERMAL_STATUS_SEVERE = 3;
    private static final int THERMAL_STATUS_CRITICAL = 4;
    private static final int THERMAL_STATUS_EMERGENCY = 5;
    private static final int THERMAL_STATUS_SHUTDOWN = 6;

    public enum ThermalTier {
        NORMAL, WARM, HOT, SEVERE, CRITICAL, SHUTDOWN
    }

    private static final int TEMP_POLL_INTERVAL_MS = 5000;

    // Rough thermal-zone temperature thresholds in degrees C (CPU/soc/battery zones)
    private static final int TEMP_WARM_C = 45;
    private static final int TEMP_HOT_C = 52;
    private static final int TEMP_SEVERE_C = 60;
    private static final int TEMP_CRITICAL_C = 68;
    private static final int TEMP_SHUTDOWN_C = 78;

    // CPU scheduling profile per tier: {nice, littleCores}
    private static final int[] NICE_PER_TIER = {0, 2, 5, 8, 12, 12};
    private static final boolean[] LITTLE_CORES_PER_TIER = {false, false, false, true, true, true};

    private final WeakReference<Activity> mActivityRef;
    private final PowerManager mPowerManager;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private final Object mStateLock = new Object();
    private Thread mPollThread;
    private volatile boolean mRunning;
    private volatile int mThermalStatus = THERMAL_STATUS_NONE;
    private volatile float mLastTempC = Float.NaN;
    private volatile ThermalTier mCurrentTier = ThermalTier.NORMAL;
    private int mBelowTierCount;
    private boolean mWasThrottled;
    private volatile boolean mExitScheduled;

    private final PowerManager.OnThermalStatusChangedListener mThermalListener = status -> {
        synchronized (mStateLock) {
            mThermalStatus = status;
        }
        evaluateAndApply();
    };

    public ThermalManager(@NonNull Activity activity) {
        mActivityRef = new WeakReference<>(activity);
        mPowerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
    }

    public void start() {
        if(mRunning) return;
        if(!LauncherPreferences.PREF_THERMAL_THROTTLING) {
            Log.i(TAG, "Thermal throttling disabled by user, not starting");
            return;
        }
        ThermalNative.ensureLoaded();
        mRunning = true;
        mCurrentTier = ThermalTier.NORMAL;
        if(mPowerManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                mPowerManager.addThermalStatusListener(mThermalListener);
                mThermalStatus = mPowerManager.getCurrentThermalStatus();
            } catch(Throwable t) {
                Log.w(TAG, "Thermal status listener unavailable", t);
            }
        }
        mPollThread = new Thread(this::pollLoop, "thermal-poll");
        mPollThread.setDaemon(true);
        mPollThread.start();
        Log.i(TAG, "Thermal throttling started");
        evaluateAndApply();
    }

    public void stop() {
        if(!mRunning) return;
        mRunning = false;
        if(mPowerManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                mPowerManager.removeThermalStatusListener(mThermalListener);
            } catch(Throwable t) {
                Log.w(TAG, "Failed to remove thermal status listener", t);
            }
        }
        if(mPollThread != null) {
            mPollThread.interrupt();
            try {
                mPollThread.join(1000);
            } catch(InterruptedException ignored) {
            }
            mPollThread = null;
        }
        applyThrottle(ThermalTier.NORMAL, true);
        Log.i(TAG, "Thermal throttling stopped");
    }

    public ThermalTier getCurrentTier() {
        return mCurrentTier;
    }

    public float getTemperatureC() {
        return mLastTempC;
    }

    private void pollLoop() {
        while(mRunning) {
            float temp = readMaxTemperature();
            synchronized (mStateLock) {
                mLastTempC = temp;
            }
            evaluateAndApply();
            try {
                Thread.sleep(TEMP_POLL_INTERVAL_MS);
            } catch(InterruptedException e) {
                return;
            }
        }
    }

    /** Reads the hottest temperature among readable sysfs thermal zones and the battery. */
    private static float readMaxTemperature() {
        float maxTemp = Float.NaN;
        int zoneIndex = 0;
        while(true) {
            String zoneType = readSysfs("/sys/class/thermal/thermal_zone" + zoneIndex + "/type");
            String zoneTemp = readSysfs("/sys/class/thermal/thermal_zone" + zoneIndex + "/temp");
            if(zoneType == null || zoneTemp == null) break;
            Float value = normalizeTemp(zoneTemp);
            if(value != null && (Float.isNaN(maxTemp) || value > maxTemp)) {
                maxTemp = value;
            }
            zoneIndex++;
        }
        String batteryTemp = readSysfs("/sys/class/power_supply/battery/temp");
        if(batteryTemp != null) {
            Float value = normalizeTemp(batteryTemp);
            if(value != null && (Float.isNaN(maxTemp) || value > maxTemp)) {
                maxTemp = value;
            }
        }
        return maxTemp;
    }

    private static String readSysfs(String path) {
        try {
            return Tools.read(path).trim();
        } catch(IOException e) {
            return null;
        }
    }

    /** Normalizes the inconsistent units thermal sysfs files report (milli-degrees, tenths, or plain). */
    private static Float normalizeTemp(String raw) {
        try {
            long value = Long.parseLong(raw.trim());
            if(value <= 0) return null;
            if(value > 10000) return value / 1000f; // millidegrees C (cpu/soc zones)
            if(value >= 400 && value <= 1200) return value / 10f; // deci-degrees C (battery)
            return (float) value; // plain degrees C
        } catch(NumberFormatException e) {
            return null;
        }
    }

    private void evaluateAndApply() {
        int status;
        float temp;
        synchronized (mStateLock) {
            status = mThermalStatus;
            temp = mLastTempC;
        }

        ThermalTier candidate = tierFromThermalStatus(status);
        ThermalTier tempTier = tierFromTemperature(temp);
        if(tempTier.ordinal() > candidate.ordinal()) candidate = tempTier;

        synchronized (mStateLock) {
            if(candidate.ordinal() >= mCurrentTier.ordinal()) {
                mCurrentTier = candidate;
                mBelowTierCount = 0;
            } else {
                // Hysteresis: only step down after the temp stayed below for two polls
                mBelowTierCount++;
                if(mBelowTierCount >= 2) {
                    mCurrentTier = candidate;
                    mBelowTierCount = 0;
                }
            }
        }

        applyThrottle(mCurrentTier, false);

        if(mCurrentTier.ordinal() >= ThermalTier.SHUTDOWN.ordinal()) {
            scheduleEmergencyExit();
        }
    }

    private static ThermalTier tierFromThermalStatus(int status) {
        switch(status) {
            case THERMAL_STATUS_LIGHT: return ThermalTier.WARM;
            case THERMAL_STATUS_MODERATE: return ThermalTier.HOT;
            case THERMAL_STATUS_SEVERE: return ThermalTier.SEVERE;
            case THERMAL_STATUS_CRITICAL: return ThermalTier.CRITICAL;
            case THERMAL_STATUS_EMERGENCY:
            case THERMAL_STATUS_SHUTDOWN: return ThermalTier.SHUTDOWN;
            case THERMAL_STATUS_NONE:
            default: return ThermalTier.NORMAL;
        }
    }

    private static ThermalTier tierFromTemperature(float tempC) {
        if(Float.isNaN(tempC)) return ThermalTier.NORMAL;
        if(tempC >= TEMP_SHUTDOWN_C) return ThermalTier.SHUTDOWN;
        if(tempC >= TEMP_CRITICAL_C) return ThermalTier.CRITICAL;
        if(tempC >= TEMP_SEVERE_C) return ThermalTier.SEVERE;
        if(tempC >= TEMP_HOT_C) return ThermalTier.HOT;
        if(tempC >= TEMP_WARM_C) return ThermalTier.WARM;
        return ThermalTier.NORMAL;
    }

    private void applyThrottle(ThermalTier tier, boolean force) {
        if(!ThermalNative.isLoaded()) return;
        if(!force && !LauncherPreferences.PREF_THERMAL_THROTTLING) return;
        int nice = NICE_PER_TIER[tier.ordinal()];
        boolean littleCores = LITTLE_CORES_PER_TIER[tier.ordinal()];
        if(!force && !mWasThrottled && nice == 0 && !littleCores) return;
        mWasThrottled = nice != 0 || littleCores;
        try {
            ThermalNative.setProcessNice(nice);
            ThermalNative.setLittleCoreAffinity(littleCores);
        } catch(Throwable t) {
            Log.w(TAG, "Failed to apply throttle", t);
        }
        Log.i(TAG, "Tier " + tier + " applied (nice=" + nice + ", littleCores=" + littleCores
                + ", temp=" + mLastTempC + "C, status=" + mThermalStatus + ")");
        notifyTier(tier);
    }

    private void notifyTier(ThermalTier tier) {
        Activity activity = mActivityRef.get();
        if(activity == null) return;
        final String message;
        switch(tier) {
            case WARM:
                message = activity.getString(R.string.thermal_warm_toast);
                break;
            case HOT:
                message = activity.getString(R.string.thermal_hot_toast);
                break;
            case SEVERE:
                message = activity.getString(R.string.thermal_severe_toast);
                break;
            case CRITICAL:
                message = activity.getString(R.string.thermal_critical_toast);
                break;
            case SHUTDOWN:
                message = activity.getString(R.string.thermal_shutdown_toast);
                break;
            default:
                return;
        }
        mMainHandler.post(() -> {
            Toast toast = Toast.makeText(activity.getApplicationContext(), message,
                    Toast.LENGTH_LONG);
            toast.show();
        });
    }

    private synchronized void scheduleEmergencyExit() {
        if(mExitScheduled) return;
        mExitScheduled = true;
        mMainHandler.postDelayed(() -> {
            Activity activity = mActivityRef.get();
            if(activity == null) return;
            Log.w(TAG, "Device critical overheating, quitting game to protect the device");
            try {
                Intent killIntent = new Intent(activity, GameService.class);
                killIntent.putExtra("kill", true);
                activity.startService(killIntent);
            } catch(Throwable t) {
                Log.e(TAG, "Failed to stop game gracefully", t);
                Tools.fullyExit();
            }
        }, 3000);
    }
}
