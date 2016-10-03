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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
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

    private static final Logger s_logger = LoggerFactory.getLogger(LinuxUdevNative.class);

    private static final String LIBRARY_NAME = "EurotechLinuxUdev";

    private final static long THREAD_TERMINATION_TOUT = 1; // in seconds

    static {
        try {
            AccessController.doPrivileged(new PrivilegedAction() {

                @Override
                public Object run() {
                    try {
                        // privileged code goes here, for example:
                        System.loadLibrary(LIBRARY_NAME);
                        return null; // nothing to return
                    } catch (Exception e) {
                        System.out.println("Unable to load: " + LIBRARY_NAME);
                        return null;
                    }
                }
            });
        } catch (Exception e) {
            System.out.println("Unable to load: " + LIBRARY_NAME);
        }
    }

    private static boolean started;
    private static Future<?> s_task;
    private ScheduledExecutorService m_executor;

    private LinuxUdevListener m_linuxUdevListener;
    private LinuxUdevNative m_linuxUdevNative;

    /*
     * Devices by their device node (e.g. ttyACM3 or sdb1) or interface name (e.g. usb1).
     */
    private static HashMap<String, UsbBlockDevice> m_blockDevices;
    private static HashMap<String, UsbNetDevice> m_netDevices;
    private static HashMap<String, UsbTtyDevice> m_ttyDevices;

    public LinuxUdevNative(LinuxUdevListener linuxUdevListener) {
        if (!started) {
            this.m_linuxUdevNative = this;
            this.m_linuxUdevListener = linuxUdevListener;

            m_blockDevices = new HashMap<String, UsbBlockDevice>();
            m_netDevices = new HashMap<String, UsbNetDevice>();
            m_ttyDevices = new HashMap<String, UsbTtyDevice>();

            /* Assume we get some "good" devices here */
            List<UsbBlockDevice> blockDevices = (List<UsbBlockDevice>) LinuxUdevNative.getUsbDevices("block");
            for (UsbBlockDevice blockDevice : blockDevices) {
                m_blockDevices.put(blockDevice.getDeviceNode(), blockDevice);
            }

            List<UsbNetDevice> netDevices = (List<UsbNetDevice>) LinuxUdevNative.getUsbDevices("net");
            for (UsbNetDevice netDevice : netDevices) {
                m_netDevices.put(netDevice.getInterfaceName(), netDevice);
            }

            List<UsbTtyDevice> ttyDevices = (List<UsbTtyDevice>) LinuxUdevNative.getUsbDevices("tty");
            for (UsbTtyDevice ttyDevice : ttyDevices) {
                m_ttyDevices.put(ttyDevice.getDeviceNode(), ttyDevice);
            }

            start();
            started = true;
        }
    }

    public void unbind() {
        if (s_task != null && !s_task.isDone()) {
            s_logger.debug("Cancelling LinuxUdevNative task ...");
            s_task.cancel(true);
            s_logger.info("LinuxUdevNative task cancelled? = {}", s_task.isDone());
            s_task = null;
        }
        if (this.m_executor != null) {
            s_logger.debug("Terminating LinuxUdevNative Thread ...");
            this.m_executor.shutdownNow();
            try {
                this.m_executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                s_logger.warn("Interrupted", e);
            }
            s_logger.info("LinuxUdevNative Thread terminated? - {}", this.m_executor.isTerminated());
            this.m_executor = null;
        }
        started = false;
    }

    public static List<UsbBlockDevice> getUsbBlockDevices() {
        return new ArrayList<UsbBlockDevice>(m_blockDevices.values());
    }

    public static List<UsbNetDevice> getUsbNetDevices() {
        return new ArrayList<UsbNetDevice>(m_netDevices.values());
    }

    public static List<UsbTtyDevice> getUsbTtyDevices() {
        return new ArrayList<UsbTtyDevice>(m_ttyDevices.values());
    }

    private native static void nativeHotplugThread(LinuxUdevNative linuxUdevNative);

    private native static ArrayList<? extends UsbDevice> getUsbDevices(String deviceClass);

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

        s_logger.debug("TYPE: {}", usbDevice.getClass());
        s_logger.debug("\tmanfufacturer name: {}", usbDevice.getManufacturerName());
        s_logger.debug("\tproduct name: {}", usbDevice.getProductName());
        s_logger.debug("\tvendor ID: {}", usbDevice.getVendorId());
        s_logger.debug("\tproduct ID: {}", usbDevice.getProductId());
        s_logger.debug("\tUSB Bus Number: {}", usbDevice.getUsbBusNumber());

        if (usbDevice instanceof UsbBlockDevice) {
            String name = ((UsbBlockDevice) usbDevice).getDeviceNode();
            if (name != null) {
                if (type.compareTo(UdevEventType.ATTACHED.name()) == 0) {
                    /*
                     * FIXME: does an already existing device, with the same name,
                     * need to be removed first?
                     */
                    m_blockDevices.put(name, (UsbBlockDevice) usbDevice);
                    this.m_linuxUdevListener.attached(usbDevice);
                } else if (type.compareTo(UdevEventType.DETACHED.name()) == 0) {
                    /*
                     * Due to the above limitations,
                     * the best we can do is to remove the device from the
                     * map of already known devices by its name.
                     */
                    UsbBlockDevice removedDevice = m_blockDevices.remove(name);
                    if (removedDevice != null) {
                        this.m_linuxUdevListener.detached(removedDevice);
                    }
                } else {
                    s_logger.debug("Unknown udev event: {}", type);
                }
            }
        } else if (usbDevice instanceof UsbNetDevice) {
            String name = ((UsbNetDevice) usbDevice).getInterfaceName();
            if (name != null) {
                if (type.compareTo(UdevEventType.ATTACHED.name()) == 0) {
                    m_netDevices.put(name, (UsbNetDevice) usbDevice);
                    this.m_linuxUdevListener.attached(usbDevice);
                } else if (type.compareTo(UdevEventType.DETACHED.name()) == 0) {
                    UsbNetDevice removedDevice = m_netDevices.remove(name);
                    if (removedDevice != null) {
                        this.m_linuxUdevListener.detached(removedDevice);
                    }
                } else {
                    s_logger.debug("Unknown udev event: {}", type);
                }
            }
        } else if (usbDevice instanceof UsbTtyDevice) {
            String name = ((UsbTtyDevice) usbDevice).getDeviceNode();
            if (name != null) {
                if (type.compareTo(UdevEventType.ATTACHED.name()) == 0) {
                    m_ttyDevices.put(name, (UsbTtyDevice) usbDevice);
                    this.m_linuxUdevListener.attached(usbDevice);
                } else if (type.compareTo(UdevEventType.DETACHED.name()) == 0) {
                    UsbTtyDevice removedDevice = m_ttyDevices.remove(name);
                    if (removedDevice != null) {
                        this.m_linuxUdevListener.detached(removedDevice);
                    }
                } else {
                    s_logger.debug("Unknown udev event: {}", type);
                }
            }
        }
    }

    /** Start this Hotplug Thread. */
    private void start() {

        this.m_executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "UdevHotplugThread");
                thread.setDaemon(true);
                return thread;
            }
        });

        s_task = this.m_executor.submit(new Runnable() {

            @Override
            public void run() {
                s_logger.info("Starting LinuxUdevNative Thread ...");
                Thread.currentThread().setName("LinuxUdevNative");
                try {
                    LinuxUdevNative.nativeHotplugThread(LinuxUdevNative.this.m_linuxUdevNative);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
