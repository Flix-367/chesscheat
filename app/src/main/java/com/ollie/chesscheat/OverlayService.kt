package com.ollie.chesscheat
import android.app.Service
import android.content.Intent
import android.graphics.*
import android.os.IBinder
import android.view.*

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
            val lp = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT)
            lp.gravity = Gravity.TOP or Gravity.START
            wm.addView(view, lp)
        } catch (e: Exception) { stopSelf() }
    }
    override fun onDestroy() {
        super.onDestroy()
        if (::view.isInitialized) { try { wm.removeView(view) } catch (e: Exception) {} }
        OverlayBus.view = null
    }
}
