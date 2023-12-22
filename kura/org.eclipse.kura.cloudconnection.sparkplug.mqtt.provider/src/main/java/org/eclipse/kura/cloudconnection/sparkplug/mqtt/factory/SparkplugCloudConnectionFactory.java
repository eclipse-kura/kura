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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.factory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

public class SparkplugCloudConnectionFactory implements CloudConnectionFactory {

    private static final String FACTORY_PID = "org.eclipse.kura.cloudconnection.sparkplug.mqtt.factory.SparkplugCloudConnectionFactory";

    private static final String CLOUD_ENDPOINT_FACTORY_PID = "org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint";
    private static final String DATA_SERVICE_FACTORY_PID = "org.eclipse.kura.data.DataService";
    private static final String DATA_TRANSPORT_SERVICE_FACTORY_PID =
            "org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport";

    private static final String CLOUD_ENDPOINT_PID = "org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint";
    private static final String DATA_SERVICE_PID = "org.eclipse.kura.cloudconnection.sparkplug.mqtt.data.SparkplugDataService";
    private static final String DATA_TRANSPORT_SERVICE_PID = "org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport";

    private static final String DATA_SERVICE_REFERENCE_NAME = "DataService"
            + ComponentConstants.REFERENCE_TARGET_SUFFIX;
    private static final String DATA_TRANSPORT_SERVICE_REFERENCE_NAME = "DataTransportService"
            + ComponentConstants.REFERENCE_TARGET_SUFFIX;

    private static final String REFERENCE_TARGET_VALUE_FORMAT = "(" + ConfigurationService.KURA_SERVICE_PID + "=%s)";

    private ConfigurationService configurationService;

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public String getFactoryPid() {
        return CLOUD_ENDPOINT_FACTORY_PID;
    }

    @Override
    public void createConfiguration(String pid) throws KuraException {
        String dataServicePid = getStackPidWithSuffix(pid, DATA_SERVICE_PID);
        String dataTransportServicePid = getStackPidWithSuffix(pid, DATA_TRANSPORT_SERVICE_PID);

        // CloudEndpoint
        Map<String, Object> cloudEndpointProperties = new HashMap<>();
        cloudEndpointProperties.put(DATA_SERVICE_REFERENCE_NAME,
                String.format(REFERENCE_TARGET_VALUE_FORMAT, dataServicePid));
        cloudEndpointProperties.put(KURA_CLOUD_CONNECTION_FACTORY_PID, FACTORY_PID);
        cloudEndpointProperties.put(DATA_TRANSPORT_SERVICE_REFERENCE_NAME,
                String.format(REFERENCE_TARGET_VALUE_FORMAT, dataTransportServicePid));

        this.configurationService.createFactoryConfiguration(CLOUD_ENDPOINT_FACTORY_PID, pid, cloudEndpointProperties,
                false);

        // DataService
        Map<String, Object> dataServiceProperties = new HashMap<>();
        dataServiceProperties.put(DATA_TRANSPORT_SERVICE_REFERENCE_NAME,
                String.format(REFERENCE_TARGET_VALUE_FORMAT, dataTransportServicePid));

        this.configurationService.createFactoryConfiguration(DATA_SERVICE_FACTORY_PID, dataServicePid,
                dataServiceProperties, false);

        // DataTransportService
        this.configurationService.createFactoryConfiguration(DATA_TRANSPORT_SERVICE_FACTORY_PID,
                dataTransportServicePid, null, true);
    }

    @Override
    public List<String> getStackComponentsPids(String pid) throws KuraException {
        String dataServicePid = getStackPidWithSuffix(pid, DATA_SERVICE_PID);
        String dataTransportServicePid = getStackPidWithSuffix(pid, DATA_TRANSPORT_SERVICE_PID);

        List<String> stackComponentPids = new LinkedList<>();
        stackComponentPids.add(pid);
        stackComponentPids.add(dataServicePid);
        stackComponentPids.add(dataTransportServicePid);

        return stackComponentPids;
    }

    @Override
    public void deleteConfiguration(String pid) throws KuraException {
        String dataServicePid = getStackPidWithSuffix(pid, DATA_SERVICE_PID);
        String dataTransportServicePid = getStackPidWithSuffix(pid, DATA_TRANSPORT_SERVICE_PID);

        this.configurationService.deleteFactoryConfiguration(pid, false);
        this.configurationService.deleteFactoryConfiguration(dataServicePid, false);
        this.configurationService.deleteFactoryConfiguration(dataTransportServicePid, true);
    }

    @Override
    public Set<String> getManagedCloudConnectionPids() throws KuraException {
        final BundleContext context = FrameworkUtil.getBundle(SparkplugCloudConnectionFactory.class).getBundleContext();

        try {
            return context
                    .getServiceReferences(CloudEndpoint.class,
                            "(service.factoryPid=" + CLOUD_ENDPOINT_FACTORY_PID + ")")
                    .stream().map(ref -> (String) ref.getProperty(ConfigurationService.KURA_SERVICE_PID))
                    .collect(Collectors.toSet());
        } catch (InvalidSyntaxException e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, e);
        }
    }

    private String getStackPidWithSuffix(String userPid, String componentPid) throws KuraException {
        if (!userPid.startsWith(CLOUD_ENDPOINT_PID)) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Invalid PID '{}'", userPid);
        }

        String[] parts = userPid.split("-");

        if (parts.length > 1) {
            return componentPid + "-" + parts[1];
        } else {
            return componentPid;
        }
    }

}
