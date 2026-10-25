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
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        view = ArrowView(this)
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
        
        // Giả lập vẽ 1 mũi tên gợi ý từ toạ độ A sang B
        view.arrow = ArrowView.Arrow(300f, 800f, 800f, 800f)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::view.isInitialized) wm.removeView(view)
    }
}
