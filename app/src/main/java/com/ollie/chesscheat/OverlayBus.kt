package com.ollie.chesscheat

import android.graphics.Rect

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
