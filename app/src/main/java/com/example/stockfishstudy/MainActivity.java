package com.example.stockfishstudy;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private EditText fenInput;
    private EditText depthInput;
    private TextView output;
    private Button analyzeButton;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private static final String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        
        // Bắt lỗi toàn cục
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            try {
                File file = new File(getExternalFilesDir(null), "crash_log.txt");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(sw.toString().getBytes());
                fos.close();
            } catch (Exception e) {}
            android.os.Process.killProcess(android.os.Process.myPid());
        });

        buildUi();
        extractEngine();
    }

    private void extractEngine() {
        File engineFile = new File(getFilesDir(), "stockfish");
        if (!engineFile.exists()) {
            try {
                InputStream is = getAssets().open("stockfish");
                FileOutputStream fos = new FileOutputStream(engineFile);
                byte[] buffer = new byte[8192];
                int length;
                while ((length = is.read(buffer)) > 0) { fos.write(buffer, 0, length); }
                fos.flush(); fos.close(); is.close();
                engineFile.setExecutable(true);
                output.setText("Đã giải nén engine thành công!");
            } catch (Exception e) {
                output.setText("Lỗi giải nén: " + e.getMessage());
            }
        } else {
            output.setText("Engine đã sẵn sàng!");
        }
    }

    private void buildUi() {
        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Stockfish Test");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        fenInput = new EditText(this);
        fenInput.setText(START_FEN);
        root.addView(fenInput, new LinearLayout.LayoutParams(-1, dp(88)));

        depthInput = new EditText(this);
        depthInput.setText("12");
        depthInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        root.addView(depthInput, new LinearLayout.LayoutParams(-1, dp(52)));

        analyzeButton = new Button(this);
        analyzeButton.setText("Phân Tích");
        root.addView(analyzeButton);

        output = new TextView(this);
        output.setText("Chờ lệnh...");
        output.setTextIsSelectable(true);
        
        ScrollView scroll = new ScrollView(this);
        scroll.addView(output);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        analyzeButton.setOnClickListener(v -> analyze());
    }

    private void analyze() {
        final String fen = fenInput.getText().toString().trim();
        int depth;
        try { depth = Integer.parseInt(depthInput.getText().toString().trim()); }
        catch (Exception e) { depth = 12; }

        final int chosenDepth = depth;
        analyzeButton.setEnabled(false);
        output.setText("Đang tính toán...");
        worker.execute(() -> {
            String result = runEngine(fen, chosenDepth);
            runOnUiThread(() -> {
                output.setText(result);
                analyzeButton.setEnabled(true);
            });
        });
    }

    private String runEngine(String fen, int depth) {
        File engine = new File(getFilesDir(), "stockfish");
        if (!engine.exists()) return "Không tìm thấy file engine!";
        
        StringBuilder lines = new StringBuilder();
        Process process = null;
        try {
            process = new ProcessBuilder(engine.getAbsolutePath()).redirectErrorStream(true).start();
            OutputStream stdin = process.getOutputStream();
            BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            send(stdin, "uci");
            readUntil(stdout, lines, "uciok", 5000);
            send(stdin, "isready");
            readUntil(stdout, lines, "readyok", 5000);
            send(stdin, "position fen " + fen);
            send(stdin, "go depth " + depth);
            readUntil(stdout, lines, "bestmove", 120000);
            send(stdin, "quit");
            process.waitFor();
            return lines.toString();
        } catch (Exception e) {
            return "Lỗi Engine: " + e.getMessage();
        } finally {
            if (process != null) process.destroy();
        }
    }

    private void send(OutputStream stdin, String cmd) throws Exception {
        stdin.write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
        stdin.flush();
    }

    private void readUntil(BufferedReader reader, StringBuilder out, String marker, long timeoutMs) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line == null) break;
                out.append(line).append('\n');
                if (line.contains(marker)) return;
            } else { Thread.sleep(30); }
        }
        out.append("[timeout]");
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
