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
package org.eclipse.kura.internal.driver.modbus;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.ModbusDriverMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModbusDriver implements Driver {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(ModbusDriver.class);

	/** Localization Resource. */
	private static final ModbusDriverMessages s_message = LocalizationAdapter.adapt(ModbusDriverMessages.class);

	/** {@inheritDoc} */
	@Override
	public void connect() throws ConnectionException {

	}

	/** {@inheritDoc} */
	@Override
	public void disconnect() throws ConnectionException {

	}

	/** {@inheritDoc} */
	@Override
	public ChannelDescriptor getChannelDescriptor() {
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public List<DriverRecord> read(final List<DriverRecord> records) throws ConnectionException {
		return null;
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

	/** {@inheritDoc} */
	@Override
	public List<DriverRecord> write(final List<DriverRecord> records) throws ConnectionException {
		return null;
	}

}
