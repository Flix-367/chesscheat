package com.ollie.chesscheat

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log

object BoardReader {
    var boardRect = Rect(0, 230, 1080, 1310) 
    private var lastScanTime = 0L
    private var currentFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    fun onFrame(bmp: Bitmap) {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < 2000) return 
        lastScanTime = now

        OverlayBus.updateBoardRect(boardRect)

        ChessEngine.getBestMove(currentFen) { bestMove ->
            if (bestMove != null) {
                Log.d("BoardReader", "AI gợi ý: $bestMove")
                val cx = (boardRect.left + boardRect.right) / 2f
                val cy = (boardRect.top + boardRect.bottom) / 2f
                
                // Bọc lệnh vẽ vào Main Thread để tránh crash
                Handler(Looper.getMainLooper()).post {
                    OverlayBus.updateArrow(cx, cy, cx + 200f, cy)
                }
            }
        }
    }
}

object OverlayBus {
    var boardRect: Rect? = null
    var arrow: ArrowView.Arrow? = null
    var view: ArrowView? = null

    fun updateBoardRect(rect: Rect) {
        this.boardRect = rect
        view?.post { view?.invalidate() } // Đổi sang post cho an toàn
    }

    fun updateArrow(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        this.arrow = ArrowView.Arrow(fromX, fromY, toX, toY)
        view?.post { view?.invalidate() }
    }
}
