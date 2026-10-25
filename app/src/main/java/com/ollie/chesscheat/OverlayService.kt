package com.ollie.chesscheat

import android.app.Service
import android.content.Intent
import android.graphics.*
import android.os.IBinder
import android.view.*
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var view: ArrowView

    override fun onBind(i: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        try {
            wm = getSystemService(WINDOW_SERVICE) as WindowManager
            view = ArrowView(this)
            OverlayBus.view = view
            
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT)
            lp.gravity = Gravity.TOP or Gravity.START
            wm.addView(view, lp)
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()
            try {
                val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                File(downloadDir, "chesscheat_overlay_crash.txt").writeText(stackTrace)
            } catch (ex: Exception) {}
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::view.isInitialized) {
            try { wm.removeView(view) } catch (e: Exception) {}
        }
        OverlayBus.view = null
    }
}
