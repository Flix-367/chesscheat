package com.ollie.chesscheat

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

object ChessEngine {
    private val client = OkHttpClient()
    // NHÉT KEY VÀO ĐÂY
    private const val GEMINI_API_KEY = "AQ.Ab8RN6LMzrOlGiSr1fM_BxiOLWF7AlF8nlygADr8vXGKvtzm_w"
    private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY"

    fun getFenFromImage(bitmap: Bitmap, onResult: (String?) -> Unit) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "Look at this chess board. Return ONLY the FEN string (just the board layout, no extra text). For example: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"))
                        put(JSONObject().put("inline_data", JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", base64Image)
                        }))
                    })
                })
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(GEMINI_URL).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ChessEngine", "Lỗi mạng: ${e.message}")
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) { 
                        Log.e("ChessEngine", "API lỗi: ${response.code}")
                        onResult(null)
                        return 
                    }
                    val body = response.body?.string() ?: return
                    try {
                        val json = JSONObject(body)
                        val text = json.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim()
                        val fen = text.replace("```", "").replace("text", "").trim()
                        onResult(fen)
                    } catch (e: Exception) {
                        Log.e("ChessEngine", "Lỗi JSON: ${e.message}")
                        onResult(null)
                    }
                }
            }
        })
    }

    fun getBestMove(fen: String, onResult: (String?) -> Unit) {
        val url = "https://stockfish.online/api/s/v2.php?fen=${fen.replace(" ", "%20")}&depth=10"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { onResult(null) }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) { onResult(null); return }
                    try {
                        val json = JSONObject(response.body?.string() ?: "")
                        if (json.optBoolean("success", false)) {
                            onResult(json.optString("bestmove").split(" ").getOrNull(1))
                        } else onResult(null)
                    } catch (e: Exception) { onResult(null) }
                }
            }
        })
    }
}
