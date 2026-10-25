package com.ollie.chesscheat

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val REQ_CAPTURE = 1001
    private val REQ_OVERLAY = 1002
    private val REQ_NOTIF = 1003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 60)
        }
        layout.addView(TextView(this).apply { text = "Chess Cheat"; textSize = 24f })
        
        layout.addView(Button(this).apply {
            text = "Bắt đầu quét"
            setOnClickListener { 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
                } else {
                    checkPermissions() 
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "Dừng quét"
            setOnClickListener { stopEverything() }
        })
        setContentView(layout)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIF) checkPermissions()
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
                // Ghi log ra Toast để xem nó chạy tới đâu
                android.widget.Toast.makeText(this, "Chuẩn bị bật Overlay...", android.widget.Toast.LENGTH_SHORT).show()
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
