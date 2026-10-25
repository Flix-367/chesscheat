package com.ollie.chesscheat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val REQ_CAPTURE = 1001
    private val REQ_OVERLAY = 1002
    private val REQ_NOTIF = 1003
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try { BoardReader.init(this) } catch (e: Exception) {}

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 60)
        }
        layout.addView(TextView(this).apply { text = "Chess Cheat"; textSize = 24f })
        
        tvError = TextView(this).apply {
            text = "Sẵn sàng."
            textSize = 14f
            setTextColor(android.graphics.Color.RED)
            setPadding(0, 20, 0, 20)
        }
        layout.addView(tvError)

        val prefs = getSharedPreferences("chess_prefs", Context.MODE_PRIVATE)
        
        fun createInputRow(label: String, key: String, default: Int): EditText {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 10, 0, 10) }
            row.addView(TextView(this).apply { text = label; width = 150 })
            val et = EditText(this).apply { 
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(prefs.getInt(key, default).toString())
            }
            row.addView(et, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            layout.addView(row)
            return et
        }

        val etLeft = createInputRow("Left:", "left", 0)
        val etTop = createInputRow("Top:", "top", 230)
        val etRight = createInputRow("Right:", "right", 1080)
        val etBottom = createInputRow("Bottom:", "bottom", 1310)

        layout.addView(Button(this).apply {
            text = "Lưu Toạ Độ"
            setOnClickListener {
                prefs.edit()
                    .putInt("left", etLeft.text.toString().toIntOrNull() ?: 0)
                    .putInt("top", etTop.text.toString().toIntOrNull() ?: 230)
                    .putInt("right", etRight.text.toString().toIntOrNull() ?: 1080)
                    .putInt("bottom", etBottom.text.toString().toIntOrNull() ?: 1310)
                    .apply()
                BoardReader.loadRectFromPrefs(this@MainActivity)
                Toast.makeText(this@MainActivity, "Đã lưu toạ độ mới!", Toast.LENGTH_SHORT).show()
            }
        })

        // NÚT TEST AI
        layout.addView(Button(this).apply {
            text = "TEST AI (Vẽ mũi tên e2-e4)"
            setOnClickListener {
                // Gửi thẳng FEN ván mới lên AI
                ChessEngine.getBestMove("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") { bestMove ->
                    if (bestMove != null && bestMove.length >= 4) {
                        tvError.text = "AI trả lời: $bestMove"
                        // Vẽ thẳng mũi tên lên Overlay
                        val cellW = (BoardReader.boardRect.right - BoardReader.boardRect.left) / 8f
                        val cellH = (BoardReader.boardRect.bottom - BoardReader.boardRect.top) / 8f
                        fun getXY(sq: String): Pair<Float, Float> {
                            val f = sq[0] - 'a'; val r = 8 - sq[1].digitToInt()
                            return Pair(BoardReader.boardRect.left + f * cellW + cellW/2, BoardReader.boardRect.top + r * cellH + cellH/2)
                        }
                        val from = getXY(bestMove.substring(0,2))
                        val to = getXY(bestMove.substring(2,4))
                        OverlayBus.updateArrow(from.first, from.second, to.first, to.second)
                    } else {
                        tvError.text = "Lỗi gọi AI, check mạng!"
                    }
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "Lật Bàn Cờ (Đen/Trắng)"
            setOnClickListener {
                BoardReader.isFlipped = !BoardReader.isFlipped
                Toast.makeText(this@MainActivity, "Lật bàn cờ: ${BoardReader.isFlipped}", Toast.LENGTH_SHORT).show()
                BoardReader.lastRequestedFen = ""
            }
        })
        
        layout.addView(Button(this).apply {
            text = "Đổi Lượt (Trắng/Đen)"
            setOnClickListener {
                BoardReader.currentTurn = if (BoardReader.currentTurn == "w") "b" else "w"
                Toast.makeText(this@MainActivity, "Lượt: ${if (BoardReader.currentTurn == "w") "Trắng" else "Đen"}", Toast.LENGTH_SHORT).show()
                BoardReader.lastRequestedFen = ""
            }
        })

        layout.addView(Button(this).apply {
            text = "Bắt đầu quét"
            setOnClickListener { 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
                } else {
                    checkPermissions() 
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "Dừng quét"
            setOnClickListener { stopEverything() }
        })
        
        val scroll = ScrollView(this).apply { addView(layout) }
        setContentView(scroll)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIF) checkPermissions()
    }

    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), REQ_OVERLAY)
        } else {
            startCapture()
        }
    }

    private fun startCapture() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE)
    }

    private fun stopEverything() {
        stopService(Intent(this, ScreenCaptureService::class.java))
        stopService(Intent(this, OverlayService::class.java))
        OverlayBus.view = null
        tvError.text = "Đã dừng quét."
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) startCapture()
            else tvError.text = "Lỗi: Chưa cấp quyền Overlay!"
        } else if (requestCode == REQ_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    startService(Intent(this, OverlayService::class.java))
                    val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                        putExtra("resultCode", resultCode)
                        putExtra("data", data)
                    }
                    startForegroundService(serviceIntent)
                    tvError.text = "Quét thành công! Bấm TEST AI để thử."
                } catch (e: Exception) {
                    tvError.text = "Lỗi Start Service: ${e.message}"
                }
            }
        }
    }
}
