/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.linux.net.dhcp;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IPAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class defines DHCP client lease block
 * 
 */
public class DhcpClientLeaseBlock {

    private static final Logger logger = LoggerFactory.getLogger(DhcpClientLeaseBlock.class);

    private static final String INTERFACE_PARAM_NAME = "interface";
    private static final String FIXED_ADDRESS_PARAM_NAME = "fixed-address";
    private static final String SUBNET_MASK_OPTION_NAME = "subnet-mask";
    private static final String ROUTERS_OPTION_NAME = "routers";
    private static final String DHCP_LEASE_TIME_OPTION_NAME = "dhcp-lease-time";
    private static final String DHCP_MESSAGE_TYPE_OPTION_NAME = "dhcp-message-type";
    private static final String DOMAIN_NANE_SERVERS_OPTION_NAME = "domain-name-servers";
    private static final String DHCP_SERVER_IDENTIFIER_OPTION_NAME = "dhcp-server-identifier";
    private static final String DHCP_RENEWAL_TIME_OPTION_NAME = "dhcp-renewal-time";
    private static final String DHCP_REBINDING_TIME_OPTION_NAME = "dhcp-rebinding-time";
    private static final String DOMAIN_NAME_OPTION_NAME = "domain-name";

    private static final String FAILED_PARSE_LINE_MSG = "Failed to parse line {}";

    private String iface;
    private IPAddress fixedAddress;
    private IPAddress subnetMask;
    private List<IPAddress> routers;
    private long dhcpLeaseTime;
    private int dhcpMessageType;
    private List<IPAddress> dnsServers;
    private IPAddress dhcpServer;
    private long dhcpRenewalTime;
    private long dhcpRebindingTime;
    private String domainName;

    /**
     * DhcpClientLeaseBlock
     * 
     * @param leaseBlock
     *            - lease block
     */
    public DhcpClientLeaseBlock(List<String> leaseBlock) throws KuraException {
        parseLeaseBlock(leaseBlock);
    }

    private void parseLeaseBlock(List<String> leaseBlock) throws KuraException {
        for (String line : leaseBlock) {
            if (line.contains(INTERFACE_PARAM_NAME)) {
                this.iface = parseString(line, INTERFACE_PARAM_NAME);
            } else if (line.contains(FIXED_ADDRESS_PARAM_NAME)) {
                this.fixedAddress = parseIPaddress(line, FIXED_ADDRESS_PARAM_NAME);
            } else if (line.contains(SUBNET_MASK_OPTION_NAME)) {
                this.subnetMask = parseIPaddress(line, SUBNET_MASK_OPTION_NAME);
            } else if (line.contains(ROUTERS_OPTION_NAME)) {
                this.routers = parseIPlist(line, ROUTERS_OPTION_NAME);
            } else if (line.contains(DHCP_LEASE_TIME_OPTION_NAME)) {
                this.dhcpLeaseTime = parseLong(line, DHCP_LEASE_TIME_OPTION_NAME);
            } else if (line.contains(DHCP_MESSAGE_TYPE_OPTION_NAME)) {
                this.dhcpMessageType = parseInteger(line, DHCP_MESSAGE_TYPE_OPTION_NAME);
            } else if (line.contains(DOMAIN_NANE_SERVERS_OPTION_NAME)) {
                this.dnsServers = parseIPlist(line, DOMAIN_NANE_SERVERS_OPTION_NAME);
            } else if (line.contains(DHCP_SERVER_IDENTIFIER_OPTION_NAME)) {
                this.dhcpServer = parseIPaddress(line, DHCP_SERVER_IDENTIFIER_OPTION_NAME);
            } else if (line.contains(DHCP_RENEWAL_TIME_OPTION_NAME)) {
                this.dhcpRenewalTime = parseLong(line, DHCP_RENEWAL_TIME_OPTION_NAME);
            } else if (line.contains(DHCP_REBINDING_TIME_OPTION_NAME)) {
                this.dhcpRebindingTime = parseLong(line, DHCP_REBINDING_TIME_OPTION_NAME);
            } else if (line.contains(DOMAIN_NAME_OPTION_NAME)) {
                this.domainName = parseString(line, DOMAIN_NAME_OPTION_NAME);
            }
        }
    }

    /**
     * Reports if supplied interface name and IP address match respective fields
     * 
     * @param ifaceName
     *            - interface name
     * @param address
     *            - fixed IP address
     * @return boolean
     */
    public boolean matches(String ifaceName, IPAddress address) {
        boolean ret = false;
        if (ifaceName != null && address != null && ifaceName.equals(this.iface) && address.equals(this.fixedAddress)) {
            ret = true;
        }
        return ret;
    }

    /**
     * Reports interface name
     * 
     * @return interface name
     */
    public String getIface() {
        return this.iface;
    }

    /**
     * Reports IP address
     * 
     * @return Fixed IP address
     */
    public IPAddress getFixedAddress() {
        return this.fixedAddress;
    }

    /**
     * Reports subnet mask
     * 
     * @return subnet mask
     */
    public IPAddress getSubnetMask() {
        return this.subnetMask;
    }

    /**
     * Reports list of routers
     * 
     * @return list of routers
     */
    public List<IPAddress> getRouters() {
        return this.routers;
    }

    /**
     * Reports DHCP lease time
     * 
     * @return DHCP lease time
     */
    public long getDhcpLeaseTime() {
        return this.dhcpLeaseTime;
    }

    /**
     * Reports DHCP message type
     * 
     * @return DHCP message type
     */
    public int getDhcpMessageType() {
        return this.dhcpMessageType;
    }

    /**
     * Reports list of DNS servers
     * 
     * @return list of DNS servers
     */
    public List<IPAddress> getDnsServers() {
        return this.dnsServers;
    }

    /**
     * Reports DHCP server
     * 
     * @return DHCP server
     */
    public IPAddress getDhcpServer() {
        return this.dhcpServer;
    }

    /**
     * Reports DHCP renewal time
     * 
     * @return DHCP renewal time
     */
    public long getDhcpRenewalTime() {
        return this.dhcpRenewalTime;
    }

    /**
     * Reports DHCP rebinding time
     * 
     * @return DHCP rebinding time
     */
    public long getDhcpRebindingTime() {
        return this.dhcpRebindingTime;
    }

    /**
     * Reports domain name
     * 
     * @return domain name
     */
    public String getDomainName() {
        return this.domainName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Dhclient Lease Block: [");
        sb.append(INTERFACE_PARAM_NAME).append('=').append(this.iface);
        sb.append("; ").append(FIXED_ADDRESS_PARAM_NAME).append('=').append(this.fixedAddress);
        sb.append("; ").append(SUBNET_MASK_OPTION_NAME).append('=').append(this.subnetMask);
        sb.append("; ").append(ROUTERS_OPTION_NAME).append('=').append(this.routers);
        sb.append("; ").append(DHCP_LEASE_TIME_OPTION_NAME).append('=').append(this.dhcpLeaseTime);
        sb.append("; ").append(DHCP_MESSAGE_TYPE_OPTION_NAME).append('=').append(this.dhcpMessageType);
        sb.append("; ").append(DOMAIN_NANE_SERVERS_OPTION_NAME).append('=').append(this.dnsServers);
        sb.append("; ").append(DHCP_SERVER_IDENTIFIER_OPTION_NAME).append('=').append(this.dhcpServer);
        sb.append("; ").append(DHCP_RENEWAL_TIME_OPTION_NAME).append('=').append(this.dhcpRenewalTime);
        sb.append("; ").append(DHCP_REBINDING_TIME_OPTION_NAME).append('=').append(this.dhcpRebindingTime);
        sb.append("; ").append(DOMAIN_NAME_OPTION_NAME).append('=').append(this.domainName);
        sb.append(']');
        return sb.toString();
    }

    private int parseInteger(String line, String name) throws KuraException {
        int ret = 0;
        try {
            ret = Integer.parseInt(line.substring(line.indexOf(name) + name.length(), line.length() - 1).trim());
        } catch (NumberFormatException e) {
            logger.error(FAILED_PARSE_LINE_MSG, line, e);
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, e);
        }
        return ret;
    }

    private long parseLong(String line, String name) throws KuraException {
        long ret = 0;
        try {
            ret = Long.parseLong(line.substring(line.indexOf(name) + name.length(), line.length() - 1).trim());
        } catch (NumberFormatException e) {
            logger.error(FAILED_PARSE_LINE_MSG, line, e);
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, e);
        }
        return ret;
    }

    private String parseString(String line, String name) {
        String val = line.substring(line.indexOf(name) + name.length(), line.length() - 1).trim();
        return val.substring(1, val.length() - 1);
    }

    private IPAddress parseIPaddress(String line, String name) throws KuraException {
        IPAddress ipAddress = null;
        try {
            ipAddress = IPAddress
                    .parseHostAddress(line.substring(line.indexOf(name) + name.length(), line.length() - 1).trim());
        } catch (UnknownHostException e) {
            logger.error(FAILED_PARSE_LINE_MSG, line, e);
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, e);
        }
        return ipAddress;
    }

    private List<IPAddress> parseIPlist(String line, String name) throws KuraException {
        List<IPAddress> list = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(line.substring(line.indexOf(name) + name.length()), ", ;");
        while (st.hasMoreTokens()) {
            try {
                IPAddress nameServer = IPAddress.parseHostAddress(st.nextToken());
                if (!list.contains(nameServer)) {
                    list.add(nameServer);
                }
            } catch (UnknownHostException e) {
                logger.error(FAILED_PARSE_LINE_MSG, line, e);
                throw new KuraException(KuraErrorCode.INVALID_PARAMETER, e);
            }
        }
        return list;
    }
}
