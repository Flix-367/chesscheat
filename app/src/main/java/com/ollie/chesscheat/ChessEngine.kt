package com.ollie.chesscheat

import android.content.Context
import android.util.Log
import java.io.*
import kotlin.concurrent.thread

object ChessEngine {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null

    fun initEngine(context: Context) {
        try {
            val binFile = File(context.filesDir, "stockfish")
            if (!binFile.exists()) {
                context.assets.open("stockfish").use { input ->
                    binFile.outputStream().use { output -> input.copyTo(output) }
                }
                binFile.setExecutable(true)
            }
            
            process = ProcessBuilder(binFile.absolutePath).start()
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            
            sendCommand("uci")
            sendCommand("isready")
            Log.d("Stockfish", "Engine Đã Sẵn Sàng!")
        } catch (e: Exception) {
            Log.e("Stockfish", "Lỗi: ${e.message}")
        }
    }

    private fun sendCommand(cmd: String) {
        try {
            writer?.write("$cmd\n")
            writer?.flush()
        } catch (e: Exception) {}
    }

    fun getBestMove(fen: String, onResult: (String?) -> Unit) {
        if (process == null) { onResult(null); return }
        thread {
            try {
                sendCommand("stop")
                sendCommand("position fen $fen")
                sendCommand("go depth 15") // Độ sâu 15 cho nó khôn

                var line: String?
                var bestMove: String? = null
                while (reader?.readLine().also { line = it } != null) {
                    if (line!!.startsWith("bestmove")) {
                        bestMove = line!!.split(" ")[1]
                        break
                    }
                }
                onResult(bestMove)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
}
