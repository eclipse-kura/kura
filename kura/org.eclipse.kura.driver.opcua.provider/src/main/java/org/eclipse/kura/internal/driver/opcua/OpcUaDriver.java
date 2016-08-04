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
import static org.eclipse.kura.driver.DriverFlag.READ_FAILURE;
import static org.eclipse.kura.driver.DriverFlag.READ_SUCCESSFUL;
import static org.eclipse.kura.driver.DriverFlag.WRITE_FAILURE;
import static org.eclipse.kura.driver.DriverFlag.WRITE_SUCCESSFUL;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.OpcUaMessages;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.nodes.attached.UaVariableNode;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class OpcUaDriver is a OPC-UA Driver implementation for Kura Asset-Driver
 * Topology. Currently it only supports reading and writing from/to a specific
 * node. As of now, it doesn't support method execution or history read.<br/>
 * <br/>
 *
 * This OPC-UA Driver can be used in cooperation with Kura Asset Model and in
 * isolation as well. In case of isolation, the properties needs to be provided
 * externally.<br/>
 * <br/>
 *
 * The required properties are enlisted in {@link OpcUaChannelDescriptor} and
 * the driver connection specific properties are enlisted in
 * {@link OpcUaOptions}
 *
 * @see OpcUaChannelDescriptor
 * @see OpcUaOptions
 */
public final class OpcUaDriver implements Driver {

	/** Node Identifier Property */
	private static final String NODE_ID = "node.id";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(OpcUaDriver.class);

	/** Localization Resource. */
	private static final OpcUaMessages s_message = LocalizationAdapter.adapt(OpcUaMessages.class);

	/** OPC-UA Client Connector */
	private OpcUaClient m_client;

	/** flag to check if the driver is connected. */
	private boolean m_isConnected;

	/** OPC-UA Configuration Options. */
	private OpcUaOptions m_options;

	/**
	 * OSGi service component callback while activation.
	 *
	 * @param componentContext
	 *            the component context
	 * @param properties
	 *            the service properties
	 */
	protected synchronized void activate(final ComponentContext componentContext,
			final Map<String, Object> properties) {
		s_logger.debug(s_message.activating());
		this.extractProperties(properties);
		s_logger.debug(s_message.activatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public void connect() throws ConnectionException {
		EndpointDescription[] endpoints;
		try {
			endpoints = UaTcpStackClient.getEndpoints(new StringBuilder().append("opc.tcp://")
					.append(this.m_options.getIp()).append(":").append(this.m_options.getPort()).append("/")
					.append(this.m_options.getServerName()).toString()).get();
			final Optional<EndpointDescription> endpoint = Arrays.stream(endpoints).findFirst();
			if (!endpoint.isPresent()) {
				throw new ConnectionException(s_message.connectionProblem());
			}
			final OpcUaClientConfig clientConfig = OpcUaClientConfig.builder().setEndpoint(endpoint.get()).build();
			this.m_client = new OpcUaClient(clientConfig);
			this.m_isConnected = true;
		} catch (final Exception e) {
			s_logger.error(ThrowableUtil.stackTraceAsString(e));
		}

	}

	/**
	 * OSGi service component callback while deactivation.
	 *
	 * @param componentContext
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.debug(s_message.deactivating());
		try {
			this.disconnect();
		} catch (final ConnectionException e) {
			s_logger.error(s_message.errorDisconnecting() + ThrowableUtil.stackTraceAsString(e));
		}
		this.m_client = null;
		s_logger.debug(s_message.deactivatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public void disconnect() throws ConnectionException {
		if (this.m_isConnected) {
			this.m_client.disconnect();
			this.m_isConnected = false;
		}
	}

	/**
	 * Extract the OPC-UA specific configurations from the provided properties.
	 *
	 * @param properties
	 *            the provided properties to parse
	 */
	private void extractProperties(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());
		this.m_options = new OpcUaOptions(properties);
	}

	/** {@inheritDoc} */
	@Override
	public ChannelDescriptor getChannelDescriptor() {
		return new OpcUaChannelDescriptor();
	}

	/**
	 * Returns the wrapped Typed Value instance based on the provided value
	 *
	 * @param value
	 *            the provided value to wrap
	 * @return the TypedValue instance
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	private TypedValue<?> getTypedValue(final Object value) {
		checkNull(value, s_message.valueNonNull());
		if (value instanceof Long) {
			return TypedValues.newLongValue(Long.valueOf(value.toString()));
		}
		if (value instanceof Short) {
			return TypedValues.newShortValue(Short.valueOf(value.toString()));
		}
		if (value instanceof Double) {
			return TypedValues.newDoubleValue(Double.valueOf(value.toString()));
		}
		if (value instanceof Integer) {
			return TypedValues.newIntegerValue(Integer.valueOf(value.toString()));
		}
		if (value instanceof Byte) {
			return TypedValues.newByteValue(Byte.valueOf(value.toString()));
		}
		if (value instanceof Boolean) {
			return TypedValues.newBooleanValue(Boolean.valueOf(value.toString()));
		}
		if (value instanceof String) {
			return TypedValues.newStringValue(value.toString());
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public List<DriverRecord> read(final List<DriverRecord> records) throws ConnectionException {
		if (!this.m_isConnected) {
			this.connect();
		}
		for (final DriverRecord record : records) {
			final Map<String, Object> config = record.getChannelConfig();
			final NodeId nodeId = new NodeId(2, config.get(NODE_ID).toString());
			final UaVariableNode node = this.m_client.getAddressSpace().getVariableNode(nodeId);
			Object value = null;
			try {
				value = node.readValueAttribute().get();
			} catch (final Exception e) {
				record.setDriverStatus(new DriverStatus(READ_FAILURE, s_message.readFailed(), e));
			}
			record.setValue(this.getTypedValue(value));
			record.setDriverStatus(new DriverStatus(READ_SUCCESSFUL));
		}
		return records;
	}

	/** {@inheritDoc} */
	@Override
	public void registerDriverListener(final Map<String, Object> channelConfig, final DriverListener listener)
			throws ConnectionException {
		throw new KuraRuntimeException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
	}

	/** {@inheritDoc} */
	@Override
	public void unregisterDriverListener(final DriverListener listener) throws ConnectionException {
		throw new KuraRuntimeException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
	}

	/**
	 * OSGi service component callback while updating.
	 *
	 * @param properties
	 *            the properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.debug(s_message.updating());
		this.extractProperties(properties);
		s_logger.debug(s_message.updatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public List<DriverRecord> write(final List<DriverRecord> records) throws ConnectionException {
		if (!this.m_isConnected) {
			this.connect();
		}
		for (final DriverRecord record : records) {
			final Map<String, Object> config = record.getChannelConfig();
			final TypedValue<?> value = record.getValue();
			final NodeId nodeId = new NodeId(2, config.get(NODE_ID).toString());
			final UaVariableNode node = this.m_client.getAddressSpace().getVariableNode(nodeId);
			final DataValue newValue = new DataValue(new Variant(value.getValue()));
			try {
				node.writeValue(newValue).get();
			} catch (final Exception e) {
				record.setDriverStatus(new DriverStatus(WRITE_FAILURE, s_message.writeFailed(), e));
			}
			record.setDriverStatus(new DriverStatus(WRITE_SUCCESSFUL));
		}
		return records;
	}

}
