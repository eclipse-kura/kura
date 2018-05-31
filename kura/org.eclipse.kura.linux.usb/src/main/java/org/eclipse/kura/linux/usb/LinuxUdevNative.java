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

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.linux.udev.LinuxUdevListener;
import org.eclipse.kura.linux.udev.UdevEventType;
import org.eclipse.kura.usb.UsbBlockDevice;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class LinuxUdevNative {

    private static final Logger logger = LoggerFactory.getLogger(LinuxUdevNative.class);

    private static final String LIBRARY_NAME = "EurotechLinuxUdev";
    private static final long THREAD_TERMINATION_TOUT = 1; // in seconds
    private static final String UNABLE_TO_LOAD_ERROR = "Unable to load: ";
    private static final String UNKNOWN_UDEV_EVENT = "Unknown udev event: {}";

    static {
        try {
            AccessController.doPrivileged((PrivilegedAction) LinuxUdevNative::loadUdevLibrary);
        } catch (Exception e) {
            logger.error(UNABLE_TO_LOAD_ERROR + LIBRARY_NAME);
        }
    }

    private static Object loadUdevLibrary() {
        try {
            System.loadLibrary(LIBRARY_NAME);
        } catch (Exception e) {
            logger.error(UNABLE_TO_LOAD_ERROR + LIBRARY_NAME);
        }
        return null;
    }

    private boolean started;
    private Future<?> task;
    private ScheduledExecutorService executor;

    private LinuxUdevListener linuxUdevListener;
    private LinuxUdevNative linuxUdevNativeInstance;

    /*
     * Devices by their device node (e.g. ttyACM3 or sdb1) or interface name (e.g. usb1).
     */
    private static HashMap<String, UsbBlockDevice> blockDevices = new HashMap<>();
    private static HashMap<String, UsbNetDevice> netDevices = new HashMap<>();
    private static HashMap<String, UsbTtyDevice> ttyDevices = new HashMap<>();

    public LinuxUdevNative(LinuxUdevListener linuxUdevListener) throws IOException {
        if (!this.started) {
            this.linuxUdevNativeInstance = this;
            this.linuxUdevListener = linuxUdevListener;

            /* Assume we get some "good" devices here */
            List<UsbBlockDevice> usbBlockDevices = (List<UsbBlockDevice>) LinuxUdevNative.getUsbDevices("block");
            if (usbBlockDevices != null) {
                for (UsbBlockDevice blockDevice : usbBlockDevices) {
                    blockDevices.put(blockDevice.getDeviceNode(), blockDevice);
                }
            }

            List<UsbNetDevice> usbNetDevices = (List<UsbNetDevice>) LinuxUdevNative.getUsbDevices("net");
            if (usbNetDevices != null) {
                for (UsbNetDevice netDevice : usbNetDevices) {
                    netDevices.put(netDevice.getInterfaceName(), netDevice);
                }
            }

            List<UsbTtyDevice> usbTtyDevices = (List<UsbTtyDevice>) LinuxUdevNative.getUsbDevices("tty");
            if (usbTtyDevices != null) {
                for (UsbTtyDevice ttyDevice : usbTtyDevices) {
                    ttyDevices.put(ttyDevice.getDeviceNode(), ttyDevice);
                }
            }

            start();
            this.started = true;
        }
    }

    public void unbind() {
        if (this.task != null && !this.task.isDone()) {
            logger.debug("Cancelling LinuxUdevNative task ...");
            this.task.cancel(true);
            logger.info("LinuxUdevNative task cancelled? = {}", this.task.isDone());
            this.task = null;
        }
        if (this.executor != null) {
            logger.debug("Terminating LinuxUdevNative Thread ...");
            this.executor.shutdownNow();
            try {
                this.executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted", e);
            }
            logger.info("LinuxUdevNative Thread terminated? - {}", this.executor.isTerminated());
            this.executor = null;
        }
        this.started = false;
    }

    public static List<UsbBlockDevice> getUsbBlockDevices() {
        return new ArrayList<>(blockDevices.values());
    }

    public static List<UsbNetDevice> getUsbNetDevices() {
        return new ArrayList<>(netDevices.values());
    }

    public static List<UsbTtyDevice> getUsbTtyDevices() {
        return new ArrayList<>(ttyDevices.values());
    }

    private static native void nativeHotplugThread(LinuxUdevNative linuxUdevNative) throws IOException;

    private static native ArrayList<? extends UsbDevice> getUsbDevices(String deviceClass) throws IOException;

    /*
     * WARNING
     *
     * The callback does not fire for devices open by a process
     * when the device is unplugged from the USB.
     * Note that `udevadm monitor' correctly reports removal of these devices so there
     * must be something wrong with our native code using libudev.
     *
     * On top of that information for detached (UdevEventType.DETACHED) devices is completely unreliable:
     * missing manufacturer and product names, wrong VID&PID (often the one of the
     * USB hub the device was attached to), wrong USB path (often the path of the USB hub
     * the device was attached to).
     * The only reliable information seems to be the device node name, e.g. ttyACM0.
     *
     * Information for devices being attached (UdevEventType.ATTACHED) seems to work more reliably.
     * The callback might still fire with wrong/incomplete information but EVENTUALLY
     * we get all the devices with the right information.
     *
     */
    private void callback(String type, UsbDevice usbDevice) {

        logger.debug("TYPE: {}", usbDevice.getClass());
        logger.debug("\tmanfufacturer name: {}", usbDevice.getManufacturerName());
        logger.debug("\tproduct name: {}", usbDevice.getProductName());
        logger.debug("\tvendor ID: {}", usbDevice.getVendorId());
        logger.debug("\tproduct ID: {}", usbDevice.getProductId());
        logger.debug("\tUSB Bus Number: {}", usbDevice.getUsbBusNumber());

        if (usbDevice instanceof UsbBlockDevice) {
            manageUsbBlockDevice(type, usbDevice);
        } else if (usbDevice instanceof UsbNetDevice) {
            manageUsbNetDevice(type, usbDevice);
        } else if (usbDevice instanceof UsbTtyDevice) {
            manageUsbTtyDevice(type, usbDevice);
        }
    }

    private void manageUsbTtyDevice(String type, UsbDevice usbDevice) {
        String name = ((UsbTtyDevice) usbDevice).getDeviceNode();
        if (name != null) {
            if (type.compareTo(UdevEventType.ATTACHED.name()) == 0) {
                UsbTtyDevice ttyDevice = (UsbTtyDevice) usbDevice;
                ttyDevices.put(name, ttyDevice);
                this.linuxUdevListener.attached(ttyDevice);
            } else if (type.compareTo(UdevEventType.DETACHED.name()) == 0) {
                UsbTtyDevice removedDevice = ttyDevices.remove(name);
                if (removedDevice != null) {
                    this.linuxUdevListener.detached(removedDevice);
                }
            } else {
                logger.debug(UNKNOWN_UDEV_EVENT, type);
            }
        }
    }

    private void manageUsbNetDevice(String type, UsbDevice usbDevice) {
        String name = ((UsbNetDevice) usbDevice).getInterfaceName();
        if (name != null) {
            if (type.compareTo(UdevEventType.ATTACHED.name()) == 0) {
                netDevices.put(name, (UsbNetDevice) usbDevice);
                this.linuxUdevListener.attached(usbDevice);
            } else if (type.compareTo(UdevEventType.DETACHED.name()) == 0) {
                UsbNetDevice removedDevice = netDevices.remove(name);
                if (removedDevice != null) {
                    this.linuxUdevListener.detached(removedDevice);
                }
            } else {
                logger.debug(UNKNOWN_UDEV_EVENT, type);
            }
        }
    }

    private void manageUsbBlockDevice(String type, UsbDevice usbDevice) {
        String name = ((UsbBlockDevice) usbDevice).getDeviceNode();
        if (name != null) {
            if (type.compareTo(UdevEventType.ATTACHED.name()) == 0) {
                /*
                 * FIXME: does an already existing device, with the same name,
                 * need to be removed first?
                 */
                blockDevices.put(name, (UsbBlockDevice) usbDevice);
                this.linuxUdevListener.attached(usbDevice);
            } else if (type.compareTo(UdevEventType.DETACHED.name()) == 0) {
                /*
                 * Due to the above limitations,
                 * the best we can do is to remove the device from the
                 * map of already known devices by its name.
                 */
                UsbBlockDevice removedDevice = blockDevices.remove(name);
                if (removedDevice != null) {
                    this.linuxUdevListener.detached(removedDevice);
                }
            } else {
                logger.debug(UNKNOWN_UDEV_EVENT, type);
            }
        }
    }

    /** Start this Hotplug Thread. */
    private void start() {

        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "UdevHotplugThread");
            thread.setDaemon(true);
            return thread;
        });

        this.task = this.executor.submit(() -> {
            logger.info("Starting LinuxUdevNative Thread ...");
            Thread.currentThread().setName("LinuxUdevNative");
            try {
                LinuxUdevNative.nativeHotplugThread(LinuxUdevNative.this.linuxUdevNativeInstance);
            } catch (Exception e) {
                logger.error("Starting LinuxUdevNative failed", e);
            }
        });
    }
}
