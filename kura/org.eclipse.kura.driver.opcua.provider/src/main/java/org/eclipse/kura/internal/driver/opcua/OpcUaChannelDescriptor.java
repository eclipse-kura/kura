/**
 * Copyright (c) 2016, 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
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
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

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

    private static final String NODE_ID_PROP_NAME = "node.id";
    private static final String NODE_NAMESPACE_INDEX_PROP_NAME = "node.namespace.index";
    private static final String OPCUA_TYPE_PROP_NAME = "opcua.type";
    private static final String NODE_ID_TYPE_PROP_NAME = "node.id.type";
    private static final String ATTRIBUTE_PROP_NAME = "attribute";
    private static final String LISTEN_SAMPLING_INTERVAL_PROP_NAME = "listen.sampling.interval";
    private static final String LISTEN_QUEUE_SIZE_PROP_NAME = "listen.queue.size";
    private static final String LISTEN_DISCARD_OLDEST_PROP_NAME = "listen.discard.oldest";
    private static final String LISTEN_SUBSCRIBE_TO_CHILDREN_PROP_NAME = "listen.subscribe.to.children";

    private static final String NODE_ID_DEFAULT = "MyNode";
    private static final String NODE_NAMESPACE_INDEX_DEFAULT = "2";
    private static final String OPCUA_TYPE_DEFAULT = VariableType.DEFINED_BY_JAVA_TYPE.name();
    private static final String NODE_ID_TYPE_DEFAULT = NodeIdType.STRING.name();
    private static final String ATTRIBUTE_DEFAULT = AttributeId.Value.name();
    private static final String LISTEN_SAMPLING_INTERVAL_DEFAULT = "1000";
    private static final String LISTEN_QUEUE_SIZE_DEFAULT = "10";
    private static final String LISTEN_DISCARD_OLDEST_DEFAULT = "true";
    private static final String LISTEN_SUBSCRIBE_TO_CHILDREN_DEFAULT = "false";

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
        nodeId.setName(NODE_ID_PROP_NAME);
        nodeId.setId(NODE_ID_PROP_NAME);
        nodeId.setDescription(NODE_ID_PROP_NAME);
        nodeId.setType(Tscalar.STRING);
        nodeId.setRequired(true);
        nodeId.setDefault(NODE_ID_DEFAULT);
        elements.add(nodeId);

        final Tad namespaceIndex = new Tad();
        namespaceIndex.setName(NODE_NAMESPACE_INDEX_PROP_NAME);
        namespaceIndex.setId(NODE_NAMESPACE_INDEX_PROP_NAME);
        namespaceIndex.setDescription(NODE_NAMESPACE_INDEX_PROP_NAME);
        namespaceIndex.setType(Tscalar.INTEGER);
        namespaceIndex.setRequired(true);
        namespaceIndex.setDefault(NODE_NAMESPACE_INDEX_DEFAULT);

        elements.add(namespaceIndex);

        final Tad opcuaType = new Tad();
        opcuaType.setName(OPCUA_TYPE_PROP_NAME);
        opcuaType.setId(OPCUA_TYPE_PROP_NAME);
        opcuaType.setDescription(OPCUA_TYPE_PROP_NAME);
        opcuaType.setType(Tscalar.STRING);
        opcuaType.setRequired(true);
        opcuaType.setDefault(OPCUA_TYPE_DEFAULT);

        addOptions(opcuaType, VariableType.values());

        elements.add(opcuaType);

        final Tad nodeIdType = new Tad();
        nodeIdType.setName(NODE_ID_TYPE_PROP_NAME);
        nodeIdType.setId(NODE_ID_TYPE_PROP_NAME);
        nodeIdType.setDescription(NODE_ID_TYPE_PROP_NAME);
        nodeIdType.setType(Tscalar.STRING);
        nodeIdType.setRequired(true);
        nodeIdType.setDefault(NODE_ID_TYPE_DEFAULT);

        addOptions(nodeIdType, NodeIdType.values());

        elements.add(nodeIdType);

        final Tad attribute = new Tad();
        attribute.setName(ATTRIBUTE_PROP_NAME);
        attribute.setId(ATTRIBUTE_PROP_NAME);
        attribute.setDescription(ATTRIBUTE_PROP_NAME);
        attribute.setType(Tscalar.STRING);
        attribute.setRequired(true);
        attribute.setDefault(ATTRIBUTE_DEFAULT);

        addOptions(attribute, AttributeId.values());

        elements.add(attribute);

        final Tad samplingInterval = new Tad();
        samplingInterval.setName(LISTEN_SAMPLING_INTERVAL_PROP_NAME);
        samplingInterval.setId(LISTEN_SAMPLING_INTERVAL_PROP_NAME);
        samplingInterval.setDescription(LISTEN_SAMPLING_INTERVAL_PROP_NAME);
        samplingInterval.setType(Tscalar.DOUBLE);
        samplingInterval.setRequired(true);
        samplingInterval.setDefault(LISTEN_SAMPLING_INTERVAL_DEFAULT);

        elements.add(samplingInterval);

        final Tad queueSize = new Tad();
        queueSize.setName(LISTEN_QUEUE_SIZE_PROP_NAME);
        queueSize.setId(LISTEN_QUEUE_SIZE_PROP_NAME);
        queueSize.setDescription(LISTEN_QUEUE_SIZE_PROP_NAME);
        queueSize.setType(Tscalar.LONG);
        queueSize.setRequired(true);
        queueSize.setDefault(LISTEN_QUEUE_SIZE_DEFAULT);

        elements.add(queueSize);

        final Tad discardOldest = new Tad();
        discardOldest.setName(LISTEN_DISCARD_OLDEST_PROP_NAME);
        discardOldest.setId(LISTEN_DISCARD_OLDEST_PROP_NAME);
        discardOldest.setDescription(LISTEN_DISCARD_OLDEST_PROP_NAME);
        discardOldest.setType(Tscalar.BOOLEAN);
        discardOldest.setRequired(true);
        discardOldest.setDefault(LISTEN_DISCARD_OLDEST_DEFAULT);

        elements.add(discardOldest);

        final Tad subscribeToChildren = new Tad();
        subscribeToChildren.setName(LISTEN_SUBSCRIBE_TO_CHILDREN_PROP_NAME);
        subscribeToChildren.setId(LISTEN_SUBSCRIBE_TO_CHILDREN_PROP_NAME);
        subscribeToChildren.setDescription(LISTEN_SUBSCRIBE_TO_CHILDREN_PROP_NAME);
        subscribeToChildren.setType(Tscalar.BOOLEAN);
        subscribeToChildren.setRequired(true);
        subscribeToChildren.setDefault(LISTEN_SUBSCRIBE_TO_CHILDREN_DEFAULT);

        elements.add(subscribeToChildren);

        return elements;
    }

    public static int getNodeNamespaceIndex(Map<String, Object> properties) {
        return Integer.parseInt(properties.get(NODE_NAMESPACE_INDEX_PROP_NAME).toString());
    }

    public static NodeIdType getNodeIdType(Map<String, Object> properties) {
        Object nodeIdType = properties.get(NODE_ID_TYPE_PROP_NAME).toString();
        if (nodeIdType == null) {
            return NodeIdType.STRING;
        }
        return NodeIdType.valueOf((String) nodeIdType);
    }

    public static VariableType getOpcuaType(Map<String, Object> properties) {
        Object variableType = properties.get(OPCUA_TYPE_PROP_NAME).toString();
        if (variableType == null) {
            return VariableType.DEFINED_BY_JAVA_TYPE;
        }
        return VariableType.valueOf((String) variableType);
    }

    public static NodeId getNodeId(Map<String, Object> properties, int nodeNamespaceIndex, NodeIdType nodeIdType) {
        String nodeIdString = properties.get(NODE_ID_PROP_NAME).toString();
        switch (nodeIdType) {
        case NUMERIC:
            return new NodeId(nodeNamespaceIndex, UInteger.valueOf(nodeIdString));
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

    public static AttributeId getAttributeId(Map<String, Object> properties) {
        Object attributeId = properties.get(ATTRIBUTE_PROP_NAME).toString();
        if (attributeId == null) {
            return AttributeId.Value;
        }
        return AttributeId.valueOf((String) attributeId);
    }

    public static double getSamplingInterval(Map<String, Object> properties) {
        String samplingInterval = properties.get(LISTEN_SAMPLING_INTERVAL_PROP_NAME).toString();
        return Double.parseDouble(samplingInterval);
    }

    public static long getQueueSize(Map<String, Object> properties) {
        String queueSize = properties.get(LISTEN_QUEUE_SIZE_PROP_NAME).toString();
        return Long.valueOf(queueSize);
    }

    public static boolean getDiscardOldest(Map<String, Object> properties) {
        String discardOldest = properties.get(LISTEN_DISCARD_OLDEST_PROP_NAME).toString();
        return Boolean.valueOf(discardOldest);
    }

    public static boolean getSubscribeToChildren(Map<String, Object> properties) {
        String discardOldest = properties.get(LISTEN_SUBSCRIBE_TO_CHILDREN_PROP_NAME).toString();
        return Boolean.valueOf(discardOldest);
    }
}