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

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Abstract class that contains common properties to describe the status of a
 * network interface. Specific interfaces, like ethernet or wifi, must extend
 * this class.
 *
 * A network interface is identified by Eclipse Kura using the interfaceId
 * field. It is used to internally manage the interface.
 * The interfaceName, instead, is the IP interface as it may appear on the
 * system. For Ethernet and WiFi interfaces the two values coincide
 * (i.e. eth0, wlp1s0, ...).
 * For modems, instead, the id is typically the usb or pci path, while the
 * interfaceName is the IP interface created when they are connected.
 * When the modem is disconnected the interfaceName can have a different value.
 *
 */
@ProviderType
public abstract class NetworkInterfaceStatus {

    private final String interfaceId;
    private final String interfaceName;
    private final byte[] hardwareAddress;
    private final NetworkInterfaceType type;
    private final String driver;
    private final String driverVersion;
    private final String firmwareVersion;
    private final boolean virtual;
    private final NetworkInterfaceState state;
    private final boolean autoConnect;
    private final int mtu;
    private final Optional<NetworkInterfaceIpAddressStatus<IP4Address>> interfaceIp4Addresses;
    private final Optional<NetworkInterfaceIpAddressStatus<IP6Address>> interfaceIp6Addresses;

    protected NetworkInterfaceStatus(NetworkInterfaceStatusBuilder<?> builder) {
        this.interfaceId = builder.interfaceId;
        this.interfaceName = builder.interfaceName;
        this.hardwareAddress = builder.hardwareAddress;
        this.type = builder.type;
        this.driver = builder.driver;
        this.driverVersion = builder.driverVersion;
        this.firmwareVersion = builder.firmwareVersion;
        this.virtual = builder.virtual;
        this.state = builder.state;
        this.autoConnect = builder.autoConnect;
        this.mtu = builder.mtu;
        this.interfaceIp4Addresses = builder.interfaceIp4Addresses;
        this.interfaceIp6Addresses = builder.interfaceIp6Addresses;
    }

    public String getInterfaceId() {
        return this.interfaceId;
    }

    public String getInterfaceName() {
        return this.interfaceName;
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
        private String interfaceId = NA;
        private String interfaceName = NA;
        private byte[] hardwareAddress = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        private NetworkInterfaceType type = NetworkInterfaceType.UNKNOWN;
        private String driver = NA;
        private String driverVersion = NA;
        private String firmwareVersion = NA;
        private boolean virtual = false;
        private NetworkInterfaceState state = NetworkInterfaceState.UNKNOWN;
        private boolean autoConnect = false;
        private int mtu = 0;
        private Optional<NetworkInterfaceIpAddressStatus<IP4Address>> interfaceIp4Addresses = Optional.empty();
        private Optional<NetworkInterfaceIpAddressStatus<IP6Address>> interfaceIp6Addresses = Optional.empty();

        public T withInterfaceId(String interfaceId) {
            this.interfaceId = interfaceId;
            return getThis();
        }

        public T withInterfaceName(String interfacenName) {
            this.interfaceName = interfacenName;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.hardwareAddress);
        result = prime * result + Objects.hash(this.autoConnect, this.driver, this.driverVersion, this.firmwareVersion,
                this.interfaceName, this.interfaceIp4Addresses, this.interfaceIp6Addresses, this.mtu, this.interfaceId,
                this.state, this.type, this.virtual);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        NetworkInterfaceStatus other = (NetworkInterfaceStatus) obj;
        return this.autoConnect == other.autoConnect && Objects.equals(this.driver, other.driver)
                && Objects.equals(this.driverVersion, other.driverVersion)
                && Objects.equals(this.firmwareVersion, other.firmwareVersion)
                && Arrays.equals(this.hardwareAddress, other.hardwareAddress)
                && Objects.equals(this.interfaceName, other.interfaceName)
                && Objects.equals(this.interfaceIp4Addresses, other.interfaceIp4Addresses)
                && Objects.equals(this.interfaceIp6Addresses, other.interfaceIp6Addresses) && this.mtu == other.mtu
                && Objects.equals(this.interfaceId, other.interfaceId) && this.state == other.state
                && this.type == other.type
                && this.virtual == other.virtual;
    }

}
