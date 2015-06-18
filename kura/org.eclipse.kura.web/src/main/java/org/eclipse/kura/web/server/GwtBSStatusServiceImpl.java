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
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtBSGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtBSModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtBSNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtBSNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtBSNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtBSNetIfType;
import org.eclipse.kura.web.shared.model.GwtBSNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtBSWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtBSWifiWirelessMode;
import org.eclipse.kura.web.shared.service.GwtBSStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtBSStatusServiceImpl extends OsgiRemoteServiceServlet implements GwtBSStatusService {

	private static final long serialVersionUID = 8256280782910423734L;
	
	private static Logger s_logger = LoggerFactory.getLogger(GwtBSNetworkServiceImpl.class);
	
	public ArrayList<GwtBSGroupedNVPair> getDeviceConfig(boolean hasNetAdmin) throws GwtKuraException {
		ArrayList<GwtBSGroupedNVPair> pairs = new ArrayList<GwtBSGroupedNVPair>();

		pairs.addAll(getCloudStatus());
		if (hasNetAdmin)
			pairs.addAll(getNetworkStatus());
		pairs.addAll(getPositionStatus());

		s_logger.debug("Status service returning "+pairs.size()+"rows.");
		return pairs;
	}
	
	private List<GwtBSGroupedNVPair> getCloudStatus() {
		List<GwtBSGroupedNVPair> pairs = new ArrayList<GwtBSGroupedNVPair>();
		
		try {
		DataService dataService = ServiceLocator.getInstance().getService(DataService.class);
		DataTransportService dataTransportService = ServiceLocator.getInstance().getService(DataTransportService.class);
		if (dataService != null) {
			pairs.add(new GwtBSGroupedNVPair("cloudStatus", "Connection Status", dataService.isConnected() ? "CONNECTED" : "DISCONNECTED"));
			pairs.add(new GwtBSGroupedNVPair("cloudStatus", "Auto-connect", dataService.isAutoConnectEnabled() ? "ON (Retry Interval is " + Integer.toString(dataService.getRetryInterval()) + "s)": "OFF"));
		}
		if (dataTransportService != null) {
			pairs.add(new GwtBSGroupedNVPair("cloudStatus", "Broker URL", dataTransportService.getBrokerUrl()));
			pairs.add(new GwtBSGroupedNVPair("cloudStatus", "Account", dataTransportService.getAccountName()));
			pairs.add(new GwtBSGroupedNVPair("cloudStatus", "Username", dataTransportService.getUsername()));
			pairs.add(new GwtBSGroupedNVPair("cloudStatus", "Client ID", dataTransportService.getClientId()));
		}
		} catch (GwtKuraException e) {
			s_logger.debug(e.getMessage());
		}
		
		return pairs;
	}
	
	private List<GwtBSGroupedNVPair> getNetworkStatus() {
		List<GwtBSGroupedNVPair> pairs = new ArrayList<GwtBSGroupedNVPair>();
		String nl = "<br />";
		String tab = "&nbsp&nbsp&nbsp&nbsp";
		
		GwtBSNetworkServiceImpl gwtBSNetworkService = new GwtBSNetworkServiceImpl();

		try {
			List<GwtBSNetInterfaceConfig> gwtBSNetInterfaceConfigs = gwtBSNetworkService.findNetInterfaceConfigurations();
			for (GwtBSNetInterfaceConfig gwtBSNetInterfaceConfig : gwtBSNetInterfaceConfigs) {
				
				String currentAddress    = gwtBSNetInterfaceConfig.getIpAddress();
				String currentSubnetMask = gwtBSNetInterfaceConfig.getSubnetMask();
				String currentStatus     = (gwtBSNetInterfaceConfig.getStatusEnum() == GwtBSNetIfStatus.netIPv4StatusDisabled ? "Disabled" : (gwtBSNetInterfaceConfig.getStatusEnum() == GwtBSNetIfStatus.netIPv4StatusEnabledLAN ? "LAN" : "WAN"));
				String currentConfigMode = gwtBSNetInterfaceConfig.getConfigModeEnum() == GwtBSNetIfConfigMode.netIPv4ConfigModeDHCP ? "DHCP" : "Manual";
				String currentRouterMode;
				if (gwtBSNetInterfaceConfig.getRouterModeEnum() == GwtBSNetRouterMode.netRouterDchp)
					currentRouterMode = "DHCPD";
				else if (gwtBSNetInterfaceConfig.getRouterModeEnum() == GwtBSNetRouterMode.netRouterNat)
					currentRouterMode = "NAT";
				else if (gwtBSNetInterfaceConfig.getRouterModeEnum() == GwtBSNetRouterMode.netRouterDchpNat)
					currentRouterMode = "DHCPD & NAT";
				else
					currentRouterMode = "";
						
				if (gwtBSNetInterfaceConfig.getHwTypeEnum() == GwtBSNetIfType.ETHERNET) {
					if (currentStatus.equals("Disabled"))
						pairs.add(new GwtBSGroupedNVPair("networkStatusEthernet", gwtBSNetInterfaceConfig.getName(), currentStatus));
					else
						pairs.add(new GwtBSGroupedNVPair("networkStatusEthernet", gwtBSNetInterfaceConfig.getName(), currentAddress + nl + tab +
																												"Subnet Mask: " + currentSubnetMask + nl + tab +
																												"Mode: " + currentStatus + nl + tab +
																												"IP Acquisition: " + currentConfigMode + nl + tab +
																												"Router Mode: " + currentRouterMode));
				}
				else if (gwtBSNetInterfaceConfig.getHwTypeEnum() == GwtBSNetIfType.WIFI && !gwtBSNetInterfaceConfig.getName().startsWith("mon")) {
					String currentWifiMode = ((GwtBSWifiNetInterfaceConfig)gwtBSNetInterfaceConfig).getWirelessModeEnum() == GwtBSWifiWirelessMode.netWifiWirelessModeStation ? "Station Mode" : "Access Point";
					String currentWifiSsid = ((GwtBSWifiNetInterfaceConfig)gwtBSNetInterfaceConfig).getActiveWifiConfig().getWirelessSsid();
					if (currentStatus.equals("Disabled"))
						pairs.add(new GwtBSGroupedNVPair("networkStatusWifi", gwtBSNetInterfaceConfig.getName(), currentStatus));
					else
						pairs.add(new GwtBSGroupedNVPair("networkStatusWifi", gwtBSNetInterfaceConfig.getName(), currentAddress + nl + tab +
																											 "Subnet Mask: " + currentSubnetMask + nl + tab +
																											 "Mode: " + currentStatus + nl + tab +
																											 "IP Acquisition: " + currentConfigMode + nl + tab +
																											 "Router Mode: " + currentRouterMode + nl + tab +
																											 "Wireless Mode:" + currentWifiMode + nl + tab +
																											 "SSID: " + currentWifiSsid + nl));
				}
				else if (gwtBSNetInterfaceConfig.getHwTypeEnum() == GwtBSNetIfType.MODEM) {
					String currentModemApn = ((GwtBSModemInterfaceConfig)gwtBSNetInterfaceConfig).getApn();
					String currentModemPppNum = Integer.toString(((GwtBSModemInterfaceConfig)gwtBSNetInterfaceConfig).getPppNum());
					if (currentStatus.equals("Disabled"))
						pairs.add(new GwtBSGroupedNVPair("networkStatusModem", gwtBSNetInterfaceConfig.getName(), currentStatus));
					else
						pairs.add(new GwtBSGroupedNVPair("networkStatusModem", gwtBSNetInterfaceConfig.getName(), currentAddress + nl +
																											 "Subnet Mask: " + currentSubnetMask + nl + tab +
																											 "Mode: " + currentStatus + nl + tab +
																											 "IP Acquisition: " + currentConfigMode + nl + tab +
																											 "APN: " + currentModemApn + nl + tab +
																											 "PPP: " + currentModemPppNum));
				}
			}
		} catch (GwtKuraException e) {
			s_logger.debug(e.getMessage());
		}

		return pairs;
		
	}
	
	private List<GwtBSGroupedNVPair> getPositionStatus() {
		List<GwtBSGroupedNVPair> pairs = new ArrayList<GwtBSGroupedNVPair>();
		
		try {
			PositionService positionService = ServiceLocator.getInstance().getService(PositionService.class);
			
			if (positionService != null) {
				pairs.add(new GwtBSGroupedNVPair("positionStatus", "Longitude", positionService.getPosition().getLongitude().toString()));
				pairs.add(new GwtBSGroupedNVPair("positionStatus", "Latitude", positionService.getPosition().getLatitude().toString()));
				pairs.add(new GwtBSGroupedNVPair("positionStatus", "Altitude", positionService.getPosition().getAltitude().toString()));
			}
			
		} catch (GwtKuraException e) {
			s_logger.debug(e.getMessage());
		}
		
		return pairs;
	}

}