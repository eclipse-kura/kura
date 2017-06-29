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

package org.eclipse.kura.linux.net.util;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.NetInterfaceType;

public class LinuxIfconfig {

    private final String name;
    private NetInterfaceType type;
    private String macAddress;
    private String inetAddress;
    private String peerInetAddr;
    private String inetBcast;
    private String inetMask;
    private int mtu;
    private boolean multicast;
    private Map<String, String> driver;
    private Boolean up;
    private boolean linkUp;

    public LinuxIfconfig(String name) {
        this.name = name;
        this.type = NetInterfaceType.UNKNOWN;
    }

    public String getName() {
        return this.name;
    }

    public NetInterfaceType getType() {
        return this.type;
    }

    public void setType(NetInterfaceType type) {
        this.type = type;
    }

    public String getMacAddress() {
        return this.macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getInetAddress() {
        return this.inetAddress;
    }

    public void setInetAddress(String inetAddress) {
        this.inetAddress = inetAddress;
    }

    public String getInetBcast() {
        return this.inetBcast;
    }

    public void setInetBcast(String inetBcast) {
        this.inetBcast = inetBcast;
    }

    public String getInetMask() {
        return this.inetMask;
    }

    public void setInetMask(String inetMask) {
        this.inetMask = inetMask;
    }

    public String getPeerInetAddr() {
        return this.peerInetAddr;
    }

    public void setPeerInetAddr(String peerInetAddr) {
        this.peerInetAddr = peerInetAddr;
    }

    public int getMtu() {
        return this.mtu;
    }

    public void setMtu(int mtu) {
        this.mtu = mtu;
    }

    public boolean isMulticast() {
        return this.multicast;
    }

    public void setMulticast(boolean multicast) {
        this.multicast = multicast;
    }

    public Map<String, String> getDriver() {
        return this.driver;
    }

    public void setDriver(Map<String, String> driver) {
        this.driver = driver;
    }

    public boolean isUp() {
        if (this.up != null) {
            return this.up;
        } else {
            // old code
            boolean ret = false;
            if (this.inetAddress != null && this.inetMask != null) {
                ret = true;
            }
            return ret;
        }
    }

    public void setUp(boolean up) {
        this.up = up;
    }

    public void setLinkUp(boolean up) {
        this.linkUp = up;
    }

    public boolean isLinkUp() {
        return this.linkUp;
    }

    public byte[] getMacAddressBytes() throws KuraException {

        if (this.macAddress == null) {
            return new byte[] { 0, 0, 0, 0, 0, 0 };
        }
        String macAddr = this.macAddress.replaceAll(":", "");
        byte[] mac = new byte[6];
        for (int i = 0; i < 6; i++) {
            mac[i] = (byte) ((Character.digit(macAddr.charAt(i * 2), 16) << 4)
                    + Character.digit(macAddr.charAt(i * 2 + 1), 16));
        }
        return mac;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.name).append(":-> type: ").append(this.type).append(", MAC: ").append(this.macAddress)
                .append(", IP Address: ").append(this.inetAddress).append(", Netmask: ").append(this.inetMask)
                .append(", Broadcast: ").append(this.inetBcast).append(", Peer IP Address: ").append(this.peerInetAddr)
                .append(", MTU: ").append(this.mtu).append(", multicast?: ").append(this.multicast).append(", up?: ")
                .append(this.up).append(", link up?: ").append(this.linkUp);
        return sb.toString();
    }
}
