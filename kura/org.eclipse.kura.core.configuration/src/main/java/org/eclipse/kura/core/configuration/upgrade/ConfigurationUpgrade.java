/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.configuration.upgrade;

import java.util.Map;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.framework.BundleContext;
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

    public static void upgrade(final ComponentConfiguration config, final BundleContext bundleContext) {

        if (config == null) {
            return;
        }

        String pid = config.getPid();
        final Map<String, Object> props = config.getConfigurationProperties();

        if (props == null) {
            return;
        }

        final Object factoryPid = props.get(ConfigurationAdmin.SERVICE_FACTORYPID);

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
        } else if (WireAssetConfigurationUpgrade.WIRE_ASSET_FACTORY_PID.equals(factoryPid)) {
            WireAssetConfigurationUpgrade.upgrade(props);
        }
    }
}
