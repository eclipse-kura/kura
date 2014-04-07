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
package org.eclipse.kura.net.admin.modem.telit.de910;

/* 
 * Copyright � 2009 Eurotech Inc. All rights reserved.
 */



public interface ICatalystSmBusService {

	public static final String SERVICE_NAME = ICatalystSmBusService.class.getName();

	
	/**
	 * writes data to the SMbus
	 * 
	 * @param slaveAddress	the slave address to write to
	 * @param command		the command to write
	 * @param data			the data to write
	 * @throws Exception	if the write can not succeed
	 */
	public void write(byte slaveAddress, byte command, byte[] data) throws Exception;

	/**
	 * reads a byte from the SMBus
	 * 
	 * @param slaveAddress	the slave address to read from
	 * @param command		the command to send
	 * @return				the byte read from the SMBus
	 * @throws Exception	if the read fails
	 */
	public byte readByte(byte slaveAddress, byte command) throws Exception;

	/**
	 * reads a 16 bit word from the SMBus
	 * 
	 * @param slaveAddress	the slave address to read from
	 * @param command		the command to send
	 * @return				the word read from the SMBus
	 * @throws Exception	if the read fails
	 */
	public short readWord(byte slaveAddress, byte command) throws Exception;
	
	/**
	 * reads a block of bytes from the SMBus
	 * 
	 * @param slaveAddress	the slave address to read from
	 * @param command		the command to send
	 * @return				an array of bytes read from the SMBus
	 * @throws Exception	if the read fails
	 */
	public byte[] readBlock(byte slaveAddress, byte command) throws Exception;

	/**
	 * gets the last error provided by the SMBus native library
	 * 
	 * @return				an int representing the last error provided by the
	 * 						SMBus native library
	 * @throws Exception	if there is an error reading the last error
	 */
	public int getLastError() throws Exception;
	
	/**
	 * sets the last error in the SMBus native library
	 * 
	 * @param error			the error code to set in the natvie library
	 * @throws Exception	if there is an error setting the error code
	 */
	public void setLastError(int error) throws Exception;
}
