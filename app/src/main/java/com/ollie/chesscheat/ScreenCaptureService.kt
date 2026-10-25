package com.ollie.chesscheat

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var lastScanTime = 0L

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notif = buildNotif()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else {
                startForeground(1, notif)
            }

            val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
            val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra("data", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra("data")
            }
            
            if (data != null && projection == null) {
                val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                projection = mpm.getMediaProjection(resultCode, data)
                
                // ĐĂNG KÝ CALLBACK BẮT BUỘC CHO ANDROID 14 TRƯỚC KHI TẠO DISPLAY
                projection?.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                        stopEverything()
                    }
                }, Handler(Looper.getMainLooper()))

                setupCapture()
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "Lỗi Service: ${e.message}", Toast.LENGTH_LONG).show()
            }
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            try {
                val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                File(downloadDir, "chesscheat_service_crash.txt").writeText(sw.toString())
            } catch (ex: Exception) {}
            stopSelf()
        }
        return START_STICKY
    }

    private fun setupCapture() {
        // Tạo luồng ngầm riêng để xử lý ảnh
        handlerThread = HandlerThread("ImageReaderThread").apply { start() }
        backgroundHandler = Handler(handlerThread!!.looper)

        val metrics = resources.displayMetrics
        // Giảm độ phân giải một nửa để đỡ tràn RAM
        val width = metrics.widthPixels / 2
        val height = metrics.heightPixels / 2

        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        projection?.createVirtualDisplay(
            "Capture", width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader?.surface, null, null
        )

        reader?.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            val now = System.currentTimeMillis()
            if (now - lastScanTime < 200) { image.close(); return@setOnImageAvailableListener }
            lastScanTime = now

            try {
                val plane = image.planes[0]
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width
                val bmp = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(plane.buffer)
                BoardReader.onFrame(bmp)
            } catch (e: Exception) {
                Log.e("ScreenCapture", "Lỗi: ${e.message}")
            } finally {
                image.close()
            }
        }, backgroundHandler)
    }

    private fun stopEverything() {
        try {
            display?.release()
            reader?.close()
            projection?.stop()
            projection = null
            handlerThread?.quitSafely()
        } catch (e: Exception) {}
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopEverything()
    }

    private fun buildNotif(): Notification {
        val channelId = "capture"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Chess Capture", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("ChessCheat")
            .setContentText("Đang quét...")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }
}
