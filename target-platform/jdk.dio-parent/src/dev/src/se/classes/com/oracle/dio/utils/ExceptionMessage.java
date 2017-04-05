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

package com.oracle.dio.utils;

public final class ExceptionMessage {
    private ExceptionMessage() {}

    public static String format(int format, Object... parameters) {
        return String.format(strings[format], parameters);
    }

    public static final int DEVICE_FIRST = 0;
    public static final int DEVICE_LOCKED_BY_OTHER_APP               = DEVICE_FIRST + 0;
    public static final int DEVICE_NULL_CONFIG_OR_INTF               = DEVICE_FIRST + 1;
    public static final int DEVICE_CONFIG_PROBLEM                    = DEVICE_FIRST + 2;
    public static final int DEVICE_EXCLUSIVE_MODE_UNSUPPORTED        = DEVICE_FIRST + 3;
    public static final int DEVICE_NULL_INTF                         = DEVICE_FIRST + 4;
    public static final int DEVICE_NOT_FOUND                         = DEVICE_FIRST + 5;
    public static final int DEVICE_HAS_DIFFERENT_TYPE                = DEVICE_FIRST + 6;
    public static final int DEVICE_NULL_NAME_AND_PROPERTIES          = DEVICE_FIRST + 7;
    public static final int DEVICE_INVALID_ID                        = DEVICE_FIRST + 8;
    public static final int DEVICE_NONUNIQUE_ID                      = DEVICE_FIRST + 9;
    public static final int DEVICE_ALREADY_EXISTING_CONFIG           = DEVICE_FIRST + 10;
    public static final int DEVICE_NEGATIVE_ID                       = DEVICE_FIRST + 11;
    public static final int DEVICE_INVALID_CLASSNAME                 = DEVICE_FIRST + 12;
    public static final int DEVICE_FAULT_CONFIG_INSTANCE             = DEVICE_FIRST + 13;
    public static final int DEVICE_FOUND_BUT_PERIPHERAL_IS_BUSY      = DEVICE_FIRST + 14;
    public static final int DEVICE_DRIVERS_NOT_MATCH                 = DEVICE_FIRST + 15;
    public static final int DEVICE_DRIVER_MISSING                    = DEVICE_FIRST + 16;
    public static final int DEVICE_NULL_ACTIONS                      = DEVICE_FIRST + 17;
    public static final int DEVICE_NULL_NAME                         = DEVICE_FIRST + 18;
    public static final int DEVICE_EMPTY_ACTIONS                     = DEVICE_FIRST + 19;
    public static final int DEVICE_INVALID_PERMISSION                = DEVICE_FIRST + 20;
    public static final int DEVICE_READONLY_PERMISSION_COLLECTION    = DEVICE_FIRST + 21;
    public static final int DEVICE_OPEN_WITH_DEVICENAME_UNSUPPORTED  = DEVICE_FIRST + 22;
    public static final int BUFFER_IS_MODIFIED                       = DEVICE_FIRST + 23;
    public static final int DEVICE_LAST = BUFFER_IS_MODIFIED;

    public static final int ADC_FIRST = DEVICE_LAST + 1;
    public static final int ADC_ANOTHER_OPERATION_PROGRESS           = ADC_FIRST + 0;
    public static final int ADC_NONPOSITIVE_INTERVAL                 = ADC_FIRST + 1;
    public static final int ADC_LESS_MINIMAL_INTERVAL                = ADC_FIRST + 2;
    public static final int ADC_CANNOT_START_ACQUISITION             = ADC_FIRST + 3;
    public static final int ADC_ARGUMENT_LOW_GREATER_THAN_HIGH       = ADC_FIRST + 4;
    public static final int ADC_BUFFER_NO_SPACE                      = ADC_FIRST + 5;
    public static final int ADC_LAST = ADC_BUFFER_NO_SPACE;

    public static final int ATCMD_FIRST = ADC_LAST + 1;
    public static final int ATCMD_DATA_CONNECTION_UNSUPPORTED        = ATCMD_FIRST + 0;
    public static final int ATCMD_LAST = ATCMD_DATA_CONNECTION_UNSUPPORTED;

    public static final int COUNTER_FIRST = ATCMD_LAST + 1;
    public static final int COUNTER_INVALID_CONTROLLER_NUMBER        = COUNTER_FIRST + 0;
    public static final int COUNTER_INVALID_CHANNEL_NUMBER           = COUNTER_FIRST + 1;
    public static final int COUNTER_INVALID_TYPE                     = COUNTER_FIRST + 2;
    public static final int COUNTER_CONFIG_CANNOT_BE_USED            = COUNTER_FIRST + 3;
    public static final int COUNTER_NOT_STARTED                      = COUNTER_FIRST + 4;
    public static final int COUNTER_NOT_SUSPENDED                    = COUNTER_FIRST + 5;
    public static final int COUNTER_IS_STARTED                       = COUNTER_FIRST + 6;
    public static final int COUNTER_NONPOSITIVE_LIMIT_AND_INTERVAL   = COUNTER_FIRST + 7;
    public static final int COUNTER_NULL_LISTENER                    = COUNTER_FIRST + 8;
    public static final int COUNTER_IS_SUSPENDED                     = COUNTER_FIRST + 9;
    public static final int COUNTER_LAST = COUNTER_IS_SUSPENDED;

    public static final int DAC_FIRST = COUNTER_LAST + 1;
    public static final int DAC_GENERATION_IS_ACTIVE                 = DAC_FIRST + 0;
    public static final int DAC_NO_BUFFER_DATA                       = DAC_FIRST + 1;
    public static final int DAC_NONPOSITIVE_INTERVAL                 = DAC_FIRST + 2;
    public static final int DAC_LESS_MINIMAL_INTERVAL                = DAC_FIRST + 3;
    public static final int DAC_UNACCEPTABLE_VALUE                   = DAC_FIRST + 4;
    public static final int DAC_CANNOT_START_CONVERSION              = DAC_FIRST + 5;
    public static final int DAC_LAST = DAC_CANNOT_START_CONVERSION;

    public static final int GPIO_FIRST = DAC_LAST + 1;
    public static final int GPIO_TRIGGER_OR_MODE                     = GPIO_FIRST + 0;
    public static final int GPIO_MODE_NOT_FOR_DIRINPUTONLY           = GPIO_FIRST + 1;
    public static final int GPIO_MODE_NOT_FOR_DIROUTPUTONLY          = GPIO_FIRST + 2;
    public static final int GPIO_MODE_NOT_FOR_DIRBOTH                = GPIO_FIRST + 3;
    public static final int GPIO_INVALID_DIRECTION                   = GPIO_FIRST + 4;
    public static final int GPIO_ILLEGAL_DIRECTION_OR_INIT_VALUE     = GPIO_FIRST + 5;
    public static final int GPIO_DIR_UNSUPPORTED_BY_PIN_CONFIG       = GPIO_FIRST + 6;
    public static final int GPIO_SET_TO_INPUT_PIN                    = GPIO_FIRST + 7;
    public static final int GPIO_REGISTER_LISTENER_TO_OUTPUT_PIN     = GPIO_FIRST + 8;
    public static final int GPIO_CANNOT_START_NOTIFICATION           = GPIO_FIRST + 9;
    public static final int GPIO_LISTENER_ALREADY_ASSIGNED           = GPIO_FIRST + 10;
    public static final int GPIO_DIR_SHOULD_BE_INPUT_OR_OUTPUT       = GPIO_FIRST + 11;
    public static final int GPIO_INCOMPATIBLE_DIR                    = GPIO_FIRST + 12;
    public static final int GPIO_WRITE_TO_INPUT_PORT                 = GPIO_FIRST + 13;
    public static final int GPIO_REGISTER_LISTENER_TO_OUTPUT_PORT    = GPIO_FIRST + 14;
    public static final int GPIO_LAST = GPIO_REGISTER_LISTENER_TO_OUTPUT_PORT;

    public static final int I2CBUS_FIRST = GPIO_LAST + 1;
    public static final int I2CBUS_ALREADY_TRANSFERRED_MESSAGE       = I2CBUS_FIRST + 0;
    public static final int I2CBUS_NULL_BUFFER                       = I2CBUS_FIRST + 1;
    public static final int I2CBUS_NEGATIVE_SKIP_ARG                 = I2CBUS_FIRST + 2;
    public static final int I2CBUS_DIFFERENT_BUS_SLAVE_OPERATION     = I2CBUS_FIRST + 3;
    public static final int I2CBUS_BUFFER_GIVEN_TWICE                = I2CBUS_FIRST + 4;
    public static final int I2CBUS_CLOSED_DEVICE                     = I2CBUS_FIRST + 5;
    public static final int I2CBUS_FIRST_MESSAGE                     = I2CBUS_FIRST + 6;
    public static final int I2CBUS_LAST_MESSAGE                      = I2CBUS_FIRST + 7;
    public static final int I2CBUS_LAST = I2CBUS_LAST_MESSAGE;

    public static final int MMIO_FIRST = I2CBUS_LAST + 1;
    public static final int MMIO_NEGATIVE_SIZE                       = MMIO_FIRST + 0;
    public static final int MMIO_NEGATIVE_OFFSET                     = MMIO_FIRST + 1;
    public static final int MMIO_INVALID_TYPE                        = MMIO_FIRST + 2;
    public static final int MMIO_INVALID_DEVICE_PARAMETERS           = MMIO_FIRST + 3;
    public static final int MMIO_ADDRESS_ACCESS_NOT_ALLOWED          = MMIO_FIRST + 4;
    public static final int MMIO_REGISTER_TYPE_UNSUPPORTED           = MMIO_FIRST + 5;
    public static final int MMIO_INVALID_BLOCK_PARAMETERS            = MMIO_FIRST + 6;
    public static final int MMIO_MIXED_ENDIANNESS_UNSUPPORTED        = MMIO_FIRST + 7;
    public static final int MMIO_INVALID_INDEX                       = MMIO_FIRST + 8;
    public static final int MMIO_LAST = MMIO_INVALID_INDEX;

    public static final int POWER_FIRST = MMIO_LAST + 1;
    public static final int POWER_INVALID_STATE_MASK                 = POWER_FIRST + 0;
    public static final int POWER_INVALID_STATE                      = POWER_FIRST + 1;
    public static final int POWER_INVALID_DURATION                   = POWER_FIRST + 2;
    public static final int POWER_ALREADY_ASSIGNED_HANDLER           = POWER_FIRST + 3;
    public static final int POWER_STANDBY_MODE                       = POWER_FIRST + 4;
    public static final int POWER_LAST = POWER_STANDBY_MODE;

    public static final int PWM_FIRST = POWER_LAST + 1;
    public static final int PWM_OUTPUT_PIN_NOT_CONFIGURED            = PWM_FIRST + 0;
    public static final int PWM_NONPOSITIVE_PERIOD                   = PWM_FIRST + 1;
    public static final int PWM_OUT_OF_RANGE_PERIOD                  = PWM_FIRST + 2;
    public static final int PWM_ILLEGAL_WIDTH_OR_COUNT               = PWM_FIRST + 3;
    public static final int PWM_NULL_SRC                             = PWM_FIRST + 4;
    public static final int PWM_NULL_LISTENER                        = PWM_FIRST + 5;
    public static final int PWM_NO_DATA                              = PWM_FIRST + 6;
    public static final int PWM_NULL_SRC1_OR_SRC2                    = PWM_FIRST + 7;
    public static final int PWM_GENERATION_SESSION_ACTIVE            = PWM_FIRST + 8;
    public static final int PWM_LAST = PWM_GENERATION_SESSION_ACTIVE;

    public static final int SPIBUS_FIRST = PWM_LAST + 1;
    public static final int SPIBUS_NULL_BUFFER                       = SPIBUS_FIRST + 0;
    public static final int SPIBUS_SLAVE_WORD_LENGTH                 = SPIBUS_FIRST + 1;
    public static final int SPIBUS_BYTE_NUMBER_BELIES_WORD_LENGTH    = SPIBUS_FIRST + 2;
    public static final int SPIBUS_LAST = SPIBUS_BYTE_NUMBER_BELIES_WORD_LENGTH;

    public static final int UART_FIRST = SPIBUS_LAST + 1;
    public static final int UART_CANT_GET_PORT_NAME               = UART_FIRST + 0;
    public static final int UART_UTF8_UNCONVERTIBLE_DEVNAME          = UART_FIRST + 1;
    public static final int UART_NULL_SRC_OR_LISTENER                = UART_FIRST + 2;
    public static final int UART_NULL_SRC1_OR_SRC2_OR_LISTENER       = UART_FIRST + 3;
    public static final int UART_NULL_DST                            = UART_FIRST + 4;
    public static final int UART_NULL_SRC                            = UART_FIRST + 5;
    public static final int UART_ACTIVE_READ_OPERATION               = UART_FIRST + 6;
    public static final int UART_ACTIVE_WRITE_OPERATION              = UART_FIRST + 7;
    public static final int UART_UNKNOWN_SIGNAL_ID                   = UART_FIRST + 8;
    public static final int UART_SIGNALS_NOT_BITWISE_COMBINATION     = UART_FIRST + 9;
    public static final int UART_LISTENER_ALREADY_REGISTERED         = UART_FIRST + 10;
    public static final int UART_NEGATIVE_TIMEOUT                    = UART_FIRST + 11;

    private static final String strings[] = {

        // core classes messages
        "Locked by other application",
        "config or intf is null",
        "There is problem with configuration: %s",
        "EXCLUSIVE access mode is not supported",
        "intf is null",
        "Device %s not found",
        "Device %d has different type %s",
        "Name and properties are NULL",
        "id is not equal to UNSPECIFIED_ID or is not greater than or equal to 0",
        "Device ID must be unique",
        "There is configuration with such name, type and proprties",
        "Device ID must be positive or 0",
        "Invalid class name: %s",
        "Invalid instance of DeviceConfig class",
        "Device found by driver loader but peripheral is busy",
        "Neither embedded nor installed driver knows about peripheral",
        "There is no driver",
        "actions is null",
        "Name is null",
        "actions are empty",
        "Invalid permission class: %s",
        "Cannot add a Permission to a readonly PermissionCollection",
        "Opening with deviceName is unsupported",
        "Buffer was modified by application",

        // adc messages
        "Another operation on ADC channel is in progress",
        "'interval' is negative or 0",
        "'interval' is less than minimal sampling interval",
        "Cannot start acquisition",
        "Argument 'low' is greater than 'high'",
        "No free space in buffer",

        // atcmd messages
        "Emulator does not support data connection",

        // counter messages
        "Invalid controllerNumber",
        "Invalid channelNumber",
        "Invalid type",
        "Provided config cannot be used as pulse input",
        "Counting has not been started yet",
        "Counting wasn't suspended",
        "Counting is already started",
        "Both limit and interval are equal or less than 0",
        "Counting listener is null",
        "Counting is already suspended",

        // dac messages
        "Generation is already active",
        "No data in buffer",
        "'interval' is negative or 0",
        "'interval' is less than minimal sampling interval",
        "'value' is out of an allowed range",
        "Cannot start conversion",

        // gpio messages
        "Trigger or mode",
        "mode is not for DIR_INPUT_ONLY",
        "mode is not for DIR_OUTPUT_ONLY",
        "mode is not for DIR_BOTH: %d",
        "Invalid direction",
        "Illegal direction or initValue",
        "Pin config does not support required direction",
        "Try to Set value to input pin",
        "Try to register Listener to output pin",
        "Cannot start notification",
        "The listener is already assigned",
        "Direction should be INPUT or OUTPUT",
        "Incompatible direction",
        "Trying to write to input port",
        "Try to register Listener to output port",

        // i2c bus messages
        "the message has already been transferred once",
        "buffer is null",
        "'skip' argument is negative",
        "operation to a slave on a different bus",
        "the same buffer is given twice",
        "combined message with closed device",
        "first message",
        "last message",

        // mmio messages
        "size is negative",
        "offset is negative",
        "Invalid type: %s",
        "Invalid MMIO device parameters",
        "Address access is not allowed: %d, %d",
        "Unsupported register type: %s",
        "Invalid block parameters",
        "Mixed endiannes is not supported",
        "invalid index value",

        // power messages
        "Invalid power state mask %d",
        "Invalid power state %d",
        "Invalid duration",
        "Handler is already assigned",
        "Standby mode",

        // pwm messages
        "output pin is not configured for output",
        "Period %d is negative or zero ",
        "Period %d is out of the supported range",
        "width or count is illegal",
        "src buffer is null ",
        "listener is null",
        "No data in the buffer",
        "src1 or src2 buffer is null",
        "pulse generation session is already active",

        // spi bus messages
        "Buffer is null",
        "Slave Word Length is %d",
        "the number of bytes to receive/send belies word length",

        // uart messages
        "Cannot get serial port name",
        "Unable to convert dev name to UTF-8",
        "src buffer or listener is null",
        "src1, src2 buffer or listener is null",
        "dst buffer is null",
        "src buffer is null",
        "another synchronous or asynchronous read operation is already active",
        "another synchronous or asynchronous write operation is already active",
        "signalID is not one of the defined values",
        "signals is not a bit-wise combination of valid signal IDs.",
        "listener is not null and a listener is already registered",
        "timeout cannot be negative",
    };
}
