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
package org.eclipse.kura.asset.test;

import static org.eclipse.kura.asset.internal.AssetConfiguration.ASSET_DESC_PROP;
import static org.eclipse.kura.asset.internal.AssetConfiguration.ASSET_DRIVER_PROP;
import static org.eclipse.kura.asset.internal.AssetConfiguration.ASSET_ID_PROP;
import static org.eclipse.kura.asset.internal.AssetConfiguration.CHANNEL_PROPERTY_POSTFIX;
import static org.eclipse.kura.asset.internal.AssetConfiguration.CHANNEL_PROPERTY_PREFIX;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.Driver;
import org.eclipse.kura.asset.internal.test.DriverStub;
import org.eclipse.kura.wire.internal.WireAsset;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * The bundle activator which registers tests as services.
 */
public final class Activator extends DependencyActivatorBase {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(WireAsset.class);

	/** {@inheritDoc} */
	@Override
	public void destroy(final BundleContext context, final DependencyManager manager) throws Exception {
		// Not used
	}

	private WireAsset getService(final BundleContext context, final Class<Asset> clazz) {
		final ServiceReference<Asset> sr = context.getServiceReference(clazz);
		final Asset device = context.getService(sr);
		return (WireAsset) device;
	}

	/** {@inheritDoc} */
	@Override
	public void init(final BundleContext context, final DependencyManager manager) throws Exception {
		// Registering Driver
		manager.add(this.createComponent().setInterface(Driver.class.getName(), this.newDriverProperties())
				.setImplementation(DriverStub.class));

		s_logger.debug("Getting Wire Device Service Instance.....");
		final WireAsset device = this.getService(context, Asset.class);
		s_logger.debug("Wire Asset Service Instance ====>" + device);
		s_logger.debug("Wire Asset Previous Properties ====>" + device.getAssetConfiguration());
		s_logger.debug("Updating Wire Asset Service Properties.....");
		device.updated(this.newDeviceProperties());
		s_logger.debug("Wire Asset New Properties ====>" + device.getAssetConfiguration());

		// Creating service component for Base Device Test
		// manager.add(this.createComponent()
		// .setInterface(Object.class.getName(),
		// this.newTestProperties("BasicDeviceStubTest"))
		// .setImplementation(DeviceTest.class).add(this.createServiceDependency().setService(Asset.class)
		// .setRequired(true).setCallbacks("bindDevice", "unbindDevice")));
		// Creating service component for Device Configuration Test
		// manager.add(this.createComponent()
		// .setInterface(Object.class.getName(),
		// this.newTestProperties("DeviceConfigurationTest"))
		// .setImplementation(DeviceConfigurationTest.class));
	}

	/**
	 * Prepares and returns the properties of the service component for device.
	 *
	 * @return the dictionary
	 */
	private Map<String, Object> newDeviceProperties() {
		final Map<String, Object> properties = Maps.newHashMap();

		// Service Property
		properties.put("service.pid", "wiredevice.1");

		// General Information
		properties.put(ASSET_DRIVER_PROP, "sample.driver");
		properties.put(ASSET_ID_PROP, "sample.device");
		properties.put(ASSET_DESC_PROP, "sample.device.description");

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
		driverProperties.put(ASSET_DRIVER_PROP, "sample.driver");
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
		properties.put(ASSET_DRIVER_PROP, "sample.driver");
		properties.put(ASSET_ID_PROP, "sample.device");
		properties.put(ASSET_DESC_PROP, "sample.device.description");
		// test runner specific properties
		properties.put("eosgi.testId", testName);
		properties.put("eosgi.testEngine", "junit4");
		return properties;
	}

}
