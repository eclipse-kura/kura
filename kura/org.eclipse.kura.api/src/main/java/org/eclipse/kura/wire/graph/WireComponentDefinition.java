/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.wire.graph;

import java.util.Map;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be extended by clients.
 * @since 1.4
 */
@ProviderType
public class WireComponentDefinition {

    private String factoryPid;

    private int minInputPorts;
    private int maxInputPorts;
    private int defaultInputPorts;

    private int minOutputPorts;
    private int maxOutputPorts;
    private int defaultOutputPorts;

    private Map<Integer, String> inputPortNames;
    private Map<Integer, String> outputPortNames;

    private ComponentConfiguration componentOCD;

    public String getFactoryPid() {
        return this.factoryPid;
    }

    public void setFactoryPid(String factoryPid) {
        this.factoryPid = factoryPid;
    }

    public int getMinInputPorts() {
        return this.minInputPorts;
    }

    public void setMinInputPorts(int minInputPorts) {
        this.minInputPorts = minInputPorts;
    }

    public int getMaxInputPorts() {
        return this.maxInputPorts;
    }

    public void setMaxInputPorts(int maxInputPorts) {
        this.maxInputPorts = maxInputPorts;
    }

    public int getDefaultInputPorts() {
        return this.defaultInputPorts;
    }

    public void setDefaultInputPorts(int defaultInputPorts) {
        this.defaultInputPorts = defaultInputPorts;
    }

    public int getMinOutputPorts() {
        return this.minOutputPorts;
    }

    public void setMinOutputPorts(int minOutputPorts) {
        this.minOutputPorts = minOutputPorts;
    }

    public int getMaxOutputPorts() {
        return this.maxOutputPorts;
    }

    public void setMaxOutputPorts(int maxOutputPorts) {
        this.maxOutputPorts = maxOutputPorts;
    }

    public int getDefaultOutputPorts() {
        return this.defaultOutputPorts;
    }

    public void setDefaultOutputPorts(int defaultOutputPorts) {
        this.defaultOutputPorts = defaultOutputPorts;
    }

    public Map<Integer, String> getInputPortNames() {
        return this.inputPortNames;
    }

    public void setInputPortNames(Map<Integer, String> inputPortNames) {
        this.inputPortNames = inputPortNames;
    }

    public Map<Integer, String> getOutputPortNames() {
        return this.outputPortNames;
    }

    public void setOutputPortNames(Map<Integer, String> outputPortNames) {
        this.outputPortNames = outputPortNames;
    }

    public ComponentConfiguration getComponentOCD() {
        return this.componentOCD;
    }

    public void setComponentOCD(ComponentConfiguration componentOCD) {
        this.componentOCD = componentOCD;
    }
}
