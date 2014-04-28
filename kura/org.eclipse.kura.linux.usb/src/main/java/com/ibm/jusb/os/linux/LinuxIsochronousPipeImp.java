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
import javax.usb.util.*;

import com.ibm.jusb.*;
import com.ibm.jusb.os.*;

/**
 * LinuxPipeOsImp implementation for Isochronous pipe.
 * <p>
 * This must be set up before use.  See {@link com.ibm.jusb.os.linux.LinuxPipeOsImp LinuxPipeOsImp} for details.
 * @author Dan Streetman
 */
class LinuxIsochronousPipeImp extends LinuxPipeOsImp
{
	/** Constructor */
    public LinuxIsochronousPipeImp( UsbPipeImp pipe, LinuxInterfaceOsImp iface ) { super(pipe,iface); }

	/**
	 * Asynchronously submit a List of UsbIrpImps.
	 * @param list The List of UsbIrpImps.
	 * @exception UsbException If one of the UsbIrpImps is invalid.
	 */
	public void asyncSubmit(List list) throws UsbException
	{
		LinuxIsochronousRequest request = listToLinuxIsochronousRequest(list);

		getLinuxInterfaceOsImp().submit(request);

		synchronized (inProgressList) {
			inProgressList.add(request);
		}
	}

	/**
	 * Convert a List of UsbIrpImps to a LinuxIsochronousRequest.
	 * @param list The List.
	 * @return A LinuxIsochronousRequest.
	 */
	protected LinuxIsochronousRequest listToLinuxIsochronousRequest(List list)
	{
		LinuxIsochronousRequest request = new LinuxIsochronousRequest(getEndpointAddress());
		request.setUsbIrpImps(list);
		request.setCompletion(this);
		return request;
	}

}

