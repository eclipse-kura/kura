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

/*
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
 * UsbDeviceOsImp implemenation for Linux platform.
 * <p>
 * This must be set up before use.
 * <ul>
 * <li>The {@link #getUsbDeviceImp() UsbDeviceImp} must be set
 *     either in the constructor or by its {@link #setUsbDeviceImp(UsbDeviceImp) setter}.</li>
 * <li>The {@link #getLinuxDeviceProxy() LinuxDeviceProxy} must be set
 *     either in the constructor or by its {@link #setLinuxDeviceProxy(LinuxDeviceProxy) setter}.</li>
 * </ul>
 * @author Dan Streetman
 */
class LinuxDeviceOsImp extends DefaultUsbDeviceOsImp implements UsbDeviceOsImp
{
	/** Constructor */
	public LinuxDeviceOsImp( UsbDeviceImp device, LinuxDeviceProxy proxy )
	{
		setUsbDeviceImp(device);
		setLinuxDeviceProxy(proxy);
	}

	/** @return The UsbDeviceImp for this */
	public UsbDeviceImp getUsbDeviceImp() { return usbDeviceImp; }

	/** @param device The UsbDeviceImp for this */
	public void setUsbDeviceImp( UsbDeviceImp device ) { usbDeviceImp = device; }

	/**
	 * Get the LinuxDeviceProxy.
	 * <p>
	 * This will start up the LinuxDeviceProxy if not running.
	 * @return The LinuxDeviceProxy.
	 * @exception UsbException If an UsbException occurred while starting the LinuxDeviceProxy.
	 */
	public LinuxDeviceProxy getLinuxDeviceProxy() throws UsbException
	{
		synchronized(linuxDeviceProxy) {
			if (!linuxDeviceProxy.isRunning()) {
 				linuxDeviceProxy.start();
			}
		}

		return linuxDeviceProxy;
	}

	/** @param proxy The LinuxDeviceProxy */
	public void setLinuxDeviceProxy(LinuxDeviceProxy proxy)
	{
		linuxDeviceProxy = proxy;
		key = linuxDeviceProxy.getKey();
	}

	/** AsyncSubmit a UsbControlIrpImp */
	public void asyncSubmit( UsbControlIrpImp usbControlIrpImp ) throws UsbException
	{
		LinuxControlRequest request = null;

		try {
			checkUnclaimedInterface(usbControlIrpImp);
		} catch ( UsbPlatformException upE ) {
			usbControlIrpImp.setUsbException(upE);
			usbControlIrpImp.complete();
			throw upE;
		}

		if (usbControlIrpImp.isSetConfiguration())
			request = new LinuxSetConfigurationRequest();
		else if (usbControlIrpImp.isSetInterface())
			request = new LinuxSetInterfaceRequest();
		else
			request = new LinuxControlRequest();

		request.setUsbIrpImp(usbControlIrpImp);

		submit(request);
	}

	/**
	 * This is used by LinuxUsbServices to compare devices.
	 * @return The key.
	 */
	public String getKey() { return key; }

	/** Submit a Request. */
	void submit(LinuxRequest request) throws UsbException { getLinuxDeviceProxy().submit(request); }

	/** Cancel a Request. */
	void cancel(LinuxRequest request)
	{
		/* Ignore proxy-starting exception, it should already be started */
		try { getLinuxDeviceProxy().cancel(request); }
		catch ( UsbException uE ) { }
	}

	/**
	 * If this is a request to an unclaimed/invalid interface, throw an exception.
	 * @param irp The UsbControlIrpImp.
	 * @exception UsbPlatformException If the recipient interface is unclaimed or the interface/endpoint number is invalid.
	 */
	protected void checkUnclaimedInterface(UsbControlIrpImp irp) throws UsbPlatformException
	{
		/* Ignore "recipient" of Vendor requests */
		if (UsbConst.REQUESTTYPE_TYPE_VENDOR == (byte)(UsbConst.REQUESTTYPE_TYPE_MASK & irp.bmRequestType()))
			return;

		if (UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE == (byte)(UsbConst.REQUESTTYPE_RECIPIENT_MASK & irp.bmRequestType())) {
			byte ifacenum = (byte)irp.wIndex();
			UsbInterfaceImp iface = interfaceNumberToUsbInterfaceImp(ifacenum);
			String iStr = "0x" + UsbUtil.toHexString(ifacenum);
			if (null == iface) {
				IllegalArgumentException iaE = new IllegalArgumentException("No active interface with number " + iStr);
				throw new UsbPlatformException("Request with recipient interface " + iStr + ", but no such interface in current active configuration", iaE);
			} else if (!iface.isJavaClaimed()) {
				UsbNotClaimedException uncE = new UsbNotClaimedException("Interface " + iStr + " is not claimed");
				throw new UsbPlatformException("Request with recipient interface " + iStr + ", but interface is not claimed", uncE);
			}
		}

		if (UsbConst.REQUESTTYPE_RECIPIENT_ENDPOINT == (byte)(UsbConst.REQUESTTYPE_RECIPIENT_MASK & irp.bmRequestType())) {
			byte epNum = (byte)irp.wIndex();
			if (0 == epNum)
				return;
			UsbInterfaceImp iface = endpointAddressToUsbInterfaceImp(epNum);
			String eStr = "0x" + UsbUtil.toHexString(epNum);
			if (null == iface) {
				IllegalArgumentException iaE = new IllegalArgumentException("No active enpoint with address " + eStr);
				throw new UsbPlatformException("Request with recipient endpoint " + eStr + ", but no such endpoint in current active configuration and interfaces", iaE);
			} else if (!iface.isJavaClaimed()) {
				String iStr = "0x" + UsbUtil.toHexString(iface.getUsbInterfaceDescriptor().bInterfaceNumber());
				UsbNotClaimedException uncE = new UsbNotClaimedException("Interface " + iStr + ", which owns endpoint " + eStr + ", is not claimed");
				throw new UsbPlatformException("Request with recipient endpoint " + eStr + " which belongs to interface " + iStr + ", but interface is not claimed", uncE);
			}
		}
	}

	protected UsbInterfaceImp interfaceNumberToUsbInterfaceImp(byte num)
	{
		return getUsbDeviceImp().getActiveUsbConfigurationImp().getUsbInterfaceImp(num);
	}

	protected UsbInterfaceImp endpointAddressToUsbInterfaceImp(byte addr)
	{
		List ifaces = getUsbDeviceImp().getActiveUsbConfigurationImp().getUsbInterfaces();
		for (int i=0; i<ifaces.size(); i++) {
			UsbInterfaceImp iface = (UsbInterfaceImp)ifaces.get(i);
			List eps = iface.getUsbEndpoints();
			for (int e=0; e<eps.size(); e++) {
				UsbEndpointImp ep = (UsbEndpointImp)eps.get(e);
				if (ep.getUsbEndpointDescriptor().bEndpointAddress() == addr)
					return iface;
			}
		}
		return null;
	}

	private UsbDeviceImp usbDeviceImp = null;

	private LinuxDeviceProxy linuxDeviceProxy = null;
	private String key = "";

}
