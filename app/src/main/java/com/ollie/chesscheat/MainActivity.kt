package com.ollie.chesscheat

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity() {
    private val REQ_CAPTURE = 1001
    private val REQ_OVERLAY = 1002
    private val REQ_NOTIF = 1003
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BoardReader.init(this)

        // Bộ bắt lỗi: Ghi ra Download/crash_log.txt
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()
            
            try {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val logFile = File(downloadDir, "chesscheat_crash.txt")
                logFile.writeText(stackTrace)
            } catch (e: Exception) {}

            runOnUiThread {
                Toast.makeText(applicationContext, "App văng! Xem file Download/chesscheat_crash.txt", Toast.LENGTH_LONG).show()
            }
            Thread.sleep(1000)
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 60)
        }
        layout.addView(TextView(this).apply { text = "Chess Cheat"; textSize = 24f })
        
        tvError = TextView(this).apply {
            text = "Chưa có lỗi."
            textSize = 14f
            setTextColor(android.graphics.Color.RED)
            setPadding(0, 20, 0, 40)
        }
        layout.addView(tvError)

        layout.addView(Button(this).apply {
            text = "Bắt đầu quét"
            setOnClickListener { 
                tvError.text = "Đang xin quyền..."
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
            tvError.text = "Đang xin quyền Overlay..."
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), REQ_OVERLAY)
        } else {
            startCapture()
        }
    }

    private fun startCapture() {
        tvError.text = "Đang xin quyền chụp màn hình..."
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE)
    }

    private fun stopEverything() {
        stopService(Intent(this, ScreenCaptureService::class.java))
        stopService(Intent(this, OverlayService::class.java))
        tvError.text = "Đã dừng quét."
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) startCapture()
            else tvError.text = "Lỗi: Mày chưa cấp quyền Overlay!"
        } else if (requestCode == REQ_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    tvError.text = "Đang bật Overlay và Dịch vụ..."
                    startService(Intent(this, OverlayService::class.java))
                    val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                        putExtra("resultCode", resultCode)
                        putExtra("data", data)
                    }
                    startForegroundService(serviceIntent)
                    tvError.text = "Quét thành công! Mở cờ vua lên."
                } catch (e: Exception) {
                    tvError.text = "Lỗi Start Service: ${e.message}"
                }
            } else {
                tvError.text = "Mày đã bấm Hủy cấp quyền chụp màn hình."
            }
        }
    }
}
