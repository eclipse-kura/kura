/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.factory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.locale.LocaleContextHolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;

/**
 * The Kura default {@link CloudConnectionFactory} implements a three layer stack architecture.
 * Each layer is an OSGi Declarative Services Factory Component and provides a service as follows:
 *
 * <table>
 * <thead>
 * <tr>
 * <th>Factory PID</th>
 * <th>Service interface</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>org.eclipse.kura.cloud.CloudService</td>
 * <td>{@link DataService}</td>
 * </tr>
 * <tr>
 * <td>org.eclipse.kura.data.DataService</td>
 * <td>{@link DataService}</td>
 * </tr>
 * <tr>
 * <td>org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport</td>
 * <td>{@link DataTransportService}</td>
 * </tr>
 * </tbody>
 * </table>
 * <br>
 * When a new CloudService is created the factory creates also a DataService and a DataTransportService.
 * Since the <i>pid</i> parameter of {@link #createConfiguration(String)} only specifies the PID of
 * the CloudService layer, a convention is needed to derive the PIDs of the lower layers.
 * <br>
 * <br>
 * The default stack instance is special.
 * For backward compatibility the PIDs of the default stack must be as follows:
 *
 * <table>
 * <thead>
 * <tr>
 * <th>PID (kura.service.pid)</th>
 * <th>Factory PID</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>org.eclipse.kura.cloud.CloudService</td>
 * <td>org.eclipse.kura.cloud.CloudService</td>
 * </tr>
 * <tr>
 * <td>org.eclipse.kura.data.DataService</td>
 * <td>org.eclipse.kura.data.DataService</td>
 * </tr>
 * <tr>
 * <td></td>
 * <td>org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport</td>
 * </tr>
 * </tbody>
 * </table>
 * <br>
 *
 * For other stack instances the convention used to generate the PIDs for the lower layers is
 * to use the sub string in the CloudService PID starting after the first occurrence of the '-' character and append
 * the sub string to the PIDs of the default stack above, for example:
 *
 * <table>
 * <thead>
 * <tr>
 * <th>PID (kura.service.pid)</th>
 * <th>Factory PID</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>org.eclipse.kura.cloud.CloudService-2</td>
 * <td>org.eclipse.kura.cloud.CloudService</td>
 * </tr>
 * <tr>
 * <td>org.eclipse.kura.data.DataService-2</td>
 * <td>org.eclipse.kura.data.DataService</td>
 * </tr>
 * <tr>
 * <td>org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport-2</td>
 * <td>org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport</td>
 * </tr>
 * </tbody>
 * </table>
 * <br>
 * The (configuration of) layer instances of each stack are persisted to Kura snapshot and
 * recreated at every Kura start.
 * On startup every stack must be properly reassembled with the right layer instances.
 * <br>
 * This can be achieved using a standard OSGi Declarative Services magic property set in a layer configuration
 * specifying the layer dependency on a specific PID of its next lower layer.
 * The following example shows this selective dependency mechanism for the DataService and MqttDataTransport services.
 * <br>
 * The DataService component definition specifies a dependency on a DataTransportService as follows:
 *
 * <pre>
 * &ltreference name="DataTransportService"
 *              bind="setDataTransportService"
 *              unbind="unsetDataTransportService"
 *              cardinality="1..1"
 *              policy="static"
 *              interface="org.eclipse.kura.data.DataTransportService"/&gt
 * </pre>
 *
 * <br>
 * The DataService with PID <i>org.eclipse.kura.data.DataService-2</i> needs to be activated
 * only when its dependency on a specific DataTransportService with
 * PID <i>org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport-2</i> is satisfied.
 * <br>
 * The OSGi Declarative Services specification provides a magic <i>&ltreference name&gt.target</i>
 * property that can be set at runtime to specify a selective dependency.
 * <br>
 * In the above example the <i>org.eclipse.kura.data.DataService-2</i> component instance will have a
 * <i>DataTransportService.target</i> property set to the value:
 *
 * <pre>
 * (kura.service.pid = org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport - 2)
 * </pre>
 *
 * <br>
 */
public class DefaultCloudConnectionFactory implements CloudConnectionFactory {

    // The following constants must match the factory component definitions
    private static final String CLOUD_SERVICE_FACTORY_PID = "org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager";
    private static final String DATA_SERVICE_FACTORY_PID = "org.eclipse.kura.data.DataService";
    private static final String DATA_TRANSPORT_SERVICE_FACTORY_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";

    private static final String DATA_TRANSPORT_SERVICE_PID = "org.eclipse.kura.cloudconnection.eclipseiot.mqtt.MqttDataTransport";

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

        final BundleContext context = FrameworkUtil.getBundle(DefaultCloudConnectionFactory.class).getBundleContext();

        try {
            return context.getServiceReferences(CloudConnectionManager.class, null).stream().filter(ref -> {
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