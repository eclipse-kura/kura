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
package org.eclipse.kura.linux.net.modem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.linux.net.util.KuraConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupportedSerialModems {

	private static final Logger s_logger = LoggerFactory.getLogger(SupportedSerialModems.class);
	private static final String OS_VERSION = System.getProperty("kura.os.version");
	private static final String TARGET_NAME = System.getProperty("target.device");
	
	private static final String SERIAL_MODEM_INIT_WORKER_THREAD_NAME = "SerialModemInitWorker";
	private final static long THREAD_INTERVAL = 2000;
	
	private static boolean modemReachable = false;
	
	private static Future<?>  s_task;
    private static AtomicBoolean s_stopThread;
    private static ExecutorService s_executor;
    
    private static ServiceTracker<EventAdmin, EventAdmin> s_serviceTracker;
	
	static {
		BundleContext bundleContext = FrameworkUtil.getBundle(SupportedSerialModems.class).getBundleContext();
		s_serviceTracker = new ServiceTracker<EventAdmin, EventAdmin>(bundleContext, EventAdmin.class, null);
		s_serviceTracker.open(true);
		
		s_stopThread = new AtomicBoolean();
		s_stopThread.set(false);
		s_executor = Executors.newSingleThreadExecutor();
		s_task = s_executor.submit(new Runnable() {
    		@Override
    		public void run() {
    			Thread.currentThread().setName(SERIAL_MODEM_INIT_WORKER_THREAD_NAME);
    			while (!s_stopThread.get()) {
	    			try {
	    				worker();
	    				workerWait();
					} catch (InterruptedException interruptedException) {
						Thread.interrupted();
						s_logger.debug("{} interrupted - {}", SERIAL_MODEM_INIT_WORKER_THREAD_NAME, interruptedException);
					} catch (Throwable t) {
						s_logger.error("activate() :: Exception while monitoring cellular connection {}", t);
					}
    			}
    			
    			if ((s_task != null) && !s_task.isCancelled()) {
    				s_logger.info("Cancelling {} task", SERIAL_MODEM_INIT_WORKER_THREAD_NAME);
    				boolean status = s_task.cancel(true);
    				s_logger.info("Task {} cancelled - {} ", SERIAL_MODEM_INIT_WORKER_THREAD_NAME, status);
    				s_task = null;
    			}
    				
    			if (s_executor != null) {
    		    	s_logger.info("Terminating {} Thread ...", SERIAL_MODEM_INIT_WORKER_THREAD_NAME);
    		    	s_executor.shutdownNow();
    		    }
    	}
    		});
	}
	
	public static SupportedSerialModemInfo getModem(String imageName, String imageVersion, String targetName) {
		SupportedSerialModemInfo supportedSerialModemInfo = null;
		
		for (SupportedSerialModemInfo modem : SupportedSerialModemInfo.values()) {
			if (modem.getOsImageName().equals(imageName) && modem.getOsImageVersion().equals(imageVersion) && modem.getTargetName().equals(targetName)) {
				if (modemReachable) {
					s_logger.debug("The {} modem is attached", modem.getModemName());
					supportedSerialModemInfo = modem;
				} else {
					// do not return this modem if it isn't reachable
					s_logger.debug("The {} modem is not attached", modem.getModemName());
				}
				break;
			}
		}
		
		return supportedSerialModemInfo;
	}
	
	private static void worker() {
		SupportedSerialModemInfo modem = null;
		if ((OS_VERSION != null && OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())) &&
				(TARGET_NAME != null && TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName()))) {
			
			modem = SupportedSerialModemInfo.MiniGateway_Telit_HE910_NAD;
			
		} else if ((OS_VERSION != null && OS_VERSION.equals(KuraConstants.Reliagate_10_11.getImageName() + "_" + KuraConstants.Reliagate_10_11.getImageVersion())) &&
				(TARGET_NAME != null && TARGET_NAME.equals(KuraConstants.Reliagate_10_11.getTargetName()))) {
			
			modem = SupportedSerialModemInfo.Reliagate_10_11_Telit_HE910_NAD;
			
		}
		
		if (modem != null) {
			
			s_logger.info("Installing modem driver for {} ...", modem.getModemName());
			try {
				if (!SupportedUsbModems.isAttached(SupportedUsbModemInfo.Telit_HE910_D.getVendorId(), SupportedUsbModemInfo.Telit_HE910_D.getProductId())) {
					s_logger.warn("USB modem {}:{} is not detected ...", SupportedUsbModemInfo.Telit_HE910_D.getVendorId(), SupportedUsbModemInfo.Telit_HE910_D.getProductId());
					if (modem.getDriver().install() == 0) {
						for (String modemModel : modem.getModemModels()) {
							if (modemModel.equals(modem.getDriver().getModemModel())) {
								s_logger.info("Driver for the {} modem has been installed. Modem is reachable as serial device." , modemModel);
								EventAdmin eventAdmin = s_serviceTracker.getService();
								
								if (eventAdmin != null) {
									s_logger.info("posting the SerialModemAddedEvent ...");
									eventAdmin.postEvent(new SerialModemAddedEvent(modem));
								}
								s_stopThread.set(true);
					        	workerNotity();
								modemReachable = true;
							}
						}
					}
					s_logger.warn("Failed to install modem driver for {}", modem.getModemName());
				} else {
					s_logger.info("{} modem is reachable as a USB device ...", modem.getModemName());
					s_stopThread.set(true);
		        	workerNotity();
					modemReachable = true;
				}
			} catch (Exception e) {
				s_logger.error("Worker exception", e);
			}
			
		}
			
	}
	
	private static void workerNotity() {
		if (s_stopThread != null) {
			synchronized (s_stopThread) {
				s_stopThread.notifyAll();
			}
		}
	}
	
	private static void workerWait() throws InterruptedException {
		if (s_stopThread != null) {
			synchronized (s_stopThread) {
				s_stopThread.wait(THREAD_INTERVAL);
			}
		}
	}
}
