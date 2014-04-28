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

import java.util.*;

import javax.usb.*;
import javax.usb.event.*;
import javax.usb.util.*;

import com.ibm.jusb.*;
import com.ibm.jusb.os.*;
import com.ibm.jusb.util.*;

/**
 * UsbPipeOsImp implementation for Linux platform.
 * <p>
 * This must be set up before use.
 * <ul>
 * <li>The {@link #getUsbPipeImp() UsbPipeImp} must be set
 *     either in the constructor or by its {@link #setUsbPipeImp(UsbPipeImp) setter}.</li>
 * <li>The {@link #getLinuxInterfaceOsImp() LinuxInterfaceOsImp} must be set
 *     either in the constructor or by its {@link #setLinuxInterfaceOsImp(LinuxInterfaceOsImp) setter}.</li>
 * </ul>
 * @author Dan Streetman
 */
public class LinuxPipeOsImp extends DefaultUsbPipeOsImp implements UsbPipeOsImp,LinuxRequest.Completion
{
	/** Constructor */
	public LinuxPipeOsImp( UsbPipeImp pipe, LinuxInterfaceOsImp iface )
	{
		setUsbPipeImp(pipe);
		setLinuxInterfaceOsImp(iface);
	}

	/** @return The UsbPipeImp for this */
	public UsbPipeImp getUsbPipeImp() { return usbPipeImp; }

	/** @param pipe The UsbPipeImp for this */
	public void setUsbPipeImp( UsbPipeImp pipe ) { usbPipeImp = pipe; }

	/** @return The LinuxInterfaceOsImp */
	public LinuxInterfaceOsImp getLinuxInterfaceOsImp() { return linuxInterfaceOsImp; }

	/** @param iface The LinuxInterfaceOsImp */
	public void setLinuxInterfaceOsImp(LinuxInterfaceOsImp iface) { linuxInterfaceOsImp = iface; }

	/**
	 * Asynchronous submission using a UsbIrpImp.
	 * @param irp the UsbIrpImp to use for this submission
	 * @exception javax.usb.UsbException if error occurs
	 */
	public void asyncSubmit( UsbIrpImp irp ) throws UsbException
	{
		LinuxPipeRequest request = usbIrpImpToLinuxPipeRequest(irp);

		getLinuxInterfaceOsImp().submit(request);

		synchronized(inProgressList) {
			inProgressList.add(request);
		}
	}

	/**
	 * Stop all submissions in progress
	 */
	public void abortAllSubmissions()
	{
		Object[] requests = null;

		synchronized(inProgressList) {
			requests = inProgressList.toArray();
			inProgressList.clear();
		}

		for (int i=0; i<requests.length; i++)
			getLinuxInterfaceOsImp().cancel((LinuxPipeRequest)requests[i]);

		for (int i=0; i<requests.length; i++) {
			LinuxPipeRequest lpR = (LinuxPipeRequest)requests[i];
			lpR.waitUntilCompleted(ABORT_COMPLETION_TIMEOUT);
			if (!lpR.isCompleted()) {
				lpR.setError(ABORT_TIMEOUT_ERROR);
				lpR.setCompleted(true);
			}
		}
	}

	/** @param request The LinuxRequest that completed. */
	public void linuxRequestComplete(LinuxRequest request)
	{
		synchronized (inProgressList) {
			inProgressList.remove(request);
		}
	}

	/**
	 * Create a LinuxPipeRequest to wrap a UsbIrpImp.
	 * @param usbIrpImp The UsbIrpImp.
	 * @return A LinuxPipeRequest for a UsbIrpImp.
	 * @exception If there is an error while converting.
	 */
	protected LinuxPipeRequest usbIrpImpToLinuxPipeRequest(UsbIrpImp usbIrpImp) throws UsbException
	{
		LinuxPipeRequest request = new LinuxPipeRequest(getPipeType(),getEndpointAddress());
		request.setUsbIrpImp(usbIrpImp);
		request.setCompletion(this);
		return request;
	}

	/** @return The endpoint address */
	protected byte getEndpointAddress()
	{
		if (0 == endpointAddress)
			endpointAddress = usbPipeImp.getUsbEndpointImp().getUsbEndpointDescriptor().bEndpointAddress();

		return endpointAddress;
	}

	/** @return The pipe type */
	protected byte getPipeType()
	{
		if (0 == pipeType)
			pipeType = usbPipeImp.getUsbEndpointImp().getType();

		return pipeType;
	}

	private UsbPipeImp usbPipeImp = null;
	private LinuxInterfaceOsImp linuxInterfaceOsImp = null;

	protected byte pipeType = 0;
	protected byte endpointAddress = 0;

	protected List inProgressList = new LinkedList();

	protected static final int ABORT_TIMEOUT_ERROR = -2; /* -ENOENT in UNIX /usr/include/asm/errno.h */
	protected static final long ABORT_COMPLETION_TIMEOUT = 500; /* half second */
}
