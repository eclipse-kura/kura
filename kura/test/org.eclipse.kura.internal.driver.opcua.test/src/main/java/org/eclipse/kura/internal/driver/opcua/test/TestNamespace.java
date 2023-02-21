/*******************************************************************************
 * Copyright (c) 2016, 2023 Kevin Herron and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Kevin Herron
 *  Eurotech
 ******************************************************************************/

package org.eclipse.kura.internal.driver.opcua.test;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ulong;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRank;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespace;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.api.methods.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.XmlElement;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class TestNamespace extends ManagedNamespace {

    public static final String NAMESPACE_URI = "urn:eclipse:milo:hello-world";
    public static final String IDENTIFIER_HELLO_WORLD = "HelloWorld";

    private static final Object[][] STATIC_SCALAR_NODES = new Object[][] {
            { "Boolean", Identifiers.Boolean, new Variant(false) },
            { "Byte", Identifiers.Byte, new Variant(ubyte(0x00)) },
            { "SByte", Identifiers.SByte, new Variant((byte) 0x00) }, { "Int16", Identifiers.Int16, new Variant(0) },
            { "Int32", Identifiers.Int32, new Variant(0) }, { "Int64", Identifiers.Int64, new Variant(0) },
            { "UInt16", Identifiers.UInt16, new Variant(ushort(0)) },
            { "UInt32", Identifiers.UInt32, new Variant(uint(0)) },
            { "UInt64", Identifiers.UInt64, new Variant(ulong(0)) }, { "Float", Identifiers.Float, new Variant(0.0f) },
            { "Double", Identifiers.Double, new Variant(0.0d) },
            { "String", Identifiers.String, new Variant("string value") },
            { "DateTime", Identifiers.DateTime, new Variant(DateTime.now()) },
            { "Guid", Identifiers.Guid, new Variant(UUID.randomUUID()) },
            { "ByteString", Identifiers.ByteString,
                    new Variant(new ByteString(new byte[] { 0x01, 0x02, 0x03, 0x04 })) },
            { "XmlElement", Identifiers.XmlElement, new Variant(new XmlElement("<a>hello</a>")) },
            { "LocalizedText", Identifiers.LocalizedText, new Variant(LocalizedText.english("localized text")) },
            { "QualifiedName", Identifiers.QualifiedName, new Variant(new QualifiedName(1234, "defg")) },
            { "NodeId", Identifiers.NodeId, new Variant(new NodeId(1234, "abcd")) },
            { "Duration", Identifiers.Duration, new Variant(1.0) },
            { "UtcTime", Identifiers.UtcTime, new Variant(DateTime.now()) }, };

    private static final Object[][] STATIC_ARRAY_NODES = new Object[][] {
            { "BooleanArray", Identifiers.Boolean, false }, { "ByteArray", Identifiers.Byte, ubyte(0) },
            { "SByteArray", Identifiers.SByte, (byte) 0x00 }, { "Int16Array", Identifiers.Int16, (short) 16 },
            { "Int32Array", Identifiers.Int32, 32 }, { "Int64Array", Identifiers.Int64, 64L },
            { "UInt16Array", Identifiers.UInt16, ushort(16) }, { "UInt32Array", Identifiers.UInt32, uint(32) },
            { "UInt64Array", Identifiers.UInt64, ulong(64L) }, { "FloatArray", Identifiers.Float, 3.14f },
            { "DoubleArray", Identifiers.Double, 3.14d }, { "StringArray", Identifiers.String, "string value" },
            { "DateTimeArray", Identifiers.DateTime, new Variant(DateTime.now()) },
            { "GuidArray", Identifiers.Guid, new Variant(UUID.randomUUID()) },
            { "ByteStringArray", Identifiers.ByteString,
                    new Variant(new ByteString(new byte[] { 0x01, 0x02, 0x03, 0x04 })) },
            { "XmlElementArray", Identifiers.XmlElement, new Variant(new XmlElement("<a>hello</a>")) },
            { "LocalizedTextArray", Identifiers.LocalizedText, new Variant(LocalizedText.english("localized text")) },
            { "QualifiedNameArray", Identifiers.QualifiedName, new Variant(new QualifiedName(1234, "defg")) },
            { "NodeIdArray", Identifiers.NodeId, new Variant(new NodeId(1234, "abcd")) } };

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SubscriptionModel subscriptionModel;

    private final OpcUaServer server;
    private final UShort namespaceIndex;

    public TestNamespace(OpcUaServer server) {
        super(server, NAMESPACE_URI);
        this.server = server;
        this.namespaceIndex = server.getNamespaceTable().addUri(NAMESPACE_URI);

        subscriptionModel = new SubscriptionModel(server, this);

        NodeId folderNodeId = new NodeId(namespaceIndex, IDENTIFIER_HELLO_WORLD);

        UaFolderNode folderNode = new UaFolderNode(getNodeContext(), folderNodeId,
                new QualifiedName(namespaceIndex, IDENTIFIER_HELLO_WORLD),
                LocalizedText.english(IDENTIFIER_HELLO_WORLD));

        getNodeManager().addNode(folderNode);

        folderNode.addReference(new Reference(
                Identifiers.ObjectsFolder,
                Identifiers.Organizes,
                folderNodeId.expanded(),
                true));

        addVariableNodes(folderNode);
    }

    private void addVariableNodes(UaFolderNode rootNode) {
        addArrayNodes(rootNode);
        addScalarNodes(rootNode);
    }

    private void addArrayNodes(UaFolderNode rootNode) {
        UaFolderNode arrayTypesFolder = new UaFolderNode(getNodeContext(),
                new NodeId(namespaceIndex, "HelloWorld/ArrayTypes"), new QualifiedName(namespaceIndex, "ArrayTypes"),
                LocalizedText.english("ArrayTypes"));

        getNodeManager().addNode(arrayTypesFolder);
        rootNode.addOrganizes(arrayTypesFolder);

        for (Object[] os : STATIC_ARRAY_NODES) {
            String name = (String) os[0];
            NodeId typeId = (NodeId) os[1];
            Object value = os[2];
            Object array = Array.newInstance(value.getClass(), 4);
            for (int i = 0; i < 4; i++) {
                Array.set(array, i, value);
            }
            Variant variant = new Variant(array);

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                    .setNodeId(new NodeId(namespaceIndex, name))
                    .setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE))
                    .setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE))
                    .setBrowseName(new QualifiedName(namespaceIndex, name)).setDisplayName(LocalizedText.english(name))
                    .setDataType(typeId).setTypeDefinition(Identifiers.BaseDataVariableType)
                    .setValueRank(ValueRank.OneDimension.getValue()).setArrayDimensions(new UInteger[] { uint(0) })
                    .build();

            node.setValue(new DataValue(variant));

            getNodeManager().addNode(node);
            arrayTypesFolder.addOrganizes(node);
        }
    }

    private void addScalarNodes(UaFolderNode rootNode) {
        UaFolderNode scalarTypesFolder = new UaFolderNode(getNodeContext(),
                new NodeId(namespaceIndex, "HelloWorld/ScalarTypes"), new QualifiedName(namespaceIndex, "ScalarTypes"),
                LocalizedText.english("ScalarTypes"));

        getNodeManager().addNode(scalarTypesFolder);
        rootNode.addOrganizes(scalarTypesFolder);

        for (Object[] os : STATIC_SCALAR_NODES) {
            String name = (String) os[0];
            NodeId typeId = (NodeId) os[1];
            Variant variant = (Variant) os[2];

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                    .setNodeId(new NodeId(namespaceIndex, name))
                    .setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE))
                    .setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE))
                    .setBrowseName(new QualifiedName(namespaceIndex, name)).setDisplayName(LocalizedText.english(name))
                    .setDataType(typeId).setTypeDefinition(Identifiers.BaseDataVariableType).build();

            node.setValue(new DataValue(variant));

            getNodeManager().addNode(node);
            scalarTypesFolder.addOrganizes(node);
        }

        UaVariableNode largeIndex = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(new NodeId(namespaceIndex, UInteger.MAX))
                .setAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE))
                .setUserAccessLevel(AccessLevel.toValue(AccessLevel.READ_WRITE))
                .setBrowseName(new QualifiedName(namespaceIndex, "largeIndex"))
                .setDisplayName(LocalizedText.english("largeIndex")).setDataType(Identifiers.Int32)
                .setTypeDefinition(Identifiers.BaseDataVariableType).build();

        largeIndex.setValue(new DataValue(new Variant(1234)));

        getNodeManager().addNode(largeIndex);
    }

    @Override
    public void browse(BrowseContext context, NodeId nodeId) {
        UaNode node = getNodeManager().get(nodeId);

        if (node != null) {
            context.success(node.getReferences());
        } else {
            context.failure(new UaException(StatusCodes.Bad_NodeIdUnknown));
        }
    }

    @Override
    public void read(ReadContext context, Double maxAge, TimestampsToReturn timestamps,
            List<ReadValueId> readValueIds) {

        List<DataValue> results = Lists.newArrayListWithCapacity(readValueIds.size());

        for (ReadValueId readValueId : readValueIds) {
            UaNode node = getNodeManager().get(readValueId.getNodeId());

            if (node != null) {
                DataValue value = node.readAttribute(new AttributeContext(context), readValueId.getAttributeId(),
                        timestamps, readValueId.getIndexRange(), readValueId.getDataEncoding());

                results.add(value);
            } else {
                results.add(new DataValue(StatusCodes.Bad_NodeIdUnknown));
            }
        }

        context.success(results);
    }

    @Override
    public void write(WriteContext context, List<WriteValue> writeValues) {
        List<StatusCode> results = Lists.newArrayListWithCapacity(writeValues.size());

        for (WriteValue writeValue : writeValues) {
            UaNode node = getNodeManager().get(writeValue.getNodeId());

            if (node != null) {
                try {
                    node.writeAttribute(new AttributeContext(context), writeValue.getAttributeId(),
                            writeValue.getValue(), writeValue.getIndexRange());

                    results.add(StatusCode.GOOD);

                    logger.info("Wrote value {} to {} attribute of {}", writeValue.getValue().getValue(),
                            AttributeId.from(writeValue.getAttributeId()).map(Object::toString).orElse("unknown"),
                            node.getNodeId());
                } catch (UaException e) {
                    logger.error("Unable to write value={}", writeValue.getValue(), e);
                    results.add(e.getStatusCode());
                }
            } else {
                results.add(new StatusCode(StatusCodes.Bad_NodeIdUnknown));
            }
        }

        context.success(results);
    }

    @Override
    protected Optional<MethodInvocationHandler> getInvocationHandler(NodeId objectId, NodeId methodId) {
        Optional<UaNode> node = getNodeManager().getNode(methodId);

        return node.flatMap(n -> {
            if (n instanceof UaMethodNode) {
                return Optional.of(((UaMethodNode) n).getInvocationHandler());
            } else {
                return Optional.empty();
            }
        });
    }

    @Override
    public void onDataItemsCreated(List<DataItem> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDataItemsDeleted(List<DataItem> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDataItemsModified(List<DataItem> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> arg0) {
        // TODO Auto-generated method stub

    }

}
