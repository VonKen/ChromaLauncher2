package net.kdt.pojavlaunch.modloaders.modpacks.models;

import net.kdt.pojavlaunch.instances.Instance;

/**
 * Denotes the Minecraft version and mod loader of an instance,
 * used to automatically pick compatible mod versions and filter searches.
 */
public class InstanceInfo {
    public final String mcVersion;
    public final String loader;

    private InstanceInfo(String mcVersion, String loader) {
        this.mcVersion = mcVersion;
        this.loader = loader;
    }

    public static InstanceInfo fromInstance(Instance instance) {
        if (instance == null) return null;
        return parse(instance.versionId);
    }

    /**
     * Parse the mod loader and Minecraft version from an instance version ID
     * (e.g. "neoforge-21.1.227", "fabric-loader-0.15.11-1.21.1", "1.21.1").
     * The loader is a Modrinth category slug when recognized, null for vanilla.
     */
    public static InstanceInfo parse(String versionId) {
        if (versionId == null) return null;
        String version = versionId.trim();
        if (version.isEmpty()) return null;
        if ("latest_release".equals(version) || "latest_snapshot".equals(version)) return null;

        String loader = null;
        String mcVersion;
        if (version.startsWith("neoforge-")) {
            loader = "neoforge";
            mcVersion = mcVersionFromNeoForge(version.substring("neoforge-".length()));
        } else if (version.startsWith("legacy-fabric-loader-")) {
            loader = "legacy_fabric";
            mcVersion = mcVersionFromLoader(version);
        } else if (version.startsWith("fabric-loader-")) {
            loader = "fabric";
            mcVersion = mcVersionFromLoader(version);
        } else if (version.startsWith("quilt-loader-")) {
            loader = "quilt";
            mcVersion = mcVersionFromLoader(version);
        } else if (version.contains("-forge")) {
            loader = "forge";
            mcVersion = version.substring(0, version.indexOf("-forge"));
        } else {
            mcVersion = version;
        }

        if (mcVersion == null || mcVersion.isEmpty()) return null;
        return new InstanceInfo(mcVersion, loader);
    }

    private static String mcVersionFromLoader(String version) {
        int dashIndex = version.lastIndexOf('-');
        if (dashIndex == -1) return null;
        return version.substring(dashIndex + 1);
    }

    private static String mcVersionFromNeoForge(String neoForgeVersion) {
        try {
            int firstIndex = neoForgeVersion.indexOf('.');
            int secondIndex = neoForgeVersion.indexOf('.', firstIndex + 1);
            if (firstIndex == -1 || secondIndex == -1) return null;
            return "1." + neoForgeVersion.substring(0, secondIndex);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
}
