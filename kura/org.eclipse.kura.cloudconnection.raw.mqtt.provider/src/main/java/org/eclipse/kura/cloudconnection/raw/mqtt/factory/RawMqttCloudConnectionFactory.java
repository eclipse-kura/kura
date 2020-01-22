/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.raw.mqtt.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentConstants;

public class RawMqttCloudConnectionFactory implements CloudConnectionFactory {

    private static final String FACTORY_PID = "org.eclipse.kura.cloudconnection.raw.mqtt.factory.RawMqttCloudConnectionFactory";

    // The following constants must match the factory component definitions
    private static final String CLOUD_ENDPOINT_FACTORY_PID = "org.eclipse.kura.cloudconnection.raw.mqtt.cloud.RawMqttCloudEndpoint";
    private static final String DATA_SERVICE_FACTORY_PID = "org.eclipse.kura.data.DataService";
    private static final String DATA_TRANSPORT_SERVICE_FACTORY_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";

    private static final String CLOUD_SERVICE_PID = "org.eclipse.kura.cloudconnection.raw.mqtt.CloudEndpoint";
    private static final String DATA_SERVICE_PID = "org.eclipse.kura.cloudconnection.raw.mqtt.DataService";
    private static final String DATA_TRANSPORT_SERVICE_PID = "org.eclipse.kura.cloudconnection.raw.mqtt.MqttDataTransport";

    private static final String DATA_SERVICE_REFERENCE_NAME = "DataService";
    private static final String DATA_TRANSPORT_SERVICE_REFERENCE_NAME = "DataTransportService";

    private static final String REFERENCE_TARGET_VALUE_FORMAT = "(" + ConfigurationService.KURA_SERVICE_PID + "=%s)";

    private static final Pattern MANAGED_CLOUD_SERVICE_PID_PATTERN = Pattern
            .compile("^org\\.eclipse\\.kura\\.cloudconnection\\.raw\\.mqtt\\.CloudEndpoint(-[a-zA-Z0-9]+)?$");

    private ConfigurationService configurationService;

    protected void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    protected void unsetConfigurationService(ConfigurationService configurationService) {
        if (configurationService == this.configurationService) {
            this.configurationService = null;
        }
    }

    @Override
    public String getFactoryPid() {
        return CLOUD_ENDPOINT_FACTORY_PID;
    }

    @Override
    public void createConfiguration(String pid) throws KuraException {
        String[] parts = pid.split("-");
        if (parts.length != 0 && CLOUD_SERVICE_PID.equals(parts[0])) {
            String suffix = null;
            if (parts.length > 1) {
                suffix = parts[1];
            }

            String dataServicePid = DATA_SERVICE_PID;
            String dataTransportServicePid = DATA_TRANSPORT_SERVICE_PID;
            if (suffix != null) {
                dataServicePid += "-" + suffix;
                dataTransportServicePid += "-" + suffix;
            }

            // create the CloudService layer and set the selective dependency on the DataService PID
            Map<String, Object> cloudServiceProperties = new HashMap<>();
            String name = DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
            cloudServiceProperties.put(name, String.format(REFERENCE_TARGET_VALUE_FORMAT, dataServicePid));
            cloudServiceProperties.put(KURA_CLOUD_CONNECTION_FACTORY_PID, FACTORY_PID);

            this.configurationService.createFactoryConfiguration(CLOUD_ENDPOINT_FACTORY_PID, pid,
                    cloudServiceProperties, false);

            // create the DataService layer and set the selective dependency on the DataTransportService PID
            Map<String, Object> dataServiceProperties = new HashMap<>();
            name = DATA_TRANSPORT_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
            dataServiceProperties.put(name, String.format(REFERENCE_TARGET_VALUE_FORMAT, dataTransportServicePid));

            this.configurationService.createFactoryConfiguration(DATA_SERVICE_FACTORY_PID, dataServicePid,
                    dataServiceProperties, false);

            // create the DataTransportService layer and take a snapshot
            this.configurationService.createFactoryConfiguration(DATA_TRANSPORT_SERVICE_FACTORY_PID,
                    dataTransportServicePid, null, true);
        } else {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Invalid PID '{}'", pid);
        }
    }

    @Override
    public void deleteConfiguration(String pid) throws KuraException {
        String[] parts = pid.split("-");
        if (parts.length != 0 && CLOUD_SERVICE_PID.equals(parts[0])) {
            String suffix = null;
            if (parts.length > 1) {
                suffix = parts[1];
            }

            String dataServicePid = DATA_SERVICE_PID;
            String dataTransportServicePid = DATA_TRANSPORT_SERVICE_PID;
            if (suffix != null) {
                dataServicePid += "-" + suffix;
                dataTransportServicePid += "-" + suffix;
            }

            this.configurationService.deleteFactoryConfiguration(pid, false);
            this.configurationService.deleteFactoryConfiguration(dataServicePid, false);
            this.configurationService.deleteFactoryConfiguration(dataTransportServicePid, true);
        }
    }

    @Override
    public List<String> getStackComponentsPids(String pid) throws KuraException {
        List<String> componentPids = new ArrayList<>();
        String[] parts = pid.split("-");
        if (parts.length != 0 && CLOUD_SERVICE_PID.equals(parts[0])) {
            String suffix = null;
            if (parts.length > 1) {
                suffix = parts[1];
            }

            String dataServicePid = DATA_SERVICE_PID;
            String dataTransportServicePid = DATA_TRANSPORT_SERVICE_PID;
            if (suffix != null) {
                dataServicePid += "-" + suffix;
                dataTransportServicePid += "-" + suffix;
            }

            componentPids.add(pid);
            componentPids.add(dataServicePid);
            componentPids.add(dataTransportServicePid);
            return componentPids;
        } else {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Invalid PID '{}'", pid);
        }
    }

    @Override
    public Set<String> getManagedCloudConnectionPids() throws KuraException {

        final BundleContext context = FrameworkUtil.getBundle(RawMqttCloudConnectionFactory.class).getBundleContext();

        try {
            return context.getServiceReferences(CloudEndpoint.class, null).stream().filter(ref -> {
                final Object kuraServicePid = ref.getProperty(ConfigurationService.KURA_SERVICE_PID);

                if (!(kuraServicePid instanceof String)) {
                    return false;
                }

                return MANAGED_CLOUD_SERVICE_PID_PATTERN.matcher((String) kuraServicePid).matches()
                        && FACTORY_PID.equals(ref.getProperty(KURA_CLOUD_CONNECTION_FACTORY_PID));
            }).map(ref -> (String) ref.getProperty(ConfigurationService.KURA_SERVICE_PID)).collect(Collectors.toSet());
        } catch (InvalidSyntaxException e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, e);
        }
    }
}