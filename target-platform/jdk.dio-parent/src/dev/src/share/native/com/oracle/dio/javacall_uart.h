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

#ifndef __JAVACALL_UART_H_
#define __JAVACALL_UART_H_

/**
 * @file javacall_uart.h
 * @ingroup UART
 * @brief Javacall interfaces for DIO serial port
 */

#ifdef __cplusplus
extern "C" {
#endif

#include "javacall_defs.h"
#include "javacall_dio.h"


/**
 * @enum javacall_uart_parity
 * @brief UART parities
 */
typedef enum {
    /** No parity bit. */
    UART_PARITY_NONE = 0,
    /** ODD parity scheme. */
    UART_PARITY_ODD = 1,
    /** EVEN parity scheme. */
    UART_PARITY_EVEN = 2,
    /** MARK parity scheme. */
    UART_PARITY_MARK = 3,
    /** SPACE parity scheme. */
    UART_PARITY_SPACE = 4
} javacall_uart_parity;

/**
 * @enum javacall_uart_bits_per_char
 * @brief UART bits per char
 */
typedef enum {
    /** 5-bit chars */
    BITS_PER_CHAR_5 = 5,
    /** 6-bit chars */
    BITS_PER_CHAR_6 = 6,
    /** 7-bit chars */
    BITS_PER_CHAR_7 = 7,
    /** 8-bit chars */
    BITS_PER_CHAR_8 = 8,
    /** 9-bit chars */
    BITS_PER_CHAR_9 = 9
} javacall_uart_bits_per_char;

/**
 * @enum javacall_uart_stop_bits
 * @brief UART bits per char
 */
typedef enum {
    /* Number of STOP bits - 1. */
    STOPBITS_1 = 1,
    /* Number of STOP bits - 1-1/2. */
    STOPBITS_1_5 = 2,
    /* Number of STOP bits - 2. */
    STOPBITS_2 = 3,
} javacall_uart_stop_bits;

/**
 * @enum javacall_uart_event_type
 * @brief UART event types
 */
typedef enum {
    /** Input data available */
    INPUT_DATA_AVAILABLE = 0,
    /** Input buffer overrun */
    INPUT_BUFFER_OVERRUN = 1,
    /** Output buffer empty */
    OUTPUT_BUFFER_EMPTY = 2,
    /** Event ID indicating a break interrupt */
    BREAK_INTERRUPT = 4,
    /** Event ID indicating a parity error */
    PARITY_ERROR = 8,
    /** Event ID indicating a parity error*/
    FRAMING_ERROR = 16
} javacall_uart_event_type;

/**
 * A callback function to be called for notification of uart events.
 *
 * @param hPort     uart port handle
 *
 * @param type      type of event: Either
 *                  -INPUT_DATA_AVAILABLE,
 *                  -INPUT_BUFFER_OVERRUN,
 *                  -OUTPUT_BUFFER_EMPTY
 */
void javanotify_uart_event(javacall_uart_event_type type, javacall_handle hPort,
                           javacall_int32 bytesProcessed, javacall_result result);

/** @} */
/** @} */

/**
 * Starts opening uart according to the given parameters.
 *
 * @param devName     the name of the port / device to be opened ("COM1")
 * @param baudRate    the baud rate for the open connection
 * @param stopBits    the number of stop bits
 * @param flowControl the flow control
 * @param bitsPerchar the number of bits per character
 * @param parity      the parity
 * @param exclusive   the exclusive mode flag
 * @param pHandle     the handle of the port to be opened
 *
 * @retval JAVACALL_DIO_OK               success
 * @retval JAVACALL_DIO_FAIL             fail
 * @retval JAVACALL_DIO_BUSY             if the device is already open with exclusive mode
 * @retval JAVACALL_DIO_WOULD_BLOCK      if the caller must call the finish function again to complete the operation
 * @retval JAVACALL_DIO_INVALID_CONFIG   if incoming parameters are invalid
 * @retval JAVACALL_DIO_UNSUPPORTED_ACCESS_MODE  if EXCLUSIVE or
 *        SHARED mode is not supported
 */
javacall_dio_result
javacall_uart_open_start(const char *devName, unsigned int baudRate,
                         javacall_uart_stop_bits stopBits, unsigned int flowControl,
                         javacall_uart_bits_per_char bitsPerchar, javacall_uart_parity parity,
                         const javacall_bool exclusive, /*OUT*/javacall_handle *pHandle);

/**
 * Finishes opening uart according to the given parameters.
 *
 * @param devName     the name of the port / device to be opened ("COM1")
 * @param baudRate    the baud rate for the open connection
 * @param stopBits    the number of stop bits
 * @param flowControl the flow control
 * @param bitsPerchar the number of bits per character
 * @param parity      the parity
 * @param exclusive   the exclusive mode flag
 * @param pHandle     the handle of the port to be opened
 *
 * @retval JAVACALL_DIO_OK               success
 * @retval JAVACALL_DIO_FAIL             fail
 * @retval JAVACALL_DIO_WOULD_BLOCK      if the caller must call the finish function again to complete the operation
 * @retval JAVACALL_DIO_INVALID_CONFIG if incoming parameters are invalid
 */
javacall_dio_result
javacall_uart_open_finish(const char *devName, unsigned int baudRate,
                         javacall_uart_stop_bits stopBits, unsigned int flowControl,
                         javacall_uart_bits_per_char bitsPerchar, javacall_uart_parity parity,
                         /*OUT*/javacall_handle *pHandle);

/**
 * Initiates closing serial link.
 *
 * @param hPort the port to close
 * @param pContext filled by ptr to data for reinvocations
 * after this call, java is guaranteed not to call javacall_uart_read_xx() or
 * javacall_uart_write_xx() before issuing another javacall_uart_open_xx( ) call.
 *
 * @retval JAVACALL_DIO_OK on success,
 * @retval JAVACALL_DIO_FAIL
 * @retval JAVACALL_DIO_WOULD_BLOCK  if the caller must call the finish function again to complete the operation
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_close_start(javacall_handle hPort, void **pContext);

/**
 * Finishes closing serial link.
 *
 * @param hPort the port to close
 * @param context ptr to data saved before sleeping
 * @retval JAVACALL_DIO_OK on success,
 * @retval JAVACALL_DIO_FAIL
 * @retval JAVACALL_DIO_WOULD_BLOCK  if the caller must call the finish function again to complete the operation
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_close_finish(javacall_handle hPort, void *context);

/**
 * Initiates reading a specified number of bytes from serial link,

 * @param hPort the port to read the data from
 * @param buffer to which data is read
 * @param size number of bytes to be read. Actual number of bytes
 *              read may be less, if less data is available
 * @param bytesRead actual number the were read from the port.
 * @param pContext filled by ptr to data for reinvocations
 * @retval JAVACALL_DIO_OK          success
 * @retval JAVACALL_DIO_FAIL        if there was an error
 * @retval JAVACALL_DIO_WOULD_BLOCK  if the caller must call the finish function again to complete the operation
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_read_start(javacall_handle hPort, unsigned char* buffer, int size ,/*OUT*/int *bytesRead, void **pContext);

/**
 * Finishes reading a specified number of bytes from serial link,
 *
 * @param hPort the port to read the data from
 * @param buffer to which data is read
 * @param size number of bytes to be read. Actual number of bytes
 *              read may be less, if less data is available
 * @param bytesRead actual number the were read from the port.
 * @param size number of bytes to be write.
 * @param context ptr to data saved before sleeping
 * @retval JAVACALL_DIO_OK          success
 * @retval JAVACALL_DIO_FAIL        if there was an error
 * @retval JAVACALL_DIO_WOULD_BLOCK  if the caller must call the finish function again to complete the operation
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_read_finish(javacall_handle hPort, unsigned char* buffer, int size, int *bytesRead, void *context);

/**
 * Initiates writing a specified number of bytes to serial link,
 *
 * @param hPort the port to write the data to
 * @param buffer buffer to write data from
 * @param size number of bytes to be written.
 * @param bytesWritten the number of bytes actually written.
 * @param pContext filled by ptr to data for reinvocations
 * @retval JAVACALL_OK          success
 * @retval JAVACALL_FAIL        if there was an error
 * @retval JAVACALL_WOULD_BLOCK  if the caller must call the finish function again to complete the operation
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_write_start(javacall_handle hPort, unsigned char* buffer,
  int size, int *bytesWritten, void **pContext);

/**
 * Finishes writing a specified number of bytes to serial link,
 *
 * @param hPort the port to write the data to
 * @param buffer buffer to write data from
 * @param size number of bytes to be write.
 * @param bytesWritten the number of bytes actually written.
 * @param context ptr to data saved before sleeping
 * @retval JAVACALL_OK          success
 * @retval JAVACALL_FAIL        if there was an error
 * @retval JAVACALL_WOULD_BLOCK  if the caller must call the finish function again to complete the operation
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_write_finish(javacall_handle hPort, unsigned char* buffer, int size, int *bytesWritten, void *context);

/**
 * Update the stopBits of an open uart port
 *
 * @param hPort the port to configure
 * @param stopBits new stopBits for the open connection
 * @retval JAVACALL_DIO_OK    on success,
 * @retval JAVACALL_DIO_UNSUPPORTED_OPERATION  if the stopBits
 *         is not supported
 * @retval JAVACALL_DIO_FAIL  on error
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_set_stop_bits(javacall_handle handle, javacall_uart_stop_bits stopBits);

/**
 * Retrive the current stopBits of the open uart port
 *
 * @param hPort the port to configure
 * @param stopBits pointer to where to return the stopBits
 * @retval JAVACALL_DIO_OK on success,
 *         JAVACALL_DIO_FAIL on error
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_get_stop_bits(javacall_handle handle, /*OUT*/ javacall_uart_stop_bits *stopbits);

/**
 * Update the parity of an open uart port
 *
 * @param hPort the port to configure
 * @param parity new parity for the open connection
 * @retval JAVACALL_DIO_OK on success,
 * @retval JAVACALL_DIO_UNSUPPORTED_OPERATION if the stopBits is
 *         not supported
 * @retval JAVACALL_DIO_FAIL on error
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_set_parity(javacall_handle handle, javacall_uart_parity parity);

/**
 * Retrive the current parity of the open uart port
 *
 * @param hPort the port to configure
 * @param parity pointer to where to return the parity
 * @retval JAVACALL_DIO_OK on success,
 *         JAVACALL_DIO_FAIL on error
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_get_parity(javacall_handle handle, /*OUT*/ javacall_uart_parity *parity);

/**
 * Update the bitsPerChar of an open uart port
 *
 * @param hPort the port to configure
 * @param bitsPerChar new bits per char for the open connection
 * @retval JAVACALL_DIO_OK on success,
 * @retval JAVACALL_DIO_UNSUPPORTED_OPERATION if the bitsPerChar
 *         is not supported
 * @retval JAVACALL_DIO_FAIL on error
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_set_bits_per_char(javacall_handle handle, javacall_uart_bits_per_char bitsPerChar);

/**
 * Retrive the bits per char of the open uart port
 *
 * @param hPort the port to configure
 * @param parity pointer to where to return the parity
 * @retval JAVACALL_DIO_OK on success,
 *         JAVACALL_DIO_FAIL on error
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_get_bits_per_char(javacall_handle handle, /*OUT*/ javacall_uart_bits_per_char *bitsPerChar);

/**
 * Stops write operations if any pending
 *
 * @param hPort the port to configure
 * @retval JAVACALL_DIO_OK on success,
 *         JAVACALL_DIO_FAIL on error
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_stop_writing(javacall_handle handle);


/**
 * Stops read operations if any pending
 *
 * @param hPort the port to configure
 * @retval JAVACALL_DIO_OK on success,
 *         JAVACALL_DIO_FAIL on error
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_stop_reading(javacall_handle handle);

/**
 * Starts listening for uart events.
 *
 * @param handle serial port handle
 *
 * @param type   type of event: Either
 *                  -INPUT_DATA_AVAILABLE,
 *                  -INPUT_BUFFER_OVERRUN,
 *                  -OUTPUT_BUFFER_EMPTY
 *                  -BREAK_INTERRUPT
 *                  -FRAMING_ERROR
 *                  -PARITY_ERROR
 *
 * @retval JAVACALL_DIO_OK if no error
 * @retval JAVACALL_DIO_UNSUPPORTED_OPERATION if event is not
 *         supported
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_start_event_listening(javacall_handle handle, javacall_uart_event_type eventId);

/**
 * Stops listening for uart events.
 *
 * @param handle serial port handle
 *
 * @param type   type of event: Either
 *                  -INPUT_DATA_AVAILABLE,
 *                  -INPUT_BUFFER_OVERRUN,
 *                  -OUTPUT_BUFFER_EMPTY
 *                  -BREAK_INTERRUPT
 *                  -FRAMING_ERROR
 *                  -PARITY_ERROR
 *
 * @retval JAVACALL_DIO_OK if no error
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_stop_event_listening(javacall_handle handle, javacall_uart_event_type eventId);

/**
 * Returns power control group of this channel. It is used for
 * power management notification.
 *
 * @param handle open device handle
 * @param grp    power managment group
 *
 *
 * @retval JAVACALL_DIO_FAIL if the device was closed,
 *         JAVACALL_DIO_OK otherwise
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_get_group_id(const javacall_handle handle, javacall_int32* const  grp);

/**
 * Attempts to lock for exclusive access the underlying
 * peripheral device resource.
 * <p>
 * Checks for status and returns immediately if the resource is
 * already locked.
 *
 * @param handle of open resource
 * @param owner a pointer to current owner handle
 *
 * @return JAVACALL_DIO_OK if exclusive access was granted,
 *         JAVACALL_DIO_FAIL if the resource is locked by other
 *         application
 */
javacall_dio_result
javacall_uart_lock(const javacall_handle handle, javacall_handle* const owner);

/**
 * Releases from exclusive access the underlying peripheral
 * device resource.
 * <p>
 * Returns silently if the resource was not
 * locked to <code>handle</code>
 *
 * @param handle of open resource
 * @param owner a pointer to current owner handle
 *
 * @return JAVACALL_OK if no errors,
 *         JAVACALL_FAIL if the resource is locked by other
 *         application
 */
javacall_dio_result
javacall_uart_unlock(const javacall_handle handle);

#ifdef __cplusplus
}
#endif

#endif

