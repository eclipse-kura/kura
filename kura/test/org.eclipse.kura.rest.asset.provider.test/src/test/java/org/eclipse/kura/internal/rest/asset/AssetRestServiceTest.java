/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.rest.asset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.ws.rs.WebApplicationException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.google.gson.JsonElement;

public class AssetRestServiceTest {

    @Test
    public void testListAssetPids() throws InvalidSyntaxException {
        // test retrieval of asset service pids

        Collection<ServiceReference<Asset>> svcRefs = new ArrayList<>();

        ServiceReference<Asset> svcRef = mock(ServiceReference.class);
        when(svcRef.getProperty("kura.service.pid")).thenReturn("pid1");
        svcRefs.add(svcRef);

        ServiceReference<Asset> svcRef2 = mock(ServiceReference.class);
        when(svcRef2.getProperty("kura.service.pid")).thenReturn("pid2");
        svcRefs.add(svcRef2);

        ServiceReference<Asset> svcRef3 = mock(ServiceReference.class);
        when(svcRef3.getProperty("kura.pid")).thenReturn("pid3");
        svcRefs.add(svcRef3);

        AssetRestService svc = new AssetRestService() {

            @Override
            protected Collection<ServiceReference<Asset>> getAssetServiceReferences() throws InvalidSyntaxException {
                return svcRefs;
            }
        };

        List<String> pids = svc.listAssetPids();
        assertEquals(3, pids.size());
        assertEquals("pid1", pids.get(0));
        assertEquals("pid2", pids.get(1));
        assertNull(pids.get(2));
    }

    @Test(expected = WebApplicationException.class)
    public void testGetAssetChannelsAssetNotFound() {
        // test unsuccessful retrieval of channels due to asset not being found

        AssetRestService svc = new AssetRestService();

        String pid = "pid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = null;
        when(asMock.getAsset(pid)).thenReturn(asset);

        svc.getAssetChannels(pid);
    }

    @Test
    public void testGetAssetChannels() {
        // test successful retrieval of asset channels

        AssetRestService svc = new AssetRestService();

        String pid = "pid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);

        Map<String, Channel> channels = new TreeMap<>();
        Map<String, Object> channelConfig = new HashMap<>();
        Channel ch1 = new Channel("ch1", ChannelType.READ, DataType.INTEGER, channelConfig);
        channels.put(ch1.getName(), ch1);
        channelConfig = new HashMap<>();
        Channel ch2 = new Channel("ch2", ChannelType.WRITE, DataType.STRING, channelConfig);
        channels.put(ch2.getName(), ch2);

        AssetConfiguration assetConfig = new AssetConfiguration("description", "driverPid", channels);
        when(asset.getAssetConfiguration()).thenReturn(assetConfig);

        Collection<Channel> assetChannels = svc.getAssetChannels(pid);

        assertEquals(2, assetChannels.size());
        assertEquals(ch1, assetChannels.toArray()[0]);
        assertEquals(ch2, assetChannels.toArray()[1]);
    }

    @Test
    public void testReadAllChannels() throws KuraException {
        // test reading all channels with all supported data types

        AssetRestService svc = new AssetRestService();

        String pid = "pid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("ch1", DataType.INTEGER);
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setValue(TypedValues.newIntegerValue(1));
        records.add(record);
        record = ChannelRecord.createReadRecord("ch2", DataType.STRING);
        record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE));
        records.add(record);
        record = ChannelRecord.createReadRecord("ch3", DataType.STRING);
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setValue(TypedValues.newStringValue("val"));
        records.add(record);
        record = ChannelRecord.createReadRecord("ch4", DataType.BOOLEAN);
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setValue(TypedValues.newBooleanValue(true));
        records.add(record);
        record = ChannelRecord.createReadRecord("ch5", DataType.BYTE_ARRAY);
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setValue(TypedValues.newByteArrayValue(new byte[] {1,2,3}));
        records.add(record);
        record = ChannelRecord.createReadRecord("ch6", DataType.DOUBLE);
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setValue(TypedValues.newDoubleValue(1.234));
        records.add(record);
        record = ChannelRecord.createReadRecord("ch7", DataType.FLOAT);
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setValue(TypedValues.newFloatValue(12.34f));
        records.add(record);
        record = ChannelRecord.createReadRecord("ch8", DataType.LONG);
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setValue(TypedValues.newLongValue(1234));
        records.add(record);
        when(asset.readAllChannels()).thenReturn(records);

        JsonElement json = svc.read(pid);
        assertEquals(
                "[{\"channelStatus\":{\"channelFlag\":\"SUCCESS\"},\"name\":\"ch1\",\"valueType\":\"INTEGER\",\"value\":1,\"timestamp\":0},"
                        + "{\"channelStatus\":{\"channelFlag\":\"FAILURE\"},\"name\":\"ch2\",\"valueType\":\"STRING\",\"timestamp\":0},"
                        + "{\"channelStatus\":{\"channelFlag\":\"SUCCESS\"},\"name\":\"ch3\",\"valueType\":\"STRING\",\"value\":\"val\",\"timestamp\":0},"
                        + "{\"channelStatus\":{\"channelFlag\":\"SUCCESS\"},\"name\":\"ch4\",\"valueType\":\"BOOLEAN\",\"value\":true,\"timestamp\":0},"
                        + "{\"channelStatus\":{\"channelFlag\":\"SUCCESS\"},\"name\":\"ch5\",\"valueType\":\"BYTE_ARRAY\",\"value\":\"AQID\",\"timestamp\":0},"
                        + "{\"channelStatus\":{\"channelFlag\":\"SUCCESS\"},\"name\":\"ch6\",\"valueType\":\"DOUBLE\",\"value\":1.234,\"timestamp\":0},"
                        + "{\"channelStatus\":{\"channelFlag\":\"SUCCESS\"},\"name\":\"ch7\",\"valueType\":\"FLOAT\",\"value\":12.34,\"timestamp\":0},"
                        + "{\"channelStatus\":{\"channelFlag\":\"SUCCESS\"},\"name\":\"ch8\",\"valueType\":\"LONG\",\"value\":1234,\"timestamp\":0}]",
                json.toString());
    }

    @Test
    public void testReadSelectedChannelsValidationException() throws KuraException {
        // test selective channel read with invalid request

        AssetRestService svc = new AssetRestService();

        String pid = "pid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);

        ReadRequest requestMock = mock(ReadRequest.class);
        try {
            svc.read(pid, requestMock);
            fail("Expected an exception.");
        } catch (WebApplicationException e) {
            // OK
        }

        verify(requestMock, times(1)).isValid();
    }

    @Test
    public void testReadSelectedChannels() throws KuraException {
        // test selective channel read

        AssetRestService svc = new AssetRestService();

        String pid = "pid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset assetMock = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(assetMock);

        Set<String> channelNames = new HashSet<>();
        channelNames.add("ch1");

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("ch1", DataType.BOOLEAN);
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setValue(TypedValues.newBooleanValue(true));
        records.add(record);
        when(assetMock.read(channelNames)).thenReturn(records);

        ReadRequest request = new ReadRequest() {

            @Override
            public Set<String> getChannelNames() {
                return channelNames;
            }

            @Override
            public boolean isValid() {
                return true;
            }
        };

        JsonElement json = svc.read(pid, request);
        assertEquals(
                "[{\"channelStatus\":{\"channelFlag\":\"SUCCESS\"},\"name\":\"ch1\",\"valueType\":\"BOOLEAN\",\"value\":true,\"timestamp\":0}]",
                json.toString());
    }

    @Test
    public void testWriteValidationException() throws KuraException {
        // test channel write with invalid request

        AssetRestService svc = new AssetRestService();

        String pid = "pid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);

        WriteRequestList requestsMock = mock(WriteRequestList.class);
        try {
            svc.write(pid, requestsMock);
            fail("Expected an exception.");
        } catch (WebApplicationException e) {
            // OK
        }

        verify(requestsMock, times(1)).isValid();
    }

    @Test
    public void testWrite() throws KuraException, NoSuchFieldException {
        // write multiple channels of various types

        AssetRestService svc = new AssetRestService();

        String pid = "pid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);

        List<WriteRequest> requests = new ArrayList<>();
        addRequest(requests, "ch1", DataType.DOUBLE, "1.234");
        addRequest(requests, "ch2", DataType.STRING, "val");
        addRequest(requests, "ch3", DataType.BOOLEAN, "true");
        addRequest(requests, "ch4", DataType.BYTE_ARRAY, "AQID");
        addRequest(requests, "ch5", DataType.FLOAT, "12.34");
        addRequest(requests, "ch6", DataType.INTEGER, "12");
        addRequest(requests, "ch7", DataType.LONG, "1234");

        WriteRequestList requestsMock = new WriteRequestList() {

            @Override
            public List<WriteRequest> getRequests() {
                return requests;
            }

            @Override
            public boolean isValid() {
                return true;
            }
        };

        doAnswer(invocation -> {
            List<ChannelRecord> records = invocation.getArgumentAt(0, List.class);
            assertEquals(7, records.size());

            assertChannelWrite(records, 0, "ch1", TypedValues.newDoubleValue(1.234));
            assertChannelWrite(records, 1, "ch2", TypedValues.newStringValue("val"));
            assertChannelWrite(records, 2, "ch3", TypedValues.newBooleanValue(true));
            assertChannelWrite(records, 3, "ch4", TypedValues.newByteArrayValue(new byte[] { 1, 2, 3 }));
            assertChannelWrite(records, 4, "ch5", TypedValues.newFloatValue(12.34f));
            assertChannelWrite(records, 5, "ch6", TypedValues.newIntegerValue(12));
            assertChannelWrite(records, 6, "ch7", TypedValues.newLongValue(1234L));

            return null;
        }).when(asset).write(anyObject());

        svc.write(pid, requestsMock);

        verify(asset, times(1)).write(anyObject());
    }

    private void assertChannelWrite(List<ChannelRecord> records, int idx, String channel, TypedValue value) {
        ChannelRecord channelRecord = records.get(idx);
        assertEquals(channel, channelRecord.getChannelName());
        assertEquals(value, channelRecord.getValue());

        channelRecord.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
    }

    private void addRequest(List<WriteRequest> requests, String name, DataType type, String value)
            throws NoSuchFieldException {

        WriteRequest request = new WriteRequest();
        TestUtil.setFieldValue(request, "name", name);
        TestUtil.setFieldValue(request, "type", type);
        TestUtil.setFieldValue(request, "value", value);
        requests.add(request);
    }

    @Test
    public void testWriteRequestListValidation() throws NoSuchFieldException {
        // test WriteRequestList.isValid()

        WriteRequestList req = new WriteRequestList();

        assertFalse(req.isValid());

        List<WriteRequest> channels = new ArrayList<>();
        TestUtil.setFieldValue(req, "channels", channels);

        assertTrue(req.isValid());

        WriteRequest request = new WriteRequest();
        channels.add(request);

        assertFalse(req.isValid());

        channels.clear();
        addRequest(channels, "ch1", DataType.BOOLEAN, "true");
        assertTrue(req.isValid());

        addRequest(channels, null, DataType.BOOLEAN, "true");
        assertFalse(req.isValid());
    }

}
