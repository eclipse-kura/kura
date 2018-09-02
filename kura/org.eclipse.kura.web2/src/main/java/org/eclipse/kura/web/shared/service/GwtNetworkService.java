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
package org.eclipse.kura.web.shared.service;

import java.util.List;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtFirewallNatEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtModemPdpEntry;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiHotspotEntry;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("network")
public interface GwtNetworkService extends RemoteService {

    public List<GwtNetInterfaceConfig> findNetInterfaceConfigurations() throws GwtKuraException;

    public void updateNetInterfaceConfigurations(GwtXSRFToken xsrfToken, GwtNetInterfaceConfig config)
            throws GwtKuraException;

    public List<GwtFirewallOpenPortEntry> findDeviceFirewallOpenPorts(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public void updateDeviceFirewallOpenPorts(GwtXSRFToken xsrfToken, List<GwtFirewallOpenPortEntry> entries)
            throws GwtKuraException;

    public List<GwtFirewallPortForwardEntry> findDeviceFirewallPortForwards(GwtXSRFToken xsrfToken)
            throws GwtKuraException;

    public List<GwtFirewallNatEntry> findDeviceFirewallNATs(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public void updateDeviceFirewallPortForwards(GwtXSRFToken xsrfToken, List<GwtFirewallPortForwardEntry> entries)
            throws GwtKuraException;

    public void updateDeviceFirewallNATs(GwtXSRFToken xsrfToken, List<GwtFirewallNatEntry> entries)
            throws GwtKuraException;

    public void renewDhcpLease(GwtXSRFToken xsrfToken, String interfaceName) throws GwtKuraException;

    public List<GwtWifiHotspotEntry> findWifiHotspots(GwtXSRFToken xsrfToken, String interfaceName, String wirelessSsid)
            throws GwtKuraException;

    public boolean verifyWifiCredentials(GwtXSRFToken xsrfToken, String interfaceName, GwtWifiConfig gwtWifiConfig)
            throws GwtKuraException;

    public List<GwtModemPdpEntry> findPdpContextInfo(GwtXSRFToken xsrfToken, String interfaceName)
            throws GwtKuraException;
}
