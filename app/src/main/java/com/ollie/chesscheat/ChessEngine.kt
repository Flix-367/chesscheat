package com.ollie.chesscheat

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object ChessEngine {
    private val client = OkHttpClient()

    fun getBestMove(fen: String, onResult: (String?) -> Unit) {
        // Dùng API Stockfish Online (chạy engine thật trên server)
        val url = "https://stockfish.online/api/s/v2.php?fen=${fen.replace(" ", "%20")}&depth=12"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ChessEngine", "Lỗi gọi API: ${e.message}")
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onResult(null); return
                    }
                    val body = response.body?.string()
                    try {
                        val json = JSONObject(body ?: "")
                        if (json.optBoolean("success", false)) {
                            // Chuỗi trả về dạng: "bestmove e2e4 ponder ..."
                            val bestMoveStr = json.optString("bestmove")
                            val bestMove = bestMoveStr.split(" ").getOrNull(1) // Lấy e2e4
                            onResult(bestMove)
                        } else {
                            onResult(null)
                        }
                    } catch (e: Exception) {
                        Log.e("ChessEngine", "Lỗi parse JSON: ${e.message}")
                        onResult(null)
                    }
                }
            }
        })
    }
}
