package net.kdt.pojavlaunch.utils;

import android.util.Log;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.Tools;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Patches Simple Voice Chat mod jars so that the bundled glibc-only native
 * libraries are replaced with bionic builds that can actually be loaded on
 * Android. The bionic libraries are shipped inside this launcher's jniLibs and
 * are written into the mod jar under the natives/linux-<arch>/ path that
 * de.maxhenkel.nativeutils.LibraryLoader resolves at runtime.
 *
 * librnnoise4j.so is bundled for every supported ABI, while the codec libraries
 * (libopus4j.so, libspeex4j.so, liblame4j.so) are only bundled for 64-bit ARM.
 */
public class SVCNativePatcher {
    private static final String TAG = "SVCNativePatcher";

    private static final String NATIVE_PREFIX = "natives/linux-";
    private static final String RNNOISE4J_CLASS = "de/maxhenkel/rnnoise4j/Denoiser.class";
    private static final String VOICECHAT_PREFIX = "de/maxhenkel/voicechat/";

    private static final String[] NATIVE_LIBS = {
            "librnnoise4j.so",
            "libopus4j.so",
            "libspeex4j.so",
            "liblame4j.so"
    };

    /**
     * Replaces the glibc-only SVC natives inside every Simple Voice Chat mod jar
     * found in the instance's mods directory. Safe to call every launch: jars
     * that are already patched (or contain no SVC classes) are skipped untouched.
     *
     * @param gameDir the instance game directory
     */
    public static void patch(File gameDir) {
        try {
            String arch = archNativeName();
            if (arch == null) {
                Log.i(TAG, "No patched natives for this device architecture");
                return;
            }
            Map<String, byte[]> swapLibs = new LinkedHashMap<>();
            for (String libName : NATIVE_LIBS) {
                if (archForLib(libName, arch) == null) continue;
                byte[] lib = readNativeLib(libName);
                if (lib == null) {
                    Log.w(TAG, "Bundled " + libName + " not found in launcher natives");
                    continue;
                }
                swapLibs.put(NATIVE_PREFIX + arch + "/" + libName, lib);
            }
            if (swapLibs.isEmpty()) {
                Log.i(TAG, "No patched natives bundled for this device architecture");
                return;
            }
            File modsDir = new File(gameDir, "mods");
            File[] mods = modsDir.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
            if (mods == null) return;
            int patched = 0;
            for (File mod : mods) {
                try {
                    if (patchJar(mod, swapLibs)) patched++;
                } catch (IOException e) {
                    Log.w(TAG, "Failed to patch " + mod.getName(), e);
                }
            }
            if (patched > 0) Log.i(TAG, "Patched SVC natives in " + patched + " voice chat mod jar(s)");
        } catch (Throwable t) {
            Log.w(TAG, "Failed to patch voice chat natives", t);
        }
    }

    private static String archNativeName() {
        switch (Architecture.getDeviceArchitecture()) {
            case Architecture.ARCH_ARM64:
                return "aarch64";
            case Architecture.ARCH_X86_64:
                return "x64";
            case Architecture.ARCH_X86:
                return "x86";
            default:
                // 32-bit ARM is not shipped by native-utils with a stable name
                return null;
        }
    }

    /**
     * @return the device arch name usable for the given library, or null if no
     *         bionic build of that library is bundled for this device.
     */
    private static String archForLib(String libName, String deviceArch) {
        if ("librnnoise4j.so".equals(libName)) return deviceArch;
        return "aarch64".equals(deviceArch) ? deviceArch : null;
    }

    private static byte[] readNativeLib(String libName) throws IOException {
        File libFile = new File(Tools.NATIVE_LIB_DIR, libName);
        if (!libFile.isFile()) return null;
        try (InputStream in = new FileInputStream(libFile)) {
            return IOUtils.toByteArray(in);
        }
    }

    /**
     * @return true if the jar was rewritten
     */
    private static boolean patchJar(File mod, Map<String, byte[]> swapLibs) throws IOException {
        try (ZipFile zipFile = new ZipFile(mod)) {
            if (!isVoiceChatJar(zipFile)) return false;
            for (Map.Entry<String, byte[]> swap : swapLibs.entrySet()) {
                ZipEntry existing = zipFile.getEntry(swap.getKey());
                if (existing == null || !entryMatches(zipFile, existing, swap.getValue())) {
                    return rewriteJar(mod, swapLibs);
                }
            }
            return false;
        }
    }

    private static boolean isVoiceChatJar(ZipFile zipFile) {
        if (zipFile.getEntry(RNNOISE4J_CLASS) != null) return true;
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            if (entries.nextElement().getName().startsWith(VOICECHAT_PREFIX)) return true;
        }
        return false;
    }

    private static boolean entryMatches(ZipFile zipFile, ZipEntry entry, byte[] expected) throws IOException {
        if (entry.getSize() != expected.length) return false;
        byte[] buf = new byte[65536];
        int offset = 0;
        try (InputStream in = zipFile.getInputStream(entry)) {
            int read;
            while ((read = in.read(buf, 0, Math.min(buf.length, expected.length - offset))) > 0) {
                for (int i = 0; i < read; i++) {
                    if (buf[i] != expected[offset + i]) return false;
                }
                offset += read;
                if (offset == expected.length) break;
            }
        }
        return offset == expected.length;
    }

    private static boolean rewriteJar(File mod, Map<String, byte[]> swapLibs) throws IOException {
        File tmp = new File(mod.getParentFile(), mod.getName() + ".svcpatch.tmp");
        try (ZipFile zipFile = new ZipFile(mod);
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmp))) {
            byte[] buf = new byte[65536];
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (swapLibs.containsKey(entry.getName())) continue;
                ZipEntry outEntry = new ZipEntry(entry.getName());
                outEntry.setTime(entry.getTime());
                zos.putNextEntry(outEntry);
                if (!entry.isDirectory()) {
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        int read;
                        while ((read = in.read(buf)) > 0) zos.write(buf, 0, read);
                    }
                }
                zos.closeEntry();
            }
            for (Map.Entry<String, byte[]> swap : swapLibs.entrySet()) {
                ZipEntry libEntry = new ZipEntry(swap.getKey());
                libEntry.setTime(System.currentTimeMillis());
                zos.putNextEntry(libEntry);
                zos.write(swap.getValue());
                zos.closeEntry();
            }
        }
        replaceFile(tmp, mod);
        return true;
    }

    private static void replaceFile(File tmp, File target) throws IOException {
        File backup = new File(target.getParentFile(), target.getName() + ".svcbak");
        if (backup.exists() && !backup.delete()) backup.deleteOnExit();
        if (!target.renameTo(backup)) {
            if (!tmp.renameTo(target)) {
                copyFile(tmp, target);
                tmp.delete();
            }
            return;
        }
        if (!tmp.renameTo(target)) {
            if (!backup.renameTo(target)) {
                throw new IOException("Failed to restore " + target.getName() + " after patching");
            }
            throw new IOException("Failed to move patched jar over " + target.getName());
        }
    }

    private static void copyFile(File from, File to) throws IOException {
        try (InputStream in = new FileInputStream(from);
             FileOutputStream out = new FileOutputStream(to)) {
            IOUtils.copy(in, out);
        }
    }
}
