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
    private var currentFen = ""
    private var lastRequestedFen = ""
    private var isWhiteTurn = true

    fun init(ctx: Context) {
        PieceDetector.loadTemplates(ctx)
    }

    fun onFrame(bmp: Bitmap) {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < 1000) return 
        lastScanTime = now

        OverlayBus.updateBoardRect(boardRect)

        val turnStr = if (isWhiteTurn) "w" else "b"
        val newFen = PieceDetector.getFenFromBoard(bmp, boardRect, turnStr)

        if (newFen != currentFen) {
            currentFen = newFen
            Log.d("BoardReader", "FEN mới: $newFen")
            isWhiteTurn = !isWhiteTurn 
            
            if (currentFen != lastRequestedFen) {
                lastRequestedFen = currentFen
                ChessEngine.getBestMove(currentFen) { bestMove ->
                    if (bestMove != null && bestMove.length >= 4) {
                        Log.d("BoardReader", "AI gợi ý: $bestMove")
                        drawArrow(bestMove)
                    }
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
