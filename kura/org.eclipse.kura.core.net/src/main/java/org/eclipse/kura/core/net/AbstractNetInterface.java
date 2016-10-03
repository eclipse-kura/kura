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
package org.eclipse.kura.core.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.usb.UsbDevice;

public abstract class AbstractNetInterface<T extends NetInterfaceAddress> implements NetInterface<T> {

    private String name;
    private byte[] hardwareAddress;
    private boolean loopback;
    private boolean pointToPoint;
    private boolean virtual;
    private boolean supportsMulticast;
    private boolean up;
    private int mtu;
    private UsbDevice usbDevice;
    private String driver;
    private String driverVersion;
    private String firmwareVersion;
    private NetInterfaceState state;
    private boolean autoConnect;
    private List<T> interfaceAddresses;

    protected AbstractNetInterface(String name) {
        super();
        this.name = name;
        this.interfaceAddresses = new ArrayList<T>();
    }

    protected AbstractNetInterface(NetInterface<? extends NetInterfaceAddress> other) {
        super();
        this.name = other.getName();
        this.hardwareAddress = other.getHardwareAddress();
        this.loopback = other.isLoopback();
        this.pointToPoint = other.isPointToPoint();
        this.virtual = other.isVirtual();
        this.supportsMulticast = other.supportsMulticast();
        this.up = other.isUp();
        this.mtu = other.getMTU();
        this.usbDevice = other.getUsbDevice();
        this.driver = other.getDriver();
        this.driverVersion = other.getDriverVersion();
        this.firmwareVersion = other.getFirmwareVersion();
        this.state = other.getState();
        this.autoConnect = other.isAutoConnect();
        this.interfaceAddresses = new ArrayList<T>();
        // note - copying of interfaceAddresses are handled in the subclasses
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public byte[] getHardwareAddress() {
        return this.hardwareAddress;
    }

    @Override
    public boolean isLoopback() {
        return this.loopback;
    }

    @Override
    public boolean isPointToPoint() {
        return this.pointToPoint;
    }

    @Override
    public boolean isVirtual() {
        return this.virtual;
    }

    @Override
    public boolean supportsMulticast() {
        return this.supportsMulticast;
    }

    @Override
    public boolean isUp() {
        return this.up;
    }

    @Override
    public int getMTU() {
        return this.mtu;
    }

    public void setMTU(int mtu) {
        this.mtu = mtu;
    }

    @Override
    public String getDriver() {
        return this.driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    @Override
    public String getDriverVersion() {
        return this.driverVersion;
    }

    public void setDriverVersion(String driverVersion) {
        this.driverVersion = driverVersion;
    }

    @Override
    public String getFirmwareVersion() {
        return this.firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    @Override
    public NetInterfaceState getState() {
        return this.state;
    }

    public void setState(NetInterfaceState state) {
        this.state = state;
    }

    @Override
    public UsbDevice getUsbDevice() {
        return this.usbDevice;
    }

    public void setHardwareAddress(byte[] hardwareAddress) {
        this.hardwareAddress = hardwareAddress;
    }

    public void setLoopback(boolean loopback) {
        this.loopback = loopback;
    }

    public void setPointToPoint(boolean pointToPoint) {
        this.pointToPoint = pointToPoint;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public void setSupportsMulticast(boolean supportsMulticast) {
        this.supportsMulticast = supportsMulticast;
    }

    public void setUp(boolean up) {
        this.up = up;
    }

    public void setUsbDevice(UsbDevice usbDevice) {
        this.usbDevice = usbDevice;
    }

    @Override
    public boolean isAutoConnect() {
        return this.autoConnect;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    @Override
    public List<T> getNetInterfaceAddresses() {
        if (this.interfaceAddresses != null) {
            return Collections.unmodifiableList(this.interfaceAddresses);
        }
        return null;
    }

    public void setNetInterfaceAddresses(List<T> interfaceAddresses) {
        this.interfaceAddresses = interfaceAddresses;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("name=").append(this.name);
        if (this.hardwareAddress != null && this.hardwareAddress.length == 6) {
            sb.append(" :: hardwareAddress=").append(NetworkUtil.macToString(this.hardwareAddress));
        }
        sb.append(" :: loopback=").append(this.loopback).append(" :: pointToPoint=").append(this.pointToPoint)
                .append(" :: virtual=").append(this.virtual).append(" :: supportsMulticast=")
                .append(this.supportsMulticast).append(" :: up=").append(this.up).append(" :: mtu=").append(this.mtu);
        if (this.usbDevice != null) {
            sb.append(" :: usbDevice=").append(this.usbDevice);
        }
        sb.append(" :: driver=").append(this.driver).append(" :: driverVersion=").append(this.driverVersion)
                .append(" :: firmwareVersion=").append(this.firmwareVersion).append(" :: state=").append(this.state)
                .append(" :: autoConnect=").append(this.autoConnect);
        if (this.interfaceAddresses != null && this.interfaceAddresses.size() > 0) {
            sb.append(" :: InterfaceAddress=");
            for (T interfaceAddress : this.interfaceAddresses) {
                sb.append(interfaceAddress).append(" ");
            }
        }

        return sb.toString();
    }
}
