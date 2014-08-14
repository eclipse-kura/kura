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
 * Control parameters to pass to native code
 * <p>
 * This must be set up before use.  See {@link com.ibm.jusb.os.linux.LinuxPipeOsImp LinuxPipeOsImp} for details.
 * @author Dan Streetman
 */
class LinuxControlPipeImp extends LinuxPipeOsImp
{
	/** Constructor */
	public LinuxControlPipeImp( UsbPipeImp pipe, LinuxInterfaceOsImp iface ) { super(pipe,iface); }

	/**
	 * Create a LinuxPipeRequest to wrap a UsbIrpImp.
	 * @param usbIrpImp The UsbIrpImp.
	 * @return A LinuxPipeRequest for a UsbIrpImp.
	 * @exception UsbException If there is an error.
	 */
	protected LinuxPipeRequest usbIrpImpToLinuxPipeRequest(UsbIrpImp usbIrpImp) throws UsbException
	{
		try { return usbIrpImpToLinuxPipeRequest((UsbControlIrpImp)usbIrpImp); }
		catch ( ClassCastException ccE ) { throw new UsbException("Cannot submit a UsbIrp on a Control-type pipe."); }
	}

	/**
	 * Create a LinuxPipeRequest to wrap a UsbControlIrpImp.
	 * @param usbControlIrpImp The UsbControlIrpImp.
	 * @return A LinuxPipeRequest for a UsbControlIrpImp.
	 * @exception UsbException If there is an error.
	 */
	protected LinuxPipeRequest usbIrpImpToLinuxPipeRequest(UsbControlIrpImp usbControlIrpImp) throws UsbException
	{
		LinuxControlRequest request = null;

		/* FIXME - a set-config or set-interface on a non-DCP isn't possible using Linux's calls!
		 * Should this do a 'normal' set-config or set-interface call on the non-DCP?
		 * That would most likely drop the Linux kernel out of sync with the actual device config/setting.
		 * The LinuxSetConfigurationRequest and LinuxSetInterfaceRequest both use the provided
		 * mechanisms, which use the DCP.  Which isn't what's being requested here (since we're not the DCP).
		 * Note that non-setters (simple LinuxControlRequests on non-DCP pipe) are ok and what we want to do,
		 * since we're setting the endpoint address to non-0 (the DCP is ep0).
		 */
		if (usbControlIrpImp.isSetConfiguration())
			request = new LinuxSetConfigurationRequest();
		else if (usbControlIrpImp.isSetInterface())
			request = new LinuxSetInterfaceRequest();
		else
			request = new LinuxControlRequest();

		request.setEndpointAddress(getEndpointAddress());
		request.setUsbIrpImp(usbControlIrpImp);
		request.setCompletion(this);
		return request;
	}

}
