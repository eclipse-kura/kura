/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _SERIAL_H
#define _SERIAL_H

#ifdef __cplusplus
extern "C" {
#endif


#include "javacall_serial.h"
#include "javacall_logging.h"
#include "javacall_memory.h"
#include "javacall_properties.h"
#include "javautil_circular_buffer.h"

#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>
#include <fcntl.h>
#include <termios.h>
#include <unistd.h>
#include <pthread.h>
#include <poll.h>

#include <stdint.h>             /* Definition of uint64_t */
#include <termios.h>
#include <sys/ioctl.h>

#include <dirent.h>
#include <string.h>

#define SERIAL_BUF_SIZE    0x400
static const char* const SERIAL_BUFFER_SIZE_PROPERTY_NAME = "uart_native_buffer_size";

static const char* const DEFAULT_PREFIX   = "ttyUSB";

typedef void (*asynch_event_cb)(javacall_handle handle, int param, javacall_result res);

typedef struct serial_descr{
    int                      fd;
    javacall_handle          inBuffer;
    pthread_t                inPollThread;
    pthread_mutex_t          lock;
    int                      internal_port; //0 - not, 1 - is

    pthread_t                write_thread;
    pthread_mutex_t          write_lock;
    pthread_cond_t           out_buffer_ready;

    javacall_handle          out_buffer;
    int                      out_buffer_size;
    int                      out_total_written;
    int                      buffer_max_size;
    //file descriptor returned by eventfd() for poll interupting
    int                      event_fd;

    asynch_event_cb buffer_overrun_cb;
    asynch_event_cb new_data_avail_cb;
    asynch_event_cb write_complete_cb;
} SERIAL_DESC, *serial_handle;

#define getTermios(handle, term) (tcgetattr(((serial_handle)handle)->fd, (struct termios*)term)==0?JAVACALL_OK:JAVACALL_FAIL)
#define setTermios(handle, term) (tcsetattr(((serial_handle)handle)->fd, TCSANOW, (struct termios*)term)==0?JAVACALL_OK:JAVACALL_FAIL)

javacall_result jc_serial_init_buffers_threads(serial_handle sHandle);
javacall_result jc_serial_read_common(serial_handle port, unsigned char* buffer,int bufferSize ,/*OUT*/int *bytesRead,/*OUT*/int *bytesAvailable);
javacall_result jc_serial_write_common(serial_handle port,  unsigned char* buffer, int size, int *bytesMoved);

javacall_result jc_serial_open(const char *devName, serial_handle p);
int             baud_to_int(speed_t baud);
speed_t         int_to_baud(int baud);

#ifdef __cplusplus
}
#endif

#endif // _SERIAL_H
