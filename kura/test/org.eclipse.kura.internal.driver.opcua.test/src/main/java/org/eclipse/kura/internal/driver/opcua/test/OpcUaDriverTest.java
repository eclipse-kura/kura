/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.internal.driver.opcua.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.internal.driver.opcua.VariableType;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfigBuilder;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.application.CertificateManager;
import org.eclipse.milo.opcua.stack.core.application.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateValidator;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class OpcUaDriverTest {

    private static CountDownLatch dependencyLatch = new CountDownLatch(1);

    private static ConfigurationService cfgsvc;

    private static Driver driver;
    private Object driverLock = new Object();

    private static OpcUaServer server;

    @BeforeClass
    public static void setup() throws KuraException, UaException {
        startServer();

        try {
            boolean ok = dependencyLatch.await(10, TimeUnit.SECONDS);
            assertTrue(ok);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Dependencies should have been injected.");
        }
    }

    private static void startServer() throws UaException {
        CertificateManager certificateManager = new DefaultCertificateManager();
        CertificateValidator certificateValidator = new DefaultCertificateValidator(new File("/tmp"));
        List<String> bindAddresses = new ArrayList<>();
        bindAddresses.add("localhost");
        List<String> endpointAddresses = new ArrayList<>();
        endpointAddresses.add("localhost");
        OpcUaServerConfig config = new OpcUaServerConfigBuilder().setBindPort(12685).setApplicationUri("opcsvr")
                .setBindAddresses(bindAddresses).setEndpointAddresses(endpointAddresses).setServerName("opcsvr")
                .setApplicationName(LocalizedText.english("opcsvr")).setCertificateManager(certificateManager)
                .setCertificateValidator(certificateValidator)
                .setUserTokenPolicies(Arrays.asList(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS)).build();
        server = new OpcUaServer(config);
        server.getNamespaceManager().registerAndAdd(TestNamespace.NAMESPACE_URI, idx -> new TestNamespace(server, idx));
        server.startup();
    }

    @AfterClass
    public static void tearDown() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Before
    public void init() throws InterruptedException {
        if (driver == null) {
            synchronized (driverLock) {
                driverLock.wait(3000);
            }
        }
    }

    protected void activate() throws KuraException {
        if (cfgsvc != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("endpoint.ip", "localhost");
            props.put("endpoint.port", 12685);
            props.put("server.name", "opcsvr");

            props.put("security.policy", "http://opcfoundation.org/UA/SecurityPolicy#None");

            props.put("application.uri", "opcsvr");
            props.put("request.timeout", 1500);
            props.put("session.timeout", 2000);

            props.put("keystore.client.alias", "");
            props.put("keystore.password", "");
            props.put("keystore.server.alias", "");
            props.put("keystore.type", "");
            props.put("certificate.location", "");

            cfgsvc.createFactoryConfiguration("org.eclipse.kura.driver.opcua", "opcuadriver", props, false);
        }
    }

    @Test
    public void testSvcs() {
        assertNotNull(cfgsvc);
        assertNotNull(driver);
    }

    @Test
    public void testConnect() throws ConnectionException {
        // test connection to the server; it is OK if no exception is thrown

        driver.connect();
    }

    @Test
    public void testRead() throws ConnectionException {
        List<ChannelRecord> records = new ArrayList<>();

        ChannelRecord record = ChannelRecord.createReadRecord("ch1", DataType.INTEGER);
        Map<String, Object> channelConfig = new HashMap<>();
        channelConfig.put("node.namespace.index", "1");
        channelConfig.put("node.id.type", "NUMERIC");
        channelConfig.put("node.id", "1");
        record.setChannelConfig(channelConfig);
        records.add(record);

        driver.read(records);

        assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag()); // read fails - no server config
    }

    @Test
    public void testWrite() throws ConnectionException {
        List<ChannelRecord> records = new ArrayList<>();

        IntegerValue value = new IntegerValue(10);
        ChannelRecord record = ChannelRecord.createWriteRecord("ch1", value);
        Map<String, Object> channelConfig = new HashMap<>();
        channelConfig.put("node.namespace.index", "1");
        channelConfig.put("node.id.type", "NUMERIC");
        channelConfig.put("node.id", "1");
        record.setChannelConfig(channelConfig);
        records.add(record);

        driver.write(records);

        assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag()); // read fails - no server config
    }

    private void assertSuccess(ChannelRecord record) {
        assertEquals(new ChannelStatus(ChannelFlag.SUCCESS), record.getChannelStatus());
    }

    private ChannelRecord createReadRecord(String nodeId, DataType valueType) {
        return createReadRecord(nodeId, "STRING", valueType);
    }

    private ChannelRecord createReadRecord(String nodeId, String nodIdType, DataType valueType) {
        ChannelRecord record = ChannelRecord.createReadRecord("test", valueType);
        Map<String, Object> channelConfig = new HashMap<>();
        channelConfig.put("node.namespace.index", "2");
        channelConfig.put("node.id.type", nodIdType);
        channelConfig.put("opcua.type", VariableType.DEFINED_BY_JAVA_TYPE.name());
        channelConfig.put("node.id", nodeId);
        channelConfig.put("attribute", "Value");
        record.setChannelConfig(channelConfig);
        return record;
    }

    private ChannelRecord createWriteRecord(String nodeId, TypedValue<?> value, VariableType opcuaType) {
        return createWriteRecord(nodeId, "STRING", value, opcuaType);
    }

    private ChannelRecord createWriteRecord(String nodeId, String type, TypedValue<?> value, VariableType opcuaType) {
        ChannelRecord record = ChannelRecord.createWriteRecord("test", value);
        Map<String, Object> channelConfig = new HashMap<>();
        channelConfig.put("node.namespace.index", "2");
        channelConfig.put("node.id.type", type);
        channelConfig.put("opcua.type", opcuaType.name());
        channelConfig.put("node.id", nodeId);
        channelConfig.put("attribute", "Value");
        record.setChannelConfig(channelConfig);
        return record;
    }

    private TypedValue<?> adapt(Number number, DataType dataType) {
        if (dataType == DataType.INTEGER) {
            return new IntegerValue(number.intValue());
        } else if (dataType == DataType.LONG) {
            return new LongValue(number.longValue());
        } else if (dataType == DataType.FLOAT) {
            return new FloatValue(number.floatValue());
        } else if (dataType == DataType.DOUBLE) {
            return new DoubleValue(number.doubleValue());
        }
        throw new IllegalArgumentException();
    }

    private void testRead(String nodeId, TypedValue<?> expectedValue) throws ConnectionException {
        final ChannelRecord readRecord = createReadRecord(nodeId, expectedValue.getType());
        driver.read(Arrays.asList(readRecord));
        assertSuccess(readRecord);
        assertEquals(expectedValue, readRecord.getValue());
    }

    private void testRead(String nodeId, String nodeIdType, TypedValue<?> expectedValue) throws ConnectionException {
        final ChannelRecord readRecord = createReadRecord(nodeId, nodeIdType, expectedValue.getType());
        driver.read(Arrays.asList(readRecord));
        assertSuccess(readRecord);
        assertEquals(expectedValue, readRecord.getValue());
    }

    private void testReadWrite(String nodeId, TypedValue<?> value, VariableType opcuaType) throws ConnectionException {
        final ChannelRecord writeRecord = createWriteRecord(nodeId, value, opcuaType);
        driver.write(Arrays.asList(writeRecord));
        assertSuccess(writeRecord);
        testRead(nodeId, value);
    }

    private void testReadWrite(String nodeId, String type, TypedValue<?> value, VariableType opcuaType)
            throws ConnectionException {
        final ChannelRecord writeRecord = createWriteRecord(nodeId, type, value, opcuaType);
        driver.write(Arrays.asList(writeRecord));
        assertSuccess(writeRecord);
        testRead(nodeId, type, value);
    }

    private void testRead(String nodeId, Number number, EnumSet<DataType> types) throws ConnectionException {
        for (DataType type : types) {
            testRead(nodeId, adapt(number, type));
        }
        testRead(nodeId, new StringValue(number.toString()));
    }

    private void testReadWrite(String nodeId, Number number, EnumSet<DataType> types, VariableType opcuaType)
            throws ConnectionException {
        for (DataType type : types) {
            testReadWrite(nodeId, adapt(number, type), opcuaType);
        }
        testReadWrite(nodeId, new StringValue(number.toString()), opcuaType);
    }

    private void testReadWrite(String nodeId, EnumSet<DataType> types, VariableType opcuaType, Number... values)
            throws ConnectionException {
        for (Number number : values) {
            testReadWrite(nodeId, number, types, opcuaType);
        }
    }

    @Test
    public void testByte() throws ConnectionException {
        final EnumSet<DataType> applicableTypes = EnumSet.of(DataType.INTEGER, DataType.LONG, DataType.FLOAT,
                DataType.DOUBLE);

        final String nodeId = "Byte";
        final VariableType opcuaType = VariableType.BYTE;

        testRead(nodeId, 0, applicableTypes);
        testReadWrite(nodeId, applicableTypes, opcuaType, 0, 10, 20, 255);
    }

    @Test
    public void testUInt16() throws ConnectionException {
        final EnumSet<DataType> applicableTypes = EnumSet.of(DataType.INTEGER, DataType.LONG, DataType.FLOAT,
                DataType.DOUBLE);

        final String nodeId = "UInt16";
        final VariableType opcuaType = VariableType.UINT16;

        testRead(nodeId, 0, applicableTypes);
        testReadWrite(nodeId, applicableTypes, opcuaType, 0, 10, 20, ((int) Short.MAX_VALUE) * 2 + 1);
    }

    @Test
    public void testUInt32() throws ConnectionException {
        final String nodeId = "UInt32";
        final VariableType opcuaType = VariableType.UINT32;

        testRead(nodeId, 0, EnumSet.of(DataType.INTEGER, DataType.LONG, DataType.FLOAT, DataType.DOUBLE));
        testReadWrite(nodeId, EnumSet.of(DataType.INTEGER, DataType.LONG, DataType.FLOAT, DataType.DOUBLE), opcuaType,
                0, 10, 20, ((int) Short.MAX_VALUE) * 2 + 1);
        testReadWrite(nodeId, EnumSet.of(DataType.LONG, DataType.DOUBLE), opcuaType,
                ((long) Integer.MAX_VALUE) * 2 + 1);
    }

    @Test
    public void testUInt64() throws ConnectionException {
        final String nodeId = "UInt64";
        final VariableType opcuaType = VariableType.UINT64;

        testRead(nodeId, 0, EnumSet.of(DataType.INTEGER, DataType.LONG, DataType.FLOAT, DataType.DOUBLE));
        testReadWrite(nodeId, EnumSet.of(DataType.INTEGER, DataType.LONG, DataType.FLOAT, DataType.DOUBLE), opcuaType,
                0, 10, 20, ((int) Short.MAX_VALUE) * 2 + 1);
        testReadWrite(nodeId, EnumSet.of(DataType.LONG, DataType.DOUBLE), opcuaType,
                ((long) Integer.MAX_VALUE) * 2 + 1);
        testReadWrite(nodeId, EnumSet.of(DataType.LONG, DataType.DOUBLE), opcuaType, Long.MAX_VALUE);
    }

    @Test
    public void testSByte() throws ConnectionException {
        final EnumSet<DataType> applicableTypes = EnumSet.of(DataType.INTEGER, DataType.LONG, DataType.FLOAT,
                DataType.DOUBLE);

        final String nodeId = "SByte";
        final VariableType opcuaType = VariableType.SBYTE;

        testRead(nodeId, 0, applicableTypes);
        testReadWrite(nodeId, applicableTypes, opcuaType, 0, 10, 20, 127, -10, -20, -127);
    }

    @Test
    public void testInt16() throws ConnectionException {
        final EnumSet<DataType> applicableTypes = EnumSet.of(DataType.INTEGER, DataType.LONG, DataType.FLOAT,
                DataType.DOUBLE);

        final String nodeId = "Int16";
        final VariableType opcuaType = VariableType.INT16;

        testRead(nodeId, 0, applicableTypes);
        testReadWrite(nodeId, applicableTypes, opcuaType, 0, 10, 20, Short.MAX_VALUE, -10, -20, -Short.MAX_VALUE);
    }

    @Test
    public void testInt32() throws ConnectionException {
        final EnumSet<DataType> applicableTypes = EnumSet.of(DataType.INTEGER, DataType.LONG, DataType.FLOAT,
                DataType.DOUBLE);
        final String nodeId = "Int32";
        final VariableType opcuaType = VariableType.INT32;

        testRead(nodeId, 0, applicableTypes);
        testReadWrite(nodeId, applicableTypes, opcuaType, 0, 10, 20, Short.MAX_VALUE, Integer.MAX_VALUE, -10, -20,
                -Short.MAX_VALUE, -Integer.MAX_VALUE);
    }

    @Test
    public void testInt64() throws ConnectionException {
        final String nodeId = "Int64";
        final VariableType opcuaType = VariableType.INT64;

        testRead(nodeId, 0, EnumSet.of(DataType.INTEGER, DataType.LONG, DataType.FLOAT, DataType.DOUBLE));
        testReadWrite(nodeId, EnumSet.of(DataType.INTEGER, DataType.LONG, DataType.FLOAT, DataType.DOUBLE), opcuaType,
                0, 10, 20, Short.MAX_VALUE, Integer.MAX_VALUE, -10, -20, -Short.MAX_VALUE, -Integer.MAX_VALUE);
        testReadWrite(nodeId, EnumSet.of(DataType.LONG, DataType.DOUBLE), opcuaType, Long.MAX_VALUE, -Long.MAX_VALUE);
    }

    @Test
    public void testFloat() throws ConnectionException {
        final String nodeId = "Float";
        final VariableType opcuaType = VariableType.FLOAT;

        testRead(nodeId, 0.0f, EnumSet.of(DataType.FLOAT));
        testReadWrite(nodeId, EnumSet.of(DataType.FLOAT), opcuaType, 0.0f, Float.MIN_VALUE, 2e1f, 2e2f, 2e3f,
                Float.MAX_VALUE, -Float.MIN_VALUE, -2e1f, -2e2f, -2e3f, -Float.MAX_VALUE);
    }

    @Test
    public void testDouble() throws ConnectionException {
        final String nodeId = "Double";
        final VariableType opcuaType = VariableType.DOUBLE;

        testRead(nodeId, 0.0d, EnumSet.of(DataType.DOUBLE));
        testReadWrite(nodeId, EnumSet.of(DataType.DOUBLE), opcuaType, 0.0d, Double.MIN_VALUE, 2e1d, 2e2d, 2e3d,
                Double.MAX_VALUE, -Double.MIN_VALUE, -2e1d, -2e2d, -2e3d, -Double.MAX_VALUE);
    }

    @Test
    public void testBoolean() throws ConnectionException {
        final String nodeId = "Boolean";
        final VariableType opcuaType = VariableType.BOOLEAN;

        testRead(nodeId, TypedValues.newBooleanValue(false));
        testReadWrite(nodeId, TypedValues.newBooleanValue(true), opcuaType);
    }

    @Test
    public void testString() throws ConnectionException {
        final String nodeId = "String";
        final VariableType opcuaType = VariableType.STRING;

        testRead(nodeId, TypedValues.newStringValue("string value"));
        testReadWrite(nodeId, TypedValues.newStringValue("other string value"), opcuaType);
    }

    @Test
    public void testByteString() throws ConnectionException {
        final String nodeId = "ByteString";
        final VariableType opcuaType = VariableType.BYTE_STRING;

        testRead(nodeId, TypedValues.newByteArrayValue(new byte[] { 0x01, 0x02, 0x03, 0x04 }));
        testReadWrite(nodeId, TypedValues.newByteArrayValue(new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                0x09, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff }), opcuaType);
    }

    @Test
    public void testByteArray() throws ConnectionException {
        final String nodeId = "ByteArray";
        final VariableType opcuaType = VariableType.BYTE_ARRAY;

        testRead(nodeId, TypedValues.newByteArrayValue(new byte[] { 0x00, 0x00, 0x00, 0x00 }));
        testReadWrite(nodeId, TypedValues.newByteArrayValue(new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                0x09, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff }), opcuaType);
    }

    @Test
    public void testSByteArray() throws ConnectionException {
        final String nodeId = "SByteArray";
        final VariableType opcuaType = VariableType.SBYTE_ARRAY;

        testRead(nodeId, TypedValues.newByteArrayValue(new byte[] { 0x00, 0x00, 0x00, 0x00 }));
        testReadWrite(nodeId, TypedValues.newByteArrayValue(new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                0x09, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff }), opcuaType);
    }

    @Test
    public void testLargeIndex() throws ConnectionException {
        final String nodeId = Long.toString(0xffffffffL);
        final VariableType opcuaType = VariableType.INT32;

        testRead(nodeId, "NUMERIC", TypedValues.newLongValue(1234));
        testReadWrite(nodeId, "NUMERIC", TypedValues.newLongValue(12345), opcuaType);
    }

    public void bindCfgSvc(ConfigurationService cfgSvc) {
        OpcUaDriverTest.cfgsvc = cfgSvc;
        dependencyLatch.countDown();
    }

    public void unbindCfgSvc(ConfigurationService cfgSvc) {
        OpcUaDriverTest.cfgsvc = null;
    }

    protected void bindDriver(Driver driver) {
        OpcUaDriverTest.driver = driver;

        synchronized (driverLock) {
            driverLock.notifyAll();
        }
    }

    protected void unbindDriver(Driver driver) {
        OpcUaDriverTest.driver = null;
    }

}
