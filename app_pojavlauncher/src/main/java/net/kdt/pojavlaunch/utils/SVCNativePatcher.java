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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Patches Simple Voice Chat mod jars so that the bundled RNNoise native library
 * (a glibc-only build) is replaced with a bionic build that can actually be loaded
 * on Android. The native library is shipped inside this launcher's jniLibs and
 * is written into the mod jar under the natives/linux-<arch>/ path that
 * de.maxhenkel.nativeutils.LibraryLoader resolves at runtime.
 */
public class SVCNativePatcher {
    private static final String TAG = "SVCNativePatcher";

    private static final String LIB_NAME = "librnnoise4j.so";
    private static final String NATIVE_PREFIX = "natives/linux-";
    private static final String RNNOISE4J_CLASS = "de/maxhenkel/rnnoise4j/Denoiser.class";
    private static final String VOICECHAT_PREFIX = "de/maxhenkel/voicechat/";

    /**
     * Replaces the RNNoise native inside every Simple Voice Chat mod jar found in
     * the instance's mods directory. Safe to call every launch: jars that are
     * already patched (or contain no SVC classes) are skipped untouched.
     *
     * @param gameDir the instance game directory
     */
    public static void patch(File gameDir) {
        try {
            String arch = archNativeName();
            if (arch == null) {
                Log.i(TAG, "No patched native for this device architecture");
                return;
            }
            byte[] nativeLib = readNativeLib();
            if (nativeLib == null) {
                Log.w(TAG, "Bundled librnnoise4j.so not found in launcher natives");
                return;
            }
            File modsDir = new File(gameDir, "mods");
            File[] mods = modsDir.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
            if (mods == null) return;
            String targetEntry = NATIVE_PREFIX + arch + "/" + LIB_NAME;
            int patched = 0;
            for (File mod : mods) {
                try {
                    if (patchJar(mod, targetEntry, nativeLib)) patched++;
                } catch (IOException e) {
                    Log.w(TAG, "Failed to patch " + mod.getName(), e);
                }
            }
            if (patched > 0) Log.i(TAG, "Patched RNNoise natives in " + patched + " voice chat mod jar(s)");
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

    private static byte[] readNativeLib() throws IOException {
        File libFile = new File(Tools.NATIVE_LIB_DIR, LIB_NAME);
        if (!libFile.isFile()) return null;
        try (InputStream in = new FileInputStream(libFile)) {
            return IOUtils.toByteArray(in);
        }
    }

    /**
     * @return true if the jar was rewritten
     */
    private static boolean patchJar(File mod, String targetEntry, byte[] nativeLib) throws IOException {
        try (ZipFile zipFile = new ZipFile(mod)) {
            if (!isVoiceChatJar(zipFile)) return false;
            ZipEntry existing = zipFile.getEntry(targetEntry);
            if (existing != null && entryMatches(zipFile, existing, nativeLib)) return false;
        }
        rewriteJar(mod, targetEntry, nativeLib);
        return true;
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

    private static void rewriteJar(File mod, String targetEntry, byte[] nativeLib) throws IOException {
        File tmp = new File(mod.getParentFile(), mod.getName() + ".svcpatch.tmp");
        try (ZipFile zipFile = new ZipFile(mod);
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmp))) {
            byte[] buf = new byte[65536];
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals(targetEntry)) continue;
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
            ZipEntry libEntry = new ZipEntry(targetEntry);
            libEntry.setTime(System.currentTimeMillis());
            zos.putNextEntry(libEntry);
            zos.write(nativeLib);
            zos.closeEntry();
        }
        replaceFile(tmp, mod);
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
