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
package org.eclipse.kura.core.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.usb.UsbDevice;

public abstract class AbstractNetInterface<T extends NetInterfaceAddress> implements NetInterface<T> 
{
	private String 			   name;
	private byte[]			   hardwareAddress;
	private boolean            loopback;
	private boolean            pointToPoint;
	private boolean            virtual;
	private boolean            supportsMulticast;
	private boolean            up;
	private int                mtu;
	private UsbDevice          usbDevice;
	private String             driver;
	private String             driverVersion;
	private String             firmwareVersion;
	private NetInterfaceState  state;
	private boolean            autoConnect;
	private List<T>            interfaceAddresses;
	
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
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
	    this.name = name;
	}

	public byte[] getHardwareAddress() {
		return hardwareAddress;
	}
	
	public boolean isLoopback() {
		return loopback;
	}
	
	public boolean isPointToPoint() {
		return pointToPoint;
	}
	
	public boolean isVirtual() {
		return virtual;
	}
	
	public boolean supportsMulticast() {
		return supportsMulticast;
	}
	
	public boolean isUp() {
		return up;
	}

	public int getMTU() {
		return mtu;
	}

	public void setMTU(int mtu) {
		this.mtu = mtu;
	}
	
	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getDriverVersion() {
		return driverVersion;
	}

	public void setDriverVersion(String driverVersion) {
		this.driverVersion = driverVersion;
	}

	public String getFirmwareVersion() {
		return firmwareVersion;
	}

	public void setFirmwareVersion(String firmwareVersion) {
		this.firmwareVersion = firmwareVersion;
	}

	public NetInterfaceState getState() {
		return state;
	}

	public void setState(NetInterfaceState state) {
		this.state = state;
	}

	public UsbDevice getUsbDevice() {
		return usbDevice;
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
	
	public boolean isAutoConnect() {
		return autoConnect;
	}

	public void setAutoConnect(boolean autoConnect) {
		this.autoConnect = autoConnect;
	}
	
	public List<T> getNetInterfaceAddresses() {
	    if(this.interfaceAddresses != null) {
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
		sb.append("name=").append(name);
		if(hardwareAddress != null && hardwareAddress.length == 6) {
			sb.append(" :: hardwareAddress=")
			.append(NetworkUtil.macToString(hardwareAddress));
		}
		sb.append(" :: loopback=").append(loopback)
		.append(" :: pointToPoint=").append(pointToPoint)
		.append(" :: virtual=").append(virtual)
		.append(" :: supportsMulticast=").append(supportsMulticast)
		.append(" :: up=").append(up)
		.append(" :: mtu=").append(mtu);
		if(usbDevice != null) {
			sb.append(" :: usbDevice=").append(usbDevice);
		}
		sb.append(" :: driver=").append(driver)
		.append(" :: driverVersion=").append(driverVersion)
		.append(" :: firmwareVersion=").append(firmwareVersion)
		.append(" :: state=").append(state)
		.append(" :: autoConnect=").append(autoConnect);
		if(interfaceAddresses != null && interfaceAddresses.size() > 0) {
			sb.append(" :: InterfaceAddress=");
			for(T interfaceAddress : interfaceAddresses) {
				sb.append(interfaceAddress)
				.append(" ");
			}
		}
		
		return sb.toString();
	}
}
