/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.driver.opcua;

import java.util.List;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.OpcUaMessages;
import org.eclipse.kura.util.collection.CollectionUtil;

/**
 * OPC-UA specific channel descriptor. The descriptor contains the following
 * attribute definition identifier.
 *
 * <ul>
 * <li>node.id</li> denotes the OPC-UA Variable Node.
 * <li>node.namespace.index</li> denotes the OPC-UA Node Namespace index.
 *
 * </ul>
 */
public final class OpcUaChannelDescriptor implements ChannelDescriptor {

	/** Localization Resource. */
	private static final OpcUaMessages s_message = LocalizationAdapter.adapt(OpcUaMessages.class);

	/** The descriptor elements. */
	private List<Tad> m_elements;

	/** {@inheritDoc} */
	@Override
	public Object getDescriptor() {
		this.m_elements = CollectionUtil.newArrayList();

		final Tad nodeId = new Tad();
		nodeId.setName(s_message.nodeId());
		nodeId.setId("node.id");
		nodeId.setDescription(s_message.nodeId());
		nodeId.setType(Tscalar.STRING);
		nodeId.setRequired(true);
		nodeId.setDefault("/opc/ua/node/example");

		final Tad namespaceIndex = new Tad();
		namespaceIndex.setName(s_message.nodeNamespaceIndex());
		namespaceIndex.setId("node.namespace.index");
		namespaceIndex.setDescription(s_message.nodeNamespaceIndex());
		namespaceIndex.setType(Tscalar.INTEGER);
		namespaceIndex.setRequired(true);
		namespaceIndex.setDefault("2");

		this.m_elements.add(namespaceIndex);
		return this.m_elements;
	}

}
