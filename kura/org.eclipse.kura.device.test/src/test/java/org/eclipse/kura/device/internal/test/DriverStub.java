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
package org.eclipse.kura.device.internal.test;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.device.ChannelDescriptor;
import org.eclipse.kura.device.Devices;
import org.eclipse.kura.device.Driver;
import org.eclipse.kura.device.DriverEvent;
import org.eclipse.kura.device.DriverFlag;
import org.eclipse.kura.device.DriverListener;
import org.eclipse.kura.device.DriverRecord;
import org.eclipse.kura.type.TypedValues;

import com.google.common.collect.Lists;

/**
 * Example Driver Implementation
 */
public final class DriverStub implements Driver {

	/**
	 * List of driver listeners
	 */
	protected List<DriverListener> m_listeners;

	/** Constructor */
	public DriverStub() {
		this.m_listeners = Lists.newCopyOnWriteArrayList();
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void connect() throws KuraException {
		// Not required to be tested as Device doesn't use this directly
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void disconnect() throws KuraException {
		// Not required to be tested as Device doesn't use this directly
	}

	/** {@inheritDoc} */
	@Override
	public synchronized ChannelDescriptor getChannelDescriptor() {
		// Not required to be tested as Device doesn't use this directly
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void read(final List<DriverRecord> records) throws KuraRuntimeException, KuraException {
		for (final DriverRecord record : records) {
			record.setValue(TypedValues.newBooleanValue(true));
			record.setDriverFlag(DriverFlag.READ_SUCCESSFUL);
		}
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void registerDriverListener(final Map<String, Object> channelConfig,
			final DriverListener listener) throws KuraRuntimeException, KuraException {
		final DriverRecord record = Devices.newDriverRecord("sample.channel3");
		record.setValue(TypedValues.newIntegerValue(20));
		record.setDriverFlag(DriverFlag.READ_SUCCESSFUL);
		record.setTimestamp(System.nanoTime());
		final DriverEvent event = new DriverEvent(record);
		this.m_listeners.add(listener);
		listener.onDriverEvent(event);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void unregisterDriverListener(final DriverListener listener)
			throws KuraRuntimeException, KuraException {
		this.m_listeners.remove(listener);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void write(final List<DriverRecord> records) throws KuraRuntimeException, KuraException {
		for (final DriverRecord record : records) {
			record.setDriverFlag(DriverFlag.WRITE_SUCCESSFUL);
		}
	}

}