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

#include "javacall_defs.h"
#include "javacall_spi.h"
#include "javacall_logging.h"
#include "javacall_memory.h"
#include <getopt.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/types.h>
#include <linux/spi/spidev.h>
#include <stdio.h>
#include <errno.h>
#include <pthread.h>

#define MAX(a,b)    ((a) > (b) ? (a) : (b))

#define SPI_WORD_LENGTH     8

#define SPI_CPHA_MASK       0x0001
#define SPI_CPOL_MASK       0x0010

#define DEFAULT_BUS_ID           0          /* SPI1       */
#define DEFAULT_CLOCK_FREQUENCY  2000000    /* Hz         */
#define DEFAULT_WORD_LENGTH      8          /* bits       */
#define DEFAULT_BIT_ORDERING     1          /* big-endian */

typedef struct _spi_context spi_context;

 struct _spi_context {
    spi_context* next;
    int ref;
    unsigned int len;
    const char* txBuf;
    char* rxBuf;
};

typedef struct _spi_slave_config spi_slave_config;

struct _spi_slave_config{
    int                busId;        /* bus ID */
    int                address;      /* chip enable:                */
                                     /*    =  0 - NO_CS             */
                                     /*    =  1 - CE0/Pin8, CS_LOW  */
                                     /*    =  2 - CE1/Pin7, CS_LOW  */
                                     /*    = -1 - CE0/Pin8, CS_HIGH */
                                     /*    = -2 - CE1/Pin7, CS_HIGH */
    int                wordSize;     /* word length in bits */
    int                clkFrequency; /* clock frequency */
    int                clkMode;      /* clock polarity & phase: */
                                     /* Mode CPOL CPHA */
                                     /*   0    0    0  */
                                     /*   1    0    1  */
                                     /*   2    1    0  */
                                     /*   3    1    1  */
    javacall_byteorder bitOrdering;  /* bit (shifting) ordering */
    int                devFd;
    javacall_spi_cs_active csActive;
    javacall_bool          busOwner;
    spi_slave_config* next;
    spi_context* xfer_list;
};

#define SSC(arg) ((spi_slave_config*)arg)

static spi_slave_config* _spi_dev_list = NULL;
static pthread_mutex_t _list_mutex = PTHREAD_MUTEX_INITIALIZER;

static javacall_dio_result spi_configure (const spi_slave_config* cfg);
static void            spi_disable   (spi_slave_config* cfg);
static javacall_dio_result spi_enable    (spi_slave_config* cfg);
static javacall_dio_result spi_transfer  (const spi_slave_config* cfg, spi_context* data);


static spi_slave_config* get_opened_device(int bus_id, int address){
    spi_slave_config* next = _spi_dev_list;

    if(0 != pthread_mutex_lock(&_list_mutex)){
        JAVACALL_REPORT_ERROR1(JC_DAAPI, "[SPI] get_opened_device: cannot lock mutex errno=%d\n", errno);
        return NULL;
    }

    while (NULL != next) {
        if (next->busId == bus_id && next->address == address) {
            break;
        }
        next = next->next;
    }

    if(0 != pthread_mutex_unlock(&_list_mutex)){
        JAVACALL_REPORT_WARN1(JC_DAAPI, "[SPI] get_opened_device: cannot unlock mutex errno=%d\n", errno);
    }
    return next;
}

static spi_slave_config* get_bus_owner(int bus_id){
    spi_slave_config* next = _spi_dev_list;

    if(0 != pthread_mutex_lock(&_list_mutex)){
        JAVACALL_REPORT_ERROR1(JC_DAAPI, "[SPI] get_bus_owner: cannot lock mutex errno=%d\n", errno);
        return NULL;
    }

    while (NULL != next) {
        if (next->busId == bus_id && next->busOwner == JAVACALL_TRUE) {
            break;
        }
        next = next->next;
    }

    if(0 != pthread_mutex_unlock(&_list_mutex)){
        JAVACALL_REPORT_WARN1(JC_DAAPI, "[SPI] get_bus_owner: cannot unlock mutex errno=%d\n", errno);
    }
    return next;
}


/**
 * Add spi config pointer to globl spi list
 *
 *@param spi_dev      pointer to spi device structure
 *
 *@retval JAVACALL_DIO_OK        on success
 *@retval JAVACALL_DIO_FAIL      fail to get mutex, the device is NOT in the list
 */
static javacall_dio_result add_dev_to_list(spi_slave_config* spi_dev) {

    if(0 != pthread_mutex_lock(&_list_mutex)){
        JAVACALL_REPORT_ERROR1(JC_DAAPI, "[SPI] add_dev_to_list: cannot lock mutex errno=%d\n", errno);
        return JAVACALL_DIO_FAIL;
    }

    spi_dev->next = _spi_dev_list;
    _spi_dev_list = spi_dev;

    if(0 != pthread_mutex_unlock(&_list_mutex)){
        JAVACALL_REPORT_WARN1(JC_DAAPI, "[SPI] add_dev_to_list: cannot unlock mutex errno=%d\n", errno);
    }

    return JAVACALL_DIO_OK;
}

/**
 * Remove spi config pointer to globl spi list
 *
 *@param spi_dev      pointer to spi device structure
 *
 *@retval JAVACALL_DIO_OK        on success
 *@retval JAVACALL_DIO_FAIL      fail to get mutex, the device is NOT removed from the list
 */
static javacall_dio_result remove_dev_from_list(spi_slave_config* spi_dev) {

    spi_slave_config* next;

    if(0 != pthread_mutex_lock(&_list_mutex)){
        JAVACALL_REPORT_ERROR1(JC_DAAPI, "[SPI] remove_dev_from_list: cannot lock mutex errno=%d\n", errno);
        return JAVACALL_DIO_FAIL;
    }

    next = _spi_dev_list;

    if(_spi_dev_list == NULL || _spi_dev_list == spi_dev){
        //1 dev at the list
        _spi_dev_list = NULL;
    }else{
        while (NULL != next->next){
            if(spi_dev != next->next) {
                next = next->next;
            }else{
                next->next = spi_dev->next;
            }
        }
    }
    if(0 != pthread_mutex_unlock(&_list_mutex)){
        JAVACALL_REPORT_WARN1(JC_DAAPI, "[SPI] remove_dev_from_list: cannot unlock mutex errno=%d\n", errno);
    }
    return JAVACALL_DIO_OK;
}

/**
* See javacall_spi.h for definition
*/
javacall_dio_result javacall_spi_begin(javacall_handle handle) {
    /*
        check if other device own the bus
    */
    if (NULL != get_bus_owner(SSC(handle)->busId)) {
        return JAVACALL_DIO_INVALID_STATE;
    }
    SSC(handle)->busOwner = JAVACALL_TRUE;
    return JAVACALL_DIO_OK;
}

/**
* See javacall_spi.h for definition
*/
javacall_dio_result javacall_spi_end(javacall_handle handle) {
    javacall_dio_result res;
    spi_context* ctx;
    spi_slave_config* cfg = (spi_slave_config*)handle;

    if (SSC(handle)->busOwner != JAVACALL_TRUE) {
        return JAVACALL_DIO_INVALID_STATE;
    }

    ctx = cfg->xfer_list;
    cfg->xfer_list = NULL;
    res = spi_transfer(cfg, ctx);

    while (NULL != ctx) {
        spi_context* tmp = ctx;
        ctx = ctx->next;
        javacall_free(tmp);
    }
    SSC(handle)->busOwner = JAVACALL_FALSE;

    return res;
}

/**
* See javacall_spi.h for definition
*/
javacall_dio_result javacall_spi_unlock(const javacall_handle handle){
    (void)handle;
    return JAVACALL_DIO_OK;
}
/**
* See javacall_spi.h for definition
*/
javacall_dio_result javacall_spi_lock(const javacall_handle handle, javacall_handle* const owner){
    (void)handle;
    (void)owner;
    return JAVACALL_DIO_OK;
}

/**
 * See javacall_spi.h for definition
 */
javacall_dio_result javacall_spi_open_slave_with_config(javacall_int32 busId,
        javacall_int32 address, javacall_spi_cs_active csActive, javacall_int32 clockFrequency,
        javacall_int32 clockMode, javacall_int32 wordLength,
        javacall_byteorder bitOrdering,
        const javacall_bool exclusive,
        /*OUT*/javacall_handle* pHandle) {

    javacall_dio_result res;

    if (JAVACALL_TRUE != exclusive) {
        JAVACALL_REPORT_ERROR(JC_DIO, "[SPI] Shared mode is unsupported for SPI device");
        return JAVACALL_DIO_UNSUPPORTED_ACCESS_MODE;
    }

    if( NULL != get_opened_device(busId, address)){
        return JAVACALL_DIO_BUSY;
    }

    if (csActive == DAAPI_SPI_CS_DEFAULT) {
        csActive = DAAPI_SPI_CS_ACTIVE_LOW;
    }

    spi_slave_config* cfg = (spi_slave_config*) javacall_malloc(sizeof(spi_slave_config));
    if (NULL == cfg) {
        JAVACALL_REPORT_ERROR(JC_DIO, "malloc error in javacall_spi_open_slave_with_config()\n");
        return JAVACALL_DIO_OUT_OF_MEMORY;
    }

    cfg->csActive = csActive;
    cfg->busId        = (busId == PERIPHERAL_CONFIG_DEFAULT) ?
                        DEFAULT_BUS_ID : busId;
    cfg->address      = address;
    cfg->clkFrequency = (clockFrequency == PERIPHERAL_CONFIG_DEFAULT) ?
                        DEFAULT_CLOCK_FREQUENCY : clockFrequency;
    cfg->clkMode      = clockMode;
    cfg->wordSize     = (wordLength == PERIPHERAL_CONFIG_DEFAULT) ?
                        DEFAULT_WORD_LENGTH : wordLength;
    cfg->bitOrdering  = (bitOrdering == PERIPHERAL_CONFIG_DEFAULT) ?
                        DEFAULT_BIT_ORDERING : bitOrdering;
    cfg->devFd        = -1;

    cfg->xfer_list = NULL;

    cfg->busOwner = JAVACALL_FALSE;

    if ((res = spi_enable(cfg)) != JAVACALL_DIO_OK) {
        javacall_spi_close_slave(cfg);
        return res;
    }

    if (JAVACALL_DIO_OK != add_dev_to_list(cfg)){
        javacall_spi_close_slave(cfg);
        return JAVACALL_DIO_FAIL;
    }
    *pHandle = (javacall_handle)cfg;
    return JAVACALL_DIO_OK;
}

/**
* See javacall_spi.h for definition
*/
void javacall_spi_close_slave(javacall_handle handle) {

    spi_slave_config* cfg = (spi_slave_config*)handle;
    spi_disable(cfg);
    remove_dev_from_list(cfg);
    javacall_free(cfg);
}

void add_to_xfer_list(spi_slave_config* cfg, spi_context* context) {
    spi_context** next = &cfg->xfer_list;
    while (NULL != *next) {
        next = &(*next)->next;
    }
    *next = context;
}

/**
* See javacall_spi.h for definition
*/
javacall_dio_result javacall_spi_send_and_receive_start(javacall_handle handle,const char* pTxBuf, /*OUT*/char* pRxBuf, const int len) {

    javacall_dio_result res = JAVACALL_DIO_OK;
    spi_context* context;
    spi_slave_config* cfg = (spi_slave_config*)handle;

    if ( (SSC(handle)->busOwner != JAVACALL_TRUE) && (get_bus_owner(SSC(handle)->busId) != NULL) ) {
        return JAVACALL_DIO_INVALID_STATE;
    }

    context = (spi_context*)javacall_malloc(sizeof(spi_context));
    if (NULL != context) {

        context->len = len;
        context->txBuf = pTxBuf;
        context->rxBuf = pRxBuf;
        context->ref = 1;
        context->next = NULL;

        if (SSC(handle)->busOwner == JAVACALL_TRUE) {
            //the device is owner, a transaction has been started
            add_to_xfer_list(cfg, context);
        } else {
            res = spi_transfer(cfg, context);
            javacall_free(context);
        }
    } else {
        res = JAVACALL_DIO_OUT_OF_MEMORY;
    }

    return res;
}

/**
* See javacall_spi.h for definition
*/
javacall_dio_result javacall_spi_send_and_receive_finish(javacall_handle handle, const javacall_bool cancel,
                                                         const char* pTxBuf, /*OUT*/char* pRxBuf, const int len) {
    JAVACALL_REPORT_ERROR(JC_DIO, "javacall_spi_send_and_receive_finish");
    return JAVACALL_DIO_FAIL;
}

/**
* See javacall_spi.h for definition
*/
javacall_dio_result javacall_spi_get_word_size(const javacall_handle handle,
        /*OUT*/javacall_int32* pSize){

    (*pSize) = ((spi_slave_config*)handle)->wordSize;
    return JAVACALL_DIO_OK;
}

/**
* See javacall_spi.h for definition
*/
javacall_dio_result javacall_spi_get_byte_ordering(const javacall_handle handle,
        /*OUT*/javacall_int32* pByteOrdering){

    (*pByteOrdering) = ((spi_slave_config*)handle)->bitOrdering;
    return JAVACALL_DIO_OK;
}

static javacall_dio_result spi_enable(spi_slave_config* cfg) {
    char device[32] = {0};
    struct flock lock;
    int res;
    sprintf(device, "/dev/spidev%d.%d",
            cfg->busId, cfg->address);

    cfg->devFd = open(device, O_RDWR | O_EXCL);

    if (0 <= cfg->devFd) {
        lock.l_type   = F_WRLCK;  /* exclusive lock*/
        lock.l_whence = SEEK_SET;
        lock.l_start  = 0;
        lock.l_len    = 0;
        lock.l_pid    = getpid();

        if(-1 == fcntl(cfg->devFd , F_SETLK, &lock)){
            if (errno == EACCES || errno == EAGAIN) {
                close(cfg->devFd);
                cfg->devFd = -1;
                return JAVACALL_DIO_BUSY;
            }else{
                JAVACALL_REPORT_ERROR1(JC_DAAPI, "[SPI] Can't lock device. errno %d", errno);
                close(cfg->devFd);
                cfg->devFd = -1;
                return JAVACALL_DIO_FAIL;
            }
        }
        if (JAVACALL_DIO_OK != spi_configure(cfg)) {
            close(cfg->devFd);
            return JAVACALL_DIO_INVALID_CONFIG;
        }
        return JAVACALL_DIO_OK;
    }else{
        if(errno == EACCES){
            JAVACALL_REPORT_ERROR1(JC_DAAPI, "[SPI] Can't open %s. Permission denied", device);
        }
    }

    return JAVACALL_DIO_NOT_FOUND;
}

static void spi_disable(spi_slave_config* cfg) {
    struct flock lock;
    if (cfg->devFd < 0)
        return;

    lock.l_type   = F_UNLCK;
    lock.l_whence = SEEK_SET;
    lock.l_start  = 0;
    lock.l_len    = 0;
    lock.l_pid    = getpid();

    if(-1 == fcntl(cfg->devFd , F_SETLK, &lock)){
        JAVACALL_REPORT_ERROR1(JC_DAAPI, "[SPI] Can't unclock device. errno %d", errno);
    }

    close( cfg->devFd );
    cfg->devFd = -1;
}

static javacall_dio_result spi_configure(const spi_slave_config* cfg) {

    int mode  = (cfg->clkMode & SPI_CPHA_MASK ? SPI_CPHA : 0) |
                (cfg->clkMode & SPI_CPOL_MASK ? SPI_CPOL : 0) |
                (cfg->bitOrdering == DAAPI_LITTLE_ENDIAN  ? SPI_LSB_FIRST : 0);
    int bits  = cfg->wordSize;
    int speed = cfg->clkFrequency;

    switch (cfg->csActive) {
    case DAAPI_SPI_CS_ACTIVE_HIGH:
        mode |= SPI_CS_HIGH;
        break;
    case DAAPI_SPI_CS_NOT_CONTROLLED:
        mode |= SPI_NO_CS;
        break;
    default:
        break;
    }

    if (bits != SPI_WORD_LENGTH) {
        return JAVACALL_DIO_FAIL;
    }

    /* spi mode */
    if (ioctl(cfg->devFd, SPI_IOC_WR_MODE, &mode) < 0) {
        JAVACALL_REPORT_ERROR(JC_DIO, "[SPI] Can't setup mode");
        return JAVACALL_DIO_FAIL;
    }

    /* bits per word */
    if (ioctl(cfg->devFd, SPI_IOC_WR_BITS_PER_WORD, &bits) < 0) {
        JAVACALL_REPORT_ERROR(JC_DIO, "[SPI] Can't bits per word");
        return JAVACALL_DIO_FAIL;
    }

    /* max speed hz */
    if (ioctl(cfg->devFd, SPI_IOC_WR_MAX_SPEED_HZ, &speed) < 0) {
        JAVACALL_REPORT_ERROR(JC_DIO, "[SPI] Can't setup speed");
        return JAVACALL_DIO_FAIL;
    }


    return JAVACALL_DIO_OK;
}

static javacall_dio_result spi_transfer(const spi_slave_config* cfg, spi_context* data) {
    struct spi_ioc_transfer* tr;
    int c = 0, size;
    javacall_dio_result res;
    spi_context** next = &data;
    while (NULL != *next) {
        c++;
        next = &((*next)->next);
    }

    if (!c) {
        JAVACALL_REPORT_INFO(JC_DIO, "[SPI] Skip empty transaction");
        return JAVACALL_DIO_OK;
    }

    size = c * sizeof(struct spi_ioc_transfer);
    tr = (struct spi_ioc_transfer*)javacall_malloc(size);
    memset((void*)tr, 0, size);

    c = 0;
    next = &data;
    while (NULL != *next) {
        const spi_context* d = *next;
        tr[c].tx_buf        = (unsigned long)d->txBuf;
        tr[c].rx_buf        = (unsigned long)d->rxBuf;
        tr[c].len           = d->len;
        tr[c].speed_hz      = cfg->clkFrequency;
        tr[c].bits_per_word = cfg->wordSize;
        tr[c].cs_change = 0;
        c++;
        next = &((*next)->next);
    }

    JAVACALL_REPORT_INFO1(JC_DIO, "[SPI] Transfer %d messages", c);

    res = (ioctl(cfg->devFd, SPI_IOC_MESSAGE(c), tr) < 0) ? JAVACALL_DIO_FAIL : JAVACALL_DIO_OK;
    javacall_free(tr);
    return res;
}

javacall_dio_result javacall_spi_get_group_id(const javacall_handle handle, javacall_int32* const  grp){
    spi_slave_config* spiSlave = (spi_slave_config*) handle;
    *grp =  'S' | 'P'<<8 | 'I'<<16 | spiSlave->busId << 24;
    return JAVACALL_DIO_OK;
}
