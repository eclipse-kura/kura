/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.wire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class JsonEncoderDecoder {

    /**
     * Converts to JSON format a {@link WireGraphConfiguration}
     *
     * @param graphConfiguration
     *            the Wire Graph representation that needs to be converted to JSON before persistence.
     * @return a {@link JsonObject} representing the input conversion.
     */
    public static JsonObject toJson(WireGraphConfiguration graphConfiguration) {
        JsonArray wireConfigurationJson = encodeWireConfigurationList(graphConfiguration.getWireConfigurations());
        JsonArray wireComponentConfigurationJson = encodeWireComponentConfigurationList(
                graphConfiguration.getWireComponentConfigurations());

        JsonObject wireGraphConfiguration = new JsonObject();
        wireGraphConfiguration.add("components", wireComponentConfigurationJson);
        wireGraphConfiguration.add("wires", wireConfigurationJson);

        return wireGraphConfiguration;
    }

    /**
     * Converts the JSON string to a corresponding {@link WireGraphConfiguration} that represents the configuration of
     * the desired Wire Graph.
     *
     * @param jsonString
     *            a {@link String} representing the JSON of the Wire Graph
     * @return a {@link WireGraphConfiguration} that corresponds to the String passed as input.
     */
    public static WireGraphConfiguration fromJson(String jsonString) {

        List<WireComponentConfiguration> wireCompConfigList = new ArrayList<>();
        List<WireConfiguration> wireConfigList = new ArrayList<>();

        JsonObject json = Json.parse(jsonString).asObject();

        for (JsonObject.Member member : json) {
            String name = member.getName();
            JsonValue value = member.getValue();
            if ("wires".equalsIgnoreCase(name) && value.isArray()) {
                wireConfigList = decodeWireConfiguration(value.asArray());
            } else if ("components".equalsIgnoreCase(name) && value.isArray()) {
                wireCompConfigList = decodeWireComponentConfiguration(value.asArray());
            }
        }
        return new WireGraphConfiguration(wireCompConfigList, wireConfigList);
    }

    private static List<WireConfiguration> decodeWireConfiguration(JsonArray array) {
        List<WireConfiguration> wireConfigurationList = new ArrayList<>();

        Iterator<JsonValue> jsonIterator = array.iterator();
        while (jsonIterator.hasNext()) {
            JsonObject jsonWireConfig = jsonIterator.next().asObject();

            String emitterPid = null;
            String receiverPid = null;
            for (JsonObject.Member member : jsonWireConfig) {
                String name = member.getName();
                JsonValue value = member.getValue();
                if ("emitter".equalsIgnoreCase(name) && value.isString()) {
                    emitterPid = value.asString();
                } else if ("receiver".equalsIgnoreCase(name) && value.isString()) {
                    receiverPid = value.asString();
                }

            }

            if (emitterPid != null && receiverPid != null) {
                WireConfiguration wireConfiguration = new WireConfiguration(emitterPid, receiverPid);
                wireConfigurationList.add(wireConfiguration);
            }
        }

        return wireConfigurationList;
    }

    private static List<WireComponentConfiguration> decodeWireComponentConfiguration(JsonArray array) {
        List<WireComponentConfiguration> wireComponentConfigurationList = new ArrayList<>();

        Iterator<JsonValue> jsonIterator = array.iterator();
        while (jsonIterator.hasNext()) {
            Map<String, Object> properties = new HashMap<>();
            String componentPid = null;
            JsonObject jsonWireComponentConfiguration = jsonIterator.next().asObject();

            for (JsonObject.Member member : jsonWireComponentConfiguration) {
                String name = member.getName();
                JsonValue value = member.getValue();
                if ("inputPortCount".equalsIgnoreCase(name) && value.isNumber()) {
                    properties.put(name, value.asInt());
                } else if ("outputPortCount".equalsIgnoreCase(name) && value.isNumber()) {
                    properties.put(name, value.asInt());
                } else if ("renderingProperties".equalsIgnoreCase(name) && value.isObject()) {
                    Map<String, Object> renderingProperties = parseRenderingProperties(value.asObject());
                    properties.putAll(renderingProperties);
                } else if ("pid".equalsIgnoreCase(name) && value.isString()) {
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

    private static Map<String, Object> parseRenderingProperties(JsonObject jsonRenderingProps) {
        Map<String, Object> renderingProps = new HashMap<>();

        for (JsonObject.Member member : jsonRenderingProps) {
            String name = member.getName();
            JsonValue value = member.getValue();
            if ("position".equalsIgnoreCase(name) && value.isObject()) {
                Map<String, Object> positionMap = parsePosition(value.asObject());
                renderingProps.putAll(positionMap);
            } else if ("inputPortNames".equalsIgnoreCase(name) && value.isObject()) {
                Map<String, Object> inputPortNamesMap = parseInputPortNames(value.asObject());
                renderingProps.putAll(inputPortNamesMap);
            } else if ("outputPortNames".equalsIgnoreCase(name) && value.isObject()) {
                Map<String, Object> outputPortNamesMap = parseOutputPortNames(value.asObject());
                renderingProps.putAll(outputPortNamesMap);
            }
        }

        return renderingProps;
    }

    private static Map<String, Object> parseInputPortNames(JsonObject jsonInputPortNames) {
        Map<String, Object> inputPortNamesMap = new HashMap<>();

        for (JsonObject.Member member : jsonInputPortNames) {
            String name = member.getName();
            JsonValue value = member.getValue();
            if (value.isString()) {
                inputPortNamesMap.put("inputPortNames" + "." + name, value.asString());
            }
        }

        return inputPortNamesMap;
    }

    private static Map<String, Object> parseOutputPortNames(JsonObject jsonOutputPortNames) {
        Map<String, Object> outputPortNamesMap = new HashMap<>();

        for (JsonObject.Member member : jsonOutputPortNames) {
            String name = member.getName();
            JsonValue value = member.getValue();
            if (value.isString()) {
                outputPortNamesMap.put("outputPortNames" + "." + name, value.asString());
            }
        }

        return outputPortNamesMap;
    }

    private static Map<String, Object> parsePosition(JsonObject jsonPositionPros) {
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

    private static JsonObject encodeWireConfiguration(WireConfiguration wireConfig) {
        JsonObject result = new JsonObject();
        result.add("emitter", wireConfig.getEmitterPid());
        result.add("receiver", wireConfig.getReceiverPid());
        return result;
    }

    private static JsonArray encodeWireConfigurationList(List<WireConfiguration> wireConfigurations) {
        JsonArray value = new JsonArray();
        for (WireConfiguration wireConfiguration : wireConfigurations) {
            value.add(encodeWireConfiguration(wireConfiguration));
        }

        return value;
    }

    private static JsonArray encodeWireComponentConfigurationList(
            List<WireComponentConfiguration> wireComponentConfigurations) {

        JsonArray value = new JsonArray();
        for (WireComponentConfiguration wireComponentConfiguration : wireComponentConfigurations) {
            String pid = wireComponentConfiguration.getConfiguration().getPid();
            value.add(encodeComponentProperties(pid, wireComponentConfiguration.getProperties()));
        }

        return value;
    }

    private static JsonObject encodeComponentProperties(String pid, Map<String, Object> componentProperties) {
        JsonObject result = new JsonObject();

        JsonObject resultElems = new JsonObject();

        JsonObject position = encodePosition(componentProperties);
        resultElems.add("position", position);

        JsonObject inputPortNames = encodeInputPortNames(componentProperties);
        resultElems.add("inputPortNames", inputPortNames);

        JsonObject outputPortNames = encodeOutputPortNames(componentProperties);
        resultElems.add("outputPortNames", outputPortNames);

        result.add("pid", pid);
        result.add("inputPortCount", (int) componentProperties.get("inputPortCount"));
        result.add("outputPortCount", (int) componentProperties.get("outputPortCount"));
        result.add("renderingProperties", resultElems);

        return result;
    }

    private static JsonObject encodePosition(Map<String, Object> componentProperties) {
        JsonObject positionElems = new JsonObject();
        positionElems.add("x", (float) componentProperties.get("position.x"));
        positionElems.add("y", (float) componentProperties.get("position.y"));

        return positionElems;
    }

    private static JsonObject encodeInputPortNames(Map<String, Object> componentProperties) {

        JsonObject inputPortElems = new JsonObject();
        for (Entry<String, Object> mapEntry : componentProperties.entrySet()) {
            if (mapEntry.getKey().startsWith("inputPortNames")) {
                String portNumber = mapEntry.getKey().split("\\.")[1];
                inputPortElems.add(portNumber, (String) mapEntry.getValue());
            }
        }

        return inputPortElems;
    }

    private static JsonObject encodeOutputPortNames(Map<String, Object> componentProperties) {

        JsonObject outputPortElems = new JsonObject();
        for (Entry<String, Object> mapEntry : componentProperties.entrySet()) {
            if (mapEntry.getKey().startsWith("outputPortNames")) {
                String portNumber = mapEntry.getKey().split("\\.")[1];
                outputPortElems.add(portNumber, (String) mapEntry.getValue());
            }
        }

        return outputPortElems;
    }
}
