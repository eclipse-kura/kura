/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.protocol.modbus;

import java.util.Properties;

import org.eclipse.kura.KuraConnectionStatus;

/**
 * OSGI service providing a connection to a device via Serial link (RS232/RS485) or Ethernet using Modbus protocol.
 * This service implements a subset of Modbus Application Protocol as defined by Modbus Organization :
 * http://www.modbus.org/specs.php.<br>
 * For the moment in Ethernet mode, only RTU over TCP/IP is supported
 * <p>
 * Function codes implemented are :
 * <ul>
 * <li>01 (0x01) readCoils(int dataAddress, int count) : Read 1 to 2000 max contiguous status of coils from the attached
 * field device.
 * It returns an array of booleans representing the requested data points.
 * <li>02 (0x02) readDiscreteInputs(int dataAddress, int count) : Read 1 to 2000 max contiguous status of discrete
 * inputs
 * from the attached field device. It returns an array of booleans representing the requested data points.
 * <li>03 (0x03) readHoldingRegisters(int dataAddress, int count) : Read contents of 1 to 125 max contiguous block of
 * holding
 * registers from the attached field device. It returns an array of int representing the requested data points
 * (data registers on 2 bytes).
 * <li>04 (0x04) readInputRegisters(int dataAddress, int count) : Read contents of 1 to 125 max contiguous block of
 * input registers
 * from the attached field device. It returns an array of int representing the requested data points (data registers on
 * 2 bytes).
 * <li>05 (0x05) writeSingleCoil(int dataAddress, boolean data) : Write a single output to either ON or OFF in the
 * attached field
 * device.
 * <li>06 (0x06) writeSingleRegister(int dataAddress, int data) : write a single holding register in the attached field
 * device.
 * <li>07 (0x07) readExceptionStatus() : read the content of 8 Exception Status outputs in the field
 * device.
 * <li>11 (0x0B) getCommEventCounter() : Get a status word and an event count from the field
 * device.
 * <li>12 (0x0C) getCommEventLog() : Get a status word, an event count, a message count and a list of event bytes from
 * the field
 * device.
 * <li>15 (0x0F) writeMultipleCoils(int dataAddress, boolean[] data) : Write multiple coils in a sequence of coils to
 * either
 * ON or OFF in the attached field device.
 * <li>16 (0x10) writeMultipleRegister(int dataAddress, int[] data) : write a block of contiguous registers (1 to 123)
 * in the attached
 * field device.
 * </ul>
 */

public interface ModbusProtocolDeviceService {

    /**
     * name of this service
     */
    public static final String SERVICE_NAME = ModbusProtocolDeviceService.class.getName();

    /**
     * returns the unit name given in the configureProtocol call. Prior to
     * configuration, this method will return the built-in name of the protocol,
     * the same as returned by getProtocolName.
     *
     * @return assigned unit name
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString();

    /**
     * returns the protocol name for the specific protocol implemented. This
     * name should follow Java member naming conventions, so the first (or only)
     * part of the name should be all lower case, all subsequent parts to the
     * name should begin with an upper case and continue with lower case.
     *
     * @return name following the above rules
     */
    public String getProtocolName();

    /**
     * Configure access to the physical device.
     *
     * @param connectionConfig
     *            (key/value pairing directly from configuration file)
     *            <ul>
     *            <li>connectionType : serial = "RS232" or Ethernet = "TCP-RTU" = RTU over TCP/IP or 
     *            "TCP/IP" = real MODBUS-TCP/IP
     *            </ul>
     *            <br>for SERIAL mode :
     *            <ul>
     *            <li>port : Name of the port ("/dev/ttyUSB0")
     *            <li>baudRate : baudrate
     *            <li>stopBits : number of stopbits
     *            <li>parity : parity mode (0=none, 1=odd, 2=even)
     *            <li>bitsPerWord : number of bits per word
     *            </ul>
     *            <br>for ETHERNET mode :
     *            <ul>
     *            <li>port : TCP port to be used
     *            <li>ipAddress : the 4 bytes IP address of the field device (xxx.xxx.xxx.xxx)
     *            </ul>
     *            <br>Modbus properties :
     *            <ul>
     *            <li>transmissionMode : modbus transmission mode, can be RTU or ASCII, in Ethernet mode only RTU is
     *            supported
     *            <li>respTimeout : Timeout in milliseconds on a question/response request.
     *            </ul>
     *            
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#INVALID_CONFIGURATION}
     *             unspecified problem with the configuration
     */
    public void configureConnection(Properties connectionConfig) throws ModbusProtocolException;

    /**
     * for expedience, can test the status of the connection prior to attempting
     * a command. A connection status of <b>CONNECTED</b> does not assure that a
     * subsequent command will succeed.
     * <p>
     * All protocols must implement this method.
     *
     * @return current connection status as defined in
     *         {@link KuraConnectionStatus KuraConnectionStatus}.
     */
    public int getConnectStatus();

    /**
     * attempt to connect to the field device using the provided configuration.
     * Attempts to connect before configuring the connection or any issues with
     * connecting to the field device will result in an exception being thrown.
     * This includes things like a networking failure in the case of a protocol
     * configured to access the field device of a network.
     * <p>
     * Refer to {@link #getConnectStatus() getConnectStatus} to determine if the
     * connection is completed.
     * <p>
     * All protocols must implement this method.
     *
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#INVALID_CONFIGURATION}
     *             this operates on the basic assumption that access to a device
     *             should exist, if the device is unreachable, it is interpreted
     *             as a failure of the configuration.
     */
    public void connect() throws ModbusProtocolException;

    /**
     * attempt to disconnect from the field device. This should close any port
     * used exclusively for this protocol to talk with its attached device.
     * Attempting to close an already closed connection is not invalid.
     * <p>
     * All protocols must implement this method.
     *
     * @throws ModbusProtocolException
     *
     * @see #getConnectStatus()
     */
    public void disconnect() throws ModbusProtocolException;

    /**
     * <b>Modbus function 01</b><br>
     * Read 1 to 2000 contiguous status of coils from the attached field device.
     * <p>
     *
     * @param unitAddr
     *            modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *            starting address
     * @param count
     *            quantity of coils
     * @return an array of booleans representing the requested data points.
     *         <b>true</b> for a given point if the point is set, <b>false</b>
     *         otherwise.
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *             current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *             should include a protocol specific message to help clarify
     *             the cause of the exception
     */
    public boolean[] readCoils(int unitAddr, int dataAddress, int count) throws ModbusProtocolException;

    /**
     * <b>Modbus function 02</b><br>
     * Read 1 to 2000 contiguous status of discrete inputs from the attached field device.
     * <p>
     *
     * @param unitAddr
     *            modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *            starting address
     * @param count
     *            quantity of inputs
     * @return an array of booleans representing the requested data points.
     *         <b>true</b> for a given point if the point is set, <b>false</b>
     *         otherwise.
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *             current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *             should include a protocol specific message to help clarify
     *             the cause of the exception
     */
    public boolean[] readDiscreteInputs(int unitAddr, int dataAddress, int count) throws ModbusProtocolException;

    /**
     * <b>Modbus function 05</b><br>
     * write a single output to either ON or OFF in the attached field device.
     * <p>
     *
     * @param unitAddr
     *            modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *            Output address.
     * @param data
     *            Output value (boolean) to write.
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *             current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *             should include a protocol specific message to help clarify
     *             the cause of the exception
     */
    public void writeSingleCoil(int unitAddr, int dataAddress, boolean data) throws ModbusProtocolException;

    /**
     * <b>Modbus function 15 (0x0F)</b><br>
     * write multiple coils in a sequence of coils to either ON or OFF in the attached field device.
     * <p>
     *
     * @param unitAddr
     *            modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *            Starting Output address.
     * @param data
     *            Outputs value (array of boolean) to write.
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *             current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *             should include a protocol specific message to help clarify
     *             the cause of the exception
     */
    public void writeMultipleCoils(int unitAddr, int dataAddress, boolean[] data) throws ModbusProtocolException;

    /**
     * <b>Modbus function 03</b><br>
     * Read contents of 1 to 125 contiguous block of holding registers from the attached field device.
     * <p>
     *
     * @param unitAddr
     *            modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *            starting address
     * @param count
     *            quantity of registers (maximum 0x7D)
     * @return an array of int representing the requested data points (data registers on 2 bytes).
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *             current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *             should include a protocol specific message to help clarify
     *             the cause of the exception
     */
    public int[] readHoldingRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException;

    /**
     * <b>Modbus function 04</b><br>
     * Read contents of 1 to 125 contiguous block of input registers from the attached field device.
     * <p>
     *
     * @param unitAddr
     *            modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *            starting address
     * @param count
     *            quantity of registers (maximum 0x7D)
     * @return an array of int representing the requested data points (data registers on 2 bytes).
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *             current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *             should include a protocol specific message to help clarify
     *             the cause of the exception
     */
    public int[] readInputRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException;

    /**
     * <b>Modbus function 06</b><br>
     * write a single holding register in the attached field device.
     * <p>
     *
     * @param unitAddr
     *            modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *            Output address.
     * @param data
     *            Output value (2 bytes) to write.
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *             current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *             should include a protocol specific message to help clarify
     *             the cause of the exception
     */
    public void writeSingleRegister(int unitAddr, int dataAddress, int data) throws ModbusProtocolException;

    /**
     * <b>Modbus function 07</b><br>
     * read the content of 8 Exception Status outputs in the field device.
     * <p>
     *
     * @param unitAddr
     *            modbus slave address (must be unique in the range 1 - 247)
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *             current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *             should include a protocol specific message to help clarify
     *             the cause of the exception
     */
    public boolean[] readExceptionStatus(int unitAddr) throws ModbusProtocolException;

    /**
     * <b>Modbus function 11 (0x0B)</b><br>
     * Get a status word and an event count from the device.<br>
     * Return values in a ModbusCommEvent.
     * <p>
     * 
     * @param unitAddr
     *            modbus slave address (must be unique in the range 1 - 247)
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *             current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *             should include a protocol specific message to help clarify
     *             the cause of the exception
     * @see ModbusCommEvent
     */
    public ModbusCommEvent getCommEventCounter(int unitAddr) throws ModbusProtocolException;

    /**
     * <b>Modbus function 12 (0x0C)</b><br>
     * Get a status word, an event count, a message count and a list of event bytes
     * from the device.<br>
     * Return values in a ModbusCommEvent.
     * <p>
     * 
     * @param unitAddr
     *            modbus slave address (must be unique in the range 1 - 247)
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *             current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *             should include a protocol specific message to help clarify
     *             the cause of the exception
     * @see ModbusCommEvent
     */
    public ModbusCommEvent getCommEventLog(int unitAddr) throws ModbusProtocolException;

    /**
     * <b>Modbus function 16 (0x10)</b><br>
     * write a block of contiguous registers (1 to 123) in the attached field device.
     * <p>
     *
     * @param unitAddr
     *            modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *            Output address.
     * @param data
     *            Registers value (array of int converted in 2 bytes values) to write.
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *             current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *             should include a protocol specific message to help clarify
     *             the cause of the exception
     */
    public void writeMultipleRegister(int unitAddr, int dataAddress, int[] data) throws ModbusProtocolException;
}
