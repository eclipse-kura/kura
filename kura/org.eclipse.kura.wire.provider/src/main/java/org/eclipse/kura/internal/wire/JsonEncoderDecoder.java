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
    
    public JsonObject toJson(WireGraphConfiguration graphConfiguration) {
        JsonArray wireConfigurationJson = encodeWireConfigurationList(graphConfiguration.getWireConfigurations());
        JsonArray wireComponentConfigurationJson = encodeWireComponentConfigurationList(
                graphConfiguration.getWireComponentConfigurations());
        
        JsonObject wireGraphConfiguration = new JsonObject();
        wireGraphConfiguration.add("components", wireComponentConfigurationJson);
        wireGraphConfiguration.add("wires", wireConfigurationJson);
        
        return wireGraphConfiguration;
    }
    
    public WireGraphConfiguration fromJson(String jsonString) {

        List<WireComponentConfiguration> wireCompConfigList = new ArrayList<>();
        List<WireConfiguration> wireConfigList = new ArrayList<>();

        JsonObject json = Json.parse(jsonString).asObject();

        for (JsonObject.Member member : json) {
            String name = member.getName();
            JsonValue value = member.getValue();
            if ("wires".equalsIgnoreCase(name) && member.getValue().isArray()) {
                wireConfigList = decodeWireConfiguration(value.asArray());
            } else if ("components".equalsIgnoreCase(name) && member.getValue().isArray()) {
                wireCompConfigList = decodeWireComponentConfiguration(value.asArray());
            }
        }
        return new WireGraphConfiguration(wireCompConfigList, wireConfigList);
    }

    
    
    private List<WireConfiguration> decodeWireConfiguration(JsonArray array) {
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

            WireConfiguration wireConfiguration = new WireConfiguration(emitterPid, receiverPid);
            wireConfigurationList.add(wireConfiguration);
        }

        return wireConfigurationList;
    }
    
    private List<WireComponentConfiguration> decodeWireComponentConfiguration(JsonArray array) {
        List<WireComponentConfiguration> wireComponentConfigurationList = new ArrayList<>();

        Iterator<JsonValue> jsonIterator = array.iterator();
        while (jsonIterator.hasNext()) {
            Map<String, Object> properties = new HashMap<>();
            String componentPid= null;
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
            ComponentConfiguration componentConfiguration = new ComponentConfigurationImpl(componentPid, null, null);

            WireComponentConfiguration wireComponentConfiguration = new WireComponentConfiguration(componentConfiguration, properties);
            wireComponentConfigurationList.add(wireComponentConfiguration);
        }

        return wireComponentConfigurationList;
    }

    private Map<String, Object> parseRenderingProperties(JsonObject jsonRenderingProps) {
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

    private Map<String, Object> parseInputPortNames(JsonObject jsonInputPortNames) {
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

    private Map<String, Object> parseOutputPortNames(JsonObject jsonOutputPortNames) {
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

    private Map<String, Object> parsePosition(JsonObject jsonPositionPros) {
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

    private JsonObject encodeWireConfiguration(WireConfiguration wireConfig) {
        JsonObject result = new JsonObject();
        result.add("emitter", wireConfig.getEmitterPid());
        result.add("receiver", wireConfig.getReceiverPid());
        return result;
    }

    private JsonArray encodeWireConfigurationList(List<WireConfiguration> wireConfigurations) {
        JsonArray value = new JsonArray();
        for (WireConfiguration wireConfiguration : wireConfigurations) {
            value.add(encodeWireConfiguration(wireConfiguration));
        }

        return value;
    }

    private JsonArray encodeWireComponentConfigurationList(
            List<WireComponentConfiguration> wireComponentConfigurations) {

        JsonArray value = new JsonArray();
        for (WireComponentConfiguration wireComponentConfiguration : wireComponentConfigurations) {
            String pid = wireComponentConfiguration.getConfiguration().getPid();
            value.add(encodeComponentProperties(pid, wireComponentConfiguration.getProperties()));
        }

        return value;
    }

    private JsonObject encodeComponentProperties(String pid, Map<String, Object> componentProperties) {
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

    private JsonObject encodePosition(Map<String, Object> componentProperties) {
        JsonObject positionElems = new JsonObject();
        positionElems.add("x", (float) componentProperties.get("position.x"));
        positionElems.add("y", (float) componentProperties.get("position.y"));

        return positionElems;
    }

    private JsonObject encodeInputPortNames(Map<String, Object> componentProperties) {

        JsonObject inputPortElems = new JsonObject();
        for (Entry<String, Object> mapEntry : componentProperties.entrySet()) {
            if (mapEntry.getKey().startsWith("inputPortNames")) {
                String portNumber = mapEntry.getKey().split("\\.")[1];
                inputPortElems.add(portNumber, (String) mapEntry.getValue());
            }
        }

        return inputPortElems;
    }

    private JsonObject encodeOutputPortNames(Map<String, Object> componentProperties) {

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
