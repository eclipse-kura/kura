/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.shared.service;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtBSNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtBSFirewallNatEntry;
import org.eclipse.kura.web.shared.model.GwtBSFirewallOpenPortEntry;
import org.eclipse.kura.web.shared.model.GwtBSFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtBSWifiConfig;
import org.eclipse.kura.web.shared.model.GwtBSWifiHotspotEntry;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("network")
public interface GwtBSNetworkService extends RemoteService
{
	public ArrayList<GwtBSNetInterfaceConfig> findNetInterfaceConfigurations() throws GwtKuraException;

	public void updateNetInterfaceConfigurations(GwtBSNetInterfaceConfig config) throws GwtKuraException;
		
	public ArrayList<GwtBSFirewallOpenPortEntry> findDeviceFirewallOpenPorts() throws GwtKuraException;

	public void updateDeviceFirewallOpenPorts(List<GwtBSFirewallOpenPortEntry> entries) throws GwtKuraException;
		
	public ArrayList<GwtBSFirewallPortForwardEntry> findDeviceFirewallPortForwards() throws GwtKuraException;
	
	public ArrayList<GwtBSFirewallNatEntry> findDeficeFirewallNATs() throws GwtKuraException;
	
	public void updateDeviceFirewallPortForwards(List<GwtBSFirewallPortForwardEntry> entries) throws GwtKuraException;
	
	public void updateDeviceFirewallNATs(List<GwtBSFirewallNatEntry> entries) throws GwtKuraException;
	
	public void renewDhcpLease(String interfaceName) throws GwtKuraException;
	
	public ArrayList<GwtBSWifiHotspotEntry> findWifiHotspots(String interfaceName) throws GwtKuraException;
	
	public boolean verifyWifiCredentials(String interfaceName, GwtBSWifiConfig gwtWifiConfig) throws GwtKuraException;

	public void rollbackDefaultConfiguration();
}
