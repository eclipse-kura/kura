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
package org.eclipse.kura.linux.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.kura.usb.UsbBlockDevice;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbTtyDevice;

public class LinuxUdevNative {

	private static final Logger s_logger = LoggerFactory.getLogger(LinuxUdevNative.class);
	
	private static final String LIBRARY_NAME = "EurotechLinuxUdev";
	
	 private final static long THREAD_TERMINATION_TOUT = 1; // in seconds
	
	static {
		System.loadLibrary( LIBRARY_NAME );
	}
	
	private static List<UsbBlockDevice> blockDevices;
	private static List<UsbNetDevice> netDevices;
	private static List<UsbTtyDevice> ttyDevices;

	private static boolean started;
	private static ScheduledFuture<?> s_task;
	private ScheduledThreadPoolExecutor m_executor;
	
	private LinuxUdevListener m_linuxUdevListener;
	private LinuxUdevNative m_linuxUdevNative;
	
	public LinuxUdevNative(LinuxUdevListener linuxUdevListener) {
		if(!started) {
			m_linuxUdevNative = this;
			m_linuxUdevListener = linuxUdevListener;
			
			blockDevices = (List<UsbBlockDevice>) LinuxUdevNative.getUsbDevices("block");
			netDevices = (List<UsbNetDevice>) LinuxUdevNative.getUsbDevices("net");
			ttyDevices = (List<UsbTtyDevice>) LinuxUdevNative.getUsbDevices("tty");
			
			start();
			started = true;	
		}
	}
	
	public void unbind() {
		if ((s_task != null) && (!s_task.isDone())) {
			s_logger.debug("Cancelling LinuxUdevNative task ...");
    		s_task.cancel(true);
    		s_logger.info("LinuxUdevNative task cancelled? = {}", s_task.isDone());
			s_task = null;
		}
		if (m_executor != null) {
			s_logger.debug("Terminating LinuxUdevNative Thread ...");
    		m_executor.shutdownNow();
    		try {
				m_executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				s_logger.warn("Interrupted", e);
			}
    		s_logger.info("LinuxUdevNative Thread terminated? - {}", m_executor.isTerminated());
			m_executor = null;
		}
		started = false;
	}
	
	public static List<UsbBlockDevice> getUsbBlockDevices() {
		return new ArrayList<UsbBlockDevice>(blockDevices);
	}
	
	public static List<UsbNetDevice> getUsbNetDevices() {
		return new ArrayList<UsbNetDevice>(netDevices);
	}
	
	public static List<UsbTtyDevice> getUsbTtyDevices() {
		return new ArrayList<UsbTtyDevice>(ttyDevices);
	}

	private native static void nativeHotplugThread(LinuxUdevNative linuxUdevNative);

	private native static ArrayList<? extends UsbDevice> getUsbDevices(String deviceClass);

	private void callback(String type, UsbDevice usbDevice) {
		
		s_logger.debug("TYPE: " + usbDevice.getClass().toString());
		s_logger.debug("\tmanfufacturer name: " + usbDevice.getManufacturerName());
		s_logger.debug("\tproduct name: " + usbDevice.getProductName());
		s_logger.debug("\tvendor ID: " + usbDevice.getVendorId());
		s_logger.debug("\tproduct ID: " + usbDevice.getProductId());
		s_logger.debug("\tUSB Bus Number: " + usbDevice.getUsbBusNumber());
		
		if(type.compareTo(UdevEventType.ATTACHED.name()) == 0) {			
			if(usbDevice instanceof UsbBlockDevice) {
				s_logger.debug("Adding block device: " + usbDevice.getUsbPort() + " - " + ((UsbBlockDevice) usbDevice).getDeviceNode());
				blockDevices.add((UsbBlockDevice) usbDevice);
			} else if(usbDevice instanceof UsbNetDevice) {
				s_logger.debug("Adding new device: " + usbDevice.getUsbPort() + " - " + ((UsbNetDevice) usbDevice).getInterfaceName());
				netDevices.add((UsbNetDevice) usbDevice);
			} else if(usbDevice instanceof UsbTtyDevice) {
				s_logger.debug("Adding tty device: " + usbDevice.getUsbPort() + " - " + ((UsbTtyDevice) usbDevice).getDeviceNode());
				ttyDevices.add((UsbTtyDevice) usbDevice);
			}
			
			m_linuxUdevListener.attached(usbDevice);
		} else if(type.compareTo(UdevEventType.DETACHED.name()) == 0) {
			if(usbDevice instanceof UsbBlockDevice) {
				s_logger.debug("Removing block device: " + usbDevice.getUsbPort() + " - " + ((UsbBlockDevice) usbDevice).getDeviceNode());
				if(blockDevices != null && blockDevices.size() > 0) {
					for(int i=0; i<blockDevices.size(); i++) {
						UsbBlockDevice device = blockDevices.get(i);
						if(device.getDeviceNode().equals(((UsbBlockDevice) usbDevice).getDeviceNode())) {
							blockDevices.remove(i);
							break;
						}
					}
				}
			} else if(usbDevice instanceof UsbNetDevice) {
				s_logger.debug("Removing net device: " + usbDevice.getUsbPort() + " - " + ((UsbNetDevice) usbDevice).getInterfaceName());
				if(netDevices != null && netDevices.size() > 0) {
					for(int i=0; i<netDevices.size(); i++) {
						UsbNetDevice device = netDevices.get(i);
						if(device.getInterfaceName().equals(((UsbNetDevice) usbDevice).getInterfaceName())) {
							netDevices.remove(i);
							break;
						}
					}
				}
			} else if(usbDevice instanceof UsbTtyDevice) {
				s_logger.debug("Removing tty device: " + usbDevice.getUsbPort() + " - " + ((UsbTtyDevice) usbDevice).getDeviceNode());
				if(ttyDevices != null && ttyDevices.size() > 0) {
					for(int i=0; i<ttyDevices.size(); i++) {
						UsbTtyDevice device = ttyDevices.get(i);
						if(device.getDeviceNode().equals(((UsbTtyDevice) usbDevice).getDeviceNode())) {
							ttyDevices.remove(i);
							break;
						}
					}
				}
			}
			
			m_linuxUdevListener.detached(usbDevice);
		} else {
			s_logger.debug("Unknown udev event: " + type);
		}
	}

	/** Start this Hotplug Thread. */
	private void start() {
		
		m_executor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "UdevHotplugThread");
				thread.setDaemon(true);
				return thread;
			}
			});
		
		m_executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		m_executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		s_task = m_executor.schedule(new Runnable() {
    		@Override
    		public void run() {
    				s_logger.info("Starting LinuxUdevNative Thread ...");
    				Thread.currentThread().setName("LinuxUdevNative");
    				try {
    					LinuxUdevNative.nativeHotplugThread(m_linuxUdevNative);
    				} catch(Exception e) {
    					e.printStackTrace();
    				}
    			}
    	}, 0, TimeUnit.SECONDS);
	}
}

