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

package jdk.dio.uart;

import jdk.dio.DeviceConfig;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.DeviceManager;
import java.util.Objects;
import serializator.*;
import romizer.*;

/**
 * The {@code UARTConfig} class encapsulates the hardware addressing information, and static and dynamic configuration
 * parameters of a UART.
 * <p />
 * Some hardware addressing parameter, and static and dynamic configuration parameters may be set to {@link #DEFAULT}.
 * Whether such default settings are supported is platform- as well as device driver-dependent.
 * <p />
 * An instance of {@code UARTConfig} can be passed to the {@link DeviceManager#open(DeviceConfig)} or
 * {@link DeviceManager#open(Class, DeviceConfig)} method to open the designated UART with the specified
 * configuration. A {@link InvalidDeviceConfigException} is thrown when attempting to open a device with
 * an invalid or unsupported configuration.
 *
 * @see DeviceManager#open(DeviceConfig)
 * @see DeviceManager#open(Class, DeviceConfig)
 * @since 1.0
 */
@SerializeMe
public final class UARTConfig implements DeviceConfig<UART>, DeviceConfig.HardwareAddressing {

    /**
     * 5 data bit format.
     */
    public static final int DATABITS_5 = 5;
    /**
     * 6 data bit format.
     */
    public static final int DATABITS_6 = 6;
    /**
     * 7 data bit format.
     */
    public static final int DATABITS_7 = 7;
    /**
     * 8 data bit format.
     */
    public static final int DATABITS_8 = 8;
    /**
     * 9 data bit format.
     */
    public static final int DATABITS_9 = 9;
    /**
     * Flow control off.
     */
    public static final int FLOWCONTROL_NONE = 0;
    /**
     * RTS/CTS (hardware) flow control on input.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other flow control bit flags.
     */
    public static final int FLOWCONTROL_RTSCTS_IN = 1;
    /**
     * RTS/CTS (hardware) flow control on output.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other flow control bit flags.
     */
    public static final int FLOWCONTROL_RTSCTS_OUT = 2;
    /**
     * XON/XOFF (software) flow control on input.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other flow control bit flags.
     */
    public static final int FLOWCONTROL_XONXOFF_IN = 4;
    /**
     * XON/XOFF (software) flow control on output.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other flow control bit flags.
     */
    public static final int FLOWCONTROL_XONXOFF_OUT = 8;
    /**
     * EVEN parity scheme.
     */
    public static final int PARITY_EVEN = 2;
    /**
     * MARK parity scheme.
     */
    public static final int PARITY_MARK = 3;
    /**
     * No parity bit.
     */
    public static final int PARITY_NONE = 0;
    /**
     * ODD parity scheme.
     */
    public static final int PARITY_ODD = 1;
    /**
     * SPACE parity scheme.
     */
    public static final int PARITY_SPACE = 4;
    /**
     * Number of STOP bits - 1.
     */
    public static final int STOPBITS_1 = 1;
    /**
     * Number of STOP bits - 1-1/2.
     */
    public static final int STOPBITS_1_5 = 2;
    /**
     * Number of STOP bits - 2.
     */
    public static final int STOPBITS_2 = 3;

    private String controllerName;
    private int baudRate;
    private int dataBits;
    private int flowcontrol;
    private int inputBufferSize = DEFAULT;
    private int outputBufferSize = DEFAULT;
    private int parity;
    private int stopBits;
    private int controllerNumber;
    private int channelNumber;


    // hidden constructor for serializer
    @DontRenameMethod
    UARTConfig() {}

    /**
     * Creates a new {@code UARTConfig} with the specified hardware addressing information and configuration parameters.
     *
     * @param controllerNumber
     *            the hardware UART controller's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param channelNumber
     *            the hardware UART channel's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param baudRate
     *            the speed in Bauds of the UART (a positive integer).
     * @param dataBits
     *            the number of bits per character, one of: {@link #DATABITS_5}, {@link #DATABITS_6},
     *            {@link #DATABITS_7}, {@link #DATABITS_8} or {@link #DATABITS_9}.
     * @param parity
     *            the parity, one of: {@link #PARITY_ODD}, {@link #PARITY_EVEN}, {@link #PARITY_MARK},
     *            {@link #PARITY_SPACE}, or {@link #PARITY_NONE}.
     * @param stopBits
     *            the number of stop bits per character, on of: {@link #STOPBITS_1}, {@link #STOPBITS_1_5}, or
     *            {@link #STOPBITS_2}.
     * @param flowcontrol
     *            the flow control mode - a bit-wise OR combination of {@link #FLOWCONTROL_NONE},
     *            {@link #FLOWCONTROL_RTSCTS_IN}, {@link #FLOWCONTROL_RTSCTS_OUT}, {@link #FLOWCONTROL_XONXOFF_IN} or
     *            {@link #FLOWCONTROL_XONXOFF_OUT}.
     *
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code controllerNumber} is not in the defined range;</li>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code baudRate} is not in the defined range;</li>
     *             <li>{@code dataBits} is not in the defined range;</li>
     *             <li>{@code parity} is not in the defined range;</li>
     *             <li>{@code stopBits} is not in the defined range;</li>
     *             <li>{@code flowcontrol} is not in the defined range.</li>
     *             </ul>
     */
    public UARTConfig(int controllerNumber, int channelNumber, int baudRate, int dataBits, int parity, int stopBits, int flowcontrol) {
        this(controllerNumber, channelNumber, baudRate, dataBits, parity, stopBits, flowcontrol, DEFAULT, DEFAULT);
    }

    /**
     * Creates a new {@code UARTConfig} with the specified hardware addressing information and configuration parameters.
     * The platform/underlying driver may or may not allocate the requested sizes for the input and output buffers.
     *
     * @param controllerNumber
     *            the hardware UART controller's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param channelNumber
     *            the hardware UART channel's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param baudRate
     *            the speed in Bauds of the UART (a positive integer).
     * @param dataBits
     *            the number of bits per character, one of: {@link #DATABITS_5}, {@link #DATABITS_6},
     *            {@link #DATABITS_7}, {@link #DATABITS_8} or {@link #DATABITS_9}.
     * @param parity
     *            the parity, one of: {@link #PARITY_ODD}, {@link #PARITY_EVEN}, {@link #PARITY_MARK},
     *            {@link #PARITY_SPACE}, or {@link #PARITY_NONE}.
     * @param stopBits
     *            the number of stop bits per character, on of: {@link #STOPBITS_1}, {@link #STOPBITS_1_5}, or
     *            {@link #STOPBITS_2}.
     * @param flowcontrol
     *            the flow control mode - a bit-wise OR combination of {@link #FLOWCONTROL_NONE},
     *            {@link #FLOWCONTROL_RTSCTS_IN}, {@link #FLOWCONTROL_RTSCTS_OUT}, {@link #FLOWCONTROL_XONXOFF_IN} or
     *            {@link #FLOWCONTROL_XONXOFF_OUT}.
     * @param inputBufferSize
     *            the input buffer size (a positive or zero integer) or {@link #DEFAULT} - (advisory only).
     * @param outputBufferSize
     *            the output buffer size (a positive or zero integer) or {@link #DEFAULT} - (advisory only).
     *
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code controllerNumber} is not in the defined range;</li>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code baudRate} is not in the defined range;</li>
     *             <li>{@code dataBits} is not in the defined range;</li>
     *             <li>{@code parity} is not in the defined range;</li>
     *             <li>{@code stopBits} is not in the defined range;</li>
     *             <li>{@code flowcontrol} is not in the defined range;</li>
     *             <li>{@code inputBufferSize} is not in the defined range;</li>
     *             <li>{@code outputBufferSize} is not in the defined range.</li>
     *             </ul>
     */
    public UARTConfig(int controllerNumber, int channelNumber, int baudRate, int dataBits, int parity, int stopBits, int flowcontrol,
            int inputBufferSize, int outputBufferSize) {
        this.controllerNumber = controllerNumber;
        this.channelNumber = channelNumber;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.flowcontrol = flowcontrol;
        this.inputBufferSize = inputBufferSize;
        this.outputBufferSize = outputBufferSize;
        checkParameters();
    }

    /**
     * Creates a new {@code UARTConfig} with the specified hardware addressing information and configuration parameters.
     *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @param channelNumber
     *            the hardware UART channel's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param baudRate
     *            the speed in Bauds of the UART (a positive integer).
     * @param dataBits
     *            the number of bits per character, one of: {@link #DATABITS_5}, {@link #DATABITS_6},
     *            {@link #DATABITS_7}, {@link #DATABITS_8} or {@link #DATABITS_9}.
     * @param parity
     *            the parity, one of: {@link #PARITY_ODD}, {@link #PARITY_EVEN}, {@link #PARITY_MARK},
     *            {@link #PARITY_SPACE}, or {@link #PARITY_NONE}.
     * @param stopBits
     *            the number of stop bits per character, on of: {@link #STOPBITS_1}, {@link #STOPBITS_1_5}, or
     *            {@link #STOPBITS_2}.
     * @param flowcontrol
     *            the flow control mode - a bit-wise OR combination of {@link #FLOWCONTROL_NONE},
     *            {@link #FLOWCONTROL_RTSCTS_IN}, {@link #FLOWCONTROL_RTSCTS_OUT}, {@link #FLOWCONTROL_XONXOFF_IN} or
     *            {@link #FLOWCONTROL_XONXOFF_OUT}.
     *
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code baudRate} is not in the defined range;</li>
     *             <li>{@code dataBits} is not in the defined range;</li>
     *             <li>{@code parity} is not in the defined range;</li>
     *             <li>{@code stopBits} is not in the defined range;</li>
     *             <li>{@code flowcontrol} is not in the defined range.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code controllerName} is {@code null}.
     */
    public UARTConfig(String controllerName, int channelNumber, int baudRate, int dataBits, int parity, int stopBits, int flowcontrol) {
        this(controllerName, channelNumber, baudRate, dataBits, parity, stopBits, flowcontrol, DEFAULT, DEFAULT);
    }

    /**
     * Creates a new {@code UARTConfig} with the specified hardware addressing information and configuration parameters.
     * The platform/underlying driver may or may not allocate the requested sizes for the input and output buffers.
     *
     * @param controllerName
     *            the controller name (such as its <em>device file</em> name on UNIX systems).
     * @param channelNumber
     *            the hardware UART channel's number (a positive or zero integer) or {@link #DEFAULT}.
     * @param baudRate
     *            the speed in Bauds of the UART (a positive integer).
     * @param dataBits
     *            the number of bits per character, one of: {@link #DATABITS_5}, {@link #DATABITS_6},
     *            {@link #DATABITS_7}, {@link #DATABITS_8} or {@link #DATABITS_9}.
     * @param parity
     *            the parity, one of: {@link #PARITY_ODD}, {@link #PARITY_EVEN}, {@link #PARITY_MARK},
     *            {@link #PARITY_SPACE}, or {@link #PARITY_NONE}.
     * @param stopBits
     *            the number of stop bits per character, on of: {@link #STOPBITS_1}, {@link #STOPBITS_1_5}, or
     *            {@link #STOPBITS_2}.
     * @param flowcontrol
     *            the flow control mode - a bit-wise OR combination of {@link #FLOWCONTROL_NONE},
     *            {@link #FLOWCONTROL_RTSCTS_IN}, {@link #FLOWCONTROL_RTSCTS_OUT}, {@link #FLOWCONTROL_XONXOFF_IN} or
     *            {@link #FLOWCONTROL_XONXOFF_OUT}.
     * @param inputBufferSize
     *            the input buffer size (a positive or zero integer) or {@link #DEFAULT} - (advisory only).
     * @param outputBufferSize
     *            the output buffer size (a positive or zero integer) or {@link #DEFAULT} - (advisory only).
     *
     * @throws IllegalArgumentException
     *             if any of the following is true:
     *             <ul>
     *             <li>{@code channelNumber} is not in the defined range;</li>
     *             <li>{@code baudRate} is not in the defined range;</li>
     *             <li>{@code dataBits} is not in the defined range;</li>
     *             <li>{@code parity} is not in the defined range;</li>
     *             <li>{@code stopBits} is not in the defined range;</li>
     *             <li>{@code flowcontrol} is not in the defined range;</li>
     *             <li>{@code inputBufferSize} is not in the defined range;</li>
     *             <li>{@code outputBufferSize} is not in the defined range.</li>
     *             </ul>
     * @throws NullPointerException
     *             if {@code controllerName} is {@code null}.
     */
    public UARTConfig(String controllerName, int channelNumber, int baudRate, int dataBits, int parity, int stopBits, int flowcontrol,
            int inputBufferSize, int outputBufferSize) {
        this.controllerName = controllerName;
        this.channelNumber = channelNumber;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.flowcontrol = flowcontrol;
        this.inputBufferSize = inputBufferSize;
        this.outputBufferSize = outputBufferSize;
        //checks for null
        controllerName.length();
        checkParameters();
    }

    private void checkParameters(){

        if ((null == controllerName && channelNumber < DEFAULT) ||
            baudRate <= 0 ||
            (dataBits < DATABITS_5 || dataBits > DATABITS_9) ||
            (parity < PARITY_NONE || parity > PARITY_SPACE) ||
            (stopBits < STOPBITS_1 || stopBits > STOPBITS_2) ||
            (flowcontrol!=FLOWCONTROL_NONE && flowcontrol!=FLOWCONTROL_RTSCTS_IN &&
             flowcontrol!=FLOWCONTROL_RTSCTS_OUT && flowcontrol!=FLOWCONTROL_XONXOFF_IN && flowcontrol!=FLOWCONTROL_XONXOFF_OUT) ||
            inputBufferSize  < DEFAULT ||
            outputBufferSize < DEFAULT ) {
           throw new IllegalArgumentException();
        }
    }

    /**
     * Gets the configured default/initial speed in Bauds.
     *
     * @return the default/initial speed in Bauds of the UART (a positive integer).
     */
    public int getBaudRate() {
        return baudRate;
    }

    /**
     * Gets the configured default/initial number of bits per character.
     *
     * @return the default/initial number of bits per character, one of: {@link #DATABITS_5}, {@link #DATABITS_6},
     *         {@link #DATABITS_7}, {@link #DATABITS_8} or {@link #DATABITS_9}.
     */
    public int getDataBits() {
        return dataBits;
    }

    /**
     * Gets the configured flow control mode.
     *
     * @return the flow control mode - a bit-wise OR combination of {@link #FLOWCONTROL_NONE},
     *         {@link #FLOWCONTROL_RTSCTS_IN}, {@link #FLOWCONTROL_RTSCTS_OUT}, {@link #FLOWCONTROL_XONXOFF_IN} or
     *         {@link #FLOWCONTROL_XONXOFF_OUT}.
     */
    public int getFlowControlMode() {
        return flowcontrol;
    }

    /**
     * Gets the requested input buffer size. The platform/underlying driver may or may not allocate the requested size
     * for the input buffer.
     *
     * @return the requested input buffer size (a positive or zero integer).
     */
    public int getInputBufferSize() {
        return inputBufferSize;
    }

    /**
     * Gets the requested output buffer size. The platform/underlying driver may or may not allocate the requested size
     * for the output buffer.
     *
     * @return the requested output buffer size (a positive or zero integer).
     */
    public int getOutputBufferSize() {
        return outputBufferSize;
    }

    /**
     * Gets the configured default/initial parity.
     *
     * @return the default/initial parity, one of: {@link #PARITY_ODD}, {@link #PARITY_EVEN}, {@link #PARITY_MARK},
     *         {@link #PARITY_SPACE}, or {@link #PARITY_NONE}.
     */
    public int getParity() {
        return parity;
    }

    /**
     * Gets the configured default/initial number of stop bits per character.
     *
     * @return the default/initial number of stop bits per character, on of: {@link #STOPBITS_1}, {@link #STOPBITS_1_5},
     *         or {@link #STOPBITS_2}.
     */
    public int getStopBits() {
        return stopBits;
    }
    /**
     * Gets the configured UART channel number.
     *
     * @return the hardware channel's number (a positive or zero integer) or {@link #DEFAULT}.
     */
    public int getChannelNumber() {
        return channelNumber;
    }

    /**
     * Gets the configured UART controller number.
     *
     * @return the hardware controller's number (a positive or zero integer) or {@link #DEFAULT}.
     */
    @Override
    public int getControllerNumber() {
        return controllerNumber;
    }

    /**
     * Gets the configured controller name (such as its <em>device file</em> name on UNIX systems).
     *
     * @return the controller name or {@code null}.
     */
    @Override
    public String getControllerName() {
        return controllerName;
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + Objects.hashCode(this.controllerName);
        hash = 13 * hash + this.baudRate;
        hash = 13 * hash + this.dataBits;
        hash = 13 * hash + this.flowcontrol;
        hash = 13 * hash + this.inputBufferSize;
        hash = 13 * hash + this.outputBufferSize;
        hash = 13 * hash + this.parity;
        hash = 13 * hash + this.stopBits;
        hash = 13 * hash + this.controllerNumber;
        hash = 13 * hash + this.channelNumber;
        return hash;
    }

    /**
     * Checks two {@code UARTConfig} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     *
     * @return {@code true} if {@code obj} is a {@code UARTConfig} and has
     * the same hardware addressing information and configuration parameter values
     * as this {@code UARTConfig} object.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UARTConfig other = (UARTConfig) obj;
        if (!Objects.equals(this.controllerName, other.controllerName)) {
            return false;
        }
        if (this.baudRate != other.baudRate) {
            return false;
        }
        if (this.dataBits != other.dataBits) {
            return false;
        }
        if (this.flowcontrol != other.flowcontrol) {
            return false;
        }
        if (this.inputBufferSize != other.inputBufferSize) {
            return false;
        }
        if (this.outputBufferSize != other.outputBufferSize) {
            return false;
        }
        if (this.parity != other.parity) {
            return false;
        }
        if (this.stopBits != other.stopBits) {
            return false;
        }
        if (this.controllerNumber != other.controllerNumber) {
            return false;
        }
        if (this.channelNumber != other.channelNumber) {
            return false;
        }
        return true;
    }
}
