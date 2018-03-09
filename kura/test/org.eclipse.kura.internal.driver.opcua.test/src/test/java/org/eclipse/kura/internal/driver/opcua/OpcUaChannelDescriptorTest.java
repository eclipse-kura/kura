/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.driver.opcua;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.IdType;
import org.junit.Test;

public class OpcUaChannelDescriptorTest {

    private static final String NODE_ID = "node.id";
    private static final String NODE_ID_TYPE = "node.id.type";
    private static final String NODE_NAMESPACE_INDEX = "node.namespace.index";
    private static final String VARIABLE_TYPE = "opcua.type";

    @Test
    public void testGetDescriptor() {
        OpcUaChannelDescriptor descriptor = new OpcUaChannelDescriptor();

        List<Tad> description = (List<Tad>) descriptor.getDescriptor();

        assertNotNull(description);
        assertEquals(8, description.size());

        assertEquals(NODE_ID, description.get(0).getName());
        assertEquals(NODE_NAMESPACE_INDEX, description.get(1).getName());
        assertEquals(VARIABLE_TYPE, description.get(2).getName());
        assertEquals(16, description.get(2).getOption().size());

        assertEquals(NODE_ID_TYPE, description.get(3).getName());
        assertEquals(4, description.get(3).getOption().size());
    }

    @Test
    public void testGetNodeNamespaceIndex() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(NODE_NAMESPACE_INDEX, "123");

        int result = OpcUaChannelDescriptor.getNodeNamespaceIndex(properties);

        assertEquals(123, result);

        // test exception
        properties.put(NODE_NAMESPACE_INDEX, "123.4");

        try {
            OpcUaChannelDescriptor.getNodeNamespaceIndex(properties);
            fail("Exception was expected.");
        } catch (NumberFormatException e) {
            // OK
        }

    }

    @Test
    public void testGetNodeIdType() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(NODE_ID_TYPE, "");

        try {
            OpcUaChannelDescriptor.getNodeIdType(properties);
            fail("Exception was expected.");
        } catch (IllegalArgumentException e) {
            // OK
        }

        // acceptable types
        for (NodeIdType type : NodeIdType.values()) {
            properties.put(NODE_ID_TYPE, type.name());

            NodeIdType result = OpcUaChannelDescriptor.getNodeIdType(properties);

            assertEquals(type, result);
        }
    }

    @Test
    public void testGetNodeId() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(NODE_ID, "123");

        int nodeNamespaceIndex = 1;

        // numeric
        NodeIdType nodeIdType = NodeIdType.NUMERIC;

        NodeId node = OpcUaChannelDescriptor.getNodeId(properties, nodeNamespaceIndex, nodeIdType);

        assertEquals(IdType.Numeric, node.getType());
        assertEquals(UInteger.valueOf(123), node.getIdentifier());

        // string
        nodeIdType = NodeIdType.STRING;

        node = OpcUaChannelDescriptor.getNodeId(properties, nodeNamespaceIndex, nodeIdType);

        assertEquals(IdType.String, node.getType());
        assertEquals("123", node.getIdentifier());

        // guid
        properties.put(NODE_ID, "12345678-1234-1234-1234-123456789012");
        nodeIdType = NodeIdType.GUID;

        node = OpcUaChannelDescriptor.getNodeId(properties, nodeNamespaceIndex, nodeIdType);

        assertEquals(IdType.Guid, node.getType());
        assertEquals(UUID.fromString("12345678-1234-1234-1234-123456789012"), node.getIdentifier());

        // opaque
        properties.put(NODE_ID, Base64.getEncoder().encodeToString("12345678".getBytes()));
        nodeIdType = NodeIdType.OPAQUE;

        node = OpcUaChannelDescriptor.getNodeId(properties, nodeNamespaceIndex, nodeIdType);

        assertEquals(IdType.Opaque, node.getType());
        assertEquals(ByteString.of("12345678".getBytes()), node.getIdentifier());

        // exception case
        try {
            node = OpcUaChannelDescriptor.getNodeId(properties, nodeNamespaceIndex, null);
            fail("Exception was expected.");
        } catch (NullPointerException e) {
            // OK
        }
    }
}
