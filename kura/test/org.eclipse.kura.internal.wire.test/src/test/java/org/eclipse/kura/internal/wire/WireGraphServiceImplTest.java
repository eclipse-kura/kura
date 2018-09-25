/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.wire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.JsonMarshallUnmarshallImpl;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.graph.MultiportWireConfiguration;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class WireGraphServiceImplTest {

    private static final String WIRE_GRAPH_PROPERTY_NAME = "WireGraph";
    private static final String WIRE_SERVICE_PID = "org.eclipse.kura.wire.graph.WireGraphService";
    private static final String DEFAULT_GRAPH = "{\"components\":[],\"wires\":[]}";
    private static final String SIMPLE_GRAPH = "{\"components\":[{\"pid\":\"emitterPid\",\"inputPortCount\":0,\"outputPortCount\":1,\"renderingProperties\":{\"position\":{\"x\":10,\"y\":15},\"inputPortNames\":{},\"outputPortNames\":{}}},{\"pid\":\"receiverPid\",\"inputPortCount\":1,\"outputPortCount\":0,\"renderingProperties\":{\"position\":{\"x\":100,\"y\":150},\"inputPortNames\":{},\"outputPortNames\":{}}}],\"wires\":[{\"emitter\":\"emitterPid\",\"emitterPort\":0,\"receiver\":\"receiverPid\",\"receiverPort\":0}]}";
    private static final String SIMPLE_GRAPH_NO_ARCS = "{\"components\":[{\"pid\":\"emitterPid\",\"inputPortCount\":0,\"outputPortCount\":1,\"renderingProperties\":{\"position\":{\"x\":10,\"y\":15},\"inputPortNames\":{},\"outputPortNames\":{}}},{\"pid\":\"receiverPid\",\"inputPortCount\":1,\"outputPortCount\":0,\"renderingProperties\":{\"position\":{\"x\":100,\"y\":150},\"inputPortNames\":{},\"outputPortNames\":{}}}],\"wires\":[]}";

    @Test
    public void testCreateWiresNoEmitterNoReceiver() throws NoSuchFieldException, InvalidSyntaxException {
        String emitterPid = "emitterPid";
        String receiverPid = "receiverPid";

        final Map<String, String> servicePidMappings = new HashMap<>();
        servicePidMappings.put(emitterPid, emitterPid);
        servicePidMappings.put(receiverPid, receiverPid);

        WireGraphServiceImpl wsi = getWireGraphServiceImpl(servicePidMappings);

        WireAdmin wireAdmin = mock(WireAdmin.class);

        TestUtil.setFieldValue(wsi, "wireAdmin", wireAdmin);

        ServiceTracker<WireComponent, WireComponent> wireComponentServiceTracker = mock(ServiceTracker.class);
        TestUtil.setFieldValue(wsi, "wireComponentServiceTracker", wireComponentServiceTracker);

        Map<String, Object> properties = new HashMap<>();
        properties.put(WIRE_GRAPH_PROPERTY_NAME, SIMPLE_GRAPH);

        wsi.activate(mock(ComponentContext.class), properties);
    }

    @Test
    public void testCreateWires() throws NoSuchFieldException, InvalidSyntaxException {
        final Map<String, String> servicePidMappings = mock(Map.class);

        WireGraphServiceImpl wsi = getWireGraphServiceImpl(servicePidMappings);

        Map<String, Object> properties = new HashMap<>();
        properties.put(WIRE_GRAPH_PROPERTY_NAME, SIMPLE_GRAPH);

        String emitterPid = "emitterPid";
        String receiverPid = "receiverPid";

        WireAdmin wireAdmin = mock(WireAdmin.class);

        TestUtil.setFieldValue(wsi, "wireAdmin", wireAdmin);

        wsi.activate(mock(ComponentContext.class), properties);
        wsi.createWires();

        verify(servicePidMappings, times(1)).get(emitterPid);
        verify(servicePidMappings, times(1)).get(receiverPid);
    }

    @Test
    public void testWireGraphUpdate() throws KuraException, NoSuchFieldException, InvalidSyntaxException {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        WireAdmin wireAdmin = mock(WireAdmin.class);
        when(wireAdmin.getWires(null)).thenReturn(new Wire[0]);

        WireGraphServiceImpl wireGraphService = getWireGraphServiceImpl(new HashMap<>());

        Map<String, Object> properties = new HashMap<>();
        properties.put(WIRE_GRAPH_PROPERTY_NAME, DEFAULT_GRAPH);

        TestUtil.setFieldValue(wireGraphService, "configurationService", configurationService);
        TestUtil.setFieldValue(wireGraphService, "wireAdmin", wireAdmin);

        wireGraphService.activate(mock(ComponentContext.class), properties);

        ServiceTracker<WireComponent, WireComponent> wireComponentServiceTracker = mock(ServiceTracker.class);
        TestUtil.setFieldValue(wireGraphService, "wireComponentServiceTracker", wireComponentServiceTracker);

        WireComponentConfiguration emitterWireComponentConfiguration = createEmitterWireComponentConfiguration();
        WireComponentConfiguration receiverWireComponentConfiguration = createReceiverWireComponentConfiguration();

        List<WireComponentConfiguration> wireComponentConfigurations = new ArrayList<>();
        wireComponentConfigurations.add(emitterWireComponentConfiguration);
        wireComponentConfigurations.add(receiverWireComponentConfiguration);

        MultiportWireConfiguration wire = new MultiportWireConfiguration("emitterPid", "receiverPid", 0, 0);
        List<MultiportWireConfiguration> wireConfigurations = new ArrayList<>();
        wireConfigurations.add(wire);

        WireGraphConfiguration wireGraphConfiguration = new WireGraphConfiguration(wireComponentConfigurations,
                wireConfigurations);

        when(configurationService.getComponentConfiguration(WIRE_SERVICE_PID))
                .thenReturn(new ComponentConfigurationImpl(WIRE_SERVICE_PID, null, new HashMap<>()));

        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        wireGraphService.update(wireGraphConfiguration);

        verify(configurationService).updateConfigurations(captor.capture(), eq(true));

        List<ComponentConfiguration> arguments = captor.getValue();

        assertNotNull(arguments);

        assertEquals(1, arguments.size());

        verify(configurationService, times(0)).deleteFactoryConfiguration(Matchers.anyString(), eq(false));
        verify(configurationService, times(2)).createFactoryConfiguration(Matchers.anyString(), Matchers.anyString(),
                Matchers.anyMap(), eq(false));
        verify(configurationService, times(1)).updateConfigurations(Matchers.anyList(), eq(true));

        for (ComponentConfiguration componentConfiguration : arguments) {
            if (componentConfiguration.getPid().equals(WIRE_SERVICE_PID)) {
                String persistenceJson = (String) componentConfiguration.getConfigurationProperties()
                        .get(WIRE_GRAPH_PROPERTY_NAME);
                assertEquals(SIMPLE_GRAPH, persistenceJson);
            }
        }
    }

    @Test
    public void testWireGraphUpdateComponentsToDelete()
            throws KuraException, NoSuchFieldException, InvalidSyntaxException {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        WireAdmin wireAdmin = mock(WireAdmin.class);
        when(wireAdmin.getWires(null)).thenReturn(new Wire[0]);

        WireGraphServiceImpl wireGraphService = getWireGraphServiceImpl(new HashMap<>());

        Map<String, Object> properties = new HashMap<>();
        properties.put(WIRE_GRAPH_PROPERTY_NAME, SIMPLE_GRAPH);

        TestUtil.setFieldValue(wireGraphService, "configurationService", configurationService);
        TestUtil.setFieldValue(wireGraphService, "wireAdmin", wireAdmin);

        wireGraphService.activate(mock(ComponentContext.class), properties);

        ServiceTracker<WireComponent, WireComponent> wireComponentServiceTracker = mock(ServiceTracker.class);
        TestUtil.setFieldValue(wireGraphService, "wireComponentServiceTracker", wireComponentServiceTracker);

        List<WireComponentConfiguration> wireComponentConfigurations = new ArrayList<>();

        List<MultiportWireConfiguration> wireConfigurations = new ArrayList<>();

        WireGraphConfiguration wireGraphConfiguration = new WireGraphConfiguration(wireComponentConfigurations,
                wireConfigurations);

        when(configurationService.getComponentConfiguration(WIRE_SERVICE_PID))
                .thenReturn(new ComponentConfigurationImpl(WIRE_SERVICE_PID, null, new HashMap<>()));

        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        wireGraphService.update(wireGraphConfiguration);

        verify(configurationService).updateConfigurations(captor.capture(), eq(true));

        List<ComponentConfiguration> arguments = captor.getValue();

        assertNotNull(arguments);

        assertEquals(1, arguments.size());

        for (ComponentConfiguration componentConfiguration : arguments) {
            if (componentConfiguration.getPid().equals(WIRE_SERVICE_PID)) {
                String persistenceJson = (String) componentConfiguration.getConfigurationProperties()
                        .get(WIRE_GRAPH_PROPERTY_NAME);
                assertEquals(DEFAULT_GRAPH, persistenceJson);
            }
        }
    }

    @Test
    public void testGetWireGraph() throws NoSuchFieldException, KuraException {
        ConfigurationService configurationService = mock(ConfigurationService.class);

        WireGraphServiceImpl wireGraphService = getWireGraphServiceImpl(new HashMap<>());

        TestUtil.setFieldValue(wireGraphService, "configurationService", configurationService);

        Map<String, Object> properties = new HashMap<>();
        properties.put("kura.service.pid", WIRE_SERVICE_PID);
        properties.put(WIRE_GRAPH_PROPERTY_NAME, SIMPLE_GRAPH);

        ComponentConfiguration wireGraphServiceComponentConfig = createWireGraphServiceComponentConfiguration();
        ComponentConfiguration emitterComponentConfig = createEmitterComponentConfiguration();
        ComponentConfiguration receiverComponentConfig = createReceiverComponentConfiguration();

        List<ComponentConfiguration> configServiceComponentConfigurations = new ArrayList<>();
        configServiceComponentConfigurations.add(wireGraphServiceComponentConfig);
        configServiceComponentConfigurations.add(emitterComponentConfig);
        configServiceComponentConfigurations.add(receiverComponentConfig);

        when(configurationService.getComponentConfigurations()).thenReturn(configServiceComponentConfigurations);

        wireGraphService.activate(mock(ComponentContext.class), properties);
        WireGraphConfiguration wireGraphConfiguration = wireGraphService.get();

        assertNotNull(wireGraphConfiguration);

        assertEquals(2, wireGraphConfiguration.getWireComponentConfigurations().size());
        assertEquals(1, wireGraphConfiguration.getWireConfigurations().size());
    }

    @Test
    public void testDeleteWireGraph() throws NoSuchFieldException, InvalidSyntaxException, KuraException {

        ConfigurationService configurationService = mock(ConfigurationService.class);
        WireAdmin wireAdmin = mock(WireAdmin.class);
        when(wireAdmin.getWires(null)).thenReturn(new Wire[0]);
        when(configurationService.getComponentConfiguration(WIRE_SERVICE_PID))
                .thenReturn(new ComponentConfigurationImpl(WIRE_SERVICE_PID, null, new HashMap<>()));

        WireGraphServiceImpl wireGraphService = getWireGraphServiceImpl(new HashMap<>());

        Map<String, Object> properties = new HashMap<>();
        properties.put(WIRE_GRAPH_PROPERTY_NAME, SIMPLE_GRAPH);

        TestUtil.setFieldValue(wireGraphService, "configurationService", configurationService);
        TestUtil.setFieldValue(wireGraphService, "wireAdmin", wireAdmin);

        wireGraphService.activate(mock(ComponentContext.class), properties);
        wireGraphService.delete();

        final ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(configurationService).updateConfiguration(eq(WIRE_SERVICE_PID), captor.capture(), eq(true));

        Map<String, Object> arguments = captor.getValue();

        assertNotNull(arguments);

        assertEquals(1, arguments.size());
        assertEquals("{\"components\":[],\"wires\":[]}", arguments.get(WIRE_GRAPH_PROPERTY_NAME));
    }

    private WireGraphServiceImpl getWireGraphServiceImpl(final Map<String, String> servicePidMappings) {
        WireGraphServiceImpl wireGraphService = new WireGraphServiceImpl() {

            @Override
            protected <T> T unmarshal(String jsonWireGraph, Class<T> clazz) {
                JsonMarshallUnmarshallImpl jsonMarshaller = new JsonMarshallUnmarshallImpl();
                try {
                    return jsonMarshaller.unmarshal(jsonWireGraph, clazz);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected String marshal(Object object) {
                JsonMarshallUnmarshallImpl jsonMarshaller = new JsonMarshallUnmarshallImpl();
                try {
                    return jsonMarshaller.marshal(object);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected String getServicePidByKuraServicePid(String kuraServicePid) {
                return servicePidMappings.get(kuraServicePid);
            }
        };
        return wireGraphService;
    }

    private ComponentConfiguration createEmitterComponentConfiguration() {
        Map<String, Object> emitterProperties = new HashMap<>();
        String emitterPid = "emitterPid";
        emitterProperties.put("kura.service.pid", emitterPid);
        emitterProperties.put("fakeProp1", "value1");
        return new ComponentConfigurationImpl(emitterPid, null, emitterProperties);
    }

    private ComponentConfiguration createWireGraphServiceComponentConfiguration() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("kura.service.pid", WIRE_SERVICE_PID);
        properties.put(WIRE_GRAPH_PROPERTY_NAME, SIMPLE_GRAPH);
        return new ComponentConfigurationImpl(WIRE_SERVICE_PID, null, properties);
    }

    private WireComponentConfiguration createEmitterWireComponentConfiguration() {

        String emitterPid = "emitterPid";
        ComponentConfiguration emitterComponentConfig = createEmitterComponentConfiguration();

        Map<String, Object> visualizationProps = new HashMap<>();
        visualizationProps.put("pid", emitterPid);
        visualizationProps.put("position.x", 10f);
        visualizationProps.put("position.y", 15f);
        visualizationProps.put("inputPortCount", 0);
        visualizationProps.put("outputPortCount", 1);

        return new WireComponentConfiguration(emitterComponentConfig, visualizationProps);
    }

    private ComponentConfiguration createReceiverComponentConfiguration() {
        Map<String, Object> receiverProperties = new HashMap<>();
        String receiverPid = "receiverPid";
        receiverProperties.put("kura.service.pid", receiverPid);
        receiverProperties.put("fakeProp1", "value2");
        return new ComponentConfigurationImpl(receiverPid, null, receiverProperties);
    }

    private WireComponentConfiguration createReceiverWireComponentConfiguration() {

        String receiverPid = "receiverPid";
        ComponentConfiguration emitterComponentConfig = createReceiverComponentConfiguration();

        Map<String, Object> visualizationProps = new HashMap<>();
        visualizationProps.put("pid", receiverPid);
        visualizationProps.put("position.x", 100f);
        visualizationProps.put("position.y", 150f);
        visualizationProps.put("inputPortCount", 1);
        visualizationProps.put("outputPortCount", 0);

        return new WireComponentConfiguration(emitterComponentConfig, visualizationProps);
    }
}
