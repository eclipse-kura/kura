/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include <sys/types.h>
#include <sys/stat.h>
#include <stdint.h>
#include <fcntl.h>
#include <stdio.h>
#include <errno.h>

#include <linux/i2c-dev.h>
#include <pthread.h>

#include "javacall_i2c.h"
#include "javacall_logging.h"
#include "javacall_memory.h"

enum {
    READ = 0,
    WRITE = 1
};

typedef struct {
    // I2C javacall handle to notify java thread
    javacall_handle pDev;
    // I2C device handle to avoid pDev dereferencing that may be closed at the end of native operation
    int fd;
    // operation type
    javacall_int8 type;
    // next data chunk len
    javacall_int32 len;
    // src/dst buffer pointer
    javacall_int8* buf;
    // temp buffer if any
    javacall_int8 tmp[0];
} i2c_io_context;

typedef struct i2c_bus_t i2c_bus;

typedef struct i2c_slave_str i2c_slave;

struct i2c_bus_t {
    i2c_bus *next;
    unsigned int busId;
    javacall_bool isBusy;
    javacall_bool lastMessage;
    /* list of opened I2C slaves on the bus */
    i2c_slave *slaves;
};

struct i2c_slave_str {
    uint8_t address;  /* 7 bits long */
    int fd;
    pthread_t context;
    i2c_bus* bus;
    i2c_slave* next;
};

/* global list of opened I2C busses */
static i2c_bus *_i2c_buses = NULL;

static inline javacall_dio_result i2c_create_bus(unsigned int busId, i2c_bus** pBus) {
    i2c_bus *tmpBus;

    /* /dev/i2c-X file has been opened successfully,
     * created new 'i2c_bus' */
    tmpBus = (i2c_bus*) javacall_malloc(sizeof(i2c_bus));
    if (tmpBus == NULL ) {
        JAVACALL_REPORT_ERROR(JC_DIO, "[I2C] cannot alloc i2c_bus");
        return JAVACALL_DIO_OUT_OF_MEMORY;
    }

    tmpBus->busId = busId;
    tmpBus->slaves = NULL;
    tmpBus->isBusy = 0;

    /* Add newly created bus to 'g_i2c_buses' list */
    tmpBus->next = _i2c_buses;
    _i2c_buses = tmpBus;

    *pBus = tmpBus;
    return JAVACALL_DIO_OK;
}

/* find the bus with a given id or create one if it's not exist */
static inline javacall_dio_result i2c_get_bus(unsigned int busId, i2c_bus** pBus) {

    i2c_bus *bus = _i2c_buses;
    while(bus) {
        if (bus->busId == busId) {
            *pBus = bus;
            return JAVACALL_DIO_OK;
        }
        bus = bus->next;
    }

    /* No currently opened i2c bus with 'busId' id. Create the new one */
    return i2c_create_bus(busId, pBus);
}

/* release bus if it has no opened slaves */
static javacall_dio_result i2c_release_bus(i2c_bus* bus) {

    i2c_bus *busPrev, *busCur;

    if (NULL != bus->slaves)
        return JAVACALL_DIO_OK;

    /* find the previous bus in the bus' list */
    busPrev = NULL;
    busCur = _i2c_buses;
    while(busCur) {
        /* Check if it's a bus to remove */
        if (busCur == bus) {
            /* Check if it is about to remove first bus from the bus' list */
            if (NULL == busPrev) {
                _i2c_buses = bus->next;
            } else {
                busPrev->next = bus->next;
            }
            javacall_free(bus);
            return JAVACALL_DIO_OK;
        }
        busPrev = busCur;
        busCur = busCur->next;
    }

    JAVACALL_REPORT_ERROR1(JC_DIO, "[I2C] inconsistency in bus list, failed to release %d", bus->busId);

    /* 'bus' bus hasn't been found */
    return JAVACALL_DIO_FAIL;
}

static inline javacall_dio_result i2c_attach_slave_to_bus(i2c_slave *slave, unsigned int busId) {
    javacall_dio_result rv;
    i2c_bus *bus;
    i2c_slave *tmpSlave;

    /* get bus descriptor */
    if (JAVACALL_DIO_OK != (rv = i2c_get_bus(busId, &bus))) {
        return rv;
    }

    /* check if a slave with the same address is already opened */
    tmpSlave = bus->slaves;
    while (tmpSlave) {
        if (tmpSlave->address == slave->address) {
            i2c_release_bus(bus);
            return JAVACALL_DIO_BUSY;
        }
        tmpSlave = tmpSlave->next;
    }

    /* attach slave to the bus */
    slave->bus = bus;
    slave->next = bus->slaves;
    bus->slaves = slave;

    return JAVACALL_DIO_OK;
}

static inline javacall_dio_result i2c_detach_slave_from_bus(i2c_slave *slave) {
    i2c_slave *slavePrev, *slaveCur;

    /* find the previous slave in the slave's list */
    slavePrev = NULL;
    slaveCur = slave->bus->slaves;
    while(slaveCur) {
        if (slaveCur == slave) {
            break;
        }
        slavePrev = slaveCur;
        slaveCur = slaveCur->next;
    }

    /* Check if there are no given slave in the list */
    if (NULL == slaveCur) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "[I2C] inconsistency in slave list, failed to release %d", slave->address);
        javacall_free(slave);
        return JAVACALL_DIO_FAIL;
    }

    /* Check if it is about to remove first slave from the slave's list */
    if (NULL == slavePrev) {
        slave->bus->slaves = slave->next;
        i2c_release_bus(slave->bus);
    } else {
        /* There is at least one opened slave in the list before 'the slave to remove'.
           It means that there is not necessary to release bus struct. */
        slavePrev->next = slave->next;
    }

    return JAVACALL_DIO_OK;
}


static void i2c_close_slave(i2c_slave *slave) {
    int rv;

    /* if device is attached to a bus */
    if (slave->bus) {
        i2c_detach_slave_from_bus(slave);
    }

    /* If i2c device fd is opened */
    if (slave->fd >= 0) {
        /* close device fd */
        while ((rv = close(slave->fd)) < 0 && EINTR == errno);

        if (rv != 0)
            JAVACALL_REPORT_ERROR1(JC_DIO, "[I2C] cannot close bus fd, errno=%d", errno);
    }

    /* dealloc descriptor */
    javacall_free(slave);
}


javacall_dio_result javacall_i2c_open_slave_with_config(javacall_int32 busNum,
        javacall_int32 devAddr, javacall_int32 addrSize,
        javacall_int32 clockFrequency,
        const javacall_bool exclusive,
        /*OUT*/javacall_handle* pHandle)
{
    javacall_uint32 reg = 0xFFFFFFFF;
    i2c_slave *slave;
    javacall_dio_result rv;
    char filename[20];

    (void) clockFrequency;

    *pHandle = NULL;

    if (JAVACALL_TRUE != exclusive) {
        JAVACALL_REPORT_ERROR(JC_DIO, "[I2C] Shared mode is unsupported for I2C device");
        return JAVACALL_DIO_UNSUPPORTED_ACCESS_MODE;
    }

    if (addrSize == PERIPHERAL_CONFIG_DEFAULT)
        addrSize = 7;

    if (busNum == PERIPHERAL_CONFIG_DEFAULT)
        busNum = 1;

    if (addrSize != 7) {
        JAVACALL_REPORT_ERROR(JC_DIO, "i2c config error: addrSize != 7");
        return JAVACALL_DIO_INVALID_CONFIG;
    }

    if (busNum < 0) {
        JAVACALL_REPORT_ERROR(JC_DIO,
                "i2c config error: busNum should not be less than zero");
        return JAVACALL_DIO_FAIL;
    }

    slave = (i2c_slave*) javacall_calloc(1, sizeof(i2c_slave));
    if (slave == NULL) {
        JAVACALL_REPORT_ERROR(JC_DIO,
                "malloc error in javacall_i2c_open_slave_with_config");
        return JAVACALL_DIO_OUT_OF_MEMORY;
    }

    slave->address = devAddr;

    /* Try to open corresponding /dev/i2c-X file first */
    snprintf(filename, 19, "/dev/i2c-%lu", busNum);

    slave->fd = open(filename, O_RDWR);

    if (slave->fd < 0) {
        i2c_close_slave(slave);
        JAVACALL_REPORT_ERROR1(JC_DIO, "[I2C] cannot open %s bus file", filename);
        return JAVACALL_DIO_FAIL;
    }

    /* Set target address */
    if (ioctl(slave->fd, I2C_SLAVE, slave->address) < 0) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "[I2C] cannot set %d address", slave->address);
        i2c_close_slave(slave);
        return JAVACALL_DIO_FAIL;
    }

    /* Attach device to a bus */
    if (JAVACALL_DIO_OK != (rv = i2c_attach_slave_to_bus(slave, busNum))) {
        i2c_close_slave(slave);
        return rv;
    }

    *pHandle = (javacall_handle) slave;

    return JAVACALL_DIO_OK;
}

static void* io_thread(void* arg) {
    i2c_io_context* ctx = (i2c_io_context*)arg;
    void* pData = (void*)ctx->buf;
    size_t len = (size_t)ctx->len;
    int type = ctx->type;
    long rv;

    JAVACALL_REPORT_INFO1(JC_DIO, "[I2C] Transfer start (%d)", type);

    if (WRITE == type) {
        rv = (int)write(ctx->fd, pData, len);

        if (-1 == rv) {
            JAVACALL_REPORT_ERROR1(JC_DIO, "[I2C] failed to write, errno=%d", errno);
        }
    } else {
        rv = (int)read(ctx->fd, pData, len);

        if (-1 == rv) {
            JAVACALL_REPORT_ERROR1(JC_DIO, "[I2C] failed to read, errno=%d", errno);
        }
    }

    if (-1 == rv) {
        // hope will get here when device is closed
        javanotify_i2c_event(JAVACALL_I2C_SEND_SIGNAL, ctx->pDev, JAVACALL_DIO_FAIL);
    } else {
        ctx->len = rv;
        javanotify_i2c_event(JAVACALL_I2C_SEND_SIGNAL, ctx->pDev, JAVACALL_DIO_OK);
    }

    javacall_free(ctx);

    JAVACALL_REPORT_INFO1(JC_DIO, "[I2C] Transfer end. rv=%d", rv);
    return (void*)rv;
}

javacall_dio_result javacall_i2c_transfer_start(const javacall_handle handle,
                                                const javacall_i2c_message_type type,
                                                const javacall_bool write,
                                                char* pData, int len,
                                                javacall_int32 *const pBytes){
    pthread_attr_t attr;
    i2c_slave* pDev = (i2c_slave*)handle;
    int flag = write ? WRITE : READ;

    if (pDev->bus->isBusy &&
        (JAVACALL_I2C_COMBINED_START == type || JAVACALL_I2C_REGULAR == type)) {
        return JAVACALL_DIO_INVALID_STATE;
    }

    pDev->bus->isBusy = JAVACALL_TRUE;

    if (JAVACALL_I2C_COMBINED_END == type || JAVACALL_I2C_REGULAR == type) {
        pDev->bus->lastMessage = JAVACALL_TRUE;
    } else {
        pDev->bus->lastMessage = JAVACALL_FALSE;
    }

    i2c_io_context *ctx = javacall_malloc(sizeof(i2c_io_context));
    if (NULL == ctx) {
        JAVACALL_REPORT_ERROR1(JC_DIO,
                               "[I2C] cannot allocate temp buffer: errno=%d", errno);
        return JAVACALL_DIO_OUT_OF_MEMORY;
    }

    ctx->pDev = handle;
    ctx->len = len;
    ctx->type = flag;
    ctx->buf = (javacall_int8*)pData;
    ctx->fd = pDev->fd;

    if (pthread_create(&pDev->context, NULL, io_thread, ctx) != 0) {
        javacall_free(ctx);
        JAVACALL_REPORT_ERROR1(JC_DIO,
                               "[I2C] failed to start read operation: errno=%d", errno);
        return JAVACALL_DIO_FAIL;
    }

    return JAVACALL_DIO_WOULD_BLOCK;
}

javacall_dio_result javacall_i2c_transfer_finish(const javacall_handle handle,
                                                 const javacall_bool cancel,
                                                 char* pData, int len,
                                                 javacall_int32* const pBytes) {
    i2c_slave* pDev = (i2c_slave*)handle;
    // if not interrupt
    if (cancel) {
        pthread_detach(pDev->context);
    } else {
        if (0 != pthread_join(pDev->context, (void**)pBytes)) {
            JAVACALL_REPORT_ERROR1(JC_DIO, "[I2C] Can't joint thread: %d", errno);
        }
    }

    if(pDev->bus->lastMessage == JAVACALL_TRUE || cancel) {
        pDev->bus->isBusy = JAVACALL_FALSE;
    }

    return JAVACALL_DIO_OK;

}


/**
 * See javacall_i2c.h for definition
 */
void javacall_i2c_close(javacall_handle handle) {

    i2c_slave *pDev = (i2c_slave*)handle;

    pthread_detach(pDev->context);

    // if this was regular or last combined message then release bus
    if(pDev->bus->lastMessage == JAVACALL_TRUE) {
        pDev->bus->isBusy = JAVACALL_FALSE;
    }

    i2c_close_slave(pDev);

}

/**
 * See javacall_i2c.h for definition
 */
javacall_dio_result javacall_i2c_get_group_id(const javacall_handle handle,
        /*OUT*/ long* grpId) {
    i2c_slave *pDev = (i2c_slave*)handle;
    *grpId = (long)pDev->bus;
    return JAVACALL_DIO_OK;
}

/**
 * See javacall_i2c.h for definition
 */
javacall_dio_result javacall_i2c_lock(const javacall_handle handle, javacall_handle* const owner) {
    (void)handle;
    (void)owner;
    // exclusive mode only
    return JAVACALL_DIO_OK;
}

/**
 * See javacall_i2c.h for definition
 */
javacall_dio_result javacall_i2c_unlock(const javacall_handle handle) {
    (void)handle;
    // exclusive mode only
    return JAVACALL_DIO_OK;
}
