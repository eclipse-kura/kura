/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.status;

import java.util.Optional;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.usb.UsbNetDevice;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Abstract class that contains common properties to describe the status of a
 * network interface. Specific interfaces, like ethernet or wifi, must extend
 * this class.
 *
 */
@ProviderType
public abstract class NetworkInterfaceStatus {

    private final String name;
    private final byte[] hardwareAddress;
    private final NetworkInterfaceType type;
    private final String driver;
    private final String driverVersion;
    private final String firmwareVersion;
    private final boolean virtual;
    private final NetworkInterfaceState state;
    private final boolean autoConnect;
    private final int mtu;
    private final Optional<UsbNetDevice> usbNetDevice;
    private final Optional<NetworkInterfaceIpAddressStatus<IP4Address>> interfaceIp4Addresses;
    private final Optional<NetworkInterfaceIpAddressStatus<IP6Address>> interfaceIp6Addresses;

    protected NetworkInterfaceStatus(NetworkInterfaceStatusBuilder<?> builder) {
        this.name = builder.name;
        this.hardwareAddress = builder.hardwareAddress;
        this.type = builder.type;
        this.driver = builder.driver;
        this.driverVersion = builder.driverVersion;
        this.firmwareVersion = builder.firmwareVersion;
        this.virtual = builder.virtual;
        this.state = builder.state;
        this.autoConnect = builder.autoConnect;
        this.mtu = builder.mtu;
        this.usbNetDevice = builder.usbNetDevice;
        this.interfaceIp4Addresses = builder.interfaceIp4Addresses;
        this.interfaceIp6Addresses = builder.interfaceIp6Addresses;
    }

    public String getName() {
        return this.name;
    }

    public byte[] getHardwareAddress() {
        return this.hardwareAddress;
    }

    public NetworkInterfaceType getType() {
        return this.type;
    }

    public String getDriver() {
        return this.driver;
    }

    public String getDriverVersion() {
        return this.driverVersion;
    }

    public String getFirmwareVersion() {
        return this.firmwareVersion;
    }

    public boolean isVirtual() {
        return this.virtual;
    }

    public NetworkInterfaceState getState() {
        return this.state;
    }

    public boolean isAutoConnect() {
        return this.autoConnect;
    }

    public int getMtu() {
        return this.mtu;
    }

    public Optional<UsbNetDevice> getUsbNetDevice() {
        return this.usbNetDevice;
    }

    public Optional<NetworkInterfaceIpAddressStatus<IP4Address>> getInterfaceIp4Addresses() {
        return this.interfaceIp4Addresses;
    }

    public Optional<NetworkInterfaceIpAddressStatus<IP6Address>> getInterfaceIp6Addresses() {
        return this.interfaceIp6Addresses;
    }

    /**
     * Abstract builder for a {@link NetworkInterfaceStatus} object. The builders
     * for specific interfaces, like ethernet or wifi, must extend this class.
     * 
     */
    public abstract static class NetworkInterfaceStatusBuilder<T extends NetworkInterfaceStatusBuilder<T>> {

        private static final String NA = "N/A";
        private String name = NA;
        private byte[] hardwareAddress = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        private NetworkInterfaceType type = NetworkInterfaceType.UNKNOWN;
        private String driver = NA;
        private String driverVersion = NA;
        private String firmwareVersion = NA;
        private boolean virtual = false;
        private NetworkInterfaceState state = NetworkInterfaceState.UNKNOWN;
        private boolean autoConnect = false;
        private int mtu = 0;
        private Optional<UsbNetDevice> usbNetDevice = Optional.empty();
        private Optional<NetworkInterfaceIpAddressStatus<IP4Address>> interfaceIp4Addresses = Optional.empty();
        private Optional<NetworkInterfaceIpAddressStatus<IP6Address>> interfaceIp6Addresses = Optional.empty();

        public T withName(String name) {
            this.name = name;
            return getThis();
        }

        public T withHardwareAddress(byte[] hardwareAddress) {
            this.hardwareAddress = hardwareAddress;
            return getThis();
        }

        protected T withType(NetworkInterfaceType type) {
            this.type = type;
            return getThis();
        }

        public T withDriver(String driver) {
            this.driver = driver;
            return getThis();
        }

        public T withDriverVersion(String driverVersion) {
            this.driverVersion = driverVersion;
            return getThis();
        }

        public T withFirmwareVersion(String firmwareVersion) {
            this.firmwareVersion = firmwareVersion;
            return getThis();
        }

        public T withVirtual(boolean virtual) {
            this.virtual = virtual;
            return getThis();
        }

        public T withState(NetworkInterfaceState state) {
            this.state = state;
            return getThis();
        }

        public T withAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
            return getThis();
        }

        public T withMtu(int mtu) {
            this.mtu = mtu;
            return getThis();
        }

        public T withUsbNetDevice(Optional<UsbNetDevice> usbNetDevice) {
            this.usbNetDevice = usbNetDevice;
            return getThis();
        }

        public T withInterfaceIp4Addresses(
                Optional<NetworkInterfaceIpAddressStatus<IP4Address>> interfaceIp4Addresses) {
            this.interfaceIp4Addresses = interfaceIp4Addresses;
            return getThis();
        }

        public T withInterfaceIp6Addresses(
                Optional<NetworkInterfaceIpAddressStatus<IP6Address>> interfaceIp6Addresses) {
            this.interfaceIp6Addresses = interfaceIp6Addresses;
            return getThis();
        }

        public abstract T getThis();

        public abstract NetworkInterfaceStatus build();
    }

}
