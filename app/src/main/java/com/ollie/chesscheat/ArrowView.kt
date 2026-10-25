package com.ollie.chesscheat

import android.content.Context
import android.graphics.*
import android.view.View

class ArrowView(ctx: Context) : View(ctx) {
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 8f; color = 0xFFFF0000.toInt()
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 60f; color = 0xFFFF0000.toInt()
    }

    override fun onDraw(canvas: Canvas) {
        val rect = OverlayBus.boardRect ?: return
        // Vẽ khung đỏ khoanh vùng bàn cờ
        canvas.drawRect(rect, boxPaint)
        canvas.drawText("BOARD DETECTED", rect.left + 50f, rect.top - 20f, textPaint)
    }
}
