/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.wire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;

public class WireServiceImplTest {

    private static final String WIRE_SERVICE_PID = "org.eclipse.kura.wire.WireService";
    private static final String DEFAULT_GRAPH = "{\"components\":[],\"wires\":[]}";
    private static final String SIMPLE_GRAPH = "{\"components\":[{\"pid\":\"emitterPid\",\"inputPortCount\":0,\"outputPortCount\":1,\"renderingProperties\":{\"position\":{\"x\":10,\"y\":15},\"inputPortNames\":{},\"outputPortNames\":{}}},{\"pid\":\"receiverPid\",\"inputPortCount\":1,\"outputPortCount\":0,\"renderingProperties\":{\"position\":{\"x\":100,\"y\":150},\"inputPortNames\":{},\"outputPortNames\":{}}}],\"wires\":[{\"emitter\":\"emitterPid\",\"receiver\":\"receiverPid\"}]}";

    @Test
    public void testCreateWiresNoEmitterNoReceiver() throws NoSuchFieldException, InvalidSyntaxException {
        WireServiceImpl wsi = new WireServiceImpl();

        Set<WireConfiguration> wireConfigs = new HashSet<>();
        String emitterPid = "emmiter";
        String receiverPid = "receiver";
        WireConfiguration wc = new WireConfiguration(emitterPid, receiverPid);
        wireConfigs.add(wc);

        TestUtil.setFieldValue(wsi, "wireConfigs", wireConfigs);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        WireComponentTrackerCustomizer wctc = new WireComponentTrackerCustomizer(bundleCtxMock, wsi);

        TestUtil.setFieldValue(wsi, "wireComponentTrackerCustomizer", wctc);

        wsi.createWires();
    }

    @Test
    public void testCreateWires() throws NoSuchFieldException, InvalidSyntaxException {
        WireServiceImpl wsi = new WireServiceImpl();

        Set<WireConfiguration> wireConfigs = new HashSet<>();
        String emitterPid = "emmiter";
        String receiverPid = "receiver";
        WireConfiguration wc = new WireConfiguration(emitterPid, receiverPid);
        wireConfigs.add(wc);

        TestUtil.setFieldValue(wsi, "wireConfigs", wireConfigs);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        WireComponentTrackerCustomizer wctc = new WireComponentTrackerCustomizer(bundleCtxMock, wsi);

        TestUtil.setFieldValue(wsi, "wireComponentTrackerCustomizer", wctc);

        List<String> wireEmitterPids = new ArrayList<>();
        wireEmitterPids.add(emitterPid);
        List<String> wireReceiverPids = new ArrayList<>();
        wireReceiverPids.add(receiverPid);

        TestUtil.setFieldValue(wctc, "wireEmitterPids", wireEmitterPids);
        TestUtil.setFieldValue(wctc, "wireReceiverPids", wireReceiverPids);

        WireHelperService whsMock = mock(WireHelperService.class);

        TestUtil.setFieldValue(wsi, "wireHelperService", whsMock);

        wsi.createWires();

        verify(whsMock, times(1)).getServicePid(emitterPid);
        verify(whsMock, times(1)).getServicePid(receiverPid);
    }

    @Test
    @Ignore
    public void testUpdatedCallback() throws NoSuchFieldException {
        WireServiceImpl wsi = new WireServiceImpl();

        assertNull(TestUtil.getFieldValue(wsi, "properties"));

        Map<String, Object> properties = new HashMap<>();
        wsi.updated(properties);

        assertEquals(properties, TestUtil.getFieldValue(wsi, "properties"));
    }

    @Test
    public void testWireGraphUpdate() throws KuraException, NoSuchFieldException, InvalidSyntaxException {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        WireAdmin wireAdmin = mock(WireAdmin.class);
        when(wireAdmin.getWires(null)).thenReturn(new Wire[0]);

        WireGraphService wireGraphService = new WireServiceImpl();

        Map<String, Object> properties = new HashMap<>();
        properties.put("WireGraph", DEFAULT_GRAPH);

        TestUtil.setFieldValue(wireGraphService, "configurationService", configurationService);
        TestUtil.setFieldValue(wireGraphService, "wireAdmin", wireAdmin);
        TestUtil.setFieldValue(wireGraphService, "properties", properties);

        WireComponentConfiguration emitterWireComponentConfiguration = createEmitterWireComponentConfiguration();
        WireComponentConfiguration receiverWireComponentConfiguration = createReceiverWireComponentConfiguration();

        List<WireComponentConfiguration> wireComponentConfigurations = new ArrayList<>();
        wireComponentConfigurations.add(emitterWireComponentConfiguration);
        wireComponentConfigurations.add(receiverWireComponentConfiguration);

        WireConfiguration wire = new WireConfiguration("emitterPid", "receiverPid");
        List<WireConfiguration> wireConfigurations = new ArrayList<>();
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
                String persistenceJson = (String) componentConfiguration.getConfigurationProperties().get("WireGraph");
                assertEquals(SIMPLE_GRAPH, persistenceJson);
            }
        }
    }

    @Test
    public void testGetWireGraph() throws NoSuchFieldException, KuraException {
        ConfigurationService configurationService = mock(ConfigurationService.class);

        WireGraphService wireGraphService = new WireServiceImpl();

        TestUtil.setFieldValue(wireGraphService, "configurationService", configurationService);

        Map<String, Object> properties = new HashMap<>();
        properties.put("kura.service.pid", WIRE_SERVICE_PID);
        properties.put("WireGraph", SIMPLE_GRAPH);
        TestUtil.setFieldValue(wireGraphService, "properties", properties);

        ComponentConfiguration wireServiceComponentConfig = createWireServiceComponentConfiguration();
        ComponentConfiguration emitterComponentConfig = createEmitterComponentConfiguration();
        ComponentConfiguration receiverComponentConfig = createReceiverComponentConfiguration();

        List<ComponentConfiguration> configServiceComponentConfigurations = new ArrayList<>();
        configServiceComponentConfigurations.add(wireServiceComponentConfig);
        configServiceComponentConfigurations.add(emitterComponentConfig);
        configServiceComponentConfigurations.add(receiverComponentConfig);

        when(configurationService.getComponentConfigurations()).thenReturn(configServiceComponentConfigurations);

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
        when(configurationService.getComponentConfiguration(WIRE_SERVICE_PID)).thenReturn(new ComponentConfigurationImpl(WIRE_SERVICE_PID, null, new HashMap<>()));

        WireGraphService wireGraphService = new WireServiceImpl();

        Map<String, Object> properties = new HashMap<>();
        properties.put("WireGraph", SIMPLE_GRAPH);

        TestUtil.setFieldValue(wireGraphService, "configurationService", configurationService);
        TestUtil.setFieldValue(wireGraphService, "wireAdmin", wireAdmin);
        TestUtil.setFieldValue(wireGraphService, "properties", properties);
        
        wireGraphService.delete();
        
        final ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(configurationService).updateConfiguration(eq(WIRE_SERVICE_PID), captor.capture(), eq(true));

        Map<String,Object> arguments = captor.getValue();

        assertNotNull(arguments);

        assertEquals(1, arguments.size());
        assertEquals("{\"components\":[],\"wires\":[]}", arguments.get("WireGraph"));
    }

    private ComponentConfiguration createEmitterComponentConfiguration() {
        Map<String, Object> emitterProperties = new HashMap<>();
        String emitterPid = "emitterPid";
        emitterProperties.put("kura.service.pid", emitterPid);
        emitterProperties.put("fakeProp1", "value1");
        return new ComponentConfigurationImpl(emitterPid, null, emitterProperties);
    }

    private ComponentConfiguration createWireServiceComponentConfiguration() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("kura.service.pid", WIRE_SERVICE_PID);
        properties.put("WireGraph", SIMPLE_GRAPH);
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
