package com.ollie.chesscheat

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log

object BoardReader {
    // Vùng bàn cờ ước lượng cho màn hình dọc (mày chỉnh lại theo máy mày)
    var boardRect = Rect(0, 230, 1080, 1310) 
    private var lastScanTime = 0L

    fun onFrame(bmp: Bitmap) {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < 500) return // Quét mỗi nửa giây
        lastScanTime = now

        // Cập nhật vị trí cho mũi tên (tạm thời vẽ 1 đường chéo trong khung)
        OverlayBus.updateBoardRect(boardRect)
    }
}

object OverlayBus {
    var boardRect: Rect? = null
    var view: ArrowView? = null

    fun updateBoardRect(rect: Rect) {
        this.boardRect = rect
        view?.invalidate() // Bắt ArrowView vẽ lại
    }
}
