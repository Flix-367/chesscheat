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
    private val REQ_CAPTURE = 1001; private val REQ_OVERLAY = 1002; private val REQ_NOTIF = 1003
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { BoardReader.init(this) } catch (e: Exception) {}
        try { ChessEngine.initEngine(this) } catch (e: Exception) {}

        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(60, 100, 60, 60) }
        layout.addView(TextView(this).apply { text = "Chess Cheat Real-time"; textSize = 24f })
        tvError = TextView(this).apply { text = "Sẵn sàng."; textSize = 14f; setTextColor(android.graphics.Color.RED); setPadding(0, 20, 0, 20) }
        layout.addView(tvError)

        val prefs = getSharedPreferences("chess_prefs", Context.MODE_PRIVATE)
        fun createInputRow(label: String, key: String, default: Int): EditText {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 10, 0, 10) }
            row.addView(TextView(this).apply { text = label; width = 150 })
            val et = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; setText(prefs.getInt(key, default).toString()) }
            row.addView(et, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            layout.addView(row); return et
        }
        val etLeft = createInputRow("Left:", "left", 0); val etTop = createInputRow("Top:", "top", 230)
        val etRight = createInputRow("Right:", "right", 1080); val etBottom = createInputRow("Bottom:", "bottom", 1310)

        layout.addView(Button(this).apply { text = "Lưu Toạ Độ"; setOnClickListener {
            prefs.edit().putInt("left", etLeft.text.toString().toIntOrNull() ?: 0).putInt("top", etTop.text.toString().toIntOrNull() ?: 230).putInt("right", etRight.text.toString().toIntOrNull() ?: 1080).putInt("bottom", etBottom.text.toString().toIntOrNull() ?: 1310).apply()
            BoardReader.loadRectFromPrefs(this@MainActivity); Toast.makeText(this@MainActivity, "Đã lưu!", Toast.LENGTH_SHORT).show() } })

        layout.addView(Button(this).apply { text = "Lật Bàn Cờ"; setOnClickListener { BoardReader.isFlipped = !BoardReader.isFlipped; Toast.makeText(this@MainActivity, "Lật: ${BoardReader.isFlipped}", Toast.LENGTH_SHORT).show(); BoardReader.lastRequestedFen = "" } })
        layout.addView(Button(this).apply { text = "Đổi Lượt"; setOnClickListener { BoardReader.currentTurn = if (BoardReader.currentTurn == "w") "b" else "w"; Toast.makeText(this@MainActivity, "Lượt: ${BoardReader.currentTurn}", Toast.LENGTH_SHORT).show(); BoardReader.lastRequestedFen = "" } })
        layout.addView(Button(this).apply { text = "Bắt đầu quét"; setOnClickListener { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF) else checkPermissions() } })
        layout.addView(Button(this).apply { text = "Dừng quét"; setOnClickListener { stopEverything() } })
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); if (requestCode == REQ_NOTIF) checkPermissions() }
    private fun checkPermissions() { if (!Settings.canDrawOverlays(this)) startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), REQ_OVERLAY) else startCapture() }
    private fun startCapture() { val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager; startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE) }
    private fun stopEverything() { stopService(Intent(this, ScreenCaptureService::class.java)); stopService(Intent(this, OverlayService::class.java)); OverlayBus.view = null; tvError.text = "Đã dừng." }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) { if (Settings.canDrawOverlays(this)) startCapture() }
        else if (requestCode == REQ_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    startService(Intent(this, OverlayService::class.java))
                    startForegroundService(Intent(this, ScreenCaptureService::class.java).apply { putExtra("resultCode", resultCode); putExtra("data", data) })
                    tvError.text = "Quét thành công!"
                } catch (e: Exception) { tvError.text = "Lỗi Start: ${e.message}" }
            }
        }
    }
}
