package com.chromalauncher.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.kdt.pojavlaunch.LauncherActivity

class ChromaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, LauncherActivity::class.java))
        finish()
    }
}
