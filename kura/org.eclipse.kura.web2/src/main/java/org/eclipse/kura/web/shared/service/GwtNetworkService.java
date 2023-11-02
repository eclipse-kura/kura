/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.service;

import java.util.List;

import org.eclipse.kura.web.server.Audit;
import org.eclipse.kura.web.server.RequiredPermissions;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
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

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("network")
@RequiredPermissions(KuraPermission.NETWORK_ADMIN)
public interface GwtNetworkService extends RemoteService {

    public List<GwtNetInterfaceConfig> findNetInterfaceConfigurations(boolean recompute) throws GwtKuraException;

    @Audit(componentName = "UI Network", description = "Update network interface configurations")
    public void updateNetInterfaceConfigurations(GwtXSRFToken xsrfToken, GwtNetInterfaceConfig config)
            throws GwtKuraException;

    public List<GwtFirewallOpenPortEntry> findDeviceFirewallOpenPorts(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public List<GwtFirewallOpenPortEntry> findDeviceFirewallOpenPortsIPv6(GwtXSRFToken xsrfToken)
            throws GwtKuraException;

    public List<GwtFirewallPortForwardEntry> findDeviceFirewallPortForwards(GwtXSRFToken xsrfToken)
            throws GwtKuraException;

    public List<GwtFirewallPortForwardEntry> findDeviceFirewallPortForwardsIPv6(GwtXSRFToken xsrfToken)
            throws GwtKuraException;

    public List<GwtFirewallNatEntry> findDeviceFirewallNATs(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public List<GwtFirewallNatEntry> findDeviceFirewallNATsIPv6(GwtXSRFToken xsrfToken) throws GwtKuraException;

    @Audit(componentName = "UI Network", description = "Update firewall open ports")
    public void updateDeviceFirewallOpenPorts(GwtXSRFToken xsrfToken, List<GwtFirewallOpenPortEntry> entries)
            throws GwtKuraException;

    @Audit(componentName = "UI Network", description = "Update firewall open ports IPv6")
    public void updateDeviceFirewallOpenPortsIPv6(GwtXSRFToken xsrfToken, List<GwtFirewallOpenPortEntry> entries)
            throws GwtKuraException;

    @Audit(componentName = "UI Network", description = "Update firewall open ports")
    public void updateDeviceFirewallPortForwards(GwtXSRFToken xsrfToken, List<GwtFirewallPortForwardEntry> entries)
            throws GwtKuraException;

    @Audit(componentName = "UI Network", description = "Update firewall open ports IPv6")
    public void updateDeviceFirewallPortForwardsIPv6(GwtXSRFToken xsrfToken, List<GwtFirewallPortForwardEntry> entries)
            throws GwtKuraException;

    @Audit(componentName = "UI Network", description = "Update firewall NAT configuration")
    public void updateDeviceFirewallNATs(GwtXSRFToken xsrfToken, List<GwtFirewallNatEntry> entries)
            throws GwtKuraException;

    @Audit(componentName = "UI Network", description = "Update firewall NAT configuration IPv6")
    public void updateDeviceFirewallNATsIPv6(GwtXSRFToken xsrfToken, List<GwtFirewallNatEntry> entries)
            throws GwtKuraException;

    @Audit(componentName = "UI Network", description = "Renew DHCP lease")
    public void renewDhcpLease(GwtXSRFToken xsrfToken, String interfaceName) throws GwtKuraException;

    @Audit(componentName = "UI Network", description = "WiFi scan")
    public List<GwtWifiHotspotEntry> findWifiHotspots(GwtXSRFToken xsrfToken, String interfaceName, String wirelessSsid,
            boolean recompute) throws GwtKuraException;

    @Audit(componentName = "UI Network", description = "Get Wifi channels and frequencies")
    public List<GwtWifiChannelFrequency> findFrequencies(GwtXSRFToken xsrfToken, String interfaceName,
            GwtWifiRadioMode radiomode) throws GwtKuraException;

    @Audit(componentName = "UI Network", description = "Get Wifi Country Code")
    public String getWifiCountryCode(GwtXSRFToken xsrfToken) throws GwtKuraException;

    @Audit(componentName = "UI Network", description = "Verify Wifi credentials")
    public boolean verifyWifiCredentials(GwtXSRFToken xsrfToken, String interfaceName, GwtWifiConfig gwtWifiConfig)
            throws GwtKuraException;

    @Audit(componentName = "UI Network", description = "Detect if using Network Manager or legacy networking implementation")
    public boolean isNet2();

    public List<GwtModemPdpEntry> findPdpContextInfo(GwtXSRFToken xsrfToken, String interfaceName)
            throws GwtKuraException;

    public boolean isIEEE80211ACSupported(GwtXSRFToken xsrfToken, String ifaceName) throws GwtKuraException;

    public List<String> getDhcpLeases(GwtXSRFToken xsrfToken, String interfaceName) throws GwtKuraException;
}
