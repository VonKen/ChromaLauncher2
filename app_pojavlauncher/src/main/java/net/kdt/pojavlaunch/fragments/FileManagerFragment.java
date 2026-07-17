package net.kdt.pojavlaunch.fragments;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.pickafile.FileListView;
import com.kdt.pickafile.FileSelectedListener;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;

import java.io.File;

public class FileManagerFragment extends Fragment {
    public static final String TAG = "FileManagerFragment";

    private FileListView mFileListView;
    private TextView mFilePathView;
    private ImageButton mUpButton;
    private ImageButton mNewFolderButton;
    private ImageButton mDeleteButton;

    public FileManagerFragment() {
        super(R.layout.fragment_file_manager);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mFileListView = view.findViewById(R.id.file_manager_list);
        mFilePathView = view.findViewById(R.id.file_manager_path);
        mUpButton = view.findViewById(R.id.file_manager_up);
        mNewFolderButton = view.findViewById(R.id.file_manager_new_folder);
        mDeleteButton = view.findViewById(R.id.file_manager_delete);

        String rootPath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                ? Tools.DIR_GAME_NEW
                : Environment.getExternalStorageDirectory().getAbsolutePath();

        mFileListView.setShowFiles(true);
        mFileListView.setShowFolders(true);
        mFileListView.lockPathAt(new File(rootPath));
        mFileListView.setDialogTitleListener(title -> mFilePathView.setText(title));
        mFileListView.refreshPath();

        mFileListView.setFileSelectedListener(new FileSelectedListener() {
            @Override
            public void onFileSelected(File file, String path) {
                if (file.isDirectory()) {
                    mFileListView.listFileAt(file);
                    mFilePathView.setText(file.getAbsolutePath());
                } else {
                    showFileInfo(file);
                }
            }
        });

        mUpButton.setOnClickListener(v -> {
            File current = mFileListView.getFullPath();
            File parent = current.getParentFile();
            if (parent != null) {
                mFileListView.listFileAt(parent);
                mFilePathView.setText(parent.getAbsolutePath());
            }
        });

        mNewFolderButton.setOnClickListener(v -> {
            EditText editText = new EditText(getContext());
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.folder_dialog_insert_name)
                    .setView(editText)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.folder_dialog_create, (dialog, which) -> {
                        String name = editText.getText().toString().trim();
                        if (name.isEmpty()) return;
                        File folder = new File(mFileListView.getFullPath(), name);
                        if (folder.mkdir()) {
                            mFileListView.listFileAt(folder);
                        } else {
                            mFileListView.refreshPath();
                            Toast.makeText(getContext(), R.string.fm_create_folder_failed, Toast.LENGTH_SHORT).show();
                        }
                    }).show();
        });

        mDeleteButton.setOnClickListener(v -> {
            File current = mFileListView.getFullPath();
            if (current.equals(new File(mFileListView.getFullPath().getParent()))) {
                Toast.makeText(getContext(), R.string.fm_cannot_delete_root, Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.fm_delete_title)
                    .setMessage(getString(R.string.fm_delete_message, current.getName()))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        if (deleteRecursive(current)) {
                            File parent = current.getParentFile();
                            if (parent != null) {
                                mFileListView.listFileAt(parent);
                                mFilePathView.setText(parent.getAbsolutePath());
                            }
                        } else {
                            Toast.makeText(getContext(), R.string.fm_delete_failed, Toast.LENGTH_SHORT).show();
                        }
                    }).show();
        });
    }

    private void showFileInfo(File file) {
        String size = formatFileSize(file.length());
        String name = file.getName();
        String path = file.getAbsolutePath();

        new AlertDialog.Builder(getContext())
                .setTitle(name)
                .setMessage(getString(R.string.fm_file_info, size, path))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private boolean deleteRecursive(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursive(child)) return false;
                }
            }
        }
        return fileOrDir.delete();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
