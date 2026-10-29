//
// Created by maks on 05.01.2025.
// Reworked for Dream Displays compatibility.
//

#include "jvm_hooks.h"
#include <dlfcn.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <errno.h>
#include <limits.h>
#include <sys/stat.h>
#include <unistd.h>

#include "utils.h"

extern char **environ;

static jint (*orig_ProcessImpl_forkAndExec)(JNIEnv *env, jobject process, jint mode, jbyteArray helperpath, jbyteArray prog, jbyteArray argBlock, jint argc, jbyteArray envBlock, jint envc, jbyteArray dir, jintArray std_fds, jboolean redirectErrorStream);

/* ------------------------------------------------------------------ */
/* Small helpers                                                       */
/* ------------------------------------------------------------------ */

// A list of NUL-terminated strings packed back to back (the format Java
// uses for both argBlock and envBlock).
typedef struct {
    char *data;
    size_t len;
    size_t cap;
    jint count;
} PackedList;

static bool packedAppend(PackedList *l, const char *s, size_t n) {
    size_t need = l->len + n + 1;
    if (need > l->cap) {
        size_t ncap = l->cap ? l->cap : 256;
        while (ncap < need) ncap *= 2;
        char *nd = realloc(l->data, ncap);
        if (!nd) return false;
        l->data = nd;
        l->cap = ncap;
    }
    memcpy(l->data + l->len, s, n);
    l->data[l->len + n] = '\0';
    l->len += n + 1;
    l->count++;
    return true;
}

// Copy a Java byte[] into a malloc'd, always NUL-terminated buffer.
static char *readBytes(JNIEnv *env, jbyteArray arr, jsize *len_out) {
    if (arr == NULL) return NULL;
    jsize len = (*env)->GetArrayLength(env, arr);
    char *buf = malloc((size_t)len + 1);
    if (!buf) return NULL;
    (*env)->GetByteArrayRegion(env, arr, 0, len, (jbyte *)buf);
    buf[len] = '\0';
    if (len_out) *len_out = len;
    return buf;
}

static jbyteArray toJByteArray(JNIEnv *env, const char *data, size_t len) {
    jbyteArray result = (*env)->NewByteArray(env, (jsize)len);
    if (result == NULL) return NULL;
    (*env)->SetByteArrayRegion(env, result, 0, (jsize)len, (const jbyte *)data);
    return result;
}

// Turn a C string into a NUL-terminated Java byte array (what ProcessImpl expects for `prog`).
static jbyteArray stringToBytes(JNIEnv *env, const char *string) {
    return toJByteArray(env, string, strlen(string) + 1);
}

// Non-destructive dirname. The libc dirname() may modify its argument, and the
// old code passed it the string returned by getenv(), which could corrupt
// POJAV_FFMPEG_PATH so that the 2nd+ ffmpeg launch tried to exec a directory
// (which fails with "Permission denied").
static char *dupDirname(const char *path) {
    const char *slash = strrchr(path, '/');
    if (!slash) return strdup(".");
    if (slash == path) return strdup("/");
    return strndup(path, (size_t)(slash - path));
}

// Matches "ffmpeg", "ffmpeg.exe", "ffmpeg-8.1", "ffmpeg_linux64"... but not "ffmpegthumbnailer".
static bool matchesTool(const char *base, const char *tool) {
    size_t n = strlen(tool);
    if (strncasecmp(base, tool, n) != 0) return false;
    char next = base[n];
    return next == '\0' || next == '.' || next == '-' || next == '_';
}

/* ------------------------------------------------------------------ */
/* Environment handling                                                */
/* ------------------------------------------------------------------ */

static bool keepOrRecordEnv(PackedList *list, const char *entry, size_t n, const char **origPath) {
    if (strncmp(entry, "PATH=", 5) == 0) {
        *origPath = entry + 5;
        return true;
    }
    // Drop the launcher's library overrides: they break the plugin's ffmpeg dependencies.
    if (strncmp(entry, "LD_LIBRARY_PATH=", 16) == 0) return true;
    if (strncmp(entry, "LD_PRELOAD=", 11) == 0) return true;
    return packedAppend(list, entry, n);
}

// Build a new env block that PRESERVES the original environment (HOME, TMPDIR, proxy vars, ...)
// and only overrides LD_LIBRARY_PATH / PATH. When Java passes a NULL block it means
// "inherit the JVM's environment", so in that case we start from environ.
static bool buildToolEnv(JNIEnv *env, jbyteArray oldBlock, jint oldCount, const char *dir,
                         jbyteArray *outBlock, jint *outCount) {
    PackedList list = {0};
    char *oldData = NULL;
    jsize oldLen = 0;
    const char *origPath = NULL;
    bool ok = true;

    if (oldBlock != NULL) {
        oldData = readBytes(env, oldBlock, &oldLen);
        if (!oldData) return false;
        const char *p = oldData, *end = oldData + oldLen;
        for (jint i = 0; i < oldCount && p < end && ok; i++) {
            size_t n = strnlen(p, (size_t)(end - p));
            ok = keepOrRecordEnv(&list, p, n, &origPath);
            p += n + 1;
        }
    } else {
        for (char **e = environ; e && *e && ok; e++) {
            ok = keepOrRecordEnv(&list, *e, strlen(*e), &origPath);
        }
    }

    if (ok) {
        size_t cap = strlen(dir) + (origPath ? strlen(origPath) : 0) + 32;
        char *tmp = malloc(cap);
        if (!tmp) {
            ok = false;
        } else {
            snprintf(tmp, cap, "LD_LIBRARY_PATH=%s", dir);
            ok = packedAppend(&list, tmp, strlen(tmp));
            if (ok) {
                if (origPath && *origPath) snprintf(tmp, cap, "PATH=%s:%s", dir, origPath);
                else snprintf(tmp, cap, "PATH=%s", dir);
                ok = packedAppend(&list, tmp, strlen(tmp));
            }
            free(tmp);
        }
    }

    if (ok) {
        *outBlock = toJByteArray(env, list.data, list.len);
        *outCount = list.count;
        ok = (*outBlock != NULL);
    }
    free(list.data);
    free(oldData);
    return ok;
}

/* ------------------------------------------------------------------ */
/* Argument rewriting (for yt-dlp style "--ffmpeg-location <path>")    */
/* ------------------------------------------------------------------ */

// Tools that receive the ffmpeg location as an argument never run a program called
// "ffmpeg" themselves, so the program-name check alone would miss them.
// Returns true and fills *out only if something was actually rewritten.
static bool rewriteFfmpegLocationArgs(JNIEnv *env, jbyteArray argBlock, jint argc,
                                      const char *ffmpegPath, jbyteArray *out) {
    if (argBlock == NULL || argc <= 0) return false;
    jsize len = 0;
    char *data = readBytes(env, argBlock, &len);
    if (!data) return false;

    PackedList list = {0};
    bool ok = true, changed = false, replaceNext = false;
    const char *p = data, *end = data + len;

    for (jint i = 0; i < argc && p < end && ok; i++) {
        size_t n = strnlen(p, (size_t)(end - p));
        if (replaceNext) {
            ok = packedAppend(&list, ffmpegPath, strlen(ffmpegPath));
            replaceNext = false;
            changed = true;
        } else if (n == 17 && memcmp(p, "--ffmpeg-location", 17) == 0) {
            ok = packedAppend(&list, p, n);
            replaceNext = true;
        } else if (n > 18 && strncmp(p, "--ffmpeg-location=", 18) == 0) {
            size_t cap = strlen(ffmpegPath) + 32;
            char *tmp = malloc(cap);
            if (!tmp) { ok = false; }
            else {
                snprintf(tmp, cap, "--ffmpeg-location=%s", ffmpegPath);
                ok = packedAppend(&list, tmp, strlen(tmp));
                free(tmp);
            }
            changed = true;
        } else {
            ok = packedAppend(&list, p, n);
        }
        p += n + 1;
    }

    bool result = false;
    if (ok && changed && list.count == argc) {
        *out = toJByteArray(env, list.data, list.len);
        result = (*out != NULL);
    }
    free(list.data);
    free(data);
    return result;
}


/* ------------------------------------------------------------------ */
/* Dream Displays: skip its ffmpeg download                            */
/* ------------------------------------------------------------------ */

// Dream Displays' FFmpegBinary.kt looks for ./dreamdisplays/ffmpeg/<platform>/ffmpeg
// (relative to the JVM working directory). If that file exists, is non-empty and
// canExecute(), it uses it and never downloads the ~150 MB desktop BtbN build that
// Android can't run anyway. Every spawn named "ffmpeg" is redirected by the hook below,
// so a tiny executable placeholder is enough.
static void ensureFfmpegPlaceholders(void) {
    if (getenv("POJAV_FFMPEG_PATH") == NULL) return;
    char cwd[PATH_MAX];
    if (getcwd(cwd, sizeof(cwd)) == NULL) return;

    static const char *keys[] = {"linux-aarch64", "linux-x64"};
    char path[PATH_MAX];
    snprintf(path, sizeof(path), "%s/dreamdisplays", cwd);
    mkdir(path, 0755);
    snprintf(path, sizeof(path), "%s/dreamdisplays/ffmpeg", cwd);
    mkdir(path, 0755);

    for (size_t i = 0; i < sizeof(keys) / sizeof(keys[0]); i++) {
        snprintf(path, sizeof(path), "%s/dreamdisplays/ffmpeg/%s", cwd, keys[i]);
        if (mkdir(path, 0755) != 0 && errno != EEXIST) continue;
        snprintf(path, sizeof(path), "%s/dreamdisplays/ffmpeg/%s/ffmpeg", cwd, keys[i]);
        struct stat st;
        if (stat(path, &st) == 0 && st.st_size > 0) continue; // already there, leave it alone
        FILE *f = fopen(path, "w");
        if (!f) continue;
        fputs("#!/bin/sh\n# placeholder: launched processes named ffmpeg are redirected by exec_hooks\n", f);
        fclose(f);
        chmod(path, 0755);
    }
}

/* ------------------------------------------------------------------ */
/* The hook                                                            */
/* ------------------------------------------------------------------ */

/**
 * Hooked version of java.lang.UNIXProcess/ProcessImpl.forkAndExec()
 * which is used to handle the "open" command and "ffmpeg"/"ffprobe" invocations.
 */
static jint hooked_ProcessImpl_forkAndExec(JNIEnv *env, jobject process, jint mode, jbyteArray helperpath, jbyteArray prog, jbyteArray argBlock, jint argc, jbyteArray envBlock, jint envc, jbyteArray dir, jintArray std_fds, jboolean redirectErrorStream) {
    char *progStr = readBytes(env, prog, NULL);
    if (progStr == NULL) {
        return orig_ProcessImpl_forkAndExec(env, process, mode, helperpath, prog, argBlock, argc, envBlock, envc, dir, std_fds, redirectErrorStream);
    }
    const char *base = strrchr(progStr, '/');
    base = base ? base + 1 : progStr;

    if (strcmp(base, "xdg-open") == 0) {
        // When invoking xdg-open, send the open URL into Android
        free(progStr);
        char *url = readBytes(env, argBlock, NULL);
        if (url) {
            openLink(url);
            free(url);
        }
        return 0;
    }

    // Take private copies: getenv() memory must not be modified or kept across threads.
    const char *e1 = getenv("POJAV_FFMPEG_PATH");
    const char *e2 = getenv("POJAV_FFPROBE_PATH");
    char *ffmpegPath = e1 ? strdup(e1) : NULL;
    char *ffprobePath = e2 ? strdup(e2) : NULL;

    // 1) Programs that take the ffmpeg location as an argument (yt-dlp etc.)
    jbyteArray newArgs = NULL;
    if (ffmpegPath && rewriteFfmpegLocationArgs(env, argBlock, argc, ffmpegPath, &newArgs)) {
        argBlock = newArgs;
        printf("exec_hooks: rewrote --ffmpeg-location for %s -> %s\n", base, ffmpegPath);
    }

    // 2) Direct invocations of ffmpeg / ffprobe.
    // Android won't let us execute arbitrary files, only lib*.so files from the native
    // library dir, so replace the program with the one shipped by the plugin.
    const char *replacement = NULL;
    if (matchesTool(base, "ffmpeg")) replacement = ffmpegPath;
    else if (matchesTool(base, "ffprobe")) replacement = ffprobePath;

    if (replacement != NULL) {
        char *libDir = dupDirname(replacement);
        jbyteArray newEnv = NULL;
        jint newEnvc = 0;
        jbyteArray newProg = stringToBytes(env, replacement);

        if (libDir && newProg && buildToolEnv(env, envBlock, envc, libDir, &newEnv, &newEnvc)) {
            printf("exec_hooks: redirecting '%s' -> '%s'\n", progStr, replacement);
            prog = newProg;
            envBlock = newEnv;
            envc = newEnvc;
        } else {
            printf("exec_hooks WARN: failed to prepare redirect for '%s', running original\n", progStr);
            if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);
        }
        free(libDir);
    }

    free(progStr);
    free(ffmpegPath);
    free(ffprobePath);
    return orig_ProcessImpl_forkAndExec(env, process, mode, helperpath, prog, argBlock, argc, envBlock, envc, dir, std_fds, redirectErrorStream);
}

// Hook the forkAndExec method in the Java runtime for custom executable overriding.
void hookExec(JNIEnv *env) {
    void *libjava = dlopen("libjava.so", RTLD_NOLOAD);
    if (libjava == NULL) {
        printf("Failed to register forkAndExec: libjava not found\n");
        return;
    }
    jclass hookClass;
    orig_ProcessImpl_forkAndExec = dlsym(libjava, "Java_java_lang_UNIXProcess_forkAndExec");
    if (!orig_ProcessImpl_forkAndExec) {
        orig_ProcessImpl_forkAndExec = dlsym(libjava, "Java_java_lang_ProcessImpl_forkAndExec");
        hookClass = (*env)->FindClass(env, "java/lang/ProcessImpl");
    } else {
        hookClass = (*env)->FindClass(env, "java/lang/UNIXProcess");
    }
    if (hookClass == NULL || orig_ProcessImpl_forkAndExec == NULL) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return;
    }
    JNINativeMethod methods[] = {
            {"forkAndExec", "(I[B[B[BI[BI[B[IZ)I", (void *)&hooked_ProcessImpl_forkAndExec}
    };
    (*env)->RegisterNatives(env, hookClass, methods, 1);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    } else {
        printf("Registered forkAndExec [exec_hooks v2 dream-displays]\n");
        const char *ff = getenv("POJAV_FFMPEG_PATH");
        if (ff == NULL) {
            printf("exec_hooks WARN: POJAV_FFMPEG_PATH is not set, ffmpeg launches will NOT be redirected\n");
        } else {
            printf("exec_hooks: POJAV_FFMPEG_PATH=%s (exists: %s)\n", ff, access(ff, X_OK) == 0 ? "yes, executable" : "NO or not executable");
        }
        ensureFfmpegPlaceholders();
    }
}
