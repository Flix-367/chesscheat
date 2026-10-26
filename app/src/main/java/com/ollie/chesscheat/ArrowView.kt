package com.ollie.chesscheat
import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ArrowView(ctx: Context) : View(ctx) {
    data class Arrow(val fromX: Float, val fromY: Float, val toX: Float, val toY: Float, val color: Int = 0xFF00FF88.toInt())
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = 0xFFFF0000.toInt() }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = 0x88FFFFFF.toInt() }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 12f; strokeCap = Paint.Cap.ROUND; color = 0xFF00FF88.toInt() }
    private val arrowHead = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xFF00FF88.toInt() }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 35f; color = 0xFFFF0000.toInt(); typeface = Typeface.DEFAULT_BOLD }

    override fun onDraw(canvas: Canvas) {
        OverlayBus.boardRect?.let { rect ->
            canvas.drawRect(rect, boxPaint)
            canvas.drawText(OverlayBus.fenText, rect.left.toFloat(), (rect.top + 40).toFloat(), textPaint)
            val cellW = rect.width() / 8f
            val cellH = rect.height() / 8f
            for (i in 1..7) {
                canvas.drawLine(rect.left + i * cellW, rect.top.toFloat(), rect.left + i * cellW, rect.bottom.toFloat(), gridPaint)
                canvas.drawLine(rect.left.toFloat(), rect.top + i * cellH, rect.right.toFloat(), rect.top + i * cellH, gridPaint)
            }
        }
        OverlayBus.arrow?.let { a ->
            canvas.drawLine(a.fromX, a.fromY, a.toX, a.toY, arrowPaint)
            val ang = atan2((a.toY - a.fromY).toDouble(), (a.toX - a.fromX).toDouble())
            val s = 40.0
            val path = Path().apply {
                moveTo(a.toX, a.toY)
                lineTo((a.toX - s * cos(ang - 0.4)).toFloat(), (a.toY - s * sin(ang - 0.4)).toFloat())
                lineTo((a.toX - s * cos(ang + 0.4)).toFloat(), (a.toY - s * sin(ang + 0.4)).toFloat())
                close()
            }
            canvas.drawPath(path, arrowHead)
        }
    }
}
