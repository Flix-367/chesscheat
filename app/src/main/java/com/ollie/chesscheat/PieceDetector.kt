package com.ollie.chesscheat

import android.content.Context
import android.graphics.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
                templates[name] = Bitmap.createScaledBitmap(bmp, 32, 32, true)
            } catch (e: Exception) {}
        }
        isLoaded = templates.isNotEmpty()
    }

    fun getFenFromBoard(fullBmp: Bitmap, rect: Rect, turn: String, isFlipped: Boolean): String {
        if (!isLoaded) return "LỖI: Thiếu 12 ảnh quân cờ!"
        
        val safeLeft = max(0, rect.left)
        val safeTop = max(0, rect.top)
        val safeRight = min(fullBmp.width, rect.right)
        val safeBottom = min(fullBmp.height, rect.bottom)
        
        if (safeRight <= safeLeft || safeBottom <= safeTop) return "LỖI: Khung toạ độ sai!"

        val boardBmp = Bitmap.createBitmap(fullBmp, safeLeft, safeTop, safeRight - safeLeft, safeBottom - safeTop)
        val scaledBoard = Bitmap.createScaledBitmap(boardBmp, 256, 256, true)
        
        val allPixels = IntArray(256 * 256)
        scaledBoard.getPixels(allPixels, 0, 256, 0, 0, 256, 256)

        val rows = mutableListOf<String>()
        for (r in 0..7) {
            var empty = 0
            var rowStr = ""
            for (c in 0..7) {
                val actualR = if (isFlipped) 7 - r else r
                val actualC = if (isFlipped) 7 - c else c

                val cellPixels = IntArray(32 * 32)
                var idx = 0
                for (cy in 0 until 32) {
                    for (cx in 0 until 32) {
                        cellPixels[idx++] = allPixels[((actualR * 32 + cy) * 256) + (actualC * 32 + cx)]
                    }
                }
                
                val bestMatch = findBestMatch(cellPixels)
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

    private fun findBestMatch(cellPixels: IntArray): String? {
        var bestName: String? = null
        var minDiff = Double.MAX_VALUE
        val threshold = 50.0 // Hạ ngưỡng xuống để bớt đọc nhầm nền thành quân cờ

        // 1. Tính độ sáng trung bình của ô cờ hiện tại
        var totalBrightness = 0
        for (px in cellPixels) {
            val b = (px shr 16 and 0xFF) * 0.299 + (px shr 8 and 0xFF) * 0.587 + (px and 0xFF) * 0.114
            totalBrightness += b.toInt()
        }
        val avgBrightness = totalBrightness / cellPixels.size

        for ((name, template) in templates) {
            val tplPixels = IntArray(32 * 32)
            template.getPixels(tplPixels, 0, 32, 0, 0, 32, 32)

            var diff = 0.0
            var validPixels = 0
            
            for (i in 0 until 32 * 32) {
                val tplAlpha = (tplPixels[i] ushr 24) and 0xFF
                if (tplAlpha > 100) { // Nới lỏng alpha một chút để bắt được viền quân cờ
                    val c1 = (cellPixels[i] shr 16 and 0xFF) * 0.299 + (cellPixels[i] shr 8 and 0xFF) * 0.587 + (cellPixels[i] and 0xFF) * 0.114
                    val c2 = (tplPixels[i] shr 16 and 0xFF) * 0.299 + (tplPixels[i] shr 8 and 0xFF) * 0.587 + (tplPixels[i] and 0xFF) * 0.114
                    diff += abs(c1 - c2)
                    validPixels++
                }
            }
            
            if (validPixels > 100) {
                diff /= validPixels.toDouble()
                if (diff < minDiff && diff < threshold) {
                    minDiff = diff
                    // 2. Check xem quân cờ này có khớp màu với độ sáng của ô không
                    val isWhitePiece = name[0].isUpperCase()
                    // Nếu là quân Trắng mà độ sáng TB < 100 -> Nhầm, nó là quân Đen
                    // Nếu là quân Đen mà độ sáng TB > 150 -> Nhầm, nó là quân Trắng
                    if (isWhitePiece && avgBrightness < 100) continue
                    if (!isWhitePiece && avgBrightness > 150) continue

                    bestName = name
                }
            }
        }
        return bestName
    }
}
