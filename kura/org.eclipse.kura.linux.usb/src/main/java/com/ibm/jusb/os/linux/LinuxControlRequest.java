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

import com.ibm.jusb.*;
import com.ibm.jusb.util.*;

/**
 * LinuxRequest for use on control pipes.
 * @author Dan Streetman
 */
class LinuxControlRequest extends LinuxPipeRequest
{
	public LinuxControlRequest() { super(UsbConst.ENDPOINT_TYPE_CONTROL,(byte)0x00); }

	/** @param irp The (Control)UsbIrpImp */
	public void setUsbIrpImp( UsbIrpImp irp ) { setUsbIrpImp((UsbControlIrpImp)irp); }

	/** @param irp The UsbControlIrpImp */
	public void setUsbIrpImp( UsbControlIrpImp irp )
	{
		super.setUsbIrpImp(irp);

		bmRequestType = irp.bmRequestType();
		bRequest = irp.bRequest();
		wValue = irp.wValue();
		wIndex = irp.wIndex();
		setupPacket = irp.getSetupPacket();
	}

	/** @return The bmRequestType */
	public byte bmRequestType() { return bmRequestType; }

	/** @return The bRequest */
	public byte bRequest() { return bRequest; }

	/** @return The wValue */
	public short wValue() { return wValue; }

	/** @return The wIndex */
	public short wIndex() { return wIndex; }

	/** @return The setup packet */
	public byte[] getSetupPacket() { return setupPacket; }

	private byte bmRequestType = 0;
	private byte bRequest = 0;
	private short wValue = 0;
	private short wIndex = 0;
	private byte[] setupPacket = null;
}
