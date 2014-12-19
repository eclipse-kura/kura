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
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;
import org.eclipse.kura.web.shared.service.GwtStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.extjs.gxt.ui.client.data.ListLoadResult;

public class GwtStatusServiceImpl extends OsgiRemoteServiceServlet implements GwtStatusService {

	private static final long serialVersionUID = 8256280782910423734L;
	
	private static Logger s_logger = LoggerFactory.getLogger(GwtNetworkServiceImpl.class);
	
	public ListLoadResult<GwtGroupedNVPair> getDeviceConfig(boolean hasNetAdmin) throws GwtKuraException {
		List<GwtGroupedNVPair> pairs = new ArrayList<GwtGroupedNVPair>();

		pairs.addAll(getCloudStatus());
		if (hasNetAdmin)
			pairs.addAll(getNetworkStatus());
		pairs.addAll(getPositionStatus());

		return new BaseListLoadResult<GwtGroupedNVPair>(pairs);
	}
	
	private List<GwtGroupedNVPair> getCloudStatus() {
		List<GwtGroupedNVPair> pairs = new ArrayList<GwtGroupedNVPair>();
		
		try {
		DataService dataService = ServiceLocator.getInstance().getService(DataService.class);
		DataTransportService dataTransportService = ServiceLocator.getInstance().getService(DataTransportService.class);
		if (dataService != null) {
			pairs.add(new GwtGroupedNVPair("cloudStatus", "Connection Status", dataService.isConnected() ? "CONNECTED" : "DISCONNECTED"));
			pairs.add(new GwtGroupedNVPair("cloudStatus", "Auto-connect", dataService.isAutoConnectEnabled() ? "ON (Retry Interval is " + Integer.toString(dataService.getRetryInterval()) + "s)": "OFF"));
		}
		if (dataTransportService != null) {
			pairs.add(new GwtGroupedNVPair("cloudStatus", "Broker URL", dataTransportService.getBrokerUrl()));
			pairs.add(new GwtGroupedNVPair("cloudStatus", "Account", dataTransportService.getAccountName()));
			pairs.add(new GwtGroupedNVPair("cloudStatus", "Username", dataTransportService.getUsername()));
			pairs.add(new GwtGroupedNVPair("cloudStatus", "Client ID", dataTransportService.getClientId()));
		}
		} catch (GwtKuraException e) {
			s_logger.debug(e.getMessage());
		}
		
		return pairs;
	}
	
	private List<GwtGroupedNVPair> getNetworkStatus() {
		List<GwtGroupedNVPair> pairs = new ArrayList<GwtGroupedNVPair>();
		String nl = "<br />";
		String tab = "&nbsp&nbsp&nbsp&nbsp";
		
		GwtNetworkServiceImpl gwtNetworkService = new GwtNetworkServiceImpl();

		try {
			List<GwtNetInterfaceConfig> gwtNetInterfaceConfigs = gwtNetworkService.findNetInterfaceConfigurations().getData();
			for (GwtNetInterfaceConfig gwtNetInterfaceConfig : gwtNetInterfaceConfigs) {
				
				String currentAddress    = gwtNetInterfaceConfig.getIpAddress();
				String currentSubnetMask = gwtNetInterfaceConfig.getSubnetMask();
				String currentStatus     = (gwtNetInterfaceConfig.getStatusEnum() == GwtNetIfStatus.netIPv4StatusDisabled ? "Disabled" : (gwtNetInterfaceConfig.getStatusEnum() == GwtNetIfStatus.netIPv4StatusEnabledLAN ? "LAN" : "WAN"));
				String currentConfigMode = gwtNetInterfaceConfig.getConfigModeEnum() == GwtNetIfConfigMode.netIPv4ConfigModeDHCP ? "DHCP" : "Manual";
				String currentRouterMode;
				if (gwtNetInterfaceConfig.getRouterModeEnum() == GwtNetRouterMode.netRouterDchp)
					currentRouterMode = "DHCPD";
				else if (gwtNetInterfaceConfig.getRouterModeEnum() == GwtNetRouterMode.netRouterNat)
					currentRouterMode = "NAT";
				else if (gwtNetInterfaceConfig.getRouterModeEnum() == GwtNetRouterMode.netRouterDchpNat)
					currentRouterMode = "DHCPD & NAT";
				else
					currentRouterMode = "";
						
				if (gwtNetInterfaceConfig.getHwTypeEnum() == GwtNetIfType.ETHERNET) {
					if (currentStatus.equals("Disabled"))
						pairs.add(new GwtGroupedNVPair("networkStatusEthernet", gwtNetInterfaceConfig.getName(), currentStatus));
					else
						pairs.add(new GwtGroupedNVPair("networkStatusEthernet", gwtNetInterfaceConfig.getName(), currentAddress + nl + tab +
																												"Subnet Mask: " + currentSubnetMask + nl + tab +
																												"Mode: " + currentStatus + nl + tab +
																												"IP Acquisition: " + currentConfigMode + nl + tab +
																												"Router Mode: " + currentRouterMode));
				}
				else if (gwtNetInterfaceConfig.getHwTypeEnum() == GwtNetIfType.WIFI && !gwtNetInterfaceConfig.getName().startsWith("mon")) {
					String currentWifiMode = ((GwtWifiNetInterfaceConfig)gwtNetInterfaceConfig).getWirelessModeEnum() == GwtWifiWirelessMode.netWifiWirelessModeStation ? "Station Mode" : "Access Point";
					String currentWifiSsid = ((GwtWifiNetInterfaceConfig)gwtNetInterfaceConfig).getActiveWifiConfig().getWirelessSsid();
					if (currentStatus.equals("Disabled"))
						pairs.add(new GwtGroupedNVPair("networkStatusWifi", gwtNetInterfaceConfig.getName(), currentStatus));
					else
						pairs.add(new GwtGroupedNVPair("networkStatusWifi", gwtNetInterfaceConfig.getName(), currentAddress + nl + tab +
																											 "Subnet Mask: " + currentSubnetMask + nl + tab +
																											 "Mode: " + currentStatus + nl + tab +
																											 "IP Acquisition: " + currentConfigMode + nl + tab +
																											 "Router Mode: " + currentRouterMode + nl + tab +
																											 "Wireless Mode:" + currentWifiMode + nl + tab +
																											 "SSID: " + currentWifiSsid + nl));
				}
				else if (gwtNetInterfaceConfig.getHwTypeEnum() == GwtNetIfType.MODEM) {
					String currentModemApn = ((GwtModemInterfaceConfig)gwtNetInterfaceConfig).getApn();
					String currentModemPppNum = Integer.toString(((GwtModemInterfaceConfig)gwtNetInterfaceConfig).getPppNum());
					if (currentStatus.equals("Disabled"))
						pairs.add(new GwtGroupedNVPair("networkStatusModem", gwtNetInterfaceConfig.getName(), currentStatus));
					else
						pairs.add(new GwtGroupedNVPair("networkStatusModem", gwtNetInterfaceConfig.getName(), currentAddress + nl +
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
	
	private List<GwtGroupedNVPair> getPositionStatus() {
		List<GwtGroupedNVPair> pairs = new ArrayList<GwtGroupedNVPair>();
		
		try {
			PositionService positionService = ServiceLocator.getInstance().getService(PositionService.class);
			
			if (positionService != null) {
				pairs.add(new GwtGroupedNVPair("positionStatus", "Longitude", positionService.getPosition().getLongitude().toString()));
				pairs.add(new GwtGroupedNVPair("positionStatus", "Latitude", positionService.getPosition().getLatitude().toString()));
				pairs.add(new GwtGroupedNVPair("positionStatus", "Altitude", positionService.getPosition().getAltitude().toString()));
			}
			
		} catch (GwtKuraException e) {
			s_logger.debug(e.getMessage());
		}
		
		return pairs;
	}

}
