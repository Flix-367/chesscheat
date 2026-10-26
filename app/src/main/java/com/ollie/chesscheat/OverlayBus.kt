package com.ollie.chesscheat
import android.graphics.Rect

object OverlayBus {
    var boardRect: Rect? = null
    var arrow: ArrowView.Arrow? = null
    var view: ArrowView? = null
    var fenText: String = "Đang chờ đọc bàn cờ..."
    fun updateBoardRect(rect: Rect) { this.boardRect = rect; view?.post { view?.invalidate() } }
    fun updateArrow(fromX: Float, fromY: Float, toX: Float, toY: Float) { this.arrow = ArrowView.Arrow(fromX, fromY, toX, toY); view?.post { view?.invalidate() } }
    fun updateFenText(text: String) { this.fenText = text; view?.post { view?.invalidate() } }
}
