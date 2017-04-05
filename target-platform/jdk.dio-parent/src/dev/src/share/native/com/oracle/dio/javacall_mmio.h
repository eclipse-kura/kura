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


#ifndef __JAVACALL_MMIO_H
#define __JAVACALL_MMIO_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @file javacall_mmio.h
 * @ingroup MMIOAPI
 * @brief Javacall interfaces for MMIO device access JSR
 *
 */

#include "javacall_defs.h"
#include "javacall_dio.h"

/**
 * @defgroup MMIOAPI MMIO API
 * @ingroup DeviceAccess
 * @{
 */

/**
 * @defgroup MandatoryMMIO Mandatory MMIO API
 * @ingroup MMIOAPI
 *
 *
 * @{
 */


/**
 * Platfrom specific procedure to provide access to requested
 * memory region. Typically it does nothing on MMU-less
 * platforms.
 *
 * @param addr      address of MMIO peripheral memory region
 * @param size      size of MMIO peripheral memory region
 * @param handle    a pointer to store handle that identifies
 *                  mmio resource
 * @param mappedAddr    a region in application address space
 *                      where requested region is mapped to.
 *
 * @return result of operation
 * @retval JAVACALL_OK in case of success
 * @retval JAVACALL_OUT_OF_MEMORY if there is no enough room to
 *         succeed operation
 * @retval JAVACALL_INVALID_ARGUMENT if {@code addr} or {@code
 *         size} values are invalid or unsupported or forbidden
 * @retval JAVACALL_FAIL if there is other I/O error
 *
 */
javacall_result javacall_mmio_open(const javacall_uint8* addr, const javacall_int32 size,
                                   /*out*/javacall_handle* const handle,
                                   /*out*/javacall_uint8** const mappedAddr);


/**
 * Release resources acquired by {@link #javacall_mmio_open}
 *
 * @param handle    resource handle
 *
 * @return result of operation
 * @retval JAVACALL_OK in case of success
 * @retval JAVACALL_FAIL if there is I/O error
 */
javacall_result javacall_mmio_close(const javacall_handle handle);

/**
 * Notifies driver that application is ready to receive
 * notification about event described by {@code event_id}
 * <p>
 * Event nature is implementation specific. External interrupt
 * handler is one option.
 *
 *
 * @param handle    mmio resource handle
 * @param offset    offset in the MMIO memory region
 * @param event_id  event ID
 * @param buffer    buffer pointer where to store copy of MMIO
 *                  memory region in the time of the event;
 *                  May be {@code NULL}
 * @param bufferLength copy buffer length. {@code 0} if {@code
 *                     buffer} is NULL
 *
 * @return result of operation
 * @retval  JAVACALL_OK if notification is supported and started
 * @retval  JAVACALL_FAIL if notification is not suuprted
 */
javacall_result javacall_mmio_start_listening_with_buffer(
        const javacall_handle handle,
        const javacall_uint32 offset,
        const javacall_uint32 event_id,
        javacall_uint8* const buffer,
        const javacall_uint32 bufferLength);

/**
 * Request to stop notification of event happens at mmio
 * resource
 *
 * @param handle    mmio resource handle
 * @param event_id  eevnt ID
 *
 * @return result of operation
 * @retval  JAVACALL_OK if notification is inactive
 * @retval  JAVACALL_FAIL generic I/O error
 */
javacall_result javacall_mmio_stop_listening(const javacall_handle handle, const javacall_uint32 event_id);


/** @} */
/** @} */

#ifdef __cplusplus
}
#endif

#endif /*__JAVACALL_MMIO_H */

