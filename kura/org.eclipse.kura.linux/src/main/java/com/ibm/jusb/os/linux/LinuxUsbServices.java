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
 * UsbServices implementation for Linux platform.
 * @author Dan Streetman
 */
public class LinuxUsbServices extends AbstractUsbServices implements UsbServices
{
	public LinuxUsbServices() throws UsbException
	{
		JavaxUsb.loadLibrary();

		topologyUpdateManager.setMaxSize(Long.MAX_VALUE);

		checkProperties();

		startTopologyListener();
	}

    //*************************************************************************
    // Public methods

    /** @return The virtual USB root hub */
    public synchronized UsbHub getRootUsbHub() throws UsbException
	{
		synchronized (topologyLock) {
			if (!firstUpdateDone) {
				try { topologyLock.wait(MAX_FIRST_UPDATE_DELAY); }
				catch ( InterruptedException iE ) { }
			}
		}

		return getRootUsbHubImp();
	}

	/** @return The minimum API version this supports. */
	public String getApiVersion() { return com.ibm.jusb.os.linux.Version.LINUX_API_VERSION; }

	/** @return The version number of this implementation. */
	public String getImpVersion() { return com.ibm.jusb.os.linux.Version.LINUX_IMP_VERSION; }

	/** @return Get a description of this UsbServices implementation. */
	public String getImpDescription() { return com.ibm.jusb.os.linux.Version.LINUX_IMP_DESCRIPTION; }

    //*************************************************************************
    // Private methods

	/** Set variables from user-specified properties */
	private void checkProperties()
	{
		Properties p = null;

		try { p = UsbHostManager.getProperties(); } catch ( Exception e ) { return; }

		try {
			if (p.containsKey(TOPOLOGY_UPDATE_DELAY_KEY))
				topologyUpdateDelay = Integer.decode(p.getProperty(TOPOLOGY_UPDATE_DELAY_KEY)).intValue();
		} catch ( Exception e ) { }

		try {
			if (p.containsKey(TOPOLOGY_UPDATE_NEW_DEVICE_DELAY_KEY))
				topologyUpdateNewDeviceDelay = Integer.decode(p.getProperty(TOPOLOGY_UPDATE_NEW_DEVICE_DELAY_KEY)).intValue();
		} catch ( Exception e ) { }

		try {
			if (p.containsKey(TOPOLOGY_UPDATE_USE_POLLING_KEY))
				topologyUpdateUsePolling = Boolean.valueOf(p.getProperty(TOPOLOGY_UPDATE_USE_POLLING_KEY)).booleanValue();
		} catch ( Exception e ) { }

		try {
			if (p.containsKey(TRACING))
				JavaxUsb.nativeSetTracing(Boolean.valueOf(p.getProperty(TRACING)).booleanValue());
		} catch ( Exception e ) { }

		/* The JNI library can't default to stderr (it isn't a portable reference) so we pre-set it here. */
		JavaxUsb.nativeSetTraceOutput(2, "");

		try {
			if (p.containsKey(TRACE_OUTPUT)) {
				int output = Integer.decode(p.getProperty(TRACE_OUTPUT)).intValue();
				String traceFilename = "";
				if ((3 == output || 4 == output)) {
					traceFilename = p.getProperty(TRACE_FILENAME);
					if ((null == traceFilename) || ("".equals(traceFilename))) {
						traceFilename = "";
						output = 2;
					}
				}
				JavaxUsb.nativeSetTraceOutput(output, traceFilename);
			}
		} catch ( Exception e ) { }

//FIXME - the names of the tracers should be more generically processed
		try {
			if (p.containsKey(TRACE_DEFAULT))
				JavaxUsb.nativeSetTraceType(Boolean.valueOf(p.getProperty(TRACE_DEFAULT)).booleanValue(), "default");
		} catch ( Exception e ) { }

		try {
			if (p.containsKey(TRACE_HOTPLUG))
				JavaxUsb.nativeSetTraceType(Boolean.valueOf(p.getProperty(TRACE_HOTPLUG)).booleanValue(), "hotplug");
		} catch ( Exception e ) { }

		try {
			if (p.containsKey(TRACE_XFER))
				JavaxUsb.nativeSetTraceType(Boolean.valueOf(p.getProperty(TRACE_XFER)).booleanValue(), "xfer");
		} catch ( Exception e ) { }

		try {
			if (p.containsKey(TRACE_URB))
				JavaxUsb.nativeSetTraceType(Boolean.valueOf(p.getProperty(TRACE_URB)).booleanValue(), "urb");
		} catch ( Exception e ) { }

		try {
			if (p.containsKey(TRACE_LEVEL))
				JavaxUsb.nativeSetTraceLevel(Integer.decode(p.getProperty(TRACE_LEVEL)).intValue());
		} catch ( Exception e ) { }
	}

	/** @return If the topology listener is listening */
	private boolean isListening()
	{
		try { return topologyListener.isAlive(); }
		catch ( NullPointerException npE ) { return false; }
	}

	/** Start Topology Change Listener Thread */
	private void startTopologyListener()
	{
		Runnable r = null;
		String threadName = null;

		if (topologyUpdateUsePolling) {
			threadName = "javax.usb Linux implementation Topology Poller";
			r = new Runnable() {
					public void run()
					{
						while (true) {
							try { Thread.sleep(topologyUpdateDelay); } catch ( InterruptedException iE ) { }
							updateTopology();
						}
					}
				};
		} else {
			threadName = "javax.usb Linux implementation Topology Listener";
			r = new Runnable() {
					public void run()
					{ topologyListenerExit(JavaxUsb.nativeTopologyListener(LinuxUsbServices.this)); }
				};
		}

		topologyListener = new Thread(r);

		topologyListener.setDaemon(true);
		topologyListener.setName(threadName);

		topologyListenerError = 0;
		topologyListener.start();
	}

	/**
	 * Called when the topology listener exits.
	 * @param error The return code of the topology listener.
	 */
	private void topologyListenerExit(int error)
	{
//FIXME - disconnet all devices

		topologyListenerError = error;

		synchronized (topologyLock) {
			topologyLock.notifyAll();
		}
	}

	/** Enqueue an update topology request */
	private void topologyChange()
	{
		try { Thread.sleep(topologyUpdateDelay); } catch ( InterruptedException iE ) { }

		Runnable r = new Runnable() {
				public void run()
				{ updateTopology(); }
			};

		topologyUpdateManager.add(r);
	}

	/**
	 * Fill the List with all devices.
	 * @param device The device to add.
	 * @param list The list to add to.
	 */
	private void fillDeviceList( UsbDeviceImp device, List list )
	{
		list.add(device);

		if (device.isUsbHub()) {
			UsbHubImp hub = (UsbHubImp)device;

//FIXME - Iterators can throw ConcurrentModificationException!
			Iterator iterator = hub.getAttachedUsbDevices().iterator();
			while (iterator.hasNext())
				fillDeviceList( (UsbDeviceImp)iterator.next(), list );
		}
	}

	/** Update the topology and fire connect/disconnect events */
	private void updateTopology()
	{
		List connectedDevices = new ArrayList();
		List disconnectedDevices = new ArrayList();

		fillDeviceList(getRootUsbHubImp(), disconnectedDevices);
		disconnectedDevices.remove(getRootUsbHubImp());

		topologyUpdateResult = JavaxUsb.nativeTopologyUpdater( this, connectedDevices, disconnectedDevices );

		for (int i=0; i<disconnectedDevices.size(); i++)
			((UsbDeviceImp)disconnectedDevices.get(i)).disconnect();

		for (int i=0; i<connectedDevices.size(); i++) {
			UsbDeviceImp device = (UsbDeviceImp)connectedDevices.get(i);
			setActiveConfigAndInterfaceSettings(device);
			device.getParentUsbPortImp().attachUsbDeviceImp(device);
		}

		for (int i=0; i<disconnectedDevices.size(); i++)
			listenerImp.usbDeviceDetached(new UsbServicesEvent(this, (UsbDevice)disconnectedDevices.get(i)));

		for (int i=0; i<connectedDevices.size(); i++) {
			/* Let's wait a bit before each new device's event, so its driver can have some time to
			 * talk to it without interruptions.
			 */
			try { Thread.sleep(topologyUpdateNewDeviceDelay); } catch ( InterruptedException iE ) { }
			listenerImp.usbDeviceAttached(new UsbServicesEvent(this, (UsbDevice)connectedDevices.get(i)));
		}

		synchronized (topologyLock) {
			firstUpdateDone = true;
			topologyLock.notifyAll();
		}
	}

	/**
	 * Check a device.
	 * <p>
	 * If the device exists, the existing device is removed from the disconnected list and returned.
	 * If the device is new, it is added to the connected list and returned.  If the new device replaces
	 * an existing device, the old device is retained in the disconnected list, and the new device is returned.
	 * @param hub The parent UsbHubImp.
	 * @param p The parent port number.
	 * @param device The UsbDeviceImp to add.
	 * @param disconnected The List of disconnected devices.
	 * @param connected The List of connected devices.
	 * @return The new UsbDeviceImp or existing UsbDeviceImp.
	 */
	private UsbDeviceImp checkUsbDeviceImp( UsbHubImp hub, int p, UsbDeviceImp device, List connected, List disconnected )
	{
		byte port = (byte)p;
		UsbPortImp usbPortImp = hub.getUsbPortImp(port);

		if (null == usbPortImp) {
			hub.resize(port);
			usbPortImp = hub.getUsbPortImp(port);
		}

		if (!usbPortImp.isUsbDeviceAttached()) {
			connected.add(device);
			device.setParentUsbPortImp(usbPortImp);
			return device;
		}

		UsbDeviceImp existingDevice = usbPortImp.getUsbDeviceImp();

		if (isUsbDevicesEqual(existingDevice, device)) {
			disconnected.remove(existingDevice);
			return existingDevice;
		} else {
			connected.add(device);
			device.setParentUsbPortImp(usbPortImp);
			return device;
		}
	}

	/**
	 * Return if the specified devices appear to be equal.
	 * <p>
	 * If either of the device's descriptors are null, this returns false.
	 * @param dev1 The first device.
	 * @param dev2 The second device.
	 * @return If the devices appear to be equal.
	 */
	protected boolean isUsbDevicesEqual(UsbDeviceImp dev1, UsbDeviceImp dev2)
	{
		try {
			UsbDeviceDescriptor desc1 = dev1.getUsbDeviceDescriptor();
			UsbDeviceDescriptor desc2 = dev2.getUsbDeviceDescriptor();

			String key1 = ((LinuxDeviceOsImp)dev1.getUsbDeviceOsImp()).getKey();
			String key2 = ((LinuxDeviceOsImp)dev2.getUsbDeviceOsImp()).getKey();

			return
				(key1.equals(key2)) &&
				(dev1.isUsbHub() == dev1.isUsbHub()) &&
				dev1.getSpeed() == dev2.getSpeed() &&
				desc1.equals(desc2);
		} catch ( NullPointerException npE ) {
//FIXME - log/trace!!
			return false;
		} catch ( ClassCastException ccE ) {
			//FIXME - log/trace!!
			return false;
		}
	}

	/**
	 * Find and set the active config and interface settings for this device.
	 * @param device The UsbDeviceImp.
	 */
	protected void setActiveConfigAndInterfaceSettings(UsbDeviceImp device)
	{
		LinuxDeviceOsImp linuxDeviceOsImp = (LinuxDeviceOsImp)device.getUsbDeviceOsImp();
		int config = JavaxUsb.nativeGetActiveConfigurationNumber(linuxDeviceOsImp);

		/* The device isn't configured. */
		if (0 == config)
			device.setActiveUsbConfigurationNumber((byte)config);

		/* There was an error getting the active configuration, can't continue */
//FIXME - log/trace
		if (0 > config)
			return;

		/* Got the active configuration number, make sure it's valid, then set it */
		if (device.containsUsbConfiguration((byte)config))
			device.setActiveUsbConfigurationNumber((byte)config);
		else if (1 == device.getUsbConfigurations().size()) /* the device told us a non-existent configuration was active, but it's only got one configuration, so we'll pick that */
			device.setActiveUsbConfigurationNumber(((UsbConfiguration)device.getUsbConfigurations().get(0)).getUsbConfigurationDescriptor().bConfigurationValue());
		else
			return; /* the device told us a non-existent configuration was active, and it has more than one to pick from, so we can't do anything. */

		Iterator interfaces = device.getActiveUsbConfiguration().getUsbInterfaces().iterator();
		while (interfaces.hasNext()) {
			UsbInterfaceImp usbInterfaceImp = (UsbInterfaceImp)interfaces.next();
			int setting = 0;
			if (1 < usbInterfaceImp.getNumSettings()) {
				byte interfaceNumber = usbInterfaceImp.getUsbInterfaceDescriptor().bInterfaceNumber();
				setting = JavaxUsb.nativeGetActiveInterfaceSettingNumber(linuxDeviceOsImp, UsbUtil.unsignedInt(interfaceNumber));
				if (0 <= setting)
					usbInterfaceImp.setActiveSettingNumber( (byte)setting );
			} else {
				/* If there is only one setting, just set it to the active setting. */
				usbInterfaceImp.setActiveSettingNumber( usbInterfaceImp.getUsbInterfaceDescriptor().bAlternateSetting() );
			}
		}
	}

    //*************************************************************************
    // Instance variables

	private RunnableManager topologyUpdateManager = new RunnableManager();

	private Thread topologyListener = null;
	private Object topologyLock = new Object();

	private boolean firstUpdateDone = false;

    private int topologyListenerError = 0;
	private int topologyUpdateResult = 0;

	protected boolean topologyUpdateUsePolling = TOPOLOGY_UPDATE_USE_POLLING;
	protected int topologyUpdateDelay = TOPOLOGY_UPDATE_DELAY;
	protected int topologyUpdateNewDeviceDelay = TOPOLOGY_UPDATE_NEW_DEVICE_DELAY;

	//*************************************************************************
	// Class constants

	public static final int MAX_FIRST_UPDATE_DELAY = 10000; /* 10 seconds */

	/* If not polling, this is the delay in ms after getting a connect/disconnect notification
	 * before checking for device updates.  If polling, this is the number of ms between polls.
	 */
	public static final int TOPOLOGY_UPDATE_DELAY = 5000; /* 5 seconds */
	public static final String TOPOLOGY_UPDATE_DELAY_KEY = "com.ibm.jusb.os.linux.LinuxUsbServices.topologyUpdateDelay";

	/* This is a delay when new devices are found, before sending the notification event that there is a new device.
	 * This delay is per-device.
	 */
	public static final int TOPOLOGY_UPDATE_NEW_DEVICE_DELAY = 500; /* 1/2 second per device */
	public static final String TOPOLOGY_UPDATE_NEW_DEVICE_DELAY_KEY = "com.ibm.jusb.os.linux.LinuxUsbServices.topologyUpdateNewDeviceDelay";

	/* Whether to use polling to wait for connect/disconnect notification */
	public static final boolean TOPOLOGY_UPDATE_USE_POLLING = true;
	public static final String TOPOLOGY_UPDATE_USE_POLLING_KEY = "com.ibm.jusb.os.linux.LinuxUsbServices.topologyUpdateUsePolling";

	/* These enables (or disables) JNI tracing of data. */
	public static final String TRACING = "com.ibm.jusb.os.linux.LinuxUsbServices.JNI.tracing";
	public static final String TRACE_OUTPUT = "com.ibm.jusb.os.linux.LinuxUsbServices.JNI.trace_output";
	public static final String TRACE_FILENAME = "com.ibm.jusb.os.linux.LinuxUsbServices.JNI.trace_filename";
	public static final String TRACE_LEVEL = "com.ibm.jusb.os.linux.LinuxUsbServices.JNI.trace_level";
	public static final String TRACE_DEFAULT = "com.ibm.jusb.os.linux.LinuxUsbServices.JNI.trace_default";
	public static final String TRACE_HOTPLUG = "com.ibm.jusb.os.linux.LinuxUsbServices.JNI.trace_hotplug";
	public static final String TRACE_XFER = "com.ibm.jusb.os.linux.LinuxUsbServices.JNI.trace_xfer";
	public static final String TRACE_URB = "com.ibm.jusb.os.linux.LinuxUsbServices.JNI.trace_urb";

}
