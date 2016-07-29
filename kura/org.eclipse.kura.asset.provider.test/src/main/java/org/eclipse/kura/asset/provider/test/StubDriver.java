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
package org.eclipse.kura.asset.provider.test;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverConstants;
import org.eclipse.kura.driver.DriverFlag;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValues;

/**
 * Stub Driver implementation required for test
 */
public final class StubDriver implements Driver {

	/** flag to check if driver is connected */
	private boolean isConnected;

	/** The Driver Service instance. */
	private volatile DriverService m_driverService;

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
	public void connect() throws KuraException {
		this.isConnected = true;
	}

	/** {@inheritDoc} */
	@Override
	public void disconnect() throws KuraException {
		this.isConnected = false;
	}

	/** {@inheritDoc} */
	@Override
	public ChannelDescriptor getChannelDescriptor() {
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public List<DriverRecord> read(final List<DriverRecord> records) throws KuraException {
		if (!this.isConnected) {
			this.connect();
		}

		for (final DriverRecord record : records) {
			final Map<String, Object> driverRecordConf = record.getChannelConfig();
			switch ((DataType) driverRecordConf.get(DriverConstants.CHANNEL_VALUE_TYPE.value())) {
			case BOOLEAN:
				record.setValue(TypedValues.newBooleanValue(true));
				break;
			case SHORT:
				record.setValue(TypedValues.newShortValue((short) 1));
				break;
			case LONG:
				record.setValue(TypedValues.newLongValue(1L));
				break;
			case BYTE:
				record.setValue(TypedValues.newByteValue((byte) 1));
				break;
			case BYTE_ARRAY:
				record.setValue(TypedValues.newByteArrayValue("dummy".getBytes()));
				break;
			case DOUBLE:
				record.setValue(TypedValues.newDoubleValue(1.0));
				break;
			case STRING:
				record.setValue(TypedValues.newStringValue("dummy"));
				break;
			case INTEGER:
				record.setValue(TypedValues.newIntegerValue(1));
				break;
			}
			record.setDriverFlag(DriverFlag.READ_SUCCESSFUL);
		}
		return records;
	}

	/** {@inheritDoc} */
	@Override
	public void registerDriverListener(final Map<String, Object> channelConfig, final DriverListener listener)
			throws KuraException {
		final DriverRecord record = this.m_driverService.newDriverRecord();
		record.setChannelConfig(channelConfig);
		record.setValue(TypedValues.newIntegerValue(1));
		record.setDriverFlag(DriverFlag.READ_SUCCESSFUL);
		record.setTimestamp(System.currentTimeMillis());
		listener.onDriverEvent(this.m_driverService.newDriverEvent(record));
	}

	/**
	 * Unbinds the Driver Service.
	 *
	 * @param driverService
	 *            the Driver Service
	 */
	public synchronized void unbindDriverService(final DriverService driverService) {
		if (this.m_driverService == driverService) {
			this.m_driverService = null;
		}
	}

	/** {@inheritDoc} */
	@Override
	public void unregisterDriverListener(final DriverListener listener) throws KuraException {
	}

	/** {@inheritDoc} */
	@Override
	public List<DriverRecord> write(final List<DriverRecord> records) throws KuraException {
		if (!this.isConnected) {
			this.connect();
		}

		for (final DriverRecord record : records) {
			record.setDriverFlag(DriverFlag.WRITE_SUCCESSFUL);
		}
		return records;
	}

}
