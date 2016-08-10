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

import java.util.List;
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

public final class StubOpcUaDriverClient {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(StubOpcUaDriverClient.class);

	private volatile Driver m_driver;

	protected synchronized void activate(final ComponentContext context, final Map<String, Object> properties) {
		final List<DriverRecord> list = CollectionUtil.newArrayList();
		final Map<String, Object> channelConfig1 = CollectionUtil.newHashMap();
		channelConfig1.put("channel.id", 1);
		channelConfig1.put("channel.value.type", DataType.DOUBLE);
		channelConfig1.put("node.id", "MyLevel");
		channelConfig1.put("node.namespace.index", 2);
		final DriverRecord record1 = new DriverRecord();
		record1.setChannelConfig(channelConfig1);

		final Map<String, Object> channelConfig2 = CollectionUtil.newHashMap();
		channelConfig2.put("channel.id", 1);
		channelConfig2.put("channel.value.type", DataType.BOOLEAN);
		channelConfig2.put("node.id", "MySwitch");
		channelConfig2.put("node.namespace.index", 2);
		final DriverRecord record2 = new DriverRecord();
		record2.setChannelConfig(channelConfig2);

		list.add(record1);
		list.add(record2);
		try {
			this.m_driver.read(list);
		} catch (final ConnectionException e) {
			s_logger.info("=========>Error from OPC-UA Driver =====>" + ThrowableUtil.stackTraceAsString(e));
			return;
		}
		s_logger.info("=========>Data Read from OPC-UA Driver =====>" + list);

	}

	protected synchronized void bindDriver(final Driver driver) {
		if (this.m_driver == null) {
			this.m_driver = driver;
		}
	}

	protected synchronized void deactivate(final Map<String, Object> properties) throws ConnectionException {
		this.m_driver.disconnect();
	}

	protected synchronized void unbindDriver(final Driver driver) {
		if (this.m_driver == driver) {
			this.m_driver = null;
		}
	}

}
