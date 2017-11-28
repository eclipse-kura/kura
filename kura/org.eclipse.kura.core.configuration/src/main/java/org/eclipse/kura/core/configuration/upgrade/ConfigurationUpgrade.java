/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.marshalling.Marshalling;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class ConfigurationUpgrade {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationUpgrade.class);

    private static final String KURA_CLOUD_SERVICE_FACTORY_PID = "kura.cloud.service.factory.pid";
    private static final String FACTORY_PID = "org.eclipse.kura.core.cloud.factory.DefaultCloudServiceFactory";

    private static final String CLOUD_SERVICE_FACTORY_PID = "org.eclipse.kura.cloud.CloudService";
    private static final String DATA_SERVICE_FACTORY_PID = "org.eclipse.kura.data.DataService";
    private static final String DATA_TRANSPORT_SERVICE_FACTORY_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";

    private static final String CLOUD_SERVICE_PID = "org.eclipse.kura.cloud.CloudService";
    private static final String DATA_SERVICE_PID = "org.eclipse.kura.data.DataService";
    private static final String DATA_TRANSPORT_SERVICE_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";

    private static final String WIRE_SERVICE_PID = "org.eclipse.kura.wire.WireService";

    private static final String DATA_SERVICE_REFERENCE_NAME = "DataService";
    private static final String DATA_TRANSPORT_SERVICE_REFERENCE_NAME = "DataTransportService";
    private static final String REFERENCE_TARGET_VALUE_FORMAT = "(" + ConfigurationService.KURA_SERVICE_PID + "=%s)";

    private static final String NEW_WIRE_GRAPH_PROPERTY = "WireGraph";
    private static final String SEPARATOR = ".";
    private static final String PATTERN = "%s.";

    public static XmlComponentConfigurations upgrade(XmlComponentConfigurations xmlConfigs,
            BundleContext bundleContext) {
        List<ComponentConfiguration> result = new ArrayList<>();

        for (ComponentConfiguration config : xmlConfigs.getConfigurations()) {
            String pid = config.getPid();
            Map<String, Object> props = new HashMap<>(config.getConfigurationProperties());
            ComponentConfigurationImpl cc = new ComponentConfigurationImpl(pid, (Tocd) config.getDefinition(), props);
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
            } else if (WIRE_SERVICE_PID.equals(pid)) {
                Map<String, Object> convertedProps = new HashMap<>(convertToNewWiresJsonFormat(props, bundleContext));
                props.clear();
                props.putAll(convertedProps);
            }
        }

        XmlComponentConfigurations xmlConfigurations = new XmlComponentConfigurations();
        xmlConfigurations.setConfigurations(result);
        return xmlConfigurations;
    }

    private static Map<String, Object> convertToNewWiresJsonFormat(Map<String, Object> oldProperties,
            BundleContext bundleContext) {

        String oldWireGraph = (String) oldProperties.get("wiregraph");
        if (oldWireGraph != null) {
            List<WireComponentConfiguration> wireComponentConfigurations = getWireComponentConfigurationsFromOldJson(
                    oldWireGraph);
            List<WireConfiguration> wireConfigurations = getInstance(oldProperties);
            WireGraphConfiguration wireGraphConfiguration = new WireGraphConfiguration(wireComponentConfigurations,
                    wireConfigurations);
            String newJson = marshalJson(bundleContext, wireGraphConfiguration);

            Map<String, Object> convertedProps = new HashMap<>();
            convertedProps.put(NEW_WIRE_GRAPH_PROPERTY, newJson);
            return convertedProps;
        }
        return oldProperties;
    }

    private static List<WireComponentConfiguration> getWireComponentConfigurationsFromOldJson(String oldWireGraph) {
        List<WireComponentConfiguration> wireComponentConfigurations = new ArrayList<>();

        JsonObject json = Json.parse(oldWireGraph).asObject();

        for (JsonObject.Member member : json) {
            JsonValue value = member.getValue();
            WireComponentConfiguration wireComponentConfiguration = parseJsonComponent(value.asObject());
            wireComponentConfigurations.add(wireComponentConfiguration);
        }
        return wireComponentConfigurations;
    }

    private static WireComponentConfiguration parseJsonComponent(JsonObject componentJson) {
        String componentPid = null;
        Map<String, Object> properties = new HashMap<>();

        for (JsonObject.Member member : componentJson) {
            String name = member.getName();
            JsonValue value = member.getValue();

            if ("pid".equalsIgnoreCase(name)) {
                componentPid = value.asString();
            } else if ("loc".equalsIgnoreCase(name)) {
                String[] location = value.asString().split(",");
                properties.put("position.x", Float.parseFloat(location[0]));
                properties.put("position.y", Float.parseFloat(location[1]));
            } else if ("type".equalsIgnoreCase(name)) {
                String componentType = value.asString();
                if ("producer".equalsIgnoreCase(componentType)) {
                    properties.put("inputPortCount", 0);
                    properties.put("outputPortCount", 1);
                } else if ("consumer".equalsIgnoreCase(componentType)) {
                    properties.put("inputPortCount", 1);
                    properties.put("outputPortCount", 0);
                } else {
                    properties.put("inputPortCount", 1);
                    properties.put("outputPortCount", 1);
                }
            }
        }

        // TODO: fill also the component configuration?
        return new WireComponentConfiguration(new ComponentConfigurationImpl(componentPid, null, null), properties);
    }

    private static List<WireConfiguration> getInstance(final Map<String, Object> properties) {
        final List<WireConfiguration> wireConfs = new CopyOnWriteArrayList<>();
        final Set<Long> wireIds = new HashSet<>();
        for (final Map.Entry<String, Object> entry : properties.entrySet()) {
            final String key = entry.getKey();
            if (key.contains(SEPARATOR) && Character.isDigit(key.charAt(0))) {
                final Long wireConfId = Long.parseLong(key.substring(0, key.indexOf(SEPARATOR)));
                wireIds.add(wireConfId);
            }
        }
        final Iterator<Long> it = wireIds.iterator();
        while (it.hasNext()) {
            final String wireConfId = String.valueOf(it.next());
            String emitterPid = null;
            String receiverPid = null;
            String filter = null;
            for (final Map.Entry<String, Object> entry : properties.entrySet()) {
                final String key = entry.getKey();
                final String value = String.valueOf(entry.getValue());

                if (!key.contains(SEPARATOR)) {
                    continue;
                }
                if (key.startsWith(String.format(PATTERN, wireConfId))) {
                    if (key.contains("emitter")) {
                        emitterPid = value;
                    }
                    if (key.contains("receiver")) {
                        receiverPid = value;
                    }
                }
            }
            final WireConfiguration configuration = new WireConfiguration(emitterPid, receiverPid);
            configuration.setFilter(filter);
            wireConfs.add(configuration);
        }
        return wireConfs;
    }

    private static ServiceReference<Marshalling>[] getJsonMarshallers(BundleContext bundleContext) {
        String filterString = String.format("(&(kura.service.pid=%s))", "org.eclipse.kura.marshalling.json.provider");
        return ServiceUtil.getServiceReferences(bundleContext, Marshalling.class, filterString);
    }

    private static void ungetMarshallersServiceReferences(BundleContext bundleContext,
            final ServiceReference<Marshalling>[] refs) {
        ServiceUtil.ungetServiceReferences(bundleContext, refs);
    }

    private static String marshalJson(BundleContext bundleContext, WireGraphConfiguration wireGraphConfiguration) {
        String result = null;
        ServiceReference<Marshalling>[] jsonMarshallerSRs = getJsonMarshallers(bundleContext);
        try {
            for (final ServiceReference<Marshalling> jsonMarshallerSR : jsonMarshallerSRs) {
                Marshalling jsonMarshaller = bundleContext.getService(jsonMarshallerSR);
                result = jsonMarshaller.marshal(wireGraphConfiguration);
            }
        } catch (Exception e) {
            logger.warn("Failed to marshal Wire Graph configuration.");
        } finally {
            ungetMarshallersServiceReferences(bundleContext, jsonMarshallerSRs);
        }
        return result;
    }
}
