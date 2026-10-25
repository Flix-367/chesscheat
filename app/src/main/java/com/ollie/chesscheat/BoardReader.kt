package com.ollie.chesscheat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log

object BoardReader {
    var boardRect = Rect(0, 230, 1080, 1310) 
    private var lastScanTime = 0L
    var lastRequestedFen = ""
    var currentTurn = "w"

    fun init(ctx: Context) {
        PieceDetector.loadTemplates(ctx)
        loadRectFromPrefs(ctx)
    }

    fun loadRectFromPrefs(ctx: Context) {
        val prefs = ctx.getSharedPreferences("chess_prefs", Context.MODE_PRIVATE)
        boardRect = Rect(
            prefs.getInt("left", 0),
            prefs.getInt("top", 230),
            prefs.getInt("right", 1080),
            prefs.getInt("bottom", 1310)
        )
        OverlayBus.updateBoardRect(boardRect)
    }

    fun onFrame(bmp: Bitmap) {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < 300) return 
        lastScanTime = now

        OverlayBus.updateBoardRect(boardRect)

        val newFen = PieceDetector.getFenFromBoard(bmp, boardRect, currentTurn)
        OverlayBus.updateFenText("FEN: $newFen")

        if (newFen.startsWith("LỖI")) return

        if (newFen != lastRequestedFen) {
            lastRequestedFen = newFen
            Log.d("BoardReader", "Đọc được FEN: $newFen")
            
            ChessEngine.getBestMove(newFen) { bestMove ->
                if (bestMove != null && bestMove.length >= 4) {
                    drawArrow(bestMove)
                }
            }
        }
    }

    private fun drawArrow(bestMove: String) {
        val cellW = (boardRect.right - boardRect.left) / 8f
        val cellH = (boardRect.bottom - boardRect.top) / 8f
        
        fun getXY(square: String): Pair<Float, Float> {
            val file = square[0].lowercaseChar() - 'a'
            val rank = 8 - square[1].digitToInt()
            val x = boardRect.left + file * cellW + cellW / 2
            val y = boardRect.top + rank * cellH + cellH / 2
            return Pair(x, y)
        }

        val from = getXY(bestMove.substring(0, 2))
        val to = getXY(bestMove.substring(2, 4))

        Handler(Looper.getMainLooper()).post {
            OverlayBus.updateArrow(from.first, from.second, to.first, to.second)
        }
    }
}
