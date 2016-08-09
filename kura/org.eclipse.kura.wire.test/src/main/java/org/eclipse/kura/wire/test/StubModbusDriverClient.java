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
package org.eclipse.kura.wire.test;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StubModbusDriverClient {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(StubModbusDriverClient.class);

	private volatile Driver m_driver;

	protected synchronized void activate(final ComponentContext context, final Map<String, Object> properties) {
		final Map<String, Object> channelConfig = CollectionUtil.newHashMap();
		channelConfig.put("unit.id", 10);
		channelConfig.put("memory.address", 1);
		channelConfig.put("channel.id", 1);
		channelConfig.put("channel.value.type", DataType.BOOLEAN);
		channelConfig.put("primary.table", "COILS");
		final DriverRecord record = new DriverRecord();
		record.setChannelConfig(channelConfig);
		try {
			this.m_driver.read(Arrays.asList(record));
		} catch (final ConnectionException e) {
			s_logger.info("=========>Error from Modbus Driver =====>" + ThrowableUtil.stackTraceAsString(e));
			return;
		}
		s_logger.info("=========>Data Read from Modbus Driver =====>" + record.getValue());

	}

	protected synchronized void bindDriver(final Driver driver) {
		if (this.m_driver == null) {
			this.m_driver = driver;
		}
	}

	protected synchronized void deactivate(final Map<String, Object> properties) {

	}

	protected synchronized void unbindDriver(final Driver driver) {
		if (this.m_driver == driver) {
			this.m_driver = null;
		}
	}

}
