/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifdef __cplusplus
extern "C" {
#endif

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>  /* symbolic names of errors */
#include <pthread.h>

#include "javacall_memory.h"
#include "javacall_os.h"

#if ENABLE_FLUSH_ICACHE
#if TARGET_CPU_ARM
void arm_flush_icache_eabi(void *start, void *end); // same API as __clear_cache in GCC
#endif
#endif

extern void javacall_logging_initialize(void);
/*
 * Initialize the OS structure.
 * This is where timers and threads get started for the first
 * real_time_tick event, and where signal handlers and other I/O
 * initialization should occur.
 *
*/
void javacall_os_initialize(void){
    javacall_logging_initialize();
    return;
}


/*
 * Performs a clean-up of all threads and other OS related activity
 * to allow for a clean and complete restart.  This should undo
 * all the work that initialize does.
 */
void javacall_os_dispose(){
    return;
}

/**
 * Exits calling OS process the VM is running in.
 * @param status the exit status aligned with POSIX systems convention
 */
void javacall_os_exit(int status){
    exit(status);
}

/**
 * javacall_os_flush_icache is used, for example, to flush any caches used by a
 * code segment that is deoptimized or moved during a garbage collection.
 * flush at least [address, address + size] (may flush complete icache).
 *
 * @param address   Start address to flush
 * @param size      Size to flush
 * @retval JAVACALL_OK in case of success
 * @retval JAVACALL_FAIL in case of an error
 */
javacall_result javacall_os_flush_icache(unsigned char* address, int size) {
#if ENABLE_FLUSH_ICACHE
#if TARGET_CPU_ARM
    arm_flush_icache_eabi(address, address+size);
#endif
/*
    #if ARM_EXECUTABLE
#if ENABLE_BRUTE_FORCE_ICACHE_FLUSH
  // This is a brute-force way of flushing the icache. The function
  // brute_flush_icache() contains 64KB of no-ops.
  volatile int x;
  volatile int * ptr = (int*)brute_force_flush_icache;
  for (int i=0; i<8192; i++) {
    // flush writeback cache, too.
    x = *ptr++;
  }
  brute_force_flush_icache(); // IMPL_NOTE: jump to brute_flush_icache() + xx
                              // if size is small ...
#elif defined(__NetBSD__)
  // arch-specific syscall from libarm
  arm_sync_icache((unsigned int)start, size);
#else
  // This is in assembly language
  arm_flush_icache(start, size);
#endif
#endif // ARM_EXECUTABLE

  // Valgrind deploys VM technology with JITter so when after modify ourselves
  // let Valgrind know about it
#if ENABLE_VALGRIND
  VALGRIND_DISCARD_TRANSLATIONS(start, size);
#endif
*/
#endif
    return JAVACALL_OK;
}

/**
 * Returns a handle that uniquely identifies the current thread.
 * @return current thread handle
 */
javacall_handle javacall_os_thread_self() {
  return (javacall_handle)pthread_self();
}

/* Internal structure of a mutex */
struct _javacall_mutex {
    pthread_mutex_t mutex;
};

/* Internal structure of a condition variable */
struct _javacall_cond {
    pthread_cond_t condvar;
    struct _javacall_mutex *mutex;
};

/* Debug stuff */
#ifndef NDEBUG
#define PRINT_ERROR(func_,text_,code_)    \
    fprintf(stderr, \
        "%s: %s: error=%s (#%d)\n", \
        __FUNCTION__, #func_, text_, code_)

#define REPORT_ERROR(func_)   do {\
    PRINT_ERROR(func_,err2str(err),err); \
} while (0)

static char *err2str(int i);

#else
#define PRINT_ERROR(func_,text_,code_)
#define REPORT_ERROR(func_)
#endif

/* creates a POSIX mutex */
javacall_mutex javacall_os_mutex_create() {
    struct _javacall_mutex *m = javacall_malloc(sizeof *m);
    int err;

    if (m == NULL) {
        PRINT_ERROR(javacall_malloc, "No memory", 0);
        return NULL;
    }
    if ((err = pthread_mutex_init(&m->mutex, NULL)) != 0) {
        REPORT_ERROR(pthread_mutex_init);
        javacall_free(m);
        return NULL;
    }
    return m;
}

/* destroys the mutex */
void javacall_os_mutex_destroy(struct _javacall_mutex *m) {
    int err;

    if (m == NULL) {
      PRINT_ERROR(javacall_os_mutex_destroy, "Null mutex", 0);
      return;
    }
    if ((err = pthread_mutex_destroy(&m->mutex)) != 0) {
        REPORT_ERROR(pthread_mutex_destroy);
    } else {
        javacall_free(m);
    }
}

/* locks the mutex */
javacall_result javacall_os_mutex_lock(struct _javacall_mutex *m) {
    int err;

    if (m == NULL) {
      PRINT_ERROR(javacall_os_mutex_lock, "Null mutex", 0);
      return JAVACALL_FAIL;
    }

    if ((err = pthread_mutex_lock(&m->mutex)) != 0) {
        REPORT_ERROR(pthread_mutex_lock);
        return JAVACALL_FAIL;
    }
    return JAVACALL_OK;
}

/* tries to lock the mutex */
javacall_result javacall_os_mutex_try_lock(struct _javacall_mutex *m) {
    int err;

    if (m == NULL) {
      PRINT_ERROR(javacall_os_mutex_try_lock, "Null mutex", 0);
      return JAVACALL_FAIL;
    }

    if ((err = pthread_mutex_trylock(&m->mutex)) != 0 && err != EBUSY) {
        REPORT_ERROR(pthread_mutex_trylock);
        return JAVACALL_FAIL;
    }
    return err == EBUSY ? JAVACALL_WOULD_BLOCK : JAVACALL_OK;
}

/* unlocks the mutex */
javacall_result javacall_os_mutex_unlock(struct _javacall_mutex *m) {
    int err;

    if (m == NULL) {
      PRINT_ERROR(javacall_os_mutex_unlock, "Null mutex", 0);
      return JAVACALL_FAIL;
    }

    if ((err = pthread_mutex_unlock(&m->mutex)) != 0) {
        REPORT_ERROR(pthread_mutex_lock);
    }
    return err;
}

/* creates a POSIX condvar */
javacall_cond javacall_os_cond_create(struct _javacall_mutex *m) {
    struct _javacall_cond *c = javacall_malloc(sizeof *c);
    int err;

    if (c == NULL) {
        PRINT_ERROR(javacall_os_cond_create, "No memory", 0);
        return NULL;
    }
    if (m == NULL) {
      PRINT_ERROR(javacall_os_cond_create, "Null mutex", 0);
      javacall_free(c);
      return NULL;
    }

    if ((err = pthread_cond_init(&c->condvar, NULL)) != 0) {
        REPORT_ERROR(pthread_cond_init);
        javacall_free(c);
        return NULL;
    }
    c->mutex = m;
    return c;
}

/* just returns the saved mutex */
javacall_mutex javacall_os_cond_get_mutex(struct _javacall_cond *c) {
    return c->mutex;
}

/* destroys the condvar */
void javacall_os_cond_destroy(struct _javacall_cond *c) {
    int err;

    if (c == NULL) {
      PRINT_ERROR(javacall_os_cond_destroy, "Null mutex", 0);
      return;
    }

    if ((err = pthread_cond_destroy(&c->condvar)) != 0) {
        REPORT_ERROR(pthread_cond_destroy);
    } else {
        javacall_free(c);
    }
}

/* waits for condition. */
javacall_result javacall_os_cond_wait(struct _javacall_cond *c, long millis) {
    int err;
/* denominators */
#define milli_denom   ((long long)1000)
#define micro_denom   (milli_denom * milli_denom)
#define nano_denom    (milli_denom * milli_denom * milli_denom)

    if (c == NULL) {
      PRINT_ERROR(javacall_os_cond_wait, "Null cond", 0);
      return JAVACALL_FAIL;
    }

    if (millis == 0) {
        err = pthread_cond_wait(&c->condvar, &c->mutex->mutex);
    } else {
        struct timespec ts;

        /*
         * pthread_cond_timedwait() receives the absolute time, so
         * it is nessesary to get current time and add our millis
         */
        err = clock_gettime(CLOCK_REALTIME, &ts);
        if (err != 0) {
            REPORT_ERROR(clock_gettime);
            return JAVACALL_FAIL;
        }
        if (ts.tv_sec > 0) {
          PRINT_ERROR(javacall_os_cond_wait, "Invalid time", 0);
          return JAVACALL_FAIL;
        }

        /* calculate the time of deadline */
        ts.tv_sec += millis / milli_denom;
        ts.tv_nsec += (millis % milli_denom) * nano_denom / milli_denom;
        if (ts.tv_nsec > nano_denom) {
            ts.tv_sec += (time_t) ts.tv_nsec / nano_denom;
            ts.tv_nsec %= nano_denom;
        }
        err = pthread_cond_timedwait(&c->condvar, &c->mutex->mutex, &ts);
    }
    if (err == ETIMEDOUT) {
        return JAVACALL_TIMEOUT;
    }
    if (err != 0) {
        REPORT_ERROR(pthread_cond_XXXwait);
        return JAVACALL_FAIL;
    }
    return JAVACALL_OK;
#undef nano_denom
#undef micro_denom
#undef milli_denom
}

/* wakes up a thread that is waiting for the condition */
javacall_result javacall_os_cond_signal(struct _javacall_cond *c) {
    int err;

    if (c == NULL) {
      PRINT_ERROR(javacall_os_cond_signal, "Null cond", 0);
      return JAVACALL_FAIL;
    }

    if ((err = pthread_cond_signal(&c->condvar)) != 0) {
        REPORT_ERROR(pthread_cond_signal);
        return JAVACALL_FAIL;
    }
    return JAVACALL_OK;
}

/* wakes up all threads that are waiting for the condition */
javacall_result javacall_os_cond_broadcast(struct _javacall_cond *c) {
    int err;

    if (c == NULL) {
      PRINT_ERROR(javacall_os_cond_broadcast, "Null cond", 0);
      return JAVACALL_FAIL;
    }

    if ((err = pthread_cond_broadcast(&c->condvar)) != 0) {
        return JAVACALL_FAIL;
    }
    return JAVACALL_OK;
}

/**
 * Gets directory path path where placed fonts after installed from jar
 * @param fontPath OUT: pointer to unicode buffer, allocated by the VM,
          to be filled with the directory path of fonts
 * @param fontDirLen IN: lenght of max fontPath buffer, OUT: lenght of set fontPath
 * @return <tt>JAVACALL_OK</tt> if operation completed successfully
 *         <tt>JAVACALL_FAIL</tt> if an error occurred
 */
javacall_result javacall_os_get_font_storage_path(
        javacall_utf16* /*OUT*/ fontPath, int* /*IN|OUT*/ fontPathLen) {

    return JAVACALL_FAIL;
}

#ifndef NDEBUG

/* gets error's description */
#define CODE2STR(code_) \
    case code_:\
        return #code_;

static char *err2str(int i) {
    switch (i) {
        CODE2STR(EBUSY)
        CODE2STR(EINVAL)
        CODE2STR(EAGAIN)
        CODE2STR(EDEADLK)
        CODE2STR(EPERM)
        // CODE2STR(EOWNERDEAD)
        // CODE2STR(ENOTRECOVERABLE)
        CODE2STR(ENOMEM)
        CODE2STR(ETIMEDOUT)
        CODE2STR(EINTR)
    default:
        return "unknown";
    }
}

#endif

#ifdef __cplusplus
}
#endif


