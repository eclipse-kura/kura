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
 * LinuxRequest for use on pipes.
 * @author Dan Streetman
 */
class LinuxPipeRequest extends LinuxRequest
{
	/** Constructor. */
	public LinuxPipeRequest(byte type, byte addr)
	{
		setPipeType(type);
		setEndpointAddress(addr);
	}

	/** @return This request's type. */
	public int getType() { return LinuxRequest.LINUX_PIPE_REQUEST; }

	/** @return The direction of this request */
	public byte getDirection() { return (byte)(UsbConst.ENDPOINT_DIRECTION_MASK & getEndpointAddress()); }

	/** @return this request's data buffer */
	public byte[] getData() { return data; }

	/** @return The offset of the data */
	public int getOffset() { return offset; }

	/** @return The length of the data */
	public int getLength() { return length; }

	/** @return if Short Packet Detection should be enabled */
	public boolean getAcceptShortPacket() { return shortPacket; }

	/** @param len The actual length. */
	public void setActualLength(int len) { actualLength = len; }

	/** @return the assocaited UsbIrpImp */
	public UsbIrpImp getUsbIrpImp() { return usbIrpImp; }

	/** @param irp the assocaited UsbIrpImp */
	public void setUsbIrpImp( UsbIrpImp irp )
	{
		usbIrpImp = irp;
		data = irp.getData();
		offset = irp.getOffset();
		length = irp.getLength();
		shortPacket = irp.getAcceptShortPacket();
	}

	/** @return the address of the assocaited URB */
	public long getUrbAddress() { return urbAddress; }

	/** @param address the address of the assocaited URB */
	public void setUrbAddress( long address ) { urbAddress = address; }

	/** @param c If this is completed or not */
	public void setCompleted(boolean c)
	{
		if (c)
			completeUsbIrp();

		super.setCompleted(c);
	}

	/** Complete the UsbIrp */
	public void completeUsbIrp()
	{
//FIXME - do this here?  in other Thread?  Also, handle errors better.
		if (0 != getError())
			getUsbIrpImp().setUsbException( JavaxUsb.errorToUsbException(getError(), "Error submitting IRP") );
		getUsbIrpImp().setActualLength(actualLength);
		getUsbIrpImp().complete();
	}


	/** @param type The pipe type. */
	public void setPipeType(byte type)
	{
		switch (type) {
		case UsbConst.ENDPOINT_TYPE_CONTROL: pipeType = PIPE_CONTROL; break;
		case UsbConst.ENDPOINT_TYPE_BULK: pipeType = PIPE_BULK; break;
		case UsbConst.ENDPOINT_TYPE_INTERRUPT: pipeType = PIPE_INTERRUPT; break;
		case UsbConst.ENDPOINT_TYPE_ISOCHRONOUS: pipeType = PIPE_ISOCHRONOUS; break;
		default: /* log */
		}
	}

	/** @param addr The endpoint address */
	public void setEndpointAddress(byte addr) { epAddress = addr; }

	/** @return The type of pipe */
	public int getPipeType() { return pipeType; }

	/** @return the endpoint address */
	public byte getEndpointAddress() { return epAddress; }

	private UsbIrpImp usbIrpImp = null;
	private int pipeType = 0;
	private byte epAddress = 0;

	private byte[] data = null;
	private int offset = 0;
	private int length = 0;
	private int actualLength = 0;
	private boolean shortPacket = true;

	private LinuxPipeOsImp linuxPipeImp = null;

	private long urbAddress = 0;

	/* These MUST match those defined in jni/linux/JavaxUsbRequest.c */
	private static final int PIPE_CONTROL = 1;
	private static final int PIPE_BULK = 2;
	private static final int PIPE_INTERRUPT = 3;
	private static final int PIPE_ISOCHRONOUS = 4;
}
