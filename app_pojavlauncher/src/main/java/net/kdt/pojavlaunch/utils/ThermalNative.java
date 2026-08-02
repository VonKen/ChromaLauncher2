package net.kdt.pojavlaunch.utils;

import android.util.Log;

import net.kdt.pojavlaunch.Tools;

/** Native helpers for in-process CPU throttling. All calls affect the current process. */
public final class ThermalNative {
    private static boolean loaded = false;

    private ThermalNative() {
    }

    /** Lower (positive) nice lowers scheduling priority, i.e. reduces CPU share. */
    public static native void setProcessNice(int nice);

    /** Pin every thread of this process to the efficiency cores. */
    public static native void setLittleCoreAffinity(boolean enable);

    /** Remove any affinity restriction so the scheduler can place work on every online core. */
    public static native void spreadAcrossAllCores();

    public static void ensureLoaded() {
        if(loaded) return;
        try {
            System.loadLibrary("pojavexec");
            loaded = true;
        } catch(Throwable t) {
            loaded = false;
            Log.w(Tools.APP_NAME, "Failed to load pojavexec for thermal throttling", t);
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }
}
