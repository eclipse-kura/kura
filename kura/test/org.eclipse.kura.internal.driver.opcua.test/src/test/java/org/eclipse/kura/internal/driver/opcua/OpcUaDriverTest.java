/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.driver.opcua;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.AddressSpace;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.IdType;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.junit.Test;
import org.mockito.Mockito;

public class OpcUaDriverTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTypedValue() throws Throwable {
        // test getTypedValue

        DataType expectedValueType = DataType.BOOLEAN;
        Object containedValue = true;

        TypedValue<?> value = DataTypeMapper.map(containedValue, expectedValueType);

        assertEquals(expectedValueType, value.getType());
        assertEquals(containedValue, value.getValue());

        expectedValueType = DataType.INTEGER;
        containedValue = 10;

        value = DataTypeMapper.map(containedValue, expectedValueType);

        assertEquals(expectedValueType, value.getType());
        assertEquals(containedValue, value.getValue());

        expectedValueType = DataType.LONG;
        containedValue = 123456789123456L;

        value = DataTypeMapper.map(containedValue, expectedValueType);

        assertEquals(expectedValueType, value.getType());
        assertEquals(containedValue, value.getValue());

        expectedValueType = DataType.FLOAT;
        containedValue = 12.3f;

        value = DataTypeMapper.map(containedValue, expectedValueType);

        assertEquals(expectedValueType, value.getType());
        assertEquals(containedValue, value.getValue());

        expectedValueType = DataType.DOUBLE;
        containedValue = 123.4;

        value = DataTypeMapper.map(containedValue, expectedValueType);

        assertEquals(expectedValueType, value.getType());
        assertEquals(containedValue, value.getValue());

        expectedValueType = DataType.STRING;
        containedValue = "test";

        value = DataTypeMapper.map(containedValue, expectedValueType);

        assertEquals(expectedValueType, value.getType());
        assertEquals(containedValue, value.getValue());

        expectedValueType = DataType.BYTE_ARRAY;
        containedValue = "test".getBytes("utf8");

        value = DataTypeMapper.map(containedValue, expectedValueType);

        assertEquals(expectedValueType, value.getType());

        // FIXME check if this serialization is really OK in case of byte[] on the input
        // assertArrayEquals((byte[]) containedValue, (byte[]) value.get().getValue());

        // also test a few exception cases
        expectedValueType = null;
        containedValue = "test";

        try {
            DataTypeMapper.map(containedValue, expectedValueType);
            fail("Expected exception");
        } catch (Exception e) {
            // ok
        }

        expectedValueType = DataType.INTEGER;
        containedValue = "test";

        try {
            DataTypeMapper.map(containedValue, expectedValueType);
            fail("Expected exception");
        } catch (Exception e) {
            // ok
        }

    }

    @Test
    public void testReadNoNode() throws ConnectionException, NoSuchFieldException {
        // test read with a missing node exception

        OpcUaDriver svc = new OpcUaDriver();
        Map<String, Object> properties = new HashMap<>();
        properties.put("request.timeout", 1);
        CryptoService csMock = mock(CryptoService.class);
        OpcUaOptions options = new OpcUaOptions(properties, csMock);

        // needed for runSafe()
        TestUtil.setFieldValue(svc, "options", options);

        OpcUaClient clientMock = mock(OpcUaClient.class);

        ConnectionManager manager = new ConnectionManager(clientMock, options, (a, b) -> {
        }, new ListenerRegistrations());

        TestUtil.setFieldValue(svc, "connectionManager", Optional.of(manager));

        AddressSpace asMock = mock(AddressSpace.class);
        when(clientMock.getAddressSpace()).thenReturn(asMock);

        doAnswer(invocation -> {
            NodeId nodeId = invocation.getArgumentAt(0, NodeId.class);

            assertEquals(1, ((UInteger) nodeId.getIdentifier()).intValue());
            assertEquals(1, nodeId.getNamespaceIndex().intValue());
            assertEquals(IdType.Numeric, nodeId.getType());

            return null; // cause exception later on
        }).when(asMock).createVariableNode(anyObject());

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("ch1", DataType.BOOLEAN);
        Map<String, Object> channelConfig = new HashMap<>();
        channelConfig.put("node.namespace.index", "1");
        channelConfig.put("node.id.type", "NUMERIC");
        channelConfig.put("opcua.type", "DEFINED_BY_JAVA_TYPE");
        channelConfig.put("node.id", "1");
        record.setChannelConfig(channelConfig);
        records.add(record);

        svc.read(records);

        assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag());
    }

    @Test
    public void testReadInvalidReadResult() throws ConnectionException, NoSuchFieldException, InterruptedException,
            ExecutionException, TimeoutException {
        // test read where the successful read result renders empty typed value due to format exception

        OpcUaDriver svc = new OpcUaDriver();

        prepareForSuccessfulRead(svc, "123.4"); // expect an integer below, but get a double here

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("ch1", DataType.INTEGER);
        Map<String, Object> channelConfig = new HashMap<>();
        channelConfig.put("node.namespace.index", "1");
        channelConfig.put("node.id.type", "NUMERIC");
        channelConfig.put("opcua.type", "DEFINED_BY_JAVA_TYPE");
        channelConfig.put("node.id", "1");
        record.setChannelConfig(channelConfig);
        records.add(record);

        svc.read(records);

        assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag());
    }

    @Test
    public void testRead() throws ConnectionException, NoSuchFieldException, InterruptedException, ExecutionException,
            TimeoutException {
        // test read

        OpcUaDriver svc = new OpcUaDriver();

        prepareForSuccessfulRead(svc, "123");

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("ch1", DataType.INTEGER);
        Map<String, Object> channelConfig = new HashMap<>();
        channelConfig.put("node.namespace.index", "1");
        channelConfig.put("node.id.type", "NUMERIC");
        channelConfig.put("opcua.type", "DEFINED_BY_JAVA_TYPE");
        channelConfig.put("node.id", "1");
        channelConfig.put("attribute", "Value");
        record.setChannelConfig(channelConfig);
        records.add(record);

        svc.read(records);

        assertEquals(ChannelFlag.SUCCESS, record.getChannelStatus().getChannelFlag());
        assertEquals(123, record.getValue().getValue());
    }

    protected void prepareForSuccessfulRead(OpcUaDriver svc, String val)
            throws NoSuchFieldException, InterruptedException, ExecutionException, TimeoutException {

        Map<String, Object> properties = new HashMap<>();
        properties.put("request.timeout", 1);
        CryptoService csMock = mock(CryptoService.class);
        OpcUaOptions options = new OpcUaOptions(properties, csMock);

        // needed for runSafe()
        TestUtil.setFieldValue(svc, "options", options);

        OpcUaClient clientMock = mock(OpcUaClient.class);

        ConnectionManager manager = new ConnectionManager(clientMock, options, (a, b) -> {
        }, new ListenerRegistrations());

        TestUtil.setFieldValue(svc, "connectionManager", Optional.of(manager));

        Variant variant = new Variant(val);
        DataValue value = new DataValue(variant, StatusCode.GOOD);

        ReadResponse response = mock(ReadResponse.class);
        when(response.getResults()).thenReturn(new DataValue[] { value });

        CompletableFuture<ReadResponse> futureValue = mock(CompletableFuture.class);
        when(futureValue.get(1000, TimeUnit.MILLISECONDS)).thenReturn(response);

        when(clientMock.read(Mockito.eq(0.0), Mockito.eq(TimestampsToReturn.Both), anyObject()))
                .thenReturn(futureValue);
    }

    @Test
    public void testPrepareRead() throws NoSuchFieldException, ConnectionException, KuraException, InterruptedException,
            ExecutionException, TimeoutException {
        // test prepareRead

        OpcUaDriver svc = new OpcUaDriver();

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("ch1", DataType.INTEGER);
        Map<String, Object> channelConfig = new HashMap<>();
        channelConfig.put("node.namespace.index", "1");
        channelConfig.put("opcua.type", "DEFINED_BY_JAVA_TYPE");
        channelConfig.put("node.id.type", "NUMERIC");
        channelConfig.put("node.id", "1");
        channelConfig.put("attribute", "Value");
        record.setChannelConfig(channelConfig);
        records.add(record);

        PreparedRead preparedRead = svc.prepareRead(records);

        assertEquals(1, preparedRead.getChannelRecords().size());
        assertEquals(record, preparedRead.getChannelRecords().get(0));

        List<?> requestInfos = (List<?>) TestUtil.getFieldValue(preparedRead, "requests");
        assertEquals(1, requestInfos.size());

        // now really execute the read, but everything needs to be prepared beforehand
        prepareForSuccessfulRead(svc, "123");

        List<ChannelRecord> result = preparedRead.execute();

        assertEquals(1, result.size());
        assertEquals(ChannelFlag.SUCCESS, record.getChannelStatus().getChannelFlag());
        assertEquals(123, record.getValue().getValue());
    }

}
