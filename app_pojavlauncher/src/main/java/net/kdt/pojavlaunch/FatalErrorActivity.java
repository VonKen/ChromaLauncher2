package net.kdt.pojavlaunch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class FatalErrorActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }

    public static void showError(Context context, String crashFile, boolean storageAllowed, Throwable th) {
        Intent intent = new Intent(context, FatalErrorActivity.class);
        intent.putExtra("show_error", crashFile);
        intent.putExtra("throwable", th);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
