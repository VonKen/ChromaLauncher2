package com.chromalauncher.app

import android.util.Log
import net.kdt.pojavlaunch.PojavApplication

class ChromaApplication : PojavApplication() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ChromaApplication
            private set
    }
}
