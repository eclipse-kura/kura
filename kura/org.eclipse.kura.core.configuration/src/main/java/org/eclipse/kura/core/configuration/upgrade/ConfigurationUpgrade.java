/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.configuration.upgrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;

public class ConfigurationUpgrade {
	
	private static final String CLOUD_SERVICE_FACTORY_PID = "org.eclipse.kura.cloud.CloudService";
	private static final String DATA_SERVICE_FACTORY_PID = "org.eclipse.kura.data.DataService";
	private static final String DATA_TRANSPORT_SERVICE_FACTORY_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";
	private static final String CLOUD_CONNECTION_STATUS_SERVICE_FACTORY_PID = "org.eclipse.kura.status.CloudConnectionStatusService";
	
	private static final String CLOUD_SERVICE_PID = "org.eclipse.kura.cloud.CloudService";
	private static final String DATA_SERVICE_PID = "org.eclipse.kura.data.DataService";
	private static final String DATA_TRANSPORT_SERVICE_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";
	private static final String CLOUD_CONNECTION_STATUS_SERVICE_PID = "org.eclipse.kura.status.CloudConnectionStatusService";
	
	private static final String DATA_SERVICE_REFERENCE_NAME = "DataService";
	private static final String DATA_TRANSPORT_SERVICE_REFERENCE_NAME = "DataTransportService";		
	private static final String REFERENCE_TARGET_VALUE_FORMAT = "("+ConfigurationService.KURA_SERVICE_PID+"=%s)";

	
	public static XmlComponentConfigurations upgrade(XmlComponentConfigurations xmlConfigs) {
		List<ComponentConfigurationImpl> result = new ArrayList<ComponentConfigurationImpl>();
		
		boolean CloudConnectionStatusConfigFound = false;
		for (ComponentConfiguration config : xmlConfigs.getConfigurations()) {
			String pid = config.getPid();
			Map<String, Object> props = new HashMap<String, Object>(config.getConfigurationProperties());
			ComponentConfigurationImpl cc = new ComponentConfigurationImpl(pid, (Tocd) config.getDefinition(), props);
			result.add(cc);
			
			if (CLOUD_SERVICE_PID.equals(pid)) {
				props.put(ConfigurationAdmin.SERVICE_FACTORYPID, CLOUD_SERVICE_FACTORY_PID);
				String name = DATA_SERVICE_REFERENCE_NAME+ComponentConstants.REFERENCE_TARGET_SUFFIX;
				props.put(name, String.format(REFERENCE_TARGET_VALUE_FORMAT, DATA_SERVICE_PID));
			} else if (DATA_SERVICE_PID.equals(pid)) {
				props.put(ConfigurationAdmin.SERVICE_FACTORYPID, DATA_SERVICE_FACTORY_PID);
				String name = DATA_TRANSPORT_SERVICE_REFERENCE_NAME+ComponentConstants.REFERENCE_TARGET_SUFFIX;
				props.put(name, String.format(REFERENCE_TARGET_VALUE_FORMAT, DATA_TRANSPORT_SERVICE_PID));
			} else if (DATA_TRANSPORT_SERVICE_PID.equals(pid)) {
				props.put(ConfigurationAdmin.SERVICE_FACTORYPID, DATA_TRANSPORT_SERVICE_FACTORY_PID);
			} else if (CLOUD_CONNECTION_STATUS_SERVICE_PID.equals(pid)){
				CloudConnectionStatusConfigFound = true;
			}
		}
		
		if(!CloudConnectionStatusConfigFound){
			Map<String, Object> props = new HashMap<String, Object>();
			props.put(ConfigurationAdmin.SERVICE_FACTORYPID, CLOUD_CONNECTION_STATUS_SERVICE_FACTORY_PID);
			ComponentConfigurationImpl cc = new ComponentConfigurationImpl(CLOUD_CONNECTION_STATUS_SERVICE_PID, new Tocd(), props);
			result.add(cc);
		}
		
		XmlComponentConfigurations xmlConfigurations = new XmlComponentConfigurations();
		xmlConfigurations.setConfigurations(result);
		return xmlConfigurations;
	}
}
