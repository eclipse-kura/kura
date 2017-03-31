/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.internal.wire.asset;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.ChannelType;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.driver.DriverFlag;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WireAssetTest {

    @Test
    public void testOnWireReceive() throws NoSuchFieldException, ConnectionException {
        Map<String, Object> readChannel1Config = new HashMap<>();
        readChannel1Config.put("channel.id", 1);
        Channel readChannel1 = new Channel(1, "readChannel1", ChannelType.READ, DataType.BOOLEAN, readChannel1Config);

        Map<String, Object> writeChannel2Config = new HashMap<>();
        writeChannel2Config.put("channel.id", 2);
        Channel writeChannel2 = new Channel(2, "writeChannel2", ChannelType.WRITE, DataType.BOOLEAN,
                writeChannel2Config);

        Map<Long, Channel> channels = new HashMap<>();
        channels.put(readChannel1.getId(), readChannel1);
        channels.put(writeChannel2.getId(), writeChannel2);

        AssetConfiguration assetConfiguration = new AssetConfiguration("description", "driverPid", channels);

        List<WireRecord> writeWireRecords = new ArrayList<>();
        Map<String, TypedValue<?>> writeWireRecordProperties = new HashMap<>();
        writeWireRecordProperties.put("writeChannel2_assetName", new StringValue("componentName"));
        writeWireRecordProperties.put("writeChannel2", new BooleanValue(true));
        writeWireRecordProperties.put("writeChannel2_channelId", new LongValue(2));
        WireRecord writeWireRecord = new WireRecord(writeWireRecordProperties);
        writeWireRecords.add(writeWireRecord);

        WireEnvelope wireEnvelope = new WireEnvelope("pid", writeWireRecords);

        Map<String, Object> assetProperties = new HashMap<>();
        assetProperties.put(ConfigurationService.KURA_SERVICE_PID, "componentName");

        WireAsset wireAsset = new WireAsset();
        TestUtil.setFieldValue(wireAsset, "kuraServicePid", "componentName");
        TestUtil.setFieldValue(wireAsset, "properties", assetProperties);
        TestUtil.setFieldValue(wireAsset, "assetConfiguration", assetConfiguration);

        Driver mockDriver = mock(Driver.class);
        wireAsset.setDriver(mockDriver);

        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            assertEquals(1, arguments.length);

            List<DriverRecord> records = (List<DriverRecord>) arguments[0];

            assertEquals(1, records.size());

            DriverRecord record = records.get(0);
            record.setValue(new BooleanValue(true));
            record.setTimestamp(42);
            record.setDriverStatus(new DriverStatus(DriverFlag.READ_SUCCESSFUL));

            return null;
        }).when(mockDriver).read(any());

        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            assertEquals(1, arguments.length);

            List<DriverRecord> records = (List<DriverRecord>) arguments[0];

            assertEquals(1, records.size());

            DriverRecord record = records.get(0);
            Map<String, Object> channelConfig = record.getChannelConfig();
            assertEquals((long) 2, channelConfig.get("channel.id"));
            assertEquals(DataType.BOOLEAN, channelConfig.get("channel.value.type"));

            assertEquals(new BooleanValue(true), record.getValue());

            record.setTimestamp(111);
            record.setDriverStatus(new DriverStatus(DriverFlag.WRITE_SUCCESSFUL));

            return null;
        }).when(mockDriver).write(any());

        WireSupport mockWireSupport = mock(WireSupport.class);
        TestUtil.setFieldValue(wireAsset, "wireSupport", mockWireSupport);

        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            assertEquals(1, arguments.length);

            List<WireRecord> wireRecords = (List<WireRecord>) arguments[0];

            assertEquals(1, wireRecords.size());
            Map<String, TypedValue<?>> properties = wireRecords.get(0).getProperties();

            assertEquals(4, properties.size());
            assertEquals(new StringValue("componentName"), properties.get("readChannel1_assetName"));
            assertEquals(new BooleanValue(true), properties.get("readChannel1"));
            assertEquals(new LongValue(1), properties.get("readChannel1_channelId"));
            assertEquals(new LongValue(42), properties.get("readChannel1_timestamp"));

            return null;
        }).when(mockWireSupport).emit(any());

        wireAsset.onWireReceive(wireEnvelope);

        verify(mockWireSupport).emit(any());
        verify(mockDriver).read(any());
        verify(mockDriver).write(any());
    }

}
