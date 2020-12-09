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

import org.eclipse.kura.internal.driver.opcua.DataTypeMapper;
import org.eclipse.kura.internal.driver.opcua.NodeIdType;
import org.eclipse.kura.internal.driver.opcua.OpcUaChannelDescriptor;
import org.eclipse.kura.internal.driver.opcua.Utils;
import org.eclipse.kura.internal.driver.opcua.VariableType;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;

public class WriteParams {

    private final WriteValue writeValue;

    public WriteParams(final Map<String, Object> channelConfig, final Object value) {
        final int nodeNamespaceIndex = Utils.tryExtract(channelConfig, OpcUaChannelDescriptor::getNodeNamespaceIndex,
                "Error while retrieving Node Namespace index");
        final NodeIdType nodeIdType = tryExtract(channelConfig, OpcUaChannelDescriptor::getNodeIdType,
                "Error while retrieving Node ID type");

        final NodeId nodeId = tryExtract(channelConfig,
                config -> OpcUaChannelDescriptor.getNodeId(config, nodeNamespaceIndex, nodeIdType),
                "Error while retrieving Node ID");

        final AttributeId attributeId = tryExtract(channelConfig, OpcUaChannelDescriptor::getAttributeId,
                "Error while retrieving Attribute ID");

        final VariableType opcuaType = tryExtract(channelConfig, OpcUaChannelDescriptor::getOpcuaType,
                "Error while retrieving OPC UA variable type");

        this.writeValue = new WriteValue(nodeId, attributeId.uid(), null,
                new DataValue(DataTypeMapper.map(value, opcuaType), StatusCode.GOOD, null));
    }

    public WriteValue getWriteValue() {
        return this.writeValue;
    }
}
