package com.chromalauncher.app.backend

import android.app.Activity
import android.content.Context
import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.PojavProfile
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader
import net.kdt.pojavlaunch.tasks.AsyncVersionList
import net.kdt.pojavlaunch.tasks.MinecraftDownloader
import net.kdt.pojavlaunch.value.MinecraftAccount
import java.io.File

object LauncherBackend {

    fun getInstalledVersions(): List<String> {
        return try {
            val dir = File(Tools.DIR_HOME_VERSION)
            if (!dir.exists()) return emptyList()
            dir.listFiles()
                ?.filter { it.isDirectory && File(it, "${it.name}.json").exists() }
                ?.map { it.name }
                ?.sorted()
                ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun fetchVersionList(onResult: (JMinecraftVersionList?) -> Unit) {
        AsyncVersionList().getVersionList({ onResult(it) }, false)
    }

    fun downloadVersion(
        activity: Activity,
        version: JMinecraftVersionList.Version,
        versionId: String,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        MinecraftDownloader().start(activity, version, versionId, object : AsyncMinecraftDownloader.DoneListener {
            override fun onDownloadDone() { onDone() }
            override fun onDownloadFailed(throwable: Throwable) { onError(throwable) }
        })
    }

    fun getAccountNames(): List<String> {
        return try {
            val dir = File(Tools.DIR_ACCOUNT_NEW)
            if (!dir.exists()) return emptyList()
            dir.listFiles()
                ?.filter { it.name.endsWith(".json") }
                ?.map { it.nameWithoutExtension }
                ?.sorted()
                ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun getSelectedAccount(ctx: Context): String {
        return try { PojavProfile.getCurrentProfileName(ctx) ?: "" } catch (_: Exception) { "" }
    }

    fun createOfflineAccount(username: String) {
        try {
            val account = MinecraftAccount()
            account.username = username
            account.accessToken = "0"
            account.profileId = "00000000-0000-0000-0000-000000000000"
            account.isMicrosoft = false
            account.save()
        } catch (_: Exception) {}
    }

    fun selectAccount(ctx: Context, username: String) {
        PojavProfile.setCurrentProfile(ctx, username)
    }

    fun deleteAccount(username: String) {
        try { File(Tools.DIR_ACCOUNT_NEW, "$username.json").delete() } catch (_: Exception) {}
    }
}
