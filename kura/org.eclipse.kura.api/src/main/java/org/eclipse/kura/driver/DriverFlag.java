/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.driver;

/**
 * This represents all the Kura driver specific flag codes.
 */
public enum DriverFlag {

    /** The COMM device not connected. */
    COMM_DEVICE_NOT_CONNECTED,

    /** The custom error 0. */
    CUSTOM_ERROR_0,

    /** The custom error 1. */
    CUSTOM_ERROR_1,

    /** The custom error 2. */
    CUSTOM_ERROR_2,

    /** The custom error 3. */
    CUSTOM_ERROR_3,

    /** The custom error 4. */
    CUSTOM_ERROR_4,

    /** The custom error 5. */
    CUSTOM_ERROR_5,

    /** The custom error 6. */
    CUSTOM_ERROR_6,

    /** The custom error 7. */
    CUSTOM_ERROR_7,

    /** The custom error 8. */
    CUSTOM_ERROR_8,

    /** The custom error 9. */
    CUSTOM_ERROR_9,

    /** The device or interface busy. */
    DEVICE_OR_INTERFACE_BUSY,

    /** The driver error channel address invalid. */
    DRIVER_ERROR_CHANNEL_ADDRESS_INVALID,

    /** The driver error channel not accessible. */
    DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,

    /** The driver error channel value type conversion exception. */
    DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,

    /** The driver error unspecified. */
    DRIVER_ERROR_UNSPECIFIED,

    /** The driver threw unknown exception. */
    DRIVER_THREW_UNKNOWN_EXCEPTION,

    /** The read failure. */
    READ_FAILURE,

    /** The read successful. */
    READ_SUCCESSFUL,

    /** The unknown. */
    UNKNOWN,

    /** The write failure. */
    WRITE_FAILURE,

    /** The write successful. */
    WRITE_SUCCESSFUL;

}
