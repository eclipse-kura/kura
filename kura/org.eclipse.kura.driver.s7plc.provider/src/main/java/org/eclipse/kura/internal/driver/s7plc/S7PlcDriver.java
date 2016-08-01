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

import java.util.List;
import java.util.Map;

import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.listener.DriverListener;

public final class S7PlcDriver implements Driver {

	@Override
	public void connect() throws ConnectionException {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnect() throws ConnectionException {
		// TODO Auto-generated method stub

	}

	@Override
	public ChannelDescriptor getChannelDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DriverRecord> read(final List<DriverRecord> records) throws ConnectionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerDriverListener(final Map<String, Object> channelConfig, final DriverListener listener)
			throws ConnectionException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unregisterDriverListener(final DriverListener listener) throws ConnectionException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<DriverRecord> write(final List<DriverRecord> records) throws ConnectionException {
		// TODO Auto-generated method stub
		return null;
	}

}
