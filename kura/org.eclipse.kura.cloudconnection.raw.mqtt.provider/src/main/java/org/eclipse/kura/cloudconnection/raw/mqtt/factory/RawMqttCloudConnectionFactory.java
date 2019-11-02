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
import org.eclipse.kura.locale.LocaleContextHolder;
import org.osgi.framework.BundleContext;
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

    private static final String REFERENCE_TARGET_VALUE_FORMAT = "(" + ConfigurationService.KURA_SERVICE_PID + "=%s)";

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
        try {
            ComponentConfiguration config = this.configurationService
                    .getDefaultComponentConfiguration(CLOUD_SERVICE_FACTORY_PID);
            return config.getLocalizedDefinition(LocaleContextHolder.getLocale().getLanguage()).getName();
        } catch (Exception e) {
            return CLOUD_SERVICE_FACTORY_PID;
        }
    }

    @Override
    public void createConfiguration(String pid) throws KuraException {

        String dataTransportServicePid = DATA_TRANSPORT_SERVICE_PID + "-" + new Date().getTime();
        this.configurationService.createFactoryConfiguration(DATA_TRANSPORT_SERVICE_FACTORY_PID,
                dataTransportServicePid, null, false);
        Map<String, Object> dataServiceProperties = new HashMap<String, Object>();
        String name = DATA_TRANSPORT_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
        dataServiceProperties.put(name, String.format(REFERENCE_TARGET_VALUE_FORMAT, dataTransportServicePid));

        String dataServicePid = DATA_SERVICE_FACTORY_PID + "-" + new Date().getTime();
        this.configurationService.createFactoryConfiguration(DATA_SERVICE_FACTORY_PID, dataServicePid,
                dataServiceProperties, false);

        Map<String, Object> cloudServiceProperties = new HashMap<>();
        name = DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
        cloudServiceProperties.put(name, String.format(REFERENCE_TARGET_VALUE_FORMAT, dataServicePid));
        this.configurationService.createFactoryConfiguration(CLOUD_SERVICE_FACTORY_PID, pid, cloudServiceProperties,
                true);
    }

    @Override
    public void deleteConfiguration(String pid) throws KuraException {
        String[] result = getTargetPids(pid);

        this.configurationService.deleteFactoryConfiguration(pid, false);
        if (result[0] != null)
            this.configurationService.deleteFactoryConfiguration(result[0], false);
        if (result[1] != null)
            this.configurationService.deleteFactoryConfiguration(result[1], true);
    }

    @Override
    public List<String> getStackComponentsPids(String pid) throws KuraException {
        List<String> componentPids = new ArrayList<String>();

        String[] result = getTargetPids(pid);
        componentPids.add(pid);
        componentPids.add(result[0]);
        componentPids.add(result[1]);
        return componentPids;
    }

    private String[] getTargetPids(String pid) throws KuraException {
        String[] result = new String[2];
        ComponentConfiguration config = this.configurationService.getComponentConfiguration(pid);
        String dataTransportServicePid = null;
        String name = DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
        String dataServicePid = (String) config.getConfigurationProperties().get(name);
        if (dataServicePid != null) {
            String[] names = dataServicePid.split("=");
            if (names.length == 2)
                dataServicePid = names[1];
            dataServicePid = dataServicePid.substring(0, dataServicePid.indexOf(')'));

            if (dataServicePid != null) {
                config = this.configurationService.getComponentConfiguration(dataServicePid);
                name = DATA_TRANSPORT_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX;
                dataTransportServicePid = (String) config.getConfigurationProperties().get(name);
                if (dataTransportServicePid != null) {
                    names = dataTransportServicePid.split("=");
                    if (names.length == 2)
                        dataTransportServicePid = names[1];
                    dataTransportServicePid = dataTransportServicePid.substring(0,
                            dataTransportServicePid.indexOf(')'));
                }
            }
        }
        result[0] = dataServicePid;
        result[1] = dataTransportServicePid;
        return result;
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