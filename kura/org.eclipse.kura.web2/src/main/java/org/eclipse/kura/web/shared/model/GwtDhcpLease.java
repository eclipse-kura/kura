/*******************************************************************************
 * Copyright (c) 2022 Sterwen Technology and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

public class GwtDhcpLease {
    private String macAddress;
    private String ipAddress;
    private String hostname;

    public GwtDhcpLease() {
        this.macAddress = "";
        this.ipAddress = "";
        this.hostname = "";
    }

    public GwtDhcpLease(String macAddress, String ipAddress, String hostname) {
        super();
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.hostname = hostname;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(macAddress + "\t").append(ipAddress + "\t").append(hostname);
        return sb.toString();
    }
}
