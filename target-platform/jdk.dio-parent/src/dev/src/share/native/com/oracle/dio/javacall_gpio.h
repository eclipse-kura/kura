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

#ifndef __JAVACALL_GPIO_H
#define __JAVACALL_GPIO_H

#ifdef __cplusplus
extern "C"{
#endif

/**
 * @file javacall_gpio.h
 * @ingroup GPIOAPI
 * @brief Javacall interfaces for GPIO device access JSR
 *
 */

#include "javacall_defs.h"
#include "javacall_dio.h"

/**
 * @defgroup GPIOAPI GPIO API
 * @ingroup DeviceAccess
 * @{
 */

/**
 * @defgroup MandatoryGPIO Mandatory GPIO API
 * @ingroup GPIOAPI
 *
 *
 * @{
 */

/**
 * @enum javacall_gpio_dir
 * @brief pin/port direction values
 */
typedef enum {
    /** Input only direction */
    JAVACALL_GPIO_INPUT_MODE  = 0,
    /** Output only direction */
    JAVACALL_GPIO_OUTPUT_MODE = 1,
    /** Both direction mode, initial state is input */
    JAVACALL_GPIO_BOTH_MODE_INIT_INPUT = 2,
    /** Both direction mode, initial state is output */
    JAVACALL_GPIO_BOTH_MODE_INIT_OUTPUT = 3
}javacall_gpio_dir;

/** Used to indicate that the default value of a configuration parameter is necessary*/
#define JAVACALL_DEFAULT_INT32_VALUE    (-1)
/**
 * @enum  javacall_gpio_mode
 * @brief The drive mode of the pin or the port is got with
 * bitwise OR of this values */
typedef enum {
    /** Input mode, I/O pull-up  */
    JAVACALL_MODE_INPUT_PULL_UP  = 1,
    /** Input mode, I/O pull-down */
    JAVACALL_MODE_INPUT_PULL_DOWN = 2,
    /** Output mode, I/O push-up */
    JAVACALL_MODE_OUTPUT_PUSH_PULL = 4,
    /** Output mode, I/O open-drain */
    JAVACALL_MODE_OUTPUT_OPEN_DRAIN  = 8
} javacall_gpio_mode;


/**
 * @enum javacall_gpio_trigger_mode
 * @brief the interrupt trigger events
 */
typedef enum {
    /** No interrupt trigger. */
    JAVACALL_TRIGGER_NONE = 0,
    /** Falling edge trigger. */
    JAVACALL_TRIGGER_FALLING_EDGE = 1,
    /** Rising edge trigger. */
    JAVACALL_TRIGGER_RISING_EDGE = 2,
    /** Rising edge trigger. */
    JAVACALL_TRIGGER_BOTH_EDGES = 3,
    /** High level trigger. */
    JAVACALL_TRIGGER_HIGH_LEVEL = 4,
    /** Low level trigger. */
    JAVACALL_TRIGGER_LOW_LEVEL = 5,
    /** Both levels trigger. */
    JAVACALL_TRIGGER_BOTH_LEVELS = 6,
} javacall_gpio_trigger_mode;


/**
 * Release and close the GPIO pin.
 *
 * @param handle handle of the GPIO pin.
 *
 * @retval JAVACALL_DIO_OK          success
 * @retval JAVACALL_DIO_FAIL        if there was an error
 */
javacall_dio_result javacall_gpio_pin_close(javacall_handle handle);

/**
 * Release and close the GPIO port.
 *
 * @param handle handle of the GPIO port.
 *
 * @retval JAVACALL_DIO_OK          success
 * @retval JAVACALL_DIO_FAIL        if there was an error
 */
javacall_dio_result javacall_gpio_port_close(javacall_handle handle);

/**
*Get Max value of GPIO port
*
*@param handle GPIO port handle
*@param pVal  the pointer to the variable to receive maximum value of GPIO port
*
*@retval JAVACALL_DIO_OK    read success
*@retval JAVACALL_DIO_FAIL  read fail
*/
javacall_dio_result javacall_gpio_port_get_max_value(const javacall_handle handle,
        /*OUT*/javacall_int32* pVal);


/**
*Read data from given GPIO pin number
*
*@param handle GPIO       pin handle
*@param val                       the pointer to the variable to receive value of GPIO pin
*
*@retval JAVACALL_DIO_OK        read success
*@retval JAVACALL_DIO_FAIL      on read error
*/
javacall_dio_result javacall_gpio_pin_read(const javacall_handle handle,
        /*OUT*/javacall_bool* pVal);

/**
* Write data to GPIO pin
*
*@param     handle GPIO pin handle
*@param     val value to be written to pin
*
*@retval JAVACALL_DIO_OK   write success
*@retval JAVACALL_DIO_FAIL write fail
*/
javacall_dio_result javacall_gpio_pin_write(const javacall_handle handle,
        const javacall_bool val);


/**
*Read data from GPIO port
*
*@param handle GPIO port handle
*@param val  the pointer to the variable to receive value of GPIO port
*
*@retval JAVACALL_DIO_OK    read success
*@retval JAVACALL_DIO_FAIL  read fail
*/
javacall_dio_result javacall_gpio_port_read(const javacall_handle handle,
        /*OUT*/javacall_int32* pVal);


/**
*Write data to GPIO port
*
*@param handle GPIO port handle
*@param val value to be written to port
*
*@retval JAVACALL_DIO_OK     write success
*@retval JAVACALL_DIO_FAIL  write fail
*/
javacall_dio_result javacall_gpio_port_write(const javacall_handle handle,
        const javacall_int32 val);

/**
* Start notification of given GPIO pin
*
*@see javanotify_gpio_pin_value_changed
*
*@param handle GPIO pin handle
*
*@retval JAVACALL_DIO_OK
*@retval JAVACALL_DIO_UNSUPPORTED_OPERATION  if pin was setup
*          for output
*@retval JAVACALL_DIO_FAIL  general IO error
*/
javacall_dio_result javacall_gpio_pin_notification_start(const javacall_handle handle);

/**
* Stop notification of given GPIO pin
*
*@see javanotify_gpio_pin_value_changed
*
*@param handle GPIO pin handle
*
*@retval JAVACALL_DIO_OK
*/
javacall_dio_result javacall_gpio_pin_notification_stop(const javacall_handle handle);

/**
* Start notification of given GPIO port
*
*@see javanotify_gpio_port_value_changed
*
*@param handle GPIO port handle
*
*@retval JAVACALL_DIO_OK
*@retval JAVACALL_DIO_UNSUPPORTED_OPERATION  if port was setup
*          for output
*@retval JAVACALL_DIO_FAIL  general IO error
*/
javacall_dio_result javacall_gpio_port_notification_start(const javacall_handle handle);


/**
* Start notification of given GPIO port
*
*@see javanotify_gpio_port_value_changed
*
*@param handle GPIO port handle
*
*@retval JAVACALL_DIO_OK
*/
javacall_dio_result javacall_gpio_port_notification_stop(const javacall_handle handle);

/**
* change direction of given GPIO pin
*
*@param handle GPIO pin handle
*@param direction 1 is output mode ,0 is input mode
*
*@retval JAVACALL_DIO_OK
* @retval JAVACALL_DIO_UNSUPPORTED_OPERATION  if
*         <code>direction</code> value is not supported
* @retval JAVACALL_DIO_FAIL  general IO error
*/
javacall_dio_result javacall_gpio_pin_direction_set(const javacall_handle handle,
        const javacall_bool direction);

/**
*get current direction of given GPIO pin
*
*@param handle GPIO pin handle
*@param pDirection the pointer to the variable to receive GPIO pin mode
*
*@retval JAVACALL_DIO_OK read pin status success
*/
javacall_dio_result javacall_gpio_pin_direction_get(const javacall_handle handle,
        /*OUT*/javacall_bool* const pDirection);

/**
* change direction of given GPIO port
*
*@param handle GPIO port handle
*@param direction 1 is output mode ,0 is input mode
*
*@retval JAVACALL_DIO_OK
* @retval JAVACALL_DIO_UNSUPPORTED_OPERATION  if
*         <code>direction</code> value is not supported
* @retval JAVACALL_DIO_FAIL  general IO error
*/
javacall_dio_result javacall_gpio_port_direction_set(const javacall_handle handle,
        const javacall_bool direction);

/**
*get current direction of given GPIO port
*
*@param handle GPIO port handle
*@param pinMode the pointer to the variable to receive GPIO pin mode
*
*@retval JAVACALL_DIO_OK read port status success
*/
javacall_dio_result javacall_gpio_port_direction_get(const javacall_handle handle,
        /*OUT*/javacall_bool* const pDirection);

/**
 * Open GPIO pin by given pin number.
 * <p>
 * A peripheral device may be opened in shared mode if supported
 * by the underlying driver and hardware and if it is not
 * already opened in exclusive mode. A peripheral device may be
 * opened in exclusive mode if supported by the underlying
 * driver and hardware and if it is not already opened.
 *
 * @param port hardware GPIO Port's number
 * @param pin hardware GPIO Pin's number in the port
 * @param direction direction for pin JAVACALL_GPIO_INPUT_MODE, JAVACALL_GPIO_OUTPUT_MODE,
 *        JAVACALL_GPIO_BOTH_MODE_INIT_INPUT or JAVACALL_GPIO_BOTH_MODE_INIT_OUTPUT
 * @param mode the drive mode of the pin, a bitwise OR of drive mode possible values
 * @param trigger the interrupt trigger events, one of javacall_gpio_trigger_mode
 * @param initValue the initial value of the pin when direction set for output
 * @param exclusive      exclusive mode flag: JAVACALL_TRUE
 *                       means EXCLUSIVE mode, SHARED for the
 *                       rest
 * @param pHandle pointer to store the GPIO pin handle
 *
 * @retval JAVACALL_DIO_OK          success
 * @retval JAVACALL_DIO_FAIL        general I/O error
 * @retval JAVACALL_DIO_NOT_FOUND   Peripheral is not found
 * @retval JAVACALL_DIO_BUSY   attempt to open already opened pin
 *         in exclusive mode or pin was locked by {@link
 *         #javacall_gpio_pin_lock(const javacall_handle,
 *         javacall_handle* const)}
 * @retval JAVACALL_DIO_UNSUPPORTED_ACCESS_MODE    if EXCLUSIVE or SHARED
 *         mode is not supported
 *
 * Note: call parameters must conform specifications
 */
javacall_dio_result javacall_gpio_pin_open(const javacall_int32 port,
        const javacall_int32 pin, const javacall_gpio_dir direction,
        const javacall_gpio_mode mode, const javacall_gpio_trigger_mode trigger,
        const javacall_bool initValue,
        const javacall_bool exclusive,
        /*OUT*/ javacall_handle* pHandle);

/**
 *Open GPIO port as given pins.
 * @param portsAndPins array of pairs port and pin, where
 * portsAndPins[n][0] is hardware port's number and
 * portsAndPins[n][1] is hardware pin's number in this port.
 * portsAndPins[n][3] is hardware pin's driver mode.
 * portsAndPins[n][4] is hardware pin's trigger type.
 * The pins are arrange in the exact same order they compose the
 * virtual port
 * @param pinCount number of port and pin pairs in the first parameter
 * @param direction direction for pin JAVACALL_GPIO_INPUT_MODE, JAVACALL_GPIO_OUTPUT_MODE,
 *        JAVACALL_GPIO_BOTH_MODE_INIT_INPUT or JAVACALL_GPIO_BOTH_MODE_INIT_OUTPUT
 * @param trigger the interrupt trigger events, one of javacall_gpio_trigger_mode
 * @param initValue the initial value of the port when direction set for output
 * @param exclusive      exclusive mode flag: JAVACALL_TRUE
 *                       means EXCLUSIVE mode, SHARED for the
 *                       rest
 * @param pHandle pointer to store the virtual GPIO port handle
 * @retval JAVACALL_DIO_OK          success
 * @retval JAVACALL_DIO_INVALID_CONFIG if one or more of parameters are wrong or unsupported by target platform
 * @retval JAVACALL_DIO_FAIL        if there was an error
 * @retval JAVACALL_DIO_BUSY  Peripheral is in busy state. so it's not available.
 * @retval JAVACALL_DIO_UNSUPPORTED_ACCESS_MODE    if EXCLUSIVE or SHARED
 *         mode is not supported
 *
 * Note: call parameters must conform specifications
 */
javacall_dio_result javacall_gpio_port_open_with_pins(
        const javacall_int32 portsAndPins[][4],
        const javacall_int32 pinCount,
        const javacall_gpio_dir direction,
        const javacall_int32 initValue, const javacall_bool exclusive,
        /*OUT*/ javacall_handle* pHandle);


/**
 * Returns power control group of this pin. It is used for
 * power management notification.
 *
 * @param handle open device handle
 * @param grp    power management group
 *
 * @return javacall_dio_result JAVACALL_DIO_FAIL if the device was
 *         closed, JAVACALL_DIO_OK otherwise
 */
javacall_dio_result javacall_gpio_pin_get_group_id(const javacall_handle handle, javacall_int32* const grp);

/**
 * Attempts to lock for exclusive access the underlying
 * peripheral device resource.
 * <p>
 * Checks for status and returns immediately if the resource is
 * already locked.
 *
 * @param handle of open pin
 * @param owner a pointer to current owner handle if attempt
 *              failed
 *
 * @retval JAVACALL_DIO_OK if exclusive access was granted,
 *         JAVACALL_DIO_FAIL if the resource is locked by other
 *         application
 */
javacall_dio_result javacall_gpio_pin_lock(const javacall_handle handle, javacall_handle* const owner);

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
javacall_dio_result javacall_gpio_pin_unlock(const javacall_handle handle);

/**
 * Attempts to lock given port for exclusive access.
 * <p>
 * Checks for status and returns immediately if the resource is
 * already locked.
 *
 * @param handle of open port
 * @param blocker a pointer to handle that prevents to lock the
 *                resource.
 *
 * @retval JAVACALL_DIO_OK if exclusive access was granted,
 * @retval JAVACALL_DIO_FAIL if the resource is locked by other
 *         application
 */
javacall_dio_result javacall_gpio_port_lock(const javacall_handle handle, javacall_handle* const blocker);

/**
 * Releases from exclusive access the underlying peripheral
 * device resource.
 * <p>
 * Returns silently if the resource was not
 * locked to <code>handle</code>
 *
 * @param handle open resource handle
 *
 * @return JAVACALL_DIO_OK if <code>handle</code> is owner of the
 *         resource and the resuorce is released
 * @return JAVACALL_DIO_FAIL otherwise
 *
 */
javacall_dio_result javacall_gpio_port_unlock(const javacall_handle handle);

/**
 * Changes trigger mode of a pin.
 *
 *
 * @param handle    open pin handle
 * @param trigger   trigger mode
 *
 * @retval JAVACALL_DIO_OK if trigger mode was changed
 * @retval JAVACALL_DIO_UNSUPPORTED_OPERATION  if <code>trigger</code> value is not
 *         supported
 * @retval JAVACALL_DIO_FAIL  general IO error
 */
javacall_dio_result javacall_gpio_pin_set_trigger(const javacall_handle handle, const javacall_gpio_trigger_mode trigger);

/**
 * Returns trigger mode of a pin.
 *
 *
 * @param handle    open pin handle
 * @param trigger   a pointer to trigger mode storage
 *
 * @return JAVACALL_DIO_OK if trigger mode was acquired
 */
javacall_dio_result javacall_gpio_pin_get_trigger(const javacall_handle handle, javacall_gpio_trigger_mode* const trigger);

/** @} */
/** @} */


#ifdef __cplusplus
}
#endif

#endif //__JAVACALL_GPIO_H
