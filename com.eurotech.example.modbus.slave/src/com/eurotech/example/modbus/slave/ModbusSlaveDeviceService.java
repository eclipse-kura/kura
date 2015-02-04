package com.eurotech.example.modbus.slave;

import java.util.Properties;


public interface ModbusSlaveDeviceService {
	
	/**
	 * name of this service
	 */
	public static final String SERVICE_NAME = ModbusSlaveDeviceService.class.getName();
	
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
	 * <br><br>
	 * Modbus properties :
	 * <li>transmissionMode : modbus transmission mode, can be RTU or ASCII, in Ethernet mode only RTU is supported
	 * <li>respTimeout      : Timeout in milliseconds on a question/response request.
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
	 * @throws ModbusProtocolException 
	 * 
	 * @see #getConnectStatus()
	 */
	public void disconnect() throws ModbusProtocolException;

}
