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

#include "serial.h"
#include "javacall_uart.h"

typedef enum {
    SERIAL_NONE         = 0,
    SERIAL_IN_AVAILABLE = 1,
    SERIAL_IN_OVERRUN   = 2,
    SERIAL_OUT_EMPTY    = 4
} SERIAL_EVENT;

typedef struct uart_desc{
    SERIAL_DESC              serial_descr;
    volatile int             subscribedEvents;
    volatile int             notifiedEvents;
} UART_DESC, *uart_handle;

typedef void *(thread_func)(void*);

static javacall_dio_result uart_open(const char *devName, serial_handle p);

static void _write_complete_cb(javacall_handle handle, int param, javacall_result res) {
    uart_handle port = (uart_handle)handle;
    if (port->subscribedEvents & SERIAL_OUT_EMPTY) {
        if(!(port->notifiedEvents & SERIAL_OUT_EMPTY)){
            javanotify_uart_event(OUTPUT_BUFFER_EMPTY, handle, param, res);
            port->notifiedEvents |= SERIAL_OUT_EMPTY;
        }
    }else{
        javanotify_serial_event(JAVACALL_EVENT_SERIAL_WRITE, handle, res);
    }
}

static void _buffer_overrun_cb(javacall_handle handle, int param, javacall_result error) {
    uart_handle port = (uart_handle)handle;
    if ( (port->subscribedEvents & SERIAL_IN_OVERRUN) && !(port->notifiedEvents & SERIAL_IN_OVERRUN)) {
        javanotify_uart_event(INPUT_BUFFER_OVERRUN, (javacall_handle) port, 0, error);
        port->notifiedEvents |= SERIAL_IN_OVERRUN;
    }
}

static void _new_data_avail_cb(javacall_handle handle, int param, javacall_result res) {
    uart_handle port = (uart_handle)handle;
    if ( port->subscribedEvents & SERIAL_IN_AVAILABLE){
        if(!(port->notifiedEvents & SERIAL_IN_AVAILABLE)) {
            javanotify_uart_event(INPUT_DATA_AVAILABLE, (javacall_handle) port, param, res);
            port->notifiedEvents |= SERIAL_IN_AVAILABLE;
        }
    }else{
    //unblock midp thread
    //never happens on this implementation.
    }
}

inline static javacall_dio_result javacall_result2dio_result(javacall_result result){
    javacall_dio_result dio_result;
    switch(result){
        case JAVACALL_OK: dio_result = JAVACALL_DIO_OK;break;
        case JAVACALL_FAIL: dio_result = JAVACALL_DIO_FAIL;break;
        case JAVACALL_WOULD_BLOCK: dio_result = JAVACALL_DIO_WOULD_BLOCK;break;
        default: dio_result= JAVACALL_DIO_FAIL;
    }
    return dio_result;
}


/**
 * See javacall_uart.h for definition
 */
javacall_dio_result
javacall_uart_close_start(javacall_handle handle,
        void **pContext) {
    return javacall_result2dio_result(javacall_serial_close_start(handle, pContext));
}

/**
 * See javacall_uart.h for definition
 */
javacall_dio_result
javacall_uart_close_finish(javacall_handle hPort,
        void *context) {

    return javacall_result2dio_result(javacall_serial_close_finish(hPort, context));
}


/**
 * See javacall_uart.h for definition
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_read_start(javacall_handle handle,
        unsigned char* buffer, int size,/*OUT*/int *bytesRead,
        void **pContext) {

    int bytesAvailable;

    pthread_mutex_lock( &((serial_handle)handle)->lock );

    if (JAVACALL_FAIL == jc_serial_read_common((serial_handle)handle, buffer, size, bytesRead, &bytesAvailable)){
        JAVACALL_REPORT_INFO(JC_SERIAL, "javacall_serial_read_start: cannot read from the internal buffer");
        return JAVACALL_DIO_FAIL;
    }

    ((uart_handle)handle)->notifiedEvents &= ~SERIAL_IN_AVAILABLE;
    if(bytesAvailable > 0){
        ((serial_handle)handle)->new_data_avail_cb((serial_handle)handle, bytesAvailable, JAVACALL_DIO_OK);
    }
    /*  read from the internal buffer complete, buffer may overrun again
    */
    ((uart_handle)handle)->notifiedEvents &= ~SERIAL_IN_OVERRUN;
    pthread_mutex_unlock( &((serial_handle)handle)->lock );

    return JAVACALL_DIO_OK;
}


/**
 * See javacall_uart.h for definition
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_read_finish(javacall_handle handle,
        unsigned char* buffer, int size, int *bytesRead,
        void *context) {
    //cannot be called in this implementation
    return JAVACALL_DIO_FAIL;

}

/**
 * See javacall_uart.h for definition
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_write_start(javacall_handle handle,
        unsigned char* buffer, int size, int *bytesWritten,
        void **pContext) {

    ((serial_handle)handle)->out_total_written = 0;

        ((uart_handle)handle)->notifiedEvents &= ~SERIAL_OUT_EMPTY;

    javacall_result result = jc_serial_write_common(handle, buffer, size, bytesWritten);

    if(JAVACALL_WOULD_BLOCK == result && ((uart_handle)handle)->subscribedEvents & SERIAL_OUT_EMPTY){
        //case of asynch operation no WOULD_BLOCK
        result = JAVACALL_OK;
    }
    return javacall_result2dio_result(result);
}

/**
 * See javacall_uart.h for definition
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_write_finish(javacall_handle handle,
        unsigned char* buffer, int size, int *bytesWritten,
        void *context) {

    javacall_result result = jc_serial_write_common(handle, buffer, size, bytesWritten);

    if(JAVACALL_WOULD_BLOCK == result && ((uart_handle)handle)->subscribedEvents & SERIAL_OUT_EMPTY){
        // case of asynch operation no WOULD_BLOCK
        result = JAVACALL_OK;
    }
    return javacall_result2dio_result(result);
}

/**
 * See javacall_uart.h for definition
 */
javacall_dio_result
javacall_uart_start_event_listening(javacall_handle handle, javacall_uart_event_type eventId) {

    uart_handle port = (uart_handle)handle;
    javacall_int32 dataAvailable=0;
    javacall_int32 freeSize=0;

    switch (eventId) {
    case INPUT_DATA_AVAILABLE:
            if (JAVACALL_OK == javautil_circular_buffer_get_count(((serial_handle)port)->inBuffer, &dataAvailable)){
                //data is in the internal buffer
                port->subscribedEvents |= SERIAL_IN_AVAILABLE;
                port->notifiedEvents  &= ~SERIAL_IN_AVAILABLE;
                if(dataAvailable > 0){
                    ((serial_handle)port)->new_data_avail_cb(port, dataAvailable, JAVACALL_DIO_OK);
                }
            }else{
                JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] cannot start listening INPUT_DATA_AVAILABLE");
                return JAVACALL_DIO_FAIL;
            }
            break;

        case INPUT_BUFFER_OVERRUN:
            if (JAVACALL_OK == javautil_circular_buffer_free_size(((serial_handle)port)->inBuffer, &freeSize)){
                port->subscribedEvents |= SERIAL_IN_OVERRUN;
                port->notifiedEvents  &= ~SERIAL_IN_OVERRUN;

                if(0 == freeSize){
                    ((serial_handle)port)->buffer_overrun_cb(port, 0, JAVACALL_DIO_OK);
                }
            }else{
                JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] cannot start listening INPUT_BUFFER_OVERRUN");
                return JAVACALL_DIO_FAIL;
            }
            break;

    case OUTPUT_BUFFER_EMPTY:
            port->subscribedEvents |= SERIAL_OUT_EMPTY;
            port->notifiedEvents   &= ~SERIAL_OUT_EMPTY;
            JAVACALL_REPORT_INFO(JC_SERIAL, "[UART] start listening OUTPUT_BUFFER_EMPTY");
            break;
    default:
            JAVACALL_REPORT_INFO1(JC_SERIAL, "[UART] unknown event %d", eventId);
            return JAVACALL_DIO_UNSUPPORTED_OPERATION;
    };

    return JAVACALL_DIO_OK;
}

/**
 * See javacall_uart.h for definition
 */
javacall_dio_result
javacall_uart_stop_event_listening(javacall_handle handle, javacall_uart_event_type eventId) {

    uart_handle p = (uart_handle)handle;
    switch (eventId) {
        case INPUT_DATA_AVAILABLE:
            p->subscribedEvents &= ~SERIAL_IN_AVAILABLE;
            p->notifiedEvents   &= ~SERIAL_IN_AVAILABLE;
            break;
        case INPUT_BUFFER_OVERRUN:
            p->subscribedEvents &= ~SERIAL_IN_OVERRUN;
            p->notifiedEvents   &= ~SERIAL_IN_OVERRUN;
            break;
        case OUTPUT_BUFFER_EMPTY:
            p->subscribedEvents &= ~SERIAL_OUT_EMPTY;
            p->notifiedEvents   &= ~SERIAL_OUT_EMPTY;
            break;
        default:
            JAVACALL_REPORT_INFO1(JC_SERIAL, "[UART] unknown event %d", eventId);
            return JAVACALL_DIO_UNSUPPORTED_OPERATION;
    };

    return JAVACALL_DIO_OK;
}

static javacall_dio_result uart_open(const char *devName, serial_handle p) {

    return javacall_result2dio_result(jc_serial_open(devName, p));
}

/**
 * Update the current stopBits of the open serial port
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_set_stop_bits(javacall_handle handle, javacall_uart_stop_bits stopBits){
    struct termios term;
    if(getTermios(handle, &term) != JAVACALL_OK){
        return JAVACALL_DIO_FAIL;
    }
    cfmakeraw(&term);

    //IMPL_NOTE: linux termios defines 1 or 2 stopbits only
    switch(stopBits){
        case STOPBITS_1: term.c_cflag &= ~CSTOPB;
            break;
        case STOPBITS_2: term.c_cflag |= CSTOPB;
            break;
        default:
            JAVACALL_REPORT_ERROR1(JC_SERIAL, "[UART] unsupported stop bits parameter %d", stopBits);
            return JAVACALL_DIO_UNSUPPORTED_OPERATION;
    }
    return javacall_result2dio_result(setTermios(handle, &term));
}

/**
 * Retrive the current baudRate of the open serial port
 *
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_get_baudRate(javacall_handle handle, /*OUT*/ int *baudRate){

    struct termios term;
    if(getTermios(handle, &term) != JAVACALL_OK){
        return JAVACALL_DIO_FAIL;
    }

    if (-1 == (*baudRate = baud_to_int(cfgetospeed(&term)))) {
        JAVACALL_REPORT_ERROR1(JC_SERIAL, "[UART] baud_to_int failed for %d",
        cfgetospeed(&term));
        return JAVACALL_DIO_FAIL;
    }
    return JAVACALL_DIO_OK;
}

/**
 * Update the parity of an open serial port
 *
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_set_parity(javacall_handle handle, javacall_uart_parity parity){

    struct termios term;
    if(getTermios(handle, &term) != JAVACALL_OK){
        return JAVACALL_DIO_FAIL;
    }

    switch(parity){
        case UART_PARITY_NONE: term.c_cflag &= ~PARENB;
            break;
        case UART_PARITY_ODD:  term.c_cflag |= PARENB; term.c_cflag |= PARODD;
            break;
        case UART_PARITY_EVEN:  term.c_cflag |= PARENB; term.c_cflag &= ~PARODD;
            break;
        default:
             JAVACALL_REPORT_ERROR1(JC_SERIAL, "[UART] unknown parity parameter %d", parity);
             return JAVACALL_DIO_UNSUPPORTED_OPERATION;
    }
    return javacall_result2dio_result(setTermios(handle, &term));
}

/**
 * Retrive the current parity of the open serial port
 *
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_get_parity(javacall_handle handle, /*OUT*/ javacall_uart_parity *parity){
    struct termios term;
    if(getTermios(handle, &term) != JAVACALL_OK){
        return JAVACALL_DIO_FAIL;
    }

    if (term.c_cflag & PARENB){
        if(term.c_cflag & PARODD){
            *parity = UART_PARITY_ODD;
        }else{
            *parity = UART_PARITY_EVEN;
        }
    }else{
        *parity = UART_PARITY_NONE;
    }
    return JAVACALL_DIO_OK;
}

/**
 * Retrive the current stopBits of the open serial port
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_get_stop_bits(javacall_handle handle, javacall_uart_stop_bits *stopBits){
    struct termios term;
    if(getTermios(handle, &term) != JAVACALL_OK){
        return JAVACALL_DIO_FAIL;
    }

    if (term.c_cflag & CSTOPB){
        *stopBits = STOPBITS_2;
    }else{
        *stopBits = STOPBITS_1;
    }
    return JAVACALL_DIO_OK;
}

/**
 * Update the bitsPerChar of an open serial port
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_set_bits_per_char(javacall_handle handle, javacall_uart_bits_per_char bitsPerChar){
    struct termios term;

    if(getTermios(handle, &term) != JAVACALL_OK){
        return JAVACALL_DIO_FAIL;
    }
    //IMPL_NOTE: linux PI 7 and 8 supported
    switch(bitsPerChar){
        case BITS_PER_CHAR_7: term.c_cflag = (term.c_cflag & ~CSIZE) | CS7;
            break;
        case BITS_PER_CHAR_8: term.c_cflag = (term.c_cflag & ~CSIZE) | CS8;
            break;
        case BITS_PER_CHAR_5:
        case BITS_PER_CHAR_6:
        default:
            JAVACALL_REPORT_ERROR1(JC_SERIAL, "[UART] unsupported bitsperchar value %d", bitsPerChar);
            return JAVACALL_DIO_UNSUPPORTED_OPERATION;
    }

    return javacall_result2dio_result(setTermios(handle, &term));
}

/**
 * Retrive the bits per char of the open serial port
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_get_bits_per_char(javacall_handle handle, /*OUT*/ javacall_uart_bits_per_char *bitsPerChar){
    struct termios term;
    if(getTermios(handle, &term) != JAVACALL_OK){
        return JAVACALL_DIO_FAIL;
    }


    switch(term.c_cflag&CSIZE){
        case CS7:
            *bitsPerChar = BITS_PER_CHAR_7; break;
        case CS8:
            *bitsPerChar = BITS_PER_CHAR_8; break;
        default:
            JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] tcgetattr returns unknown bitsPerchar value");
            return JAVACALL_DIO_FAIL;
    }
    return JAVACALL_DIO_OK;
}

javacall_dio_result /*OPTIONAL*/
javacall_uart_open_start(const char *devName, unsigned int baudRate, javacall_uart_stop_bits stopBits, \
                   unsigned int flowControl, javacall_uart_bits_per_char bitsPerchar, \
                   javacall_uart_parity parity, const javacall_bool exclusive,
                   /*OUT*/javacall_handle *pHandle){
    int size;
    serial_handle p;
    javacall_result res;

    NOT_USED(exclusive);
    p = (serial_handle)javacall_malloc(sizeof(UART_DESC));

    if (NULL == p) {
        JAVACALL_REPORT_ERROR(JC_DIO,
                "[UART] malloc error while creating port descriptor");
        return JAVACALL_DIO_OUT_OF_MEMORY;
    }
    memset(p, 0, sizeof(UART_DESC));

    // open port
    if ( JAVACALL_DIO_OK != (res = uart_open(devName, p)) ) {
        javacall_free(p);
        return res;
    }

    //setting daapi parameters
    if (JAVACALL_OK != javacall_serial_set_baudRate((javacall_handle)p, baudRate) ){
        javacall_uart_close_start(p, NULL);
        return JAVACALL_DIO_UNSUPPORTED_OPERATION;
    }

    if (JAVACALL_DIO_OK != javacall_uart_set_bits_per_char((javacall_handle)p, bitsPerchar)){
        javacall_uart_close_start(p, NULL);
        return JAVACALL_DIO_UNSUPPORTED_OPERATION;
    }

    if (JAVACALL_DIO_OK != javacall_uart_set_stop_bits((javacall_handle)p, stopBits)){
        javacall_uart_close_start(p, NULL);
        return JAVACALL_DIO_UNSUPPORTED_OPERATION;
    }

    if (JAVACALL_DIO_OK != javacall_uart_set_parity((javacall_handle)p, parity)){
        javacall_uart_close_start(p, NULL);
        return JAVACALL_DIO_UNSUPPORTED_OPERATION;
    }

    if(JAVACALL_DIO_OK != jc_serial_init_buffers_threads(p)){
        javacall_uart_close_start(p, NULL);
        return JAVACALL_DIO_FAIL;
    }

    p->buffer_overrun_cb = _buffer_overrun_cb;
    p->new_data_avail_cb = _new_data_avail_cb;
    p->write_complete_cb = _write_complete_cb;

    *pHandle = (javacall_handle)p;

    return JAVACALL_DIO_OK;
}

/**
* See javacall_uart.h for definition
*/
javacall_dio_result
javacall_uart_open_finish(const char *devName, unsigned int baudRate,
                         javacall_uart_stop_bits stopBits, unsigned int flowControl,
                         javacall_uart_bits_per_char bitsPerchar, javacall_uart_parity parity,
                         /*OUT*/javacall_handle *pHandle) {
    NOT_USED(devName);
    NOT_USED(baudRate);
    NOT_USED(stopBits);
    NOT_USED(flowControl);
    NOT_USED(bitsPerchar);
    NOT_USED(parity);
    NOT_USED(pHandle);
    return JAVACALL_DIO_UNSUPPORTED_OPERATION;
}

/**
 * Stops write operations if any pending
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_stop_writing(javacall_handle handle){
        void *value;
    serial_handle p = (serial_handle)handle;

    uint64_t c = 1;
    int r;
    if(0 != p->write_thread){
        if(0 != pthread_mutex_trylock(&p->write_lock)){
            //writing is onging
            r = write(p->event_fd, &c, sizeof(uint64_t));
        }else{
            //no write activity
            pthread_mutex_unlock(&p->write_lock);
        }
    }
    return JAVACALL_DIO_OK;
}

/**
 * Stops read operations if any pending
 */
javacall_dio_result /*OPTIONAL*/
javacall_uart_stop_reading(javacall_handle handle){
    ((uart_handle)handle)->subscribedEvents &= ~SERIAL_IN_AVAILABLE;
    ((uart_handle)handle)->notifiedEvents   &= ~SERIAL_IN_AVAILABLE;

    return JAVACALL_DIO_OK;
}

/**
 * Attempts to lock for exclusive access the underlying
 * peripheral device resource.
 */
javacall_dio_result
javacall_uart_lock(const javacall_handle handle, javacall_handle* const owner){
    (void)handle;
    (void)owner;
    // EXCLUSIVE mode is supported only
    return JAVACALL_DIO_OK;
}

/**
 * Releases from exclusive access the underlying peripheral
 * device resource.
 *
 */
javacall_dio_result
javacall_uart_unlock(const javacall_handle handle){
    (void)handle;
    // EXCLUSIVE mode is supported only
    return JAVACALL_DIO_OK;
}

/**
 * Returns power control group of this channel. It is used for
 * power management notification.
 */
javacall_dio_result
javacall_uart_get_group_id(const javacall_handle handle, javacall_int32* const  grp){
    *grp = -1;
    return JAVACALL_DIO_UNSUPPORTED_OPERATION;
}


