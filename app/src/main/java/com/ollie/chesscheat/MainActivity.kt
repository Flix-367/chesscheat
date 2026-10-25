package com.ollie.chesscheat

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val REQ_CAPTURE = 1001
    private val REQ_OVERLAY = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 60)
        }
        
        val title = TextView(this).apply {
            text = "Chess Cheat - Ollie Build"
            textSize = 24f
            setPadding(0, 0, 0, 60)
        }
        layout.addView(title)

        val btnStart = Button(this).apply {
            text = "Bắt đầu quét bàn cờ"
            setOnClickListener { checkPermissions() }
        }
        layout.addView(btnStart)

        setContentView(layout)
    }

    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQ_OVERLAY)
        } else {
            startCapture()
        }
    }

    private fun startCapture() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) startCapture()
        } else if (requestCode == REQ_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // Bật overlay vẽ mũi tên
                startService(Intent(this, OverlayService::class.java))
            }
        }
    }
}
