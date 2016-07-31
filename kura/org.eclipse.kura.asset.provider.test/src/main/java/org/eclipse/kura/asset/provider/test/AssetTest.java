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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetConstants;
import org.eclipse.kura.asset.AssetEvent;
import org.eclipse.kura.asset.AssetFlag;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.ChannelType;
import org.eclipse.kura.asset.listener.AssetListener;
import org.eclipse.kura.test.annotation.TestTarget;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This AssetTest is responsible to test {@link Asset}
 */
public final class AssetTest {

	/** Asset Instance */
	private static Asset asset;

	/** A latch to be initialized with the no of OSGi dependencies it needs */
	private static CountDownLatch dependencyLatch = new CountDownLatch(2);

	/** The Asset Service instance. */
	private static volatile AssetService s_assetService;

	/** Logger */
	private static final Logger s_logger = LoggerFactory.getLogger(AssetTest.class);

	/** The Device Configuration instance. */
	public AssetConfiguration m_configuration;

	/** The properties to be parsed as Device Configuration. */
	public Map<String, Object> m_properties;

	/** The Channel Configuration */
	public Map<String, Object> m_sampleChannelConfig;

	/**
	 * Binds the Asset Service.
	 *
	 * @param assetService
	 *            the new Asset Helper Service
	 */
	public synchronized void bindAssetService(final AssetService assetService) {
		if (s_assetService == null) {
			s_assetService = assetService;
			dependencyLatch.countDown();
		}
	}

	/**
	 * Test generic asset properties.
	 */
	@TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	@Test
	public void testBasicProperties() {
		final AssetConfiguration assetConfiguration = asset.getAssetConfiguration();
		assertNotNull(assetConfiguration);
		assertEquals("org.eclipse.kura.asset.stub.driver", assetConfiguration.getDriverId());
		assertEquals("sample.asset.desc", assetConfiguration.getAssetDescription());
		assertEquals("sample.asset.name", assetConfiguration.getAssetName());
	}

	/**
	 * Test sample channel properties.
	 */
	@TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	@Test
	public void testChannelProperties() {
		final AssetConfiguration assetConfiguration = asset.getAssetConfiguration();
		assertNotNull(assetConfiguration);
		final Map<Long, Channel> channels = assetConfiguration.getAssetChannels();
		assertEquals(2, channels.size());
		final Channel channel1 = channels.get(1L);
		assertEquals(1L, channel1.getId());
		assertEquals("sample.channel1.name", channel1.getName());
		assertEquals(ChannelType.READ, channel1.getType());
		assertEquals(DataType.INTEGER, channel1.getValueType());
		assertEquals("sample.channel1.modbus.register", channel1.getConfiguration().get("modbus.register"));
		assertEquals("sample.channel1.modbus.FC", channel1.getConfiguration().get("modbus.FC"));
		final Channel channel2 = channels.get(2L);
		assertEquals(2L, channel2.getId());
		assertEquals(ChannelType.WRITE, channel2.getType());
		assertEquals(DataType.BOOLEAN, channel2.getValueType());
		assertEquals("sample.channel2.modbus.register", channel2.getConfiguration().get("modbus.register"));
		assertEquals("sample.channel2.modbus.FC", channel2.getConfiguration().get("modbus.DUMMY.NN"));
	}

	/**
	 * Test listening operation.
	 */
	@TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	@Test
	public void testListen() throws KuraException {
		final AssetListener listener = new AssetListener() {
			/** {@inheritDoc} */
			@Override
			public void onAssetEvent(final AssetEvent event) {
				assertEquals(1, event.getAssetRecord().getValue().getValue());
			}
		};
		asset.registerAssetListener(1, listener);
	}

	/**
	 * Test reading operation.
	 */
	@TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	@Test
	public void testRead() throws KuraException {
		final List<AssetRecord> records = asset.read(Arrays.asList(1L));
		assertEquals(1, records.size());
		assertEquals(1, records.get(0).getValue().getValue());
	}

	/**
	 * Tests the condition in case the channel type is not readable
	 */
	@TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	@Test(expected = KuraRuntimeException.class)
	public void testReadChannelNotReadable() throws KuraException {
		asset.read(Arrays.asList(2L));
	}

	/**
	 * Test writing operation.
	 */
	@TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	@Test
	public void testWrite() throws KuraException {
		final AssetRecord assetRecord = s_assetService.newAssetRecord(2L);
		assetRecord.setValue(TypedValues.newBooleanValue(true));
		final List<AssetRecord> records = asset.write(Arrays.asList(assetRecord));
		assertEquals(1, records.size());
		assertEquals(AssetFlag.WRITE_SUCCESSFUL, records.get(0).getAssetFlag());
	}

	/**
	 * Tests the condition in case the channel type is not writable
	 */
	@TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	@Test(expected = KuraRuntimeException.class)
	public void testWriteChannelNotWritable() throws KuraException {
		final AssetRecord assetRecord = s_assetService.newAssetRecord(1L);
		asset.write(Arrays.asList(assetRecord));
	}

	/**
	 * Unbinds the Asset Service.
	 *
	 * @param assetService
	 *            the Asset Service
	 */
	public synchronized void unbindAssetService(final AssetService assetService) {
		if (s_assetService == assetService) {
			s_assetService = null;
		}
	}

	/**
	 * Initializes asset data
	 */
	private static void init() {
		final Map<String, Object> channels = CollectionUtil.newHashMap();
		channels.put(AssetConstants.ASSET_DESC_PROP.value(), "sample.asset.desc");
		channels.put(AssetConstants.ASSET_NAME_PROP.value(), "sample.asset.name");
		channels.put(AssetConstants.ASSET_DRIVER_PROP.value(), "org.eclipse.kura.asset.stub.driver");
		channels.put("1.CH.name", "sample.channel1.name");
		channels.put("1.CH.type", "READ");
		channels.put("1.CH.value.type", "INTEGER");
		channels.put("1.CH.DRIVER.modbus.register", "sample.channel1.modbus.register");
		channels.put("1.CH.DRIVER.modbus.FC", "sample.channel1.modbus.FC");
		channels.put("2.CH.name", "sample.channel2.name");
		channels.put("2.CH.type", "WRITE");
		channels.put("2.CH.value.type", "BOOLEAN");
		channels.put("2.CH.DRIVER.modbus.register", "sample.channel2.modbus.register");
		channels.put("2.CH.DRIVER.modbus.DUMMY.NN", "sample.channel2.modbus.FC");
		asset.initialize(channels);
	}

	/**
	 * JUnit Callback to be triggered before creating the instance of this suite
	 *
	 * @throws Exception
	 *             if the dependent services are null
	 */
	@BeforeClass
	public static void setUpClass() throws Exception {
		// Wait for OSGi dependencies
		s_logger.info("Setting Up The Testcase....");
		try {
			dependencyLatch.await(10, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			fail("OSGi dependencies unfulfilled");
		}
		asset = s_assetService.newAsset();
		init();
	}

}
