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
package org.eclipse.kura.device.internal;

import static org.eclipse.kura.device.internal.DeviceConfiguration.CHANNEL_PROPERTY_POSTFIX;
import static org.eclipse.kura.device.internal.DeviceConfiguration.CHANNEL_PROPERTY_PREFIX;
import static org.eclipse.kura.device.internal.DeviceConfiguration.DEVICE_DESC_PROP;
import static org.eclipse.kura.device.internal.DeviceConfiguration.DEVICE_DRIVER_PROP;
import static org.eclipse.kura.device.internal.DeviceConfiguration.DEVICE_ID_PROP;

import java.util.Map;

import org.eclipse.kura.device.Channel;
import org.eclipse.kura.device.ChannelType;
import org.eclipse.kura.type.DataType;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;

import junit.framework.TestCase;

/**
 * The Class DeviceConfigurationTest is responsible for testing device
 * configuration retrieval operations
 */
public final class DeviceConfigurationTest extends TestCase {

	/** The Device Configuration instance. */
	public DeviceConfiguration m_configuration;

	/** The properties to be parsed as Device Configuration. */
	public Map<String, Object> m_properties;

	/** The Channel Configuration */
	public Map<String, Object> m_sampleChannelConfig;

	/** {@inheritDoc} */
	@BeforeClass
	@Override
	protected void setUp() {
		this.m_properties = Maps.newHashMap();
		this.m_properties.put(DEVICE_DRIVER_PROP, "sample.driver");
		this.m_properties.put(DEVICE_ID_PROP, "sample.device");
		this.m_properties.put(DEVICE_DESC_PROP, "sample.device.description");
		this.m_sampleChannelConfig = Maps.newHashMap();
		this.m_sampleChannelConfig.put("name", "sample.channel");
		this.m_sampleChannelConfig.put("type", "READ");
		this.m_sampleChannelConfig.put("value_type", "INTEGER");
		this.m_properties.put(CHANNEL_PROPERTY_PREFIX + System.currentTimeMillis() + CHANNEL_PROPERTY_POSTFIX,
				this.m_sampleChannelConfig);
		this.m_configuration = DeviceConfiguration.of(this.m_properties);
	}

	/**
	 * Test properties.
	 */
	@Test
	public void testProperties() {
		assertNotNull(this.m_configuration);
		assertNotNull(this.m_configuration.getDeviceName());
		assertNotNull(this.m_configuration.getDeviceDescription());
		assertNotNull(this.m_configuration.getDriverId());
		assertEquals("sample.driver", this.m_configuration.getDriverId());
		assertEquals("sample.device", this.m_configuration.getDeviceName());
		assertEquals("sample.device.description", this.m_configuration.getDeviceDescription());
		assertEquals(1, this.m_configuration.getChannels().size());
		assertNotNull(this.m_configuration.getChannels().get("sample.channel"));
		final Channel channel = new Channel("sample.channel", ChannelType.READ, DataType.INTEGER,
				this.m_sampleChannelConfig);
		assertEquals("sample.channel", channel.getName());
		assertEquals(ChannelType.READ, channel.getType());
		assertEquals(DataType.INTEGER, channel.getValueType());
	}

}
