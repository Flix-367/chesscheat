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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private EditText fenInput;
    private EditText depthInput;
    private TextView output;
    private Button analyzeButton;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private static final String START_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
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
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                fos.flush();
                fos.close();
                is.close();
                engineFile.setExecutable(true);
            } catch (Exception e) {
                output.setText("Lỗi giải nén engine: " + e.getMessage());
            }
        }
    }

    private void buildUi() {
        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(0xFFF5F7FA);

        TextView title = new TextView(this);
        title.setText("Stockfish Study");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(0xFF172033);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Offline chess analysis · UCI engine");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFF566176);
        root.addView(subtitle);

        TextView fenLabel = new TextView(this);
        fenLabel.setText("FEN position");
        fenLabel.setPadding(0, dp(18), 0, dp(6));
        root.addView(fenLabel);

        fenInput = new EditText(this);
        fenInput.setText(START_FEN);
        fenInput.setTextSize(13);
        fenInput.setSingleLine(false);
        fenInput.setMinLines(2);
        fenInput.setGravity( android.view.Gravity.TOP );
        root.addView(fenInput, new LinearLayout.LayoutParams(-1, dp(88)));

        TextView depthLabel = new TextView(this);
        depthLabel.setText("Search depth (1–20)");
        depthLabel.setPadding(0, dp(12), 0, dp(4));
        root.addView(depthLabel);

        depthInput = new EditText(this);
        depthInput.setText("12");
        depthInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        root.addView(depthInput, new LinearLayout.LayoutParams(-1, dp(52)));

        analyzeButton = new Button(this);
        analyzeButton.setText("Analyze position");
        root.addView(analyzeButton);

        output = new TextView(this);
        output.setText("Ready. Engine extracted to internal storage.");
        output.setTextIsSelectable(true);
        output.setTextSize(14);
        output.setTextColor(0xFF172033);
        output.setPadding(dp(12), dp(12), dp(12), dp(12));
        output.setBackgroundColor(0xFFFFFFFF);

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
        if (depth < 1 || depth > 20) {
            Toast.makeText(this, "Depth must be between 1 and 20", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!looksLikeFen(fen)) {
            output.setText("FEN format looks invalid.");
            return;
        }

        final int chosenDepth = depth;
        analyzeButton.setEnabled(false);
        output.setText("Starting Stockfish…");
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
        if (!engine.exists()) {
            return "Stockfish engine not found.";
        }
        StringBuilder lines = new StringBuilder();
        Process process = null;
        try {
            process = new ProcessBuilder(engine.getAbsolutePath())
                    .directory(getFilesDir()).redirectErrorStream(true).start();
            OutputStream stdin = process.getOutputStream();
            BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            send(stdin, "uci");
            readUntil(stdout, lines, "uciok", 5000);
            send(stdin, "isready");
            readUntil(stdout, lines, "readyok", 5000);
            send(stdin, "position fen " + fen);
            send(stdin, "go depth " + depth);
            readUntil(stdout, lines, "bestmove", 120000);
            send(stdin, "quit");
            process.waitFor();
            return lines.length() == 0 ? "Engine returned no output." : lines.toString();
        } catch (Exception e) {
            return "Engine error: " + e.getMessage();
        } finally {
            if (process != null) process.destroy();
        }
    }

    private void send(OutputStream stdin, String command) throws Exception {
        stdin.write((command + "\n").getBytes(StandardCharsets.UTF_8));
        stdin.flush();
    }

    private void readUntil(BufferedReader reader, StringBuilder out, String marker, long timeoutMs)
            throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line == null) break;
                out.append(line).append('\n');
                if (line.contains(marker)) return;
            } else {
                Thread.sleep(30);
            }
        }
        out.append("[timeout waiting for ").append(marker).append("]\n");
    }

    private boolean looksLikeFen(String fen) {
        String[] parts = fen.split("\\s+");
        if (parts.length != 6) return false;
        String[] ranks = parts[0].split("/");
        if (ranks.length != 8 || !(parts[1].equals("w") || parts[1].equals("b"))) return false;
        for (String rank : ranks) {
            int count = 0;
            for (char c : rank.toCharArray()) {
                if (c >= '1' && c <= '8') count += c - '0';
                else if ("prnbqkPRNBQK".indexOf(c) >= 0) count++;
                else return false;
            }
            if (count != 8) return false;
        }
        return true;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
