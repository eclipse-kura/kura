/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.monitor;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.linux.net.wifi.HostapdManager;
import org.eclipse.kura.linux.net.wifi.WpaSupplicantManager;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WifiInterfaceState extends InterfaceState {

    private static final Logger logger = LoggerFactory.getLogger(WifiInterfaceState.class);

    /**
     * WifiInterfaceState
     *
     * @param interfaceName
     *            - interface name as {@link String}
     * @param wifiMode
     *            configured wifi mode as {@link WifiMode}
     * @throws KuraException
     */
    public WifiInterfaceState(String interfaceName, WifiMode wifiMode) throws KuraException {
        super(NetInterfaceType.WIFI, interfaceName);
        setWifiLinkState(interfaceName, wifiMode);
    }

    /**
     * WifiInterfaceState
     *
     * @param interfaceName
     *            - interface name as {@link String}
     * @param wifiMode
     *            - configured wifi mode as {@link WifiMode}
     * @param isL2OnlyInterface
     *            - is Layer 2 only interface
     * @throws KuraException
     */
    public WifiInterfaceState(String interfaceName, WifiMode wifiMode, boolean isL2OnlyInterface) throws KuraException {
        super(NetInterfaceType.WIFI, interfaceName, isL2OnlyInterface);
        setWifiLinkState(interfaceName, wifiMode);
    }

    private void setWifiLinkState(String interfaceName, WifiMode wifiMode) throws KuraException {
        if (this.link) {
            if (WifiMode.MASTER.equals(wifiMode)) {
                boolean isHostapdRunning = HostapdManager.isRunning(interfaceName);
                boolean isIfaceInApMode = WifiMode.MASTER.equals(LinuxNetworkUtil.getWifiMode(interfaceName));
                if (!isHostapdRunning || !isIfaceInApMode) {
                    logger.warn("setWifiLinkState() :: !! Link is down for the " + interfaceName
                            + " interface. isHostapdRunning? " + isHostapdRunning + " isIfaceInApMode? "
                            + isIfaceInApMode);
                    this.link = false;
                }
            } else if (WifiMode.INFRA.equals(wifiMode)) {
                boolean isSupplicantRunning = WpaSupplicantManager.isRunning(interfaceName);
                boolean isIfaceInManagedMode = WifiMode.INFRA.equals(LinuxNetworkUtil.getWifiMode(interfaceName));
                if (!isSupplicantRunning || !isIfaceInManagedMode) {
                    logger.warn("setWifiLinkState() :: !! Link is down for the " + interfaceName
                            + " interface. isSupplicantRunning? " + isSupplicantRunning + " isIfaceInManagedMode? "
                            + isIfaceInManagedMode);
                    this.link = false;
                }
            }
        }
    }
}