package com.chromalauncher.app.backend

import net.kdt.pojavlaunch.JVersionList
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.tasks.AsyncVersionList
import net.kdt.pojavlaunch.authenticator.accounts.Account
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.Instances

object LauncherBackend {

    data class AccountInfo(
        val username: String,
        val isMicrosoft: Boolean,
        val isLocal: Boolean,
        val profileId: String,
    )

    data class VersionInfo(
        val id: String,
        val type: String,
        val releaseTime: String,
    )

    data class InstanceInfo(
        val name: String?,
        val versionId: String?,
    )

    fun loadAccounts(): List<AccountInfo> {
        return try {
            val accounts = Accounts.load()
            accounts.accounts.map { acc ->
                AccountInfo(
                    username = acc.username ?: "Unknown",
                    isMicrosoft = acc.isMicrosoft,
                    isLocal = acc.isLocal,
                    profileId = acc.profileId ?: "",
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getCurrentAccount(): AccountInfo? {
        return try {
            val acc = Accounts.getCurrent() ?: return null
            AccountInfo(
                username = acc.username ?: "Unknown",
                isMicrosoft = acc.isMicrosoft,
                isLocal = acc.isLocal,
                profileId = acc.profileId ?: "",
            )
        } catch (e: Exception) {
            null
        }
    }

    fun deleteAccount(index: Int) {
        try {
            val accounts = Accounts.load()
            if (index in accounts.accounts.indices) {
                Accounts.delete(accounts.accounts[index])
            }
        } catch (_: Exception) {}
    }

    fun fetchVersionList(callback: (List<VersionInfo>) -> Unit) {
        AsyncVersionList().getVersionList { versionList: JVersionList? ->
            if (versionList?.versions == null) {
                callback(emptyList())
                return@getVersionList
            }
            val versions = versionList.versions
                .filter { it.type == "release" || it.type == "snapshot" }
                .map { v ->
                    VersionInfo(
                        id = v.id,
                        type = v.type ?: "unknown",
                        releaseTime = v.releaseTime ?: "",
                    )
                }
            callback(versions)
        }
    }

    fun getInstances(): List<InstanceInfo> {
        return try {
            val displayInstances = Instances.loadDisplay()
            displayInstances.list.map { inst ->
                InstanceInfo(
                    name = inst.name,
                    versionId = inst.versionId,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getSelectedInstance(): InstanceInfo? {
        return try {
            val inst = Instances.loadSelectedInstance() ?: return null
            InstanceInfo(
                name = inst.name,
                versionId = inst.versionId,
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getLatestReleaseVersionId(): String {
        return Instance.VERSION_LATEST_RELEASE
    }
}
