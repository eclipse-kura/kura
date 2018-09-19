/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.internal.wire.asset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.eclipse.kura.asset.provider.AssetConstants;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WireAssetTest {

    private static void sync(final WireAsset asset) {
        final CountDownLatch latch = new CountDownLatch(1);

        asset.getBaseAssetExecutor().runConfig(latch::countDown);

        try {
            latch.await();
        } catch (InterruptedException e) {
            fail("Interrupted during sync");
        }
    }

    private static void putChannel(final Channel channel, final Map<String, Object> properties) {
        properties.put(
                channel.getName() + AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value() + AssetConstants.NAME.value(),
                channel.getName());
        properties.put(
                channel.getName() + AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value() + AssetConstants.TYPE.value(),
                channel.getType().toString());
        properties.put(channel.getName() + AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value()
                + AssetConstants.VALUE_TYPE.value(), channel.getValueType().toString());
        properties.put(
                channel.getName() + AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value() + AssetConstants.ENABLED.value(),
                Boolean.toString(channel.isEnabled()));
    }

    @Test
    public void testOnWireReceive() throws NoSuchFieldException, ConnectionException {
        final Map<String, Object> wireAssetProperties = new HashMap<>();

        wireAssetProperties.put(AssetConstants.ASSET_DESC_PROP.value(), "description");
        wireAssetProperties.put(AssetConstants.ASSET_DRIVER_PROP.value(), "driverPid");
        wireAssetProperties.put(ConfigurationService.KURA_SERVICE_PID, "componentName");

        Channel readChannel1 = new Channel("readChannel1", ChannelType.READ, DataType.BOOLEAN, new HashMap<>());
        Channel writeChannel2 = new Channel("writeChannel2", ChannelType.WRITE, DataType.BOOLEAN, new HashMap<>());

        putChannel(readChannel1, wireAssetProperties);
        putChannel(writeChannel2, wireAssetProperties);

        List<WireRecord> writeWireRecords = new ArrayList<>();
        Map<String, TypedValue<?>> writeWireRecordProperties = new HashMap<>();
        writeWireRecordProperties.put("writeChannel2_assetName", new StringValue("componentName"));
        writeWireRecordProperties.put("writeChannel2", new BooleanValue(true));
        WireRecord writeWireRecord = new WireRecord(writeWireRecordProperties);
        writeWireRecords.add(writeWireRecord);

        WireEnvelope wireEnvelope = new WireEnvelope("pid", writeWireRecords);

        WireAsset wireAsset = new WireAsset();

        WireSupport mockWireSupport = mock(WireSupport.class);
        WireHelperService wireHelperService = mock(WireHelperService.class);

        when(wireHelperService.newWireSupport(any(), any())).thenReturn(mockWireSupport);

        wireAsset.bindWireHelperService(wireHelperService);

        final ComponentContext mockComponentContext = mock(ComponentContext.class);
        when(mockComponentContext.getBundleContext()).thenReturn(mock(BundleContext.class));

        wireAsset.activate(mockComponentContext, wireAssetProperties);

        Driver mockDriver = mock(Driver.class);

        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            assertEquals(1, arguments.length);

            List<ChannelRecord> records = (List<ChannelRecord>) arguments[0];

            assertEquals(1, records.size());

            ChannelRecord record = records.get(0);
            record.setValue(new BooleanValue(true));
            record.setTimestamp(42);
            record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));

            return null;
        }).when(mockDriver).read(any());

        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            assertEquals(1, arguments.length);

            List<ChannelRecord> records = (List<ChannelRecord>) arguments[0];

            assertEquals(1, records.size());

            ChannelRecord record = records.get(0);
            assertEquals("writeChannel2", record.getChannelName());
            assertEquals(DataType.BOOLEAN, record.getValueType());

            assertEquals(new BooleanValue(true), record.getValue());

            record.setTimestamp(111);
            record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));

            return null;
        }).when(mockDriver).write(any());

        when(mockDriver.getChannelDescriptor()).thenReturn(Collections::emptyList);

        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            assertEquals(1, arguments.length);

            List<WireRecord> wireRecords = (List<WireRecord>) arguments[0];

            assertEquals(1, wireRecords.size());
            Map<String, TypedValue<?>> properties = wireRecords.get(0).getProperties();

            assertEquals(3, properties.size());
            assertEquals(new StringValue("componentName"), properties.get("assetName"));
            assertEquals(new BooleanValue(true), properties.get("readChannel1"));
            assertEquals(new LongValue(42), properties.get("readChannel1_timestamp"));

            return null;
        }).when(mockWireSupport).emit(any());

        wireAsset.setDriver(mockDriver);
        sync(wireAsset);

        wireAsset.onWireReceive(wireEnvelope);

        verify(mockWireSupport).emit(any());
        verify(mockDriver).read(any());
        verify(mockDriver).write(any());
    }

    @Test
    public void testTimestampModes() throws NoSuchFieldException, ConnectionException {

        final Map<String, Object> wireAssetProperties = new HashMap<>();

        wireAssetProperties.put(AssetConstants.ASSET_DESC_PROP.value(), "description");
        wireAssetProperties.put(AssetConstants.ASSET_DRIVER_PROP.value(), "driverPid");
        wireAssetProperties.put(ConfigurationService.KURA_SERVICE_PID, "componentName");

        Channel readChannel1 = new Channel("0", ChannelType.READ, DataType.BOOLEAN, Collections.emptyMap());
        Channel readChannel2 = new Channel("1", ChannelType.READ, DataType.BOOLEAN, Collections.emptyMap());
        Channel readChannel3 = new Channel("2", ChannelType.READ, DataType.BOOLEAN, Collections.emptyMap());

        putChannel(readChannel1, wireAssetProperties);
        putChannel(readChannel2, wireAssetProperties);
        putChannel(readChannel3, wireAssetProperties);

        WireAsset wireAsset = new WireAsset();

        WireSupport mockWireSupport = mock(WireSupport.class);
        WireHelperService wireHelperService = mock(WireHelperService.class);

        when(wireHelperService.newWireSupport(any(), any())).thenReturn(mockWireSupport);

        wireAsset.bindWireHelperService(wireHelperService);

        final ComponentContext mockComponentContext = mock(ComponentContext.class);
        when(mockComponentContext.getBundleContext()).thenReturn(mock(BundleContext.class));

        WireEnvelope wireEnvelope = new WireEnvelope("pid", Collections.emptyList());

        wireAsset.activate(mockComponentContext, wireAssetProperties);

        Driver mockDriver = mock(Driver.class);

        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            assertEquals(1, arguments.length);

            List<ChannelRecord> records = (List<ChannelRecord>) arguments[0];

            assertEquals(3, records.size());

            final long timestamps[] = { 84, 22, 150 };

            for (int i = 0; i < 3; i++) {
                final ChannelRecord record = records.get(i);
                final int channelIndex = Integer.parseInt(record.getChannelName());
                record.setValue(new IntegerValue(channelIndex));
                record.setTimestamp(timestamps[channelIndex]);
                record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
            }

            return null;
        }).when(mockDriver).read(any());

        wireAsset.setDriver(mockDriver);

        {
            wireAssetProperties.put(WireAssetOptions.TIMESTAMP_MODE_PROP_NAME, TimestampMode.NO_TIMESTAMPS.name());

            wireAsset.updated(wireAssetProperties);
            sync(wireAsset);

            final long timestamp = System.currentTimeMillis();
            doAnswer(invocation -> {
                Object[] arguments = invocation.getArguments();
                assertEquals(1, arguments.length);

                List<WireRecord> wireRecords = (List<WireRecord>) arguments[0];

                assertEquals(1, wireRecords.size());
                Map<String, TypedValue<?>> properties = wireRecords.get(0).getProperties();

                assertEquals(4, properties.size());
                assertEquals(new StringValue("componentName"), properties.get("assetName"));
                assertEquals(new IntegerValue(0), properties.get("0"));
                assertEquals(new IntegerValue(1), properties.get("1"));
                assertEquals(new IntegerValue(2), properties.get("2"));

                return null;
            }).when(mockWireSupport).emit(any());

            wireAsset.onWireReceive(wireEnvelope);

            verify(mockWireSupport).emit(any());
        }

        {
            wireAssetProperties.put(WireAssetOptions.TIMESTAMP_MODE_PROP_NAME, TimestampMode.PER_CHANNEL.name());

            wireAsset.updated(wireAssetProperties);
            sync(wireAsset);

            final long timestamp = System.currentTimeMillis();
            doAnswer(invocation -> {
                Object[] arguments = invocation.getArguments();
                assertEquals(1, arguments.length);

                List<WireRecord> wireRecords = (List<WireRecord>) arguments[0];

                assertEquals(1, wireRecords.size());
                Map<String, TypedValue<?>> properties = wireRecords.get(0).getProperties();

                assertEquals(7, properties.size());
                assertEquals(new StringValue("componentName"), properties.get("assetName"));
                assertEquals(new IntegerValue(0), properties.get("0"));
                assertEquals(new LongValue(84), properties.get("0_timestamp"));
                assertEquals(new IntegerValue(1), properties.get("1"));
                assertEquals(new LongValue(22), properties.get("1_timestamp"));
                assertEquals(new IntegerValue(2), properties.get("2"));
                assertEquals(new LongValue(150), properties.get("2_timestamp"));

                return null;
            }).when(mockWireSupport).emit(any());

            wireAsset.onWireReceive(wireEnvelope);

            verify(mockWireSupport, times(2)).emit(any());
        }

        {
            wireAssetProperties.put(WireAssetOptions.TIMESTAMP_MODE_PROP_NAME,
                    TimestampMode.SINGLE_ASSET_GENERATED.name());

            wireAsset.updated(wireAssetProperties);
            sync(wireAsset);

            final long timestamp = System.currentTimeMillis();
            doAnswer(invocation -> {
                Object[] arguments = invocation.getArguments();
                assertEquals(1, arguments.length);

                List<WireRecord> wireRecords = (List<WireRecord>) arguments[0];

                assertEquals(1, wireRecords.size());
                Map<String, TypedValue<?>> properties = wireRecords.get(0).getProperties();

                assertEquals(5, properties.size());
                assertEquals(new StringValue("componentName"), properties.get("assetName"));
                assertTrue(((Long) properties.get(WireAssetConstants.PROP_SINGLE_TIMESTAMP_NAME.value())
                        .getValue()) > timestamp);
                assertEquals(new IntegerValue(0), properties.get("0"));
                assertEquals(new IntegerValue(1), properties.get("1"));
                assertEquals(new IntegerValue(2), properties.get("2"));

                return null;
            }).when(mockWireSupport).emit(any());

            wireAsset.onWireReceive(wireEnvelope);

            verify(mockWireSupport, times(3)).emit(any());
        }

        {
            wireAssetProperties.put(WireAssetOptions.TIMESTAMP_MODE_PROP_NAME,
                    TimestampMode.SINGLE_DRIVER_GENERATED_MAX.name());

            wireAsset.updated(wireAssetProperties);
            sync(wireAsset);

            doAnswer(invocation -> {
                Object[] arguments = invocation.getArguments();
                assertEquals(1, arguments.length);

                List<WireRecord> wireRecords = (List<WireRecord>) arguments[0];

                assertEquals(1, wireRecords.size());
                Map<String, TypedValue<?>> properties = wireRecords.get(0).getProperties();

                assertEquals(5, properties.size());
                assertEquals(new StringValue("componentName"), properties.get("assetName"));
                assertEquals(new LongValue(150), properties.get(WireAssetConstants.PROP_SINGLE_TIMESTAMP_NAME.value()));
                assertEquals(new IntegerValue(0), properties.get("0"));
                assertEquals(new IntegerValue(1), properties.get("1"));
                assertEquals(new IntegerValue(2), properties.get("2"));

                return null;
            }).when(mockWireSupport).emit(any());

            wireAsset.onWireReceive(wireEnvelope);

            verify(mockWireSupport, times(4)).emit(any());
        }

        {
            wireAssetProperties.put(WireAssetOptions.TIMESTAMP_MODE_PROP_NAME,
                    TimestampMode.SINGLE_DRIVER_GENERATED_MIN.name());

            wireAsset.updated(wireAssetProperties);
            sync(wireAsset);

            doAnswer(invocation -> {
                Object[] arguments = invocation.getArguments();
                assertEquals(1, arguments.length);

                List<WireRecord> wireRecords = (List<WireRecord>) arguments[0];

                assertEquals(1, wireRecords.size());
                Map<String, TypedValue<?>> properties = wireRecords.get(0).getProperties();

                assertEquals(5, properties.size());
                assertEquals(new StringValue("componentName"), properties.get("assetName"));
                assertEquals(new LongValue(22), properties.get(WireAssetConstants.PROP_SINGLE_TIMESTAMP_NAME.value()));
                assertEquals(new IntegerValue(0), properties.get("0"));
                assertEquals(new IntegerValue(1), properties.get("1"));
                assertEquals(new IntegerValue(2), properties.get("2"));

                return null;
            }).when(mockWireSupport).emit(any());

            wireAsset.onWireReceive(wireEnvelope);

            verify(mockWireSupport, times(5)).emit(any());
        }

        verify(mockDriver, times(5)).read(any());
    }

}
