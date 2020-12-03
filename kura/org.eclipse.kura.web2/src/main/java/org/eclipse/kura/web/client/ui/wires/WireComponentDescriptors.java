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
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.wires;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.web.client.ui.wires.composer.PortNames;
import org.eclipse.kura.web.client.ui.wires.composer.WireComponent;
import org.eclipse.kura.web.client.ui.wires.composer.WireComponentRenderingProperties;
import org.eclipse.kura.web.shared.model.GwtWireComponentDescriptor;

public class WireComponentDescriptors {

    private final Map<String, GwtWireComponentDescriptor> descriptors = new HashMap<>();

    public void setDescriptors(List<GwtWireComponentDescriptor> descriptors) {
        this.descriptors.clear();
        for (GwtWireComponentDescriptor desc : descriptors) {
            this.descriptors.put(desc.getFactoryPid(), desc);
        }
    }

    public GwtWireComponentDescriptor getDescriptor(String factoryPid) {
        return this.descriptors.get(factoryPid);
    }

    public Map<String, GwtWireComponentDescriptor> getDescriptors() {
        return this.descriptors;
    }

    public WireComponent createNewComponent(String pid, String factoryPid) {
        final GwtWireComponentDescriptor descriptor = this.descriptors.get(factoryPid);
        if (descriptor == null) {
            return null;
        }

        final WireComponent result = WireComponent.create();

        result.setPid(pid);
        result.setFactoryPid(factoryPid);
        result.setInputPortCount(descriptor.getMinInputPorts());
        result.setOutputPortCount(descriptor.getMinOutputPorts());

        final WireComponentRenderingProperties renderingProperties = WireComponentRenderingProperties.create();
        renderingProperties.setInputPortNames(PortNames.fromMap(descriptor.getInputPortNames()));
        renderingProperties.setOutputPortNames(PortNames.fromMap(descriptor.getOutputPortNames()));
        result.setRenderingProperties(renderingProperties);

        return result;
    }
}
