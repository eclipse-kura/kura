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
package org.eclipse.kura.linux.net;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.ConnectionInfo;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionInfoImpl implements ConnectionInfo {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionInfo.class);
    /**
     * The interface name associated with the connection information
     */
    private String ifaceName = null;

    /**
     * The properties representing the connection information
     */
    private Properties props = null;

    /**
     * Creates a ConnectionInfo instance with the previously persisted connection properties if they existed
     *
     * @param ifaceName
     *            The interface name tied to the connection information
     * @throws KuraException
     */
    public ConnectionInfoImpl(String ifaceName) throws KuraException {

        this.ifaceName = ifaceName;
        this.props = new Properties();

        File coninfoFile = new File(formConinfoFileName(ifaceName));
        if (coninfoFile.exists()) {
            try (FileInputStream fis = new FileInputStream(coninfoFile)) {
                this.props.load(fis);
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.INVALID_PARAMETER, e);
            }
        }
    }

    @Override
    public IP4Address getIpAddress() {
        IP4Address ipAddress = null;
        String sIpAddress = this.props.getProperty("IPADDR");
        if (sIpAddress != null) {
            try {
                ipAddress = (IP4Address) IPAddress.parseHostAddress(sIpAddress);
            } catch (Exception e) {
                logger.error("Error parsing IP address!", e);
            }
        }

        return ipAddress;
    }

    /**
     * Gets the gateway address associated with this interface
     *
     * @return A IP4Address representing the gateway if it is not null
     */
    @Override
    public IP4Address getGateway() {

        IP4Address gateway = null;
        String sGateway = this.props.getProperty("GATEWAY");
        if (sGateway != null) {
            try {
                gateway = (IP4Address) IPAddress.parseHostAddress(sGateway);
            } catch (Exception e) {
                logger.error("Error parsing gateway address!", e);
            }
        }

        return gateway;
    }

    /**
     * Gets the DNS addresses associated with this interface
     *
     * @return A List of IP4Address objects representing the DNS of this interface. If there are none it returns an
     *         empty list.
     */
    @Override
    public List<IP4Address> getDnsServers() {

        List<IP4Address> lDnsServers = new ArrayList<>(2);

        for (int i = 1; i <= 2; i++) {
            String sDns = this.props.getProperty("DNS" + i);
            if (sDns != null) {
                try {
                    IP4Address dns = (IP4Address) IPAddress.parseHostAddress(sDns);
                    lDnsServers.add(dns);
                } catch (Exception e) {
                    logger.error("Error parsing DNS addresses!", e);
                }
            }
        }

        return lDnsServers;
    }

    /**
     * Gets the interface name associated with this connection information
     *
     * @return The interface name associated with this connection information
     */
    @Override
    public String getIfaceName() {
        return this.ifaceName;
    }

    private static String formConinfoFileName(String ifaceName) {
        StringBuilder sb = new StringBuilder();
        sb.append("/tmp/.kura/coninfo-");
        sb.append(ifaceName);

        return sb.toString();
    }
}
