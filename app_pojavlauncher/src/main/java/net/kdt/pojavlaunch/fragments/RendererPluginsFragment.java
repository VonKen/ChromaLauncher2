package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chromalauncher.app.data.Renderer;
import com.chromalauncher.app.manager.RendererManager;
import com.chromalauncher.app.plugins.DriverPlugin;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.plugins.RendererPlugin;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.RendererCompatUtil;

import java.util.ArrayList;
import java.util.List;

public class RendererPluginsFragment extends Fragment {
    public static final String TAG = "RendererPluginsFragment";

    private RecyclerView mRecyclerView;

    private final ActivityResultLauncher<String> mInstallApkLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                installApkFromUri(uri);
            });

    public RendererPluginsFragment() {
        super(R.layout.fragment_renderer_plugins);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView = view.findViewById(R.id.renderer_list);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        view.findViewById(R.id.install_renderer_apk_button).setOnClickListener(v -> {
            mInstallApkLauncher.launch("application/vnd.android.package-archive");
        });

        view.findViewById(R.id.mobileglues_settings_button).setOnClickListener(v -> {
            Tools.swapFragment(requireActivity(), MobileGluesConfigFragment.class, MobileGluesConfigFragment.TAG, null);
        });

        view.findViewById(R.id.vulkan_driver_button).setOnClickListener(v -> showVulkanDriverDialog());

        loadRenderers();
    }

    private void showVulkanDriverDialog() {
        Context context = getContext();
        if (context == null) return;

        DriverPlugin.INSTANCE.refresh(context);
        java.util.List<DriverPlugin.Driver> drivers = DriverPlugin.INSTANCE.getDriverList();
        String[] names = new String[drivers.size()];
        int checked = 0;
        String current = LauncherPreferences.PREF_VK_DRIVER;
        for (int i = 0; i < drivers.size(); i++) {
            DriverPlugin.Driver driver = drivers.get(i);
            names[i] = driver.getDriver() + " (" + driver.getPath() + ")";
            if (driver.getDriver().equals(current)) checked = i;
        }

        boolean[] systemChecked = new boolean[]{LauncherPreferences.PREF_VK_DRIVER_SYSTEM};

        new AlertDialog.Builder(context)
                .setTitle(R.string.renderer_plugin_vulkan_driver)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    String selectedDriver = drivers.get(which).getDriver();
                    LauncherPreferences.PREF_VK_DRIVER = selectedDriver;
                    LauncherPreferences.DEFAULT_PREF.edit().putString("vkDriver", selectedDriver).apply();
                })
                .setMultiChoiceItems(
                        new String[]{context.getString(R.string.renderer_plugin_vulkan_system_driver)},
                        new boolean[]{systemChecked[0]},
                        (dialog, which, isChecked) -> {
                            systemChecked[0] = isChecked;
                            LauncherPreferences.PREF_VK_DRIVER_SYSTEM = isChecked;
                            LauncherPreferences.DEFAULT_PREF.edit().putBoolean("vkDriverSystem", isChecked).apply();
                        }
                )
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void loadRenderers() {
        Context context = getContext();
        if (context == null) return;

        List<RendererEntry> entries = new ArrayList<>();
        boolean hasMobileGlues = false;

        try {
            // Read ALL built-in renderers from arrays (not filtered by compatibility)
            String[] rendererIds = context.getResources().getStringArray(R.array.renderer_values);
            String[] rendererNames = context.getResources().getStringArray(R.array.renderer);

            // Get the compatible set so we can show status
            RendererCompatUtil.RenderersList compatible = RendererCompatUtil.getCompatibleRenderers(context);

            for (int i = 0; i < rendererIds.length; i++) {
                boolean isCompatible = compatible.rendererIds.contains(rendererIds[i]);
                entries.add(new RendererEntry(rendererIds[i], rendererNames[i], isCompatible, false));
                if ("mobileglues".equals(rendererIds[i])) hasMobileGlues = true;
            }

            // Discover plugin renderers
            List<RendererPlugin> plugins = RendererPlugin.discoverPlugins(context);
            for (RendererPlugin plugin : plugins) {
                entries.add(new RendererEntry(
                        plugin.getRendererId(),
                        plugin.getDisplayName(),
                        plugin.isInstalled(),
                        true
                ));
            }

            // Discover Fold Craft Launcher renderer plugins
            for (Renderer fclRenderer : RendererManager.INSTANCE.getRendererList()) {
                entries.add(new RendererEntry(
                        fclRenderer.getId(),
                        fclRenderer.getName(),
                        true,
                        true
                ));
            }
        } catch (Exception e) {
            entries.add(new RendererEntry("error", "Failed to load renderers: " + e.getMessage(), false, false));
        }

        // Show MobileGlues settings button if mobileglues is in the renderer list
        View mgSettingsButton = getView() != null ? getView().findViewById(R.id.mobileglues_settings_button) : null;
        if (mgSettingsButton != null) {
            mgSettingsButton.setVisibility(hasMobileGlues ? View.VISIBLE : View.GONE);
        }

        mRecyclerView.setAdapter(new RendererAdapter(entries));
    }

    private void installApkFromUri(Uri uri) {
        Context context = getContext();
        if (context == null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, R.string.modpack_install_download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private static class RendererEntry {
        final String id;
        final String name;
        final boolean isInstalled;
        final boolean isPlugin;

        RendererEntry(String id, String name, boolean isInstalled, boolean isPlugin) {
            this.id = id;
            this.name = name;
            this.isInstalled = isInstalled;
            this.isPlugin = isPlugin;
        }
    }

    private static class RendererAdapter extends RecyclerView.Adapter<RendererAdapter.ViewHolder> {
        private final List<RendererEntry> mEntries;

        RendererAdapter(List<RendererEntry> entries) {
            mEntries = entries;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_renderer_entry, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RendererEntry entry = mEntries.get(position);
            holder.mNameText.setText(entry.name);
            holder.mIdText.setText(entry.id);
            String tag;
            if (entry.isPlugin) {
                tag = entry.isInstalled ? "Plugin" : "Plugin (not installed)";
            } else {
                tag = entry.isInstalled ? "Built-in" : "Not compatible";
            }
            holder.mTagText.setText(tag);
        }

        @Override
        public int getItemCount() {
            return mEntries.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mNameText;
            final TextView mIdText;
            final TextView mTagText;

            ViewHolder(View view) {
                super(view);
                mNameText = view.findViewById(R.id.renderer_entry_name);
                mIdText = view.findViewById(R.id.renderer_entry_id);
                mTagText = view.findViewById(R.id.renderer_entry_tag);
            }
        }
    }
}
