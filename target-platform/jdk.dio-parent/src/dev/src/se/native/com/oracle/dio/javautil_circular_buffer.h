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


/**
 * @file
 *
 * Interface for the circular buffer handling.
 *
 * Concurrency note.
 *
 * Implementation of the circular buffer is a locked free and can be safely used in a multy threaded environment
 * if the conditions below are fulfilled:
 * -only two threads work with a circular buffer. One thread must perform write operations and the other one - read operations;
 * -load/store assembler instruction for a 16 bit volatile variable is atomic
 * -system has only one CPU unit (to avoid cache coherency issues when data can be cached in CPU's internal cache)
 *
 * In other cases you must apply different techniques to make the implementation thread-safe.
 *
 */

#ifndef _JAVAUTIL_CIRCULARBUFFER_
#define _JAVAUTIL_CIRCULARBUFFER_

#include "javacall_defs.h"

#ifdef __cplusplus
extern "C" {
#endif


/**
 * Allocate a buffer of a given size.
 *
 * @param bufferHandle   handle to store circular buffer's handle to
 * @param num            number of elements in the buffer; must be greater than 0
 * @param size           size of an element in bytes; must be greater than 0
 *
 * @return <code>JAVACALL_OK</code>                on success
 *         <code>JAVACALL_OUT_OF_MEMORY</code>     if unable to allocate the buffer
 *         <code>JAVACALL_INVALID_ARGUMENT</code>  if the buffer handle, the number of elements or
 *                                                 an element size are not valid
 */
javacall_result javautil_circular_buffer_create(javacall_handle *bufferHandle, javacall_uint16 num, javacall_uint16 size);

/**
 * Free a buffer.
 *
 * @param bufferHandle a handle to a circular buffer
 */
void javautil_circular_buffer_destroy(javacall_handle bufferHandle);

/**
 * Put an element to a buffer.
 *
 * @param bufferHandle    handle to the circular buffer
 * @param elem element    being written to the buffer
 *
 * @return <code>JAVACALL_OK</code>                 on success
 *         <code>JAVACALL_FAIL</code>               if unable to put an element to the buffer 'cause it's full
 *         <code>JAVACALL_INVALID_ARGUMENT</code>   if the buffer handle is not valid
 */
javacall_result javautil_circular_buffer_put(javacall_handle bufferHandle, javacall_handle elem);

/**
 * Put several elements to a buffer.
 *
 * @param bufferHandle  handle to the circular buffer
 * @param elements      handle to the elements being written to the buffer
 * @param count         quantity of <code>elements</codes> to write
 *
 * @return <code>JAVACALL_OK</code>                 on success
 *         <code>JAVACALL_FAIL</code>               if unable to put all elements to the buffer cause it's full
 *         <code>JAVACALL_INVALID_ARGUMENT</code>   if the buffer handle is not valid or <code>count</code> is bigger
 *                                                  than buffer's size
 *
 */
javacall_result javautil_cicular_buffer_put_array(javacall_handle bufferHandle, javacall_handle elements,
                                            javacall_int32 count);

/**
 * Get an element from a buffer.
 *
 * @param bufferHandle   handle to the circular buffer
 * @param elem           element to read from the buffer
 *
 * @return <code>JAVACALL_OK</code>                 on success
 *         <code>JAVACALL_FAIL</code>               if unable to get an element from the buffer 'cause it's empty
 *         <code>JAVACALL_INVALID_ARGUMENT</code>   if the buffer handle is not valid
 */
javacall_result javautil_circular_buffer_get(javacall_handle bufferHandle, javacall_handle elem);

/**
 * Get several elements from a buffer.
 *
 * @param bufferHandle   handle to the circular buffer
 * @param elements       handle to the elements that have to be read from the circular buffer
 * @param len            IN - size of the <code>elements</code> buffer
 *                       OUT - total number of elements read to the <code>elements<code> buffer
 *
 * @return <code>JAVACALL_OK</code>                 on success
 *         <code>JAVACALL_FAIL</code>               buffer is empty
 *         <code>JAVACALL_INVALID_ARGUMENT</code>   if the buffer handle is not valid or len is less or equal to 0
 */
javacall_result javautil_circular_buffer_get_array(javacall_handle bufferHandle, javacall_handle elements,
                                             javacall_int32 /*IN/OUT*/*len);

/**
 * Get number of elements in a buffer available for read.
 *
 * @param bufferHandle handle to the circular buffer
 * @param count number of elements in the buffer available for reading
 *
 * @return <code>JAVACALL_OK</code>                 on success
 *         <code>JAVACALL_FAIL</code>               on erros
 */
javacall_result javautil_circular_buffer_get_count(javacall_handle bufferHandle, javacall_int32 /*OUT*/*count);

/**
 * Returns the number of elements that can be written into the buffer.
 *
 * @param bufferHandle handle to the circular buffer
 * @param size number of elements that can be written into the buffer
 *
 * @return <code>JAVACALL_OK</code>                 on success
 *         <code>JAVACALL_FAIL</code>               on erros
 */
javacall_result javautil_circular_buffer_free_size(javacall_handle bufferHandle, javacall_int32 /*OUT*/*size);

#ifdef __cplusplus
} // extern "C"
#endif

#endif
