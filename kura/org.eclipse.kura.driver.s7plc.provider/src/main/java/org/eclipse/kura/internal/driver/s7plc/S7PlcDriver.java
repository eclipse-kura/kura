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
import static org.eclipse.kura.driver.DriverFlag.READ_FAILURE;
import static org.eclipse.kura.driver.DriverFlag.READ_SUCCESSFUL;
import static org.eclipse.kura.driver.DriverFlag.WRITE_FAILURE;
import static org.eclipse.kura.driver.DriverFlag.WRITE_SUCCESSFUL;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.S7PlcMessages;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.s7connector.api.DaveArea;
import com.github.s7connector.api.S7Connector;
import com.github.s7connector.api.factory.S7ConnectorFactory;

/**
 * The Class S7PlcDriver is a S7 PLC Driver implementation for Kura Asset-Driver
 * Topology.
 *
 * @see S7PlcChannelDescriptor
 */
public final class S7PlcDriver implements Driver {

	/** DB Area Number Property */
	private static final String AREA_NO = "area.no";

	/** DB Area Offset Property */
	private static final String OFFSET = "offset";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(S7PlcDriver.class);

	/** Localization Resource. */
	private static final S7PlcMessages s_message = LocalizationAdapter.adapt(S7PlcMessages.class);

	/** Connector instance */
	private S7Connector m_connector;

	/** The Driver Service instance. */
	private volatile DriverService m_driverService;

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

	/**
	 * Binds the Driver Service.
	 *
	 * @param driverService
	 *            the Driver Service instance
	 */
	public synchronized void bindDriverService(final DriverService driverService) {
		if (this.m_driverService == null) {
			this.m_driverService = driverService;
		}
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
			final Map<String, Object> config = record.getChannelConfig();
			final TypedValue<?> recordValue = record.getValue();
			if (!(recordValue instanceof IntegerValue)) {
				record.setDriverStatus(
						this.m_driverService.newDriverStatus(READ_FAILURE, s_message.instanceOfInteger(), null));
				continue;
			}
			final int val = ((IntegerValue) recordValue).getValue();
			final byte[] value = this.m_connector.read(DaveArea.DB, Integer.valueOf(config.get(AREA_NO).toString()),
					val, Integer.valueOf(config.get(OFFSET).toString()));
			if (value != null) {
				record.setDriverStatus(this.m_driverService.newDriverStatus(READ_SUCCESSFUL));
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

	/**
	 * Unbinds the Driver Service.
	 *
	 * @param driverService
	 *            the Driver Service instance
	 */
	public synchronized void unbindDriverService(final DriverService driverService) {
		if (this.m_driverService == driverService) {
			this.m_driverService = null;
		}
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
			final TypedValue<?> recordValue = record.getValue();
			if (!(recordValue instanceof ByteArrayValue)) {
				record.setDriverStatus(
						this.m_driverService.newDriverStatus(WRITE_FAILURE, s_message.instanceOfByteArray(), null));
				continue;
			}
			final byte[] value = ((ByteArrayValue) recordValue).getValue();
			this.m_connector.write(DaveArea.DB, Integer.valueOf(config.get(AREA_NO).toString()),
					Integer.valueOf(config.get(OFFSET).toString()), value);
			record.setDriverStatus(this.m_driverService.newDriverStatus(WRITE_SUCCESSFUL));
		}
		return records;
	}

}
