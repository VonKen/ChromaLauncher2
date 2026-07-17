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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.plugins.RendererPlugin;
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

        loadRenderers();
    }

    private void loadRenderers() {
        Context context = getContext();
        if (context == null) return;

        RendererCompatUtil.RenderersList renderers = RendererCompatUtil.getCompatibleRenderers(context);
        List<RendererPlugin> plugins = RendererPlugin.discoverPlugins(context);

        List<RendererEntry> entries = new ArrayList<>();

        // Add built-in renderers
        for (int i = 0; i < renderers.rendererIds.size(); i++) {
            String id = renderers.rendererIds.get(i);
            String name = renderers.rendererDisplayNames[i];
            // Skip plugin renderers here, they'll be added below
            RendererPlugin plugin = RendererPlugin.findPlugin(id);
            if (plugin == null) {
                entries.add(new RendererEntry(id, name, true, false));
            }
        }

        // Add plugin renderers
        for (RendererPlugin plugin : plugins) {
            entries.add(new RendererEntry(
                    plugin.getRendererId(),
                    plugin.getDisplayName(),
                    plugin.isInstalled(),
                    true
            ));
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
            String tag = entry.isPlugin
                    ? (entry.isInstalled ? "Plugin - Installed" : "Plugin - Not installed")
                    : "Built-in";
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
