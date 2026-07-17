package net.kdt.pojavlaunch.plugins;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RendererPlugin {
    private static final String TAG = "RendererPlugin";
    private static final String PLUGIN_PACKAGE_PREFIX = "git.mojo.renderer.";

    private static List<RendererPlugin> sCachedPlugins;

    private final String rendererId;
    private final String displayName;
    private final String libraryPath;
    private final String nativeLibraryName;

    private RendererPlugin(String rendererId, String displayName, String libraryPath, String nativeLibraryName) {
        this.rendererId = rendererId;
        this.displayName = displayName;
        this.libraryPath = libraryPath;
        this.nativeLibraryName = nativeLibraryName;
    }

    public String getRendererId() { return rendererId; }
    public String getDisplayName() { return displayName; }
    public String getLibraryPath() { return libraryPath; }
    public String getNativeLibraryName() { return nativeLibraryName; }

    public String resolveAbsolutePath() {
        return new File(libraryPath, nativeLibraryName).getAbsolutePath();
    }

    public boolean isInstalled() {
        return new File(libraryPath, nativeLibraryName).exists();
    }

    public static RendererPlugin findPlugin(String rendererId) {
        if (sCachedPlugins == null) return null;
        for (RendererPlugin plugin : sCachedPlugins) {
            if (plugin.getRendererId().equals(rendererId)) return plugin;
        }
        return null;
    }

    public static void setCachedPlugins(List<RendererPlugin> plugins) {
        sCachedPlugins = plugins;
    }

    public static void releaseCache() {
        sCachedPlugins = null;
    }

    public static List<RendererPlugin> discoverPlugins(Context context) {
        List<RendererPlugin> plugins = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);

        for (PackageInfo pkg : packages) {
            String packageName = pkg.packageName;
            if (!packageName.startsWith(PLUGIN_PACKAGE_PREFIX)) continue;

            try {
                PackageInfo pluginPkg = pm.getPackageInfo(packageName, PackageManager.GET_SHARED_LIBRARY_FILES);
                String nativeLibDir = pluginPkg.applicationInfo.nativeLibraryDir;

                // Renderer plugins must provide a librenderer.so
                String libName = "librenderer.so";
                if (!new File(nativeLibDir, libName).exists()) continue;

                // Extract renderer ID from package name: git.mojo.renderer.foo -> foo
                String rendererId = packageName.substring(PLUGIN_PACKAGE_PREFIX.length());

                // Try to read display name from app label
                String displayName = pluginPkg.applicationInfo.loadLabel(pm).toString();

                plugins.add(new RendererPlugin(rendererId, displayName, nativeLibDir, libName));
                Log.i(TAG, "Discovered renderer plugin: " + rendererId + " (" + displayName + ")");
            } catch (Exception e) {
                Log.e(TAG, "Failed to discover plugin: " + packageName, e);
            }
        }
        return plugins;
    }
}
