/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtFirewallNatEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtModemPdpEntry;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiChannelFrequency;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiHotspotEntry;
import org.eclipse.kura.web.shared.model.GwtWifiRadioMode;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtNetworkService;

public class GwtNetworkServiceImplFacade extends OsgiRemoteServiceServlet implements GwtNetworkService {

    private static final long serialVersionUID = -4188750359099902616L;

    @Override
    public List<GwtNetInterfaceConfig> findNetInterfaceConfigurations(boolean recompute)
            throws GwtKuraException {

        if (isNet2()) {
            return org.eclipse.kura.web.server.net2.GwtNetworkServiceImpl.findNetInterfaceConfigurations(recompute);
        } else {
            return org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.findNetInterfaceConfigurations(recompute);
        }
    }

    @Override
    public void updateNetInterfaceConfigurations(GwtXSRFToken xsrfToken, GwtNetInterfaceConfig config)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        if (isNet2()) {
            org.eclipse.kura.web.server.net2.GwtNetworkServiceImpl.updateNetInterfaceConfigurations(config);
        } else {
            org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.updateNetInterfaceConfigurations(config);
        }
    }

    @Override
    public ArrayList<GwtFirewallOpenPortEntry> findDeviceFirewallOpenPorts(GwtXSRFToken xsrfToken)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        if (isNet2()) {
            return (ArrayList<GwtFirewallOpenPortEntry>) org.eclipse.kura.web.server.net2.GwtNetworkServiceImpl
                    .findDeviceFirewallOpenPorts();
        } else {
            return org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.findDeviceFirewallOpenPorts();
        }
    }

    @Override
    public ArrayList<GwtWifiHotspotEntry> findWifiHotspots(GwtXSRFToken xsrfToken, String interfaceName,
            String wirelessSsid) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        if (isNet2()) {
            // TODO
            return new ArrayList<>();
        } else {
            return org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.findWifiHotspots(interfaceName, wirelessSsid);
        }
    }

    @Override
    public List<GwtModemPdpEntry> findPdpContextInfo(GwtXSRFToken xsrfToken, String interfaceName)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        if (isNet2()) {
            // TODO
            return new ArrayList<>();
        } else {
            return org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.findPdpContextInfo(interfaceName);
        }
    }

    @Override
    public boolean verifyWifiCredentials(GwtXSRFToken xsrfToken, String interfaceName, GwtWifiConfig gwtWifiConfig)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        if (isNet2()) {
            // TODO
            return false;
        } else {
            return org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.verifyWifiCredentials(interfaceName,
                    gwtWifiConfig);
        }
    }

    @Override
    public ArrayList<GwtFirewallPortForwardEntry> findDeviceFirewallPortForwards(GwtXSRFToken xsrfToken)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        if (isNet2()) {
            return (ArrayList<GwtFirewallPortForwardEntry>) org.eclipse.kura.web.server.net2.GwtNetworkServiceImpl
                    .findDeviceFirewallPortForwards();
        } else {
            return org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.findDeviceFirewallPortForwards();
        }
    }

    @Override
    public ArrayList<GwtFirewallNatEntry> findDeviceFirewallNATs(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        if (isNet2()) {
            return (ArrayList<GwtFirewallNatEntry>) org.eclipse.kura.web.server.net2.GwtNetworkServiceImpl
                    .findDeviceFirewallNATs();
        } else {
            return org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.findDeviceFirewallNATs();
        }
    }

    @Override
    public void updateDeviceFirewallOpenPorts(GwtXSRFToken xsrfToken, List<GwtFirewallOpenPortEntry> entries)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.updateDeviceFirewallOpenPorts(entries);
    }

    @Override
    public void updateDeviceFirewallPortForwards(GwtXSRFToken xsrfToken, List<GwtFirewallPortForwardEntry> entries)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.updateDeviceFirewallPortForwards(entries);
    }

    @Override
    public void updateDeviceFirewallNATs(GwtXSRFToken xsrfToken, List<GwtFirewallNatEntry> entries)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.updateDeviceFirewallNATs(entries);
    }

    @Override
    public void renewDhcpLease(GwtXSRFToken xsrfToken, String interfaceName) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        if (isNet2()) {
            // TODO
        } else {
            org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.renewDhcpLease(interfaceName);
        }
    }

    @Override
    public List<GwtWifiChannelFrequency> findFrequencies(GwtXSRFToken xsrfToken, String interfaceName,
            GwtWifiRadioMode radioMode) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        if (isNet2()) {
            // TODO
            return new ArrayList<>();
        } else {
            return org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.findFrequencies(interfaceName, radioMode);
        }
    }

    @Override
    public String getWifiCountryCode(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        if (isNet2()) {
            // TODO
            return "";
        } else {
            return org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.getWifiCountryCode();
        }
    }

    @Override
    public boolean isIEEE80211ACSupported(GwtXSRFToken xsrfToken, String ifaceName) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        if (isNet2()) {
            // TODO
            return false;
        } else {
            return org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.isIEEE80211ACSupported(ifaceName);
        }
    }

    @Override
    public List<String> getDhcpLeases(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        if (isNet2()) {
            // TODO
            return new ArrayList<>();
        } else {
            return org.eclipse.kura.web.server.net.GwtNetworkServiceImpl.getDhcpLeases();
        }
    }

    @Override
    public boolean isNet2() {
        try {
            ServiceLocator.getInstance().getService(NetworkAdminService.class);
            return false;
        } catch (GwtKuraException networkAdminServiceNotFound) {
            return true;
        }
    }

}
