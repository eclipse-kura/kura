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
import org.eclipse.kura.asset.ChannelDescriptor;
import org.eclipse.kura.asset.Driver;
import org.eclipse.kura.asset.DriverListener;
import org.eclipse.kura.asset.DriverRecord;

public final class StubDriver implements Driver {

	@Override
	public void connect() throws KuraException {
	}

	@Override
	public void disconnect() throws KuraException {
	}

	@Override
	public ChannelDescriptor getChannelDescriptor() {
		return null;
	}

	@Override
	public void read(final List<DriverRecord> records) throws KuraException {
	}

	@Override
	public void registerDriverListener(final Map<String, Object> channelConfig, final DriverListener listener)
			throws KuraException {
	}

	@Override
	public void unregisterDriverListener(final DriverListener listener) throws KuraException {
	}

	@Override
	public void write(final List<DriverRecord> records) throws KuraException {
	}

}
