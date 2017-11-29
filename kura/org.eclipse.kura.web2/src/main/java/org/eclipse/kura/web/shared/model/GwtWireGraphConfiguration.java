/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.List;

/**
 * The Class GwtWiresConfiguration represents a POJO for all the Wires Configuration
 * needed for GWT to render the instances for Composer UI
 */
public final class GwtWireGraphConfiguration extends GwtBaseModel implements Serializable {

    /** Serial Version */
    private static final long serialVersionUID = 50782654510063453L;

    private List<String> allActivePids;
    private List<GwtWireComponentConfiguration> wireComponentConfigurations;
    private List<GwtConfigComponent> additionalConfigurations;
    private List<GwtWireConfiguration> wires;

    public GwtWireGraphConfiguration() {
    }

    public List<GwtConfigComponent> getAdditionalConfigurations() {
        return additionalConfigurations;
    }

    public void setAdditionalConfigurations(List<GwtConfigComponent> additionalConfigurations) {
        this.additionalConfigurations = additionalConfigurations;
    }

    public List<GwtWireConfiguration> getWires() {
        return wires;
    }

    public void setWires(List<GwtWireConfiguration> wires) {
        this.wires = wires;
    }

    public List<GwtWireComponentConfiguration> getWireComponentConfigurations() {
        return wireComponentConfigurations;
    }

    public void setWireComponentConfigurations(List<GwtWireComponentConfiguration> wireComponentConfigurations) {
        this.wireComponentConfigurations = wireComponentConfigurations;
    }

    public List<String> getAllActivePids() {
        return allActivePids;
    }

    public void setAllActivePids(List<String> allActivePids) {
        this.allActivePids = allActivePids;
    }

}
