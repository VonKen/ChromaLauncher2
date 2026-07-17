package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import git.artdeell.mojo.R;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MobileGluesConfigFragment extends Fragment {
    public static final String TAG = "MobileGluesConfigFragment";

    private static final String MG_CONFIG_DIR = "/sdcard/MG";
    private static final String MG_CONFIG_FILE = MG_CONFIG_DIR + "/config.json";

    private SwitchCompat mSwShaderCompat;
    private SwitchCompat mSwComputeMultiDraw;
    private SwitchCompat mSwIgnoreErrors;
    private SwitchCompat mSwCrashReport;
    private SwitchCompat mSwFsrUpscale;
    private TextView mTvConfigStatus;

    public MobileGluesConfigFragment() {
        super(R.layout.fragment_mobileglues_config);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSwShaderCompat = view.findViewById(R.id.sw_shader_compatibility);
        mSwComputeMultiDraw = view.findViewById(R.id.sw_compute_multidraw);
        mSwIgnoreErrors = view.findViewById(R.id.sw_ignore_errors);
        mSwCrashReport = view.findViewById(R.id.sw_crash_report);
        mSwFsrUpscale = view.findViewById(R.id.sw_fsr_upscale);
        mTvConfigStatus = view.findViewById(R.id.tv_config_status);

        loadConfig();

        view.findViewById(R.id.btn_save_config).setOnClickListener(v -> saveConfig());
        view.findViewById(R.id.btn_reset_config).setOnClickListener(v -> resetConfig());
    }

    private void loadConfig() {
        try {
            File configFile = new File(MG_CONFIG_FILE);
            if (!configFile.exists()) {
                mTvConfigStatus.setText("No config found. Using defaults.");
                mSwShaderCompat.setChecked(true);
                mSwComputeMultiDraw.setChecked(false);
                mSwIgnoreErrors.setChecked(false);
                mSwCrashReport.setChecked(false);
                mSwFsrUpscale.setChecked(false);
                return;
            }

            StringBuilder sb = new StringBuilder();
            try (FileReader reader = new FileReader(configFile)) {
                char[] buffer = new char[4096];
                int len;
                while ((len = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, len);
                }
            }

            String json = sb.toString();
            mSwShaderCompat.setChecked(parseBool(json, "enableShaderCompatibility", true));
            mSwComputeMultiDraw.setChecked(parseBool(json, "enableComputeMultiDraw", false));
            mSwIgnoreErrors.setChecked(parseBool(json, "ignoreError", false));
            mSwCrashReport.setChecked(parseBool(json, "enableCrashReport", false));
            mSwFsrUpscale.setChecked(parseBool(json, "enableFsr1", false));

            mTvConfigStatus.setText("Config loaded from " + MG_CONFIG_FILE);
        } catch (Exception e) {
            mTvConfigStatus.setText("Error loading config: " + e.getMessage());
            mSwShaderCompat.setChecked(true);
            mSwComputeMultiDraw.setChecked(false);
            mSwIgnoreErrors.setChecked(false);
            mSwCrashReport.setChecked(false);
            mSwFsrUpscale.setChecked(false);
        }
    }

    private void saveConfig() {
        try {
            File configDir = new File(MG_CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            String json = "{\n"
                    + "  \"enableShaderCompatibility\": " + mSwShaderCompat.isChecked() + ",\n"
                    + "  \"enableComputeMultiDraw\": " + mSwComputeMultiDraw.isChecked() + ",\n"
                    + "  \"ignoreError\": " + (mSwIgnoreErrors.isChecked() ? 1 : 0) + ",\n"
                    + "  \"enableCrashReport\": " + mSwCrashReport.isChecked() + ",\n"
                    + "  \"enableFsr1\": " + mSwFsrUpscale.isChecked() + "\n"
                    + "}";

            try (FileWriter writer = new FileWriter(new File(MG_CONFIG_FILE))) {
                writer.write(json);
            }

            mTvConfigStatus.setText("Config saved to " + MG_CONFIG_FILE);
            Toast.makeText(getContext(), "MobileGlues config saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            mTvConfigStatus.setText("Error saving config: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to save config: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetConfig() {
        mSwShaderCompat.setChecked(true);
        mSwComputeMultiDraw.setChecked(false);
        mSwIgnoreErrors.setChecked(false);
        mSwCrashReport.setChecked(false);
        mSwFsrUpscale.setChecked(false);
        mTvConfigStatus.setText("Settings reset to defaults. Click Save to apply.");
    }

    private static boolean parseBool(String json, String key, boolean defaultValue) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return defaultValue;
        int colonIdx = json.indexOf(':', idx);
        if (colonIdx == -1) return defaultValue;
        int start = colonIdx + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return defaultValue;
        if (json.charAt(start) == 't') return true;
        if (json.charAt(start) == 'f') return false;
        if (json.charAt(start) == '1') return true;
        if (json.charAt(start) == '0') return false;
        return defaultValue;
    }
}
