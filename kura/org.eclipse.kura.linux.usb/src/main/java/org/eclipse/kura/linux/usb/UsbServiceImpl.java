/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.usb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbServices;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.udev.LinuxUdevListener;
import org.eclipse.kura.usb.UsbBlockDevice;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbDeviceAddedEvent;
import org.eclipse.kura.usb.UsbDeviceRemovedEvent;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsbServiceImpl implements UsbService, LinuxUdevListener {
	
	private static final Logger s_logger = LoggerFactory.getLogger(UsbServiceImpl.class);

	private LinuxUdevNative m_linuxUdevNative;
	private EventAdmin m_eventAdmin;
	
	protected void activate(ComponentContext componentContext) {
		// only support Linux
		String osName= System.getProperty("os.name");
		String osVersion= System.getProperty("os.version");
		if(osName.equals("Linux")) {
			m_linuxUdevNative = new LinuxUdevNative(this);
		} else {
			s_logger.error("This is not Linux! - can not start the USB service.  This is " + osVersion);
			throw new ComponentException("This is not Linux! - can not start the USB service.  This is " + osVersion);
		}
	}

	protected void deactivate(ComponentContext componentContext) {
		m_linuxUdevNative.unbind();
		m_linuxUdevNative = null;
	}

	public void setEventAdmin(EventAdmin eventAdmin) {
		m_eventAdmin = eventAdmin;
	}

	public void unsetEventAdmin(EventAdmin eventAdmin) {
		m_eventAdmin = null;
	}
	
	@Override
	public UsbServices getUsbServices() throws KuraException {
		try {
			return UsbHostManager.getUsbServices( );
		} catch (SecurityException e) {
			throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION, e, (Object[]) null);
		} catch (UsbException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e, (Object[]) null);
		}
	}

	@Override
	public synchronized List<? extends UsbDevice> getUsbDevices() {
		List<UsbDevice> usbDevices = new ArrayList<UsbDevice>(); 
		usbDevices.addAll(getUsbBlockDevices());
		usbDevices.addAll(getUsbNetDevices());
		usbDevices.addAll(getUsbTtyDevices());
		
		return usbDevices;
	}

	public synchronized List<UsbBlockDevice> getUsbBlockDevices() {
		return (List<UsbBlockDevice>) LinuxUdevNative.getUsbBlockDevices();
	}

	public synchronized List<UsbNetDevice> getUsbNetDevices() {
		return (List<UsbNetDevice>) LinuxUdevNative.getUsbNetDevices();
	}
	
	public synchronized List<UsbTtyDevice> getUsbTtyDevices() {
		return (List<UsbTtyDevice>) LinuxUdevNative.getUsbTtyDevices();
	}

	@Override
	public synchronized void attached(UsbDevice device) {
		s_logger.debug("firing UsbDeviceAddedEvent for: {}", device);
		Map<String, String> map = new HashMap<String, String>();
		map.put(UsbDeviceAddedEvent.USB_EVENT_USB_PORT_PROPERTY, device.getUsbPort());
		map.put(UsbDeviceAddedEvent.USB_EVENT_VENDOR_ID_PROPERTY, device.getVendorId());
		map.put(UsbDeviceAddedEvent.USB_EVENT_PRODUCT_ID_PROPERTY, device.getProductId());
		map.put(UsbDeviceAddedEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY, device.getManufacturerName());
		map.put(UsbDeviceAddedEvent.USB_EVENT_PRODUCT_NAME_PROPERTY, device.getProductName());
		map.put(UsbDeviceAddedEvent.USB_EVENT_BUS_NUMBER_PROPERTY, device.getUsbBusNumber());
		map.put(UsbDeviceAddedEvent.USB_EVENT_DEVICE_PATH_PROPERTY, device.getUsbDevicePath());

		if(device instanceof UsbBlockDevice) {
			map.put(UsbDeviceAddedEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbBlockDevice) device).getDeviceNode());
		} else if(device instanceof UsbNetDevice) {
			map.put(UsbDeviceAddedEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbNetDevice)device).getInterfaceName());
		} else if(device instanceof UsbTtyDevice) {
			map.put(UsbDeviceAddedEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbTtyDevice)device).getDeviceNode());
		}

		m_eventAdmin.postEvent(new UsbDeviceAddedEvent(map));
	}

	@Override
	public synchronized void detached(UsbDevice device) {
		s_logger.debug("firing UsbDeviceRemovedEvent for: {}", device);
		Map<String, String> map = new HashMap<String, String>();
		map.put(UsbDeviceRemovedEvent.USB_EVENT_USB_PORT_PROPERTY, device.getUsbPort());
		map.put(UsbDeviceRemovedEvent.USB_EVENT_VENDOR_ID_PROPERTY, device.getVendorId());
		map.put(UsbDeviceRemovedEvent.USB_EVENT_PRODUCT_ID_PROPERTY, device.getProductId());
		map.put(UsbDeviceRemovedEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY, device.getManufacturerName());
		map.put(UsbDeviceRemovedEvent.USB_EVENT_PRODUCT_NAME_PROPERTY, device.getProductName());
		map.put(UsbDeviceRemovedEvent.USB_EVENT_BUS_NUMBER_PROPERTY, device.getUsbBusNumber());
		map.put(UsbDeviceRemovedEvent.USB_EVENT_DEVICE_PATH_PROPERTY, device.getUsbDevicePath());

		if(device instanceof UsbBlockDevice) {
			map.put(UsbDeviceRemovedEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbBlockDevice) device).getDeviceNode());
		} else if(device instanceof UsbNetDevice) {
			map.put(UsbDeviceRemovedEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbNetDevice)device).getInterfaceName());
		} else if(device instanceof UsbTtyDevice) {
			map.put(UsbDeviceRemovedEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbTtyDevice)device).getDeviceNode());
		}

		m_eventAdmin.postEvent(new UsbDeviceRemovedEvent(map));
	}
}
