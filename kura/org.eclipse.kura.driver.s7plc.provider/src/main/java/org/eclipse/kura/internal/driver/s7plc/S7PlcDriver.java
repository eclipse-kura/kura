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
import org.eclipse.kura.util.base.ThrowableUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.s7connector.api.S7Connector;
import com.github.s7connector.api.factory.S7ConnectorFactory;

/**
 * The Class S7PlcDriver is a S7 PLC Driver implementation for Kura Asset-Driver
 * Topology.
 *
 * @see S7PlcChannelDescriptor
 */
public final class S7PlcDriver implements Driver {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(S7PlcDriver.class);

	/** Localization Resource. */
	private static final S7PlcMessages s_message = LocalizationAdapter.adapt(S7PlcMessages.class);

	/** Connector instance */
	private S7Connector m_connector;

	/** The Driver Service instance. */
	private volatile DriverService m_driverService;

	/** flag to check if the driver is connected. */
	private boolean m_isConnected = false;

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

	/** {@inheritDoc} */
	@Override
	public void disconnect() throws ConnectionException {
		if (this.m_isConnected) {
			try {
				this.m_connector.close();
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
			// TODO
		}
		return null;
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

	@Override
	public List<DriverRecord> write(final List<DriverRecord> records) throws ConnectionException {
		return null;
	}

}
