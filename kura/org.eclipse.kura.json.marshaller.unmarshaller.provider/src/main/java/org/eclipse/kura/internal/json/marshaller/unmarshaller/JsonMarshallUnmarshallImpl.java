/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.json.marshaller.unmarshaller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.wire.graph.MultiportWireConfiguration;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class JsonMarshallUnmarshallImpl implements Marshaller, Unmarshaller {

    private static final String RENDERING_PROPERTIES_KEY = "renderingProperties";
    private static final String OUTPUT_PORT_COUNT_KEY = "outputPortCount";
    private static final String INPUT_PORT_COUNT_KEY = "inputPortCount";
    private static final String PID_KEY = "pid";
    private static final String OUTPUT_PORT_NAMES_KEY = "outputPortNames";
    private static final String INPUT_PORT_NAMES_KEY = "inputPortNames";
    private static final String POSITION_KEY = "position";
    private static final String RECEIVER_PID_KEY = "receiver";
    private static final String RECEIVER_PORT_KEY = "receiverPort";
    private static final String EMITTER_PID_KEY = "emitter";
    private static final String EMITTER_PORT_KEY = "emitterPort";
    private static final String WIRES_KEY = "wires";
    private static final String COMPONENTS_KEY = "components";

    @Override
    public String marshal(Object object) throws KuraException {
        if (object instanceof WireGraphConfiguration) {
            JsonObject result = marshalWireGraphConfiguration((WireGraphConfiguration) object);
            return result.toString();
        }
        throw new KuraException(KuraErrorCode.INVALID_PARAMETER);
    }

    private JsonObject marshalWireGraphConfiguration(WireGraphConfiguration graphConfiguration) {
        JsonArray wireConfigurationJson = marshalWireConfigurationList(graphConfiguration.getWireConfigurations());
        JsonArray wireComponentConfigurationJson = marshalWireComponentConfigurationList(
                graphConfiguration.getWireComponentConfigurations());

        JsonObject wireGraphConfiguration = new JsonObject();
        wireGraphConfiguration.add(COMPONENTS_KEY, wireComponentConfigurationJson);
        wireGraphConfiguration.add(WIRES_KEY, wireConfigurationJson);

        return wireGraphConfiguration;
    }

    private static JsonObject marshalWireConfiguration(MultiportWireConfiguration wireConfig) {
        JsonObject result = new JsonObject();
        result.add(EMITTER_PID_KEY, wireConfig.getEmitterPid());
        result.add(EMITTER_PORT_KEY, wireConfig.getEmitterPort());
        result.add(RECEIVER_PID_KEY, wireConfig.getReceiverPid());
        result.add(RECEIVER_PORT_KEY, wireConfig.getReceiverPort());
        return result;
    }

    private static JsonArray marshalWireConfigurationList(List<MultiportWireConfiguration> wireConfigurations) {
        JsonArray value = new JsonArray();
        for (MultiportWireConfiguration wireConfiguration : wireConfigurations) {
            value.add(marshalWireConfiguration(wireConfiguration));
        }

        return value;
    }

    private static JsonArray marshalWireComponentConfigurationList(
            List<WireComponentConfiguration> wireComponentConfigurations) {

        JsonArray value = new JsonArray();
        for (WireComponentConfiguration wireComponentConfiguration : wireComponentConfigurations) {
            String pid = wireComponentConfiguration.getConfiguration().getPid();
            value.add(marshalComponentProperties(pid, wireComponentConfiguration.getProperties()));
        }

        return value;
    }

    private static JsonObject marshalComponentProperties(String pid, Map<String, Object> componentProperties) {
        JsonObject result = new JsonObject();

        JsonObject resultElems = new JsonObject();

        JsonObject position = marshalPosition(componentProperties);
        resultElems.add(POSITION_KEY, position);

        JsonObject inputPortNames = marshalInputPortNames(componentProperties);
        resultElems.add(INPUT_PORT_NAMES_KEY, inputPortNames);

        JsonObject outputPortNames = marshalOutputPortNames(componentProperties);
        resultElems.add(OUTPUT_PORT_NAMES_KEY, outputPortNames);

        result.add(PID_KEY, pid);
        result.add(INPUT_PORT_COUNT_KEY, (int) componentProperties.get(INPUT_PORT_COUNT_KEY));
        result.add(OUTPUT_PORT_COUNT_KEY, (int) componentProperties.get(OUTPUT_PORT_COUNT_KEY));
        result.add(RENDERING_PROPERTIES_KEY, resultElems);

        return result;
    }

    private static JsonObject marshalPosition(Map<String, Object> componentProperties) {
        JsonObject positionElems = new JsonObject();
        positionElems.add("x", (float) componentProperties.get("position.x"));
        positionElems.add("y", (float) componentProperties.get("position.y"));

        return positionElems;
    }

    private static JsonObject marshalInputPortNames(Map<String, Object> componentProperties) {

        JsonObject inputPortElems = new JsonObject();
        for (Entry<String, Object> mapEntry : componentProperties.entrySet()) {
            if (mapEntry.getKey().startsWith(INPUT_PORT_NAMES_KEY)) {
                String portNumber = mapEntry.getKey().split("\\.")[1];
                inputPortElems.add(portNumber, (String) mapEntry.getValue());
            }
        }

        return inputPortElems;
    }

    private static JsonObject marshalOutputPortNames(Map<String, Object> componentProperties) {

        JsonObject outputPortElems = new JsonObject();
        for (Entry<String, Object> mapEntry : componentProperties.entrySet()) {
            if (mapEntry.getKey().startsWith(OUTPUT_PORT_NAMES_KEY)) {
                String portNumber = mapEntry.getKey().split("\\.")[1];
                outputPortElems.add(portNumber, (String) mapEntry.getValue());
            }
        }

        return outputPortElems;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unmarshal(String s, Class<T> clazz) throws KuraException {
        if (clazz.equals(WireGraphConfiguration.class)) {
            return (T) unmarshalToWireGraphConfiguration(s);
        }
        throw new IllegalArgumentException("Invalid parameter!");
    }

    private WireGraphConfiguration unmarshalToWireGraphConfiguration(String jsonString) {

        List<WireComponentConfiguration> wireCompConfigList = new ArrayList<>();
        List<MultiportWireConfiguration> wireConfigList = new ArrayList<>();

        JsonObject json = Json.parse(jsonString).asObject();

        for (JsonObject.Member member : json) {
            String name = member.getName();
            JsonValue value = member.getValue();
            if (WIRES_KEY.equalsIgnoreCase(name) && value.isArray()) {
                wireConfigList = unmarshalWireConfiguration(value.asArray());
            } else if (COMPONENTS_KEY.equalsIgnoreCase(name) && value.isArray()) {
                wireCompConfigList = unmarshalWireComponentConfiguration(value.asArray());
            }
        }
        return new WireGraphConfiguration(wireCompConfigList, wireConfigList);
    }

    private static List<MultiportWireConfiguration> unmarshalWireConfiguration(JsonArray array) {
        List<MultiportWireConfiguration> wireConfigurationList = new ArrayList<>();

        Iterator<JsonValue> jsonIterator = array.iterator();
        while (jsonIterator.hasNext()) {
            JsonObject jsonWireConfig = jsonIterator.next().asObject();

            String emitterPid = null;
            String receiverPid = null;
            int emitterPort = 0;
            int receiverPort = 0;

            for (JsonObject.Member member : jsonWireConfig) {
                String name = member.getName();
                JsonValue value = member.getValue();
                if (EMITTER_PID_KEY.equalsIgnoreCase(name) && value.isString()) {
                    emitterPid = value.asString();
                } else if (RECEIVER_PID_KEY.equalsIgnoreCase(name) && value.isString()) {
                    receiverPid = value.asString();
                } else if (EMITTER_PORT_KEY.equalsIgnoreCase(name) && value.isNumber()) {
                    emitterPort = value.asInt();
                } else if (RECEIVER_PORT_KEY.equalsIgnoreCase(name) && value.isNumber()) {
                    receiverPort = value.asInt();
                }

            }

            if (emitterPid != null && receiverPid != null) {
                MultiportWireConfiguration wireConfiguration = new MultiportWireConfiguration(emitterPid, receiverPid,
                        emitterPort, receiverPort);
                wireConfigurationList.add(wireConfiguration);
            }
        }

        return wireConfigurationList;
    }

    private static List<WireComponentConfiguration> unmarshalWireComponentConfiguration(JsonArray array) {
        List<WireComponentConfiguration> wireComponentConfigurationList = new ArrayList<>();

        Iterator<JsonValue> jsonIterator = array.iterator();
        while (jsonIterator.hasNext()) {
            Map<String, Object> properties = new HashMap<>();
            String componentPid = null;
            JsonObject jsonWireComponentConfiguration = jsonIterator.next().asObject();

            for (JsonObject.Member member : jsonWireComponentConfiguration) {
                String name = member.getName();
                JsonValue value = member.getValue();
                if (INPUT_PORT_COUNT_KEY.equalsIgnoreCase(name) && value.isNumber()) {
                    properties.put(name, value.asInt());
                } else if (OUTPUT_PORT_COUNT_KEY.equalsIgnoreCase(name) && value.isNumber()) {
                    properties.put(name, value.asInt());
                } else if (RENDERING_PROPERTIES_KEY.equalsIgnoreCase(name) && value.isObject()) {
                    Map<String, Object> renderingProperties = unmarshalRenderingProperties(value.asObject());
                    properties.putAll(renderingProperties);
                } else if (PID_KEY.equalsIgnoreCase(name) && value.isString()) {
                    componentPid = value.asString();
                }
            }
            if (componentPid != null) {
                ComponentConfiguration componentConfiguration = new ComponentConfigurationImpl(componentPid, null,
                        null);

                WireComponentConfiguration wireComponentConfiguration = new WireComponentConfiguration(
                        componentConfiguration, properties);
                wireComponentConfigurationList.add(wireComponentConfiguration);
            }
        }

        return wireComponentConfigurationList;
    }

    private static Map<String, Object> unmarshalRenderingProperties(JsonObject jsonRenderingProps) {
        Map<String, Object> renderingProps = new HashMap<>();

        for (JsonObject.Member member : jsonRenderingProps) {
            String name = member.getName();
            JsonValue value = member.getValue();
            if (POSITION_KEY.equalsIgnoreCase(name) && value.isObject()) {
                Map<String, Object> positionMap = unmarshalPosition(value.asObject());
                renderingProps.putAll(positionMap);
            } else if (INPUT_PORT_NAMES_KEY.equalsIgnoreCase(name) && value.isObject()) {
                Map<String, Object> inputPortNamesMap = unmarshalInputPortNames(value.asObject());
                renderingProps.putAll(inputPortNamesMap);
            } else if (OUTPUT_PORT_NAMES_KEY.equalsIgnoreCase(name) && value.isObject()) {
                Map<String, Object> outputPortNamesMap = unmarshalOutputPortNames(value.asObject());
                renderingProps.putAll(outputPortNamesMap);
            }
        }

        return renderingProps;
    }

    private static Map<String, Object> unmarshalInputPortNames(JsonObject jsonInputPortNames) {
        Map<String, Object> inputPortNamesMap = new HashMap<>();

        for (JsonObject.Member member : jsonInputPortNames) {
            String name = member.getName();
            JsonValue value = member.getValue();
            if (value.isString()) {
                inputPortNamesMap.put(INPUT_PORT_NAMES_KEY + "." + name, value.asString());
            }
        }

        return inputPortNamesMap;
    }

    private static Map<String, Object> unmarshalOutputPortNames(JsonObject jsonOutputPortNames) {
        Map<String, Object> outputPortNamesMap = new HashMap<>();

        for (JsonObject.Member member : jsonOutputPortNames) {
            String name = member.getName();
            JsonValue value = member.getValue();
            if (value.isString()) {
                outputPortNamesMap.put(OUTPUT_PORT_NAMES_KEY + "." + name, value.asString());
            }
        }

        return outputPortNamesMap;
    }

    private static Map<String, Object> unmarshalPosition(JsonObject jsonPositionPros) {
        Map<String, Object> positionMap = new HashMap<>();

        for (JsonObject.Member member : jsonPositionPros) {
            String name = member.getName();
            JsonValue value = member.getValue();
            if ("x".equalsIgnoreCase(name) && value.isNumber()) {
                positionMap.put("position.x", value.asFloat());
            } else if ("y".equalsIgnoreCase(name) && value.isNumber()) {
                positionMap.put("position.y", value.asFloat());
            }
        }

        return positionMap;
    }
}
