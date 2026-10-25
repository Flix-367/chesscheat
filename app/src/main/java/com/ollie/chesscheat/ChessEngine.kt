package com.ollie.chesscheat

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object ChessEngine {
    private val client = OkHttpClient()

    fun getBestMove(fen: String, onResult: (String?) -> Unit) {
        // Dùng API Lichess Cloud Eval miễn phí
        val url = "https://lichess.org/api/cloud-eval?fen=${fen.replace(" ", "%20")}&multiPv=1"
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
                        val pvs = json.optJSONArray("pvs")
                        if (pvs != null && pvs.length() > 0) {
                            val moves = pvs.getJSONObject(0).optString("moves")
                            // Lấy nước đầu tiên (ví dụ: e2e4)
                            val bestMove = moves.split(" ").firstOrNull()
                            onResult(bestMove)
                        } else {
                            onResult(null)
                        }
                    } catch (e: Exception) {
                        onResult(null)
                    }
                }
            }
        })
    }
}
