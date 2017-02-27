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
 * @author IBM
 * @version 1.2.0
 * @since 1.0
 */
public class NSParallelPort extends ParallelPort {
	/**
	 * Define the lpt mode any (int) constant.
	 */
	public static final int LPT_MODE_ANY = 0;

	/**
	 * Define the lpt mode spp (int) constant.
	 */
	public static final int LPT_MODE_SPP = 1;

	/**
	 * Define the lpt mode ps2 (int) constant.
	 */
	public static final int LPT_MODE_PS2 = 2;

	/**
	 * Define the lpt mode epp (int) constant.
	 */
	public static final int LPT_MODE_EPP = 3;

	/**
	 * Define the lpt mode ecp (int) constant.
	 */
	public static final int LPT_MODE_ECP = 4;

	/**
	 * Define the lpt mode nibble (int) constant.
	 */
	public static final int LPT_MODE_NIBBLE = 5;

	/**
	 * Define the mode (int) field.
	 */
	private int mode = LPT_MODE_SPP; // only SPP mode is supported at this time

	/**
	 * Define the fd (int) field.
	 */
	int fd = -1; // file descriptor for the open device

	/**
	 * Define the fd (FileDescriptor) field.
	 */
	FileDescriptor FD = null; // FileDescriptor for the open device

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
	 * Define the outs suspended (boolean) field.
	 */
	boolean outsSuspended = false;

	/**
	 * Disable write buffering for default
	 */
	int outsBufferSize = 0;

	/**
	 * Define the outs buffer count (int) field.
	 */
	int outsBufferCount = 0;

	/**
	 * Define the listener (ParallelPortEventListener) field.
	 */
	private ParallelPortEventListener listener = null;

	/**
	 * Define the notify on error flag (boolean) field.
	 */
	private boolean notifyOnErrorFlag = false;

	/**
	 * Define the notify on buffer flag (boolean) field.
	 */
	boolean notifyOnBufferFlag = false;

	/**
	 * Define the error thread (ParallelErrorEventThread) field.
	 */
	private ParallelErrorEventThread errorThread = null;

	/**
	 * Define the dle (DeviceListEntry) field.
	 */
	private DeviceListEntry dle = null;

	/**
	 * Define the cd (NSCommDriver) field.
	 */
	private NSCommDriver cd = null;

	/**
	 * Constructs an instance of this class from the specified port name and driver parameters.
	 * @param portName The port name (<code>String</code>) parameter.
	 * @param driver The driver (<code>NSCommDriver</code>) parameter.
	 * @throws IOException IOException.
	 */
	NSParallelPort(final String portName, final NSCommDriver driver) throws IOException {
		/* NSParallelPort-extends-ParallelPort-extends-CommPort->name */
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
	 * Add event listener with the specified lst parameter.
	 * @param lst The lst (<code>ParallelPortEventListener</code>) parameter.
	 * @throws TooManyListenersException Too Many Listeners Exception.
	 * @see #removeEventListener()
	 */
	public synchronized void addEventListener(final ParallelPortEventListener lst) throws TooManyListenersException {
		if (this.listener != null) {
			throw new TooManyListenersException();
		}
		this.listener = lst;
		if (this.notifyOnErrorFlag && (this.errorThread == null)) {
			this.errorThread = new ParallelErrorEventThread(this.fd, this);
			// errorThread.setDaemon( true ); // check it out
			this.errorThread.start();
		}
	}

	/**
	 * Close.
	 */
	public void close() {
		// check if either fd or opened is not valid
		// nothing to be done
		if (this.fd == -1) {
			return;
		}
		// if the error thread is alive, kill it
		if (this.errorThread != null) {
			this.errorThread.setStopThreadFlag(1);
			this.errorThread = null;
			this.notifyOnErrorFlag = false;
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
		if (this.ins == null) {
			if ((this.ins = new NSDeviceInputStream(this, this.dle.portType)) == null) {
				throw new IOException();
			}
			this.ins.fd = this.fd;
		}
		return this.ins;
	}

	/**
	 * Gets the mode (int) value.
	 * @return The mode (<code>int</code>) value.
	 * @see #setMode(int)
	 */
	public int getMode() {
		return this.mode;
	}

	/**
	 * Gets the output buffer free (int) value.
	 * @return The output buffer free (<code>int</code>) value.
	 */
	public int getOutputBufferFree() {
		return (this.outsBufferSize > this.outsBufferCount ? this.outsBufferSize - this.outsBufferCount : 0);
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
		if (this.outs == null) {
			if ((this.outs = new NSDeviceOutputStream(this, this.dle.portType)) == null) {
				throw new IOException();
			}
			this.outs.fd = this.fd;
		}
		return this.outs;
	}

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
	 * Gets the paper out (boolean) value.
	 * @return The paper out (<code>boolean</code>) value.
	 */
	public boolean isPaperOut() {
		return isPaperOutNC(this.fd);
	}

	/**
	 * Is paper out nc with the specified fd parameter and return the boolean result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @return Results of the is paper out nc (<code>boolean</code>) value.
	 */
	private native boolean isPaperOutNC(final int fd);

	/**
	 * Gets the printer busy (boolean) value.
	 * @return The printer busy (<code>boolean</code>) value.
	 */
	public boolean isPrinterBusy() {
		return isPrinterBusyNC(this.fd);
	}

	/**
	 * Is printer busy nc with the specified fd parameter and return the boolean result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @return Results of the is printer busy nc (<code>boolean</code>) value.
	 */
	private native boolean isPrinterBusyNC(final int fd);

	/**
	 * Gets the printer error (boolean) value.
	 * @return The printer error (<code>boolean</code>) value.
	 */
	public boolean isPrinterError() {
		return isPrinterErrorNC(this.fd);
	}

	/**
	 * Is printer error nc with the specified fd parameter and return the boolean result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @return Results of the is printer error nc (<code>boolean</code>) value.
	 */
	private native boolean isPrinterErrorNC(final int fd);

	/**
	 * Gets the printer selected (boolean) value.
	 * @return The printer selected (<code>boolean</code>) value.
	 */
	public boolean isPrinterSelected() {
		return isPrinterSelectedNC(this.fd);
	}

	/**
	 * Is printer selected nc with the specified fd parameter and return the boolean result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @return Results of the is printer selected nc (<code>boolean</code>) value.
	 */
	private native boolean isPrinterSelectedNC(final int fd);

	/**
	 * Gets the printer timed out (boolean) value.
	 * @return The printer timed out (<code>boolean</code>) value.
	 */
	public boolean isPrinterTimedOut() {
		return isPrinterTimedOutNC(this.fd);
	}

	/**
	 * Is printer timed out nc with the specified fd parameter and return the boolean result.
	 * @param fd The fd (<code>int</code>) parameter.
	 * @return Results of the is printer timed out nc (<code>boolean</code>) value.
	 */
	private native boolean isPrinterTimedOutNC(final int fd);

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
		return (this.rcvThreshold == -1 ? false : true);
	}

	/**
	 * Gets the receive timeout enabled (boolean) value.
	 * @return The receive timeout enabled (<code>boolean</code>) value.
	 */
	public boolean isReceiveTimeoutEnabled() {
		return (this.rcvTimeout == -1 ? false : true);
	}

	/**
	 * Notify on buffer with the specified notify parameter.
	 * @param notify The notify (<code>boolean</code>) parameter.
	 */
	public synchronized void notifyOnBuffer(final boolean notify) {
		this.notifyOnBufferFlag = notify;
	}

	/**
	 * Notify on error with the specified notify parameter.
	 * @param notify The notify (<code>boolean</code>) parameter.
	 */
	public synchronized void notifyOnError(final boolean notify) {
		if (notify) {
			if (!this.notifyOnErrorFlag) {
				// instantiate ParallelErrorEventThread
				if ((this.errorThread == null) && (this.listener != null)) {
					this.errorThread = new ParallelErrorEventThread(this.fd, this);
					this.errorThread.start();
				}
				this.notifyOnErrorFlag = true;
			}
		} else {
			if (this.notifyOnErrorFlag) {
				/* Stop ParallelErrorEventThread */
				if (this.errorThread != null) {
					this.errorThread.setStopThreadFlag(1);
				}
				this.notifyOnErrorFlag = false;
				this.errorThread = null;
			}
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
	 * @see #addEventListener(ParallelPortEventListener)
	 */
	public synchronized void removeEventListener() {
		if (this.listener != null) {
			if (this.errorThread != null) {
				this.errorThread.setStopThreadFlag(1);
			}
			this.errorThread = null;
			this.listener = null;
		}
	}

	/**
	 * Report parallel event with the specified event type, oldvalue and newvalue parameters.
	 * @param eventType The event type (<code>int</code>) parameter.
	 * @param oldvalue The oldvalue (<code>boolean</code>) parameter.
	 * @param newvalue The newvalue (<code>boolean</code>) parameter.
	 */
	synchronized void reportParallelEvent(final int eventType, final boolean oldvalue, final boolean newvalue) {
		if (this.listener != null) {
			final ParallelPortEvent pe = new ParallelPortEvent(this, eventType, oldvalue, newvalue);
			this.listener.parallelEvent(pe);
		}
	}

	/**
	 * Restart.
	 */
	public void restart() {
		this.outsSuspended = false;
	}

	/**
	 * Sets the input buffer size value.
	 * @param size The size (<code>int</code>) parameter.
	 * @see #getInputBufferSize()
	 */
	public void setInputBufferSize(final int size) {
		/* do nothing */
	}

	/**
	 * Sets the mode value.
	 * @param md The md (<code>int</code>) parameter.
	 * @return The mode (<code>int</code>) value.
	 * @throws UnsupportedCommOperationException Unsupported Comm Operation Exception.
	 * @see #getMode()
	 */
	public int setMode(final int md) throws UnsupportedCommOperationException {
		throw new UnsupportedCommOperationException();
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
	 * Suspend.
	 */
	public void suspend() {
		this.outsSuspended = true;
	}
}
