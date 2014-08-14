/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
/*
* Copyright (c) 2011 Eurotech Inc. All rights reserved.
*/

package org.eclipse.kura.protocol.modbus;

import java.util.Properties;

import org.eclipse.kura.position.NmeaPosition;
import org.osgi.util.position.Position;


/**
 * OSGI service providing a connection to a device via Serial link (RS232/RS485) or Ethernet using Modbus protocol.
 * This service implements a subset of Modbus Application Protocol as defined by Modbus Organization : 
 * http://www.modbus.org/specs.php.<br>
 * For the moment in Ethernet mode, only RTU over TCP/IP is supported
 * <p>
 * Function codes implemented are :
 * <li> 01 (0x01) readCoils(int dataAddress, int count) : Read 1 to 2000 max contiguous status of coils from the attached field device. 
 * It returns an array of booleans representing the requested data points.</li><br>
 * <li> 02 (0x02) readDiscreteInputs(int dataAddress, int count) : Read 1 to 2000 max contiguous status of discrete inputs 
 * from the attached field device. It returns an array of booleans representing the requested data points.</li><br>
 * <li> 03 (0x03) readHoldingRegisters(int dataAddress, int count) : Read contents of 1 to 125 max contiguous block of holding 
 * registers from the attached field device. It returns an array of int representing the requested data points 
 * (data registers on 2 bytes).</li><br>
 * <li> 04 (0x04) readInputRegisters(int dataAddress, int count) : Read contents of 1 to 125 max contiguous block of input registers
 *  from the attached field device. It returns an array of int representing the requested data points (data registers on 2 bytes).</li><br>
 * <li> 05 (0x05) writeSingleCoil(int dataAddress, boolean data) : Write a single output to either ON or OFF in the attached field 
 * device.</li><br>
 * <li> 06 (0x06) writeSingleRegister(int dataAddress, int data) : write a single holding register in the attached field 
 * device.</li><br>
 * <li> 07 (0x07) readExceptionStatus() : read the content of 8 Exception Status outputs in the field
 * device.</li><br>
 * <li> 11 (0x0B) getCommEventCounter() : Get a status word and an event count from the field
 * device.</li><br>
 * <li> 12 (0x0C) getCommEventLog() : Get a status word, an event count, a message count and a list of event bytes from the field
 * device.</li><br>
 * <li> 15 (0x0F) writeMultipleCoils(int dataAddress, boolean[] data) : Write multiple coils in a sequence of coils to either 
 * ON or OFF in the attached field device.</li><br>
 * <li> 16 (0x10) writeMultipleRegister(int dataAddress, int[] data) : write a block of contiguous registers (1 to 123) in the attached 
 * field device.</li>
 */

public interface ModbusProtocolDeviceService {

	/**
	 * name of this service
	 */
	public static final String SERVICE_NAME = ModbusProtocolDeviceService.class
			.getName();
	
	/**
	 * returns the unit name given in the configureProtocol call. Prior to
	 * configuration, this method will return the built-in name of the protocol,
	 * the same as returned by getProtocolName.
	 * 
	 * @return assigned unit name
	 * 
	 * @see java.lang.Object#toString()
	 */
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
	 * this returns the address given the RTU in the configureProtocol. Until
	 * configured, this returns an empty string. The address is returned as a
	 * string rather than an integer or other number as some protocols may have
	 * multi-part addresses or addresses that contain non-numeric parts.
	 * 
	 * @return address of physical device this instance represents
	 */
	public String getUnitAddress();

	/**
	 * Required properties for modbus protocol configuration. 
	 * 
	 * @param protocolConfig
	 * (key/value pairing directly from configuration file)<br>
	 * <li>unitName : Name to be returned by toString
	 * <li>unitAddress : Modbus address of the unit (1-255)
	 * <li>txMode : Must be set to "RTU" or "ASCII"
	 * <li>respTimeout : maximum time, in milliseconds, to wait for a
	 * response to a command 
	 * @throws ModbusProtocolException(INVALID_CONFIGURATION)
	 *             unspecified problem with the configuration
	 */
	public void configureProtocol(Properties protocolConfig)
			throws ModbusProtocolException;

	/**
	 * Configure access to the physical device. 
	 * 
	 * @param connectionConfig 
	 * (key/value pairing directly from configuration file)<br>
	 * <li>connectionType : PROTOCOL_CONNECTION_TYPE_SERIAL("SERIAL") or PROTOCOL_CONNECTION_TYPE_ETHER_TCP("ETHERTCP")<br>
	 * <br>
	 * for SERIAL mode :
	 * <li>port : Name of the port ("/dev/ttyUSB0")
	 * <li>baudRate : baudrate 
	 * <li>stopBits : number of stopbits 
	 * <li>parity : parity mode (0=none, 1=odd, 2=even)
	 * <li>bitsPerWord : number of bits per word 
	 * <li>serialMode : Serial Mode : SERIAL_232("RS232") or SERIAL_485("RS485").<br>
	 * if SERIAL_485
	 * <li>serialGPIOswitch : pin number as a filename in "/sys/class/gpio/"
	 * <li>serialGPIOrsmode : pin number as a filename in "/sys/class/gpio/" 
	 * <br><br>
	 * for ETHERNET mode :
	 * <li>port : TCP port to be used
	 * <li>ipAddress : the 4 octet IP address of the field device (xxx.xxx.xxx.xxx)
	 * @throws ModbusProtocolException(INVALID_CONFIGURATION)
	 *             unspecified problem with the configuration
	 */
	public void configureConnection(Properties connectionConfig)
			throws ModbusProtocolException;

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
	 * @throws ModbusProtocolException(INVALID_CONFIGURATION)
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
	 * @see #getConnectStatus()
	 */
	public void disconnect();

	/**
	 * <b>Modbus function 01</b><br>
	 * Read 1 to 2000 contiguous status of coils from the attached field device.
	 * <p>
	 * @param dataAddress
	 *            starting address
	 * @param count
	 *            quantity of coils
	 * @return an array of booleans representing the requested data points.
	 *         <b>true</b> for a given point if the point is set, <b>false</b>
	 *         otherwise.
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public boolean[] readCoils(int dataAddress, int count)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 02</b><br>
	 * Read 1 to 2000 contiguous status of discrete inputs from the attached field device.
	 * <p>
	 * @param dataAddress
	 *            starting address
	 * @param count
	 *            quantity of inputs
	 * @return an array of booleans representing the requested data points.
	 *         <b>true</b> for a given point if the point is set, <b>false</b>
	 *         otherwise.
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public boolean[] readDiscreteInputs(int dataAddress, int count)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 05</b><br>
	 * write a single output to either ON or OFF in the attached field device.
	 * <p>
	 * @param dataAddress
	 *            Output address.
	 * @param data
	 *            Output value (boolean) to write. 	 
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public void writeSingleCoil(int dataAddress, boolean data)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 15 (0x0F)</b><br>
	 * write multiple coils in a sequence of coils to either ON or OFF in the attached field device.
	 * <p>
	 * @param dataAddress
	 *            Starting Output address.
	 * @param data
	 *            Outputs value (array of boolean) to write. 	 
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public void writeMultipleCoils(int dataAddress, boolean[] data)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 03</b><br>
	 * Read contents of 1 to 125 contiguous block of holding registers from the attached field device.
	 * <p>
	 * @param dataAddress
	 *            starting address
	 * @param count
	 *            quantity of registers (maximum 0x7D)
	 * @return an array of int representing the requested data points (data registers on 2 bytes).
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public int[] readHoldingRegisters(int dataAddress, int count)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 04</b><br>
	 * Read contents of 1 to 125 contiguous block of input registers from the attached field device.
	 * <p>
	 * @param dataAddress
	 *            starting address
	 * @param count
	 *            quantity of registers (maximum 0x7D)
	 * @return an array of int representing the requested data points (data registers on 2 bytes).
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public int[] readInputRegisters(int dataAddress, int count)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 06</b><br>
	 * write a single holding register in the attached field device.
	 * <p>
	 * @param dataAddress
	 *            Output address.
	 * @param data
	 *            Output value (2 bytes) to write. 	 
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public void writeSingleRegister(int dataAddress, int data)
			throws ModbusProtocolException;

	/**
	 * <b>Modbus function 07</b><br>
	 * read the content of 8 Exception Status outputs in the field device.
	 * <p>
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public boolean[] readExceptionStatus()
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 11 (0x0B)</b><br>
	 * Get a status word and an event count from the device.<br>
	 * Return values in a ModbusCommEvent.
	 * @see ModbusCommEvent.
	 * <p>
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public ModbusCommEvent getCommEventCounter()
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 12 (0x0C)</b><br>
	 * Get a status word, an event count, a message count and a list of event bytes
	 * from the device.<br>
	 * Return values in a ModbusCommEvent.
	 * @see ModbusCommEvent.
	 * <p>
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public ModbusCommEvent getCommEventLog()
			throws ModbusProtocolException;	
	/**
	 * <b>Modbus function 16 (0x10)</b><br>
	 * write a block of contiguous registers (1 to 123) in the attached field device.
	 * <p>
	 * @param dataAddress
	 *            Output address.
	 * @param data
	 *            Registers value (array of int converted in 2 bytes values) to write. 	 
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public void writeMultipleRegister(int dataAddress, int[] data)
			throws ModbusProtocolException;
	
	/**
	 * Return OSGI Position of the device
	 * <p>
	 * @see Position
	 */
	public Position getPosition();

	/**
	 * Return NMEA Position of the device
	 * <p>
	 * @see NmeaPosition
	 */
	public NmeaPosition getNmeaPosition();

}
