package com.chromalauncher.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.chromalauncher.app.bridge.LauncherBridge
import net.kdt.pojavlaunch.GameActivity

@Composable
fun GameScreen() {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Launch game activity
        val intent = Intent(context, GameActivity::class.java).apply {
            putExtra("isOpenGL", true)
        }
        context.startActivity(intent)
    }
}
