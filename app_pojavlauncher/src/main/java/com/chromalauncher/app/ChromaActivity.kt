package com.chromalauncher.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import com.chromalauncher.app.ui.ChromaApp
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.MissingStorageActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.tasks.AsyncAssetManager
import net.kdt.pojavlaunch.tasks.AsyncVersionList
import net.kdt.pojavlaunch.tasks.MoJsonDownloader
import net.kdt.pojavlaunch.tasks.MoJsonExtras

class ChromaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initAndShow()
    }

    private fun initAndShow() {
        if (!Tools.checkStorageRoot(this)) {
            startActivity(Intent(this, MissingStorageActivity::class.java))
            finish()
            return
        }

        LauncherPreferences.loadPreferences(this)
        AsyncAssetManager.unpackComponents(this)
        AsyncAssetManager.unpackSingleFiles(this)

        AsyncVersionList().getVersionList { versions ->
            ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, versions)
        }

        setContent {
            ChromaApp(
                onLaunchGame = { launchGame() }
            )
        }
    }

    private fun launchGame() {
        val instance = try {
            Instances.loadSelectedInstance()
        } catch (e: Exception) {
            null
        }

        if (instance == null) {
            Toast.makeText(this, R.string.no_instance, Toast.LENGTH_SHORT).show()
            return
        }

        if (instance.installer != null) {
            instance.installer.start()
            return
        }

        if (!Tools.isValidString(instance.versionId)) {
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_SHORT).show()
            return
        }

        if (Accounts.getCurrent() == null) {
            Toast.makeText(this, R.string.no_saved_accounts, Toast.LENGTH_SHORT).show()
            return
        }

        val normalizedVersionId = MoJsonExtras.normalizeVersionId(instance.versionId)
        val mcVersion = MoJsonExtras.getListedVersion(normalizedVersionId)
        val listener = ContextAwareDoneListener(this, normalizedVersionId)

        MoJsonDownloader().start(assets, mcVersion, normalizedVersionId, listener)
    }
}
