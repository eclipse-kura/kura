/**
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 */

package org.eclipse.kura.internal.driver.opcua.request;

import static org.eclipse.kura.internal.driver.opcua.Utils.tryExtract;

import java.util.Map;

import org.eclipse.kura.internal.driver.opcua.NodeIdType;
import org.eclipse.kura.internal.driver.opcua.OpcUaChannelDescriptor;
import org.eclipse.kura.internal.driver.opcua.Utils;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

public class ReadParams {

    private final ReadValueId readValueId;

    public ReadParams(final ReadValueId readValueId) {
        this.readValueId = readValueId;
    }

    public ReadParams(final Map<String, Object> channelConfig) {
        final int nodeNamespaceIndex = Utils.tryExtract(channelConfig, OpcUaChannelDescriptor::getNodeNamespaceIndex,
                "Error while retrieving Node Namespace index");
        final NodeIdType nodeIdType = tryExtract(channelConfig, OpcUaChannelDescriptor::getNodeIdType,
                "Error while retrieving Node ID type");

        final NodeId nodeId = tryExtract(channelConfig,
                config -> OpcUaChannelDescriptor.getNodeId(config, nodeNamespaceIndex, nodeIdType),
                "Error while retrieving Node ID");

        final AttributeId attributeId = tryExtract(channelConfig, OpcUaChannelDescriptor::getAttributeId,
                "Error while retrieving Attribute ID");

        this.readValueId = new ReadValueId(nodeId, attributeId.uid(), null, null);
    }

    public ReadValueId getReadValueId() {
        return this.readValueId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.readValueId.getNodeId().hashCode();
        result = prime * result + this.readValueId.getAttributeId().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ReadParams other = (ReadParams) obj;

        return other.readValueId.getAttributeId() == this.readValueId.getAttributeId()
                && other.readValueId.getNodeId().equals(this.readValueId.getNodeId());
    }

}
