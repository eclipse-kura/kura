package org.eclipse.soda.dk.comm;

/*************************************************************************
 * Copyright (c) 2007, 2009 IBM.                                         *
 * All rights reserved. This program and the accompanying materials      *
 * are made available under the terms of the Eclipse Public License v1.0 *
 * which accompanies this distribution, and is available at              *
 * http://www.eclipse.org/legal/epl-v10.html                             *
 *                                                                       *
 * Contributors:                                                         *
 *     IBM - initial API and implementation                              *
 ************************************************************************/
/**
 * @author IBM
 * @version 1.2.0
 * @since 1.0
 */
class SerialStatusEventThread extends Thread {
	/**
	 * Define the serial port (NSSerialPort) field.
	 */
	NSSerialPort serialPort = null;

	/**
	 * Define the polling time (int) field.
	 */
	private final int pollingTime = 5; //  ??

	/**
	 * Define the file descriptor (int) field.
	 */
	private int fileDescriptor = -1;

	/**
	 * Define the stop thread flag (int) field.
	 */
	private int stopThreadFlag = 0;

	/**
	 * Constructs an instance of this class from the specified fd and sp parameters.
	 * @param fd	The fd (<code>int</code>) parameter.
	 * @param sp	The sp (<code>NSSerialPort</code>) parameter.
	 */
	SerialStatusEventThread(final int fd, final NSSerialPort sp) {
		this.fileDescriptor = fd;
		this.serialPort = sp;
	}

	/**
	 * Gets the polling time (int) value.
	 * @return	The polling time (<code>int</code>) value.
	 */
	public int getPollingTime() {
		return this.pollingTime;
	}

	/**
	 * Gets the stop thread flag (int) value.
	 * @return	The stop thread flag (<code>int</code>) value.
	 * @see #setStopThreadFlag(int)
	 */
	public int getStopThreadFlag() {
		return this.stopThreadFlag;
	}

	/**
	 * Monitor serial status nc with the specified fd parameter.
	 * @param fd	The fd (<code>int</code>) parameter.
	 */
	private native void monitorSerialStatusNC(final int fd);

	/**
	 * Run.
	 */
	public void run() {
		monitorSerialStatusNC(this.fileDescriptor);
	}

	/**
	 * Sets the stop thread flag value.
	 * @param value	The value (<code>int</code>) parameter.
	 * @see #getStopThreadFlag()
	 */
	public void setStopThreadFlag(final int value) {
		this.stopThreadFlag = value;
		return;
	}
}
