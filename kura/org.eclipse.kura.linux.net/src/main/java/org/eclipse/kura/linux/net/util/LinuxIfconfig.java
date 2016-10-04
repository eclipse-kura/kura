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

package org.eclipse.kura.linux.net.util;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.NetInterfaceType;

public class LinuxIfconfig {

    private final String m_name;
    private NetInterfaceType m_type;
    private String m_macAddress;
    private String m_inetAddress;
    private String m_peerInetAddr;
    private String m_inetBcast;
    private String m_inetMask;
    private int m_mtu;
    private boolean m_multicast;
    private Map<String, String> m_driver;
    private Boolean m_up;
    private boolean m_linkUp;

    public LinuxIfconfig(String name) {
        this.m_name = name;
        this.m_type = NetInterfaceType.UNKNOWN;
    }

    public String getName() {
        return this.m_name;
    }

    public NetInterfaceType getType() {
        return this.m_type;
    }

    public void setType(NetInterfaceType type) {
        this.m_type = type;
    }

    public String getMacAddress() {
        return this.m_macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.m_macAddress = macAddress;
    }

    public String getInetAddress() {
        return this.m_inetAddress;
    }

    public void setInetAddress(String inetAddress) {
        this.m_inetAddress = inetAddress;
    }

    public String getInetBcast() {
        return this.m_inetBcast;
    }

    public void setInetBcast(String inetBcast) {
        this.m_inetBcast = inetBcast;
    }

    public String getInetMask() {
        return this.m_inetMask;
    }

    public void setInetMask(String inetMask) {
        this.m_inetMask = inetMask;
    }

    public String getPeerInetAddr() {
        return this.m_peerInetAddr;
    }

    public void setPeerInetAddr(String peerInetAddr) {
        this.m_peerInetAddr = peerInetAddr;
    }

    public int getMtu() {
        return this.m_mtu;
    }

    public void setMtu(int mtu) {
        this.m_mtu = mtu;
    }

    public boolean isMulticast() {
        return this.m_multicast;
    }

    public void setMulticast(boolean multicast) {
        this.m_multicast = multicast;
    }

    public Map<String, String> getDriver() {
        return this.m_driver;
    }

    public void setDriver(Map<String, String> driver) {
        this.m_driver = driver;
    }

    public boolean isUp() {
        if (this.m_up != null) {
            return this.m_up;
        } else {
            // old code
            boolean ret = false;
            if (this.m_inetAddress != null && this.m_inetMask != null) {
                ret = true;
            }
            return ret;
        }
    }

    public void setUp(boolean up) {
        this.m_up = up;
    }

    public void setLinkUp(boolean up) {
        this.m_linkUp = up;
    }

    public boolean isLinkUp() {
        return this.m_linkUp;
    }

    public byte[] getMacAddressBytes() throws KuraException {

        if (this.m_macAddress == null) {
            return new byte[] { 0, 0, 0, 0, 0, 0 };
        }

        String macAddress = new String(this.m_macAddress);
        macAddress = macAddress.replaceAll(":", "");

        byte[] mac = new byte[6];
        for (int i = 0; i < 6; i++) {
            mac[i] = (byte) ((Character.digit(macAddress.charAt(i * 2), 16) << 4)
                    + Character.digit(macAddress.charAt(i * 2 + 1), 16));
        }

        return mac;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.m_name).append(":-> type: ").append(this.m_type).append(", MAC: ").append(this.m_macAddress)
                .append(", IP Address: ").append(this.m_inetAddress).append(", Netmask: ").append(this.m_inetMask)
                .append(", Broadcast: ").append(this.m_inetBcast).append(", Peer IP Address: ")
                .append(this.m_peerInetAddr).append(", MTU: ").append(this.m_mtu).append(", multicast?: ")
                .append(this.m_multicast).append(", up?: ").append(this.m_up).append(", link up?: ")
                .append(this.m_linkUp);
        return sb.toString();
    }
}
