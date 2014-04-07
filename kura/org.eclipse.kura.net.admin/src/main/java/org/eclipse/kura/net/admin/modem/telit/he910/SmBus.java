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

package org.eclipse.kura.net.admin.modem.telit.he910;

import java.io.IOException;


public class SmBus implements ICatalystSmBusService {
	static {
		System.loadLibrary("smbus");
	}
	
	private final static String LABEL = "org.eclipse.kura.sbc.catalyst.smbus.SmBus: ";

	public static final int SMBUS_1_KHZ = 1;
	public static final int SMBUS_10_KHZ = 2;
	public static final int SMBUS_50_KHZ = 3;
	public static final int SMBUS_100_KHZ = 4;
	public static final int SMBUS_400_KHZ = 5;
	public static final int SMBUS_1_MHZ = 6;

	public static final int SMBUS_ERROR_BASE = 0x4600;
	public static final int ERR_SMBUS_UNKNOWN_ERROR = SMBUS_ERROR_BASE + 0x0;
	public static final int ERR_SMBUS_DRIVER_OPEN = SMBUS_ERROR_BASE + 0x1;
	public static final int ERR_SMBUS_UNKNOWN_EXCEPTION = SMBUS_ERROR_BASE + 0x2;
	public static final int ERR_SMBUS_ACCESS_DENIED = SMBUS_ERROR_BASE + 0x3;
	public static final int ERR_SMBUS_SERVICE_NAME_INVALID = SMBUS_ERROR_BASE + 0x4;
	public static final int ERR_SMBUS_SERVICE_DOES_NOT_EXIST = SMBUS_ERROR_BASE + 0x5;
	public static final int ERR_SMBUS_SERVICE_ALREADY_RUNNING = SMBUS_ERROR_BASE + 0x6;
	public static final int ERR_SMBUS_COPYING_DRIVER = SMBUS_ERROR_BASE + 0x7;
	public static final int ERR_SMBUS_SERVICE_EXISTS = SMBUS_ERROR_BASE + 0x8;
	public static final int ERR_SMBUS_INVALID_HANDLE = SMBUS_ERROR_BASE + 0x9;
	public static final int ERR_SMBUS_TIMEOUT_WAITING_FOR_MUTEX = SMBUS_ERROR_BASE + 0xa;
	public static final int ERR_SMBUS_TIMEOUT = SMBUS_ERROR_BASE + 0xb;

	private native int OpenSMBus();
	private native int CloseSMBus(int handle);

	private native int SMBusSetClockSpeed(int handle, int freqeuncy);
	private native int SMBusGetClockSpeed(int handle);

	private native void SMBusWrite(int handle, byte slaveAddress, byte command, byte[] data);
	private native byte SMBusReadByte(int handle, byte slaveAddress, byte command);
	private native short SMBusReadWord(int handle, byte slaveAddress, byte command);
	private native byte[] SMBusReadBlock(int handle, byte slaveAddress, byte command);

	private native int SMBusGetLastError(int handle);
	private native void SMBusSetLastError(int handle, int error);

	private static Boolean lock;
	
	private static SmBus m_smBus = null;
	
	private SmBus() {
		lock = new Boolean(false);
	}
	
	public static SmBus getInstance() {
		if(m_smBus == null) {
			m_smBus = new SmBus();
		}
		
		return m_smBus;
	}
	
//	public void bind() {
//		lock = new Boolean(false);
//	}
//	
//	public void unbind() {
//		lock = null;
//	}
	
	public void write(byte slaveAddress, byte command, byte[] data) throws Exception {
		synchronized(lock) {
			int handle = OpenSMBus();
			if(handle < 0) {
				throw new IOException(LABEL + "unable to open SMBus");
			}
			
			if(SMBusSetClockSpeed(handle, SMBUS_50_KHZ) != 0) {
				throw new IOException(LABEL + "error setting SMBus clock speed");
			}
			
			SMBusWrite(handle, slaveAddress, command, data);
			
			CloseSMBus(handle);
		}
	}
	
	public byte readByte(byte slaveAddress, byte command) throws Exception {
		synchronized(lock) {
			int handle = OpenSMBus();
			if(handle < 0) {
				throw new IOException(LABEL + "unable to open SMBus");
			}
			
			if(SMBusSetClockSpeed(handle, SMBUS_50_KHZ) != 0) {
				throw new IOException(LABEL + "error setting SMBus clock speed");
			}
			
			byte result = SMBusReadByte(handle, slaveAddress, command);
			
			CloseSMBus(handle);
			
			return result;
		}
	}
	
	public short readWord(byte slaveAddress, byte command) throws Exception {
		synchronized(lock) {
			int handle = OpenSMBus();
			if(handle < 0) {
				throw new IOException(LABEL + "unable to open SMBus");
			}
			
			if(SMBusSetClockSpeed(handle, SMBUS_50_KHZ) != 0) {
				throw new IOException(LABEL + "error setting SMBus clock speed");
			}
			
			short result = SMBusReadWord(handle, slaveAddress, command);
			
			CloseSMBus(handle);
			
			return result;
		}
	}
	
	public byte[] readBlock(byte slaveAddress, byte command) throws Exception {
		synchronized(lock) {
			int handle = OpenSMBus();
			if(handle < 0) {
				throw new IOException(LABEL + "unable to open SMBus");
			}
			
			if(SMBusSetClockSpeed(handle, SMBUS_50_KHZ) != 0) {
				throw new IOException(LABEL + "error setting SMBus clock speed");
			}
			
			byte[] result = SMBusReadBlock(handle, slaveAddress, command);
			
			CloseSMBus(handle);
			
			return result;
		}
	}
	
	public int getLastError() throws Exception {
		synchronized(lock) {
			int handle = OpenSMBus();
			if(handle < 0) {
				throw new IOException(LABEL + "unable to open SMBus");
			}
			
			if(SMBusSetClockSpeed(handle, SMBUS_50_KHZ) != 0) {
				throw new IOException(LABEL + "error setting SMBus clock speed");
			}
			
			int result = SMBusGetLastError(handle);
			
			CloseSMBus(handle);
			
			return result;
		}
	}
	
	public void setLastError(int error) throws Exception {
		synchronized(lock) {
			int handle = OpenSMBus();
			if(handle < 0) {
				throw new IOException(LABEL + "unable to open SMBus");
			}
			
			if(SMBusSetClockSpeed(handle, SMBUS_50_KHZ) != 0) {
				throw new IOException(LABEL + "error setting SMBus clock speed");
			}
			
			SMBusSetLastError(handle, error);
			CloseSMBus(handle);
		}
	}
}
