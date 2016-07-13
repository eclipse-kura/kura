package org.eclipse.soda.dk.comm;

/*************************************************************************
 * Copyright (c) 1999, 2009 IBM.                                         *
 * All rights reserved. This program and the accompanying materials      *
 * are made available under the terms of the Eclipse Public License v1.0 *
 * which accompanies this distribution, and is available at              *
 * http://www.eclipse.org/legal/epl-v10.html                             *
 *                                                                       *
 * Contributors:                                                         *
 *     IBM - initial API and implementation                              *
 ************************************************************************/
import javax.comm.*;
import java.io.*;
import java.util.*;

/**
 * An RS-232 serial communications port. SerialPort describes the
 * low-level interface to a serial communications port made
 * available by the underlying system. SerialPort defines the
 * minimum required functionality for serial communications ports.
 * @author IBM
 * @version 1.2.0
 * @since 1.0
 */
class NSSerialPort extends SerialPort {
	/**
	 * Define the databits5 (int) constant.
	 */
	public static final int DATABITS_5 = 5;

	/**
	 * Define the databits6 (int) constant.
	 */
	public static final int DATABITS_6 = 6;

	/**
	 * Define the databits7 (int) constant.
	 */
	public static final int DATABITS_7 = 7;

	/**
	 * Define the databits8 (int) constant.
	 */
	public static final int DATABITS_8 = 8;

	/**
	 * Define the stopbits1 (int) constant.
	 */
	public static final int STOPBITS_1 = 1;

	/**
	 * Define the stopbits2 (int) constant.
	 */
	public static final int STOPBITS_2 = 2;

	/**
	 * Define the stopbits15 (int) constant.
	 */
	public static final int STOPBITS_1_5 = 3;

	/**
	 * Define the parity none (int) constant.
	 */
	public static final int PARITY_NONE = 0;

	/**
	 * Define the parity odd (int) constant.
	 */
	public static final int PARITY_ODD = 1;

	/**
	 * Define the parity even (int) constant.
	 */
	public static final int PARITY_EVEN = 2;

	/**
	 * Define the parity mark (int) constant.
	 */
	public static final int PARITY_MARK = 3;

	/**
	 * Define the parity space (int) constant.
	 */
	public static final int PARITY_SPACE = 4;

	/**
	 * Define the flowcontrol none (int) constant.
	 */
	public static final int FLOWCONTROL_NONE = 0;

	/**
	 * Define the flowcontrol rtscts in (int) constant.
	 */
	public static final int FLOWCONTROL_RTSCTS_IN = 1;

	/**
	 * Define the flowcontrol rtscts out (int) constant.
	 */
	public static final int FLOWCONTROL_RTSCTS_OUT = 2;

	/**
	 * Define the flowcontrol xonxoff in (int) constant.
	 */
	public static final int FLOWCONTROL_XONXOFF_IN = 4;

	/**
	 * Define the flowcontrol xonxoff out (int) constant.
	 */
	public static final int FLOWCONTROL_XONXOFF_OUT = 8;

	/**
	 * Define the flowcontrol (int) field.
	 */
	private int flowcontrol = FLOWCONTROL_NONE;

	/**
	 * Define the baudrate (int) field.
	 */
	private int baudrate = 9600;

	/**
	 * Define the databits (int) field.
	 */
	private int databits = DATABITS_8;

	/**
	 * Define the stopbits (int) field.
	 */
	private int stopbits = STOPBITS_1;

	/**
	 * Define the parity (int) field.
	 */
	private int parity = PARITY_NONE;

	/**
	 * Define the dtr (boolean) field.
	 */
	private boolean dtr;

	/**
	 * Define the rts (boolean) field.
	 */
	private boolean rts;

	/**
	 * Define the dle (DeviceListEntry) field.
	 */
	private DeviceListEntry dle = null;

	/**
	 * Define the cd (NSCommDriver) field.
	 */
	private NSCommDriver cd = null;

	/**
	 * Define the fd (int) field.
	 */
	int fd = -1; // file descriptor for the open device

	/**
	 * Define the fd (FileDescriptor) field.
	 */
	FileDescriptor FD = null; // FileDescriptor for the open device for which buffers can be built upon

	/**
	 * Define the ins (NSDeviceInputStream) field.
	 */
	private NSDeviceInputStream ins = null;

	/**
	 * Define the outs (NSDeviceOutputStream) field.
	 */
	private NSDeviceOutputStream outs = null;

	/**
	 * Define the rcv threshold (int) field.
	 */
	int rcvThreshold = -1;

	/**
	 * Define the rcv timeout (int) field.
	 */
	int rcvTimeout = -1;

	/**
	 * Define the rcv framing (boolean) field.
	 */
	boolean rcvFraming = false;

	/**
	 * Define the rcv framing byte (int) field.
	 */
	int rcvFramingByte;

	/**
	 * Define the rcv framing byte received (boolean) field.
	 */
	boolean rcvFramingByteReceived;

	/**
	 * Disable read buffering for now
	 */
	int insBufferSize = 0;

	/**
	 * Define the ins buffer count (int) field.
	 */
	int insBufferCount = 0;

	/**
	 * Disable write buffering for default
	 */
	int outsBufferSize = 0;

	/**
	 * Define the outs buffer count (int) field.
	 */
	int outsBufferCount = 0;

	/**
	 * Define the listener (SerialPortEventListener) field.
	 */
	private SerialPortEventListener listener = null;

	/**
	 * Define the notify on ctsflag (boolean) field.
	 */
	private boolean notifyOnCTSFlag = false;

	/**
	 * Define the notify on dsrflag (boolean) field.
	 */
	private boolean notifyOnDSRFlag = false;

	/**
	 * Define the notify on riflag (boolean) field.
	 */
	private boolean notifyOnRIFlag = false;

	/**
	 * Define the notify on cdflag (boolean) field.
	 */
	private boolean notifyOnCDFlag = false;

	/**
	 * Define the notify on orflag (boolean) field.
	 */
	private boolean notifyOnORFlag = false;

	/**
	 * Define the notify on peflag (boolean) field.
	 */
	private boolean notifyOnPEFlag = false;

	/**
	 * Define the notify on feflag (boolean) field.
	 */
	private boolean notifyOnFEFlag = false;

	/**
	 * Define the notify on biflag (boolean) field.
	 */
	private boolean notifyOnBIFlag = false;

	/**
	 * Define the notify on buffer flag (boolean) field.
	 */
	boolean notifyOnBufferFlag = false;

	/**
	 * Define the notify on data flag (boolean) field.
	 */
	private boolean notifyOnDataFlag = false;

	/**
	 * Define the status thread (SerialStatusEventThread) field.
	 */
	private SerialStatusEventThread statusThread = null;

	/**
	 * Define the data thread (SerialDataEventThread) field.
	 */
	private SerialDataEventThread dataThread = null;

	/**
	 * Constructor
	 * @param portName The port name (<code>String</code>) parameter.
	 * @param driver The driver (<code>NSCommDriver</code>) parameter.
	 * @throws IOException IOException.
	 */
	public NSSerialPort(final String portName, final NSCommDriver driver) throws IOException {
		/* caller wants port portName */
		/* NSSerialPort-extends-SerialPort-extends-CommPort->name */
		this.name = portName;
		// save CommDriver
		this.cd = driver;
		// look for portName in DeviceList
		for (DeviceListEntry cur = this.cd.getFirstDLE(); cur != null; cur = this.cd.getNextDLE(cur)) {
			if (cur.logicalName.equals(portName)) {
				/* found the portName in list, attempt to open it using native method. */
				if ((this.fd == -1) || !cur.opened) {
					if ((this.fd = openDeviceNC(cur.physicalName, cur.semID)) == -1) {
						// file descriptor is NOT valid, throw an Exception
						throw new IOException();
					}
					/* Got a good file descriptor. */
					/* keep a copy of the DeviceListEntry where you found the portName */
					/* get a FileDescriptor object */
					/* turn opened ON */
					this.dle = cur;
					this.dle.opened = true;
				} else {
					throw new IOException();
				}
				break; // found our port
			}
		}
	}

	/**
	 * Add event listener with the specified lstnr parameter.
	 * @param lstnr The lstnr (<code>SerialPortEventListener</code>) parameter.
	 * @throws TooManyListenersException Too Many Listeners Exception.
	 * @see #removeEventListener()
	 */
	public synchronized void addEventListener(final SerialPortEventListener lstnr) throws TooManyListenersException {
		if (this.listener != null) {
			throw new TooManyListenersException();
		}
		this.listener = lstnr;
		// check all other related flags, all must be false
		if ((this.notifyOnDSRFlag || this.notifyOnRIFlag || this.notifyOnCDFlag || this.notifyOnORFlag || this.notifyOnPEFlag || this.notifyOnFEFlag || this.notifyOnCTSFlag || this.notifyOnBIFlag) && (this.statusThread == null)) {
			this.statusThread = new SerialStatusEventThread(this.fd, this);
			// statusThread.setDaemon( true ); // check it out ???
			this.statusThread.start();
		}
		if (this.notifyOnDataFlag && (this.dataThread == null)) {
			this.dataThread = new SerialDataEventThread(this.fd, this);
			// dataThread.setDaemon( true ); // check it out ???
			this.dataThread.start();
		}
	}

	/**
	 * Close.
	 */
	public void close() {
		if (this.fd == -1) {
			return;
		}
		// if thread are alive, kill them
		if (this.statusThread != null) {
			this.statusThread.setStopThreadFlag(1);
			this.notifyOnCTSFlag = false;
			this.notifyOnDSRFlag = false;
			this.notifyOnRIFlag = false;
			this.notifyOnCDFlag = false;
			this.notifyOnORFlag = false;
			this.notifyOnPEFlag = false;
			this.notifyOnFEFlag = false;
			this.notifyOnBIFlag = false;
		}
		if (this.dataThread != null) {
			this.dataThread.setStopThreadFlag(1);
			this.notifyOnDataFlag = false;
		}
		// check ins and outs
		if (this.outs != null) {
			try {
				this.outs.flush();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			this.outs = null;
		}
		if (this.ins != null) {
			this.ins = null;
		}
		/* close the device. */
		closeDeviceNC(this.fd, this.dle.semID);
		/* reset fd and opened. */
		this.fd = -1;
		this.dle.opened = false;
		// close the commport
		super.close();
	}

	/**
	 * Close device nc with the specified fd and sem id parameters and return the int result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @param semID The sem id (<code>int</code>) parameter.
	 * @return Results of the close device nc (<code>int</code>) value.
	 */
	private native int closeDeviceNC(final int fd, final int semID);

	/**
	 * Disable receive framing.
	 * @see #enableReceiveFraming(int)
	 */
	public void disableReceiveFraming() {
		this.rcvFraming = false;
	}

	/**
	 * Disable receive threshold.
	 * @see #enableReceiveThreshold(int)
	 * @see #getReceiveThreshold()
	 */
	public void disableReceiveThreshold() {
		this.rcvThreshold = -1;
	}

	/**
	 * Disable receive timeout.
	 * @see #enableReceiveTimeout(int)
	 * @see #getReceiveTimeout()
	 */
	public void disableReceiveTimeout() {
		this.rcvTimeout = -1;
	}

	/**
	 * Enable receive framing with the specified rcv framing byte parameter.
	 * @param rcvFramingByte The rcv framing byte (<code>int</code>) parameter.
	 * @throws UnsupportedCommOperationException Unsupported Comm Operation Exception.
	 * @see #disableReceiveFraming()
	 */
	public void enableReceiveFraming(final int rcvFramingByte) throws UnsupportedCommOperationException {
		throw new UnsupportedCommOperationException();
	}

	/**
	 * Enable receive threshold with the specified thresh parameter.
	 * @param thresh The thresh (<code>int</code>) parameter.
	 * @throws UnsupportedCommOperationException Unsupported Comm Operation Exception.
	 * @see #disableReceiveThreshold()
	 * @see #getReceiveThreshold()
	 */
	public void enableReceiveThreshold(final int thresh) throws UnsupportedCommOperationException {
		if (thresh > 0) {
			this.rcvThreshold = thresh;
		}
	}

	/**
	 * Enable receive timeout with the specified rt parameter.
	 * @param rt The rt (<code>int</code>) parameter.
	 * @throws UnsupportedCommOperationException Unsupported Comm Operation Exception.
	 * @see #disableReceiveTimeout()
	 * @see #getReceiveTimeout()
	 */
	public void enableReceiveTimeout(final int rt) throws UnsupportedCommOperationException {
		if (rt > 0) {
			this.rcvTimeout = rt;
		} else if (rt == 0) {
			this.rcvTimeout = -1;
		}
	}

	/**
	 * Finalize.
	 * @throws IOException IOException.
	 */
	protected void finalize() throws IOException {
		close();
	}

	/**
	 * Gets the baud rate (int) value.
	 * @return The baud rate (<code>int</code>) value.
	 */
	public int getBaudRate() {
		int bdrate = 0;
		if (this.fd > -1) {
			bdrate = getBaudRateNC(this.fd);
			if (bdrate < 0) {
				bdrate = 0;
			} else {
				this.baudrate = bdrate;
				// no need to map native values to java values here
			}
		}
		return bdrate;
	}

	/**
	 * Get baud rate nc with the specified fd parameter and return the int result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @return Results of the get baud rate nc (<code>int</code>) value.
	 */
	private native int getBaudRateNC(final int fd);

	/**
	 * Gets the baudrate (int) value.
	 * @return The baudrate (<code>int</code>) value.
	 */
	public int getBaudrate() {
		return this.baudrate;
	}

	/**
	 * Gets the data bits (int) value.
	 * @return The data bits (<code>int</code>) value.
	 */
	public int getDataBits() {
		int db = 0;
		if (this.fd > -1) {
			db = getDataBitsNC(this.fd);
			if (db != -1) {
				switch (db) {
				case 5:
					this.databits = DATABITS_5;
					break;
				case 6:
					this.databits = DATABITS_6;
					break;
				case 7:
					this.databits = DATABITS_7;
					break;
				case 8:
					this.databits = DATABITS_8;
					break;
				}
			}
		}
		return this.databits;
	}

	/**
	 * Get data bits nc with the specified fd parameter and return the int result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @return Results of the get data bits nc (<code>int</code>) value.
	 */
	private native int getDataBitsNC(final int fd);

	/**
	 * Gets the currently configured flow control mode.
	 * Returns:
	 * an integer bitmask of the modes FLOWCONTROL_NONE,
	 * FLOWCONTROL_RTSCTS_IN,
	 * FLOWCONTROL_RTSCTS_OUT,
	 * FLOWCONTROL_XONXOFF_IN, and
	 * FLOWCONTROL_XONXOFF_OUT.
	 * @return Results of the get flow control mode (<code>int</code>) value.
	 * @see #setFlowControlMode(int)
	 */
	public int getFlowControlMode() {
		if (this.fd > -1) {
			final int retCode = getFlowControlModeNC(this.fd);
			if (retCode == -1) {
				return this.flowcontrol;
			}
			if (retCode == 0) {
				this.flowcontrol = FLOWCONTROL_NONE;
			} else {
				int fl = 0;
				if ((retCode & 1) != 0) {
					fl |= FLOWCONTROL_RTSCTS_IN;
				}
				if ((retCode & 2) != 0) {
					fl |= FLOWCONTROL_RTSCTS_OUT;
				}
				if ((retCode & 4) != 0) {
					fl |= FLOWCONTROL_XONXOFF_IN;
				}
				if ((retCode & 8) != 0) {
					fl |= FLOWCONTROL_XONXOFF_OUT;
				}
				this.flowcontrol = fl;
			}
		} else {
			return this.flowcontrol;
		}
		return this.flowcontrol;
	}

	/**
	 * Get flow control mode nc with the specified fd parameter and return the int result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @return Results of the get flow control mode nc (<code>int</code>) value.
	 */
	private native int getFlowControlModeNC(final int fd);

	/**
	 * Gets the input buffer size (int) value.
	 * @return The input buffer size (<code>int</code>) value.
	 * @see #setInputBufferSize(int)
	 */
	public int getInputBufferSize() {
		return this.insBufferSize;
	}

	/**
	 * Gets the input stream value.
	 * @return The input stream (<code>InputStream</code>) value.
	 * @throws IOException IOException.
	 */
	public InputStream getInputStream() throws IOException {
		if (this.ins != null) {
			return this.ins;
		}
		if ((this.ins = new NSDeviceInputStream(this, this.dle.portType)) == null) {
			throw new IOException();
		}
		this.ins.fd = this.fd;
		return this.ins;
	}

	/**
	 * Gets the output buffer size (int) value.
	 * @return The output buffer size (<code>int</code>) value.
	 * @see #setOutputBufferSize(int)
	 */
	public int getOutputBufferSize() {
		return this.outsBufferSize;
	}

	/**
	 * Gets the output stream value.
	 * @return The output stream (<code>OutputStream</code>) value.
	 * @throws IOException IOException.
	 */
	public OutputStream getOutputStream() throws IOException {
		if (this.outs != null) {
			return this.outs;
		}
		/* Y: get a new DeviceOutputStream */
		if ((this.outs = new NSDeviceOutputStream(this, this.dle.portType)) == null) {
			throw new IOException();
		}
		// what do I do here
		this.outs.fd = this.fd;
		return this.outs;
	}

	/**
	 * Gets the parity (int) value.
	 * @return The parity (<code>int</code>) value.
	 */
	public int getParity() {
		int p = 0;
		if (this.fd > -1) {
			p = getParityNC(this.fd);
			if (p != -1) {
				switch (p) {
				case 0:
					this.parity = PARITY_NONE;
					break;
				case 1:
					this.parity = PARITY_ODD;
					break;
				case 2:
					this.parity = PARITY_EVEN;
					break;
				case 3:
					this.parity = PARITY_MARK;
					break;
				case 4:
					this.parity = PARITY_SPACE;
					break;
				}
			}
		}
		return this.parity;
	}

	/**
	 * Get parity nc with the specified fd parameter and return the int result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @return Results of the get parity nc (<code>int</code>) value.
	 */
	private native int getParityNC(final int fd);

	/**
	 * Gets the receive framing byte (int) value.
	 * @return The receive framing byte (<code>int</code>) value.
	 */
	public int getReceiveFramingByte() {
		return this.rcvFramingByte;
	}

	/**
	 * Gets the receive threshold (int) value.
	 * @return The receive threshold (<code>int</code>) value.
	 * @see #disableReceiveThreshold()
	 * @see #enableReceiveThreshold(int)
	 */
	public int getReceiveThreshold() {
		return this.rcvThreshold;
	}

	/**
	 * Gets the receive timeout (int) value.
	 * @return The receive timeout (<code>int</code>) value.
	 * @see #disableReceiveTimeout()
	 * @see #enableReceiveTimeout(int)
	 */
	public int getReceiveTimeout() {
		return this.rcvTimeout;
	}

	/**
	 * Gets the stop bits (int) value.
	 * @return The stop bits (<code>int</code>) value.
	 */
	public int getStopBits() {
		int sb = 0;
		if (this.fd > -1) {
			sb = getStopBitsNC(this.fd);
			if (sb != -1) {
				switch (sb) {
				case 0:
					this.stopbits = STOPBITS_1_5;
					break;
				case 1:
					this.stopbits = STOPBITS_1;
					break;
				case 2:
					this.stopbits = STOPBITS_2;
					break;
				}
			}
		}
		return this.stopbits;
	}

	/**
	 * Get stop bits nc with the specified fd parameter and return the int result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @return Results of the get stop bits nc (<code>int</code>) value.
	 */
	private native int getStopBitsNC(final int fd);

	/**
	 * Gets the cd (boolean) value.
	 * @return The cd (<code>boolean</code>) value.
	 */
	public boolean isCD() {
		return isCDNC();
	}

	/**
	 * Gets the cdnc (boolean) value.
	 * @return The cdnc (<code>boolean</code>) value.
	 */
	private native boolean isCDNC();

	/**
	 * Gets the cts (boolean) value.
	 * @return The cts (<code>boolean</code>) value.
	 * @see #notifyOnCTS(boolean)
	 */
	public boolean isCTS() {
		return isCTSNC();
	}

	/**
	 * Gets the ctsnc (boolean) value.
	 * @return The ctsnc (<code>boolean</code>) value.
	 */
	private native boolean isCTSNC();

	/**
	 * Gets the dsr (boolean) value.
	 * @return The dsr (<code>boolean</code>) value.
	 * @see #notifyOnDSR(boolean)
	 */
	public boolean isDSR() {
		return isDSRNC();
	}

	/**
	 * Gets the dsrnc (boolean) value.
	 * @return The dsrnc (<code>boolean</code>) value.
	 */
	private native boolean isDSRNC();

	/**
	 * Gets the dtr (boolean) value.
	 * @return The dtr (<code>boolean</code>) value.
	 * @see #setDTR(boolean)
	 */
	public boolean isDTR() {
		return isDTRNC();
	}

	/**
	 * Gets the dtrnc (boolean) value.
	 * @return The dtrnc (<code>boolean</code>) value.
	 */
	private native boolean isDTRNC();

	/**
	 * Gets the dtr (boolean) value.
	 * @return The dtr (<code>boolean</code>) value.
	 */
	public boolean isDtr() {
		return this.dtr;
	}

	/**
	 * Gets the ri (boolean) value.
	 * @return The ri (<code>boolean</code>) value.
	 */
	public boolean isRI() {
		return isRINC();
	}

	/**
	 * Gets the rinc (boolean) value.
	 * @return The rinc (<code>boolean</code>) value.
	 */
	private native boolean isRINC();

	/**
	 * Gets the rts (boolean) value.
	 * @return The rts (<code>boolean</code>) value.
	 * @see #setRTS(boolean)
	 */
	public boolean isRTS() {
		return isRTSNC();
	}

	/**
	 * Gets the rtsnc (boolean) value.
	 * @return The rtsnc (<code>boolean</code>) value.
	 */
	private native boolean isRTSNC();

	/**
	 * Gets the receive framing enabled (boolean) value.
	 * @return The receive framing enabled (<code>boolean</code>) value.
	 */
	public boolean isReceiveFramingEnabled() {
		return this.rcvFraming;
	}

	/**
	 * Gets the receive threshold enabled (boolean) value.
	 * @return The receive threshold enabled (<code>boolean</code>) value.
	 */
	public boolean isReceiveThresholdEnabled() {
		if (this.rcvThreshold == -1) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the receive timeout enabled (boolean) value.
	 * @return The receive timeout enabled (<code>boolean</code>) value.
	 */
	public boolean isReceiveTimeoutEnabled() {
		if (this.rcvTimeout == -1) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the rts (boolean) value.
	 * @return The rts (<code>boolean</code>) value.
	 */
	public boolean isRts() {
		return this.rts;
	}

	/**
	 * Notify on break interrupt with the specified notify parameter.
	 * @param notify The notify (<code>boolean</code>) parameter.
	 */
	public synchronized void notifyOnBreakInterrupt(final boolean notify) {
		if (notify && this.notifyOnBIFlag) {
			return; // already enabled
		}
		if (notify && !this.notifyOnBIFlag) {
			// instantiate SerialStatusEventThread
			if (this.statusThread == null) {
				this.statusThread = new SerialStatusEventThread(this.fd, this);
				// statusThread.setDaemon( true ); // check it out
				this.statusThread.start();
			}
			this.notifyOnBIFlag = true;
		} else {
			// check all other related flags, all must be false
			if (!this.notifyOnCTSFlag && !this.notifyOnDSRFlag && !this.notifyOnRIFlag && !this.notifyOnCDFlag && !this.notifyOnORFlag && !this.notifyOnPEFlag && !this.notifyOnFEFlag) {
				if (this.statusThread != null) {
					this.statusThread.setStopThreadFlag(1);
					this.statusThread = null;
				}
			}
			this.notifyOnBIFlag = false;
		}
	}

	/**
	 * Notify on cts with the specified notify parameter.
	 * @param notify The notify (<code>boolean</code>) parameter.
	 */
	public synchronized void notifyOnCTS(final boolean notify) {
		if (notify && this.notifyOnCTSFlag) {
			return; // already enabled
		}
		if (notify && !this.notifyOnCTSFlag) {
			if (this.statusThread == null) {
				this.statusThread = new SerialStatusEventThread(this.fd, this);
				// statusThread.setDaemon( true ); // check it out
				this.statusThread.start();
			}
			this.notifyOnCTSFlag = true;
		} else {
			// check all other related flags, all must be false
			if (!this.notifyOnDSRFlag && !this.notifyOnRIFlag && !this.notifyOnCDFlag && !this.notifyOnORFlag && !this.notifyOnPEFlag && !this.notifyOnFEFlag && !this.notifyOnBIFlag) {
				if (this.statusThread != null) {
					this.statusThread.setStopThreadFlag(1);
					this.statusThread = null;
				}
			}
			this.notifyOnCTSFlag = false;
		}
	}

	/**
	 * Notify on carrier detect with the specified notify parameter.
	 * @param notify The notify (<code>boolean</code>) parameter.
	 */
	public synchronized void notifyOnCarrierDetect(final boolean notify) {
		if (notify && this.notifyOnCDFlag) {
			return; // already enabled
		}
		if (notify && !this.notifyOnCDFlag) {
			// instantiate SerialStatusEventThread
			if (this.statusThread == null) {
				this.statusThread = new SerialStatusEventThread(this.fd, this);
				// statusThread.setDaemon( true ); // check it out
				this.statusThread.start();
			}
			this.notifyOnCDFlag = true;
		} else {
			// check all other related flags, all must be false
			if (!this.notifyOnCTSFlag && !this.notifyOnDSRFlag && !this.notifyOnORFlag && !this.notifyOnRIFlag && !this.notifyOnPEFlag && !this.notifyOnFEFlag && !this.notifyOnBIFlag) {
				if (this.statusThread != null) {
					this.statusThread.setStopThreadFlag(1);
					this.statusThread = null;
				}
			}
			this.notifyOnCDFlag = false;
		}
	}

	/**
	 * Notify on dsr with the specified notify parameter.
	 * @param notify The notify (<code>boolean</code>) parameter.
	 */
	public synchronized void notifyOnDSR(final boolean notify) {
		if (notify && this.notifyOnDSRFlag) {
			return; // already enabled
		}
		if (notify && !this.notifyOnDSRFlag) {
			if (this.statusThread == null) {
				this.statusThread = new SerialStatusEventThread(this.fd, this);
				this.statusThread.start();
			}
			this.notifyOnDSRFlag = true;
		} else {
			// check all other related flags, all must be false
			if (!this.notifyOnCTSFlag && !this.notifyOnRIFlag && !this.notifyOnCDFlag && !this.notifyOnORFlag && !this.notifyOnPEFlag && !this.notifyOnFEFlag && !this.notifyOnBIFlag) {
				if (this.statusThread != null) {
					this.statusThread.setStopThreadFlag(1);
					this.statusThread = null;
				}
			}
			this.notifyOnDSRFlag = false;
		}
	}

	/**
	 * Notify on data available with the specified notify parameter.
	 * @param notify The notify (<code>boolean</code>) parameter.
	 */
	public synchronized void notifyOnDataAvailable(final boolean notify) {
		if (notify) {
			if (!this.notifyOnDataFlag) {
				// instantiate SerialDataEventThread
				if (this.dataThread == null) {
					this.dataThread = new SerialDataEventThread(this.fd, this);
					// dataThread.setDaemon( true ); // check it out
					this.dataThread.start();
				}
				this.notifyOnDataFlag = true;
			}
		} else {
			if (this.notifyOnDataFlag) {
				/* Stop SerialDataEventThread */
				if (this.dataThread != null) {
					this.dataThread.setStopThreadFlag(1);
				}
				this.notifyOnDataFlag = false;
				this.dataThread = null;
			}
		}
	}

	/**
	 * Notify on framing error with the specified notify parameter.
	 * @param notify The notify (<code>boolean</code>) parameter.
	 */
	public synchronized void notifyOnFramingError(final boolean notify) {
		if (notify && this.notifyOnFEFlag) {
			return; // already enabled
		}
		if (notify && !this.notifyOnFEFlag) {
			if (this.statusThread == null) {
				this.statusThread = new SerialStatusEventThread(this.fd, this);
				// statusThread.setDaemon( true ); // check it out
				this.statusThread.start();
			}
			this.notifyOnFEFlag = true;
		} else {
			// check all other related flags, all must be false
			if (!this.notifyOnCTSFlag && !this.notifyOnDSRFlag && !this.notifyOnRIFlag && !this.notifyOnCDFlag && !this.notifyOnORFlag && !this.notifyOnPEFlag && !this.notifyOnBIFlag) {
				if (this.statusThread != null) {
					this.statusThread.setStopThreadFlag(1);
					this.statusThread = null;
				}
			}
			this.notifyOnFEFlag = false;
		}
	}

	/**
	 * Notify on output empty with the specified notify parameter.
	 * @param notify The notify (<code>boolean</code>) parameter.
	 */
	public synchronized void notifyOnOutputEmpty(final boolean notify) {
		this.notifyOnBufferFlag = notify;
	}

	/**
	 * Notify on overrun error with the specified notify parameter.
	 * @param notify The notify (<code>boolean</code>) parameter.
	 */
	public synchronized void notifyOnOverrunError(final boolean notify) {
		if (notify && this.notifyOnORFlag) {
			return; // already enabled
		}
		if (notify && !this.notifyOnORFlag) {
			// instantiate SerialStatusEventThread
			if (this.statusThread == null) {
				this.statusThread = new SerialStatusEventThread(this.fd, this);
				// statusThread.setDaemon( true ); // check it out
				this.statusThread.start();
			}
			this.notifyOnORFlag = true;
		} else {
			// check all other related flags, all must be false
			if (!this.notifyOnCTSFlag && !this.notifyOnDSRFlag && !this.notifyOnRIFlag && !this.notifyOnCDFlag && !this.notifyOnPEFlag && !this.notifyOnFEFlag && !this.notifyOnBIFlag) {
				if (this.statusThread != null) {
					this.statusThread.setStopThreadFlag(1);
					this.statusThread = null;
				}
			}
			this.notifyOnORFlag = false;
		}
	}

	/**
	 * Notify on parity error with the specified notify parameter.
	 * @param notify The notify (<code>boolean</code>) parameter.
	 */
	public synchronized void notifyOnParityError(final boolean notify) {
		if (notify && this.notifyOnPEFlag) {
			return; // already enabled
		}
		if (notify && !this.notifyOnPEFlag) {
			// instantiate SerialStatusEventThread
			if (this.statusThread == null) {
				this.statusThread = new SerialStatusEventThread(this.fd, this);
				// statusThread.setDaemon( true ); // check it out
				this.statusThread.start();
			}
			this.notifyOnPEFlag = true;
		} else {
			// check all other related flags, all must be false
			if (!this.notifyOnCTSFlag && !this.notifyOnDSRFlag && !this.notifyOnRIFlag && !this.notifyOnCDFlag && !this.notifyOnORFlag && !this.notifyOnFEFlag && !this.notifyOnBIFlag) {
				if (this.statusThread != null) {
					this.statusThread.setStopThreadFlag(1);
					this.statusThread = null;
				}
			}
			this.notifyOnPEFlag = false;
		}
	}

	/**
	 * Notify on ring indicator with the specified notify parameter.
	 * @param notify The notify (<code>boolean</code>) parameter.
	 */
	public synchronized void notifyOnRingIndicator(final boolean notify) {
		if (notify && this.notifyOnRIFlag) {
			return; // already enabled
		}
		if (notify && !this.notifyOnRIFlag) {
			if (this.statusThread == null) {
				this.statusThread = new SerialStatusEventThread(this.fd, this);
				// statusThread.setDaemon( true ); // check it out
				this.statusThread.start();
			}
			this.notifyOnRIFlag = true;
		} else {
			// check all other related flags, all must be false
			if (!this.notifyOnCTSFlag && !this.notifyOnDSRFlag && !this.notifyOnCDFlag && !this.notifyOnORFlag && !this.notifyOnPEFlag && !this.notifyOnFEFlag && !this.notifyOnBIFlag) {
				if (this.statusThread != null) {
					this.statusThread.setStopThreadFlag(1);
					this.statusThread = null;
				}
			}
			this.notifyOnRIFlag = false;
		}
	}

	/**
	 * Open device nc with the specified device name and sem id parameters and return the int result.
	 * @param deviceName The device name (<code>String</code>) parameter.
	 * @param semID The sem id (<code>int</code>) parameter.
	 * @return Results of the open device nc (<code>int</code>) value.
	 */
	private native int openDeviceNC(final String deviceName, final int semID);

	/**
	 * Remove event listener.
	 * @see #addEventListener(SerialPortEventListener)
	 */
	public synchronized void removeEventListener() {
		if (this.listener != null) {
			if (this.statusThread != null) {
				this.statusThread.setStopThreadFlag(1);
			}
			this.statusThread = null;
			if (this.dataThread != null) {
				this.dataThread.setStopThreadFlag(1);
			}
			this.dataThread = null;
			this.listener = null;
		}
	}

	/**
	 * Report serial event with the specified event type, oldvalue and newvalue parameters.
	 * @param eventType The event type (<code>int</code>) parameter.
	 * @param oldvalue The oldvalue (<code>boolean</code>) parameter.
	 * @param newvalue The newvalue (<code>boolean</code>) parameter.
	 */
	synchronized void reportSerialEvent(final int eventType, final boolean oldvalue, final boolean newvalue) {
		if (this.listener != null) {
			final SerialPortEvent se = new SerialPortEvent(this, eventType, oldvalue, newvalue);
			this.listener.serialEvent(se);
		}
	}

	/**
	 * Send break with the specified millis parameter.
	 * @param millis The millis (<code>int</code>) parameter.
	 */
	public void sendBreak(final int millis) {
		if (this.fd != -1) {
			sendBreakNC(this.fd, millis);
		}
	}

	/**
	 * Send break nc with the specified fd and millis parameters and return the int result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @param millis The millis (<code>int</code>) parameter.
	 * @return Results of the send break nc (<code>int</code>) value.
	 */
	private native int sendBreakNC(final int fd, final int millis);

	/**
	 * Sets the dtr value.
	 * @param dtr The dtr (<code>boolean</code>) parameter.
	 * @see #isDTR()
	 */
	public void setDTR(final boolean dtr) {
		setDTRNC(dtr);
	}

	/**
	 * Sets the dtrnc value.
	 * @param dtr The dtr (<code>boolean</code>) parameter.
	 */
	private native void setDTRNC(final boolean dtr);

	/**
	 * Sets the flow control mode value.
	 * @param flowctrl The flowctrl (<code>int</code>) parameter.
	 * @throws UnsupportedCommOperationException Unsupported Comm Operation Exception.
	 * @see #getFlowControlMode()
	 */
	public void setFlowControlMode(final int flowctrl) throws UnsupportedCommOperationException {
		/* Check for invalid combinations. */
		if ((this.fd == -1) ||
		/* Now FLOWCONTROL_NONE is 0 instead of 1, and hence no need for this
		   check below!!! */
		/**************************
		   (((flowctrl & FLOWCONTROL_NONE) != 0) &&
		(((flowctrl & FLOWCONTROL_RTSCTS_IN) != 0) ||
		 ((flowctrl & FLOWCONTROL_RTSCTS_OUT) != 0) ||
		 ((flowctrl & FLOWCONTROL_XONXOFF_IN) != 0) ||
		 ((flowctrl & FLOWCONTROL_XONXOFF_OUT) != 0))) ||
		 **************************/
		(((flowctrl & FLOWCONTROL_RTSCTS_IN) != 0) && ((flowctrl & FLOWCONTROL_XONXOFF_OUT) != 0)) || (((flowctrl & FLOWCONTROL_XONXOFF_IN) != 0) && ((flowctrl & FLOWCONTROL_RTSCTS_OUT) != 0))
				|| (((flowctrl & FLOWCONTROL_RTSCTS_IN) != 0) && ((flowctrl & FLOWCONTROL_XONXOFF_IN) != 0)) || (((flowctrl & FLOWCONTROL_RTSCTS_OUT) != 0) && ((flowctrl & FLOWCONTROL_XONXOFF_OUT) != 0))) {
			throw new UnsupportedCommOperationException();
		}
		// retcode of -1 is a problem
		if (setFlowControlModeNC(this.fd, flowctrl) != -1) {
			this.flowcontrol = flowctrl;
		} else {
			throw new UnsupportedCommOperationException();
		}
	}

	/**
	 * Set flow control mode nc with the specified fd and flowctrl parameters and return the int result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @param flowctrl The flowctrl (<code>int</code>) parameter.
	 * @return Results of the set flow control mode nc (<code>int</code>) value.
	 */
	private native int setFlowControlModeNC(final int fd, final int flowctrl);

	/**
	 * Sets the input buffer size value.
	 * @param size The size (<code>int</code>) parameter.
	 * @see #getInputBufferSize()
	 */
	public void setInputBufferSize(final int size) {
		/* do nothing */
	}

	/**
	 * Sets the output buffer size value.
	 * @param size The size (<code>int</code>) parameter.
	 * @see #getOutputBufferSize()
	 */
	public void setOutputBufferSize(final int size) {
		if (size >= 0) {
			this.outsBufferSize = size;
		}
	}

	/**
	 * Sets the rts value.
	 * @param rts The rts (<code>boolean</code>) parameter.
	 * @see #isRTS()
	 */
	public void setRTS(final boolean rts) {
		setRTSNC(rts);
	}

	/**
	 * Sets the rtsnc value.
	 * @param rts The rts (<code>boolean</code>) parameter.
	 */
	private native void setRTSNC(final boolean rts);

	/**
	 * Sets the rcv fifo trigger value.
	 * @param trigger The trigger (<code>int</code>) parameter.
	 */
	public void setRcvFifoTrigger(final int trigger) {
		/* do nothing */
	}

	/**
	 * Set serial port params with the specified bd, db, sb and par parameters.
	 * @param bd The bd (<code>int</code>) parameter.
	 * @param db The db (<code>int</code>) parameter.
	 * @param sb The sb (<code>int</code>) parameter.
	 * @param par The par (<code>int</code>) parameter.
	 * @throws UnsupportedCommOperationException Unsupported Comm Operation Exception.
	 */
	public void setSerialPortParams(final int bd, final int db, final int sb, final int par) throws UnsupportedCommOperationException {
		/* Validate the values. */
		if (this.fd == -1) {
			throw new UnsupportedCommOperationException();
		}
		if ((db != DATABITS_5) && (db != DATABITS_6) && (db != DATABITS_7) && (db != DATABITS_8)) {
			throw new UnsupportedCommOperationException();
		}
		if ((sb != STOPBITS_1) && (sb != STOPBITS_2) && (sb != STOPBITS_1_5)) { // 1.5 not supported
			throw new UnsupportedCommOperationException();
		}
		if ((par != PARITY_NONE) && (par != PARITY_ODD) && (par != PARITY_EVEN) && (par != PARITY_MARK) && (par != PARITY_SPACE)) {
			throw new UnsupportedCommOperationException();
		}
		/* Now set the desired communication characteristics. */
		if (setSerialPortParamsNC(this.fd, bd, db, sb, par) < 0) {
			throw new UnsupportedCommOperationException();
		}
	}

	/**
	 * Set serial port params nc with the specified fd, bd, db, sb and par parameters and return the int result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @param bd The bd (<code>int</code>) parameter.
	 * @param db The db (<code>int</code>) parameter.
	 * @param sb The sb (<code>int</code>) parameter.
	 * @param par The par (<code>int</code>) parameter.
	 * @return Results of the set serial port params nc (<code>int</code>) value.
	 */
	private native int setSerialPortParamsNC(final int fd, final int bd, final int db, final int sb, final int par);
}
