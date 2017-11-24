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

public final class GwtWireComponentDescriptor extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = -5716936754311577904L;

    private String factoryPid;

    private int minInputPorts;
    private int maxInputPorts;
    private int minOutputPorts;
    private int maxOutputPorts;

    public GwtWireComponentDescriptor() {
    }

    public GwtWireComponentDescriptor(String factoryPid, int minInputPorts, int maxInputPorts, int minOutputPorts,
            int maxOutputPorts) {
        this.factoryPid = factoryPid;
        this.minInputPorts = minInputPorts;
        this.maxInputPorts = maxInputPorts;
        this.minOutputPorts = minOutputPorts;
        this.maxOutputPorts = maxOutputPorts;
    }

    public String getFactoryPid() {
        return factoryPid;
    }

    public int getMinInputPorts() {
        return minInputPorts;
    }

    public int getMaxInputPorts() {
        return maxInputPorts;
    }

    public int getMinOutputPorts() {
        return minOutputPorts;
    }

    public int getMaxOutputPorts() {
        return maxOutputPorts;
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

}