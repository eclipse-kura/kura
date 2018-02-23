/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.usb.UsbDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNetInterface<T extends NetInterfaceAddress> implements NetInterface<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractNetInterface.class);

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
        this.interfaceAddresses = new ArrayList<>();
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
        this.interfaceAddresses = new ArrayList<>();
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
        return Collections.emptyList();
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
        if (this.interfaceAddresses != null && !this.interfaceAddresses.isEmpty()) {
            sb.append(" :: InterfaceAddress=");
            for (T interfaceAddress : this.interfaceAddresses) {
                sb.append(interfaceAddress).append(" ");
            }
        }

        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.autoConnect ? 1231 : 1237);
        result = prime * result + (this.driver == null ? 0 : this.driver.hashCode());
        result = prime * result + (this.driverVersion == null ? 0 : this.driverVersion.hashCode());
        result = prime * result + (this.firmwareVersion == null ? 0 : this.firmwareVersion.hashCode());
        result = prime * result + Arrays.hashCode(this.hardwareAddress);
        result = prime * result + (this.interfaceAddresses == null ? 0 : this.interfaceAddresses.hashCode());
        result = prime * result + (this.loopback ? 1231 : 1237);
        result = prime * result + this.mtu;
        result = prime * result + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result + (this.pointToPoint ? 1231 : 1237);
        result = prime * result + (this.state == null ? 0 : this.state.hashCode());
        result = prime * result + (this.supportsMulticast ? 1231 : 1237);
        result = prime * result + (this.up ? 1231 : 1237);
        result = prime * result + (this.usbDevice == null ? 0 : this.usbDevice.hashCode());
        result = prime * result + (this.virtual ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractNetInterface)) {
            return false;
        }
        AbstractNetInterface<?> other = (AbstractNetInterface<?>) obj;
        if (this.autoConnect != other.autoConnect) {
            return false;
        }
        if (this.driver == null) {
            if (other.driver != null) {
                return false;
            }
        } else if (!this.driver.equals(other.driver)) {
            return false;
        }
        if (this.driverVersion == null) {
            if (other.driverVersion != null) {
                return false;
            }
        } else if (!this.driverVersion.equals(other.driverVersion)) {
            return false;
        }
        if (this.firmwareVersion == null) {
            if (other.firmwareVersion != null) {
                return false;
            }
        } else if (!this.firmwareVersion.equals(other.firmwareVersion)) {
            return false;
        }
        if (!Arrays.equals(this.hardwareAddress, other.hardwareAddress)) {
            return false;
        }
        if (this.interfaceAddresses == null) {
            if (other.interfaceAddresses != null) {
                return false;
            }
        } else if (!this.interfaceAddresses.equals(other.interfaceAddresses)) {
            return false;
        }
        if (this.loopback != other.loopback) {
            return false;
        }
        if (this.mtu != other.mtu) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.pointToPoint != other.pointToPoint) {
            return false;
        }
        if (this.state != other.state) {
            return false;
        }
        if (this.supportsMulticast != other.supportsMulticast) {
            return false;
        }
        if (this.up != other.up) {
            return false;
        }
        if (this.usbDevice == null) {
            if (other.usbDevice != null) {
                return false;
            }
        } else if (!this.usbDevice.equals(other.usbDevice)) {
            return false;
        }
        if (this.virtual != other.virtual) {
            return false;
        }
        return true;
    }

    public NetInterfaceAddressConfig getNetInterfaceAddressConfig() throws KuraException {
        if (this.getNetInterfaceAddresses() == null || this.getNetInterfaceAddresses().isEmpty()) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "Empty NetInterfaceAddressConfig list");
        }
        return (NetInterfaceAddressConfig) this.getNetInterfaceAddresses().get(0);
    }

    /**
     * Returns a list of network configurations
     *
     * @return list of network configurations as {@link List<NetConfig>}
     */
    public List<NetConfig> getNetConfigs() {
        List<NetConfig> ret = new ArrayList<>();
        try {
            List<NetConfig> netConfigs = getNetInterfaceAddressConfig().getConfigs();
            if (netConfigs != null) {
                ret = netConfigs;
            }
        } catch (KuraException e) {
            logger.error("Failed to obtain NetConfigs", e);
        }
        return ret;
    }

    /**
     * Reports interface status
     *
     * @return interface status as {@link NetInterfaceStatus}
     */
    public NetInterfaceStatus getInterfaceStatus() {
        List<NetConfig> netConfigs = getNetConfigs();
        if (netConfigs == null) {
            return NetInterfaceStatus.netIPv4StatusUnknown;
        }
        NetInterfaceStatus status = NetInterfaceStatus.netIPv4StatusUnknown;
        for (NetConfig netConfig : netConfigs) {
            if (netConfig instanceof NetConfigIP4) {
                status = ((NetConfigIP4) netConfig).getStatus();
                break;
            }
        }
        return status;
    }

    /**
     * Reports IPv4 configuration
     *
     * @return IPv4 configuration as {@link NetConfigIP4}
     */
    public NetConfigIP4 getIP4config() {
        NetConfigIP4 netConfigIP4 = null;
        List<NetConfig> netConfigs = getNetConfigs();
        for (NetConfig netConfig : netConfigs) {
            if (netConfig instanceof NetConfigIP4) {
                netConfigIP4 = (NetConfigIP4) netConfig;
                break;
            }
        }
        return netConfigIP4;
    }

    /**
     * Reports if interface is managed by the NetAdmin
     *
     * @return boolean
     */
    public boolean isInterfaceManaged() {
        NetInterfaceStatus status = getInterfaceStatus();
        return status != NetInterfaceStatus.netIPv4StatusUnmanaged ? true : false;
    }

    /**
     * Reports if interface is enabled in configuration
     *
     * @return boolean
     */
    public boolean isInterfaceEnabled() {
        NetInterfaceStatus status = getInterfaceStatus();
        return status == NetInterfaceStatus.netIPv4StatusL2Only || status == NetInterfaceStatus.netIPv4StatusEnabledLAN
                || status == NetInterfaceStatus.netIPv4StatusEnabledWAN ? true : false;
    }
}
