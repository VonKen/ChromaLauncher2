package com.chromalauncher.app.bridge

import android.content.Context
import android.util.Log
import net.kdt.pojavlaunch.PojavProfile
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.value.MinecraftAccount
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

    fun launchGame(context: Context, version: String): Boolean {
        return try {
            Log.i(TAG, "Launching game version: $version")
            @Suppress("UNCHANGED_ARGUMENT_VALUE")
            Tools.launchMinecraft(
                null,
                null,
                null,
                version,
                0
            )
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to launch game", e)
            false
        }
    }

    // --- Account management ---

    fun getAccountNames(): List<String> {
        return try {
            val accountDir = File(Tools.DIR_ACCOUNT_NEW)
            if (!accountDir.exists()) return emptyList()
            accountDir.listFiles()
                ?.filter { it.name.endsWith(".json") }
                ?.map { it.nameWithoutExtension }
                ?.sorted()
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list accounts", e)
            emptyList()
        }
    }

    fun createOfflineAccount(username: String): Boolean {
        return try {
            val account = MinecraftAccount()
            account.username = username
            account.accessToken = "0"
            account.profileId = "00000000-0000-0000-0000-000000000000"
            account.isMicrosoft = false
            account.save()
            Log.i(TAG, "Created offline account: $username")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create account: $username", e)
            false
        }
    }

    fun deleteAccount(username: String): Boolean {
        return try {
            val file = File(Tools.DIR_ACCOUNT_NEW, "$username.json")
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete account: $username", e)
            false
        }
    }

    fun selectAccount(context: Context, username: String) {
        PojavProfile.setCurrentProfile(context, username)
    }

    fun getSelectedAccountName(context: Context): String {
        return PojavProfile.getCurrentProfileName(context) ?: ""
    }

    fun getSelectedAccount(context: Context): MinecraftAccount? {
        return PojavProfile.getCurrentProfileContent(context, null)
    }

    // --- Game launching ---

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
}
