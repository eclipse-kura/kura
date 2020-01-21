/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.Map;

public final class GwtWireComponentDescriptor extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = -5716936754311577904L;

    private String name;
    private String factoryPid;

    private int minInputPorts;
    private int maxInputPorts;
    private int defaultInputPorts;
    private int minOutputPorts;
    private int maxOutputPorts;
    private int defaultOutputPorts;
    private Map<Integer, String> inputPortNames;
    private Map<Integer, String> outputPortNames;

    public GwtWireComponentDescriptor() {
    }

    public GwtWireComponentDescriptor(String name, String factoryPid, int minInputPorts, int maxInputPorts,
            int defaultInputPorts, int minOutputPorts, int maxOutputPorts, int defaultOutputPorts,
            Map<Integer, String> inputPortNames, Map<Integer, String> outputPortNames) {
        this.name = name;
        this.factoryPid = factoryPid;
        this.minInputPorts = minInputPorts;
        this.maxInputPorts = maxInputPorts;
        this.minOutputPorts = minOutputPorts;
        this.maxOutputPorts = maxOutputPorts;
        this.defaultInputPorts = defaultInputPorts;
        this.defaultOutputPorts = defaultOutputPorts;
        this.inputPortNames = inputPortNames;
        this.outputPortNames = outputPortNames;
    }

    public String getName() {
        return this.name;
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

    public int getMinOutputPorts() {
        return this.minOutputPorts;
    }

    public int getMaxOutputPorts() {
        return this.maxOutputPorts;
    }

    public int getDefaultInputPorts() {
        return this.defaultInputPorts;
    }

    public int getDefaultOutputPorts() {
        return this.defaultOutputPorts;
    }

    public void setFactoryPid(String factoryPid) {
        this.factoryPid = factoryPid;
    }

    public void setMinInputPorts(int minInputPorts) {
        this.minInputPorts = minInputPorts;
    }

    public void setMaxInputPorts(int maxInputPorts) {
        this.maxInputPorts = maxInputPorts;
    }

    public void setMinOutputPorts(int minOutputPorts) {
        this.minOutputPorts = minOutputPorts;
    }

    public void setMaxOutputPorts(int maxOutputPorts) {
        this.maxOutputPorts = maxOutputPorts;
    }

    public void setDefaultInputPorts(int defaultInputPorts) {
        this.defaultInputPorts = defaultInputPorts;
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
}