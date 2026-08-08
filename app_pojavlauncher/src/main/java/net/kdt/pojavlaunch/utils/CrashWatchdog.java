package net.kdt.pojavlaunch.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import net.kdt.pojavlaunch.Logger;
import net.kdt.pojavlaunch.Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects when the game died without a clean exit (crash, native crash, or the system
 * killing the process) and reports what closed it the next time the app is opened.
 *
 * A tiny state file is written under the game home when a game session starts, and a low
 * priority background thread keeps refreshing memory / temperature snapshots every few
 * seconds while the game is running. Clean exits (Tools.fullyExit) flag the state as
 * "normal". If the state is still marked as running when the next session starts, the
 * system's process-exit history is consulted to figure out the reason.
 */
public class CrashWatchdog {
    private static final String TAG = "CrashWatchdog";
    private static final String STATE_DIR = ".crashwatchdog";
    private static final String STATE_FILE = "state";
    private static final long SNAPSHOT_INTERVAL_MS = 3000;
    private static final long LOG_INTERVAL_MS = 20000;

    private static volatile boolean sRunning = false;
    private static volatile long sLastLogMs = 0;

    /** Marks the start of a game session and arms the memory monitor. */
    public static void markGameStart() {
        try {
            File state = stateFile();
            File parent = state.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                Log.w(TAG, "Failed to create watchdog state directory");
                return;
            }
            Map<String, String> props = new LinkedHashMap<>();
            props.put("pid", String.valueOf(android.os.Process.myPid()));
            props.put("start_ts", String.valueOf(System.currentTimeMillis()));
            props.put("normal_exit", "0");
            props.putAll(readMemSnapshot());
            writeState(state, props);

            sRunning = true;
            sLastLogMs = 0;
            Thread monitor = new Thread(CrashWatchdog::monitorLoop, TAG);
            monitor.setDaemon(true);
            monitor.start();
        } catch (Throwable t) {
            Log.w(TAG, "Failed to arm the crash watchdog", t);
        }
    }

    /** Marks the current game session as ended cleanly. Safe to call repeatedly. */
    public static void markGameExited() {
        sRunning = false;
        try {
            File state = stateFile();
            if (!state.exists()) return;
            Map<String, String> props = readState(state);
            props.put("normal_exit", "1");
            writeState(state, props);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to mark a clean game exit", t);
        }
    }

    /**
     * Called on every fresh launcher start. If the previous session never flagged a clean
     * exit, it is treated as an unexpected death, a report is written to the log and shown
     * to the user. The state file is consumed in all cases.
     */
    public static void checkLastSession(Activity activity) {
        try {
            File state = stateFile();
            if (!state.exists()) return;
            Map<String, String> props = readState(state);
            if (!state.delete()) state.deleteOnExit();
            if ("1".equals(props.get("normal_exit"))) return;

            String report = buildReport(activity, props);
            try {
                Logger.appendToLog(report);
            } catch (Throwable ignored) {
            }
            appendToLatestLog(report);
            showReportDialog(activity, report);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to check the last game session", t);
        }
    }

    private static void monitorLoop() {
        while (sRunning) {
            try {
                Thread.sleep(SNAPSHOT_INTERVAL_MS);
            } catch (InterruptedException e) {
                break;
            }
            try {
                Map<String, String> props = readState(stateFile());
                props.putAll(readMemSnapshot());
                writeState(stateFile(), props);
                long now = System.currentTimeMillis();
                if (now - sLastLogMs >= LOG_INTERVAL_MS) {
                    sLastLogMs = now;
                    Logger.appendToLog("MemWatch: MemAvailable=" + props.get("mem_available_mb")
                            + "MB MemFree=" + props.get("mem_free_mb")
                            + "MB SwapFree=" + props.get("swap_free_mb")
                            + "MB oom_score_adj=" + props.get("oom_score_adj")
                            + " temp=" + props.get("temp_c") + "C");
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static Map<String, String> readMemSnapshot() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("last_ts", String.valueOf(System.currentTimeMillis()));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream("/proc/meminfo"), StandardCharsets.US_ASCII))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length < 2) continue;
                String key = parts[0].replace(":", "");
                switch (key) {
                    case "MemAvailable":
                    case "MemFree":
                    case "SwapTotal":
                    case "SwapFree":
                        props.put(key.toLowerCase() + "_mb", kbToMb(parts[1]));
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException ignored) {
        }
        props.put("oom_score_adj", readFirstToken("/proc/self/oom_score_adj"));
        String temp = readMaxThermalTempC();
        if (temp != null) props.put("temp_c", temp);
        return props;
    }

    private static String kbToMb(String kb) {
        try {
            return String.valueOf(Long.parseLong(kb) / 1024);
        } catch (NumberFormatException e) {
            return "n/a";
        }
    }

    private static String readFirstToken(String path) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.US_ASCII))) {
            String line = reader.readLine();
            if (line == null) return "n/a";
            String[] parts = line.trim().split("\\s+");
            return parts.length > 0 ? parts[0] : "n/a";
        } catch (IOException e) {
            return "n/a";
        }
    }

    /** Returns the hottest readable thermal zone in Celsius, or null. */
    private static String readMaxThermalTempC() {
        File[] zones = new File("/sys/class/thermal").listFiles(
                file -> file.getName().startsWith("thermal_zone"));
        if (zones == null) return null;
        long max = Long.MIN_VALUE;
        for (File zone : zones) {
            String value = readFirstToken(new File(zone, "temp").getAbsolutePath());
            if ("n/a".equals(value)) continue;
            try {
                long millidegrees = Long.parseLong(value);
                if (millidegrees > max) max = millidegrees;
            } catch (NumberFormatException ignored) {
            }
        }
        return max == Long.MIN_VALUE ? null : String.valueOf(max / 1000);
    }

    private static String buildReport(Context context, Map<String, String> props) {
        StringBuilder sb = new StringBuilder();
        sb.append("================ CRASH WATCHDOG REPORT ================\n");
        sb.append("The game closed unexpectedly during the last session.\n");
        sb.append("What closed it: ").append(exitReason(context, props)).append('\n');
        sb.append("State recorded just before it closed:\n");
        sb.append("- Free memory: ").append(props.get("mem_available_mb"))
                .append(" MB available, ").append(props.get("mem_free_mb"))
                .append(" MB free, ").append(props.get("swap_free_mb"))
                .append(" MB swap free\n");
        sb.append("- System kill priority (oom_score_adj): ").append(props.get("oom_score_adj"));
        if (props.get("temp_c") != null) {
            sb.append(" - device temperature: ").append(props.get("temp_c")).append(" C");
        }
        sb.append('\n');
        long lastTs = parseLong(props.get("last_ts"), 0);
        long agoSeconds = lastTs > 0 ? (System.currentTimeMillis() - lastTs) / 1000 : -1;
        if (agoSeconds >= 0) {
            sb.append("- Last memory sample taken ").append(agoSeconds)
                    .append(" s before it closed\n");
        }
        sb.append("If it was low memory, try reducing the RAM allocation, render distance,\n");
        sb.append("or disabling shaders. Attach the full latestlog.txt when reporting this.\n");
        sb.append("==============================================================");
        return sb.toString();
    }

    private static String exitReason(Context context, Map<String, String> props) {
        long startTs = parseLong(props.get("start_ts"), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    List<ApplicationExitInfo> infos = am.getHistoricalProcessExitReasons(
                            context.getPackageName(), 0, 10);
                    ApplicationExitInfo best = null;
                    for (ApplicationExitInfo info : infos) {
                        long ts = info.getTimestamp();
                        if (ts < startTs) continue;
                        if (best == null || ts < best.getTimestamp()) best = info;
                    }
                    if (best != null) {
                        String reason;
                        switch (best.getReason()) {
                            case ApplicationExitInfo.REASON_LOW_MEMORY:
                                reason = "the Android system killed the app because the device ran out of memory";
                                break;
                            case ApplicationExitInfo.REASON_CRASH_NATIVE:
                                reason = "a native crash (signal " + best.getStatus() + signalName(best.getStatus()) + ")";
                                break;
                            case ApplicationExitInfo.REASON_CRASH:
                                reason = "a Java/Kotlin crash (exit code " + best.getStatus() + ")";
                                break;
                            case ApplicationExitInfo.REASON_ANR:
                                reason = "the app stopped responding and was killed by the system (ANR)";
                                break;
                            case ApplicationExitInfo.REASON_SIGNALED:
                                reason = "the process was terminated by signal " + best.getStatus() + signalName(best.getStatus());
                                break;
                            case ApplicationExitInfo.REASON_EXIT_SELF:
                                reason = "the app exited on its own";
                                break;
                            case ApplicationExitInfo.REASON_FREEZER:
                                reason = "the app was frozen by the system";
                                break;
                            case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE:
                                reason = "the app was killed for excessive resource usage";
                                break;
                            case ApplicationExitInfo.REASON_USER_REQUESTED:
                                reason = "the app was closed on the user's request";
                                break;
                            default:
                                reason = "a system kill (reason code " + best.getReason() + ")";
                        }
                        String description = best.getDescription();
                        if (description != null && !description.isEmpty()) {
                            reason += " - " + description;
                        }
                        return reason + ".";
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "Failed to query process exit reason", t);
            }
            return "unknown (no exit record found for this session yet).";
        }
        return "unknown (device system below Android 11).";
    }

    private static String signalName(int signal) {
        switch (signal) {
            case 6: return " (SIGABRT)";
            case 9: return " (SIGKILL)";
            case 11: return " (SIGSEGV)";
            case 15: return " (SIGTERM)";
            default: return "";
        }
    }

    private static void appendToLatestLog(String text) {
        try {
            File latest = new File(Tools.DIR_GAME_HOME, "latestlog.txt");
            if (!latest.isFile()) return;
            try (FileOutputStream fos = new FileOutputStream(latest, true)) {
                fos.write(("\n" + text + "\n").getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
        }
    }

    private static void showReportDialog(Activity activity, String report) {
        if (activity == null || activity.isFinishing()) return;
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                .setTitle("Game closed unexpectedly")
                .setMessage(report)
                .setPositiveButton(android.R.string.ok, null)
                .show());
    }

    private static File stateFile() {
        return new File(new File(Tools.DIR_GAME_HOME, STATE_DIR), STATE_FILE);
    }

    private static Map<String, String> readState(File file) {
        Map<String, String> props = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq > 0) props.put(line.substring(0, eq), line.substring(eq + 1));
            }
        } catch (IOException ignored) {
        }
        return props;
    }

    private static void writeState(File file, Map<String, String> props) {
        File parent = file.getParentFile();
        if (parent == null) return;
        File tmp = new File(parent, file.getName() + ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            for (Map.Entry<String, String> entry : props.entrySet()) {
                if (entry.getValue() == null) continue;
                fos.write((entry.getKey() + "=" + entry.getValue() + "\n")
                        .getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            return;
        }
        if (!tmp.renameTo(file)) tmp.delete();
    }

    private static long parseLong(String value, long fallback) {
        if (value == null) return fallback;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
