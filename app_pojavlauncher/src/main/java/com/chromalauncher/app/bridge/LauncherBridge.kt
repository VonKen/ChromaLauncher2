package com.chromalauncher.app.bridge

import android.util.Log
import net.kdt.pojavlaunch.Tools
import java.io.File

object LauncherBridge {
    private const val TAG = "LauncherBridge"

    fun getGameDir(): String = Tools.DIR_GAME_HOME

    fun getInstalledVersions(): List<String> {
        return try {
            val versionsDir = File(getGameDir(), "versions")
            if (versionsDir.exists() && versionsDir.isDirectory) {
                versionsDir.listFiles()
                    ?.filter { it.isDirectory }
                    ?.map { it.name }
                    ?.sorted()
                    ?: emptyList()
            } else {
                listOf("1.21.1", "1.20.4", "1.20.2")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get versions", e)
            listOf("1.21.1", "1.20.4", "1.20.2")
        }
    }

    fun launchGame(version: String): Boolean {
        return try {
            Log.i(TAG, "Launching game version: $version")
            Tools.launchMinecraft(
                null,
                null,
                null,
                version,
                "org.lwjgl.opengl.experimental=true",
                1024
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch game", e)
            false
        }
    }

    fun getVersionDir(): String = "${Tools.DIR_GAME_HOME}/versions"
    fun getLibrariesDir(): String = "${Tools.DIR_GAME_HOME}/libraries"
    fun getModsDir(): String = "${Tools.DIR_GAME_HOME}/mods"

    fun getInstalledMods(): List<String> {
        return try {
            val modsDir = File(getModsDir())
            if (modsDir.exists() && modsDir.isDirectory) {
                modsDir.listFiles()
                    ?.filter { it.isFile && it.extension == "jar" }
                    ?.map { it.nameWithoutExtension }
                    ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get mods", e)
            emptyList()
        }
    }

    fun deleteVersion(version: String): Boolean {
        return try {
            val versionDir = File(getVersionDir(), version)
            versionDir.deleteRecursively()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete version", e)
            false
        }
    }

    fun getJavaArgs(): String {
        return try {
            Tools.read("java_args.txt")
        } catch (e: Exception) {
            "-Xms512m -Xmx1024m"
        }
    }

    fun setJavaArgs(args: String) {
        try {
            Tools.write("java_args.txt", args)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save java args", e)
        }
    }
}
