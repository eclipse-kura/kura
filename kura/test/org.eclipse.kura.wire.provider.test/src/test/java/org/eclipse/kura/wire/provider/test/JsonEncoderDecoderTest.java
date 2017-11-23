/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.wire.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.internal.wire.JsonEncoderDecoder;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class JsonEncoderDecoderTest {

    @Test
    public void testDecodeWireConfigurationEmptyArray() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        @SuppressWarnings("unchecked")
        List<WireConfiguration> wireConfigList = (List<WireConfiguration>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "decodeWireConfiguration", new JsonArray());
        assertNotNull(wireConfigList);
        assertEquals(0, wireConfigList.size());
    }

    @Test
    public void testDecodeWireConfigurationSingleWire() throws Throwable {

        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        JsonArray array = new JsonArray();
        JsonObject wire = new JsonObject();
        wire.add("emitter", "foo");
        wire.add("receiver", "bar");
        array.add(wire);

        @SuppressWarnings("unchecked")
        List<WireConfiguration> wireConfigList = (List<WireConfiguration>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "decodeWireConfiguration", array);

        assertNotNull(wireConfigList);
        assertEquals(1, wireConfigList.size());

        assertEquals("foo", wireConfigList.get(0).getEmitterPid());
        assertEquals("bar", wireConfigList.get(0).getReceiverPid());
    }

    @Test
    public void testDecodeWireComponentConfigurationEmptyArray() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        @SuppressWarnings("unchecked")
        List<WireComponentConfiguration> wireComponentConfigurationList = (List<WireComponentConfiguration>) TestUtil
                .invokePrivate(jsonEncoderDecoder, "decodeWireComponentConfiguration", new JsonArray());
        assertNotNull(wireComponentConfigurationList);
        assertEquals(0, wireComponentConfigurationList.size());
    }

    @Test
    public void testDecodeWireComponentConfigurationSingleComponent() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();
        String json = "[{\n" + "        \"pid\": \"foo\",\n" + "        \"inputPortCount\": 0,\n"
                + "        \"outputPortCount\": 5,\n" + "        \"renderingProperties\": {\n"
                + "            \"position\": {\n" + "                \"x\": 10,\n" + "                \"y\": 100\n"
                + "            },\n" + "            \"inputPortNames\": {\n" + "                \"0\": \"resetPort\"\n"
                + "            },\n" + "            \"outputPortNames\": {\n" + "                \"3\": \"then\",\n"
                + "                \"4\": \"else\"\n" + "            },\n" + "            \"rotation\": 0,\n"
                + "            \"color\": \"red\",\n" + "            \"size\": {\n"
                + "                \"width\": 100,\n" + "                \"height\": 50\n" + "            }\n"
                + "        }\n" + "    }\n" + "]";

        JsonArray jsonArray = Json.parse(json).asArray();
        @SuppressWarnings("unchecked")
        List<WireComponentConfiguration> wireComponentConfigurationList = (List<WireComponentConfiguration>) TestUtil
                .invokePrivate(jsonEncoderDecoder, "decodeWireComponentConfiguration", jsonArray);
        assertNotNull(wireComponentConfigurationList);
        assertEquals(1, wireComponentConfigurationList.size());

        WireComponentConfiguration wireComponentConfiguration = wireComponentConfigurationList.get(0);
        assertEquals("foo", wireComponentConfiguration.getConfiguration().getPid());
        assertEquals(null, wireComponentConfiguration.getConfiguration().getDefinition());
        assertEquals(null, wireComponentConfiguration.getConfiguration().getConfigurationProperties());
        assertEquals(0, wireComponentConfiguration.getProperties().get("inputPortCount"));
        assertEquals(5, wireComponentConfiguration.getProperties().get("outputPortCount"));
        assertEquals(10f, wireComponentConfiguration.getProperties().get("position.x"));
        assertEquals(100f, wireComponentConfiguration.getProperties().get("position.y"));
        assertEquals("resetPort", wireComponentConfiguration.getProperties().get("inputPortNames.0"));
        assertEquals("then", wireComponentConfiguration.getProperties().get("outputPortNames.3"));
        assertEquals("else", wireComponentConfiguration.getProperties().get("outputPortNames.4"));
    }

    @Test
    public void testDecodeWireComponentConfigurationTwoComponents() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();
        String json = "[{\n" + "        \"pid\": \"foo\",\n" + "        \"inputPortCount\": 0,\n"
                + "        \"outputPortCount\": 5,\n" + "        \"renderingProperties\": {\n"
                + "            \"position\": {\n" + "                \"x\": 10,\n" + "                \"y\": 100\n"
                + "            },\n" + "            \"inputPortNames\": {\n" + "                \"0\": \"resetPort\"\n"
                + "            },\n" + "            \"outputPortNames\": {\n" + "                \"3\": \"then\",\n"
                + "                \"4\": \"else\"\n" + "            },\n" + "            \"rotation\": 0,\n"
                + "            \"color\": \"red\",\n" + "            \"size\": {\n"
                + "                \"width\": 100,\n" + "                \"height\": 50\n" + "            }\n"
                + "        }\n" + "    },\n" + "    {\n" + "        \"pid\": \"bar\",\n"
                + "        \"inputPortCount\": 6,\n" + "        \"outputPortCount\": 1,\n"
                + "        \"renderingProperties\": {\n" + "            \"position\": {\n"
                + "                \"x\": 150,\n" + "                \"y\": 100\n" + "            },\n"
                + "            \"rotation\": 0,\n" + "            \"color\": \"red\",\n" + "            \"size\": {\n"
                + "                \"width\": 100,\n" + "                \"height\": 50\n" + "            }\n"
                + "        }\n" + "    }\n" + "]";

        JsonArray jsonArray = Json.parse(json).asArray();
        @SuppressWarnings("unchecked")
        List<WireComponentConfiguration> wireComponentConfigurationList = (List<WireComponentConfiguration>) TestUtil
                .invokePrivate(jsonEncoderDecoder, "decodeWireComponentConfiguration", jsonArray);
        assertNotNull(wireComponentConfigurationList);
        assertEquals(2, wireComponentConfigurationList.size());
    }

    @Test
    public void testDecodeDefaultJson() {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();
        String defaultJson = "{\"components\":[],\"wires\":[]}";

        WireGraphConfiguration wireGraphConfiguration = JsonEncoderDecoder.fromJson(defaultJson);
        assertNotNull(wireGraphConfiguration);

        assertEquals(0, wireGraphConfiguration.getWireComponentConfigurations().size());
        assertEquals(0, wireGraphConfiguration.getWireConfigurations().size());
    }

    @Test
    public void testDecodeWrongJson() {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();
        String defaultJson = "{\"components\":{},\"wires\":{}}";

        WireGraphConfiguration wireGraphConfiguration = JsonEncoderDecoder.fromJson(defaultJson);
        assertNotNull(wireGraphConfiguration);

        assertEquals(0, wireGraphConfiguration.getWireComponentConfigurations().size());
        assertEquals(0, wireGraphConfiguration.getWireConfigurations().size());
    }

    @Test
    public void testDecodeWireConfigurationEmitterReceiverNotStrings() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        JsonArray wireConfigArray = new JsonArray();
        JsonObject wire = new JsonObject();
        wire.add("emitter", 3);
        wire.add("receiver", 2);
        wireConfigArray.add(wire);

        List<WireConfiguration> result = (List<WireConfiguration>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "decodeWireConfiguration", wireConfigArray);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testDecodeWireConfigurationReceiverNotString() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        JsonArray wireConfigArray = new JsonArray();
        JsonObject wire = new JsonObject();
        wire.add("emitter", "test");
        wire.add("receiver", 2);
        wireConfigArray.add(wire);

        List<WireConfiguration> result = (List<WireConfiguration>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "decodeWireConfiguration", wireConfigArray);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testDecodeWireComponentConfigurationWrongArray() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        JsonArray wireComponentConfiguration = new JsonArray();
        JsonObject compProps = new JsonObject();
        compProps.add("inputPortCount", "test");
        compProps.add("outputPortCount", "test2");
        compProps.add("renderingProperties", 2);
        compProps.add("pid", 2);
        wireComponentConfiguration.add(compProps);

        List<WireComponentConfiguration> result = (List<WireComponentConfiguration>) TestUtil
                .invokePrivate(jsonEncoderDecoder, "decodeWireComponentConfiguration", wireComponentConfiguration);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testParseOutputPortNamesValueNotString() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        JsonObject ports = new JsonObject();
        ports.add("outputPort1", "CorrectName");
        ports.add("outputPort2", 3);

        Map<String, Object> result = (Map<String, Object>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "parseOutputPortNames", ports);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CorrectName", result.get("outputPortNames.outputPort1"));
    }

    @Test
    public void testParseInputPortNamesValueNotString() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        JsonObject ports = new JsonObject();
        ports.add("inputPort1", "CorrectName");
        ports.add("inputPort2", 3);

        Map<String, Object> result = (Map<String, Object>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "parseInputPortNames", ports);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CorrectName", result.get("inputPortNames.inputPort1"));
    }

    @Test
    public void testParseRenderingPropertiesWrongInput() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        JsonObject renderProps = new JsonObject();
        renderProps.add("position", 1);
        renderProps.add("inputPortNames", 3);
        renderProps.add("outputPortNames", "test");

        Map<String, Object> result = (Map<String, Object>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "parseRenderingProperties", renderProps);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testParsePosition() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        JsonObject renderProps = new JsonObject();
        renderProps.add("x", "1");
        renderProps.add("y", "3");

        Map<String, Object> result = (Map<String, Object>) TestUtil.invokePrivate(jsonEncoderDecoder, "parsePosition",
                renderProps);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testWireSerializationSingleArc() throws Throwable {
        WireConfiguration wireConfig = new WireConfiguration("emitterPid", "receiverPid");

        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();
        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "encodeWireConfiguration",
                wireConfig);
        assertNotNull(result);

        String expected = "{\"emitter\":\"emitterPid\",\"receiver\":\"receiverPid\"}";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testWireSerializationWireConfigListEmpty() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        List<WireConfiguration> emptyWireConfig = new ArrayList<>();
        JsonArray result = (JsonArray) TestUtil.invokePrivate(jsonEncoderDecoder, "encodeWireConfigurationList",
                emptyWireConfig);

        assertNotNull(result);

        String expected = "[]";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testWireSerializationWireConfigListOneElement() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        List<WireConfiguration> wireConfigList = new ArrayList<>();
        WireConfiguration wireConfig = new WireConfiguration("emitterPid", "receiverPid");
        wireConfigList.add(wireConfig);

        JsonArray result = (JsonArray) TestUtil.invokePrivate(jsonEncoderDecoder, "encodeWireConfigurationList",
                wireConfigList);

        assertNotNull(result);

        String expected = "[{\"emitter\":\"emitterPid\",\"receiver\":\"receiverPid\"}]";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testWireSerializationWireConfigListMoreElements() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        List<WireConfiguration> wireConfigList = new ArrayList<>();
        WireConfiguration wireConfig = new WireConfiguration("emitterPid", "receiverPid");
        WireConfiguration wireConfig2 = new WireConfiguration("foo", "bar");
        wireConfigList.add(wireConfig);
        wireConfigList.add(wireConfig2);

        JsonArray result = (JsonArray) TestUtil.invokePrivate(jsonEncoderDecoder, "encodeWireConfigurationList",
                wireConfigList);

        assertNotNull(result);

        String expected = "[{\"emitter\":\"emitterPid\",\"receiver\":\"receiverPid\"},{\"emitter\":\"foo\",\"receiver\":\"bar\"}]";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testSerializationPosition() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        Map<String, Object> positionMap = new HashMap<>();
        positionMap.put("position.x", 10f);
        positionMap.put("position.y", 100f);

        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "encodePosition", positionMap);

        assertNotNull(result);

        String expected = "{\"x\":10,\"y\":100}";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testSerializationInputPortNames() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("inputPortNames.0", "resetPort");

        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "encodeInputPortNames", inputMap);

        assertNotNull(result);

        String expected = "{\"0\":\"resetPort\"}";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testSerializationOutputPortNames() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("outputPortNames.3", "then");

        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "encodeOutputPortNames", inputMap);

        assertNotNull(result);

        String expected = "{\"3\":\"then\"}";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testEncodeComponentProperties() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("inputPortNames.0", "resetPort");
        inputMap.put("position.x", 10f);
        inputMap.put("position.y", 100f);
        inputMap.put("outputPortNames.3", "then");
        inputMap.put("inputPortCount", 0);
        inputMap.put("outputPortCount", 5);

        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "encodeComponentProperties",
                "testPid", inputMap);

        assertNotNull(result);

        String expected = "{\"pid\":\"testPid\",\"inputPortCount\":0,\"outputPortCount\":5,\"renderingProperties\":{\"position\":{\"x\":10,\"y\":100},\"inputPortNames\":{\"0\":\"resetPort\"},\"outputPortNames\":{\"3\":\"then\"}}}";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testEncodeWireComponentConfigurationList() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("inputPortNames.0", "resetPort");
        inputMap.put("position.x", 10f);
        inputMap.put("position.y", 100f);
        inputMap.put("outputPortNames.3", "then");
        inputMap.put("inputPortCount", 0);
        inputMap.put("outputPortCount", 5);

        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "encodeComponentProperties",
                "testPid", inputMap);

        assertNotNull(result);

        String expected = "{\"pid\":\"testPid\",\"inputPortCount\":0,\"outputPortCount\":5,\"renderingProperties\":{\"position\":{\"x\":10,\"y\":100},\"inputPortNames\":{\"0\":\"resetPort\"},\"outputPortNames\":{\"3\":\"then\"}}}";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testEncodeWireGraphConfiguration() throws Throwable {
        JsonEncoderDecoder jsonEncoderDecoder = new JsonEncoderDecoder();

        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("inputPortNames.0", "resetPort");
        inputMap.put("position.x", 10f);
        inputMap.put("position.y", 100f);
        inputMap.put("outputPortNames.3", "then");
        inputMap.put("inputPortCount", 0);
        inputMap.put("outputPortCount", 5);

        ComponentConfiguration emitterConfig = new ComponentConfigurationImpl("emitterPid", null, null);
        ComponentConfiguration receiverConfig = new ComponentConfigurationImpl("receiverPid", null, null);

        List<WireComponentConfiguration> wireComponentConfigurations = new ArrayList<>();
        WireComponentConfiguration emitterWireComponentConfiguration = new WireComponentConfiguration(emitterConfig,
                inputMap);
        WireComponentConfiguration receiverWireComponentConfiguration = new WireComponentConfiguration(receiverConfig,
                inputMap);
        wireComponentConfigurations.add(emitterWireComponentConfiguration);
        wireComponentConfigurations.add(receiverWireComponentConfiguration);

        List<WireConfiguration> wireConfigurations = new ArrayList<>();
        WireConfiguration wireConfiguration = new WireConfiguration("emitterPid", "receiverPid");
        wireConfigurations.add(wireConfiguration);

        WireGraphConfiguration wireGraphConfiguration = new WireGraphConfiguration(wireComponentConfigurations,
                wireConfigurations);

        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "toJson", wireGraphConfiguration);

        assertNotNull(result);

        String expected = "{\"components\":[{\"pid\":\"emitterPid\",\"inputPortCount\":0,\"outputPortCount\":5,\"renderingProperties\":{\"position\":{\"x\":10,\"y\":100},\"inputPortNames\":{\"0\":\"resetPort\"},\"outputPortNames\":{\"3\":\"then\"}}},{\"pid\":\"receiverPid\",\"inputPortCount\":0,\"outputPortCount\":5,\"renderingProperties\":{\"position\":{\"x\":10,\"y\":100},\"inputPortNames\":{\"0\":\"resetPort\"},\"outputPortNames\":{\"3\":\"then\"}}}],\"wires\":[{\"emitter\":\"emitterPid\",\"receiver\":\"receiverPid\"}]}";
        assertEquals(expected, result.toString());
    }

}
