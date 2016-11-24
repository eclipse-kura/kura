/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
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
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;

public class ConfigurationUpgrade {

    private static final String KURA_CLOUD_SERVICE_FACTORY_PID = "kura.cloud.service.factory.pid";
    private static final String FACTORY_PID = "org.eclipse.kura.core.cloud.factory.DefaultCloudServiceFactory";

    private static final String CLOUD_SERVICE_FACTORY_PID = "org.eclipse.kura.cloud.CloudService";
    private static final String DATA_SERVICE_FACTORY_PID = "org.eclipse.kura.data.DataService";
    private static final String DATA_TRANSPORT_SERVICE_FACTORY_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";

    private static final String CLOUD_SERVICE_PID = "org.eclipse.kura.cloud.CloudService";
    private static final String DATA_SERVICE_PID = "org.eclipse.kura.data.DataService";
    private static final String DATA_TRANSPORT_SERVICE_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";

    private static final String DATA_SERVICE_REFERENCE_NAME = "DataService";
    private static final String DATA_TRANSPORT_SERVICE_REFERENCE_NAME = "DataTransportService";
    private static final String REFERENCE_TARGET_VALUE_FORMAT = "(" + ConfigurationService.KURA_SERVICE_PID + "=%s)";

    public static XmlComponentConfigurations upgrade(XmlComponentConfigurations xmlConfigs) {
        List<ComponentConfiguration> result = new ArrayList<ComponentConfiguration>();

        for (ComponentConfiguration config : xmlConfigs.getConfigurations()) {
            String pid = config.getPid();
            Map<String, Object> props = new HashMap<String, Object>(config.getConfigurationProperties());
            ComponentConfigurationImpl cc = new ComponentConfigurationImpl(pid, config.getDefinition(), props);
            result.add(cc);

            if (CLOUD_SERVICE_PID.equals(pid)) {
                props.put(ConfigurationAdmin.SERVICE_FACTORYPID, CLOUD_SERVICE_FACTORY_PID);
                String name = DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
                props.put(name, String.format(REFERENCE_TARGET_VALUE_FORMAT, DATA_SERVICE_PID));
                props.put(KURA_CLOUD_SERVICE_FACTORY_PID, FACTORY_PID);
            } else if (DATA_SERVICE_PID.equals(pid)) {
                props.put(ConfigurationAdmin.SERVICE_FACTORYPID, DATA_SERVICE_FACTORY_PID);
                String name = DATA_TRANSPORT_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
                props.put(name, String.format(REFERENCE_TARGET_VALUE_FORMAT, DATA_TRANSPORT_SERVICE_PID));
                props.put(KURA_CLOUD_SERVICE_FACTORY_PID, FACTORY_PID);
            } else if (DATA_TRANSPORT_SERVICE_PID.equals(pid)) {
                props.put(ConfigurationAdmin.SERVICE_FACTORYPID, DATA_TRANSPORT_SERVICE_FACTORY_PID);
                props.put(KURA_CLOUD_SERVICE_FACTORY_PID, FACTORY_PID);
            }
        }

        XmlComponentConfigurations xmlConfigurations = new XmlComponentConfigurations();
        xmlConfigurations.setConfigurations(result);
        return xmlConfigurations;
    }
}
