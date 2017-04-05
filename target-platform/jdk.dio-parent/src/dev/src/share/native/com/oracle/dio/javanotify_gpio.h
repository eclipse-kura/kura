/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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


#ifndef __JAVANOTIFY_GPIO_H
#define __JAVANOTIFY_GPIO_H

#ifdef __cplusplus
extern "C"{
#endif

/**
 * @file javanotify_gpio.h
 * @ingroup GPIOAPI
 * @brief Notification functions for GPIO of DeviceAccess
 *
 */

#include "javacall_defs.h"


/**
 * @defgroup NotificationGPIO Notification API for GPIO
 * @ingroup GPIOAPI
 * @{
 */


/**
*this function is called when platform want to send
*GPIO pin value changed event to VM.
*
* @param pinId    open GPIO pin handle
* @param value      value of given GPIO pin
*/
void javanotify_gpio_pin_value_changed(const javacall_handle handle,
        const javacall_int32 value);

/**
*this function is called when platform want to send
*GPIO port value changed event to VM.
*
* @param portId    open GPIO port handle
* @param value      value of given GPIO port
*/
void javanotify_gpio_port_value_changed(const javacall_handle portId,
        const javacall_int32 value);

/** @} */

#ifdef __cplusplus
}
#endif


#endif //__JAVANOTIFY_GPIO_H

