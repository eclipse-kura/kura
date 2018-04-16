/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.linux.net.dhcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IPAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class defines DHCP leases
 *
 */
public class DhcpClientLeases {

    private static final Logger logger = LoggerFactory.getLogger(DhcpClientLeases.class);

    private static final String GLOBAL_DHCP_LEASES_DIR = "/var/lib/dhcp";
    private static final String IFACE_DHCP_LEASES_DIR = "/var/lib/dhclient";

    private static DhcpClientLeases dhcpClientLeases;

    public DhcpClientLeases() {
        File leasesDirectory = new File(IFACE_DHCP_LEASES_DIR);
        if (!leasesDirectory.exists() && !leasesDirectory.mkdirs()) {
            logger.error("Failed to create {}", IFACE_DHCP_LEASES_DIR);
        }
    }

    public static synchronized DhcpClientLeases getInstance() {
        if (dhcpClientLeases == null) {
            dhcpClientLeases = new DhcpClientLeases();
        }
        return dhcpClientLeases;
    }

    public List<IPAddress> getDhcpGateways(String interfaceName, String address) throws KuraException {
        return getDhcpGateways(interfaceName, parseHostAddress(address));
    }

    public List<IPAddress> getDhcpGateways(String interfaceName, IPAddress address) throws KuraException {
        List<IPAddress> gateways = new ArrayList<>();
        if (interfaceName == null || interfaceName.isEmpty() || address == null || address.getAddress() == null) {
            return gateways;
        }

        List<DhcpClientLeaseBlock> leaseBlocks = getLeaseBlocks(getDhclientLeasesFile(interfaceName));
        for (DhcpClientLeaseBlock leaseBlock : leaseBlocks) {
            if (!leaseBlock.matches(interfaceName, address)) {
                continue;
            }
            List<IPAddress> routers = leaseBlock.getRouters();
            for (IPAddress router : routers) {
                if (!gateways.contains(router)) {
                    gateways.add(router);
                }
            }
        }
        return gateways;
    }

    public List<IPAddress> getDhcpDnsServers(String interfaceName, String address) throws KuraException {
        return getDhcpDnsServers(interfaceName, parseHostAddress(address));
    }

    public List<IPAddress> getDhcpDnsServers(String interfaceName, IPAddress address) throws KuraException {

        List<IPAddress> servers = new ArrayList<>();
        if (interfaceName == null || interfaceName.isEmpty() || address == null || address.getAddress() == null) {
            return servers;
        }

        List<DhcpClientLeaseBlock> leaseBlocks = getLeaseBlocks(getDhclientLeasesFile(interfaceName));
        for (DhcpClientLeaseBlock leaseBlock : leaseBlocks) {
            if (!leaseBlock.matches(interfaceName, address)) {
                continue;
            }
            List<IPAddress> dnsServers = leaseBlock.getDnsServers();
            for (IPAddress dnsServer : dnsServers) {
                if (!servers.contains(dnsServer)) {
                    servers.add(dnsServer);
                }
            }
        }
        return servers;
    }

    public String getDhclientLeasesFilePath(String interfaceName) {
        return formInterfaceDhclientLeasesFilename(interfaceName);
    }

    public File getDhclientLeasesFile(String interfaceName) throws KuraException {
        File ret;
        File globalDhClientFile = new File(formGlobalDhclientLeasesFilename());
        File interfaceDhClientFile = new File(formInterfaceDhclientLeasesFilename(interfaceName));

        if (interfaceDhClientFile.exists()) {
            ret = interfaceDhClientFile;
        } else if (globalDhClientFile.exists()) {
            ret = globalDhClientFile;
        } else {
            try {
                if (interfaceDhClientFile.createNewFile()) {
                    logger.info("The {} doesn't exist, created new empty file ...", interfaceDhClientFile.getName());
                }
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
            }
            ret = interfaceDhClientFile;
        }
        return ret;
    }

    private IPAddress parseHostAddress(String address) throws KuraException {
        IPAddress ipAddress;
        try {
            ipAddress = IPAddress.parseHostAddress(address);
        } catch (UnknownHostException e) {
            logger.error("Error parsing ip address {} ", address, e);
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, e);
        }
        return ipAddress;
    }

    private List<DhcpClientLeaseBlock> getLeaseBlocks(File dhClientFile) throws KuraException {
        List<DhcpClientLeaseBlock> leaseBlocks = new ArrayList<>();
        try (FileReader fr = new FileReader(dhClientFile); BufferedReader br = new BufferedReader(fr)) {
            String line = null;
            List<String> leaseBlock = null;
            while ((line = br.readLine()) != null) {
                if ("lease {".equals(line.trim())) {
                    leaseBlock = new ArrayList<>();
                } else if ("}".equals(line.trim())) {
                    addLeaseBlock(leaseBlock, leaseBlocks);
                    leaseBlock = null;
                } else if ((leaseBlock != null) && !line.trim().isEmpty()) {
                    leaseBlock.add(line.trim());
                }
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }
        return leaseBlocks;
    }

    private void addLeaseBlock(List<String> leaseBlock, List<DhcpClientLeaseBlock> leaseBlocks) {
        try {
            leaseBlocks.add(new DhcpClientLeaseBlock(leaseBlock));
        } catch (KuraException e) {
            logger.error("Failed to add a lease block due to an error parsing dhclient lease file", e);
        }
    }

    private String formGlobalDhclientLeasesFilename() {
        StringBuilder sb = new StringBuilder(GLOBAL_DHCP_LEASES_DIR);
        sb.append('/');
        sb.append(DhcpClientTool.DHCLIENT.getValue());
        sb.append(".leases");
        return sb.toString();
    }

    private String formInterfaceDhclientLeasesFilename(String ifaceName) {
        StringBuilder sb = new StringBuilder(IFACE_DHCP_LEASES_DIR);
        sb.append('/');
        sb.append(DhcpClientTool.DHCLIENT.getValue());
        sb.append('.');
        sb.append(ifaceName);
        sb.append(".leases");
        return sb.toString();
    }
}
