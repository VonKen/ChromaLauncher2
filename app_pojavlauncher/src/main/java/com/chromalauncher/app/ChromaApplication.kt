package com.chromalauncher.app

import android.os.StrictMode
import android.util.Log
import net.kdt.pojavlaunch.BuildConfig
import net.kdt.pojavlaunch.PojavApplication

class ChromaApplication : PojavApplication() {

    override fun onCreate() {
        try {
            super.onCreate()
        } catch (e: Throwable) {
            Log.e("ChromaApplication", "PojavApplication init failed, continuing with minimal init", e)
        }

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }

        instance = this
    }

    companion object {
        lateinit var instance: ChromaApplication
            private set
    }
}
