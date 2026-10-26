package com.ollie.chesscheat
import android.util.Log
import java.io.*
import kotlin.concurrent.thread

object ChessEngine {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null

    fun initEngine(context: android.content.Context) {
        try {
            val enginePath = context.applicationInfo.nativeLibraryDir + "/libstockfish.so"
            process = ProcessBuilder(enginePath).redirectErrorStream(true).start()
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            sendCommand("uci")
            sendCommand("isready")
        } catch (e: Exception) { Log.e("Stockfish", "Lỗi: ${e.message}") }
    }

    private fun sendCommand(cmd: String) {
        try { writer?.write("$cmd\n"); writer?.flush() } catch (e: Exception) {}
    }

    fun getBestMove(fen: String, onResult: (String?) -> Unit) {
        if (process == null) { onResult(null); return }
        thread {
            try {
                sendCommand("stop")
                sendCommand("position fen $fen")
                sendCommand("go depth 12")
                var line: String?
                var bestMove: String? = null
                while (reader?.readLine().also { line = it } != null) {
                    if (line!!.startsWith("bestmove")) {
                        bestMove = line!!.split(" ")[1]
                        break
                    }
                }
                onResult(bestMove)
            } catch (e: Exception) { onResult(null) }
        }
    }
}
