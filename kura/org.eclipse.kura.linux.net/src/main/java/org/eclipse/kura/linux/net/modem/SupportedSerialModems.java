/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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

    private static final Logger logger = LoggerFactory.getLogger(SupportedSerialModems.class);
    private static final String OS_VERSION = System.getProperty("kura.os.version");
    private static final String TARGET_NAME = System.getProperty("target.device");

    private static final String SERIAL_MODEM_INIT_WORKER_THREAD_NAME = "SerialModemInitWorker";
    private static final long THREAD_INTERVAL = 2000;

    private static boolean modemReachable = false;

    private static Future<?> task;
    private static AtomicBoolean stopThread;
    private static ExecutorService executor;

    private static ServiceTracker<EventAdmin, EventAdmin> serviceTracker;

    static {
        BundleContext bundleContext = FrameworkUtil.getBundle(SupportedSerialModems.class).getBundleContext();
        serviceTracker = new ServiceTracker<>(bundleContext, EventAdmin.class, null);
        serviceTracker.open(true);

        stopThread = new AtomicBoolean();
        stopThread.set(false);
        executor = Executors.newSingleThreadExecutor();
        task = executor.submit(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setName(SERIAL_MODEM_INIT_WORKER_THREAD_NAME);
                while (!stopThread.get()) {
                    try {
                        worker();
                        workerWait();
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        logger.debug("{} interrupted - {}", SERIAL_MODEM_INIT_WORKER_THREAD_NAME, interruptedException);
                    } catch (Throwable t) {
                        logger.error("activate() :: Exception while monitoring cellular connection ", t);
                    }
                }

                if (task != null && !task.isCancelled()) {
                    logger.info("Cancelling {} task", SERIAL_MODEM_INIT_WORKER_THREAD_NAME);
                    boolean status = task.cancel(true);
                    logger.info("Task {} cancelled - {} ", SERIAL_MODEM_INIT_WORKER_THREAD_NAME, status);
                    task = null;
                }

                if (executor != null) {
                    logger.info("Terminating {} Thread ...", SERIAL_MODEM_INIT_WORKER_THREAD_NAME);
                    executor.shutdownNow();
                }
            }
        });
    }

    public static SupportedSerialModemInfo getModem(String imageName, String imageVersion, String targetName) {
        SupportedSerialModemInfo supportedSerialModemInfo = null;

        for (SupportedSerialModemInfo modem : SupportedSerialModemInfo.values()) {
            if (modem.getOsImageName().equals(imageName) && modem.getOsImageVersion().equals(imageVersion)
                    && modem.getTargetName().equals(targetName)) {
                if (modemReachable) {
                    logger.debug("The {} modem is attached", modem.getModemName());
                    supportedSerialModemInfo = modem;
                } else {
                    // do not return this modem if it isn't reachable
                    logger.debug("The {} modem is not attached", modem.getModemName());
                }
                break;
            }
        }

        return supportedSerialModemInfo;
    }

    private static void worker() {
        SupportedSerialModemInfo modem = null;
        if (OS_VERSION != null
                && OS_VERSION.equals(
                        KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())
                && TARGET_NAME != null && TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {

            modem = SupportedSerialModemInfo.MiniGateway_Telit_HE910_NAD;

        }

        if (modem != null) {

            logger.info("Installing modem driver for {} ...", modem.getModemName());
            try {
                if (!SupportedUsbModems.isAttached(SupportedUsbModemInfo.Telit_HE910_D.getVendorId(),
                        SupportedUsbModemInfo.Telit_HE910_D.getProductId())) {
                    logger.warn("USB modem {}:{} is not detected ...",
                            SupportedUsbModemInfo.Telit_HE910_D.getVendorId(),
                            SupportedUsbModemInfo.Telit_HE910_D.getProductId());
                    if (modem.getDriver().install() == 0) {
                        for (String modemModel : modem.getModemModels()) {
                            if (modemModel.equals(modem.getDriver().getModemModel())) {
                                logger.info(
                                        "Driver for the {} modem has been installed. Modem is reachable as serial device.",
                                        modemModel);
                                EventAdmin eventAdmin = serviceTracker.getService();

                                if (eventAdmin != null) {
                                    logger.info("posting the SerialModemAddedEvent ...");
                                    eventAdmin.postEvent(new SerialModemAddedEvent(modem));
                                }
                                stopThread.set(true);
                                workerNotity();
                                modemReachable = true;
                            }
                        }
                    }
                    logger.warn("Failed to install modem driver for {}", modem.getModemName());
                } else {
                    logger.info("{} modem is reachable as a USB device ...", modem.getModemName());
                    stopThread.set(true);
                    workerNotity();
                    modemReachable = true;
                }
            } catch (Exception e) {
                logger.error("Worker exception", e);
            }

        }

    }

    private static void workerNotity() {
        if (stopThread != null) {
            synchronized (stopThread) {
                stopThread.notifyAll();
            }
        }
    }

    private static void workerWait() throws InterruptedException {
        if (stopThread != null) {
            synchronized (stopThread) {
                stopThread.wait(THREAD_INTERVAL);
            }
        }
    }
}
