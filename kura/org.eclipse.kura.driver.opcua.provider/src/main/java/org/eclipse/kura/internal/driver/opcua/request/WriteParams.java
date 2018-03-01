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
import org.eclipse.kura.internal.driver.opcua.DataTypeMapper;
import org.eclipse.kura.internal.driver.opcua.NodeIdType;
import org.eclipse.kura.internal.driver.opcua.OpcUaChannelDescriptor;
import org.eclipse.kura.internal.driver.opcua.Utils;
import org.eclipse.kura.internal.driver.opcua.VariableType;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;

public class WriteParams {

    private static final OpcUaMessages message = LocalizationAdapter.adapt(OpcUaMessages.class);

    private final WriteValue writeValue;

    public WriteParams(final Map<String, Object> channelConfig, final Object value) {
        final int nodeNamespaceIndex = Utils.tryExtract(channelConfig, OpcUaChannelDescriptor::getNodeNamespaceIndex,
                message.errorRetrievingNodeNamespace());
        final NodeIdType nodeIdType = tryExtract(channelConfig, OpcUaChannelDescriptor::getNodeIdType,
                message.errorRetrievingNodeIdType());

        final NodeId nodeId = tryExtract(channelConfig,
                config -> OpcUaChannelDescriptor.getNodeId(config, nodeNamespaceIndex, nodeIdType),
                message.errorRetrievingNodeId());

        final AttributeId attributeId = tryExtract(channelConfig, OpcUaChannelDescriptor::getAttributeId,
                message.errorRetrievingAttributeId());

        final VariableType opcuaType = tryExtract(channelConfig, OpcUaChannelDescriptor::getOpcuaType,
                message.errorRetrievingOpcuaType());

        this.writeValue = new WriteValue(nodeId, attributeId.uid(), null,
                new DataValue(DataTypeMapper.map(value, opcuaType), StatusCode.GOOD, null));
    }

    public WriteValue getWriteValue() {
        return writeValue;
    }
}
