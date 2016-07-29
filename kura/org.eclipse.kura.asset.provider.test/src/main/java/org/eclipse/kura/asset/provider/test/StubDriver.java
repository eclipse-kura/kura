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
import org.eclipse.kura.driver.DriverFlag;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.type.TypedValues;

public final class StubDriver implements Driver {

	private boolean isConnected;

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
			record.setDriverFlag(DriverFlag.READ_SUCCESSFUL);
			record.setValue(TypedValues.newBooleanValue(false));
		}
		return records;
	}

	/** {@inheritDoc} */
	@Override
	public void registerDriverListener(final Map<String, Object> channelConfig, final DriverListener listener)
			throws KuraException {
	}

	/** {@inheritDoc} */
	@Override
	public void unregisterDriverListener(final DriverListener listener) throws KuraException {
	}

	/** {@inheritDoc} */
	@Override
	public List<DriverRecord> write(final List<DriverRecord> records) throws KuraException {
		return null;
	}

}
