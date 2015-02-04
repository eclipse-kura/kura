package com.eurotech.example.modbus.slave;

public interface ModbusProtocol {
	
	/**
	 * <b>Modbus function 01</b><br>
	 * Read 1 to 2000 contiguous status of coils from the attached field device.
	 * <p>
	 * @param unitAddr
	 *            modbus slave address (must be unique in the range 1 - 247)
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
	public boolean[] readCoils(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 02</b><br>
	 * Read 1 to 2000 contiguous status of discrete inputs from the attached field device.
	 * <p>
	 * @param unitAddr
	 *            modbus slave address (must be unique in the range 1 - 247)
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
	public boolean[] readDiscreteInputs(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 05</b><br>
	 * write a single output to either ON or OFF in the attached field device.
	 * <p>
	 * @param unitAddr
	 *            modbus slave address (must be unique in the range 1 - 247)
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
	public void writeSingleCoil(int unitAddr, int dataAddress, boolean data)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 15 (0x0F)</b><br>
	 * write multiple coils in a sequence of coils to either ON or OFF in the attached field device.
	 * <p>
	 * @param unitAddr
	 *            modbus slave address (must be unique in the range 1 - 247)
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
	public void writeMultipleCoils(int unitAddr, int dataAddress, boolean[] data)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 03</b><br>
	 * Read contents of 1 to 125 contiguous block of holding registers from the attached field device.
	 * <p>
	 * @param unitAddr
	 *            modbus slave address (must be unique in the range 1 - 247)
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
	public byte[] readHoldingRegisters(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 04</b><br>
	 * Read contents of 1 to 125 contiguous block of input registers from the attached field device.
	 * <p>
	 * @param unitAddr
	 *            modbus slave address (must be unique in the range 1 - 247)
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
	public int[] readInputRegisters(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 06</b><br>
	 * write a single holding register in the attached field device.
	 * <p>
	 * @param unitAddr
	 *            modbus slave address (must be unique in the range 1 - 247)
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
	public void writeSingleRegister(int unitAddr, int dataAddress, int data)
			throws ModbusProtocolException;

	/**
	 * <b>Modbus function 07</b><br>
	 * read the content of 8 Exception Status outputs in the field device.
	 * <p>
	 * @param unitAddr
	 *            modbus slave address (must be unique in the range 1 - 247)
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public boolean[] readExceptionStatus(int unitAddr)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 11 (0x0B)</b><br>
	 * Get a status word and an event count from the device.<br>
	 * Return values in a ModbusCommEvent.
	 * @see ModbusCommEvent.
	 * <p>
	 * @param unitAddr
	 *            modbus slave address (must be unique in the range 1 - 247)
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public ModbusCommEvent getCommEventCounter(int unitAddr)
			throws ModbusProtocolException;
	
	/**
	 * <b>Modbus function 12 (0x0C)</b><br>
	 * Get a status word, an event count, a message count and a list of event bytes
	 * from the device.<br>
	 * Return values in a ModbusCommEvent.
	 * @see ModbusCommEvent.
	 * <p>
	 * @param unitAddr
	 *            modbus slave address (must be unique in the range 1 - 247)
	 * @throws ModbusProtocolException(NOT_CONNECTED)
	 *             current connection is in a status other than <b>CONNECTED</b>
	 * @throws ModbusProtocolException(TRANSACTION_FAILURE)
	 *             should include a protocol specific message to help clarify
	 *             the cause of the exception
	 */
	public ModbusCommEvent getCommEventLog(int unitAddr)
			throws ModbusProtocolException;	
	/**
	 * <b>Modbus function 16 (0x10)</b><br>
	 * write a block of contiguous registers (1 to 123) in the attached field device.
	 * <p>
	 * @param unitAddr
	 *            modbus slave address (must be unique in the range 1 - 247)
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
	public byte[] writeMultipleRegister(int unitAddr, int dataAddress, byte[] data, int numRegisters)
			throws ModbusProtocolException;

}
