package com.chromalauncher.app

import android.app.Application
import android.os.StrictMode
import net.kdt.pojavlaunch.BuildConfig
import net.kdt.pojavlaunch.PojavApplication
import java.io.File

class ChromaApplication : PojavApplication() {

    override fun onCreate() {
        super.onCreate()

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
