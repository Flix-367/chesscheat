package com.ollie.chesscheat
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import kotlin.math.max
import kotlin.math.min

object BoardReader {
    var boardRect = Rect(0, 230, 1080, 1310)
    private var lastScanTime = 0L
    var lastRequestedFen = ""
    var currentTurn = "w"
    var isFlipped = false

    fun init(ctx: Context) {
        try { PieceDetector.loadTemplates(ctx) } catch (e: Exception) {}
        loadRectFromPrefs(ctx)
    }

    fun loadRectFromPrefs(ctx: Context) {
        val prefs = ctx.getSharedPreferences("chess_prefs", Context.MODE_PRIVATE)
        boardRect = Rect(prefs.getInt("left", 0), prefs.getInt("top", 230), prefs.getInt("right", 1080), prefs.getInt("bottom", 1310))
        OverlayBus.updateBoardRect(boardRect)
    }

    fun onFrame(bmp: Bitmap) {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < 1000) return
        lastScanTime = now
        OverlayBus.updateBoardRect(boardRect)
        try {
            val newFen = PieceDetector.getFenFromBoard(bmp, boardRect, currentTurn, isFlipped)
            if (newFen.startsWith("LỖI")) { OverlayBus.updateFenText(newFen); return }
            if (newFen != lastRequestedFen) {
                lastRequestedFen = newFen
                OverlayBus.updateFenText("FEN: $newFen")
                ChessEngine.getBestMove(newFen) { bestMove ->
                    if (bestMove != null && bestMove.length >= 4) { drawArrow(bestMove) }
                }
            }
        } catch (e: Exception) { OverlayBus.updateFenText("Lỗi quét: ${e.message}") }
    }

    private fun drawArrow(bestMove: String) {
        val cellW = (boardRect.right - boardRect.left) / 8f; val cellH = (boardRect.bottom - boardRect.top) / 8f
        fun getXY(square: String): Pair<Float, Float> {
            var file = square[0].lowercaseChar() - 'a'; var rank = 8 - square[1].digitToInt()
            if (isFlipped) { file = 7 - file; rank = 7 - rank }
            return Pair(boardRect.left + file * cellW + cellW / 2, boardRect.top + rank * cellH + cellH / 2)
        }
        val from = getXY(bestMove.substring(0, 2)); val to = getXY(bestMove.substring(2, 4))
        Handler(Looper.getMainLooper()).post { OverlayBus.updateArrow(from.first, from.second, to.first, to.second) }
    }
}
