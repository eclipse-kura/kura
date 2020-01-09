/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.util.wire.test;

import static org.eclipse.kura.util.wire.test.WireTestUtil.componentsActivated;
import static org.eclipse.kura.util.wire.test.WireTestUtil.wiresConnected;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.wire.graph.MultiportWireConfiguration;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;

public class GraphBuilder {

    private static final String TEST_EMITTER_RECEIVER_FACTORY_PID = "org.eclipse.kura.util.wire.test.TestEmitterReceiver";

    private static final String PROP_WIRE_COMP_POS_X = "position.x";
    private static final String PROP_WIRE_COMP_POS_Y = "position.y";
    private static final String PROP_WIRE_COMP_IN_CNT = "inputPortCount";
    private static final String PROP_WIRE_COMP_OUT_CNT = "outputPortCount";

    private final List<WireComponentConfiguration> configurations = new ArrayList<>();
    private final Set<MultiportWireConfiguration> wires = new HashSet<>();

    private final Map<String, Object> trackedObjects = new HashMap<>();

    public GraphBuilder addTestEmitterReceiver(final String pid) {
        return addWireComponent(pid, TEST_EMITTER_RECEIVER_FACTORY_PID);
    }

    public GraphBuilder addWireComponent(final String pid, final String factoryPid) {
        return addWireComponent(pid, factoryPid, Collections.emptyMap(), 1, 1);
    }

    public GraphBuilder addWireComponent(final String pid, final String factoryPid, final int inputPortCount,
            final int outputPortCount) {
        return addWireComponent(pid, factoryPid, Collections.emptyMap(), inputPortCount, outputPortCount);
    }

    public GraphBuilder addWireComponent(final String pid, final String factoryPid,
            final Map<String, Object> configuration, final int inputPortCount, final int outputPortCount) {
        Map<String, Object> props = new HashMap<>();
        props.put(PROP_WIRE_COMP_POS_X, 0f);
        props.put(PROP_WIRE_COMP_POS_Y, 0f);
        props.put(PROP_WIRE_COMP_IN_CNT, inputPortCount);
        props.put(PROP_WIRE_COMP_OUT_CNT, outputPortCount);
        return addWireComponent(pid, factoryPid, configuration, props);
    }

    public GraphBuilder addWireComponent(final String pid, final String factoryPid,
            final Map<String, Object> configuration, final Map<String, Object> renderingProperties) {

        final ComponentConfiguration componentConfiguration = new ComponentConfiguration() {

            @Override
            public String getPid() {
                return pid;
            }

            @Override
            public OCD getDefinition() {
                return null;
            }

            @Override
            public Map<String, Object> getConfigurationProperties() {
                Map<String, Object> properties = new HashMap<>(configuration);
                properties.put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);
                return properties;
            }
        };

        final WireComponentConfiguration wireComponentConfiguration = new WireComponentConfiguration(
                componentConfiguration, renderingProperties);

        this.configurations.add(wireComponentConfiguration);

        return this;
    }

    public GraphBuilder addWire(final String emitterPid, final String receiverPid) {
        return addWire(emitterPid, 0, receiverPid, 0);
    }

    public GraphBuilder addWire(final String emitterPid, final String receiverPid, final int receiverPort) {
        return addWire(emitterPid, 0, receiverPid, receiverPort);
    }

    public GraphBuilder addWire(final String emitterPid, final int emitterPort, final String receiverPid,
            final int receiverPort) {
        final MultiportWireConfiguration multiportWireConfiguration = new MultiportWireConfiguration(emitterPid,
                receiverPid, emitterPort, receiverPort);
        wires.add(multiportWireConfiguration);

        return this;
    }

    public CompletableFuture<Void> replaceExistingGraph(final BundleContext context,
            final WireGraphService wireGraphService) throws KuraException {

        final WireGraphConfiguration wireGraph = new WireGraphConfiguration(this.configurations,
                new ArrayList<>(this.wires));

        wireGraphService.delete();

        final Set<String> expectedPids = this.configurations.stream().map(c -> c.getConfiguration().getPid())
                .collect(Collectors.toSet());

        final CompletableFuture<Void> result = wiresConnected(context, this.wires)
                .thenCompose(ok -> componentsActivated(context, expectedPids, trackedObjects::put));

        wireGraphService.update(wireGraph);

        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> T getTrackedWireComponent(final String pid) {
        return (T) trackedObjects.get(pid);
    }

}
