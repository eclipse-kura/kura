/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.rest.wire.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.kura.rest.configuration.api.AdDTO;
import org.eclipse.kura.wire.graph.WireComponentDefinition;

public class WireComponentDefinitionDTO {

    private final String factoryPid;

    private final int minInputPorts;
    private final int maxInputPorts;
    private final int defaultInputPorts;

    private final int minOutputPorts;
    private final int maxOutputPorts;
    private final int defaultOutputPorts;

    private final Map<Integer, String> inputPortNames;
    private final Map<Integer, String> outputPortNames;

    private final List<AdDTO> componentOCD;

    public WireComponentDefinitionDTO(final WireComponentDefinition definition) {
        this.factoryPid = definition.getFactoryPid();
        this.minInputPorts = definition.getMinInputPorts();
        this.maxInputPorts = definition.getMaxInputPorts();
        this.defaultInputPorts = definition.getDefaultInputPorts();
        this.minOutputPorts = definition.getMinOutputPorts();
        this.maxOutputPorts = definition.getMaxOutputPorts();
        this.defaultOutputPorts = definition.getDefaultOutputPorts();
        this.inputPortNames = definition.getInputPortNames();
        this.outputPortNames = definition.getOutputPortNames();

        if (definition.getComponentOCD() != null && definition.getComponentOCD().getDefinition() != null) {
            this.componentOCD = definition.getComponentOCD().getDefinition().getAD().stream().map(AdDTO::new)
                    .collect(Collectors.toList());
        } else {
            this.componentOCD = null;
        }
    }

    public String getFactoryPid() {
        return this.factoryPid;
    }

    public int getMinInputPorts() {
        return this.minInputPorts;
    }

    public int getMaxInputPorts() {
        return this.maxInputPorts;
    }

    public int getDefaultInputPorts() {
        return this.defaultInputPorts;
    }

    public int getMinOutputPorts() {
        return this.minOutputPorts;
    }

    public int getMaxOutputPorts() {
        return this.maxOutputPorts;
    }

    public int getDefaultOutputPorts() {
        return this.defaultOutputPorts;
    }

    public Map<Integer, String> getInputPortNames() {
        return this.inputPortNames;
    }

    public Map<Integer, String> getOutputPortNames() {
        return this.outputPortNames;
    }

    public List<AdDTO> getComponentOCD() {
        return this.componentOCD;
    }

}