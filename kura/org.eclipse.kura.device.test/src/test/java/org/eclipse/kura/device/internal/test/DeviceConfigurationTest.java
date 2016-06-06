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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.eclipse.kura.device.internal.DeviceConfiguration.CHANNEL_PROPERTY_POSTFIX;
import static org.eclipse.kura.device.internal.DeviceConfiguration.CHANNEL_PROPERTY_PREFIX;

import java.util.Map;

import org.eclipse.kura.device.Channel;
import org.eclipse.kura.device.ChannelType;
import org.eclipse.kura.device.internal.DeviceConfiguration;
import org.eclipse.kura.type.DataType;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import com.google.common.collect.Maps;

/**
 * The Class DeviceConfigurationTest is responsible for testing device
 * configuration retrieval operations
 */
public final class DeviceConfigurationTest {

	/** The Device Configuration instance. */
	public DeviceConfiguration m_configuration;

	/** The properties to be parsed as Device Configuration. */
	public Map<String, Object> m_properties;

	/** The Channel Configuration */
	public Map<String, Object> m_sampleChannelConfig;

	/** Service Component Registration Callback */
	protected void activate(final ComponentContext conext, final Map<String, Object> properties) {
		this.m_properties = properties;
	}

	/**
	 * Basic information in properties
	 */
	private void basicInformation() {
		assertNotNull(this.m_configuration);
		assertNotNull(this.m_configuration.getDeviceName());
		assertNotNull(this.m_configuration.getDeviceDescription());
		assertNotNull(this.m_configuration.getDriverId());
		assertEquals("sample.driver", this.m_configuration.getDriverId());
		assertEquals("sample.device", this.m_configuration.getDeviceName());
		assertEquals("sample.device.description", this.m_configuration.getDeviceDescription());
	}

	/**
	 * Test sample channel properties.
	 */
	@Test
	public void testSampleChannelProperties1() {
		this.m_sampleChannelConfig = Maps.newHashMap();
		this.m_sampleChannelConfig.put("name", "sample.channel1");
		this.m_sampleChannelConfig.put("type", "WRITE");
		this.m_sampleChannelConfig.put("value_type", "BOOLEAN");
		this.m_sampleChannelConfig.put("channel_config", Maps.newHashMap());
		this.m_properties.put(CHANNEL_PROPERTY_PREFIX + System.currentTimeMillis() + CHANNEL_PROPERTY_POSTFIX,
				this.m_sampleChannelConfig);
		this.m_configuration = DeviceConfiguration.of(this.m_properties);
		this.basicInformation();

		assertEquals(1, this.m_configuration.getChannels().size());
		final Channel channel = this.m_configuration.getChannels().get("sample.channel1");
		assertNotNull(channel);
		assertEquals("sample.channel1", channel.getName());
		assertEquals(ChannelType.WRITE, channel.getType());
		assertEquals(DataType.BOOLEAN, channel.getValueType());
		assertNotNull(channel.getConfig());
	}

	/**
	 * Test sample channel properties.
	 */
	@Test
	public void testSampleChannelProperties2() {
		this.m_sampleChannelConfig = Maps.newHashMap();
		this.m_sampleChannelConfig.put("name", "sample.channel2");
		this.m_sampleChannelConfig.put("type", "READ");
		this.m_sampleChannelConfig.put("value_type", "INTEGER");
		this.m_sampleChannelConfig.put("channel_config", Maps.newHashMap());
		this.m_properties.put(CHANNEL_PROPERTY_PREFIX + System.currentTimeMillis() + CHANNEL_PROPERTY_POSTFIX,
				this.m_sampleChannelConfig);
		this.m_configuration = DeviceConfiguration.of(this.m_properties);
		this.basicInformation();

		assertEquals(1, this.m_configuration.getChannels().size());
		final Channel channel = this.m_configuration.getChannels().get("sample.channel2");
		assertNotNull(channel);
		assertEquals("sample.channel2", channel.getName());
		assertEquals(ChannelType.READ, channel.getType());
		assertEquals(DataType.INTEGER, channel.getValueType());
		assertNotNull(channel.getConfig());
	}

	/**
	 * Test sample channel properties.
	 */
	@Test
	public void testSampleChannelProperties3() {
		this.m_sampleChannelConfig = Maps.newHashMap();
		this.m_sampleChannelConfig.put("name", "sample.channel3");
		this.m_sampleChannelConfig.put("type", "READ");
		this.m_sampleChannelConfig.put("value_type", "LONG");
		this.m_sampleChannelConfig.put("channel_config", Maps.newHashMap());
		this.m_properties.put(CHANNEL_PROPERTY_PREFIX + System.currentTimeMillis() + CHANNEL_PROPERTY_POSTFIX,
				this.m_sampleChannelConfig);
		this.m_configuration = DeviceConfiguration.of(this.m_properties);
		this.basicInformation();

		assertEquals(1, this.m_configuration.getChannels().size());
		assertNotNull(this.m_configuration.getChannels().get("sample.channel3"));
		final Channel channel = this.m_configuration.getChannels().get("sample.channel3");
		assertNotNull(channel);
		assertEquals(ChannelType.READ, channel.getType());
		assertEquals(DataType.LONG, channel.getValueType());
		assertNotNull(channel.getConfig());
	}

	/**
	 * Test sample channel properties.
	 */
	@Test
	public void testSampleChannelProperties4() {
		this.m_sampleChannelConfig = Maps.newHashMap();
		this.m_sampleChannelConfig.put("name", "sample.channel4");
		this.m_sampleChannelConfig.put("type", "READ_WRITE");
		this.m_sampleChannelConfig.put("value_type", "BYTE");
		this.m_sampleChannelConfig.put("channel_config", Maps.newHashMap());
		this.m_properties.put(CHANNEL_PROPERTY_PREFIX + System.currentTimeMillis() + CHANNEL_PROPERTY_POSTFIX,
				this.m_sampleChannelConfig);
		this.m_configuration = DeviceConfiguration.of(this.m_properties);
		System.out.println(this.m_configuration);
		this.basicInformation();

		assertEquals(1, this.m_configuration.getChannels().size());
		final Channel channel = this.m_configuration.getChannels().get("sample.channel4");
		assertNotNull(channel);
		assertEquals("sample.channel4", channel.getName());
		assertEquals(ChannelType.READ_WRITE, channel.getType());
		assertEquals(DataType.BYTE, channel.getValueType());
		assertNotNull(channel.getConfig());
	}

}
