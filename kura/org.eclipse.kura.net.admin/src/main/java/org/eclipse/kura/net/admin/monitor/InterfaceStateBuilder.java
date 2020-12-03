/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.monitor;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.ConnectionInfoImpl;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.linux.net.wifi.HostapdManager;
import org.eclipse.kura.linux.net.wifi.WpaSupplicantManager;
import org.eclipse.kura.net.ConnectionInfo;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceStateBuilder {

    private static final Logger logger = LoggerFactory.getLogger(InterfaceStateBuilder.class);

    private String interfaceName;
    private boolean up;
    protected boolean link;
    private IPAddress ipAddress;
    private int carrierChanges;
    private boolean isL2OnlyInterface;
    private NetInterfaceType type;
    private WifiMode wifiMode;

    private final LinuxNetworkUtil linuxNetworkUtil;
    private final CommandExecutorService executorService;

    public InterfaceStateBuilder(CommandExecutorService executorService) {
        this.linuxNetworkUtil = new LinuxNetworkUtil(executorService);
        this.executorService = executorService;
    }

    public String getName() {
        return this.interfaceName;
    }

    public void setInterfaceName(String name) {
        this.interfaceName = name;
    }

    public boolean isUp() {
        return this.up;
    }

    public void setUp(boolean up) {
        this.up = up;
    }

    public boolean isLink() {
        return this.link;
    }

    public void setLink(boolean link) {
        this.link = link;
    }

    public IPAddress getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(IPAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isL2OnlyInterface() {
        return this.isL2OnlyInterface;
    }

    public void setL2OnlyInterface(boolean isL2OnlyInterface) {
        this.isL2OnlyInterface = isL2OnlyInterface;
    }

    public WifiMode getWifiMode() {
        return this.wifiMode;
    }

    public void setWifiMode(WifiMode wifiMode) {
        this.wifiMode = wifiMode;
    }

    public int getCarrierChanges() {
        return this.carrierChanges;
    }

    public void setCarrierChanges(int carrierChanges) {
        this.carrierChanges = carrierChanges;
    }

    public static Logger getLogger() {
        return logger;
    }

    public NetInterfaceType getType() {
        return this.type;
    }

    public void setType(NetInterfaceType type) {
        this.type = type;
    }

    public InterfaceState buildInterfaceState() throws KuraException {
        if (this.interfaceName == null || this.interfaceName.isEmpty() || this.ipAddress == null && this.type == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "Missing arguments");
        }
        if (this.type != null) {
            this.up = this.isL2OnlyInterface ? this.linuxNetworkUtil.isUp(this.interfaceName)
                    : this.linuxNetworkUtil.hasAddress(this.interfaceName);
            this.link = this.linuxNetworkUtil.isLinkUp(this.type, this.interfaceName);
            logger.debug("InterfaceState() :: {} - link?={}", this.interfaceName, this.link);
            logger.debug("InterfaceState() :: {} - up?={}", this.interfaceName, this.up);
            ConnectionInfo connInfo = new ConnectionInfoImpl(this.interfaceName);
            this.ipAddress = connInfo.getIpAddress();
            this.carrierChanges = this.linuxNetworkUtil.getCarrierChanges(this.interfaceName);
        }
        return new InterfaceState(this.interfaceName, this.up, this.link, this.ipAddress, this.carrierChanges);
    }

    public WifiInterfaceState buildWifiInterfaceState() throws KuraException {
        if (this.interfaceName == null || this.interfaceName.isEmpty() || this.wifiMode == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "Missing arguments");
        }
        this.up = this.isL2OnlyInterface ? this.linuxNetworkUtil.isUp(this.interfaceName)
                : this.linuxNetworkUtil.hasAddress(this.interfaceName);
        this.link = this.linuxNetworkUtil.isLinkUp(NetInterfaceType.WIFI, this.interfaceName);
        logger.debug("InterfaceState() :: {} - link?={}", this.interfaceName, this.link);
        logger.debug("InterfaceState() :: {} - up?={}", this.interfaceName, this.up);
        ConnectionInfo connInfo = new ConnectionInfoImpl(this.interfaceName);
        this.ipAddress = connInfo.getIpAddress();
        setWifiLinkState(this.interfaceName, this.wifiMode);
        this.carrierChanges = this.linuxNetworkUtil.getCarrierChanges(this.interfaceName);
        return new WifiInterfaceState(this.interfaceName, this.up, this.link, this.ipAddress, this.carrierChanges);
    }

    private void setWifiLinkState(String interfaceName, WifiMode wifiMode) throws KuraException {
        if (this.link) {
            if (WifiMode.MASTER.equals(wifiMode)) {
                boolean isHostapdRunning = new HostapdManager(this.executorService).isRunning(interfaceName);
                boolean isIfaceInApMode = WifiMode.MASTER.equals(this.linuxNetworkUtil.getWifiMode(interfaceName));
                if (!isHostapdRunning || !isIfaceInApMode) {
                    logger.warn(
                            "setWifiLinkState() :: !! Link is down for the {} interface. isHostapdRunning? {} isIfaceInApMode? {}",
                            interfaceName, isHostapdRunning, isIfaceInApMode);
                    this.link = false;
                }
            } else if (WifiMode.INFRA.equals(wifiMode)) {
                boolean isSupplicantRunning = new WpaSupplicantManager(this.executorService).isRunning(interfaceName);
                boolean isIfaceInManagedMode = WifiMode.INFRA.equals(this.linuxNetworkUtil.getWifiMode(interfaceName));
                if (!isSupplicantRunning || !isIfaceInManagedMode) {
                    logger.warn(
                            "setWifiLinkState() :: !! Link is down for the {} interface. isSupplicantRunning? {} isIfaceInManagedMode? {} ",
                            interfaceName, isSupplicantRunning, isIfaceInManagedMode);
                    this.link = false;
                }
            }
        }
    }
}
