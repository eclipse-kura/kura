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
import javax.usb.util.*;

import com.ibm.jusb.*;
import com.ibm.jusb.os.*;

/**
 * UsbInterfaceOsImp implementation for Linux platform.
 * <p>
 * This must be set up before use.
 * <ul>
 * <li>The {@link #getUsbInterfaceImp() UsbInterfaceImp} must be set
 *     either in the constructor or by its {@link #setUsbInterfaceImp(UsbInterfaceImp) setter}.</li>
 * <li>The {@link #getLinuxDeviceOsImp() LinuxDeviceOsImp} must be set
 *     either in the constructor or by its {@link #setLinuxDeviceOsImp(LinuxDeviceOsImp) setter}.</li>
 * </ul>
 * @author Dan Streetman
 */
class LinuxInterfaceOsImp implements UsbInterfaceOsImp
{
	/** Constructor */
	public LinuxInterfaceOsImp( UsbInterfaceImp iface, LinuxDeviceOsImp device )
	{
		setUsbInterfaceImp(iface);
		setLinuxDeviceOsImp(device);
	}

	//*************************************************************************
	// Public methods

	/** @return The UsbInterfaceImp for this */
	public UsbInterfaceImp getUsbInterfaceImp() { return usbInterfaceImp; }

	/** @param iface The UsbInterfaceImp for this */
	public void setUsbInterfaceImp( UsbInterfaceImp iface )
	{
		usbInterfaceImp = iface;

		try {
			interfaceNumber = usbInterfaceImp.getUsbInterfaceDescriptor().bInterfaceNumber();
		} catch ( NullPointerException npE ) {
			/* wait 'til usbInterfaceImp is non-null */
		}
	}

	/** @return The LinuxDeviceOsImp for this */
	public LinuxDeviceOsImp getLinuxDeviceOsImp() { return linuxDeviceOsImp; }

	/** @param device The LinuxDeviceOsImp for this */
	public void setLinuxDeviceOsImp( LinuxDeviceOsImp device ) { linuxDeviceOsImp = device; }

	/** Claim this interface. */
	public void claim() throws UsbException
	{
		LinuxInterfaceRequest request = new LinuxInterfaceRequest.LinuxClaimInterfaceRequest(getInterfaceNumber());
		submit(request);

		request.waitUntilCompleted();

		if (0 != request.getError())
			throw JavaxUsb.errorToUsbException(request.getError(), "Could not claim interface");
	}

	/**
	 * Claim this interface using the specified policy.
	 * @param policy The UsbInterfacePolicy to use.
	 */
	public void claim(UsbInterfacePolicy policy) throws UsbException
	{
		boolean forceClaim = policy.forceClaim(getUsbInterfaceImp());
		LinuxInterfaceRequest request = new LinuxInterfaceRequest.LinuxClaimInterfaceRequest(getInterfaceNumber(), forceClaim);
		submit(request);

		request.waitUntilCompleted();

		if (0 != request.getError())
			throw JavaxUsb.errorToUsbException(request.getError(), "Could not claim interface");
	}

	/** Release this interface. */
	public void release() throws UsbException
	{
		LinuxInterfaceRequest request = new LinuxInterfaceRequest.LinuxReleaseInterfaceRequest(getInterfaceNumber());

		submit(request);

		request.waitUntilCompleted();

		if (0 != request.getError())
			throw new UsbNativeClaimException(JavaxUsb.errorToUsbException(request.getError()).getMessage());
	}

	/** @return if this interface is claimed. */
	public boolean isClaimed()
	{
		LinuxInterfaceRequest request = new LinuxInterfaceRequest.LinuxIsClaimedInterfaceRequest(getInterfaceNumber());

		try {
			submit(request);
		} catch ( UsbException uE ) {
//FIXME - log this
			return false;
		}

		request.waitUntilCompleted();

		if (0 != request.getError()) {
//FIXME - log
				return false;
		}

		return request.isClaimed();
	}

	public byte getInterfaceNumber() { return interfaceNumber; }

	/**
	 * Submit a Request.
	 * @param request The LinuxRequest.
	 */
	void submit(LinuxRequest request) throws UsbException { getLinuxDeviceOsImp().submit(request); }

	/**
	 * Cancel a Request.
	 * @param request The LinuxRequest.
	 */
	void cancel(LinuxRequest request) { getLinuxDeviceOsImp().cancel(request); }

	protected UsbInterfaceImp usbInterfaceImp = null;
	protected LinuxDeviceOsImp linuxDeviceOsImp = null;

	private byte interfaceNumber = 0;
}
