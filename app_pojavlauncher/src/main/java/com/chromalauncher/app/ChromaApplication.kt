package com.chromalauncher.app

import android.app.Application
import android.content.Intent
import android.os.Environment
import android.os.StrictMode
import android.util.Log
import net.kdt.pojavlaunch.BuildConfig
import net.kdt.pojavlaunch.FatalErrorActivity
import net.kdt.pojavlaunch.Tools
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class ChromaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        Thread.setDefaultUncaughtExceptionHandler { thread, th ->
            try {
                val crashFile = File(cacheDir, "crash.txt")
                val sw = StringWriter()
                th.printStackTrace(PrintWriter(sw))
                val crashText = "ChromaLauncher Crash\n${sw.toString()}"
                crashFile.writeText(crashText)
                Log.e("ChromaApplication", "Crash saved to ${crashFile.absolutePath}", th)
            } catch (ignored: Throwable) {}

            try {
                FatalErrorActivity.showError(this@ChromaApplication, null, false, th)
            } catch (ignored: Throwable) {}
        }

        try {
            initDirectories()
        } catch (e: Throwable) {
            Log.e("ChromaApplication", "Directory init failed", e)
        }

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
    }

    private fun initDirectories() {
        val ctx = this
        Tools.DIR_DATA = ctx.filesDir.parent
        Tools.DIR_CACHE = ctx.cacheDir
        val extDir = if (android.os.Build.VERSION.SDK_INT >= 29) {
            ctx.getExternalFilesDir(null) ?: File(Environment.getExternalStorageDirectory(), "games/ChromaLauncher")
        } else {
            File(Environment.getExternalStorageDirectory(), "games/ChromaLauncher")
        }
        Tools.DIR_GAME_HOME = extDir.absolutePath
        Tools.DIR_GAME_NEW = extDir.absolutePath + "/.minecraft"
        Tools.MULTIRT_HOME = Tools.DIR_DATA + "/runtimes"
        Tools.DIR_ACCOUNT_NEW = Tools.DIR_DATA + "/accounts"
        Tools.CTRLMAP_PATH = extDir.absolutePath + "/controlmap"
        Tools.CTRLDEF_FILE = extDir.absolutePath + "/controlmap/default.json"
        Tools.NATIVE_LIB_DIR = applicationInfo.nativeLibraryDir
        Tools.ASSETS_PATH = extDir.absolutePath + "/assets"
        Tools.OBSOLETE_RESOURCES_PATH = extDir.absolutePath + "/resources"
        Tools.APP_NAME = "ChromaLauncher"

        File(Tools.DIR_ACCOUNT_NEW).mkdirs()
    }

    companion object {
        lateinit var instance: ChromaApplication
            private set
    }
}
