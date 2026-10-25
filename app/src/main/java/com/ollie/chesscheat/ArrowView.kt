package com.ollie.chesscheat

import android.content.Context
import android.graphics.*
import android.view.View

class ArrowView(ctx: Context) : View(ctx) {
    data class Arrow(val fromX: Float, val fromY: Float,
                     val toX: Float, val toY: Float, val color: Int = 0xFF00FF88.toInt())

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 12f; strokeCap = Paint.Cap.ROUND
    }
    private val head = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    @Volatile var arrow: Arrow? = null

    override fun onDraw(canvas: Canvas) {
        val a = arrow ?: return
        paint.color = a.color; head.color = a.color
        canvas.drawLine(a.fromX, a.fromY, a.toX, a.toY, paint)
        val ang = Math.atan2((a.toY - a.fromY).toDouble(), (a.toX - a.fromX).toDouble())
        val s = 40.0
        val path = Path().apply {
            moveTo(a.toX, a.toY)
            lineTo((a.toX - s * Math.cos(ang - 0.4)).toFloat(), (a.toY - s * Math.sin(ang - 0.4)).toFloat())
            lineTo((a.toX - s * Math.cos(ang + 0.4)).toFloat(), (a.toY - s * Math.sin(ang + 0.4)).toFloat())
            close()
        }
        canvas.drawPath(path, head)
    }
}
