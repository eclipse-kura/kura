/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.eclipse.kura.internal.driver.opcua.request;

import static org.eclipse.kura.internal.driver.opcua.Utils.tryExtract;

import java.util.Map;

import org.eclipse.kura.driver.opcua.localization.OpcUaMessages;
import org.eclipse.kura.internal.driver.opcua.NodeIdType;
import org.eclipse.kura.internal.driver.opcua.OpcUaChannelDescriptor;
import org.eclipse.kura.internal.driver.opcua.Utils;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

public class ReadParams {

    private static final OpcUaMessages message = LocalizationAdapter.adapt(OpcUaMessages.class);

    private final ReadValueId readValueId;

    public ReadParams(final Map<String, Object> channelConfig) {
        final int nodeNamespaceIndex = Utils.tryExtract(channelConfig, OpcUaChannelDescriptor::getNodeNamespaceIndex,
                message.errorRetrievingNodeNamespace());
        final NodeIdType nodeIdType = tryExtract(channelConfig, OpcUaChannelDescriptor::getNodeIdType,
                message.errorRetrievingNodeIdType());

        final NodeId nodeId = tryExtract(channelConfig,
                config -> OpcUaChannelDescriptor.getNodeId(config, nodeNamespaceIndex, nodeIdType),
                message.errorRetrievingNodeId());

        final AttributeId attributeId = tryExtract(channelConfig, OpcUaChannelDescriptor::getAttributeId,
                message.errorRetrievingAttributeId());

        this.readValueId = new ReadValueId(nodeId, attributeId.uid(), null, null);
    }

    public ReadValueId getReadValueId() {
        return readValueId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + readValueId.getNodeId().hashCode();
        result = prime * result + readValueId.getAttributeId().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ReadParams other = (ReadParams) obj;

        return other.readValueId.getAttributeId() == readValueId.getAttributeId()
                && other.readValueId.getNodeId().equals(readValueId.getNodeId());
    }

}
