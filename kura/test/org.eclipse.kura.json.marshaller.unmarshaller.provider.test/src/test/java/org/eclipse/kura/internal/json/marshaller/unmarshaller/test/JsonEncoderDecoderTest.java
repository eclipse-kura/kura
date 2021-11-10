/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.json.marshaller.unmarshaller.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.inventory.resources.SystemBundle;
import org.eclipse.kura.core.inventory.resources.SystemBundles;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackage;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackages;
import org.eclipse.kura.core.inventory.resources.SystemPackage;
import org.eclipse.kura.core.inventory.resources.SystemPackages;
import org.eclipse.kura.core.inventory.resources.SystemResourcesInfo;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.JsonMarshallUnmarshallImpl;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.wiregraph.WireGraphJsonMarshallUnmarshallImpl;
import org.eclipse.kura.system.SystemResourceInfo;
import org.eclipse.kura.system.SystemResourceType;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.graph.MultiportWireConfiguration;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class JsonEncoderDecoderTest {

    @Test
    public void testUnmarshalWireConfigurationEmptyArray() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        @SuppressWarnings("unchecked")
        List<WireConfiguration> wireConfigList = (List<WireConfiguration>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "unmarshalWireConfiguration", new JsonArray());
        assertNotNull(wireConfigList);
        assertEquals(0, wireConfigList.size());
    }

    @Test
    public void testUnmarshalWireConfigurationSingleWire() throws Throwable {

        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();
        JsonArray array = new JsonArray();
        JsonObject wire = new JsonObject();
        wire.add("emitter", "foo");
        wire.add("receiver", "bar");
        array.add(wire);

        @SuppressWarnings("unchecked")
        List<WireConfiguration> wireConfigList = (List<WireConfiguration>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "unmarshalWireConfiguration", array);

        assertNotNull(wireConfigList);
        assertEquals(1, wireConfigList.size());

        assertEquals("foo", wireConfigList.get(0).getEmitterPid());
        assertEquals("bar", wireConfigList.get(0).getReceiverPid());
    }

    @Test
    public void testUnmarshalWireComponentConfigurationEmptyArray() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        @SuppressWarnings("unchecked")
        List<WireComponentConfiguration> wireComponentConfigurationList = (List<WireComponentConfiguration>) TestUtil
                .invokePrivate(jsonEncoderDecoder, "unmarshalWireComponentConfiguration", new JsonArray());
        assertNotNull(wireComponentConfigurationList);
        assertEquals(0, wireComponentConfigurationList.size());
    }

    @Test
    public void testUnmarshalWireComponentConfigurationSingleComponent() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();
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
                .invokePrivate(jsonEncoderDecoder, "unmarshalWireComponentConfiguration", jsonArray);
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
    public void testUnmarshalWireComponentConfigurationTwoComponents() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();
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
                .invokePrivate(jsonEncoderDecoder, "unmarshalWireComponentConfiguration", jsonArray);
        assertNotNull(wireComponentConfigurationList);
        assertEquals(2, wireComponentConfigurationList.size());
    }

    @Test
    public void testUnmarshalDefaultJson() throws Exception {
        JsonMarshallUnmarshallImpl jsonEncoderDecoder = new JsonMarshallUnmarshallImpl();
        String defaultJson = "{\"components\":[],\"wires\":[]}";

        WireGraphConfiguration wireGraphConfiguration = jsonEncoderDecoder.unmarshal(defaultJson,
                WireGraphConfiguration.class);
        assertNotNull(wireGraphConfiguration);

        assertEquals(0, wireGraphConfiguration.getWireComponentConfigurations().size());
        assertEquals(0, wireGraphConfiguration.getWireConfigurations().size());
    }

    @Test
    public void testUnmarshalWrongJson() throws Exception {
        JsonMarshallUnmarshallImpl jsonEncoderDecoder = new JsonMarshallUnmarshallImpl();
        String defaultJson = "{\"components\":{},\"wires\":{}}";

        WireGraphConfiguration wireGraphConfiguration = jsonEncoderDecoder.unmarshal(defaultJson,
                WireGraphConfiguration.class);
        assertNotNull(wireGraphConfiguration);

        assertEquals(0, wireGraphConfiguration.getWireComponentConfigurations().size());
        assertEquals(0, wireGraphConfiguration.getWireConfigurations().size());
    }

    @Test
    public void testUnmarshalWireConfigurationEmitterReceiverNotStrings() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        JsonArray wireConfigArray = new JsonArray();
        JsonObject wire = new JsonObject();
        wire.add("emitter", 3);
        wire.add("receiver", 2);
        wireConfigArray.add(wire);

        List<WireConfiguration> result = (List<WireConfiguration>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "unmarshalWireConfiguration", wireConfigArray);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testUnmarshalWireConfigurationReceiverNotString() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        JsonArray wireConfigArray = new JsonArray();
        JsonObject wire = new JsonObject();
        wire.add("emitter", "test");
        wire.add("receiver", 2);
        wireConfigArray.add(wire);

        List<WireConfiguration> result = (List<WireConfiguration>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "unmarshalWireConfiguration", wireConfigArray);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testUnmarshalWireComponentConfigurationWrongArray() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        JsonArray wireComponentConfiguration = new JsonArray();
        JsonObject compProps = new JsonObject();
        compProps.add("inputPortCount", "test");
        compProps.add("outputPortCount", "test2");
        compProps.add("renderingProperties", 2);
        compProps.add("pid", 2);
        wireComponentConfiguration.add(compProps);

        List<WireComponentConfiguration> result = (List<WireComponentConfiguration>) TestUtil
                .invokePrivate(jsonEncoderDecoder, "unmarshalWireComponentConfiguration", wireComponentConfiguration);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testUnmarshalOutputPortNamesValueNotString() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        JsonObject ports = new JsonObject();
        ports.add("outputPort1", "CorrectName");
        ports.add("outputPort2", 3);

        Map<String, Object> result = (Map<String, Object>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "unmarshalOutputPortNames", ports);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CorrectName", result.get("outputPortNames.outputPort1"));
    }

    @Test
    public void testUnmarshalInputPortNamesValueNotString() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        JsonObject ports = new JsonObject();
        ports.add("inputPort1", "CorrectName");
        ports.add("inputPort2", 3);

        Map<String, Object> result = (Map<String, Object>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "unmarshalInputPortNames", ports);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CorrectName", result.get("inputPortNames.inputPort1"));
    }

    @Test
    public void testUnmarshalRenderingPropertiesWrongInput() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        JsonObject renderProps = new JsonObject();
        renderProps.add("position", 1);
        renderProps.add("inputPortNames", 3);
        renderProps.add("outputPortNames", "test");

        Map<String, Object> result = (Map<String, Object>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "unmarshalRenderingProperties", renderProps);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testUnmarshalPosition() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        JsonObject renderProps = new JsonObject();
        renderProps.add("x", "1");
        renderProps.add("y", "3");

        Map<String, Object> result = (Map<String, Object>) TestUtil.invokePrivate(jsonEncoderDecoder,
                "unmarshalPosition", renderProps);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testMarshalWireSingleArc() throws Throwable {
        MultiportWireConfiguration wireConfig = new MultiportWireConfiguration("emitterPid", "receiverPid", 0, 0);

        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();
        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "marshalWireConfiguration",
                wireConfig);
        assertNotNull(result);

        String expected = "{\"emitter\":\"emitterPid\",\"emitterPort\":0,\"receiver\":\"receiverPid\",\"receiverPort\":0}";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testMarshalWireConfigListEmpty() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        List<WireConfiguration> emptyWireConfig = new ArrayList<>();
        JsonArray result = (JsonArray) TestUtil.invokePrivate(jsonEncoderDecoder, "marshalWireConfigurationList",
                emptyWireConfig);

        assertNotNull(result);

        String expected = "[]";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testMarshalWireConfigListOneElement() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        List<MultiportWireConfiguration> wireConfigList = new ArrayList<>();
        MultiportWireConfiguration wireConfig = new MultiportWireConfiguration("emitterPid", "receiverPid", 0, 0);
        wireConfigList.add(wireConfig);

        JsonArray result = (JsonArray) TestUtil.invokePrivate(jsonEncoderDecoder, "marshalWireConfigurationList",
                wireConfigList);

        assertNotNull(result);

        String expected = "[{\"emitter\":\"emitterPid\",\"emitterPort\":0,\"receiver\":\"receiverPid\",\"receiverPort\":0}]";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testMarshalWireConfigListMoreElements() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        List<MultiportWireConfiguration> wireConfigList = new ArrayList<>();
        MultiportWireConfiguration wireConfig = new MultiportWireConfiguration("emitterPid", "receiverPid", 0, 0);
        MultiportWireConfiguration wireConfig2 = new MultiportWireConfiguration("foo", "bar", 0, 0);
        wireConfigList.add(wireConfig);
        wireConfigList.add(wireConfig2);

        JsonArray result = (JsonArray) TestUtil.invokePrivate(jsonEncoderDecoder, "marshalWireConfigurationList",
                wireConfigList);

        assertNotNull(result);

        String expected = "[{\"emitter\":\"emitterPid\",\"emitterPort\":0,\"receiver\":\"receiverPid\",\"receiverPort\":0},{\"emitter\":\"foo\",\"emitterPort\":0,\"receiver\":\"bar\",\"receiverPort\":0}]";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testMarshalPosition() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        Map<String, Object> positionMap = new HashMap<>();
        positionMap.put("position.x", 10f);
        positionMap.put("position.y", 100f);

        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "marshalPosition", positionMap);

        assertNotNull(result);

        String expected = "{\"x\":10,\"y\":100}";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testMarshalInputPortNames() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("inputPortNames.0", "resetPort");

        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "marshalInputPortNames", inputMap);

        assertNotNull(result);

        String expected = "{\"0\":\"resetPort\"}";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testMarshalOutputPortNames() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("outputPortNames.3", "then");

        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "marshalOutputPortNames", inputMap);

        assertNotNull(result);

        String expected = "{\"3\":\"then\"}";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testMarshalComponentProperties() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("inputPortNames.0", "resetPort");
        inputMap.put("position.x", 10f);
        inputMap.put("position.y", 100f);
        inputMap.put("outputPortNames.3", "then");
        inputMap.put("inputPortCount", 0);
        inputMap.put("outputPortCount", 5);

        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "marshalComponentProperties",
                "testPid", inputMap);

        assertNotNull(result);

        String expected = "{\"pid\":\"testPid\",\"inputPortCount\":0,\"outputPortCount\":5,\"renderingProperties\":{\"position\":{\"x\":10,\"y\":100},\"inputPortNames\":{\"0\":\"resetPort\"},\"outputPortNames\":{\"3\":\"then\"}}}";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testMarshalWireComponentConfigurationList() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("inputPortNames.0", "resetPort");
        inputMap.put("position.x", 10f);
        inputMap.put("position.y", 100f);
        inputMap.put("outputPortNames.3", "then");
        inputMap.put("inputPortCount", 0);
        inputMap.put("outputPortCount", 5);

        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "marshalComponentProperties",
                "testPid", inputMap);

        assertNotNull(result);

        String expected = "{\"pid\":\"testPid\",\"inputPortCount\":0,\"outputPortCount\":5,\"renderingProperties\":{\"position\":{\"x\":10,\"y\":100},\"inputPortNames\":{\"0\":\"resetPort\"},\"outputPortNames\":{\"3\":\"then\"}}}";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testMarshalWireGraphConfiguration() throws Throwable {
        WireGraphJsonMarshallUnmarshallImpl jsonEncoderDecoder = new WireGraphJsonMarshallUnmarshallImpl();

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

        List<MultiportWireConfiguration> wireConfigurations = new ArrayList<>();
        MultiportWireConfiguration wireConfiguration = new MultiportWireConfiguration("emitterPid", "receiverPid", 0,
                0);
        wireConfigurations.add(wireConfiguration);

        WireGraphConfiguration wireGraphConfiguration = new WireGraphConfiguration(wireComponentConfigurations,
                wireConfigurations);

        JsonObject result = (JsonObject) TestUtil.invokePrivate(jsonEncoderDecoder, "marshalWireGraphConfiguration",
                wireGraphConfiguration);

        assertNotNull(result);

        String expected = "{\"components\":[{\"pid\":\"emitterPid\",\"inputPortCount\":0,\"outputPortCount\":5,\"renderingProperties\":{\"position\":{\"x\":10,\"y\":100},\"inputPortNames\":{\"0\":\"resetPort\"},\"outputPortNames\":{\"3\":\"then\"}}},{\"pid\":\"receiverPid\",\"inputPortCount\":0,\"outputPortCount\":5,\"renderingProperties\":{\"position\":{\"x\":10,\"y\":100},\"inputPortNames\":{\"0\":\"resetPort\"},\"outputPortNames\":{\"3\":\"then\"}}}],\"wires\":[{\"emitter\":\"emitterPid\",\"emitterPort\":0,\"receiver\":\"receiverPid\",\"receiverPort\":0}]}";
        assertEquals(expected, result.toString());
    }

    @Test
    public void testSystemDeploymentPackages() throws KuraException {
        SystemDeploymentPackages systemDeploymentPackages = new SystemDeploymentPackages();
        SystemDeploymentPackage[] systemDeploymentPackageArray = new SystemDeploymentPackage[1];
        SystemDeploymentPackage systemDeploymentPackage = new SystemDeploymentPackage("dp1", "1.0.0");
        SystemBundle[] systemBundles = new SystemBundle[1];
        systemBundles[0] = new SystemBundle("bundle1", "2.0.0");
        systemBundles[0].setId(0);
        systemBundles[0].setState("ACTIVE");
        systemDeploymentPackage.setBundleInfos(systemBundles);
        systemDeploymentPackageArray[0] = systemDeploymentPackage;
        systemDeploymentPackages.setDeploymentPackages(systemDeploymentPackageArray);

        String json = new JsonMarshallUnmarshallImpl().marshal(systemDeploymentPackages);

        String expectedJson = "{\"deploymentPackages\":[{\"name\":\"dp1\",\"version\":\"1.0.0\",\"bundles\":[{\"name\":\"bundle1\",\"version\":\"2.0.0\",\"id\":0,\"state\":\"ACTIVE\",\"signed\":false}]}]}";
        assertEquals(expectedJson, json);
    }

    @Test
    public void testSystemBundles() throws KuraException {
        SystemBundles systemBundles = new SystemBundles();
        SystemBundle[] systemBundlesArray = new SystemBundle[2];
        systemBundlesArray[0] = new SystemBundle("bundle1", "1.0.0");
        systemBundlesArray[0].setId(0);
        systemBundlesArray[0].setState("ACTIVE");
        systemBundlesArray[1] = new SystemBundle("bundle2", "2.0.0");
        systemBundlesArray[1].setId(1);
        systemBundlesArray[1].setState("RESOLVED");
        systemBundlesArray[1].setSigned(true);
        systemBundles.setBundles(systemBundlesArray);

        String json = new JsonMarshallUnmarshallImpl().marshal(systemBundles);

        String expectedJson = "{\"bundles\":[{\"name\":\"bundle1\",\"version\":\"1.0.0\",\"id\":0,\"state\":\"ACTIVE\",\"signed\":false},{\"name\":\"bundle2\",\"version\":\"2.0.0\",\"id\":1,\"state\":\"RESOLVED\",\"signed\":true}]}";
        assertEquals(expectedJson, json);
    }

    @Test
    public void testSystemPackages() throws KuraException {
        List<SystemPackage> systemPackageList = new ArrayList<>();
        systemPackageList.add(new SystemPackage("package1", "1.0.0", SystemResourceType.DEB));
        systemPackageList.add(new SystemPackage("package2", "2.0.0", SystemResourceType.RPM));
        SystemPackages systemPackages = new SystemPackages(systemPackageList);

        String json = new JsonMarshallUnmarshallImpl().marshal(systemPackages);

        String expectedJson = "{\"systemPackages\":[{\"name\":\"package1\",\"version\":\"1.0.0\",\"type\":\"DEB\"},{\"name\":\"package2\",\"version\":\"2.0.0\",\"type\":\"RPM\"}]}";
        assertEquals(expectedJson, json);
    }

    @Test
    public void testSystemResourcesInfo() throws KuraException {
        List<SystemResourceInfo> SystemResourcesInfoList = new ArrayList<>();
        SystemResourcesInfoList.add(new SystemResourceInfo("package1", "1.0.0", SystemResourceType.DEB));
        SystemResourcesInfoList.add(new SystemResourceInfo("bundle1", "2.0.0", SystemResourceType.BUNDLE));
        SystemResourcesInfoList.add(new SystemResourceInfo("dp1", "3.0.0", SystemResourceType.DP));
        SystemResourcesInfo systemResourceInfo = new SystemResourcesInfo(SystemResourcesInfoList);

        String json = new JsonMarshallUnmarshallImpl().marshal(systemResourceInfo);

        String expectedJson = "{\"inventory\":[{\"name\":\"package1\",\"version\":\"1.0.0\",\"type\":\"DEB\"},{\"name\":\"bundle1\",\"version\":\"2.0.0\",\"type\":\"BUNDLE\"},{\"name\":\"dp1\",\"version\":\"3.0.0\",\"type\":\"DP\"}]}";
        assertEquals(expectedJson, json);
    }

}
