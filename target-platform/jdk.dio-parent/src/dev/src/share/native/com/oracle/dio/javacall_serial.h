/*
 *
 * Copyright (c) 2006, 2014, Oracle and/or its affiliates. All rights reserved.
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


#ifndef __JAVACALL_SERIAL_H_
#define __JAVACALL_SERIAL_H_

/**
 * @file javacall_serial.h
 * @ingroup Serial
 * @brief Javacall interfaces for serial port
 */

#ifdef __cplusplus
extern "C" {
#endif

#include "javacall_defs.h"

/**
 * @defgroup Serial Serial Port API
 * @ingroup IMPNG
 *
 * NOTE: The following functions are optional.
 *
 * Optional API to support serial COM communication.
 *
 * @{
 */

/******************************************************************************
 ******************************************************************************
 ******************************************************************************
    OPTIONAL FUNCTIONS
 ******************************************************************************
 ******************************************************************************
 ******************************************************************************/

/**
 * @defgroup OptionalSerial Optional Serial
 * @ingroup Serial
 * @{
 */

/**
 * @enum javacall_serial_signal_type
 * @brief serial port DTE/DCE signal types
 */
typedef enum {
    /** DTR signal */
    DTR_SIGNAL = 1,
    /** DCD signal */
    DCD_SIGNAL = 2,
    /** DSR signal */
    DSR_SIGNAL = 4,
    /** RI signal */
    RI_SIGNAL = 8,
    /** RTS signal */
    RTS_SIGNAL = 16,
    /** CTS signal */
    CTS_SIGNAL = 32
} javacall_serial_signal_type;

/**
 * @enum javacall_serial_signal_type
 * @brief serial port DTE/DCE signal types
 */
typedef enum {
    /** Signal line is not supported. */
    UNSUPPORTED = -1,

    /** Signal line is in input mode. */
    INPUT_MODE  =  0,

    /** Signal line is in output mode. */
    OUTPUT_MODE =  1
} javacall_serial_signal_line_mode;

/**
 * @defgroup CommOptions COMM options
 * @ingroup OptionalSerial
 *
 * @{
 */
/** Stop bits */
#define JAVACALL_SERIAL_STOP_BITS_2     0x01
/** Odd parity */
#define JAVACALL_SERIAL_ODD_PARITY      0x02
/** Even parity */
#define JAVACALL_SERIAL_EVEN_PARITY     0x04
/** Auto RTS */
#define JAVACALL_SERIAL_AUTO_RTS        0x10
/** Auto CTS */
#define JAVACALL_SERIAL_AUTO_CTS        0x20
/** 7-bit chars */
#define JAVACALL_SERIAL_BITS_PER_CHAR_7 0x80
/** 8-bit chars */
#define JAVACALL_SERIAL_BITS_PER_CHAR_8 0xC0
/** 9-bit chars */
#define JAVACALL_SERIAL_BITS_PER_CHAR_9 0x100
/** @} */

/** Unspecified baud rate */
#define JAVACALL_UNSPECIFIED_BAUD_RATE  -1

/**
 * RTS/CTS (hardware) flow control on input.
 */
#define FLOWCONTROL_RTSCTS_IN 1
/**
  * RTS/CTS (hardware) flow control on output.
  */
#define FLOWCONTROL_RTSCTS_OUT 2
/**
 * XON/XOFF (software) flow control on input.
 */
#define FLOWCONTROL_XONXOFF_IN 4
/**
  * XON/XOFF (software) flow control on output.
  */
#define FLOWCONTROL_XONXOFF_OUT 8


/**
 * Return an string the contains a list of available ports delimited by a comma
 * (COM1,COM2)
 * If there is no available port then buffer will be empty string and return JAVACALL OK.
 *
 * @param buffer lists of available ports.This value must be null terminated.
 * @param maxBufferLen the maximum length of buffer
 * @retval JAVACALL_OK success
 * @retval JAVACALL_FAIL fail or the return length is more than maxBufferLen characters.
 */
javacall_result /*OPTIONAL*/  javacall_serial_list_available_ports(char* buffer, int maxBufLen);


/**
 * Initiates opening serial link according to the given parameters.
 *
 * @param devName the name of the port / device to be opened ("COM1")
 * @param baudRate the baud rate for the open connection.
 * @param options the serial link option (JAVACALL_SERIAL_XXX)
 * @param pHandle the handle of the port to be opend
 * @param pContext filled by ptr to data for reinvocations
 * @retval JAVACALL_OK          success
 * @retval JAVACALL_FAIL        if there was an error
 * @retval JAVACALL_WOULD_BLOCK  if the caller must call the finish function again to complete the operation
 */
javacall_result  /*OPTIONAL*/ javacall_serial_open_start(const char *devName, int baudRate, unsigned int options,
  /*OUT*/javacall_handle *pHandle, void **pContext);

/**
 * Finishes opening serial link according to the given parameters
 *
 * @param pHandle the handle of the port to be opend
 * @param context ptr to data saved before sleeping
 * @retval JAVACALL_OK on success,
 * @retval JAVACALL_FAIL on error
 * @retval JAVACALL_WOULD_BLOCK  if the caller must call the finish function again to complete the operation
 */
javacall_result /*OPTIONAL*/ javacall_serial_open_finish(javacall_handle *pHandle, void *context);

/**
 * Update the baudRate of an open serial port
 *
 * @param hPort the port to configure
 * @param baudRate the new baud rate for the open connection
 * @return <tt>JAVACALL_OK</tt> on success,
 *         <tt>JAVACALL_FAIL</tt> on error
 */
javacall_result /*OPTIONAL*/ javacall_serial_set_baudRate(javacall_handle pHandle, int baudRate);

/**
 * Retrive the current baudRate of the open serial port
 *
 * @param hPort the port to configure
 * @param baudRate pointer to where to return the baudRate
 * @return <tt>JAVACALL_OK</tt> on success,
 *         <tt>JAVACALL_FAIL</tt> on error
 */
javacall_result /*OPTIONAL*/ javacall_serial_get_baudRate(javacall_handle hPort, /*OUT*/ int *baudRate);

/**
 * Configure serial port
 *
 * @param hPort the port to configure
 * @param baudRate the new baud rate for the open connection
 * @param options options for the serial port:
 * bit 0: 0 - 1 stop bit, 1 - 2 stop bits
 * bit 2-1: 00 - no parity, 01 - odd parity, 10 - even parity
 * bit 4: 0 - no auto RTS, 1 - set auto RTS
 * bit 5: 0 - no auto CTS, 1 - set auto CTS
 * bit 7-6: 01 - 7 bits per symbol, 11 - 8 bits per symbol
 * @return <tt>JAVACALL_OK</tt> on success,
 *         <tt>JAVACALL_FAIL</tt> on error
 */
javacall_result /*OPTIONAL*/ javacall_serial_configure(javacall_handle pHandle, int baudRate, int options);

/**
 * Initiates closing serial link.
 *
 * @param hPort the port to close
 * @param pContext filled by ptr to data for reinvocations
 * after this call, java is guaranteed not to call javacall_serial_read() or
 * javacall_serial_write() before issuing another javacall_serial_open( ) call.
 *
 * @retval <tt>JAVACALL_OK</tt> on success,
 * @retval <tt>JAVACALL_FAIL</tt>
 * @retval JAVACALL_WOULD_BLOCK  if the caller must call the finish function again to complete the operation
 */
javacall_result /*OPTIONAL*/ javacall_serial_close_start(javacall_handle hPort, void **pContext);

/**
 * Finishes closing serial link.
 *
 * @param hPort the port to close
 * @param context ptr to data saved before sleeping
 * @retval <tt>JAVACALL_OK</tt> on success,
 * @retval <tt>JAVACALL_FAIL</tt>
 * @retval JAVACALL_WOULD_BLOCK  if the caller must call the finish function again to complete the operation
 */
javacall_result /*OPTIONAL*/ javacall_serial_close_finish(javacall_handle hPort, void *context);

/**
 * Initiates reading a specified number of bytes from serial link,

 * @param hPort the port to read the data from
 * @param buffer to which data is read
 * @param size number of bytes to be read. Actual number of bytes
 *              read may be less, if less data is available
 * @param bytesRead actual number the were read from the port.
 * @param pContext filled by ptr to data for reinvocations
 * @retval JAVACALL_OK          success
 * @retval JAVACALL_FAIL        if there was an error
 * @retval JAVACALL_WOULD_BLOCK  if the caller must call the finish function again to complete the operation
 */
javacall_result /*OPTIONAL*/ javacall_serial_read_start(javacall_handle hPort, unsigned char* buffer,
  int size ,/*OUT*/int *bytesRead, void **pContext);

/**
 * Reads available data from the serial port, should exits as fast as it possible
 * this function is mostly about copying availalble data from the port
 * native buffer to a java buffer
 *
 * @param hPort the port to read the data from
 * @param buffer to which data is read
 * @param size number of bytes to be read.
 * @param bytesRead actual number the were read from the port.
 * @param bytesAvailable bytes more availalbe
 * @retval JAVACALL_OK          success
 * @retval JAVACALL_FAIL        if there was an error
 */
javacall_result /*OPTIONAL*/ javacall_serial_read_asynch(javacall_handle hPort, unsigned char* buffer,
  int size ,/*OUT*/int *bytesRead,/*OUT*/int *bytesAvailable);

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
 * @retval JAVACALL_OK          success
 * @retval JAVACALL_FAIL        if there was an error
 * @retval JAVACALL_WOULD_BLOCK  if the caller must call the finish function again to complete the operation
 */
javacall_result /*OPTIONAL*/ javacall_serial_read_finish(javacall_handle hPort, unsigned char* buffer,
  int size, int *bytesRead, void *context);

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
javacall_result /*OPTIONAL*/ javacall_serial_write_start(javacall_handle hPort, unsigned char* buffer,
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
javacall_result /*OPTIONAL*/ javacall_serial_write_finish(javacall_handle hPort, unsigned char* buffer,
  int size, int *bytesWritten, void *context);

/**
 * Gets the number of bytes available to be read from the platform-specific
 * serial port without causing the system to block. If it is not possible to find
 * out the actual number of available bytes then the resulting number is 0.
 *
 * @param handle handle of an open connection
 * @param pBytesAvailable returns the number of available bytes
 *
 * @retval JAVACALL_OK      success
 * @retval JAVACALL_FAIL    if there was an error
 */
javacall_result /*OPTIONAL*/ javacall_serial_available(javacall_handle handle, int *pBytesAvailable);

/**
 * Returns signal line mode
 *
 * @param handle serial port handle
 * @param signal DCE signal type
 * @param mode signal line mode: UNSUPPORTED, INPUT_MODE, OUTPUT_MODE
 *
 * @retval JAVACALL_OK if no error
 */
javacall_result /*OPTIONAL*/ javacall_serial_get_signal_line_mode(javacall_handle handle,
                                                                  javacall_serial_signal_type signal,
                                                                  javacall_serial_signal_line_mode *mode);

/**
 * Sets DTE signal to given state
 *
 * @param handle serial port handle
 * @param signal DTE signal type
 * @param value value
 *
 * @retval JAVACALL_OK if no error
 */
javacall_result /*OPTIONAL*/
javacall_serial_set_dte_signal(javacall_handle handle, javacall_serial_signal_type signal, javacall_bool value);

/**
 * Gets DCE signal status
 *
 * @param handle serial port handle
 * @param signal DCE signal type
 * @param value pointer to store signal state
 *
 * @retval JAVACALL_OK if no error
 */
javacall_result /*OPTIONAL*/
javacall_serial_get_dce_signal(javacall_handle handle, javacall_serial_signal_type signal, javacall_bool* value);

/**
 * Starts listening for DCE signal changes.
 * <p/>
 * Notification is done through {@link #javanotify_serial_event}
 *
 * @param handle serial port handle
 * @param owner handle of process to receive event
 * @param context pointer to control structure of witing context
 *
 * @retval JAVACALL_OK if no error
 */
javacall_result /*OPTIONAL*/
javacall_serial_start_dce_signal_listening(javacall_handle handle, javacall_handle owner, javacall_handle* context);

/**
 * Stops listening for DCE signal changes.
 *
 * @param handle control structure of witing context
 *
 * @retval JAVACALL_OK if no error
 */
javacall_result /*OPTIONAL*/
javacall_serial_stop_dce_signal_listening(javacall_handle context);

/******************************************************************************
 ******************************************************************************
 ******************************************************************************

  NOTIFICATION FUNCTIONS
  - - - -  - - - - - - -
  The following functions are implemented by Sun.
  Platform is required to invoke these function for each occurence of the
  undelying event.
  The functions must be executed in platform's task/thread

 ******************************************************************************
 ******************************************************************************
 ******************************************************************************/

/**
 * @defgroup Notification functions
 * @ingroup Port
 * @{
 */

/**
 * @enum javacall_serial_callback_type
 * @brief callback type used in javanotify_serial_event
 */
typedef enum {
    /** Serial received data */
    JAVACALL_EVENT_SERIAL_RECEIVE = 1000,
    /** Serial wrote data */
    JAVACALL_EVENT_SERIAL_WRITE   = 1001,
    /** Serial port opened */
    JAVACALL_EVENT_SERIAL_OPEN    = 1002,
    /** Serial port closed */
    JAVACALL_EVENT_SERIAL_CLOSE   = 1003
} javacall_serial_callback_type;

/**
 * A callback function to be called for notification of nonblocking
 * serial communication related events.
 * The platform will invoke the call back in platform context for
 * serial related occurrence.
 *
 * @param type type of indication: Either
 *          -JAVACALL_EVENT_SERIAL_RECEIVE = 1000,
 *          -JAVACALL_EVENT_SERIAL_WRITE,
 *          -JAVACALL_EVENT_SERIAL_OPEN
 *
 * @param operation_result operation result: Either
 *          - JAVACALL_OK if operation completed successfully,
 *          - otherwise, JAVACALL_FAIL
*/
void javanotify_serial_event(
    javacall_serial_callback_type  type,
    javacall_handle                hPort,
    javacall_result                operation_result);

/**
 * A callback function to be called for notification of DTE/DCE
 * signal status
 *
 *
 * @param hPort     serial port handle
 * @param target    the application (isolate) to recevice signal
 * @param signal    signal type
 * @param value     signal value
 */
void javanotify_serial_signal(javacall_handle hPort,
                              javacall_handle target,
                              javacall_serial_signal_type signal,
                              javacall_bool value);

#ifdef __cplusplus
}
#endif

#endif

