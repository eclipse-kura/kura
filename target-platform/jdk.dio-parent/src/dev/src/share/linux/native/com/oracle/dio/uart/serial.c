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

#include "serial.h"

typedef void *(thread_func)(void*);

static void*           write_thread(void* arg);
static void*           in_poll_thread(void* arg);

static javacall_result serial_create_thread(javacall_handle handle,
        thread_func func, pthread_t *thread);

static inline void cleanup_write_buffer(serial_handle p){
    if(NULL != p->out_buffer){
        javacall_free(p->out_buffer);
        p->out_buffer = NULL;
        p->out_buffer_size = 0;
    }
}

char* skipDelimiters(char* ptr) {
    char ch = *ptr;
    // skip Delimiters
    while (' ' == ch) {
        ptr++;
        ch = *ptr;
     }
    return ptr;
}

/** CLDC spec req: only alphabet and digits */
int checkName(char* name) {
    char ch = *name;
    while (0 != ch) {
        if (!(('a' <= ch && ch <= 'z') ||
              ('A' <= ch && ch <= 'Z') ||
              ('0' <= ch && ch <= '9'))) {
            return 0;
        }
        name++;
        ch = *name;
    }
    return 1;
}

char* getNextDelimiterPtr(char* prefixList) {
    char ch = *prefixList;
    while (0 != ch && ' ' != ch ) {
        prefixList++;
        ch = *prefixList;
    }
    return prefixList;
}

/* fills provided {@code dst} buffer with file names from {@code /dev} folder that matches prefixes from comma separated {@code prefix} list */
void getDeviceList(char* dst, size_t dstLen, char* prefix) {
    int found = 0;
    *dst = 0;
    DIR* dir = opendir("/dev");
    if (dir) {
        struct dirent* de;
        while (NULL != (de = readdir(dir))) {
            if (!(de->d_type & DT_DIR)) {
                char* curPrefix = prefix;
                // retuns curPrefix if no more templates found
                char* nextPrefix = getNextDelimiterPtr(curPrefix);

                while (nextPrefix  != curPrefix) {
                    // compare file name with prefix: wildcard search
                    if(!strncmp(de->d_name, curPrefix, nextPrefix - curPrefix - 1)) {
                        // don't use strncat since we need only full names
                        size_t len = strlen(de->d_name);
                        // +2 is for null and ','
                        if (len+2 < dstLen && checkName(de->d_name)) {
                            if (found) {
                                // it is guaranteed that there is place for 0 or ','
                                *dst = ',';
                                dst++;
                                dstLen--;
                            }
                            memcpy(dst,de->d_name, len);
                            dst += len;
                            dstLen -= len;
                            found = 1;
                        }
                    }
                    curPrefix = skipDelimiters(nextPrefix);
                    nextPrefix = getNextDelimiterPtr(curPrefix);
                }
                *dst = 0;
            }
        }
        closedir(dir);
    }
}

/**
 * See javacall_serial.h for definition
 */
javacall_result javacall_serial_list_available_ports(char* buffer,
        int maxBufLen) {

    int i, len, totalCount;
    char *devPrefix;
    javacall_result res;

    JAVACALL_REPORT_INFO(JC_SERIAL, "Checking what serial ports are available");

    if (JAVACALL_OK != javacall_get_property("deviceaccess.uart.prefix",
                JAVACALL_INTERNAL_PROPERTY, &devPrefix)) {
            JAVACALL_REPORT_WARN(JC_SERIAL, "Failed to read property 'deviceaccess.uart.prefix'");
        devPrefix = (char*)DEFAULT_PREFIX;
    }

    getDeviceList(buffer, maxBufLen, devPrefix);

    return JAVACALL_OK;
}

javacall_result
jc_serial_init_buffers_threads(serial_handle p){

    p->event_fd = eventfd(0, O_NONBLOCK);
    if (p->event_fd == -1){
        JAVACALL_REPORT_ERROR(JC_DIO,
                "[UART] eventfd error while creating port descriptor");
        return JAVACALL_FAIL;
    }

    if(JAVACALL_OK != javacall_get_property_int(SERIAL_BUFFER_SIZE_PROPERTY_NAME, JAVACALL_INTERNAL_PROPERTY, &p->buffer_max_size)){
        p->buffer_max_size = SERIAL_BUF_SIZE;
    }

    // allocate input buffer
    if(JAVACALL_OK != javautil_circular_buffer_create(&(p->inBuffer), p->buffer_max_size, sizeof(char))){
        JAVACALL_REPORT_ERROR(JC_DIO, "[UART] income buffer initialization error");
        return JAVACALL_FAIL;
    }

    // initialize mutex
    if ( pthread_mutex_init(&(p->lock), NULL) != 0 ) {
        JAVACALL_REPORT_ERROR(JC_DIO, "[UART] mutex initialization error");
        return JAVACALL_FAIL;
    }

    // create polling thread
    if(JAVACALL_OK != serial_create_thread(p, in_poll_thread, &(p->inPollThread))){
        return JAVACALL_FAIL;
    }

    return JAVACALL_OK;
}

/* stubbed callbacks */
static void _write_complete_cb(javacall_handle handle, int param, javacall_result res) {
    javanotify_serial_event(JAVACALL_EVENT_SERIAL_WRITE, handle, res);
}

static void _buffer_overrun_cb(javacall_handle handle, int param, javacall_result res) {
}

static void _new_data_avail_cb(javacall_handle handle, int param, javacall_result res) {
    //never happens in this implementation
    //javanotify_serial_event(JAVACALL_EVENT_SERIAL_RECEIVE, handle, res);
}

/**
 * See javacall_serial.h for definition
 */
javacall_result javacall_serial_open_start(const char *devName, int baudRate,
        unsigned int options, javacall_handle *pHandle, void **pContext) {

    int size;
    serial_handle p;
    javacall_result res;

    p = (serial_handle) javacall_malloc( sizeof(SERIAL_DESC) );
    if (NULL == p) {
        JAVACALL_REPORT_ERROR(JC_DIO,
                "[UART] malloc error while creating port descriptor");
        return JAVACALL_OUT_OF_MEMORY;
    }

    memset(p, sizeof(SERIAL_DESC), 0);

    // open port
    if ( JAVACALL_OK != (res = jc_serial_open(devName, p)) ) {
        javacall_free(p);
        return JAVACALL_FAIL;
    }

    // configure port
    if ( JAVACALL_OK != javacall_serial_configure((javacall_handle)p, baudRate, options)){
        JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] javacall_serial_configure failed");
        javacall_serial_close_start(p, NULL);
        return JAVACALL_FAIL;
    }

    if(JAVACALL_OK != jc_serial_init_buffers_threads(p)){
        JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] jc_serial_init_buffers_threads failed");
        javacall_serial_close_start(p, NULL);
        return JAVACALL_FAIL;
    }

    p->buffer_overrun_cb = _buffer_overrun_cb;
    p->new_data_avail_cb = _new_data_avail_cb;
    p->write_complete_cb = _write_complete_cb;

    *pHandle = (javacall_handle)p;

    return JAVACALL_OK;
}

/**
 * See javacall_serial.h for definition
 */
javacall_result javacall_serial_open_finish(javacall_handle *pHandle,
        void *context) {
    return JAVACALL_FAIL;
}

/**
 * See javacall_serial.h for definition
 */
javacall_result javacall_serial_set_baudRate(javacall_handle handle,
        int baudRate) {
    struct termios term;
    speed_t baud;
    int fd = ((serial_handle)handle)->fd;

    if (-1 == (baud = int_to_baud(baudRate))) {
        JAVACALL_REPORT_ERROR1(JC_SERIAL,
                "[UART] Unsupported baudRate value: %d", baudRate);
        return JAVACALL_FAIL;
    }
    if (tcgetattr(fd, &term)) {
        JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] tcgetattr failed");
        return JAVACALL_FAIL;
    }
    if (cfsetospeed(&term, baud)) {
        JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] cfsetospeed failed");
        return JAVACALL_FAIL;
    }
    if (cfsetispeed(&term, baud)) {
        JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] cfsetispeed failed");
        return JAVACALL_FAIL;
    }
    if (tcsetattr(fd, TCSANOW, &term)) {
        JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] tcsetattr failed");
        return JAVACALL_FAIL;
    }

    return JAVACALL_OK;
}

/**
 * See javacall_serial.h for definition
 */
javacall_result javacall_serial_get_baudRate(javacall_handle handle,
        int *baudRate) {

    struct termios term;
    if (tcgetattr(((serial_handle)handle)->fd, &term)) {
        JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] tcgetattr failed");
        return JAVACALL_FAIL;
    }
    if (-1 == (*baudRate = baud_to_int(cfgetospeed(&term)))) {
        JAVACALL_REPORT_ERROR1(JC_SERIAL, "[UART] baud_to_int failed for %d",
                cfgetospeed(&term));
        return JAVACALL_FAIL;
    }
    return JAVACALL_OK;
}

/**
 * See javacall_serial.h for definition
 */

javacall_result javacall_serial_close_start(javacall_handle handle,
        void **pContext) {

    void *value;
    int rv;

    serial_handle p = (serial_handle)handle;
    if(0 != p->inPollThread){
        if ( 0 != pthread_cancel(p->inPollThread) ||
             0 != pthread_join(p->inPollThread, &value) || PTHREAD_CANCELED != value ) {
            JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] pollin thread cancellation error");
        }
        p->inPollThread = 0;
    }

    if(0 != p->write_thread){
        if ( 0 != pthread_cancel(p->write_thread) ||
             0 != pthread_join(p->write_thread, &value) || PTHREAD_CANCELED != value ) {
            JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] write thread cancellation error");
        }
        p->write_thread = 0;
        pthread_mutex_destroy(&(p->write_lock));
        pthread_cond_destroy(&(p->out_buffer_ready));
    }

    /* try to clean all up even if thread cancellation failed */
    pthread_mutex_destroy(&(p->lock));

    while (-1 == (rv = close(p->event_fd)) && EINTR == errno);
    if (rv != 0) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "[UART] cannot close event_fd, errno=%d", errno);
    }

    while (-1 == (rv = close(p->fd)) && EINTR == errno);

    if (rv != 0) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "[UART] cannot close fd, errno=%d", errno);
    }

    cleanup_write_buffer(p);
    javautil_circular_buffer_destroy(p->inBuffer);
    javacall_free(p);

    return JAVACALL_OK;
}

/**
 * See javacall_serial.h for definition
 */
javacall_result javacall_serial_close_finish(javacall_handle hPort,
        void *context) {
    return JAVACALL_FAIL;
}

/**
 * Reads available data from the serial port
 * returns JAVACALL_OK - if OK copy data to the buffer
 *         JAVACALL_FAIL - if any error
*/
javacall_result
jc_serial_read_common(serial_handle p, unsigned char* buffer,
  int bufferSize ,/*OUT*/int *bytesRead,/*OUT*/int *bytesAvailable){

    javacall_result result = JAVACALL_OK;

    *bytesRead = bufferSize;

    result = javautil_circular_buffer_get_array(p->inBuffer, buffer, (javacall_int32*)bytesRead);
    if (result == JAVACALL_FAIL){
       //empty buffer
       *bytesRead = 0;
       *bytesAvailable = 0;
    }else{
        //check if more bytes available
        javautil_circular_buffer_get_count(p->inBuffer, (javacall_int32*)bytesAvailable);
    }

/*
    if(result == JAVACALL_INVALID_ARGUMENT){
        JAVACALL_REPORT_ERROR(JC_SERIAL, "read_common: javautil_circular_buffer_get_array invalid argument\n");
        return JAVACALL_FAIL;
    }
*/
    return JAVACALL_OK;
}

/**
 * See javacall_serial.h for definition
 */
javacall_result /*OPTIONAL*/javacall_serial_read_start(javacall_handle handle,
        unsigned char* buffer, int size,/*OUT*/int *bytesRead,
        void **pContext) {

    int bytesAvailable;
    javacall_result result;

    pthread_mutex_lock( &((serial_handle)handle)->lock);
    result = jc_serial_read_common((serial_handle)handle, buffer, size, bytesRead, &bytesAvailable);
    pthread_mutex_unlock( &((serial_handle)handle)->lock);

    return result;
}


/**
 * See javacall_serial.h for definition
 */
javacall_result /*OPTIONAL*/javacall_serial_read_finish(javacall_handle handle,
        unsigned char* buffer, int size, int *bytesRead,
        void *context) {
    /*
        cannot be called in this implementation, because of _start returns OK or FAIL
    */
    return JAVACALL_FAIL;
}

javacall_result jc_serial_write_common(serial_handle handle,
        unsigned char *buffer, int size, int* bytesWritten) {

    serial_handle p = (serial_handle)handle;
    javacall_result result = JAVACALL_FAIL;

        /*
       write_thread write result stored in out_buffer_offset
           if synch operations
    */
    *bytesWritten = p->out_total_written;
    /*
        all incoming java buffer is written
    */
    if(size == p->out_total_written){
        p->write_complete_cb(p, 0, JAVACALL_OK);
        cleanup_write_buffer(p);
        return JAVACALL_OK;
    }

    p->out_buffer_size = (size - p->out_total_written) > p->buffer_max_size ? p->buffer_max_size : (size - p->out_total_written);

    p->out_buffer = javacall_malloc(p->out_buffer_size);
    if (NULL == p->out_buffer) {
        JAVACALL_REPORT_ERROR(JC_DIO,
                "[UART] malloc error while jc_serial_write_common");
        return JAVACALL_OUT_OF_MEMORY;
    }
    memcpy(p->out_buffer, buffer, p->out_buffer_size);

    if(0 == p->write_thread){
        // initialize write mutex
        if (pthread_mutex_init(&(p->write_lock), NULL) == 0 ) {
            if (pthread_cond_init(&(p->out_buffer_ready), NULL) == 0 ) {
              if(JAVACALL_OK == serial_create_thread(handle, write_thread, &p->write_thread)) {
                result = JAVACALL_WOULD_BLOCK;
              }else{
                JAVACALL_REPORT_ERROR(JC_DIO, "[UART] cannot create writing thread");
              }
            }else{
                JAVACALL_REPORT_ERROR(JC_DIO, "[UART]  condition variable initialization error");
            }
        }else{
            JAVACALL_REPORT_ERROR(JC_DIO, "[UART] mutex initialization error");
        }

        if (result == JAVACALL_FAIL){
            cleanup_write_buffer(p);
        }
    }else{
        pthread_mutex_lock(&p->write_lock);
        pthread_cond_signal(&p->out_buffer_ready);
        pthread_mutex_unlock(&p->write_lock);
        result = JAVACALL_WOULD_BLOCK;
    }
    return result;
}

/**
 * See javacall_serial.h for definition
 */
javacall_result /*OPTIONAL*/javacall_serial_write_start(javacall_handle handle,
        unsigned char* buffer, int size, int *bytesWritten,
        void **pContext) {

    /*
        init new write operation
    */
    ((serial_handle)handle)->out_total_written = 0;

    return jc_serial_write_common(handle, buffer, size, bytesWritten);
}

/**
 * See javacall_serial.h for definition
 */
javacall_result /*OPTIONAL*/javacall_serial_write_finish(javacall_handle handle,
        unsigned char* buffer, int size, int *bytesWritten,
        void *context) {
    serial_handle p = (serial_handle)handle;

    return jc_serial_write_common(handle, buffer, size, bytesWritten);
}

/**
 * See javacall_serial.h for definition
 */
javacall_result /*OPTIONAL*/ javacall_serial_available(javacall_handle handle, int *pBytesAvailable){
    serial_handle p = ((serial_handle)handle);
    javacall_result result;

    pthread_mutex_lock(&p->lock);
    result = javautil_circular_buffer_get_count(p->inBuffer, (javacall_int32 *)pBytesAvailable);
    pthread_mutex_unlock(&p->lock);

    return result;
}

/**
 * See javacall_serial.h for definition
 */
javacall_result /*OPTIONAL*/javacall_serial_configure(javacall_handle handle,
        int baudRate, int options) {
    struct termios term;
    int fd = ((serial_handle)handle)->fd;

    if (javacall_serial_set_baudRate(handle, baudRate) == JAVACALL_FAIL) {
        JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] Set baudRate failed");
        return JAVACALL_FAIL;
    }
    if (tcgetattr(fd, &term)) {
        JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] tcgetattr failed");
        return JAVACALL_FAIL;
    }
    cfmakeraw(&term);
    if (options & JAVACALL_SERIAL_STOP_BITS_2) {
        term.c_cflag |= CSTOPB;
    } else {
        term.c_cflag &= ~CSTOPB;
    }
    if (options & (JAVACALL_SERIAL_ODD_PARITY | JAVACALL_SERIAL_EVEN_PARITY)) {
        term.c_cflag |= PARENB;
        if (options & JAVACALL_SERIAL_ODD_PARITY != 0) {
            term.c_cflag |= PARODD;
        } else {
            term.c_cflag &= ~PARODD;
        }
    } else {
        term.c_cflag &= ~PARENB;
    }
#if 0
    if (options & JAVACALL_SERIAL_AUTO_RTS == 0 || options & JAVACALL_SERIAL_AUTO_CTS == 0) {
        term.c_cflag &= ~CRTSCTS;
    } else {
        term.c_cflag |= CRTSCTS;
    }
#endif
    if (options & JAVACALL_SERIAL_BITS_PER_CHAR_9) {
        JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] 9 bits per char - no way!");
        return JAVACALL_FAIL;
    };
    if (options & JAVACALL_SERIAL_BITS_PER_CHAR_8) {
        term.c_cflag = (term.c_cflag & ~CSIZE) | CS8;
    } else if (options & JAVACALL_SERIAL_BITS_PER_CHAR_7 != 0) {
        term.c_cflag = (term.c_cflag & ~CSIZE) | CS7;
    } else {
        JAVACALL_REPORT_ERROR(JC_SERIAL,
                "[UART] unsupported char size, only 7 and 8 is supported");
        return JAVACALL_FAIL;
    }

    if (tcsetattr(fd, TCSANOW, &term)) {
        JAVACALL_REPORT_ERROR(JC_SERIAL, "[UART] tcsetattr failed");
        return JAVACALL_FAIL;
    }
    return JAVACALL_OK;
}

/**
 * Returns signal line mode
 *
 * @param handle serial port handle
 * @param signal DCE signal type
 * @param mode signal line mode: UNSUPPORTED, INPUT_MODE, OUTPUT_MODE
 *
 * @return JAVACALL_OK if no error
 */
javacall_result /*OPTIONAL*/ javacall_serial_get_signal_line_mode(javacall_handle handle,
                                                                  javacall_serial_signal_type signal,
                                                                  javacall_serial_signal_line_mode *mode) {
    return JAVACALL_NOT_IMPLEMENTED;
}


/**
 * See javacall_serial.h for definition
 */
javacall_result /*OPTIONAL: PART OF DEVICE ACCESS API*/
javacall_serial_set_dte_signal(javacall_handle handle, javacall_serial_signal_type signal, javacall_bool value) {
    return JAVACALL_NOT_IMPLEMENTED;
}

/**
 * See javacall_serial.h for definition
 */
javacall_result /*OPTIONAL: PART OF DEVICE ACCESS API*/
javacall_serial_get_dce_signal(javacall_handle handle, javacall_serial_signal_type signal, javacall_bool* value) {
    return JAVACALL_NOT_IMPLEMENTED;
}

/**
 * See javacall_serial.h for definition
 */
javacall_result /*OPTIONAL: PART OF DEVICE ACCESS API*/
javacall_serial_start_dce_signal_listening(javacall_handle handle, javacall_handle owner, javacall_handle* context) {
    return JAVACALL_NOT_IMPLEMENTED;
}

/**
 * See javacall_serial.h for definition
 */
javacall_result /*OPTIONAL: PART OF DEVICE ACCESS API*/
javacall_serial_stop_dce_signal_listening(javacall_handle context) {
    return JAVACALL_NOT_IMPLEMENTED;
}

void* write_thread(void* arg) {

    int res, s, b;
    uint64_t event;
    struct pollfd pfds[2];
    serial_handle p = (serial_handle)arg;
    javacall_bool error;
    int current_write_operation;
    int current_buffer_offset;


    //wait events for the uart file descriptor: empty and error
    pfds[0].fd     = p->fd;
    pfds[0].events = POLLOUT | POLLERR | POLLHUP | POLLNVAL;
    pfds[0].revents = 0;
    //wait events for the event filedescriptor uart file descriptor: empty and error
    pfds[1].fd     = p->event_fd;
    pfds[1].events = POLLIN | POLLERR | POLLHUP | POLLNVAL;
    pfds[1].revents = 0;

    /*
        locking write_lock out of do{}while(1) is ok, because
        pthread_cond_wait unlocks write_lock
    */
    pthread_mutex_lock(&p->write_lock);

    do{
        current_write_operation = 0;
        current_buffer_offset = 0;

        error = JAVACALL_FALSE;
        while((current_buffer_offset < p->out_buffer_size) && (error!=JAVACALL_TRUE)){
            // request non blocking write
            while ((current_write_operation = write(p->fd, p->out_buffer + current_buffer_offset, p->out_buffer_size - current_buffer_offset)) < 0 && EINTR == errno);
            if (current_write_operation == -1) {
                if (errno != EAGAIN) {
                    JAVACALL_REPORT_ERROR1(JC_SERIAL, "[UART] failed to write: errno=%d", errno);
                    error = JAVACALL_TRUE;
                }//!EAGAIN
            }else{//if (current_write_operation == -1)
                //wait all data is written or an event came to event_fd
                res = poll(pfds, 2, -1);
                if(res != -1){
                    //POLLOUT event from the UARTS's fd
                    if(pfds[0].revents & POLLOUT){
                        current_buffer_offset += current_write_operation;
                    }else if(pfds[0].revents & (POLLERR | POLLHUP | POLLNVAL)){
                        error = JAVACALL_TRUE;
                    }
                    /*
                        event comming from the event_fd
                        stop writing command
                    */
                    if(pfds[1].revents & POLLIN){
                        s = read(pfds[1].fd, &event, sizeof(uint64_t));
                        b = 0;
                        /*request number of copied to the system buffer not yet written bytes*/
                        ioctl(p->fd, TIOCOUTQ, &b);
                        ioctl(p->fd, TCOFLUSH);
                        current_buffer_offset += current_write_operation - b;
                        error = JAVACALL_TRUE;
                    }else if(pfds[1].revents & (POLLERR | POLLHUP | POLLNVAL)){
                        error = JAVACALL_TRUE;
                        JAVACALL_REPORT_ERROR1(JC_SERIAL, "[UART] vent_fd error: errno=%d", errno);
                    }
                }else{//if(res != -1)
                    error = JAVACALL_TRUE;
                }
           }
        }//while
        p->out_total_written += current_buffer_offset;
        //send signal or event in DA
        p->write_complete_cb(p, current_buffer_offset, error==JAVACALL_TRUE?JAVACALL_FAIL:JAVACALL_OK);
        cleanup_write_buffer(p);
        pthread_cond_wait(&p->out_buffer_ready, &p->write_lock);
    }while(1);

    pthread_mutex_unlock(&p->write_lock);

    pthread_exit(0);
    return NULL;
}

void* in_poll_thread(void* arg) {

    int rv;
    javacall_result res;
    serial_handle p = (serial_handle)arg;
    const int buf_sz = 32;
    char buf[buf_sz];
    int num, i;

    struct pollfd pfd;
    pfd.fd     = p->fd;
    pfd.events = POLLIN;

    while ( -1 != (rv = poll(&pfd, 1, -1)) ||
           (-1 == rv && EINTR == errno) ) {

        if (-1 == rv)
            continue;

        while ( (num = read(pfd.fd, buf, buf_sz)) < 0 && EINTR == errno )
            ;

        if (num <= 0)
            continue;

        pthread_mutex_lock( &(p->lock) );

        res = javautil_cicular_buffer_put_array(p->inBuffer, (javacall_handle) buf, num);

        if ( !JAVACALL_SUCCEEDED(res) ) {
            p->buffer_overrun_cb(p, 0,JAVACALL_OK);
        }
        p->new_data_avail_cb(p, 0,JAVACALL_OK);

        pthread_mutex_unlock( &(p->lock) );
    }

    return NULL;
}

static javacall_result serial_create_thread(javacall_handle handle,
        thread_func func, pthread_t *thread) {

    pthread_t t;
    if (pthread_create(&t, NULL, func, handle) != 0) {
        JAVACALL_REPORT_ERROR1(JC_SERIAL,
                "[UART] failed to create thread: errno=%d", errno);
        return JAVACALL_FAIL;
    }

    (*thread) = t;

    return JAVACALL_OK;
}

javacall_result
jc_serial_open(const char *devName, serial_handle p) {
    char tmp[20] = "/dev/";
    if (strlen(devName) >= sizeof(tmp) - strlen(tmp)) {
        JAVACALL_REPORT_ERROR(JC_SERIAL, "Not enough room for device name");
        return JAVACALL_FAIL;
    }
    strcat(tmp, devName);
    //opening one of the /dev/ttyUSB devices, no pin busy check
    if (-1 == (p->fd = open(tmp, O_RDWR | O_NOCTTY | O_NONBLOCK))) {
        JAVACALL_REPORT_ERROR2(JC_SERIAL, "Can't open %s file errno %d",
                tmp, errno);
        return JAVACALL_FAIL;
    }

    return JAVACALL_OK;
}

speed_t int_to_baud(int baud) {
    switch (baud) {
    case 0:
        return B0;
    case 50:
        return B50;
    case 75:
        return B75;
    case 110:
        return B110;
    case 134:
        return B134;
    case 150:
        return B150;
    case 200:
        return B200;
    case 300:
        return B300;
    case 600:
        return B600;
    case 1200:
        return B1200;
    case 1800:
        return B1800;
    case 2400:
        return B2400;
    case 4800:
        return B4800;
    case 9600:
        return B9600;
    case 19200:
        return B19200;
    case 38400:
        return B38400;
    case 57600:
        return B57600;
    case 115200:
        return B115200;
#if 0
        case 230400: return B230400;
        case 460800: return B460800;
        case 500000: return B500000;
        case 576000: return B576000;
        case 921600: return B921600;
        case 1000000: return B1000000;
        case 1152000: return B1152000;
        case 1500000: return B1500000;
        case 2000000: return B2000000;
        case 2500000: return B2500000;
        case 3000000: return B3000000;
        case 3500000: return B3500000;
        case 4000000: return B4000000;
#endif
    default:
        return -1;
    }
}

int baud_to_int(speed_t baud) {
    switch (baud) {
    case B0:
        return 0;
    case B50:
        return 50;
    case B75:
        return 75;
    case B110:
        return 110;
    case B134:
        return 134;
    case B150:
        return 150;
    case B200:
        return 200;
    case B300:
        return 300;
    case B600:
        return 600;
    case B1200:
        return 1200;
    case B1800:
        return 1800;
    case B2400:
        return 2400;
    case B4800:
        return 4800;
    case B9600:
        return 9600;
    case B19200:
        return 19200;
    case B38400:
        return 38400;
    case B57600:
        return 57600;
    case B115200:
        return 115200;
#if 0
        case B230400: return 230400;
        case B460800: return 460800;
        case B500000: return 500000;
        case B576000: return 576000;
        case B921600: return 921600;
        case B1000000: return 1000000;
        case B1152000: return 1152000;
        case B1500000: return 1500000;
        case B2000000: return 2000000;
        case B2500000: return 2500000;
        case B3000000: return 3000000;
        case B3500000: return 3500000;
        case B4000000: return 4000000;
#endif
    default:
        return -1;
    }
}
