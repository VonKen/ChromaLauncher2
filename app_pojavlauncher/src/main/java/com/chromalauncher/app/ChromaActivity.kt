package com.chromalauncher.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.chromalauncher.app.ui.ChromaApp

class ChromaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    ChromaApp(
                        onLaunchGame = { version -> launchGame(version) }
                    )
                }
            }
        )
    }

    private fun launchGame(version: String) {
        val intent = android.content.Intent(this, net.kdt.pojavlaunch.GameActivity::class.java)
        intent.putExtra(net.kdt.pojavlaunch.GameActivity.INTENT_MINECRAFT_VERSION, version)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
