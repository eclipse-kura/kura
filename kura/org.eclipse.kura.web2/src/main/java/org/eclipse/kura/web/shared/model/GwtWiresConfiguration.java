/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.annotation.Nullable;

/**
 * The Class GwtWiresConfiguration represents a POJO for all the Wires Configuration
 * needed for GWT to render the instances for Composer UI
 */
public final class GwtWiresConfiguration extends GwtBaseModel implements Serializable {

    /** Serial Version */
    private static final long serialVersionUID = 50782654510063453L;

    /** Wires Configuration for Graph in JSON. */
    private String graph;

    /** Wire Component Instances. */
    private final List<String> wireComponents;

    /** Wire Components Configuration in JSON. */
    @Nullable
    private String wireComponentsJson;

    /** Wire Configurations in JSON. */
    @Nullable
    private String wireConfigurationsJson;

    /** Wire Emitter Factory PIDs. */
    private final List<String> wireEmitterFactoryPids;

    /** Wire Receiver Factory PIDs. */
    private final List<String> wireReceiverFactoryPids;

    /** Wires Configuration in JSON. */
    @Nullable
    private String wiresConfigurationJson;

    /** Constructor */
    public GwtWiresConfiguration() {
        this.wireComponents = new ArrayList<String>();
        this.wireEmitterFactoryPids = new ArrayList<String>();
        this.wireReceiverFactoryPids = new ArrayList<String>();
        this.graph = "{}";
    }

    /**
     * Gets the graph.
     *
     * @return the graph
     */
    public String getGraph() {
        return this.graph;
    }

    /**
     * Gets the wire components.
     *
     * @return the wire components
     */
    public List<String> getWireComponents() {
        return this.wireComponents;
    }

    public String getWireComponentsJson() {
        return this.wireComponentsJson;
    }

    public String getWireConfigurationsJson() {
        return this.wireConfigurationsJson;
    }

    /**
     * Gets the wire emitter factory PIDs.
     *
     * @return the wire emitter factory PIDs
     */
    public List<String> getWireEmitterFactoryPids() {
        return this.wireEmitterFactoryPids;
    }

    /**
     * Gets the wire receiver factory PIDs.
     *
     * @return the wire receiver factory PIDs
     */
    public List<String> getWireReceiverFactoryPids() {
        return this.wireReceiverFactoryPids;
    }

    /**
     * Gets the wires configuration JSON.
     *
     * @return the wires configuration JSON
     */
    public String getWiresConfigurationJson() {
        return this.wiresConfigurationJson;
    }

    /**
     * Sets the graph.
     *
     * @param graph
     *            the new graph
     */
    public void setGraph(final String graph) {
        if (graph == null) {
            this.graph = "{}";
        }
        this.graph = graph;
    }

    public void setWireComponentsJson(final String json) {
        this.wireComponentsJson = json;
    }

    public void setWireConfigurationsJson(final String wireConfigurationsJson) {
        this.wireConfigurationsJson = wireConfigurationsJson;
    }

    public void setWiresConfigurationJson(final String wiresConfigurationJson) {
        this.wiresConfigurationJson = wiresConfigurationJson;
    }

}
