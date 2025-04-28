/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
import java.util.Optional;

public final class GwtDriversAndAssetsInfo extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = -2068309846272914698L;

    private List<GwtConfigComponent> componentDefinitions;

    private List<GwtConfigComponent> driverDescriptors;

    private GwtConfigComponent baseChannelDescriptor;

    private List<GwtConfigComponent> componentConfigurations;

    private List<String> allActivePids;

    public List<GwtConfigComponent> getComponentDefinitions() {
        return componentDefinitions;
    }

    public void setComponentDefinitions(List<GwtConfigComponent> componentDefinitions) {
        this.componentDefinitions = componentDefinitions;
    }

    public List<GwtConfigComponent> getDriverDescriptors() {
        return driverDescriptors;
    }

    public void setDriverDescriptors(List<GwtConfigComponent> driverDescriptors) {
        this.driverDescriptors = driverDescriptors;
    }

    public Optional<GwtConfigComponent> getBaseChannelDescriptor() {
        return Optional.ofNullable(baseChannelDescriptor);
    }

    public void setBaseChannelDescriptor(GwtConfigComponent baseChannelDescriptor) {
        this.baseChannelDescriptor = baseChannelDescriptor;
    }

    public List<GwtConfigComponent> getComponentConfigurations() {
        return componentConfigurations;
    }

    public void setComponentConfigurations(List<GwtConfigComponent> componentConfigurations) {
        this.componentConfigurations = componentConfigurations;
    }

    public List<String> getAllActivePids() {
        return allActivePids;
    }

    public void setAllActivePids(List<String> allActivePids) {
        this.allActivePids = allActivePids;
    }

}
