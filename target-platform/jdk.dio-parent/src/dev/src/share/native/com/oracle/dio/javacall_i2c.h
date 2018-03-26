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


#ifndef __JAVACALL_I2C_H
#define __JAVACALL_I2C_H


#ifdef __cplusplus
extern "C" {
#endif

/**
 * @file javacall_i2c.h
 * @ingroup I2CAPI
 * @brief Javacall interfaces for I2C device access JSR
 *
 */

#include "javacall_defs.h"
#include "javacall_dio.h"

/**
 * @defgroup I2CAPI I2C API
 * @ingroup DeviceAccess
 * @{
 */


/**
 * @enum javacall_i2c_message_type
 * @brief differentiate I2C message by its participation in I2C
 *        combined message
 */
typedef enum {
    /** single shot mesage */
    JAVACALL_I2C_REGULAR,
    /** first part of I2C combined message */
    JAVACALL_I2C_COMBINED_START,
    /** last part of I2C combined message */
    JAVACALL_I2C_COMBINED_END,
    /** intermediate part of I2C combined message */
    JAVACALL_I2C_COMBINED_BODY
} javacall_i2c_message_type;

/**
 * @enum javacall_i2c_signal_type
 * @brief asynchronous operation signal types
 */
typedef enum {
    JAVACALL_I2C_SEND_SIGNAL,
    JAVACALL_I2C_RECV_SIGNAL
} javacall_i2c_signal_type;

/**
 * Initializes the specified I2C slave with given address on given bus and gives back a handle.
 *
 * This function should do any initialization that is required for doing communication
 * with the specified I2C slave device.
 * If the initialization is successful, a handle is returned which is used as a token
 * for doing further transactions with the specified I2C slave.
 * <p>
 * A peripheral device may be opened in shared mode if supported
 * by the underlying driver and hardware and if it is not
 * already opened in exclusive mode. A peripheral device may be
 * opened in exclusive mode if supported by the underlying
 * driver and hardware and if it is not already opened.
 *
 * @param busNum       serial number of I2C bus on the board
 * @param devAddr      I2C slave address
 * @param addrSize      7 or 10 bit address size
 * @param clockFrequency  clock frequency (in Hz)
 * @param exclusive      exclusive mode flag
 * @param pHandle     pointer to store I2C peripheral handle
 *
 * @retval JAVACALL_DIO_OK          success
 * @retval JAVACALL_DIO_INVALID_CONFIG   error/inconsistency in device configuration
 * @retval JAVACALL_DIO_FAIL   device is not found
 * @retval JAVACALL_DIO_BUSY   attempt to open already opened
 *         peripheral in exclusive mode or peripheral was locked
 *         by {@link #javacall_i2c_lock(const javacall_handle,
 *         javacall_handle* const)}
 * @retval JAVACALL_DIO_UNSUPPORTED_ACCESS_MODE    if EXCLUSIVE or SHARED mode is not supported
 *
 */
javacall_dio_result javacall_i2c_open_slave_with_config(javacall_int32 busNum, javacall_int32 devAddr,
                                                    javacall_int32 addrSize, javacall_int32 clockFrequency,
                                                    const javacall_bool exclusive,
                                                    /*OUT*/ javacall_handle* pHandle);


/**
 * Initiates data exchange operation with the I2C slave.
 * <p>
 * Supplied buffers must be accessible between {@link
 * javacall_i2c_transfer_start} {@link
 * javacall_i2c_transfer_finish} call, i.e. must not be
 * allocated at java heap
 *
 * @param handle handle of the I2C slave under use
 * @param type  message  type. used to demarcate first and last
 *              part of combined message, or to mark single
 *              message action
 * @param write direction of transfer, JAVACALL_TRUE for write,
 *             JAVACALL_FALSE for read
 * @param pData buffer to store the read data or keeps data to
 *              write
 * @param len   number of bytes to transfer
 * @param pBytes  a pointer to store a number of bytes was
 *                   transfered if the function returns {@link
 *                   JAVACALL_DIO_OK}
 * @retval JAVACALL_DIO_OK      success
 * @retval JAVACALL_DIO_WOULD_BLOCK if the caller needs a notification to complete the operation
 * @retval JAVACALL_DIO_FAIL  it is impossible to start
 *         operation. Possible another master is acting on it or
 *         native application performs communication.
 * @retval JAVACALL_DIO_INVALID_STATE the bus is occupied for
 *        communication with other peripheral or locked for I2C
 *        combined message
 *
 */
javacall_dio_result javacall_i2c_transfer_start(const javacall_handle handle,
                                                const javacall_i2c_message_type type,
                                                const javacall_bool write,
                                                char* pData, int len,
                                                javacall_int32 *const pBytes);

/**
 * Finalizes transfer operation with the I2C slave.
 * <p>
 * Set {@code cancel} field to cancel ongoing operation.
 * @note cancel request cancels whole combined message as well.
 *
 * @param handle handle of the I2C slave under use
 * @param cancel      indicate operation cancel request
 * @param pData buffer to store the read data.
 * @param len number of bytes to read from I2C slave.
 * @param pBytes a pointer to store a number of bytes was
 *                   transfered if the function returns {@link
 *                   JAVACALL_DIO_OK}
 * @retval JAVACALL_DIO_OK      success
 * @retval JAVACALL_DIO_WOULD_BLOCK if the caller needs a notification to complete the operation
 * @retval JAVACALL_DIO_FAIL    if an error occurred
 *
 */
javacall_dio_result javacall_i2c_transfer_finish(const javacall_handle handle,
                                                 const javacall_bool cancel,
                                                 char* pData, int len,
                                                 javacall_int32* const pBytes);

/**
 * Called by platform to notify about I2C asynchronous transfer
 * status.
 *
 * @param signal  type of transfer
 * @param handle  handle of I2C device that generated event
 * @param result  status of transfer operation
 */
void javanotify_i2c_event(const javacall_i2c_signal_type signal, const javacall_handle handle, javacall_int32 result);

/**
 * Disables the I2C slave and frees all resources.
 * <p>
 * The function returns no status since there is no way to
 * recover from error if any.
 * <p>
 * It is up to caller to cancel/complete  transaction/lock
 * sequence prior to close peripheral since such action requires
 * java thread state manipulation.
 *
 * @param handle handle of the I2C slave under use
 *
 */
void javacall_i2c_close(const javacall_handle handle);


/**
 * Get the I2C slave group id.
 *
 * @param handle handle of the I2C slave under use
 *
 * @retval JAVACALL_DIO_OK          success
 * @retval JAVACALL_DIO_FAIL        if there was an error
 */
javacall_dio_result javacall_i2c_get_group_id(const javacall_handle handle,
        /*OUT*/ long* grpId);


/**
 * Attempts to lock for exclusive access the underlying
 * peripheral device resource.
 * <p>
 * Checks for status and returns immediately if the resource is
 * (already) locked.
 *
 * @param handle of open resource
 * @param owner a pointer to current owner handle
 *
 * @return JAVACALL_DIO_OK if exclusive access was granted,
 *         JAVACALL_DIO_FAIL if the resource is locked by other
 *         application
 */
javacall_dio_result javacall_i2c_lock(const javacall_handle handle, javacall_handle* const owner);

/**
 * Releases from exclusive access the underlying peripheral
 * device resource.
 *
 * @param handle open resource handle
 *
 * @return JAVACALL_DIO_OK if <code>handle</code> is owner of the
 *         resource and the resuorce is released
 * @return JAVACALL_DIO_FAIL otherwise
 */
javacall_dio_result javacall_i2c_unlock(const javacall_handle handle);

/** @} */

#ifdef __cplusplus
}
#endif

#endif /*__JAVACALL_I2C_H*/
