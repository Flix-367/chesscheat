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

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 60)
        }
        layout.addView(TextView(this).apply {
            text = "Chess Cheat - Overlay Test"
            textSize = 24f
            setPadding(0, 0, 0, 60)
        })
        
        btnStart = Button(this).apply {
            text = "Bắt đầu quét"
            setOnClickListener { checkPermissions() }
        }
        layout.addView(btnStart)

        btnStop = Button(this).apply {
            text = "Dừng quét"
            setOnClickListener { stopEverything() }
        }
        layout.addView(btnStop)
        setContentView(layout)
    }

    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), REQ_OVERLAY)
        } else {
            startCapture()
        }
    }

    private fun startCapture() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE)
    }

    private fun stopEverything() {
        stopService(Intent(this, ScreenCaptureService::class.java))
        stopService(Intent(this, OverlayService::class.java))
        OverlayBus.boardRect = null
        OverlayBus.view = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) startCapture()
        } else if (requestCode == REQ_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                startService(Intent(this, OverlayService::class.java))
                val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                    putExtra("resultCode", resultCode)
                    putExtra("data", data)
                }
                startForegroundService(serviceIntent)
            }
        }
    }
}
