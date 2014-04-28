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

import com.ibm.jusb.*;
import com.ibm.jusb.util.*;

/**
 * Special request for use on Isochronous pipes.
 * @author Dan Streetman
 */
class LinuxIsochronousRequest extends LinuxPipeRequest
{
	/** Constructor */
	public LinuxIsochronousRequest(byte addr) { super(UsbConst.ENDPOINT_TYPE_ISOCHRONOUS,addr); }

	/** @return This request's type. */
	public int getType() { return LinuxRequest.LINUX_ISOCHRONOUS_REQUEST; }

	/**
	 * Get the data from the specified UsbIrpImp.
	 * @param index The index of the UsbIrpImp.
	 * @return The data from the specified UsbIrpImp.
	 */
	public byte[] getData( int index ) { return getUsbIrpImp(index).getData(); }

	/**
	 * Get the offset from the specified UsbIrpImp.
	 * @param index The index of the UsbIrpImp.
	 * @return The offset from the specified UsbIrpImp.
	 */
	public int getOffset( int index ) { return getUsbIrpImp(index).getOffset(); }

	/**
	 * Get the length from the specified UsbIrpImp.
	 * @param index The index of the UsbIrpImp.
	 * @return The length from the specified UsbIrpImp.
	 */
	public int getLength( int index ) { return getUsbIrpImp(index).getLength(); }

	/**
	 * Set the actual length of the specified UsbIrpImp.
	 * @param index The index of the UsbIrpImp.
	 * @param length The actual length of the specified UsbIrpImp.
	 */
	public void setActualLength( int index, int length ) { getUsbIrpImp(index).setActualLength(length); }

	/**
	 * Set the error of the specified UsbIrpImp.
	 * @param index The index of the UsbIrpImp.
	 * @param error The error of the specified UsbIrpImp.
	 */
	public void setError( int index, int error ) { getUsbIrpImp(index).setUsbException(JavaxUsb.errorToUsbException(error)); }

	/**
	 * Get the number of UsbIrpImps.
	 * @return The number of UsbIrpImps.
	 */
	public int size() { return usbIrpImps.size(); }

	/**
	 * Get the aggregated length of all UsbIrpImps.
	 * @return The length of all UsbIrpImps.
	 */
	public int getTotalLength()
	{
		int totalLength = 0;

		for (int i=0; i<size(); i++)
			totalLength += getUsbIrpImp(i).getLength();

		return totalLength;
	}

	/**
	 * Get the specified UsbIrpImp.
	 * @param index The index of the UsbIrpImp.
	 * @return The specified UsbIrpImp.
	 */
	public UsbIrpImp getUsbIrpImp( int index ) { return (UsbIrpImp)usbIrpImps.get(index); }

	/**
	 * Set the List of UsbIrpImps.
	 * @param list The List of UsbIrpImps.
	 */
	public void setUsbIrpImps( List list ) { usbIrpImps = list; }

	/**
	 * Complete all the UsbIrps.
	 */
	public void completeUsbIrp()
	{
		for (int i=0; i<size(); i++)
			getUsbIrpImp(i).complete();
	}

	private List usbIrpImps = null;

}
