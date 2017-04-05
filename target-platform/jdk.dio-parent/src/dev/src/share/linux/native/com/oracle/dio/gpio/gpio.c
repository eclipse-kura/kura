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

#include <fcntl.h>
#include <stdio.h>
#include <pthread.h>
#include <sys/epoll.h>
#include <time.h>
#include "javanotify_gpio.h"
#include "javautil_linked_list.h"
#include "javacall_logging.h"
#include "javacall_gpio.h"
#include "javacall_memory.h"
#include "javacall_dio.h"
#include <sys/stat.h>

#define EXPORT_FILE_NAME "/sys/class/gpio/export"
#define UNEXPORT_FILE_NAME "/sys/class/gpio/unexport"
#define GPIO_VALUE_FILENAME_TEMPLATE "/sys/class/gpio/gpio%d/value"
#define GPIO_DIRECTION_FILENAME_TEMPLATE "/sys/class/gpio/gpio%ld/direction"
#define GPIO_EDGE_FILENAME_TEMPLATE "/sys/class/gpio/gpio%d/edge"
#define PIN_NAME_TEMPLATE "%d"
#define GPIO_FILENAME_TEMPLATE "/sys/class/gpio/gpio%d"

// these pathname length values limit GPIO
// numbers to 3 digits
#define VALUE_FILENAME_MAX_LENGTH 30
#define DIRECTION_FILENAME_MAX_LENGTH 34
#define EDGE_FILENAME_MAX_LENGTH 29
#define GPIO_FILENAME_MAX_LENGTH 24

typedef struct epoll_event epoll_event;
typedef struct _GPIOHandle GPIOHandle;
struct _GPIOHandle {
    GPIOHandle* next;
    int number;
    javacall_gpio_dir direction;
    javacall_bool notificationsEnabled;
    javacall_bool lastValue;
    javacall_bool inPort;
    int valueFD;
    int directionFD;
    epoll_event* pollEvent;
    javacall_gpio_trigger_mode edgeMode;
};

typedef struct _GPIOPortHandle {
    javacall_handle pinList;
    javacall_gpio_dir direction;
    javacall_int32 maxValue;
    javacall_int32 lastValue;
    javacall_bool notificationsEnabled;
    javacall_bool needPinClose;
} GPIOPortHandle;

static javacall_int8 pinCountForPolling = 0;
pthread_t notificationThread;
volatile javacall_bool notificationThreadActivated = JAVACALL_FALSE;
pthread_mutex_t epoll_fd_lock = PTHREAD_MUTEX_INITIALIZER;
int epoll_descriptor = -1;
typedef struct _polling_data {
    GPIOHandle* pin;
    GPIOPortHandle* port;
} polling_data;

//Values. that can be passed to /sys/class/gpio/gpioX/direction file
#define PLATFORM_IN_GPIO_DIRECTION "in"
#define PLATFORM_HIGH_GPIO_DIRECTION "high"
#define PLATFORM_LOW_GPIO_DIRECTION "low"
#define PLATFORM_OUT_GPIO_DIRECTION  "out"

//Values. that can be passed to /sys/class/gpio/gpioX/edge file
#define PLATFORM_NONE_GPIO_EDGE "none"
#define PLATFORM_BOTH_GPIO_EDGE "both"
#define PLATFORM_RISING_GPIO_EDGE "rising"
#define PLATFORM_FALLING_GPIO_EDGE "falling"

void get_platform_direction_string(javacall_gpio_dir direction, javacall_bool initialValue, /*OUT*/ javacall_ascii_string* outString);
void get_platform_edge_string(javacall_gpio_trigger_mode edgeMode, javacall_ascii_string* string);
javacall_bool is_pin_pull_up(int pinNumber);
javacall_dio_result enable_gpio_pin(int pinNumber);
javacall_dio_result disable_gpio_pin(int pinNumber);
javacall_dio_result write_direction_to_file(int dirFD, javacall_gpio_dir direction, javacall_bool initialValue);
javacall_dio_result fill_value_fd_for_pin(GPIOHandle* handle);
javacall_dio_result write_value_to_pin(GPIOHandle* handle, javacall_bool value);
javacall_dio_result read_value_from_pin(GPIOHandle* handle, /*OUT*/ javacall_bool* value);
javacall_dio_result determine_name_of_value_file(GPIOHandle* handle, /*OUT*/javacall_ascii_string name);
javacall_dio_result check_gpio_pin_state(javacall_int32 pinNumber);
javacall_dio_result close_pins_in_list(javacall_handle list);
void* pin_events_listener_function(void* data);
javacall_dio_result add_pin_for_polling(GPIOHandle* pinHandle);
javacall_dio_result del_pin_from_polling(GPIOHandle* pinHandle);
javacall_dio_result add_port_for_polling(GPIOPortHandle* portHandle);
javacall_dio_result del_port_from_polling(GPIOPortHandle* portHandle);
javacall_dio_result activate_platform_notifications_to_pin(GPIOHandle* pin);
void activate_notification_thread();

static GPIOHandle* pin_list = NULL;
static pthread_mutex_t pin_list_lock = PTHREAD_MUTEX_INITIALIZER;

static void lock_pin_list() {
    pthread_mutex_lock(&pin_list_lock);
}

static void unlock_pin_list() {
    pthread_mutex_unlock(&pin_list_lock);
}

static javacall_bool check_gpio_pin_is_free(javacall_int32 pin) {
    GPIOHandle* next = pin_list;
    while (NULL != next) {
        if (next->number == pin) {
            return JAVACALL_FALSE;
        }
        next = next->next;
    }

    return JAVACALL_TRUE;
}

static GPIOHandle* get_pin_handle(javacall_int32 pin) {
    lock_pin_list();
    GPIOHandle* next = pin_list;
    while (NULL != next) {
        if (next->number == pin) {
            break;
        }
        next = next->next;
    }
    unlock_pin_list();
    return next;
}

static void add_to_pin_list(GPIOHandle* pin) {
    pin->next = pin_list;
    pin_list = pin;
}

static void remove_pin_from_list(GPIOHandle* pin) {
    lock_pin_list();
    // remove from busy list
    GPIOHandle** next = &pin_list;
    while (NULL != *next && pin != *next) {
        next = &(*next)->next;
    }
    if (*next != NULL) {
        *next = pin->next;
    }
    unlock_pin_list();
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_pin_close(javacall_handle handle) {
    GPIOHandle* gpioHandle = (GPIOHandle*) handle;

    JAVACALL_REPORT_INFO1(JC_DIO, "Close pin %d", gpioHandle->number);

    javacall_gpio_pin_notification_stop(handle);
    disable_gpio_pin(gpioHandle->number);
    close(gpioHandle->directionFD);
    close(gpioHandle->valueFD);

    remove_pin_from_list(gpioHandle);
    javacall_free(gpioHandle);

    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_port_close(javacall_handle handle){
    GPIOPortHandle* portHandle = (GPIOPortHandle*) handle;
    JAVACALL_REPORT_INFO(JC_DIO, "GPIO port close");
    javacall_gpio_port_notification_stop(handle);
    if (JAVACALL_TRUE == portHandle->needPinClose ) {
        close_pins_in_list(portHandle->pinList);
    }
    javautil_list_destroy(portHandle->pinList);
    javacall_free(portHandle);
    return JAVACALL_DIO_OK;
}


/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_port_get_max_value(const javacall_handle handle,
        /*OUT*/javacall_int32* pVal){
    GPIOPortHandle* portHandle = (GPIOPortHandle*) handle;
    *pVal = portHandle->maxValue;
    return JAVACALL_DIO_OK;
}


/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_pin_read(const javacall_handle handle,
        /*OUT*/javacall_bool* pVal){
    GPIOHandle* pinHandle = (GPIOHandle*) handle;

    JAVACALL_REPORT_INFO1(JC_DIO, "Read value from pin %d", pinHandle->number);

    if(JAVACALL_DIO_FAIL == read_value_from_pin(pinHandle, pVal)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Can not read value from pin %d", pinHandle->number);
        return JAVACALL_DIO_FAIL;
    };

    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_pin_write(const javacall_handle handle,
        const javacall_bool val){
    GPIOHandle* pinHandle = (GPIOHandle*) handle;

    JAVACALL_REPORT_INFO2(JC_DIO, "Pin %d write value %d", pinHandle->number, val);

    if(JAVACALL_DIO_OK != write_value_to_pin(pinHandle, val)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Can not write value to GPIO pin %d", pinHandle->number);
        return JAVACALL_FAIL;
    }

    JAVACALL_REPORT_INFO1(JC_DIO, "Pin %d write value done", pinHandle->number);
    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_port_read(const javacall_handle handle,
        /*OUT*/javacall_int32* pVal){
    GPIOPortHandle* portHandle = (GPIOPortHandle*) handle;
    GPIOHandle* pin;
    javacall_dio_result listOperationsResult;
    javacall_bool readValue;
    javacall_int32 value = 0;
    int iterator = 0;
    javautil_list_reset_iterator(portHandle->pinList);

    while(JAVACALL_DIO_FAIL != (listOperationsResult = javautil_list_get_next(portHandle->pinList, (javacall_handle*) &pin))) {
        if(JAVACALL_DIO_INVALID_CONFIG == listOperationsResult) {
            JAVACALL_REPORT_ERROR(JC_DIO, "Invalid handle of GPIO port was passed to read function. Operation aborted");
            return JAVACALL_DIO_INVALID_CONFIG;
        }

        if(JAVACALL_DIO_OK != javacall_gpio_pin_read(pin, &readValue)) {
            JAVACALL_REPORT_ERROR1(JC_DIO, "Read operation to port failed, because error with pin %d occurred", pin->number);
            return JAVACALL_DIO_FAIL;
        };

        value |= (readValue == JAVACALL_TRUE ? 1 : 0) << iterator;

        iterator++;
    }
    *pVal = value;

    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_port_write(const javacall_handle handle,
        const javacall_int32 val){
    GPIOPortHandle* portHandle = (GPIOPortHandle*) handle;
    GPIOHandle* pin;
    javacall_dio_result listOperationsResult;
    javacall_bool writeValue;
    int iterator = 0;
    javautil_list_reset_iterator(portHandle->pinList);

    JAVACALL_REPORT_INFO1(JC_DIO, "Write value %d to port start", val);

    while(JAVACALL_DIO_FAIL != (listOperationsResult = javautil_list_get_next(portHandle->pinList, (javacall_handle*) &pin))) {
        if(JAVACALL_DIO_INVALID_CONFIG == listOperationsResult) {
            JAVACALL_REPORT_ERROR(JC_DIO, "Invalid handle of GPIO port was passed to write function. Operation aborted");
            return JAVACALL_DIO_INVALID_CONFIG;
        }
        writeValue = ((val >> iterator) & 0x01) ? JAVACALL_TRUE : JAVACALL_FALSE;
        if(JAVACALL_DIO_OK != javacall_gpio_pin_write(pin, writeValue)) {
            JAVACALL_REPORT_ERROR1(JC_DIO, "Write operation to port failed, because error with pin %d occurred", pin->number);
            return JAVACALL_DIO_FAIL;
        };
        iterator++;
    }
    JAVACALL_REPORT_INFO(JC_DIO, "Write value to port successfully done");
    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_pin_notification_start(const javacall_handle handle) {
    GPIOHandle* pinHandle = (GPIOHandle*) handle;

    JAVACALL_REPORT_INFO1(JC_DIO, "Enable notifications for pin %d", pinHandle->number);

    if(JAVACALL_TRUE == pinHandle->notificationsEnabled) {
        JAVACALL_REPORT_INFO1(JC_DIO, "Notifications are already activated for pin %d Nothing to do", pinHandle->number);
        return JAVACALL_DIO_OK;
    }

    if(JAVACALL_DIO_OK != activate_platform_notifications_to_pin(pinHandle)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Can not activate notifications for pin %d", pinHandle->number);
        return JAVACALL_DIO_FAIL;
    }

    if(JAVACALL_DIO_OK != add_pin_for_polling(pinHandle)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Can not poll value file for GPIO pin %d", pinHandle->number);
        return JAVACALL_DIO_FAIL;
    }

    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_pin_notification_stop(const javacall_handle handle) {
    GPIOHandle* pinHandle = (GPIOHandle*) handle;
    if(JAVACALL_FALSE == pinHandle->notificationsEnabled) {
        JAVACALL_REPORT_INFO1(JC_DIO, "Notifications are already disabled for pin %d Nothing to do", pinHandle->number);
        return JAVACALL_DIO_OK;
    }
    pthread_mutex_lock(&epoll_fd_lock);
    if(JAVACALL_DIO_OK != del_pin_from_polling(pinHandle)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Notifications disable for pin %d failed", pinHandle->number);
        pthread_mutex_unlock(&epoll_fd_lock);
        return JAVACALL_DIO_FAIL;
    }
    pthread_mutex_unlock(&epoll_fd_lock);
    return JAVACALL_DIO_OK;;
}


/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_port_notification_start(const javacall_handle handle) {
    GPIOPortHandle* portHandle = (GPIOPortHandle*) handle;

    if(JAVACALL_TRUE == portHandle->notificationsEnabled) {
        JAVACALL_REPORT_INFO(JC_DIO, "Notifications are allready enebled for port, Nothing to do");
        return JAVACALL_DIO_OK;
    }

    pthread_mutex_lock(&epoll_fd_lock);
    if(JAVACALL_DIO_OK != add_port_for_polling(portHandle)) {
        JAVACALL_REPORT_INFO(JC_DIO, "Fail to start notifications for port");
        pthread_mutex_unlock(&epoll_fd_lock);
        return JAVACALL_DIO_FAIL;
    }
    pthread_mutex_unlock(&epoll_fd_lock);
    portHandle->notificationsEnabled = JAVACALL_TRUE;
    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_port_notification_stop(const javacall_handle handle) {
    GPIOPortHandle* portHandle = (GPIOPortHandle*) handle;

    if(JAVACALL_FALSE == portHandle->notificationsEnabled) {
        JAVACALL_REPORT_INFO(JC_DIO, "Notifications are allready disabled for port, Nothing to do");
        return JAVACALL_DIO_OK;
    }

    pthread_mutex_lock(&epoll_fd_lock);
    if(JAVACALL_DIO_OK != del_port_from_polling(portHandle)) {
        JAVACALL_REPORT_INFO(JC_DIO, "Fail to stop notifications for port");
        pthread_mutex_unlock(&epoll_fd_lock);
        return JAVACALL_DIO_FAIL;
    }
    pthread_mutex_unlock(&epoll_fd_lock);
    portHandle->notificationsEnabled = JAVACALL_FALSE;
    return JAVACALL_DIO_OK;
}


/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_pin_direction_set(const javacall_handle handle,
        const javacall_bool direction) {
    GPIOHandle* pinHandle = (GPIOHandle*) handle;

    if(JAVACALL_DIO_OK != write_direction_to_file(pinHandle->directionFD, direction, JAVACALL_FALSE)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "There is unexpected error, when configure direction of GPIO pin %d", pinHandle->number);
        return JAVACALL_FAIL;
    };
    pinHandle->direction = direction;
    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_pin_direction_get(const javacall_handle handle,
        /*OUT*/javacall_bool* const pDirection) {
    GPIOHandle* pinHandle = (GPIOHandle*) handle;
    JAVACALL_REPORT_INFO2(JC_DIO, "Pin %d direction: %d", pinHandle->number, pinHandle->direction);
    *pDirection = pinHandle->direction;
    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_port_direction_set(const javacall_handle handle,
        const javacall_bool direction) {
    GPIOPortHandle* portHandle = (GPIOPortHandle*) handle;
    GPIOHandle* pin;
    javacall_result listOperationsResult;
    javautil_list_reset_iterator(portHandle->pinList);
    JAVACALL_REPORT_INFO1(JC_DIO, "Set direction %d to port", direction);

    while(JAVACALL_DIO_FAIL != (listOperationsResult = javautil_list_get_next(portHandle->pinList, (javacall_handle*) &pin))) {
        if(JAVACALL_DIO_INVALID_CONFIG == listOperationsResult) {
            JAVACALL_REPORT_ERROR(JC_DIO, "Invalid handle of GPIO port was passed to set direction function. Operation aborted");
            return JAVACALL_DIO_FAIL;
        }
        if(JAVACALL_DIO_OK != javacall_gpio_pin_direction_set(pin, direction)) {
            JAVACALL_REPORT_ERROR1(JC_DIO, "Set direction operation to port failed, because error with pin %d occurred", pin->number);
            return JAVACALL_DIO_FAIL;
        };
    }

    portHandle->direction = direction;
    return JAVACALL_DIO_OK;

}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_port_direction_get(const javacall_handle handle,
        /*OUT*/javacall_bool* const pDirection) {
    GPIOPortHandle* portHandle = (GPIOPortHandle*) handle;
    *pDirection = portHandle->direction;
    return JAVACALL_DIO_OK;
}

javacall_dio_result check_trigger(const javacall_gpio_trigger_mode trigger) {
    //: Linux support only rising, falling, both and none GPIO events, so only them can be supported in our port
    if(JAVACALL_TRIGGER_FALLING_EDGE != trigger &&
            JAVACALL_TRIGGER_RISING_EDGE != trigger &&
            JAVACALL_TRIGGER_NONE != trigger &&
            JAVACALL_TRIGGER_BOTH_EDGES != trigger) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Unsupported or invalid trigger value: %d", trigger);
        return JAVACALL_DIO_INVALID_CONFIG;
    }
    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_pin_open(javacall_int32 port,
        javacall_int32 pin, const javacall_gpio_dir direction,
        const javacall_gpio_mode mode, const javacall_gpio_trigger_mode trigger,
        const javacall_bool initValue,
        const javacall_bool exclusive,
        /*OUT*/ javacall_handle* pHandle) {

    javacall_int8 bufForDirectionFilename[DIRECTION_FILENAME_MAX_LENGTH];
    GPIOHandle* handle;
    javacall_dio_result pinCondition;
    int directionFD;

    JAVACALL_REPORT_INFO3(JC_DIO, "Try to open pin %d on port %d with direction %d", pin, port, direction);

    if (JAVACALL_FALSE == exclusive) {
        // exclusive mode only
        return JAVACALL_DIO_UNSUPPORTED_ACCESS_MODE;
    }

    if(port == PERIPHERAL_CONFIG_DEFAULT){ port = 0;}
    if(pin == PERIPHERAL_CONFIG_DEFAULT){ pin = 2;}

    if (0 < port) {
        JAVACALL_REPORT_ERROR(JC_DIO, "Only port 0 can be accepted");
        return JAVACALL_DIO_NOT_FOUND;
    }

    lock_pin_list();

    if (JAVACALL_FALSE == check_gpio_pin_is_free(pin)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "GPIO pin %d busy", pin);
        unlock_pin_list();
        return JAVACALL_DIO_BUSY;
    }

    pinCondition = check_trigger(trigger);
    if (JAVACALL_DIO_OK != pinCondition) {
        unlock_pin_list();
        // error is printed at check_trigger
        return pinCondition;
    }

    if(JAVACALL_DIO_OK != enable_gpio_pin(pin)) {
        unlock_pin_list();
        return JAVACALL_DIO_FAIL;
    }

    snprintf(bufForDirectionFilename, DIRECTION_FILENAME_MAX_LENGTH, GPIO_DIRECTION_FILENAME_TEMPLATE, pin);

    directionFD = open(bufForDirectionFilename, O_WRONLY);
	
    if(-1 != directionFD) {
        if(JAVACALL_DIO_OK != write_direction_to_file(directionFD, direction, initValue)) {
            JAVACALL_REPORT_ERROR1(JC_DIO, "Cannot set direction while opening GPIO pin %d. Open failed", pin);
            close(directionFD);
            unlock_pin_list();
            return JAVACALL_DIO_FAIL;
        };
    } else {
        directionFD = open(bufForDirectionFilename, O_RDONLY);
    }

    if(-1 == directionFD) {
        JAVACALL_REPORT_ERROR1(JC_DIO,
                               "Cannot open %s file to configure GPIO pin direction",
                               bufForDirectionFilename);
        unlock_pin_list();
        return JAVACALL_DIO_FAIL;
    }

    handle = (GPIOHandle*) javacall_malloc(sizeof(GPIOHandle));

    if(NULL == handle) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Unexpected error when open GPIO pin %d Out of memory", pin);
        close(directionFD);
        unlock_pin_list();
        return JAVACALL_DIO_FAIL;
    }

    handle->direction = direction;
    handle->number = pin;
    handle->notificationsEnabled = JAVACALL_FALSE;
    handle->inPort = JAVACALL_FALSE;
    handle->directionFD = directionFD;
    handle->edgeMode = trigger;
    handle->next = NULL;

    if(JAVACALL_DIO_OK != fill_value_fd_for_pin(handle)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Can not fill valueFD for pin %d Open failed", pin);
        close(directionFD);
        javacall_free(handle);
        unlock_pin_list();
        return JAVACALL_DIO_FAIL;
    }

    *pHandle = handle;
    add_to_pin_list(handle);
    unlock_pin_list();

    JAVACALL_REPORT_INFO1(JC_DIO, "GPIO pin %d open successfully done", pin);
    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_port_open_with_pins(
        const javacall_int32 portsAndPins[][4],
        const javacall_int32 pinCount,
        const javacall_gpio_dir direction,
        const javacall_int32 initValue,  const javacall_bool exclusive,
        /*OUT*/ javacall_handle* pHandle) {

    int i = 0;
    javacall_result pinState, result;
    javacall_int32 pin, mode;
    javacall_gpio_trigger_mode trigger;
    javacall_handle listHandle;
    GPIOPortHandle* handle;
    GPIOHandle* pinHandle;
    JAVACALL_REPORT_INFO(JC_DIO, "Open GPIO port started");

    if (JAVACALL_FALSE == exclusive) {
        // exclusive mode only
        return JAVACALL_DIO_UNSUPPORTED_ACCESS_MODE;
    }

    for(; i < pinCount; i++) {
        if(JAVACALL_FALSE == check_gpio_pin_is_free(portsAndPins[i][1])) {
            JAVACALL_REPORT_ERROR1(JC_DIO, "GPIO pin %d busy", portsAndPins[i][1]);
            return JAVACALL_DIO_BUSY;
        }
    }

    if(JAVACALL_OK != javautil_list_create(&listHandle)) {
        JAVACALL_REPORT_ERROR(JC_DIO, "Unexpected error occurred while GPIO port open. Can not create list of pins");
        return JAVACALL_DIO_FAIL;
    };


    for(i = 0; i<pinCount; i++) {
        pin = portsAndPins[i][1];
        mode = portsAndPins[i][2];
        trigger = portsAndPins[i][3];
        pinState = javacall_gpio_pin_open(0, pin, direction, mode, trigger, JAVACALL_FALSE, JAVACALL_TRUE, (javacall_handle*) &pinHandle);
        if(JAVACALL_DIO_OK != pinState) {
            JAVACALL_REPORT_ERROR1(JC_DIO, "Can not open pin %d, so port open operation aborted", pin);
            close_pins_in_list(listHandle);
            javautil_list_destroy(listHandle);
            return JAVACALL_DIO_FAIL;
        }
        pinHandle->inPort = JAVACALL_TRUE;
        if(JAVACALL_OK != javautil_list_add(listHandle, pinHandle)) {
            JAVACALL_REPORT_ERROR1(JC_DIO, "Unexpected error while adding pin %d to port list", pin);
            javacall_gpio_pin_close(pinHandle);
            close_pins_in_list(listHandle);
            javautil_list_destroy(listHandle);
            return JAVACALL_DIO_FAIL;
        };
    }

    handle = (GPIOPortHandle*) javacall_malloc(sizeof(GPIOPortHandle));
    if(NULL == handle) {
        JAVACALL_REPORT_ERROR(JC_DIO, "Unexpected error occurred while GPIO port open: Out of memory");
        close_pins_in_list(listHandle);
        javautil_list_destroy(listHandle);
        return JAVACALL_DIO_OUT_OF_MEMORY;
    }

    handle->pinList = listHandle;
    handle->direction = direction;
    handle->maxValue = (1 << pinCount) - 1;
    *pHandle = handle;

    if(JAVACALL_GPIO_OUTPUT_MODE == direction) {
        if(JAVACALL_DIO_OK != javacall_gpio_port_write(handle, initValue)) {
            javacall_gpio_port_close(handle);
            return JAVACALL_DIO_FAIL;
        };
    }
    JAVACALL_REPORT_INFO(JC_DIO, "Open GPIO port successfully done");

    handle->needPinClose = JAVACALL_TRUE;

    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_pin_get_group_id(const javacall_handle handle, javacall_int32* const port) {
    (void)handle;
    // : be replaced by implementer
    *port = (javacall_int32)1;
    return JAVACALL_DIO_OK;
}

void get_platform_direction_string(javacall_gpio_dir direction, javacall_bool initialValue, /*OUT*/ javacall_ascii_string* outString) {

    switch(direction) {
        case JAVACALL_GPIO_BOTH_MODE_INIT_INPUT:
        case JAVACALL_GPIO_INPUT_MODE:
            *outString = PLATFORM_IN_GPIO_DIRECTION;
            break;
        case JAVACALL_GPIO_BOTH_MODE_INIT_OUTPUT:
        case JAVACALL_GPIO_OUTPUT_MODE:
            if(JAVACALL_TRUE == initialValue) {
                *outString = PLATFORM_HIGH_GPIO_DIRECTION;
            } else {
                *outString = PLATFORM_LOW_GPIO_DIRECTION;
            }
            break;
        default:
            *outString = PLATFORM_OUT_GPIO_DIRECTION;
    }

};

static int is_gpio_already_exported(int pinNumber) {
    char pinNameBuffer[GPIO_FILENAME_MAX_LENGTH];
    struct stat sb;

    snprintf(pinNameBuffer, GPIO_FILENAME_MAX_LENGTH, GPIO_FILENAME_TEMPLATE, pinNumber);
    return stat(pinNameBuffer, &sb);
}

javacall_dio_result enable_gpio_pin(int pinNumber) {

    char pinNameBuffer[4];

    if(is_gpio_already_exported(pinNumber) == 0) {
        return JAVACALL_DIO_OK;
    }

    int expordFD = open(EXPORT_FILE_NAME, O_WRONLY);

    if(-1 == expordFD) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Can not open %s file to export gpio."
            "May be, there is necessary superuser rights", EXPORT_FILE_NAME);
        return JAVACALL_DIO_FAIL;
    }

    snprintf(pinNameBuffer, 4, PIN_NAME_TEMPLATE, pinNumber);

    if(-1 == write(expordFD, pinNameBuffer, 4)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "There is unexpected error, when open GPIO pin %d", pinNumber);
        close(expordFD);
        return JAVACALL_DIO_FAIL;
    };

    close(expordFD);
    return JAVACALL_DIO_OK;
};

javacall_dio_result disable_gpio_pin(int pinNumber) {

    javacall_int8 nameOfPinBuffer[4];

    int unexportFD = open(UNEXPORT_FILE_NAME, O_WRONLY);

    if(-1 != unexportFD) {
        snprintf(nameOfPinBuffer, 4, PIN_NAME_TEMPLATE, pinNumber);
        if(-1 == write(unexportFD, nameOfPinBuffer, 4)) {
            JAVACALL_REPORT_WARN1(JC_DIO, "Can not unexport GPIO pin %d", pinNumber);
        }
        close(unexportFD);
    } else {
        JAVACALL_REPORT_WARN(JC_DIO, "Can not open unexport file for GPIO");
        return JAVACALL_DIO_FAIL;
    }

    return JAVACALL_OK;
}

javacall_dio_result write_direction_to_file(int dirFD, javacall_gpio_dir direction, javacall_bool initialValue) {
    javacall_ascii_string platformDirectionString;

    if(-1 == dirFD) {
            JAVACALL_REPORT_ERROR(JC_DIO, "Invalid file descriptor was passed to configure GPIO pin direction");
            return JAVACALL_DIO_FAIL;
    }

    get_platform_direction_string(direction, initialValue, &platformDirectionString);

    if(-1 == write(dirFD, platformDirectionString, strlen(platformDirectionString))) {
        JAVACALL_REPORT_ERROR(JC_DIO, "There is unexpected error, when configure direction of GPIO pin");
    }

    return JAVACALL_DIO_OK;
}

javacall_dio_result write_value_to_pin(GPIOHandle* handle, javacall_bool value) {
    javacall_ascii_string outValue;

    outValue = JAVACALL_TRUE == value ? "1" : "0";

    lseek(handle->valueFD, 0, SEEK_SET);
    if(-1 == write(handle->valueFD, outValue, 1)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Can not write value to GPIO pin %d", handle->number);
        return JAVACALL_DIO_FAIL;
    }

    return JAVACALL_DIO_OK;
}

javacall_dio_result read_value_from_pin(GPIOHandle* handle, /*OUT*/ javacall_bool* value) {
    char inBuffer[2];

    lseek(handle->valueFD, 0, SEEK_SET);
    if(-1 == read(handle->valueFD, inBuffer, 2)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Can not read value from valueFD file for GPIO pin %d", handle->number);
        return JAVACALL_DIO_FAIL;
    }

    if(48 == inBuffer[0]) {
        *value = JAVACALL_FALSE;
    } else if(49 == inBuffer[0]) {
        *value = JAVACALL_TRUE;
    } else {
        JAVACALL_REPORT_ERROR3(JC_DIO, "Error values from value GPIO pin %d file was readed: %d %d", handle->number, inBuffer[0], inBuffer[1]);
        return JAVACALL_DIO_FAIL;
    }
    return JAVACALL_DIO_OK;
}

javacall_dio_result determine_name_of_value_file(GPIOHandle* handle, /*OUT*/javacall_ascii_string name) {

    if(NULL == handle) {
        return JAVACALL_DIO_FAIL;
    }

    snprintf(name, VALUE_FILENAME_MAX_LENGTH, GPIO_VALUE_FILENAME_TEMPLATE, handle->number);
    return JAVACALL_DIO_OK;
}


javacall_dio_result close_pins_in_list(javacall_handle list) {
    GPIOHandle* pin;
    javacall_dio_result listOperationsResult;
    javautil_list_reset_iterator(list);
    while(JAVACALL_FAIL != (listOperationsResult = javautil_list_get_next(list, (javacall_handle*) &pin))) {
        if(JAVACALL_INVALID_ARGUMENT == listOperationsResult) {
            JAVACALL_REPORT_ERROR(JC_DIO, "Invalid handle of GPIO port was passed to close function. Operation aborted");
            return JAVACALL_DIO_FAIL;
        }
        if (JAVACALL_FAIL == listOperationsResult) {
            javacall_gpio_pin_close(pin);
        } else {
            JAVACALL_REPORT_ERROR(JC_DIO, "Unknown error retrieving GPIO port handle. Operation aborted");
            return JAVACALL_DIO_FAIL;
        }
    }
    return JAVACALL_DIO_OK;
}

void* pin_events_listener_function(void* data) {
    epoll_event events[17];
    int eventsCount, i;
    javacall_bool pinValue;
    javacall_int32 portValue;

    while(1) {
        eventsCount = epoll_wait(epoll_descriptor, events, 17, 1000);
        pthread_mutex_lock(&epoll_fd_lock);
        if(0 == pinCountForPolling) {
            close(epoll_descriptor);
            epoll_descriptor = -1;
            notificationThreadActivated = JAVACALL_FALSE;
            break;
        }
        for(i=0; i < eventsCount; i++) {
            polling_data* polling_data = events[i].data.ptr;
            uint32_t ev = events[i].events;
            if(JAVACALL_TRUE != polling_data->pin->notificationsEnabled) {
                continue;
            }
            if (ev & EPOLLPRI) {
                if(NULL == polling_data->port) {
                    if(JAVACALL_DIO_OK != javacall_gpio_pin_read(polling_data->pin, &pinValue) &&
                                   pinValue == polling_data->pin->lastValue) {
                        continue;
                    }
                    javanotify_gpio_pin_value_changed(polling_data->pin, pinValue);
                } else {
                    javacall_gpio_port_read(polling_data->port, &portValue);
                    if(portValue != polling_data->port->lastValue) {
                        polling_data->port->lastValue = portValue;
                        javanotify_gpio_port_value_changed(polling_data->port, portValue);
                    }
                }
            }
        }
        pthread_mutex_unlock(&epoll_fd_lock);
    }
    pthread_mutex_unlock(&epoll_fd_lock);
    JAVACALL_REPORT_INFO(JC_DIO, "Notifications thread stop executing. No pins for polling");
    return NULL;
}

javacall_dio_result add_pin_for_polling(GPIOHandle* pinHandle) {
    polling_data* poll_data;
    epoll_event* new_event = (epoll_event*) javacall_malloc(sizeof(epoll_event));

    JAVACALL_REPORT_INFO1(JC_DIO, "Adding pin %d for polling", pinHandle->number);
    
    if(NULL == new_event) {
        JAVACALL_REPORT_ERROR(JC_DIO, "OUT of memory");
        return JAVACALL_DIO_OUT_OF_MEMORY;
    }

    poll_data = (polling_data*) javacall_malloc(sizeof(polling_data));
    if(NULL == poll_data) {
        javacall_free(new_event);
        return JAVACALL_DIO_OUT_OF_MEMORY;
    }
    poll_data->pin = pinHandle;
    poll_data->port = NULL;

    new_event->events = EPOLLPRI;
    new_event->data.ptr = poll_data;

    pthread_mutex_lock(&epoll_fd_lock);
    if(-1 == epoll_descriptor) {
        epoll_descriptor = epoll_create(17);
    }
    if(-1 == epoll_ctl(epoll_descriptor, EPOLL_CTL_ADD, pinHandle->valueFD, new_event)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Add value file of pin %d to polling failed", pinHandle->number);
        pthread_mutex_unlock(&epoll_fd_lock);
        javacall_free(new_event);
        javacall_free(poll_data);
        return JAVACALL_DIO_FAIL;
    };
    pinCountForPolling++;
    activate_notification_thread();
    pinHandle->notificationsEnabled = JAVACALL_TRUE;
    pinHandle->pollEvent = new_event;
    pthread_mutex_unlock(&epoll_fd_lock);
    return JAVACALL_DIO_OK;
}

javacall_dio_result del_pin_from_polling(GPIOHandle* pinHandle) {

    JAVACALL_REPORT_INFO1(JC_DIO, "Remove value file of pin %d from polling", pinHandle->number);
    if(-1 == epoll_ctl(epoll_descriptor, EPOLL_CTL_DEL, pinHandle->valueFD, NULL)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Del value file of pin %d to polling failed", pinHandle->number);
        return JAVACALL_FAIL;
    };
    pinCountForPolling--;
    javacall_free(pinHandle->pollEvent->data.ptr);
    javacall_free(pinHandle->pollEvent);
    pinHandle->pollEvent = NULL;
    pinHandle->notificationsEnabled = JAVACALL_FALSE;
    return JAVACALL_DIO_OK;
}

javacall_dio_result fill_value_fd_for_pin(GPIOHandle* handle){
    char nameBuffer[VALUE_FILENAME_MAX_LENGTH];

    if(JAVACALL_DIO_OK != determine_name_of_value_file(handle, nameBuffer)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Can not fill valueFD for pin %d. Operation failed", handle->number);
        return JAVACALL_DIO_FAIL;
    }

    handle->valueFD = open(nameBuffer, O_RDWR);

    if(-1 == handle->valueFD) {
        JAVACALL_REPORT_ERROR(JC_DIO, "Can not open file %s for read value from GPIO pin");
        return JAVACALL_DIO_FAIL;
    }
    return JAVACALL_DIO_OK;
}

javacall_dio_result add_port_for_polling(GPIOPortHandle* portHandle) {
    GPIOHandle* pin;
    epoll_event* new_event;
    polling_data* poll_data;
    javacall_bool success = JAVACALL_TRUE;
    javautil_list_reset_iterator(portHandle->pinList);

    while(JAVACALL_FAIL != javautil_list_get_next(portHandle->pinList, (javacall_handle*) &pin)) {

        if(JAVACALL_TRUE == pin->notificationsEnabled) {
            JAVACALL_REPORT_WARN1(JC_DIO, "Notifications are already activated for pin %d it is not normal", pin->number);
            continue;
        }

        if(JAVACALL_DIO_OK != activate_platform_notifications_to_pin(pin)) {
            JAVACALL_REPORT_ERROR1(JC_DIO, "Platform notifications for pin %d cannot be enabled", pin->number);
            return JAVACALL_DIO_FAIL;
        }

        new_event = (epoll_event*) javacall_malloc(sizeof(epoll_event));
        if(NULL == new_event) {
            JAVACALL_REPORT_ERROR(JC_DIO, "OUT of memory");
            success = JAVACALL_FALSE;
            break;
        }

        poll_data = (polling_data*) javacall_malloc(sizeof(polling_data));
        if(NULL == poll_data) {
            javacall_free(new_event);
            success = JAVACALL_FALSE;
            break;
        }
        poll_data->pin = pin;
        poll_data->port = portHandle;

        new_event->events = EPOLLPRI;
        new_event->data.ptr = poll_data;
        if(-1 == epoll_descriptor) {
            epoll_descriptor = epoll_create(17);
        }
        if(-1 == epoll_ctl(epoll_descriptor, EPOLL_CTL_ADD, pin->valueFD, new_event)) {
            JAVACALL_REPORT_ERROR1(JC_DIO, "Add value file of pin %d to polling failed", pin->number);
            javacall_free(new_event);
            javacall_free(poll_data);
            success = JAVACALL_FALSE;
            break;
        };
        pinCountForPolling++;
        activate_notification_thread();
        pin->pollEvent = new_event;
        pin->notificationsEnabled = JAVACALL_TRUE;
    }

    if(JAVACALL_TRUE != success) {
        javautil_list_reset_iterator(portHandle->pinList);
        while(JAVACALL_OK == javautil_list_get_next(portHandle->pinList, (javacall_handle*) &pin)) {
            if(JAVACALL_OK != del_pin_from_polling(pin)) {
                JAVACALL_REPORT_WARN1(JC_DIO, "Can not remove pin %d from polling", pin->number);
            }
        }
        JAVACALL_REPORT_ERROR(JC_DIO, "Fail while set notifications for GPIO port");
        return JAVACALL_DIO_FAIL;
    }

    return JAVACALL_DIO_OK;
}

javacall_dio_result del_port_from_polling(GPIOPortHandle* portHandle) {
    GPIOHandle* pin;
    javautil_list_reset_iterator(portHandle->pinList);


    while(JAVACALL_OK == javautil_list_get_next(portHandle->pinList, (javacall_handle*) &pin)) {
        if(JAVACALL_DIO_OK != del_pin_from_polling(pin)) {
            JAVACALL_REPORT_WARN1(JC_DIO, "Can not remove pin %d from polling", pin->number);
        }
    }

    return JAVACALL_DIO_OK;
}

javacall_dio_result activate_platform_notifications_to_pin(GPIOHandle* pin) {
    char bufferForEdgeFileName[EDGE_FILENAME_MAX_LENGTH];
    int edgeFD;
    javacall_ascii_string platformValue;

    JAVACALL_REPORT_INFO1(JC_DIO, "Enable notifications for pin %d", pin->number);

    snprintf(bufferForEdgeFileName, EDGE_FILENAME_MAX_LENGTH, GPIO_EDGE_FILENAME_TEMPLATE, pin->number);

    edgeFD = open(bufferForEdgeFileName, O_WRONLY);

    if(-1 == edgeFD) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Can open edge file for pin %d", pin->number);
        return JAVACALL_FAIL;
    }
    get_platform_edge_string(pin->edgeMode, &platformValue);
    if(-1 == write(edgeFD, platformValue, strlen(platformValue) + 1)) {
        JAVACALL_REPORT_ERROR1(JC_DIO, "Can not set notifications to edge file for pin %d", pin->number);
        close(edgeFD);
        return JAVACALL_DIO_FAIL;
    }
    close(edgeFD);
    return JAVACALL_DIO_OK;
}

javacall_bool is_pin_pull_up(int pinNumber) {
    if(2 == pinNumber || 3 == pinNumber) {
        return JAVACALL_TRUE;
    }
    return JAVACALL_FALSE;
}

void get_platform_edge_string(javacall_gpio_trigger_mode edgeMode, javacall_ascii_string* string) {
    switch(edgeMode) {
        case JAVACALL_TRIGGER_NONE:
            *string = PLATFORM_NONE_GPIO_EDGE;
            break;
        case JAVACALL_TRIGGER_RISING_EDGE:
            *string = PLATFORM_RISING_GPIO_EDGE;
            break;
        case JAVACALL_TRIGGER_FALLING_EDGE:
            *string = PLATFORM_FALLING_GPIO_EDGE;
            break;
        case JAVACALL_TRIGGER_BOTH_EDGES:
            *string = PLATFORM_BOTH_GPIO_EDGE;
            break;
    }
}

void activate_notification_thread() {
    if(1 == pinCountForPolling && JAVACALL_FALSE == notificationThreadActivated) {
        pthread_create(&notificationThread, NULL, &pin_events_listener_function, NULL);
        notificationThreadActivated = JAVACALL_TRUE;
        JAVACALL_REPORT_INFO(JC_DIO, "Notifications thread created and starts executing");
    }
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_pin_lock(const javacall_handle handle, javacall_handle* const owner) {
    (void)handle;
    (void)owner;
    // EXCLUSIVE mode is supported only
    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_pin_unlock(const javacall_handle handle) {
    (void)handle;
    // EXCLUSIVE mode is supported only
    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_pin_set_trigger(const javacall_handle handle, const javacall_gpio_trigger_mode trigger) {
    GPIOHandle* pin  = (GPIOHandle*)handle;
    if (JAVACALL_DIO_OK == check_trigger(trigger)) {
        pin->edgeMode = trigger;
        return activate_platform_notifications_to_pin(pin);
    }
    return JAVACALL_DIO_UNSUPPORTED_OPERATION;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_pin_get_trigger(const javacall_handle handle, javacall_gpio_trigger_mode* const trigger) {
    GPIOHandle* pConfig = (GPIOHandle*)handle;
    *trigger = pConfig->edgeMode;
    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_port_lock(const javacall_handle handle, javacall_handle* const blocker) {
    (void)handle;
    (void)blocker;
    // EXCLUSIVE mode is supported only
    return JAVACALL_DIO_OK;
}

/**
* See javacall_gpio.h for definition
*/
javacall_dio_result javacall_gpio_port_unlock(const javacall_handle handle) {
    (void)handle;
    // EXCLUSIVE mode is supported only
    return JAVACALL_DIO_OK;
}
