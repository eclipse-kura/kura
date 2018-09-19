/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.asset.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.provider.AssetConstants;
import org.eclipse.kura.asset.provider.BaseAsset;
import org.eclipse.kura.asset.provider.BaseAssetExecutor;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.test.annotation.TestTarget;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This AssetTest is responsible to test {@link Asset}
 */
public final class AssetTest {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(AssetTest.class);

    /** Asset Instance */
    private static Asset asset;

    private static Object assetLock = new Object();

    private static ConfigurationService cfgsvc;

    /** A latch to be initialized with the no of OSGi dependencies it needs */
    private static CountDownLatch dependencyLatch = new CountDownLatch(1);

    /** The Device Configuration instance. */
    public AssetConfiguration configuration;

    /** The properties to be parsed as Device Configuration. */
    public Map<String, Object> properties;

    /** The Channel Configuration */
    public Map<String, Object> sampleChannelConfig;

    /**
     * JUnit Callback to be triggered before creating the instance of this suite
     *
     * @throws Exception
     *             if the dependent services are null
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        // Wait for OSGi dependencies
        logger.info("Setting Up The Testcase....");
        try {
            boolean ok = dependencyLatch.await(10, TimeUnit.SECONDS);
            assertTrue("Dependencies should be OK", ok);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("OSGi dependencies unfulfilled");
        }

        // asset = new BaseAsset();

        // initt();
    }

    @Before
    public void init() throws InterruptedException {
        // may need to wait for asset to be registered and injected
        if (asset == null) {
            synchronized (assetLock) {
                assetLock.wait(3000);
            }

        }

        initt();
    }

    /**
     * Initializes asset data
     */
    private static void initt() {
        final Map<String, Object> channels = CollectionUtil.newHashMap();
        channels.put("kura.service.pid", "AssetTest");
        channels.put(AssetConstants.ASSET_DESC_PROP.value(), "sample.asset.desc");
        channels.put(AssetConstants.ASSET_DRIVER_PROP.value(), "org.eclipse.kura.asset.stub.driver");
        channels.put("1.CH#+name", "1.CH");
        channels.put("1.CH#+type", "READ");
        channels.put("1.CH#+value.type", "INTEGER");
        channels.put("1.CH#DRIVER.modbus.register", "sample.channel1.modbus.register");
        channels.put("1.CH#DRIVER.modbus.FC", "sample.channel1.modbus.FC");
        channels.put("2.CH#+name", "2.CH");
        channels.put("2.CH#+enabled", "true");
        channels.put("2.CH#+type", "WRITE");
        channels.put("2.CH#+value.type", "BOOLEAN");
        channels.put("2.CH#DRIVER.modbus.register", "sample.channel2.modbus.register");
        channels.put("2.CH#DRIVER.modbus.DUMMY.NN", "sample.channel2.modbus.FC");
        channels.put("3.CH#+name", "3.CH");
        channels.put("3.CH#+type", "READ");
        channels.put("3.CH#+enabled", "false");
        channels.put("3.CH#+value.type", "INTEGER");
        channels.put("3.CH#DRIVER.modbus.register", "sample.channel1.modbus.register");
        channels.put("3.CH#DRIVER.modbus.FC", "sample.channel1.modbus.FC");
        channels.put("4.CH#+name", "3.CH");
        channels.put("4.CH#+enabled", "false");
        channels.put("4.CH#+type", "WRITE");
        channels.put("4.CH#+value.type", "BOOLEAN");
        channels.put("4.CH#DRIVER.modbus.register", "sample.channel2.modbus.register");
        channels.put("4.CH#DRIVER.modbus.DUMMY.NN", "sample.channel2.modbus.FC");

        ((BaseAsset) asset).updated(channels);
        sync(asset);
    }

    protected void activate() throws KuraException {
        if (cfgsvc != null) {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("driver.pid", "org.eclipse.kura.asset.stub.driver");

            cfgsvc.createFactoryConfiguration("org.eclipse.kura.asset", "testAsset", props, false);
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
        assertEquals("org.eclipse.kura.asset.stub.driver", assetConfiguration.getDriverPid());
        assertEquals("sample.asset.desc", assetConfiguration.getAssetDescription());
    }

    /**
     * Test sample channel properties.
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testChannelProperties() {
        final AssetConfiguration assetConfiguration = asset.getAssetConfiguration();
        assertNotNull(assetConfiguration);
        final Map<String, Channel> channels = assetConfiguration.getAssetChannels();
        assertEquals(4, channels.size());

        final Channel channel1 = channels.get("1.CH");
        assertTrue(channel1.isEnabled());
        assertEquals("1.CH", channel1.getName());
        assertEquals(ChannelType.READ, channel1.getType());
        assertEquals(DataType.INTEGER, channel1.getValueType());
        assertEquals("sample.channel1.modbus.register", channel1.getConfiguration().get("DRIVER.modbus.register"));
        assertEquals("sample.channel1.modbus.FC", channel1.getConfiguration().get("DRIVER.modbus.FC"));

        final Channel channel2 = channels.get("2.CH");
        assertTrue(channel2.isEnabled());
        assertEquals("2.CH", channel2.getName());
        assertEquals(ChannelType.WRITE, channel2.getType());
        assertEquals(DataType.BOOLEAN, channel2.getValueType());
        assertEquals("sample.channel2.modbus.register", channel2.getConfiguration().get("DRIVER.modbus.register"));
        assertEquals("sample.channel2.modbus.FC", channel2.getConfiguration().get("DRIVER.modbus.DUMMY.NN"));
    }

    /**
     * Test listening operation.
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testListen() throws KuraException {

        AtomicBoolean invoked = new AtomicBoolean(false);

        final ChannelListener listener = new ChannelListener() {

            @Override
            public void onChannelEvent(ChannelEvent event) {
                assertEquals(1, event.getChannelRecord().getValue().getValue());

                invoked.set(true);
            }
        };

        asset.registerChannelListener("1.CH", listener);
        sync(asset);

        assertTrue(invoked.get());
    }

    /**
     * Listeners should be removed from the driver when it is detached from the asset and added when it is attached.
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testListenerAttachOnDriverChange() throws KuraException {
        final Driver driver = ((BaseAsset) asset).getDriver();

        final ArrayList<Boolean> attachSequence = new ArrayList<>();

        final ChannelListener listener = new ChannelListener() {

            @Override
            public void onChannelEvent(ChannelEvent event) {
                if ("unregister".equals(event.getChannelRecord().getChannelName())) {
                    attachSequence.add(false);
                } else {
                    attachSequence.add(true);
                }
            }
        };

        asset.registerChannelListener("1.CH", listener);
        sync(asset);
        assertEquals(Arrays.asList(true), attachSequence);

        ((BaseAsset) asset).unsetDriver();
        sync(asset);
        assertEquals(Arrays.asList(true, false), attachSequence);

        ((BaseAsset) asset).setDriver(driver);
        sync(asset);
        assertEquals(Arrays.asList(true, false, true), attachSequence);
    }

    /**
     * It should be possible to add listeners to an asset even if the driver is not attached, the asset should attach
     * them later on when the driver is tracked
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testAttachListenerWhitoutDriver() throws KuraException {
        final Driver driver = ((BaseAsset) asset).getDriver();

        final ArrayList<Boolean> attachSequence = new ArrayList<>();

        final ChannelListener listener = new ChannelListener() {

            @Override
            public void onChannelEvent(ChannelEvent event) {
                if ("unregister".equals(event.getChannelRecord().getChannelName())) {
                    attachSequence.add(false);
                } else {
                    attachSequence.add(true);
                }
            }
        };

        ((BaseAsset) asset).unsetDriver();
        sync(asset);

        asset.registerChannelListener("1.CH", listener);
        sync(asset);
        assertEquals(Arrays.asList(), attachSequence);

        asset.registerChannelListener("1.CH", listener);
        sync(asset);
        assertEquals(Arrays.asList(), attachSequence);

        ((BaseAsset) asset).setDriver(driver);
        sync(asset);
        assertEquals(Arrays.asList(true), attachSequence);
    }

    /**
     * The same channel listener should not be attached multiple times.
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testShouldNotReattachSameListener() throws KuraException {

        final ArrayList<Boolean> attachSequence = new ArrayList<>();

        final ChannelListener listener = new ChannelListener() {

            @Override
            public void onChannelEvent(ChannelEvent event) {
                if ("unregister".equals(event.getChannelRecord().getChannelName())) {
                    attachSequence.add(false);
                } else {
                    attachSequence.add(true);
                }
            }
        };

        asset.registerChannelListener("1.CH", listener);
        sync(asset);
        assertEquals(Arrays.asList(true), attachSequence);

        asset.registerChannelListener("1.CH", listener);
        sync(asset);
        assertEquals(Arrays.asList(true), attachSequence);

        asset.registerChannelListener("1.CH", listener);
        sync(asset);
        assertEquals(Arrays.asList(true), attachSequence);

        asset.registerChannelListener("1.CH", listener);
        sync(asset);
        assertEquals(Arrays.asList(true), attachSequence);
    }

    /**
     * Test listener unregistration.
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testUnlisten() throws KuraException {
        AtomicInteger invoked = new AtomicInteger(0);

        final ChannelListener listener = new ChannelListener() {

            @Override
            public void onChannelEvent(ChannelEvent event) {
                int cnt = invoked.getAndIncrement();

                if (cnt == 0) {
                    assertEquals(1, event.getChannelRecord().getValue().getValue());
                } else if (cnt == 1) {
                    assertEquals("unregister", event.getChannelRecord().getChannelName());
                    assertEquals(DataType.BOOLEAN, event.getChannelRecord().getValueType());
                } else {
                    fail("Unexpected invocation.");
                }
            }
        };

        asset.registerChannelListener("1.CH", listener);
        sync(asset);

        assertEquals(1, invoked.get());

        asset.unregisterChannelListener(listener);
        sync(asset);

        assertEquals(2, invoked.get());
    }

    /**
     * Test exception during listener unregistration.
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testUnlistenDriverException() throws KuraException {
        AtomicInteger invoked = new AtomicInteger(0);
        AtomicBoolean disableListener = new AtomicBoolean(false);

        final ChannelListener listener = new ChannelListener() {

            @Override
            public void onChannelEvent(ChannelEvent event) {
                if (disableListener.get()) {
                    return;
                }

                int cnt = invoked.getAndIncrement();

                if (cnt == 0) {
                    assertEquals(1, event.getChannelRecord().getValue().getValue());
                } else if (cnt == 1) {
                    throw new IllegalArgumentException("test");
                } else {
                    fail("Unexpected invocation.");
                }
            }
        };

        asset.registerChannelListener("1.CH", listener);
        sync(asset);

        assertEquals(1, invoked.get());

        asset.unregisterChannelListener(listener);
        sync(asset);

        assertEquals(2, invoked.get());

        disableListener.set(true);
    }

    /**
     * Test reading operation.
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testRead() throws KuraException {
        final List<ChannelRecord> records = asset.read(new HashSet(Arrays.asList("1.CH")));

        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals(1, records.get(0).getValue().getValue());
    }

    /**
     * Tests the condition in case the channel type is not readable
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testReadChannelNotReadable() throws KuraException {
        List<ChannelRecord> result = asset.read(new HashSet(Arrays.asList("2.CH")));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ChannelFlag.FAILURE, result.get(0).getChannelStatus().getChannelFlag());
    }

    /**
     * Test reading operation on all channels.
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testReadAllChannels() throws KuraException {
        final List<ChannelRecord> records = asset.readAllChannels();

        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals(1, records.get(0).getValue().getValue());
    }

    /**
     * Test writing operation.
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testWrite() throws KuraException {
        ChannelRecord channelRecord = ChannelRecord.createWriteRecord("2.CH", TypedValues.newBooleanValue(true));

        List<ChannelRecord> records = new ArrayList<>();
        records.add(channelRecord);
        assertEquals(1, records.size());

        asset.write(records);

        assertEquals(ChannelFlag.SUCCESS, channelRecord.getChannelStatus().getChannelFlag());
    }

    /**
     * Tests the condition in case the channel type is not writable
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testWriteChannelNotWritable() throws KuraException {
        ChannelRecord channelRecord = ChannelRecord.createWriteRecord("1.CH", TypedValues.newLongValue(1L));

        List<ChannelRecord> list = Arrays.asList(channelRecord);
        assertEquals(1, list.size());

        asset.write(list);

        assertEquals(ChannelFlag.FAILURE, channelRecord.getChannelStatus().getChannelFlag());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetConfiguration() throws KuraException {
        assumeTrue(asset instanceof BaseAsset);

        ComponentConfiguration cfg = ((BaseAsset) asset).getConfiguration();

        assertEquals("AssetTest", cfg.getPid());

        OCD ocd = cfg.getDefinition();
        assertEquals("org.eclipse.kura.asset", ocd.getId());
        assertTrue("Wire Asset".equals(ocd.getName()) || "AssetMessages.ocdName".equals(ocd.getName()));

        List<AD> ads = ocd.getAD();
        assertNotNull(ads);
        assertEquals(22, ads.size()); // description, driver, 20 from BaseChannelDescriptor and StubChannelDescriptor

        assertEquals("asset.desc", ads.get(0).getId());
        assertEquals("driver.pid", ads.get(1).getId());

        String[] expectedValues = { "#+enabled", "#+name", "#+type", "#+value.type", "#unit.id" };

        final int expectedChannelCount = 4;
        for (int i = 0; i < expectedValues.length; i++) {
            for (int j = 0; j < expectedChannelCount; j++) {
                final String id = (j + 1) + ".CH" + expectedValues[i];
                assertEquals(1, ads.parallelStream().filter(ad -> ad.getId().equals(id)).count());
            }
        }
    }

    /**
     * Tests reading disabled channel
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testReadChannelDisabled() throws KuraException {
        List<ChannelRecord> result = asset.read(new HashSet(Arrays.asList("3.CH")));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ChannelFlag.FAILURE, result.get(0).getChannelStatus().getChannelFlag());
    }

    /**
     * Tests writing disabled channel
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testWriteChannelDisabled() throws KuraException {
        ChannelRecord channelRecord = ChannelRecord.createWriteRecord("4.CH", TypedValues.newLongValue(1L));

        List<ChannelRecord> list = Arrays.asList(channelRecord);
        assertEquals(1, list.size());

        asset.write(list);

        assertEquals(ChannelFlag.FAILURE, channelRecord.getChannelStatus().getChannelFlag());
    }

    /**
     * It should be possible to attach listeners on disabled channels, but listener should not be forwarded to the
     * driver in this case driver
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testListenerAttachOnDisabledChannel() throws KuraException {
        final ArrayList<Boolean> attachSequence = new ArrayList<>();

        final ChannelListener listener = new ChannelListener() {

            @Override
            public void onChannelEvent(ChannelEvent event) {
                if ("unregister".equals(event.getChannelRecord().getChannelName())) {
                    attachSequence.add(false);
                } else {
                    attachSequence.add(true);
                }
            }
        };

        asset.registerChannelListener("3.CH", listener);
        sync(asset);
        assertEquals(Arrays.asList(), attachSequence);
    }

    /**
     * Listeners attached to a channel should be detached from the driver if the channel is disabled and reattached if
     * the channel is enabled again
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testEnableDisableChannelWithListener() throws KuraException {
        final ArrayList<Boolean> attachSequence = new ArrayList<>();

        final Map<String, Object> channels = CollectionUtil.newHashMap();
        channels.put("kura.service.pid", "AssetTest");
        channels.put(AssetConstants.ASSET_DESC_PROP.value(), "sample.asset.desc");
        channels.put(AssetConstants.ASSET_DRIVER_PROP.value(), "org.eclipse.kura.asset.stub.driver");
        channels.put("3.CH#+name", "3.CH");
        channels.put("3.CH#+type", "READ");
        channels.put("3.CH#+enabled", "false");
        channels.put("3.CH#+value.type", "INTEGER");
        channels.put("3.CH#DRIVER.modbus.register", "sample.channel1.modbus.register");
        channels.put("3.CH#DRIVER.modbus.FC", "sample.channel1.modbus.FC");

        ((BaseAsset) asset).updated(channels);
        sync(asset);

        final ChannelListener listener = new ChannelListener() {

            @Override
            public void onChannelEvent(ChannelEvent event) {
                if ("unregister".equals(event.getChannelRecord().getChannelName())) {
                    attachSequence.add(false);
                } else {
                    attachSequence.add(true);
                }
            }
        };

        asset.registerChannelListener("3.CH", listener);
        sync(asset);
        assertEquals(Arrays.asList(), attachSequence);

        channels.put("3.CH#+enabled", "true");
        ((BaseAsset) asset).updated(channels);
        sync(asset);

        assertEquals(Arrays.asList(true), attachSequence);

        channels.put("3.CH#+enabled", "false");

        ((BaseAsset) asset).updated(channels);
        sync(asset);

        assertEquals(Arrays.asList(true, false), attachSequence);

        initt();
    }

    /**
     * Listeners attached to a channel should be detached from the driver if the channel is disabled and reattached if
     * the channel is enabled again
     */
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testCompleteConfigWithDefaults() throws KuraException {

        final Map<String, Object> channels = CollectionUtil.newHashMap();
        channels.put("kura.service.pid", "AssetTest");
        channels.put(AssetConstants.ASSET_DESC_PROP.value(), "sample.asset.desc");
        channels.put(AssetConstants.ASSET_DRIVER_PROP.value(), "non.existing.pid");
        channels.put("3.CH#+name", "3.CH");
        channels.put("3.CH#+type", "READ");
        channels.put("3.CH#+enabled", "false");
        channels.put("3.CH#+value.type", "INTEGER");

        ((BaseAsset) asset).updated(channels);
        sync(asset);

        assertFalse(
                asset.getAssetConfiguration().getAssetChannels().get("3.CH").getConfiguration().containsKey("unit.id"));

        channels.put(AssetConstants.ASSET_DRIVER_PROP.value(), "org.eclipse.kura.asset.stub.driver");

        ((BaseAsset) asset).updated(channels);
        sync(asset);

        assertEquals("5",
                asset.getAssetConfiguration().getAssetChannels().get("3.CH").getConfiguration().get("unit.id"));

        initt();
    }

    public void bindAsset(Asset asset) {
        AssetTest.asset = asset;

        synchronized (assetLock) {
            assetLock.notifyAll();
        }
    }

    public void unbindAsset(Asset asset) {
        AssetTest.asset = null;
    }

    public void bindCfgSvc(ConfigurationService cfgSvc) {
        AssetTest.cfgsvc = cfgSvc;
        dependencyLatch.countDown();
    }

    public void unbindCfgSvc(ConfigurationService cfgSvc) {
        AssetTest.cfgsvc = null;
    }

    private static void sync(final Asset asset) {
        final BaseAsset baseAsset = (BaseAsset) asset;
        final BaseAssetExecutor executor = baseAsset.getBaseAssetExecutor();

        final CountDownLatch latch = new CountDownLatch(1);

        executor.runConfig(latch::countDown);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted during sync");
        }
    }

}
