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


#ifndef __JAVACALL_SPI_H
#define __JAVACALL_SPI_H

#ifdef __cplusplus
extern "C"{
#endif

/**
 * @file javacall_spi.h
 * @ingroup SPIAPI
 * @brief  Javacall interfaces for SPI device access JSR
 *
 */

#include "javacall_defs.h"
#include "javacall_dio.h"
/**
 * @defgroup SPIAPI SPI API
 * @ingroup DeviceAccess
 * @{
 */

/**
 * @enum Chip Select active level possible variants
 */
typedef enum {
    DAAPI_SPI_CS_DEFAULT = -1,
    DAAPI_SPI_CS_ACTIVE_HIGH,
    DAAPI_SPI_CS_ACTIVE_LOW,
    DAAPI_SPI_CS_NOT_CONTROLLED
} javacall_spi_cs_active;


/**
 * Open SPI slave of given HINT and prepare for use.
 * <p>
 * A peripheral device may be opened in shared mode if supported
 * by the underlying driver and hardware and if it is not
 * already opened in exclusive mode. A peripheral device may be
 * opened in exclusive mode if supported by the underlying
 * driver and hardware and if it is not already opened.
 *
 * @param busNumber     number of the bus the slave is connected to
 * @param address       Chip Select address of the slave on the bus
 * @param csActive      chip select control parameter. If {@code
 *                      CS_NOT_CONTROLLED} is selected then
 *                      CS pin control is responsibility
 *                      of the application.
 * @param clockFrequency  the clock frequency (in Hz)
 * @param clockMode       clock mode
 * @param wordLength      word length (in bits)
 * @param bitOrdering     shift out ordering
 * (NB: if byte ordering is big-endian then it's assumed that the bits are shifted out in MSb order
 *  if byte ordering is little-endian then it's assumed that the bits are shifted out in LSb order)
 * @param exclusive       exclusive mode flag
 * @param pHandle         pointer to store SPI slave handle
 *
 * @retval JAVACALL_DIO_OK         open success
 * @retval JAVACALL_DIO_FAIL       open fail
 * @retval JAVACALL_DIO_NOT_FOUND  Peripheral is not found
 * @retval JAVACALL_DIO_BUSY       attempt to open already opened
 *         peripheral in exclusive mode or peripheral was locked
 *         by {@link #javacall_spi_lock(const javacall_handle,
 *         javacall_handle* const)}
 * @retval JAVACALL_DIO_INVALID_CONFIG if provided peripheral
 *        config parameters are unsupported
 * @retval JAVACALL_DIO_UNSUPPORTED_ACCESS_MODE  if EXCLUSIVE or
 *        SHARED mode is not supported
 */
javacall_dio_result javacall_spi_open_slave_with_config(javacall_int32 busNumber,
        javacall_int32 address, javacall_spi_cs_active csActive, javacall_int32 clockFrequency,
        javacall_int32 clockMode, javacall_int32 wordLength,
        javacall_byteorder bitOrdering,
        const javacall_bool exclusive,
        /*OUT*/javacall_handle* pHandle);

/**
 * Close SPI slave of given handle.
 * <p>
 * It is up to caller to cancel/complete  transaction/lock
 * sequence prior to close peripheral since such action requires
 * java thread state manipulation.
 * <p>
 * The function returns no status since there is no way to
 * recover from error if any.
 *
 * @param handle      handle of slave to be closed.
 */
void javacall_spi_close_slave(javacall_handle handle);


/**
 * Start Send and Receive Data.
 * <p>
 * pTxBuf must not be null.
 * <p>
 * Both buffers must be accessible between {@link
 * javacall_spi_send_and_receive_start} {@link
 * javacall_spi_send_and_receive_finish} call, i.e. must not be
 * allocated at java heap
 * <p>
 * Buffers len must be aligned to word length acquired by {@link
 * javacall_spi_get_word_size}
 *
 *
 * @param handle      SPI slave handle
 * @param pTxBuf      the data to send.
 * @param pRxBuf      the buffer for the received bytes, may be
 *                    {@code null} if not receive data is
 *                    expected
 * @param len         the number of bytes to send/receive
 *
 * @retval JAVACALL_DIO_OK          success
 * @retval JAVACALL_DIO_FAIL        general IO failure
 * @retval JAVACALL_DIO_WOULD_BLOCK if the caller needs a notification to complete the operation
 * @retval JAVACALL_DIO_INVALID_STATE the bus is occupied for
 *        communication with other peripheral
 *
 */
javacall_dio_result javacall_spi_send_and_receive_start(javacall_handle handle,const char* pTxBuf, /*OUT*/char* pRxBuf, const int len);

/**
 * Finish Send and Receive Data.
 * <p>
 * Requirements for receive and send buffers are the same as for
 * {@link javacall_spi_send_and_receive_start}
 * <p>
 * Set {@code cancel} field to cancel ongoing operation
 *
 * @param handle      SPI slave handle
 * @param cancel      indicate operation cancel request
 * @param pTxBuf      the data to send.
 * @param pRxBuf      the buffer for the received bytes, may be
 *                    {@code null} if not receive data is
 *                    expected
 * @param len         the number of bytes to send/receive
 *
 * @retval JAVACALL_DIO_OK    success
 * @retval JAVACALL_DIO_WOULD_BLOCK if the caller needs a notification to complete the operation
 * @retval JAVACALL_DIO_FAIL  general IO failure.
 *
 */
javacall_dio_result javacall_spi_send_and_receive_finish(javacall_handle handle, const javacall_bool cancel,
                                                         const char* pTxBuf, /*OUT*/char* pRxBuf, const int len);

/**
 * Get word size of SPI slave
 *
 * @param const for SPI slave
 * @param pSize  the pointer to the variable to store word size of SPI slave
 *
 * @retval JAVACALL_DIO_OK    success
 * @retval JAVACALL_DIO_FAIL  fail
 *
 */
javacall_dio_result javacall_spi_get_word_size(const javacall_handle handle, /*OUT*/javacall_int32* pSize);

/**
 * Get byte ordering of SPI slave
 *
 * @param handle for SPI slave
 * @param pByteOrdering  the pointer to the variable to store byte ordering of SPI slave
 *
 * @retval JAVACALL_DIO_OK    success
 * @retval JAVACALL_DIO_FAIL  fail
 */
javacall_dio_result javacall_spi_get_byte_ordering(const javacall_handle handle,
        /*OUT*/javacall_int32* pByteOrdering);

/**
 * Returns power control group of this SPI device. It is used
 * for power management notification.
 *
 * @param handle open device handle
 * @param grp    power managment group
 *
 * @retval JAVACALL_DIO_OK    success
 * @retval JAVACALL_DIO_FAIL  fail (the device was closed)
 */
javacall_dio_result javacall_spi_get_group_id(const javacall_handle handle, javacall_int32* const grp);

/**
 * Demarcates the beginning of an SPI transaction, e.g. asserts
 * CS pin and prevent other SPI client to access the bus.
 *
 * @param handle for SPI slave
 *
 * @retval JAVACALL_DIO_OK    success
 * @retval JAVACALL_DIO_INVALID_STATE if the bus is already
 *         locked by any device
 * @retval JAVACALL_DIO_UNSUPPORTED_OPERATION arbitrary control
 *         of CS pin is not supported
 * @retval JAVACALL_DIO_FAIL it is impossible to lock the bus
 */
javacall_dio_result javacall_spi_begin(javacall_handle pHandle);

/**
 * Demarcates the end of an SPI transaction, e.g. release CS pin
 * <p>
 * It is guaranteed that end() is called after begin().
 *
 * @param handle for SPI slave
 *
 * @retval JAVACALL_DIO_OK    success
 * @retval JAVACALL_DIO_INVALID_STATE    The bus hasn't been
 *         owned by the device
 */
javacall_dio_result javacall_spi_end(javacall_handle pHandle);

 /**
 * Attempts to lock for exclusive access the underlying
 * peripheral device resource.
 * <p>
 * Checks for status and returns immediately if the resource is
 * already locked.
 *
 * @param handle of open SPI device
 * @param owner a pointer to current owner handle if attempt
 *              failed
 *
 * @retval JAVACALL_DIO_OK    if exclusive access was granted
 * @retval JAVACALL_DIO_FAIL  if the resource is locked by other
 *         application
 */
javacall_dio_result javacall_spi_lock(const javacall_handle handle, javacall_handle* const owner);

/**
 * Releases from exclusive access the underlying peripheral
 * device resource.
 * <p>
 * Returns silently if the resource was not
 * locked to <code>handle</code>
 *
 * @param handle open resource handle
 *
 * @retval JAVACALL_DIO_OK if <code>handle</code> is owner of the
 *         resource and the resuorce is released
 * @retval JAVACALL_DIO_FAIL otherwise
 *
 */
javacall_dio_result javacall_spi_unlock(const javacall_handle handle);

/**
 * Called by platform to notify about SPI asynchronous transfer
 * status.
 *
 * @param handle  handle of SPI device that generated event
 * @param result  status of transfer operation
 */
void javanotify_spi_event(const javacall_handle handle, javacall_int32 result);

/** @} */

#ifdef __cplusplus
}
#endif

#endif //__JAVACALL_SPI_H

