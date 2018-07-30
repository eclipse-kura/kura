package org.eclipse.kura.internal.linux.net.config;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.NetInterfaceStatus;

public class IfaceConfig {

    private NetInterfaceStatus netInterfaceStatus = null;
    private boolean autoConnect = false;
    private boolean dhcp = false;
    private IP4Address address = null;
    private String prefixString = null;
    private String netmask = null;
    private String gateway = null;

    public IfaceConfig(NetInterfaceStatus netInterfaceStatus, boolean autoConnect, boolean dhcp,
            IP4Address address, String prefixString, String netmask, String gateway) {
        this.netInterfaceStatus = netInterfaceStatus;
        this.autoConnect = autoConnect;
        this.dhcp = dhcp;
        this.address = address;
        this.prefixString = prefixString;
        this.netmask = netmask;
        this.gateway = gateway;
    }

    public NetInterfaceStatus getNetInterfaceStatus() {
        return this.netInterfaceStatus;
    }

    public boolean isAutoConnect() {
        return this.autoConnect;
    }

    public boolean isDhcp() {
        return this.dhcp;
    }

    public IP4Address getAddress() {
        return this.address;
    }

    public String getPrefixString() {
        return this.prefixString;
    }

    public String getNetmask() {
        return this.netmask;
    }

    public String getGateway() {
        return this.gateway;
    }
}
