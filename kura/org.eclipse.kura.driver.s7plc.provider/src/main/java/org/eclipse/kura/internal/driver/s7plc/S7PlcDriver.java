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
package org.eclipse.kura.internal.driver.s7plc;

import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.driver.DriverConstants.CHANNEL_VALUE_TYPE;
import static org.eclipse.kura.driver.DriverFlag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
import static org.eclipse.kura.driver.DriverFlag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION;
import static org.eclipse.kura.driver.DriverFlag.READ_SUCCESSFUL;
import static org.eclipse.kura.driver.DriverFlag.WRITE_SUCCESSFUL;
import static org.eclipse.kura.type.DataType.BYTE_ARRAY;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.S7PlcMessages;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.s7connector.api.DaveArea;
import com.github.s7connector.api.S7Connector;
import com.github.s7connector.api.factory.S7ConnectorFactory;

/**
 * The Class S7PlcDriver is a S7 PLC Driver implementation for Kura Asset-Driver
 * Topology.<br/>
 * <br/>
 *
 * This S7 PLC Driver can be used in cooperation with Kura Asset Model and in
 * isolation as well. In case of isolation, the properties needs to be provided
 * externally.<br/>
 * <br/>
 *
 * The required properties are enlisted in {@link S7PlcChannelDescriptor} and
 * the driver connection specific properties are enlisted in
 * {@link S7PlcOptions}
 *
 * @see S7PlcChannelDescriptor
 * @see S7PlcOptions
 */
public final class S7PlcDriver implements Driver {

	/** DB Area Number Property */
	private static final String AREA_NO = "area.no";

	/** Number of bytes to read Property */
	private static final String BYTE_COUNT = "byte.count";

	/** DB Area Offset Property */
	private static final String OFFSET = "offset";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(S7PlcDriver.class);

	/** Localization Resource. */
	private static final S7PlcMessages s_message = LocalizationAdapter.adapt(S7PlcMessages.class);

	/** Connector instance */
	private S7Connector m_connector;

	/** flag to check if the driver is connected. */
	private boolean m_isConnected;

	/** S7 PLC Configuration Options. */
	private S7PlcOptions m_options;

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
		if (!this.m_isConnected) {
			this.m_connector = S7ConnectorFactory.buildTCPConnector().withHost(this.m_options.getIp())
					.withRack(this.m_options.getRack()).withSlot(this.m_options.getSlot()).build();
			this.m_isConnected = true;
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
		this.m_connector = null;
		s_logger.debug(s_message.deactivatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public void disconnect() throws ConnectionException {
		if (this.m_isConnected) {
			try {
				this.m_connector.close();
				this.m_isConnected = false;
			} catch (final IOException e) {
				throw new ConnectionException(s_message.disconnectionProblem() + ThrowableUtil.stackTraceAsString(e));
			}
		}
	}

	/**
	 * Extract the S7 PLC specific configurations from the provided properties.
	 *
	 * @param properties
	 *            the provided properties to parse
	 */
	private void extractProperties(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());
		this.m_options = new S7PlcOptions(properties);
	}

	/** {@inheritDoc} */
	@Override
	public ChannelDescriptor getChannelDescriptor() {
		return new S7PlcChannelDescriptor();
	}

	/** {@inheritDoc} */
	@Override
	public List<DriverRecord> read(final List<DriverRecord> records) throws ConnectionException {
		if (!this.m_isConnected) {
			this.connect();
		}
		for (final DriverRecord record : records) {
			// check if the channel type configuration is provided
			final Map<String, Object> channelConfig = record.getChannelConfig();
			DataType type;
			if (!channelConfig.containsKey(CHANNEL_VALUE_TYPE.value())) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,
						s_message.errorRetrievingValueType(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			type = (DataType) channelConfig.get(CHANNEL_VALUE_TYPE.value());
			// check if the area no configuration is provided
			if (!channelConfig.containsKey(AREA_NO)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.errorRetrievingAreaNo(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the byte count configuration is provided
			if (!channelConfig.containsKey(BYTE_COUNT)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingByteCount(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the area offset configuration is provided
			if (!channelConfig.containsKey(OFFSET)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingAreaOffset(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			if (type != BYTE_ARRAY) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,
						s_message.instanceOfByteArray(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			final byte[] value = this.m_connector.read(DaveArea.DB,
					Integer.parseInt(channelConfig.get(AREA_NO).toString()),
					Integer.parseInt(channelConfig.get(BYTE_COUNT).toString()),
					Integer.parseInt(channelConfig.get(OFFSET).toString()));
			if (value != null) {
				record.setDriverStatus(new DriverStatus(READ_SUCCESSFUL));
				record.setValue(TypedValues.newByteArrayValue(value));
				record.setTimestamp(System.currentTimeMillis());
			}
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
			// check if the channel type configuration is provided
			final Map<String, Object> channelConfig = record.getChannelConfig();
			DataType type;
			if (!channelConfig.containsKey(CHANNEL_VALUE_TYPE.value())) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,
						s_message.errorRetrievingValueType(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			type = (DataType) channelConfig.get(CHANNEL_VALUE_TYPE.value());
			// check if the area no configuration is provided
			if (!channelConfig.containsKey(AREA_NO)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.errorRetrievingAreaNo(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the byte count configuration is provided
			if (!channelConfig.containsKey(BYTE_COUNT)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingByteCount(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the area offset configuration is provided
			if (!channelConfig.containsKey(OFFSET)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingAreaOffset(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			final TypedValue<?> recordValue = record.getValue();
			if (!(recordValue instanceof ByteArrayValue) || (type != BYTE_ARRAY)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,
						s_message.instanceOfByteArray(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			final byte[] value = ((ByteArrayValue) recordValue).getValue();
			this.m_connector.write(DaveArea.DB, Integer.parseInt(channelConfig.get(AREA_NO).toString()),
					Integer.parseInt(channelConfig.get(OFFSET).toString()), value);
			record.setDriverStatus(new DriverStatus(WRITE_SUCCESSFUL));
			record.setTimestamp(System.currentTimeMillis());
		}
		return records;
	}

}
