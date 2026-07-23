package net.kdt.pojavlaunch.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class InstanceModsFragment extends Fragment {
    public static final String TAG = "InstanceModsFragment";
    private static final String ARG_GAME_DIR = "gameDir";

    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private File mModsDir;
    private ModAdapter mAdapter;

    public InstanceModsFragment() {
        super(R.layout.fragment_instance_mods);
    }

    public static InstanceModsFragment newInstance(String gameDir) {
        InstanceModsFragment fragment = new InstanceModsFragment();
        Bundle args = new Bundle(1);
        args.putString(ARG_GAME_DIR, gameDir);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView = view.findViewById(R.id.instance_mods_list);
        mEmptyView = view.findViewById(R.id.instance_mods_empty);

        String gameDirPath = getArguments() != null ? getArguments().getString(ARG_GAME_DIR) : null;
        if (gameDirPath == null) {
            Toast.makeText(getContext(), R.string.instance_mods_no_folder, Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        mModsDir = new File(gameDirPath, "mods");

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new ModAdapter();
        mRecyclerView.setAdapter(mAdapter);

        view.findViewById(R.id.instance_mods_install_button).setOnClickListener(v -> {
            ExtraCore.setValue(ExtraConstants.INSTANCE_MODS_DIR, gameDirPath);
            Tools.swapFragment(requireActivity(), SearchModFragment.class, SearchModFragment.TAG, null);
        });

        refreshModList();
    }

    private void refreshModList() {
        List<File> mods = new ArrayList<>();
        if (mModsDir.exists() && mModsDir.isDirectory()) {
            File[] files = mModsDir.listFiles();
            if (files != null) {
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (File f : files) {
                    if (f.isFile() && (f.getName().endsWith(".jar") || f.getName().endsWith(".jar.disabled"))) {
                        mods.add(f);
                    }
                }
            }
        }
        mAdapter.setMods(mods);
        mEmptyView.setVisibility(mods.isEmpty() ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(mods.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private class ModAdapter extends RecyclerView.Adapter<ModAdapter.ModViewHolder> {
        private List<File> mMods = new ArrayList<>();

        void setMods(List<File> mods) {
            mMods = mods;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ModViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_instance_mod, parent, false);
            return new ModViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ModViewHolder holder, int position) {
            File modFile = mMods.get(position);
            holder.bind(modFile);
        }

        @Override
        public int getItemCount() {
            return mMods.size();
        }

        class ModViewHolder extends RecyclerView.ViewHolder {
            private final TextView mNameView;
            private final TextView mStatusView;
            private final ImageButton mToggleButton;
            private final ImageButton mDeleteButton;

            ModViewHolder(@NonNull View itemView) {
                super(itemView);
                mNameView = itemView.findViewById(R.id.mod_item_name);
                mStatusView = itemView.findViewById(R.id.mod_item_status);
                mToggleButton = itemView.findViewById(R.id.mod_item_toggle);
                mDeleteButton = itemView.findViewById(R.id.mod_item_delete);
            }

            void bind(File modFile) {
                String name = modFile.getName();
                boolean enabled = !name.endsWith(".disabled");

                // Clean display name
                String displayName = name;
                if (displayName.endsWith(".disabled")) {
                    displayName = displayName.substring(0, displayName.length() - ".disabled".length());
                }

                mNameView.setText(displayName);
                mStatusView.setText(enabled ? R.string.instance_mods_enabled : R.string.instance_mods_disabled);
                mStatusView.setTextColor(getResources().getColor(enabled ? R.color.minebutton_color : R.color.secondary_text, null));
                mToggleButton.setImageResource(enabled ? R.drawable.ic_mod_enabled : R.drawable.ic_mod_disabled);
                mToggleButton.setContentDescription(getString(enabled ? R.string.instance_mods_disable : R.string.instance_mods_enable));

                mToggleButton.setOnClickListener(v -> toggleMod(modFile, enabled));
                mDeleteButton.setOnClickListener(v -> confirmDelete(modFile));
            }

            private void toggleMod(File modFile, boolean currentlyEnabled) {
                String newName;
                if (currentlyEnabled) {
                    newName = modFile.getName() + ".disabled";
                } else {
                    newName = modFile.getName().substring(0, modFile.getName().length() - ".disabled".length());
                }
                File newFile = new File(modFile.getParentFile(), newName);
                if (modFile.renameTo(newFile)) {
                    refreshModList();
                } else {
                    Toast.makeText(getContext(), R.string.fm_delete_failed, Toast.LENGTH_SHORT).show();
                }
            }

            private void confirmDelete(File modFile) {
                String displayName = modFile.getName();
                if (displayName.endsWith(".disabled")) {
                    displayName = displayName.substring(0, displayName.length() - ".disabled".length());
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.instance_mods_delete_title)
                        .setMessage(getString(R.string.instance_mods_delete_message, displayName))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            if (modFile.delete()) {
                                refreshModList();
                            } else {
                                Toast.makeText(getContext(), R.string.fm_delete_failed, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
            }
        }
    }
}
