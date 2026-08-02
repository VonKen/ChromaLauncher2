#define _GNU_SOURCE

#include <jni.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <dirent.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/resource.h>
#include <sched.h>

#include <linux/limits.h>

#define THROTTLE_TAG "thermal_throttle"

static cpu_set_t little_cores_set;
static bool little_cores_init;

static bool has_different_frequencies;
static unsigned int total_core_count;

#define FREQ_MAX 256

static void format_cpu_freq_path(char* buffer, unsigned int cpu_core) {
    snprintf(buffer, PATH_MAX, "/sys/devices/system/cpu/cpu%u/cpufreq/cpuinfo_max_freq", cpu_core);
}

static unsigned long read_core_max_freq(unsigned int cpu_core) {
    char path_buffer[PATH_MAX];
    char freq_buffer[FREQ_MAX];
    format_cpu_freq_path(path_buffer, cpu_core);
    int freqfd = open(path_buffer, O_RDONLY);
    if(freqfd == -1) return 0;
    ssize_t read_count = read(freqfd, freq_buffer, FREQ_MAX - 1);
    close(freqfd);
    if(read_count <= 0) return 0;
    freq_buffer[read_count] = 0;
    return strtoul(freq_buffer, NULL, 10);
}

static void init_little_cores() {
    unsigned long min_freq = 0;
    unsigned long max_freq = 0;
    unsigned int corecnt = 0;
    while(1) {
        unsigned long freq = read_core_max_freq(corecnt);
        if(freq == 0) break;
        if(min_freq == 0 || freq < min_freq) min_freq = freq;
        if(freq > max_freq) max_freq = freq;
        corecnt++;
    }
    if(corecnt == 0) {
        printf("%s: no cores detected\n", THROTTLE_TAG);
        return;
    }
    has_different_frequencies = min_freq != max_freq;
    total_core_count = corecnt;

    CPU_ZERO(&little_cores_set);
    if(has_different_frequencies) {
        for(unsigned int i = 0; i < corecnt; i++) {
            if(read_core_max_freq(i) == min_freq) {
                CPU_SET_S(i, CPU_SETSIZE, &little_cores_set);
            }
        }
        printf("%s: little cores set (min freq %lu Hz, %u cores)\n", THROTTLE_TAG, min_freq, corecnt);
    } else {
        // Homogeneous CPU, "little cores" is a no-op: allow every core
        for(unsigned int i = 0; i < corecnt; i++) {
            CPU_SET_S(i, CPU_SETSIZE, &little_cores_set);
        }
        printf("%s: homogeneous CPU, affinity is a no-op\n", THROTTLE_TAG);
    }
    little_cores_init = true;
}

static void set_thread_affinity(pid_t tid, const cpu_set_t* set) {
    if(sched_setaffinity(tid, CPU_SETSIZE, set) != 0) {
        printf("%s: sched_setaffinity(%d) failed: %s\n", THROTTLE_TAG, tid, strerror(errno));
    }
}

static void apply_affinity_to_all_threads(const cpu_set_t* set) {
    DIR* task_dir = opendir("/proc/self/task");
    if(task_dir == NULL) {
        printf("%s: cannot open /proc/self/task: %s\n", THROTTLE_TAG, strerror(errno));
        // Fallback: only the calling thread
        set_thread_affinity(0, set);
        return;
    }
    struct dirent* entry;
    while((entry = readdir(task_dir)) != NULL) {
        if(entry->d_name[0] == '.') continue;
        char* endptr;
        long tid = strtol(entry->d_name, &endptr, 10);
        if(*endptr == '\0' && tid > 0) {
            set_thread_affinity((pid_t) tid, set);
        }
    }
    closedir(task_dir);
}

JNIEXPORT void JNICALL Java_net_kdt_pojavlaunch_utils_ThermalNative_setProcessNice(JNIEnv* env, jclass clazz, jint nice_value) {
    if(nice_value < -20) nice_value = -20;
    if(nice_value > 19) nice_value = 19;
    if(setpriority(PRIO_PROCESS, 0, (int) nice_value) != 0) {
        printf("%s: setpriority failed: %s\n", THROTTLE_TAG, strerror(errno));
    } else {
        printf("%s: process nice set to %d\n", THROTTLE_TAG, (int) nice_value);
    }
}

JNIEXPORT void JNICALL Java_net_kdt_pojavlaunch_utils_ThermalNative_setLittleCoreAffinity(JNIEnv* env, jclass clazz, jboolean enable) {
    if(!little_cores_init) init_little_cores();
    if(!little_cores_init) return;
    if(enable) {
        if(!has_different_frequencies) {
            printf("%s: cannot use little cores on homogeneous CPU\n", THROTTLE_TAG);
            return;
        }
        apply_affinity_to_all_threads(&little_cores_set);
        printf("%s: pinned all threads to little cores\n", THROTTLE_TAG);
    } else {
        cpu_set_t all_cores_set;
        CPU_ZERO(&all_cores_set);
        for(unsigned int i = 0; i < total_core_count; i++) {
            CPU_SET_S(i, CPU_SETSIZE, &all_cores_set);
        }
        apply_affinity_to_all_threads(&all_cores_set);
        printf("%s: restored affinity to all cores\n", THROTTLE_TAG);
    }
}
