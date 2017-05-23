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

import java.util.List;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.util.collection.CollectionUtil;

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

        return elements;
    }

}
