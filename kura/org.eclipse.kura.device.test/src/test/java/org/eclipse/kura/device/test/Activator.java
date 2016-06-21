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
package org.eclipse.kura.device.test;

import static org.eclipse.kura.device.internal.DeviceConfiguration.CHANNEL_PROPERTY_POSTFIX;
import static org.eclipse.kura.device.internal.DeviceConfiguration.CHANNEL_PROPERTY_PREFIX;
import static org.eclipse.kura.device.internal.DeviceConfiguration.DEVICE_DESC_PROP;
import static org.eclipse.kura.device.internal.DeviceConfiguration.DEVICE_DRIVER_PROP;
import static org.eclipse.kura.device.internal.DeviceConfiguration.DEVICE_ID_PROP;

import java.sql.Driver;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.eclipse.kura.device.Device;
import org.eclipse.kura.device.internal.BaseDevice;
import org.eclipse.kura.device.internal.test.DeviceConfigurationTest;
import org.eclipse.kura.device.internal.test.DeviceTest;
import org.eclipse.kura.device.internal.test.DriverStub;
import org.osgi.framework.BundleContext;

import com.google.common.collect.Maps;

/**
 * The bundle activator which registers tests as services.
 */
final class Activator extends DependencyActivatorBase {

	/** {@inheritDoc} */
	@Override
	public void destroy(final BundleContext context, final DependencyManager manager) throws Exception {
		// Not used
	}

	/** {@inheritDoc} */
	@Override
	public void init(final BundleContext context, final DependencyManager manager) throws Exception {
		// Registering Driver
		manager.add(this.createComponent().setInterface(Driver.class.getName(), this.newDriverProperties())
				.setImplementation(DriverStub.class));
		// Registering basic Device Stub as a Device component
		manager.add(this.createComponent().setInterface(Device.class.getName(), this.newDeviceProperties())
				.setImplementation(BaseDevice.class));
		// Creating service component for Base Device Test
		manager.add(this.createComponent()
				.setInterface(Object.class.getName(), this.newTestProperties("BasicDeviceStubTest"))
				.setImplementation(DeviceTest.class).add(this.createServiceDependency().setService(Device.class)
						.setRequired(true).setCallbacks("bindDevice", "unbindDevice")));
		// Creating service component for Device Configuration Test
		manager.add(this.createComponent()
				.setInterface(Object.class.getName(), this.newTestProperties("DeviceConfigurationTest"))
				.setImplementation(DeviceConfigurationTest.class));
	}

	/**
	 * Prepares and returns the properties of the service component for device.
	 *
	 * @return the dictionary
	 */
	private Dictionary<String, Object> newDeviceProperties() {
		final Dictionary<String, Object> properties = new Hashtable<String, Object>();

		// General Information
		properties.put(DEVICE_DRIVER_PROP, "sample.driver");
		properties.put(DEVICE_ID_PROP, "sample.device");
		properties.put(DEVICE_DESC_PROP, "sample.device.description");

		// The Reading Channel Configuration Information
		final Map<String, Object> m_sampleChannelConfig1 = Maps.newHashMap();
		m_sampleChannelConfig1.put("name", "sample.channel1");
		m_sampleChannelConfig1.put("type", "READ");
		m_sampleChannelConfig1.put("value_type", "BOOLEAN");
		m_sampleChannelConfig1.put("channel_config", Maps.newHashMap());

		// The Writing Channel Information
		final Map<String, Object> m_sampleChannelConfig2 = Maps.newHashMap();
		m_sampleChannelConfig2.put("name", "sample.channel2");
		m_sampleChannelConfig2.put("type", "WRITE");
		m_sampleChannelConfig2.put("value_type", "DOUBLE");
		m_sampleChannelConfig2.put("channel_config", Maps.newHashMap());

		// Reading Channel Configuration for monitoring Information
		final Map<String, Object> m_sampleChannelConfig3 = Maps.newHashMap();
		m_sampleChannelConfig3.put("name", "sample.channel3");
		m_sampleChannelConfig3.put("type", "READ_WRITE");
		m_sampleChannelConfig3.put("value_type", "INTEGER");
		m_sampleChannelConfig3.put("channel_config", Maps.newHashMap());

		properties.put(CHANNEL_PROPERTY_PREFIX + System.nanoTime() + CHANNEL_PROPERTY_POSTFIX, m_sampleChannelConfig1);
		properties.put(CHANNEL_PROPERTY_PREFIX + System.nanoTime() + CHANNEL_PROPERTY_POSTFIX, m_sampleChannelConfig2);
		properties.put(CHANNEL_PROPERTY_PREFIX + System.nanoTime() + CHANNEL_PROPERTY_POSTFIX, m_sampleChannelConfig3);

		return properties;
	}

	/**
	 * Driver properties.
	 *
	 * @return the dictionary
	 */
	private Dictionary<String, Object> newDriverProperties() {
		final Dictionary<String, Object> driverProperties = new Hashtable<String, Object>();
		driverProperties.put("driver.id", "sample.driver");
		return driverProperties;
	}

	/**
	 * Test specific properties.
	 *
	 * @param testName
	 *            the test name
	 * @return the dictionary
	 */
	private Dictionary<String, Object> newTestProperties(final String testName) {
		final Dictionary<String, Object> properties = new Hashtable<String, Object>();
		// The general device properties are required
		properties.put(DEVICE_DRIVER_PROP, "sample.driver");
		properties.put(DEVICE_ID_PROP, "sample.device");
		properties.put(DEVICE_DESC_PROP, "sample.device.description");
		// test runner specific properties
		properties.put("eosgi.testId", testName);
		properties.put("eosgi.testEngine", "junit4");
		return properties;
	}

}
