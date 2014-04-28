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
package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import javax.usb.*;

/**
 * Proxy implementation for Linux's device-based access.
 * @author Dan Streetman
 */
class LinuxDeviceProxy extends LinuxRequestProxy
{
	/**
	 * Constructor.
	 * @param k The native key.  The key cannot be changed.
	 */
	public LinuxDeviceProxy(String k)
	{
		super();
		key = k;
	}

	//*************************************************************************
	// Public methods

	/** If this is running */
	public boolean isRunning()
	{
		try { return thread.isAlive(); }
		catch ( NullPointerException npE ) { return false; }
	}

	/** Start this proxy. */
	public void start() throws UsbException
	{
		Thread t = new Thread(proxyRunnable);

		t.setDaemon(true);
		t.setName("LinuxDeviceProxy " + getKey());

		synchronized (startLock) {
			t.start();

			try { startLock.wait(); }
			catch ( InterruptedException iE ) { }
		}

		if (0 != startError)
			throw JavaxUsb.errorToUsbException(startError, "Could not connect to USB device");
		else
			thread = t;
	}

	/**
	 * This is used in LinuxUsbServices to compare UsbDevicesImps.
	 * @return The native device key.
	 */
	public String getKey() { return key; }

	//*************************************************************************
	// JNI methods

	/**
	 * Signal startup completed.
	 * @param error The error number if startup failed, or 0 if startup succeeded.
	 */
	private void startCompleted( int error )
	{
		synchronized (startLock) {
			startError = error;

			startLock.notifyAll();
		}
	}

	//*************************************************************************
	// Instance variables

	private Thread thread = null;
	private String key = null;

	private Runnable proxyRunnable = new Runnable() {
		public void run()
		{ JavaxUsb.nativeDeviceProxy( LinuxDeviceProxy.this ); }
	};

	private Object startLock = new Object();
	private int startError = -1;

}
