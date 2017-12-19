/**
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal
 */
package org.eclipse.kura.internal.driver.opcua;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * OPC-UA specific channel descriptor. The descriptor contains the following
 * attribute definition identifier.
 *
 * <ul>
 * <li>node.id</li> denotes the OPC-UA Variable Node.
 * <li>node.namespace.index</li> denotes the OPC-UA Variable Node Namespace
 * index.
 * </ul>
 */
public final class OpcUaChannelDescriptor implements ChannelDescriptor {

    private static final String NODE_ID = "node.id";
    private static final String NODE_NAMESPACE_INDEX = "node.namespace.index";
    private static final String OPCUA_TYPE = "opcua.type";
    private static final String NODE_ID_TYPE = "node.id.type";

    private static void addOptions(Tad target, Enum<?>[] values) {
        final List<Option> options = target.getOption();
        for (Enum<?> value : values) {
            Toption option = new Toption();
            option.setLabel(value.name());
            option.setValue(value.name());
            options.add(option);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Object getDescriptor() {
        final List<Tad> elements = CollectionUtil.newArrayList();

        final Tad nodeId = new Tad();
        nodeId.setName(NODE_ID);
        nodeId.setId(NODE_ID);
        nodeId.setDescription(NODE_ID);
        nodeId.setType(Tscalar.STRING);
        nodeId.setRequired(true);
        nodeId.setDefault("MyNode");
        elements.add(nodeId);

        final Tad namespaceIndex = new Tad();
        namespaceIndex.setName(NODE_NAMESPACE_INDEX);
        namespaceIndex.setId(NODE_NAMESPACE_INDEX);
        namespaceIndex.setDescription(NODE_NAMESPACE_INDEX);
        namespaceIndex.setType(Tscalar.INTEGER);
        namespaceIndex.setRequired(true);
        namespaceIndex.setDefault("2");

        elements.add(namespaceIndex);

        final Tad opcuaType = new Tad();
        opcuaType.setName(OPCUA_TYPE);
        opcuaType.setId(OPCUA_TYPE);
        opcuaType.setDescription(OPCUA_TYPE);
        opcuaType.setType(Tscalar.STRING);
        opcuaType.setRequired(true);
        opcuaType.setDefault(VariableType.DEFINED_BY_JAVA_TYPE.name());

        addOptions(opcuaType, VariableType.values());

        elements.add(opcuaType);

        final Tad nodeIdType = new Tad();
        nodeIdType.setName(NODE_ID_TYPE);
        nodeIdType.setId(NODE_ID_TYPE);
        nodeIdType.setDescription(NODE_ID_TYPE);
        nodeIdType.setType(Tscalar.STRING);
        nodeIdType.setRequired(true);
        nodeIdType.setDefault(NodeIdType.STRING.name());

        addOptions(nodeIdType, NodeIdType.values());

        elements.add(nodeIdType);
        return elements;
    }

    static int getNodeNamespaceIndex(Map<String, Object> properties) {
        return Integer.parseInt((String) properties.get(NODE_NAMESPACE_INDEX));
    }

    static NodeIdType getNodeIdType(Map<String, Object> properties) {
        String nodeIdType = (String) properties.get(NODE_ID_TYPE);
        return NodeIdType.valueOf(nodeIdType);
    }

    static VariableType getOpcuaType(Map<String, Object> properties) {
        String variableType = (String) properties.get(OPCUA_TYPE);
        return VariableType.valueOf(variableType);
    }

    static NodeId getNodeId(Map<String, Object> properties, int nodeNamespaceIndex, NodeIdType nodeIdType) {
        String nodeIdString = (String) properties.get(NODE_ID);
        switch (nodeIdType) {
        case NUMERIC:
            return new NodeId(nodeNamespaceIndex, Integer.parseInt(nodeIdString));
        case STRING:
            return new NodeId(nodeNamespaceIndex, nodeIdString);
        case GUID:
            return new NodeId(nodeNamespaceIndex, UUID.fromString(nodeIdString));
        case OPAQUE:
            return new NodeId(nodeNamespaceIndex, ByteString.of(Base64.getDecoder().decode(nodeIdString)));
        default:
            throw new IllegalArgumentException();
        }
    }
}
