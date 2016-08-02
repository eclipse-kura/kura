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

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Map;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.S7PlcMessages;

/**
 * The Class OpcUaOptions is responsible to provide all the required
 * configurable options for the OPC-UA Driver
 */
final class OpcUaOptions {

	/** OPC-UA Endpoint IP */
	private static final String IP = "endpoint.ip";

	/** OPC-UA Node Identifier */
	private static final String NODE_ID = "node.id";

	/** OPC-UA Endpoint Port */
	private static final String PORT = "endpoint.port";

	/** Localization Resource. */
	private static final S7PlcMessages s_message = LocalizationAdapter.adapt(S7PlcMessages.class);

	/** OPC-UA Server Name */
	private static final String SERVER_NAME = "server.name";

	/** The properties as associated */
	private final Map<String, Object> m_properties;

	/**
	 * Instantiates a new S7 PLC options.
	 *
	 * @param properties
	 *            the properties
	 */
	OpcUaOptions(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());
		this.m_properties = properties;
	}

	/**
	 * Returns the OPC-UA Endpoint IP
	 *
	 * @return the OPC-UA Endpoint IP
	 */
	String getIp() {
		String ipAddress = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(IP) && (this.m_properties.get(IP) != null)
				&& (this.m_properties.get(IP) instanceof String)) {
			ipAddress = this.m_properties.get(IP).toString();
		}
		return ipAddress;
	}

	/**
	 * Returns the OPC-UA Node Identifier
	 *
	 * @return the OPC-UA Node Identifier
	 */
	String getNodeId() {
		String nodeId = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(NODE_ID)
				&& (this.m_properties.get(NODE_ID) != null) && (this.m_properties.get(NODE_ID) instanceof String)) {
			nodeId = this.m_properties.get(NODE_ID).toString();
		}
		return nodeId;
	}

	/**
	 * Returns OPC-UA Endpoint Port
	 *
	 * @return the OPC-UA Endpoint Port
	 */
	int getPort() {
		int port = 0;
		if ((this.m_properties != null) && this.m_properties.containsKey(PORT) && (this.m_properties.get(PORT) != null)
				&& (this.m_properties.get(PORT) instanceof String)) {
			port = Integer.valueOf(this.m_properties.get(PORT).toString());
		}
		return port;
	}

	/**
	 * Returns the OPC-UA Server Name
	 *
	 * @return the OPC-UA Server Name
	 */
	String getServerName() {
		String serverName = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(SERVER_NAME)
				&& (this.m_properties.get(SERVER_NAME) != null)
				&& (this.m_properties.get(SERVER_NAME) instanceof String)) {
			serverName = this.m_properties.get(SERVER_NAME).toString();
		}
		return serverName;
	}

}
