package net.kdt.pojavlaunch.modloaders.modpacks.api;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.instances.InstanceInstaller;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache;
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants;
import net.kdt.pojavlaunch.modloaders.modpacks.models.InstanceInfo;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDependency;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;

public class ModpackInstaller {

    /**
     * Download a single mod/resource pack/shader directly into an existing instance's directory.
     * If ExtraCore has a target INSTANCE_MODS_DIR, it is used; otherwise falls back to the selected instance.
     */
    public static void downloadToInstance(ModDetail modDetail, int selectedVersion, int contentType) throws IOException {
        downloadToInstance(null, modDetail, selectedVersion, contentType, null);
    }

    /**
     * Download a mod/resource pack/shader into an instance's directory.
     * When an API and an instance are provided, the required dependencies of a mod are downloaded too.
     */
    public static void downloadToInstance(ModpackApi modpackApi, ModDetail modDetail, int selectedVersion, int contentType,
                                          InstanceInfo instanceInfo) throws IOException {
        // Check for per-instance target directory first
        String targetGameDir = (String) ExtraCore.consumeValue(ExtraConstants.INSTANCE_MODS_DIR);

        File gameDir;
        if (targetGameDir != null) {
            gameDir = new File(targetGameDir);
        } else {
            Instance instance = Instances.loadSelectedInstance();
            if (instance == null) throw new IOException("No instance selected");
            gameDir = instance.getGameDirectory();
        }

        String versionUrl = modDetail.versionUrls[selectedVersion];
        String versionHash = modDetail.versionHashes[selectedVersion];
        String fileName = (modDetail.title.toLowerCase(Locale.ROOT) + "-" + modDetail.versionNames[selectedVersion])
                .trim().replaceAll("[\\\\/:*?\"<>| \\t\\n]", "_");

        // Determine target directory based on content type
        String subDir;
        switch (contentType) {
            case SearchFilters.TYPE_RESOURCE_PACK:
                subDir = "resourcepacks";
                break;
            case SearchFilters.TYPE_SHADER:
                subDir = "shaderpacks";
                break;
            default:
                subDir = "mods";
                break;
        }

        // Determine file extension
        String extension = ".jar";
        if (contentType == SearchFilters.TYPE_RESOURCE_PACK || contentType == SearchFilters.TYPE_SHADER) {
            extension = ".zip";
        }
        if (!fileName.endsWith(extension)) {
            fileName += extension;
        }

        File targetFile = new File(gameDir, subDir + "/" + fileName);
        FileUtils.ensureParentDirectory(targetFile);

        byte[] downloadBuffer = new byte[8192];
        try {
            DownloadUtils.ensureSha1(targetFile, versionHash, (Callable<Void>) () -> {
                DownloadUtils.downloadFileMonitored(versionUrl, targetFile, downloadBuffer,
                        new DownloaderProgressWrapper(R.string.modpack_download_downloading_metadata,
                                ProgressLayout.INSTALL_MODPACK
                        )
                );
                return null;
            });
        } catch (IOException e) {
            targetFile.delete();
            throw e;
        }

        // Mods can have required dependencies that must be installed alongside them
        if (modpackApi != null && contentType == SearchFilters.TYPE_MOD) {
            installDependencies(modpackApi, modDetail, selectedVersion, gameDir, subDir, instanceInfo, new HashSet<>());
        }
    }

    /**
     * Pick the version index from a mod's version list that best matches the given instance.
     * Prefers a version matching both the Minecraft version and the mod loader,
     * falls back to a version matching only one of them. Returns 0 when nothing matches.
     */
    public static int selectBestVersionIndex(ModDetail modDetail, InstanceInfo instanceInfo) {
        if (modDetail == null || modDetail.versionNames == null || modDetail.versionNames.length == 0) return 0;
        if (instanceInfo == null) return 0;
        String mc = instanceInfo.mcVersion;
        String loader = instanceInfo.loader;
        if (mc == null && loader == null) return 0;
        int versionCount = modDetail.versionNames.length;

        for (int i = 0; i < versionCount; ++i) {
            if (versionMatches(modDetail, i, mc, loader)) return i;
        }
        if (mc != null) {
            for (int i = 0; i < versionCount; ++i) {
                if (versionMatches(modDetail, i, mc, null)) return i;
            }
        }
        if (loader != null) {
            for (int i = 0; i < versionCount; ++i) {
                if (versionMatches(modDetail, i, null, loader)) return i;
            }
        }
        return 0;
    }

    private static boolean versionMatches(ModDetail detail, int index, String mcVersion, String loader) {
        if (mcVersion != null && !containsString(detail.versionGameVersions != null && index < detail.versionGameVersions.length
                ? detail.versionGameVersions[index] : null, mcVersion)) return false;
        if (loader != null && !containsString(detail.versionLoaders != null && index < detail.versionLoaders.length
                ? detail.versionLoaders[index] : null, loader)) return false;
        return true;
    }

    private static boolean containsString(String[] array, String value) {
        if (array == null) return false;
        for (String element : array) {
            if (value.equals(element)) return true;
        }
        return false;
    }

    private static void installDependencies(ModpackApi modpackApi, ModDetail modDetail, int selectedVersion,
                                            File gameDir, String subDir, InstanceInfo instanceInfo,
                                            Set<String> visited) throws IOException {
        if (modDetail == null || modDetail.id == null || visited.contains(modDetail.id)) return;
        visited.add(modDetail.id);
        ModDependency[] dependencies = modpackApi.getModDependencies(modDetail, selectedVersion);
        if (dependencies == null) return;

        for (ModDependency dependency : dependencies) {
            if (dependency.type != ModDependency.TYPE_REQUIRED) continue;
            if (dependency.projectId == null || dependency.projectId.isEmpty()) continue;
            if (visited.contains(dependency.projectId)) continue;

            ModDetail dependencyDetail = modpackApi.getModDetails(
                    new ModItem(Constants.SOURCE_MODRINTH, false, dependency.projectId, "", "", ""));
            if (dependencyDetail == null || dependencyDetail.versionUrls == null) continue;

            int dependencyVersion = -1;
            if (dependency.versionId != null && dependencyDetail.versionIds != null) {
                for (int i = 0; i < dependencyDetail.versionIds.length; ++i) {
                    if (dependency.versionId.equals(dependencyDetail.versionIds[i])) {
                        dependencyVersion = i;
                        break;
                    }
                }
            }
            if (dependencyVersion < 0) {
                dependencyVersion = selectBestVersionIndex(dependencyDetail, instanceInfo);
            }
            downloadDependency(modpackApi, dependencyDetail, dependencyVersion, gameDir, subDir, instanceInfo, visited);
        }
    }

    private static void downloadDependency(ModpackApi modpackApi, ModDetail dependencyDetail, int version,
                                           File gameDir, String subDir, InstanceInfo instanceInfo,
                                           Set<String> visited) throws IOException {
        if (dependencyDetail == null || version < 0 || version >= dependencyDetail.versionUrls.length) return;
        String baseName = (dependencyDetail.title == null || dependencyDetail.title.isEmpty()
                ? dependencyDetail.id : dependencyDetail.title.toLowerCase(Locale.ROOT))
                + "-" + dependencyDetail.versionNames[version];
        String fileName = baseName.trim().replaceAll("[\\\\/:*?\"<>| \\t\\n]", "_");
        if (!fileName.endsWith(".jar")) fileName += ".jar";

        File targetFile = new File(gameDir, subDir + "/" + fileName);
        FileUtils.ensureParentDirectory(targetFile);
        // Already installed, just fetch its dependencies
        if (targetFile.exists()) {
            installDependencies(modpackApi, dependencyDetail, version, gameDir, subDir, instanceInfo, visited);
            return;
        }

        byte[] downloadBuffer = new byte[8192];
        try {
            DownloadUtils.ensureSha1(targetFile, dependencyDetail.versionHashes[version], (Callable<Void>) () -> {
                DownloadUtils.downloadFileMonitored(dependencyDetail.versionUrls[version], targetFile, downloadBuffer,
                        new DownloaderProgressWrapper(R.string.modpack_download_downloading_metadata,
                                ProgressLayout.INSTALL_MODPACK
                        )
                );
                return null;
            });
            installDependencies(modpackApi, dependencyDetail, version, gameDir, subDir, instanceInfo, visited);
        } catch (IOException e) {
            targetFile.delete();
            throw e;
        }
    }

    public static ModLoader installModpack(String modpackName, String title, File modpackFile, String icon, InstallFunction installFunction) throws IOException {
        // Build a new minecraft instance, folder first
        ModLoader modLoaderInfo;
        Instance instance = Instances.createInstance(i-> i.name = title, modpackName.substring(0, Math.min(16,modpackName.length())));
        try {
            // Install the modpack
            modLoaderInfo = installFunction.installModpack(modpackFile, instance.getGameDirectory());

            if(modLoaderInfo == null) throw new IOException("Unknown modpack mod loader information");

            if(modLoaderInfo.requiresGuiInstallation()) {
                InstanceInstaller instanceInstaller = modLoaderInfo.createInstaller();
                if(instanceInstaller == null) throw new IOException("Failed to prepare data for instance installation");
                instance.installer = instanceInstaller;
            } else {
                String versionId = modLoaderInfo.installHeadlessly();
                if(versionId == null) throw new IOException("Unknown mod loader version");
                instance.versionId = versionId;
            }
            instance.write();
            ModIconCache.writeInstanceImage(instance, icon);

            Instances.setSelectedInstance(instance);
            if(modLoaderInfo.requiresGuiInstallation()) {
                instance.installer.start();
            }
        } catch (IOException e) {
            Instances.removeInstance(instance);
            throw e;
        } finally {
            modpackFile.delete();
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
        }

        return modLoaderInfo;
    }

    public static ModLoader downloadModpack(ModDetail modDetail, int selectedVersion, InstallFunction installFunction) throws IOException {
        String versionUrl = modDetail.versionUrls[selectedVersion];
        String versionHash = modDetail.versionHashes[selectedVersion];
        String modpackName = (modDetail.title.toLowerCase(Locale.ROOT) + " " + modDetail.versionNames[selectedVersion])
                .trim().replaceAll("[\\\\/:*?\"<>| \\t\\n]", "_" );
        String name = modDetail.title;
        String icon = modDetail.getIconCacheTag();

        if (versionHash != null) {
            modpackName += "_" + versionHash;
        }

        if (modpackName.length() > 255){
            modpackName = modpackName.substring(0,255);
        }

        File modpackFile = new File(Tools.DIR_CACHE, modpackName + ".cf");

        byte[] downloadBuffer = new byte[8192];
        try {
            DownloadUtils.ensureSha1(modpackFile, versionHash, (Callable<Void>) () -> {
                DownloadUtils.downloadFileMonitored(versionUrl, modpackFile, downloadBuffer,
                        new DownloaderProgressWrapper(R.string.modpack_download_downloading_metadata,
                                ProgressLayout.INSTALL_MODPACK
                        )
                );
                return null;
            });
        } catch (IOException e) {
            modpackFile.delete();
            throw e;
        }

        return installModpack(modpackName, name, modpackFile, icon, installFunction);
    }

    public interface InstallFunction {
        ModLoader installModpack(File modpackFile, File instanceDestination) throws IOException;
    }
}
