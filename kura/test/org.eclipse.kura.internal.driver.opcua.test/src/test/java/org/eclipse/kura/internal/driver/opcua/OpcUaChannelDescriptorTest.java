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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.junit.Test;


public class OpcUaChannelDescriptorTest {

    private static final String NODE_ID_TYPE = "node.id.type";
    private static final String NODE_NAMESPACE_INDEX = "node.namespace.index";

    @Test
    public void testGetDescriptor() {
        OpcUaChannelDescriptor descriptor = new OpcUaChannelDescriptor();

        List<Tad> description = (List<Tad>) descriptor.getDescriptor();

        assertNotNull(description);
        assertEquals(3, description.size());

        assertEquals("node.id", description.get(0).getName());
        assertEquals(NODE_NAMESPACE_INDEX, description.get(1).getName());
        assertEquals(NODE_ID_TYPE, description.get(2).getName());
        assertEquals(4, description.get(2).getOption().size());
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

        properties.put(NODE_ID_TYPE, "NUMERIC");

        NodeIdType result = OpcUaChannelDescriptor.getNodeIdType(properties);

        assertEquals(NodeIdType.NUMERIC, result);
    }
}
