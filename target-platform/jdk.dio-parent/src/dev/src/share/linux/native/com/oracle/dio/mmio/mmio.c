/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "javacall_mmio.h"
#include "javacall_logging.h"
#include "javacall_memory.h"

#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include "errno.h"

#define BLOCK_LEN 0x01000000
#define BASE_ADDR 0x20000000
//#define VIRT_ADDR 0x7e000000

/* /dev/mem handle */
static int mem_fd = 0;
/* /dev/mem handle user count */
static int open_count = 0;


javacall_result check_addr_len(const int addr, const int len) {
    if (addr > BASE_ADDR + BLOCK_LEN || addr + len > BASE_ADDR + BLOCK_LEN ||
        addr < BASE_ADDR || addr + len < BASE_ADDR) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Invalid MMIO address %d\n", addr);
        return JAVACALL_FAIL;
    }

    return JAVACALL_OK;
}


/* see javacall_mmio.h for description */
javacall_result javacall_mmio_open(const javacall_uint8* addr, const javacall_int32 size,
                                   /*out*/javacall_handle* const handle,
                                   /*out*/javacall_uint8** const mappedAddr) {
    void* result;
    long page_size = getpagesize();
    void* aligned_addr;
    size_t aligned_size;

    if (JAVACALL_OK != check_addr_len((int)addr, size)) {
        return JAVACALL_INVALID_ARGUMENT;
    }

    /* open /dev/mem */
    if (0 == mem_fd && (mem_fd = open("/dev/mem", O_RDWR|O_SYNC) ) < 0) {
        mem_fd = 0;
        JAVACALL_REPORT_ERROR(JC_DIO, "cannot open /dev/mem \n");
        return JAVACALL_FAIL;
    }

    aligned_addr = (void*)(((long int)addr / page_size) * page_size);

    aligned_size = size + ((unsigned int)addr - (unsigned int)aligned_addr);

    result = (void*)mmap(
                  NULL,
                  aligned_size,
                  PROT_READ|PROT_WRITE,
                  MAP_SHARED,
                  mem_fd,
                  (off_t)aligned_addr
              );

    if (result == (void*)-1) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "cannot remap: \n", errno);
        close(mem_fd);
        return JAVACALL_FAIL;
    }

    open_count++;

    *mappedAddr = (javacall_uint8*)(result + ((javacall_uint32)addr - (javacall_uint32)aligned_addr));

    {
      javacall_int32* tmp = (javacall_int32*)javacall_malloc(sizeof(javacall_int32)*2);
      tmp[0] = (javacall_int32)result;
      tmp[1] = (javacall_int32)aligned_size;
      *handle = (javacall_handle)tmp;
    }
    return JAVACALL_OK;
}


/* see javacall_mmio.h for description */
javacall_result javacall_mmio_close(const javacall_handle handle) {
    javacall_result result = JAVACALL_OK;
    javacall_int32* tmp = (javacall_int32*)handle;
    if (0 != munmap((void*)tmp[0], (size_t)tmp[1])) {
        JAVACALL_REPORT_ERROR(JC_DIO, "cannot release maped memory");
        result = JAVACALL_FAIL;
    }

    javacall_free((void*)handle);

    if (0 != mem_fd) {
        open_count--;
        if (0 == open_count) {
            close(mem_fd);
            mem_fd = 0;
        }
    }

    return result;
}

/* see javacall_mmio.h for description */
javacall_result javacall_mmio_start_listening_with_buffer(
        const javacall_handle handle,
        const javacall_uint32 offset,
        const javacall_uint32 event_id,
        javacall_uint8* const buffer,
        const javacall_uint32 bufferLength){
    (void)handle;
    (void)offset;
    (void)event_id;
    (void)buffer;
    (void)bufferLength;
    return JAVACALL_FAIL;
}

/* see javacall_mmio.h for description */
javacall_result javacall_mmio_stop_listening(const javacall_handle handle, const javacall_uint32 event_id){
    (void)handle;
    (void)event_id;
    return JAVACALL_OK;
}

