/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.emulator.usb;

import java.util.List;

import javax.usb.UsbServices;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.usb.UsbBlockDevice;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsbServiceImpl implements UsbService {
	
	private static final Logger s_logger = LoggerFactory.getLogger(UsbServiceImpl.class);

	private List m_usbDevices;		//udev doesn't properly track devices (particularly for removal events) - so we have to do it.
	
	protected void activate(ComponentContext componentContext) {
		//only support Linux
		//Properties props = System.getProperties();
	}

	protected void deactivate(ComponentContext componentContext) {
		m_usbDevices = null;
	}

	@Override
	public UsbServices getUsbServices()
			throws KuraException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends UsbDevice> getUsbDevices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UsbBlockDevice> getUsbBlockDevices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UsbNetDevice> getUsbNetDevices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UsbTtyDevice> getUsbTtyDevices() {
		// TODO Auto-generated method stub
		return null;
	}

}
