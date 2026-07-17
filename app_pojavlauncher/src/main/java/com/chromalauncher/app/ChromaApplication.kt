package com.chromalauncher.app

import android.app.Application
import android.os.Environment
import android.util.Log
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class ChromaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        Thread.setDefaultUncaughtExceptionHandler { _, th ->
            try {
                val crashFile = File(cacheDir, "crash.txt")
                val sw = StringWriter()
                th.printStackTrace(PrintWriter(sw))
                crashFile.writeText(sw.toString())
                Log.e("ChromaApplication", "Crash: ${crashFile.absolutePath}", th)
            } catch (ignored: Throwable) {}
        }

        ContextExecutor.setApplication(this)

        try {
            initDirectories()
        } catch (e: Throwable) {
            Log.e("ChromaApplication", "Directory init failed", e)
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
        Tools.DIR_HOME_VERSION = Tools.DIR_GAME_NEW + "/versions"
        Tools.DIR_HOME_LIBRARY = Tools.DIR_GAME_NEW + "/libraries"
        Tools.DIR_HOME_CRASH = Tools.DIR_GAME_NEW + "/crash-reports"
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
