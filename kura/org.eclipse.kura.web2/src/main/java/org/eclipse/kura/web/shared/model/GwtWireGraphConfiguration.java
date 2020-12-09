/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
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
        return this.additionalConfigurations;
    }

    public void setAdditionalConfigurations(List<GwtConfigComponent> additionalConfigurations) {
        this.additionalConfigurations = additionalConfigurations;
    }

    public List<GwtWireConfiguration> getWires() {
        return this.wires;
    }

    public void setWires(List<GwtWireConfiguration> wires) {
        this.wires = wires;
    }

    public List<GwtWireComponentConfiguration> getWireComponentConfigurations() {
        return this.wireComponentConfigurations;
    }

    public void setWireComponentConfigurations(List<GwtWireComponentConfiguration> wireComponentConfigurations) {
        this.wireComponentConfigurations = wireComponentConfigurations;
    }

    public List<String> getAllActivePids() {
        return this.allActivePids;
    }

    public void setAllActivePids(List<String> allActivePids) {
        this.allActivePids = allActivePids;
    }

}
