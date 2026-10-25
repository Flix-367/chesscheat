package com.ollie.chesscheat

import android.content.Context
import android.graphics.*
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
                // Resize thẳng về 32x32 cho nhẹ máy
                templates[name] = Bitmap.createScaledBitmap(bmp, 32, 32, true)
            } catch (e: Exception) {}
        }
        isLoaded = templates.isNotEmpty()
    }

    fun getFenFromBoard(fullBmp: Bitmap, boardRect: Rect, turn: String): String? {
        if (!isLoaded) return null
        
        // Cắt vùng bàn cờ ra trước cho nhẹ máy
        val boardBmp = Bitmap.createBitmap(fullBmp, boardRect.left, boardRect.top, boardRect.width(), boardRect.height())
        val cellW = boardBmp.width / 8
        val cellH = boardBmp.height / 8
        val rows = mutableListOf<String>()

        for (r in 0..7) {
            var empty = 0
            var rowStr = ""
            for (c in 0..7) {
                val left = c * cellW
                val top = r * cellH
                
                // Cắt ô cờ và resize về 32x32
                val cell = Bitmap.createBitmap(boardBmp, left, top, cellW, cellH)
                val scaledCell = Bitmap.createScaledBitmap(cell, 32, 32, true)
                
                val bestMatch = findBestMatch(scaledCell)
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

    private fun findBestMatch(cell: Bitmap): String? {
        var bestName: String? = null
        var minDiff = Double.MAX_VALUE
        val threshold = 50.0 // Nới lỏng ngưỡng ra một chút

        val cellPixels = IntArray(32 * 32)
        cell.getPixels(cellPixels, 0, 32, 0, 0, 32, 32)

        for ((name, template) in templates) {
            val tplPixels = IntArray(32 * 32)
            template.getPixels(tplPixels, 0, 32, 0, 0, 32, 32)

            var diff = 0.0
            for (i in 0 until 32 * 32) {
                // So sánh độ sáng (Grayscale) thay vì màu để tránh lệch theme
                val c1 = (cellPixels[i] shr 16 and 0xFF) * 0.299 + (cellPixels[i] shr 8 and 0xFF) * 0.587 + (cellPixels[i] and 0xFF) * 0.114
                val c2 = (tplPixels[i] shr 16 and 0xFF) * 0.299 + (tplPixels[i] shr 8 and 0xFF) * 0.587 + (tplPixels[i] and 0xFF) * 0.114
                diff += abs(c1 - c2)
            }
            diff /= (32.0 * 32.0)
            
            if (diff < minDiff && diff < threshold) {
                minDiff = diff
                bestName = name
            }
        }
        return bestName
    }
}
