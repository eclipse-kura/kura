/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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


#include "javautil_circular_buffer.h"
#include "javacall_platform_defs.h"
#include "javacall_memory.h"

#define CIRCULAR_BUFFER_NEXT_SHIFT(n, shift, num)   ((n + shift) % num)
#define CIRCULAR_BUFFER_NEXT(n, num)          (CIRCULAR_BUFFER_NEXT_SHIFT(n, 1, num))
#define IS_CIRCULAR_BUFFER_FULL(wIndex, rIndex, num) (CIRCULAR_BUFFER_NEXT(wIndex, num) == rIndex)
#define IS_CIRCULAR_BUFFER_FULL_SHIFT(wIndex, rIndex, shift, num) (CIRCULAR_BUFFER_NEXT_SHIFT(wIndex, shift, num) == rIndex)
#define IS_CIRCULAR_BUFFER_EMPTY(wIndex, rIndex)     (wIndex == rIndex)

typedef struct {
    volatile javacall_uint16   wIndex;  // index of an element to write
    volatile javacall_uint16   rIndex;  // index of an element to read
    javacall_uint16            num;     // number of elements in a buffer
    javacall_uint16            size;    // size of an element in bytes
    javacall_handle            buff;    // pointer to a memory block
} javautil_circular_buffer;


javacall_result javautil_circular_buffer_create(javacall_handle *bufferHandle, javacall_uint16 num,
                                                javacall_uint16 size) {
    javautil_circular_buffer *buffer;

    if (0 == bufferHandle || 0 == num || 0 == size) {
        return JAVACALL_INVALID_ARGUMENT;
    }

    buffer = (javautil_circular_buffer*)javacall_malloc(sizeof(javautil_circular_buffer));
    if (0 == buffer) {
        return JAVACALL_OUT_OF_MEMORY;
    }

    //ALGORITHM_NOTE: in case of put operation there always should be at least one free cell between
    //wIndex and rIndex.
    num++;
    buffer->buff  = javacall_malloc(num * size);
    if (0 == buffer->buff) {
        javacall_free(buffer);
        return JAVACALL_OUT_OF_MEMORY;
    }

    buffer->wIndex = 0;
    buffer->rIndex = 0;
    buffer->num = num;
    buffer->size = size;

    *bufferHandle = (javacall_handle)buffer;

    return JAVACALL_OK;
}

void javautil_circular_buffer_destroy(javacall_handle bufferHandle) {
    javautil_circular_buffer *buffer = (javautil_circular_buffer*)bufferHandle;

    if (0 == buffer) {
        return;
    }

    if (0 != buffer->buff) {
        javacall_free(buffer->buff);
    }

    javacall_free(buffer);
}

javacall_result javautil_circular_buffer_put(javacall_handle bufferHandle, javacall_handle elem) {
    javautil_circular_buffer *buffer = (javautil_circular_buffer*)bufferHandle;

    if (0 == buffer) {
        return JAVACALL_INVALID_ARGUMENT;
    }

    if (IS_CIRCULAR_BUFFER_FULL(buffer->wIndex, buffer->rIndex, buffer->num)) {
        return JAVACALL_FAIL;
    }

    javautil_memcpy((javacall_uint8*)buffer->buff + buffer->wIndex * buffer->size, elem, buffer->size);
    buffer->wIndex = CIRCULAR_BUFFER_NEXT(buffer->wIndex, buffer->num);
    return JAVACALL_OK;
}

javacall_result javautil_cicular_buffer_put_array(javacall_handle bufferHandle, javacall_handle elements,
                                            javacall_int32 count) {
    javautil_circular_buffer *buffer = (javautil_circular_buffer*)bufferHandle;
    javacall_uint16 lwIndex = buffer->wIndex;
    javacall_uint16 lrIndex = buffer->rIndex;
    javacall_int32 numToWriteTail = 0;
    javacall_int32 numToWriteHead = 0;
    javacall_int32 remainder;

    if (0 == buffer || count <= 0 || count > buffer->num) {
        return JAVACALL_INVALID_ARGUMENT;
    }

    if (lwIndex >= lrIndex) {
        numToWriteTail = buffer->num - lwIndex;

        if (numToWriteTail < count) {
            //ALGORITHM_NOTE: at least one free cell should be between wIndex and rIndex for next put operations
            //Look at IS_CIRCULAR_BUFFER_FULL impl
            numToWriteHead = lrIndex - 1;
            remainder = count - numToWriteTail;
            if (numToWriteHead < remainder) {
                //not enough free space in the buffer
                return JAVACALL_FAIL;

            } else {
                //all the data is put in both the tail and head of the buffer
                numToWriteHead = remainder;
            }

        } else {
            //all the data is put in the tail of the buffer
            numToWriteTail = count;
            if (IS_CIRCULAR_BUFFER_FULL_SHIFT(lwIndex, lrIndex, numToWriteTail, buffer->num)) {
                //ALGORITHM_NOTE:after all the data is written, the writing pointer will be placed at the same
                //position where reading pointer is located. It means that the buffer is overflowed.
                return JAVACALL_FAIL;
            }
        }

    } else {
        //ALGORITHM_NOTE: at least one free cell should be between wIndex and rIndex for next put operations
        //Look at IS_CIRCULAR_BUFFER_FULL impl
        numToWriteHead = (lrIndex - lwIndex) - 1;
        if (numToWriteHead < count) {
            //not enough free space in the buffer
            return JAVACALL_FAIL;

        } else {
            //all the data is put in the head
            numToWriteHead = count;
        }
    }

    if (numToWriteTail > 0) {
        javautil_memcpy((javacall_uint8*)buffer->buff + lwIndex * buffer->size, elements,
                buffer->size * numToWriteTail);
        lwIndex = CIRCULAR_BUFFER_NEXT_SHIFT(lwIndex, numToWriteTail, buffer->num);
        elements = (javacall_uint8*)elements + buffer->size * numToWriteTail;
    }

    if (numToWriteHead > 0) {
        javautil_memcpy((javacall_uint8*)buffer->buff + lwIndex * buffer->size, elements,
                buffer->size * numToWriteHead);
        lwIndex = CIRCULAR_BUFFER_NEXT_SHIFT(lwIndex, numToWriteHead, buffer->num);
    }

    buffer->wIndex = lwIndex;

    return JAVACALL_OK;
}

javacall_result javautil_circular_buffer_get(javacall_handle bufferHandle, javacall_handle elem) {

    javautil_circular_buffer *buffer = (javautil_circular_buffer*)bufferHandle;
    if (0 == buffer) {
        return JAVACALL_INVALID_ARGUMENT;
    }

    if (IS_CIRCULAR_BUFFER_EMPTY(buffer->wIndex, buffer->rIndex))   {
        return JAVACALL_FAIL;
    }

    javautil_memcpy(elem, (javacall_uint8*)buffer->buff + buffer->rIndex * buffer->size, buffer->size);
    buffer->rIndex = CIRCULAR_BUFFER_NEXT(buffer->rIndex, buffer->num);
    return JAVACALL_OK;
}

javacall_result javautil_circular_buffer_get_array(javacall_handle bufferHandle, javacall_handle elements,
                                             javacall_int32 /*IN/OUT*/*len) {
    javautil_circular_buffer *buffer = (javautil_circular_buffer*)bufferHandle;
    javacall_uint16 lwIndex = buffer->wIndex;
    javacall_uint16 lrIndex = buffer->rIndex;
    javacall_int32 numToReadTail = 0;
    javacall_int32 numToReadHead = 0;
    javacall_int32 remainder;
    javacall_int32 lengthToRead = *len;

    if (0 == buffer || lengthToRead <= 0) {
        return JAVACALL_INVALID_ARGUMENT;
    }

    if (IS_CIRCULAR_BUFFER_EMPTY(lwIndex, lrIndex))   {
        return JAVACALL_FAIL;
    }

    if (lrIndex > lwIndex) {
        numToReadTail = buffer->num - lrIndex;

        if (numToReadTail < lengthToRead) {
            remainder = lengthToRead - numToReadTail;
            //lwIndex helps to identify how many data can be read from buffer's head
            numToReadHead = lwIndex > remainder ? remainder : lwIndex;
        } else {
            numToReadTail = lengthToRead;
        }

    } else {
        //lrIndex is less than lwIndex (equality is checked by IS_CIRCULAR_BUFFER_EMPTY above)
        numToReadHead = lwIndex - lrIndex;
        if (numToReadHead > lengthToRead) {
            numToReadHead = lengthToRead;
        }
    }

    if (numToReadTail > 0) {
        javautil_memcpy(elements, (javacall_uint8*)buffer->buff + lrIndex * buffer->size, buffer->size * numToReadTail);
        lrIndex = CIRCULAR_BUFFER_NEXT_SHIFT(lrIndex, numToReadTail, buffer->num);
        elements = (javacall_uint8*)elements + buffer->size * numToReadTail;
    }

    if (numToReadHead > 0) {
        javautil_memcpy(elements, (javacall_uint8*)buffer->buff + lrIndex * buffer->size, buffer->size * numToReadHead);
        lrIndex = CIRCULAR_BUFFER_NEXT_SHIFT(lrIndex, numToReadHead, buffer->num);
    }

    buffer->rIndex = lrIndex;
    *len = numToReadHead + numToReadTail;

    return JAVACALL_OK;
}

javacall_result javautil_circular_buffer_get_count(javacall_handle bufferHandle, javacall_int32 /*OUT*/*count) {
    javautil_circular_buffer *buffer = (javautil_circular_buffer*)bufferHandle;

    if (0 == buffer) {
        return JAVACALL_FAIL;
    }

    *count = (buffer->rIndex > buffer->wIndex)
            ? ((buffer->num - buffer->rIndex) + buffer->wIndex)
            : (buffer->wIndex - buffer->rIndex);

    return JAVACALL_OK;
}

javacall_result javautil_circular_buffer_free_size(javacall_handle bufferHandle, javacall_int32 /*OUT*/*size) {
    javautil_circular_buffer *buffer = (javautil_circular_buffer*)bufferHandle;
    javacall_uint16 lwIndex = buffer->wIndex;
    javacall_uint16 lrIndex = buffer->rIndex;

    if (0 == buffer) {
        return JAVACALL_FAIL;
    }

    if (lwIndex == lrIndex) {
        *size = buffer->num - 1;
    } else {
        *size = (lwIndex > lrIndex ? ((buffer->num - lwIndex) + lrIndex) : (lrIndex - lwIndex)) - 1;
    }

    return JAVACALL_OK;
}
