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
package org.eclipse.kura.linux.usb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbServices;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxUdevNative;
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

	@SuppressWarnings("unused")
	private LinuxUdevNative m_linuxUdevNative;
	private EventAdmin m_eventAdmin;

	private List m_usbDevices;		//udev doesn't properly track devices (particularly for removal events) - so we have to do it.
	
	protected void activate(ComponentContext componentContext) {
		//only support Linux
		Properties props = System.getProperties();
		if(((String)props.getProperty("os.name")).equals("Linux")) {
			m_linuxUdevNative = new LinuxUdevNative(this);
			
			//initialize our list
			m_usbDevices = getUsbDevices();
		} else {
			s_logger.error("This is not Linux! - can not start the USB service.  This is " + ((String)props.getProperty("os.version")));
			throw new ComponentException("This is not Linux! - can not start the USB service.  This is " + ((String)props.getProperty("os.version")));
		}
	}

	protected void deactivate(ComponentContext componentContext) {
		m_linuxUdevNative.unbind();
		m_linuxUdevNative = null;
		m_usbDevices = null;
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
		List usbDevices = new ArrayList(); 
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
		//Udev doesn't properly track removal events - so we do it
		List newlyAttachedDevices = getNewlyAttachedDevices();
		
		if(newlyAttachedDevices != null && !newlyAttachedDevices.isEmpty()) {
			for(Object newDevice : newlyAttachedDevices) {
				s_logger.debug("firing UsbDeviceAddedEvent");
				Map<String, String> map = new HashMap<String, String>();
				map.put(UsbDeviceAddedEvent.USB_EVENT_USB_PORT_PROPERTY, ((UsbDevice)newDevice).getUsbPort());
				map.put(UsbDeviceAddedEvent.USB_EVENT_VENDOR_ID_PROPERTY, ((UsbDevice)newDevice).getVendorId());
				map.put(UsbDeviceAddedEvent.USB_EVENT_PRODUCT_ID_PROPERTY, ((UsbDevice)newDevice).getProductId());
				map.put(UsbDeviceAddedEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY, ((UsbDevice)newDevice).getManufacturerName());
				map.put(UsbDeviceAddedEvent.USB_EVENT_PRODUCT_NAME_PROPERTY, ((UsbDevice)newDevice).getProductName());
				map.put(UsbDeviceAddedEvent.USB_EVENT_BUS_NUMBER_PROPERTY, ((UsbDevice)newDevice).getUsbBusNumber());
				map.put(UsbDeviceAddedEvent.USB_EVENT_DEVICE_PATH_PROPERTY, ((UsbDevice)newDevice).getUsbDevicePath());

				if(newDevice instanceof UsbBlockDevice) {
					map.put(UsbDeviceAddedEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbBlockDevice) newDevice).getDeviceNode());
				} else if(newDevice instanceof UsbNetDevice) {
					map.put(UsbDeviceAddedEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbNetDevice)newDevice).getInterfaceName());
				} else if(newDevice instanceof UsbTtyDevice) {
					map.put(UsbDeviceAddedEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbTtyDevice)newDevice).getDeviceNode());
				}

				m_eventAdmin.postEvent(new UsbDeviceAddedEvent(map));
			}
		}
	}

	@Override
	public synchronized void detached(UsbDevice device) {
		//Udev doesn't properly track removal events - so we do it
		List newlyRemovedDevices = getNewlyRemovedDevices();
		
		if(newlyRemovedDevices != null && !newlyRemovedDevices.isEmpty()) {
			for(Object removedDevice : newlyRemovedDevices) {
				s_logger.debug("firing UsbDeviceAddedEvent");
				Map<String, String> map = new HashMap<String, String>();
				map.put(UsbDeviceAddedEvent.USB_EVENT_USB_PORT_PROPERTY, ((UsbDevice)removedDevice).getUsbPort());
				map.put(UsbDeviceAddedEvent.USB_EVENT_VENDOR_ID_PROPERTY, ((UsbDevice)removedDevice).getVendorId());
				map.put(UsbDeviceAddedEvent.USB_EVENT_PRODUCT_ID_PROPERTY, ((UsbDevice)removedDevice).getProductId());
				map.put(UsbDeviceAddedEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY, ((UsbDevice)removedDevice).getManufacturerName());
				map.put(UsbDeviceAddedEvent.USB_EVENT_PRODUCT_NAME_PROPERTY, ((UsbDevice)removedDevice).getProductName());
				map.put(UsbDeviceAddedEvent.USB_EVENT_BUS_NUMBER_PROPERTY, ((UsbDevice)removedDevice).getUsbBusNumber());
				map.put(UsbDeviceAddedEvent.USB_EVENT_DEVICE_PATH_PROPERTY, ((UsbDevice)removedDevice).getUsbDevicePath());

				if(removedDevice instanceof UsbBlockDevice) {
					map.put(UsbDeviceAddedEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbBlockDevice) removedDevice).getDeviceNode());
				} else if(removedDevice instanceof UsbNetDevice) {
					map.put(UsbDeviceAddedEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbNetDevice)removedDevice).getInterfaceName());
				} else if(removedDevice instanceof UsbTtyDevice) {
					map.put(UsbDeviceAddedEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbTtyDevice)removedDevice).getDeviceNode());
				}

				m_eventAdmin.postEvent(new UsbDeviceRemovedEvent(map));
			}
		}
	}
	
	private List getNewlyAttachedDevices() {
		List newlyAttachedDevices = new ArrayList();
		List currentDevices = getUsbDevices();
		
		if((m_usbDevices != null && !m_usbDevices.isEmpty()) && (currentDevices != null && !currentDevices.isEmpty())) {
			if(m_usbDevices.size() > currentDevices.size()) {
				s_logger.error("Got an USB attached event but current USB device size list is smaller than new list");
				m_usbDevices = currentDevices;
				return null;
			} else if(m_usbDevices.size() == currentDevices.size()) {
				s_logger.error("Got an USB attached event but current USB device size list is equal to old list");
				m_usbDevices = currentDevices;
				return null;				
			} else {
				for(Object potentialNewDevice : currentDevices) {
					boolean foundMatch = false;
					for(Object oldDevice : m_usbDevices) {
						if(potentialNewDevice.equals(oldDevice)) {
							//this is not the device you are looking for
							foundMatch = true;
							break;
						}
					}
					
					if(!foundMatch) {
						newlyAttachedDevices.add(potentialNewDevice);
					}
				}
				
				m_usbDevices = currentDevices;
				return newlyAttachedDevices;
			}
		} else if((m_usbDevices == null || m_usbDevices.isEmpty()) && (currentDevices != null && !currentDevices.isEmpty())) {
			//this is ok
			m_usbDevices = currentDevices;
			return currentDevices;
		} else if((m_usbDevices != null && !m_usbDevices.isEmpty()) && (currentDevices == null || currentDevices.isEmpty())) {
			s_logger.error("Got an USB attached event but current devices is null and the previous list of devices is not!");
			m_usbDevices = currentDevices;
			return null;
		} else {
			s_logger.error("Got an USB attached event but current devices is null and the previous list of devices is also null!");
			m_usbDevices = currentDevices;
			return null;
		}
	}
	
	private List getNewlyRemovedDevices() {
		List newlyRemovedDevices = new ArrayList();
		List currentDevices = getUsbDevices();
		
		if((m_usbDevices != null && !m_usbDevices.isEmpty()) && (currentDevices != null && !currentDevices.isEmpty())) {
			if(m_usbDevices.size() < currentDevices.size()) {
				s_logger.error("Got an USB removed event but current USB device size list is bigger than new list");
				m_usbDevices = currentDevices;
				return null;
			} else if(m_usbDevices.size() == currentDevices.size()) {
				s_logger.error("Got an USB removed event but current USB device size list is equal to old list");
				m_usbDevices = currentDevices;
				return null;				
			} else {
				for(Object potentialRemovedDevice : m_usbDevices) {
					boolean foundMatch = false;
					for(Object currentDevice : currentDevices) {
						if(potentialRemovedDevice.equals(currentDevice)) {
							//this is not the device you are looking for
							foundMatch = true;
							break;
						}
					}
					
					if(!foundMatch) {
						newlyRemovedDevices.add(potentialRemovedDevice);
					}
				}
				
				m_usbDevices = currentDevices;
				return newlyRemovedDevices;
			}
		} else if((m_usbDevices == null || m_usbDevices.isEmpty()) && (currentDevices != null && !currentDevices.isEmpty())) {
			s_logger.error("Got an USB removed event but current devices is not null and the previous list of devices is!");			
			m_usbDevices = currentDevices;
			return null;
		} else if((m_usbDevices != null && !m_usbDevices.isEmpty()) && (currentDevices == null || currentDevices.isEmpty())) {
			//this is ok
			List retList = new ArrayList();
			retList.addAll(m_usbDevices);
			m_usbDevices = null;
			return retList;
		} else {
			s_logger.error("Got an USB removed event but current devices is null and the previous list of devices is also null!");
			m_usbDevices = currentDevices;
			return null;
		}
	}
}
