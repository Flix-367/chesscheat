package com.ollie.chesscheat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.abs

object PieceDetector {
    private val templates = mutableMapOf<String, Bitmap>()
    private var isLoaded = false

    fun loadTemplates(ctx: Context) {
        if (isLoaded) return
        val names = listOf("P","N","B","R","Q","K","p","n","b","r","q","k")
        for (name in names) {
            try {
                val stream = ctx.assets.open("pieces/$name.png")
                val bmp = BitmapFactory.decodeStream(stream)
                // Chuyển sang ảnh xám để dễ so sánh
                val gray = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(gray)
                val paint = Paint()
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bmp, 0f, 0f, paint)
                templates[name] = Bitmap.createScaledBitmap(gray, 64, 64, true)
            } catch (e: Exception) {}
        }
        isLoaded = true
    }

    fun getFenFromBoard(bmp: Bitmap, boardRect: Rect, turn: String): String {
        if (templates.isEmpty()) return "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR $turn KQkq - 0 1"
        
        val cellW = boardRect.width() / 8f
        val cellH = boardRect.height() / 8f
        val rows = mutableListOf<String>()

        for (r in 0..7) {
            var empty = 0
            var rowStr = ""
            for (c in 0..7) {
                val left = (boardRect.left + c * cellW).toInt()
                val top = (boardRect.top + r * cellH).toInt()
                
                // Cắt ô cờ 64x64
                val cell = Bitmap.createBitmap(bmp, left, top, cellW.toInt(), cellH.toInt())
                val scaledCell = Bitmap.createScaledBitmap(cell, 64, 64, true)
                val grayCell = toGrayscale(scaledCell)
                
                val bestMatch = findBestMatch(grayCell)
                if (bestMatch == null) {
                    empty++
                } else {
                    if (empty > 0) { rowStr += empty; empty = 0 }
                    rowStr += bestMatch
                }
            }
            if (empty > 0) rowStr += empty
            rows.add(rowStr)
        }
        return rows.joinToString("/") + " $turn KQkq - 0 1"
    }

    private fun toGrayscale(bmp: Bitmap): Bitmap {
        val gray = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(gray)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        return gray
    }

    private fun findBestMatch(cell: Bitmap): String? {
        var bestName: String? = null
        var minDiff = Double.MAX_VALUE
        val threshold = 40.0 // Ngưỡng nhận diện (càng nhỏ càng khắt khe)

        val cellPixels = IntArray(64 * 64)
        cell.getPixels(cellPixels, 0, 64, 0, 0, 64, 64)

        for ((name, template) in templates) {
            val tplPixels = IntArray(64 * 64)
            template.getPixels(tplPixels, 0, 64, 0, 0, 64, 64)

            var diff = 0.0
            for (i in 0 until 64 * 64) {
                val c1 = cellPixels[i] and 0xFF
                val c2 = tplPixels[i] and 0xFF
                diff += abs(c1 - c2)
            }
            diff /= (64.0 * 64.0) // Trung bình khác biệt mỗi pixel
            
            if (diff < minDiff && diff < threshold) {
                minDiff = diff
                bestName = name
            }
        }
        return bestName
    }
}
