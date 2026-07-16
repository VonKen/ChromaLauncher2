package net.kdt.pojavlaunch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;

public class FatalErrorActivity extends Activity {
    private static final String TAG = "FatalError";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setPadding(32, 32, 32, 32);
        tv.setTextSize(12);

        StringBuilder sb = new StringBuilder();
        sb.append("=== Chroma Launcher Crash ===\n\n");

        Throwable th = null;
        String crashPath = null;

        try {
            th = (Throwable) getIntent().getSerializableExtra("throwable");
        } catch (Exception ignored) {}
        crashPath = getIntent().getStringExtra("show_error");

        if (crashPath != null) {
            sb.append("Crash file: ").append(crashPath).append("\n\n");
        }

        if (th != null) {
            StringWriter sw = new StringWriter();
            th.printStackTrace(new PrintWriter(sw));
            sb.append(sw.toString());
        } else {
            sb.append("No throwable extra available.\n");
            sb.append("Intent: ").append(getIntent()).append("\n");
            if (getIntent().getExtras() != null) {
                sb.append("Extras: ").append(getIntent().getExtras().toString()).append("\n");
            }
        }

        tv.setText(sb.toString());
        scroll.addView(tv);
        setContentView(scroll);

        Log.e(TAG, sb.toString());
    }

    public static void showError(Context context, String crashFile, boolean storageAllowed, Throwable th) {
        try {
            Intent intent = new Intent(context, FatalErrorActivity.class);
            intent.putExtra("show_error", crashFile);
            intent.putExtra("throwable", th);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e("FatalError", "Failed to show error activity", e);
        }
    }
}
