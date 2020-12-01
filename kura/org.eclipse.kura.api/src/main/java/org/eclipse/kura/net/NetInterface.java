/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net;

import java.util.List;

import org.eclipse.kura.usb.UsbDevice;
import org.osgi.annotation.versioning.ProviderType;

/**
 * NetworkInterface represent a network interface of the system.
 * Its APIs are purposefully modeled after the java.net.NetworkInterface.
 * Compared to the standard Java API, this class provides additional information
 * such as the NetworkInterfaceType, whether the interface is provided to the system
 * through a USB Adapter, and additional low-level characteristics of the interface.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface NetInterface<T extends NetInterfaceAddress> {

    /**
     * Returns the name of this NetworkInterface.
     *
     * @return interface name
     */
    public String getName();

    /**
     * Returns the type of this NetworkInterface.
     *
     * @return interface type
     */
    public NetInterfaceType getType();

    /**
     * The driver handling the device.
     *
     * @return
     */
    public String getDriver();

    /**
     * The version of the driver handling the device.
     *
     * @return
     */
    public String getDriverVersion();

    /**
     * The firmware version for the device.
     *
     * @return
     */
    public String getFirmwareVersion();

    /**
     * The current state of the device.
     *
     * @return
     */
    public NetInterfaceState getState();

    /**
     * Returns the hardware address (usually MAC) of the interface if it has one.
     *
     * @return a byte array containing the address or null if the address doesn't exist
     */
    public byte[] getHardwareAddress();

    /**
     * Returns a List of all InterfaceAddresses of this network interface.
     *
     * @return a List object with all or a subset of the InterfaceAddresss of this network interface
     */
    public List<T> getNetInterfaceAddresses();

    /**
     * Returns whether a network interface is a loopback interface.
     *
     * @return true if the interface is a loopback interface.
     */
    public boolean isLoopback();

    /**
     * Returns whether a network interface is a point to point interface.
     * A typical point to point interface would be a PPP connection through a modem.
     *
     * @return true if the interface is a point to point interface.
     */
    public boolean isPointToPoint();

    /**
     * Returns whether this interface is a virtual interface (also called subinterface).
     * Virtual interfaces are, on some systems, interfaces created as a child of a physical
     * interface and given different settings (like address or MTU).
     * Usually the name of the interface will the name of the parent followed by a colon (:)
     * and a number identifying the child since there can be several virtual interfaces
     * attached to a single physical interface.
     *
     * @return true if this interface is a virtual interface.
     */
    public boolean isVirtual();

    /**
     * Returns whether a network interface supports multicasting or not.
     *
     * @return true if the interface supports Multicasting.
     */
    public boolean supportsMulticast();

    /**
     * Returns whether a network interface is up and running.
     *
     * @return true if the interface is up and running.
     */
    public boolean isUp();

    /**
     * Returns whether a network interface will auto connect.
     *
     * @return true if the interface will auto connect.
     */
    public boolean isAutoConnect();

    /**
     * Returns the Maximum Transmission Unit (MTU) of this interface
     * - Design speed of the device, in megabits/second (Mb/s).
     *
     * @return the value of the MTU for that interface.
     */
    public int getMTU();

    //
    // Kura Extensions
    //

    /**
     * Returns the UsbDevice which provided this NetworkInterface to the system if any.
     *
     * @return the UsbDevice providing this NetworkInterface to the system
     *         or null if this NetworkInterface is not provided by a USB device
     */
    public UsbDevice getUsbDevice();

    /*
     * public String getScope();
     *
     * public String getBroadcast();
     *
     * public String getMask();
     *
     * public boolean isBroadcast();
     *
     * public boolean isRunning();
     *
     * public boolean isMulticast();
     *
     * public boolean isAllmulti();
     *
     * public boolean isPromisc();
     *
     * public long getRxPackets();
     *
     * public long getRxErrors();
     *
     * public long getRxDropped();
     *
     * public long getRxOverruns();
     *
     * public long getRxFrame();
     *
     * public long getTxPackets();
     *
     * public long getTxErrors();
     *
     * public long getTxDropped();
     *
     * public long getTxOverruns();
     *
     * public long getTxCarrier();
     *
     * public long getCollisions();
     *
     * public long getTxQueueLength();
     *
     * public long getTxBytes();
     *
     * public long getRxBytes();
     */
}
