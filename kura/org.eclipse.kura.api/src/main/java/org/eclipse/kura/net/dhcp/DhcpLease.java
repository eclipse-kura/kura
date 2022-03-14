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
package org.eclipse.kura.net.dhcp;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @since 2.3
 */
@ProviderType
public class DhcpLease {
    private String macAddress;
    private String ipAddress;
    private String hostname;

    public DhcpLease(String macAddress, String ipAddress, String hostname) {
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
        sb.append("[");
        sb.append("MacAddress:").append(macAddress).append(", IpAddress:").append(ipAddress).append(", Hostname:").append(hostname);
        sb.append("]");
        return sb.toString();
    }
}
