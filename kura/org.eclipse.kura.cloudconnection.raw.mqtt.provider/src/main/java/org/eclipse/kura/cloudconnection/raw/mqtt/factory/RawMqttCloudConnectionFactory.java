/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.raw.mqtt.factory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;

public class RawMqttCloudConnectionFactory implements CloudConnectionFactory {

    // The following constants must match the factory component definitions
    private static final String CLOUD_SERVICE_FACTORY_PID = "org.eclipse.kura.cloudconnection.raw.mqtt.cloud.RawMqttCloudEndpoint";
    private static final String DATA_SERVICE_FACTORY_PID = "org.eclipse.kura.data.DataService";
    private static final String DATA_TRANSPORT_SERVICE_FACTORY_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";

    private static final String DATA_TRANSPORT_SERVICE_PID = "org.eclipse.kura.cloudconnection.raw.mqtt.MqttDataTransport";

    private static final String DATA_SERVICE_REFERENCE_NAME = "DataService";
    private static final String DATA_TRANSPORT_SERVICE_REFERENCE_NAME = "DataTransportService";

    private static final String REFERENCE_TARGET_VALUE_FORMAT = "(" + Constants.SERVICE_PID + "=%s)";

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
        return CLOUD_SERVICE_FACTORY_PID;
    }

    @Override
    public void createConfiguration(String pid) throws KuraException {

        String dataTransportServicePid = this.configurationService.createFactoryConfiguration(
                DATA_TRANSPORT_SERVICE_FACTORY_PID, DATA_TRANSPORT_SERVICE_PID + "-" + new Date().getTime(), null,
                false);
        Map<String, Object> dataServiceProperties = new HashMap<String, Object>();
        String name = DATA_TRANSPORT_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
        dataServiceProperties.put(name, String.format(REFERENCE_TARGET_VALUE_FORMAT, dataTransportServicePid));

        String dataServicePid = this.configurationService.createFactoryConfiguration(DATA_SERVICE_FACTORY_PID,
                DATA_SERVICE_FACTORY_PID + "-" + new Date().getTime(), dataServiceProperties, false);

        Map<String, Object> cloudServiceProperties = new HashMap<>();
        name = DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
        cloudServiceProperties.put(name, String.format(REFERENCE_TARGET_VALUE_FORMAT, dataServicePid));
        this.configurationService.createFactoryConfiguration(CLOUD_SERVICE_FACTORY_PID, pid, cloudServiceProperties,
                true);
    }

    @Override
    public void deleteConfiguration(String pid) throws KuraException {
        ComponentConfiguration config = this.configurationService.getComponentConfiguration(pid);
        String dataServicePid = null;
        String dataTransportServicePid = null;
        String name = DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
        String dataServiceServicePid = (String) config.getConfigurationProperties().get(name);
        if (dataServiceServicePid != null) {
            String[] names = dataServiceServicePid.split("=");
            if (names.length == 2)
                dataServiceServicePid = names[1];
            dataServiceServicePid = dataServiceServicePid.substring(0, dataServiceServicePid.indexOf(')'));

            if (dataServiceServicePid != null)
                dataServicePid = this.configurationService.getPidByServicePid(dataServiceServicePid);
            if (dataServicePid != null) {
                config = this.configurationService.getComponentConfiguration(dataServicePid);
                name = DATA_TRANSPORT_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
                String dataTransportServiceServicePid = (String) config.getConfigurationProperties().get(name);
                if (dataTransportServiceServicePid != null) {
                    names = dataTransportServiceServicePid.split("=");
                    if (names.length == 2)
                        dataTransportServiceServicePid = names[1];
                    dataTransportServiceServicePid = dataTransportServiceServicePid.substring(0,
                            dataTransportServiceServicePid.indexOf(')'));
                    dataTransportServicePid = this.configurationService
                            .getPidByServicePid(dataTransportServiceServicePid);
                }
            }
        }

        this.configurationService.deleteFactoryConfiguration(pid, false);
        if (dataServicePid != null)
            this.configurationService.deleteFactoryConfiguration(dataServicePid, false);
        if (dataTransportServicePid != null)
            this.configurationService.deleteFactoryConfiguration(dataTransportServicePid, true);
    }

    @Override
    public List<String> getStackComponentsPids(String pid) throws KuraException {
        ComponentConfiguration config = this.configurationService.getComponentConfiguration(pid);
        String dataServicePid = null;
        String dataTransportServicePid = null;
        String name = DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
        String dataServiceServicePid = (String) config.getConfigurationProperties().get(name);
        if (dataServiceServicePid != null) {
            String[] names = dataServiceServicePid.split("=");
            if (names.length == 2)
                dataServiceServicePid = names[1];
            dataServiceServicePid = dataServiceServicePid.substring(0, dataServiceServicePid.indexOf(')'));

            if (dataServiceServicePid != null)
                dataServicePid = this.configurationService.getPidByServicePid(dataServiceServicePid);
            if (dataServicePid != null) {
                config = this.configurationService.getComponentConfiguration(dataServicePid);
                name = DATA_TRANSPORT_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
                String dataTransportServiceServicePid = (String) config.getConfigurationProperties().get(name);
                if (dataTransportServiceServicePid != null) {
                    names = dataTransportServiceServicePid.split("=");
                    if (names.length == 2)
                        dataTransportServiceServicePid = names[1];
                    dataTransportServiceServicePid = dataTransportServiceServicePid.substring(0,
                            dataTransportServiceServicePid.indexOf(')'));
                    dataTransportServicePid = this.configurationService
                            .getPidByServicePid(dataTransportServiceServicePid);
                }
            }
        }

        List<String> componentPids = new ArrayList<String>();

        componentPids.add(pid);
        componentPids.add(dataServicePid);
        componentPids.add(dataTransportServicePid);
        return componentPids;
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
                final String factoryPid = (String) ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);
                return factoryPid.equals(CLOUD_SERVICE_FACTORY_PID);
            }).map(ref -> (String) ref.getProperty(ConfigurationService.KURA_SERVICE_PID)).collect(Collectors.toSet());
        } catch (InvalidSyntaxException e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, e);
        }
    }
}