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
package org.eclipse.kura.core.cloud.factory;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.factory.CloudServiceFactory;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataTransportService;
import org.osgi.service.component.ComponentConstants;

/**
 * The Kura default {@link CloudServiceFactory} implements a four layer stack architecture.
 * Each layer is an OSGi Declarative Services Factory Component and provides a service as follows:
 *
 * <table>
 * <thead>
 *    <tr><th>Factory PID</th><th>Service interface</th></tr>
 * </thead>
 * <tbody>
 *    <tr><td>org.eclipse.kura.cloud.CloudService</td><td>{@link CloudService}</td></tr>
 *    <tr><td>org.eclipse.kura.data.DataService</td><td>{@link DataService}</td></tr>
 *    <tr><td>org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport</td><td>{@link DataTransportService}</td></tr>
 *    <tr><td>org.eclipse.kura.status.CloudConnectionStatusService</td><td>{@link CloudConnectionStatusService}</td></tr>
 * </tbody>
 * </table>
 * <br>
 * When a new CloudService is created the factory creates also a DataService, a DataTransportService and a CloudConnectionStatusService.
 * Since the <i>pid</i> parameter of {@link #createConfiguration(String)} only specifies the PID of
 * the CloudService layer, a convention is needed to derive the PIDs of the lower layers.
 * <br>
 * <br>
 * The default stack instance is special.
 * For backward compatibility the PIDs of the default stack must be as follows:
 * 
 * <table>
 * <thead>
 *    <tr><th>PID (kura.service.pid)</th><th>Factory PID</th></tr>
 * </thead>
 * <tbody>
 *    <tr><td>org.eclipse.kura.cloud.CloudService</td><td>org.eclipse.kura.cloud.CloudService</td></tr>
 *    <tr><td>org.eclipse.kura.data.DataService</td><td>org.eclipse.kura.data.DataService</td></tr>
 *    <tr><td>org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport</td><td>org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport</td></tr>
 *    <tr><td>org.eclipse.kura.status.CloudConnectionStatusService</td><td>org.eclipse.kura.status.CloudConnectionStatusService</td></tr>
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
 *    <tr><th>PID (kura.service.pid)</th><th>Factory PID</th></tr>
 * </thead>
 * <tbody>
 *    <tr><td>org.eclipse.kura.cloud.CloudService-2</td><td>org.eclipse.kura.cloud.CloudService</td></tr>
 *    <tr><td>org.eclipse.kura.data.DataService-2</td><td>org.eclipse.kura.data.DataService</td></tr>
 *    <tr><td>org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport-2</td><td>org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport</td></tr>
 *    <tr><td>org.eclipse.kura.status.CloudConnectionStatusService-2</td><td>org.eclipse.kura.status.CloudConnectionStatusService</td></tr>
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
 * <pre>
 * &ltreference name="DataTransportService"
 *              bind="setDataTransportService"
 *              unbind="unsetDataTransportService"
 *              cardinality="1..1"
 *              policy="static"
 *              interface="org.eclipse.kura.data.DataTransportService"/&gt
 * </pre>
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
 * <pre>
 * (kura.service.pid=org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport-2)
 * </pre>
 * <br>
 */
public class DefaultCloudServiceFactory implements CloudServiceFactory {

	// The following constants must match the factory component definitions
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
	private static final String CLOUD_CONNECTION_STATUS_SERVICE_REFERENCE_NAME = "CloudConnectionStatusService";
		
	private static final String REFERENCE_TARGET_VALUE_FORMAT = "("+ConfigurationService.KURA_SERVICE_PID+"=%s)";
	
	private ConfigurationService m_configurationService;
	
	protected void setConfigurationService(ConfigurationService configurationService) {
		m_configurationService = configurationService;
	}
	
	protected void unsetConfigurationService(ConfigurationService configurationService) {
		if (configurationService == m_configurationService) {
			m_configurationService = null;
		}
	}
	
	@Override
	public String getFactoryPid() {
		return CLOUD_SERVICE_FACTORY_PID;
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
			String cloudConnectionStatusServicePid = CLOUD_CONNECTION_STATUS_SERVICE_PID;
			if (suffix != null) {
				dataServicePid += "-" + suffix;
				dataTransportServicePid += "-" + suffix;
				cloudConnectionStatusServicePid += "-" + suffix;
			}
			
			// create the CloudService layer and set the selective dependency on the DataService PID
			Map<String, Object> cloudServiceProperties = new HashMap<String, Object>();
			String name = DATA_SERVICE_REFERENCE_NAME+ComponentConstants.REFERENCE_TARGET_SUFFIX;
			cloudServiceProperties.put(name, String.format(REFERENCE_TARGET_VALUE_FORMAT, dataServicePid));
			
			m_configurationService.createFactoryConfiguration(CLOUD_SERVICE_FACTORY_PID, pid, cloudServiceProperties, false);
			
			// create the DataService layer and set the selective dependency on the DataTransportService PID
			Map<String, Object> dataServiceProperties = new HashMap<String, Object>();
			name = DATA_TRANSPORT_SERVICE_REFERENCE_NAME+ComponentConstants.REFERENCE_TARGET_SUFFIX;
			dataServiceProperties.put(name, String.format(REFERENCE_TARGET_VALUE_FORMAT, dataTransportServicePid));
			
			m_configurationService.createFactoryConfiguration(DATA_SERVICE_FACTORY_PID, dataServicePid, dataServiceProperties, false);
			
			// create the DataTransportService layer and set the selective dependency on the CloudConnectionStatusService PID
			Map<String, Object> dataTransportProperties = new HashMap<String, Object>();
			name = CLOUD_CONNECTION_STATUS_SERVICE_REFERENCE_NAME+ComponentConstants.REFERENCE_TARGET_SUFFIX;
			dataTransportProperties.put(name, String.format(REFERENCE_TARGET_VALUE_FORMAT, cloudConnectionStatusServicePid));
			
			m_configurationService.createFactoryConfiguration(DATA_TRANSPORT_SERVICE_FACTORY_PID, dataTransportServicePid, dataTransportProperties, false);

			// create the CloudConnectionStatus layer and take a snapshot
			m_configurationService.createFactoryConfiguration(CLOUD_CONNECTION_STATUS_SERVICE_FACTORY_PID, cloudConnectionStatusServicePid, null, true);
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
			String cloudConnectionStatusServicePid = CLOUD_CONNECTION_STATUS_SERVICE_PID;
			if (suffix != null) {
				dataServicePid += "-" + suffix;
				dataTransportServicePid += "-" + suffix;
				cloudConnectionStatusServicePid += "-" + suffix;
			}
			
			m_configurationService.deleteFactoryConfiguration(pid, false);
			m_configurationService.deleteFactoryConfiguration(dataServicePid, false);
			m_configurationService.deleteFactoryConfiguration(dataTransportServicePid, false);
			m_configurationService.deleteFactoryConfiguration(cloudConnectionStatusServicePid, true);
		}
	}
}
