package com.ollie.chesscheat

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log

object BoardReader {
    var boardRect = Rect(0, 230, 1080, 1310) 
    private var lastScanTime = 0L
    // FEN mặc định: Ván mới (để test)
    private var currentFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    fun onFrame(bmp: Bitmap) {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < 3000) return // 3 giây quét 1 lần cho đỡ lag
        lastScanTime = now

        OverlayBus.updateBoardRect(boardRect)

        ChessEngine.getBestMove(currentFen) { bestMove ->
            if (bestMove != null && bestMove.length >= 4) {
                Log.d("BoardReader", "AI gợi ý: $bestMove")
                
                // Tính toán toạ độ vẽ mũi tên (giả lập)
                val cellW = (boardRect.right - boardRect.left) / 8f
                val cellH = (boardRect.bottom - boardRect.top) / 8f
                
                // Đổi toạ độ e2e4 sang pixel
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
    }
}

object OverlayBus {
    var boardRect: Rect? = null
    var arrow: ArrowView.Arrow? = null
    var view: ArrowView? = null

    fun updateBoardRect(rect: Rect) {
        this.boardRect = rect
        view?.post { view?.invalidate() }
    }

    fun updateArrow(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        this.arrow = ArrowView.Arrow(fromX, fromY, toX, toY)
        view?.post { view?.invalidate() }
    }
}
