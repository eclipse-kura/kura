/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.wire.h2db.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

public class H2DbWireRecordStoreTest {

    @BeforeClass
    public static void setup() {
        try {
            DriverManager.registerDriver(new org.h2.Driver());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:kuradb", "SA", "");
        return connection;
    }

    private H2DbService createMockH2DbService(final Connection connection) throws SQLException {
        final H2DbService dbServiceMock = mock(H2DbService.class);
        when(dbServiceMock.withConnection(anyObject())).thenAnswer(invocation -> {
            return invocation.getArgumentAt(0, H2DbService.ConnectionCallable.class).call(connection);
        });
        when(dbServiceMock.getConnection()).thenReturn(connection);
        return dbServiceMock;
    }

    @Test
    public void testSequence() throws SQLException {
        // create DB, insert a few wire records, check they are actually in there, trigger column type update,
        // update configuration

        Connection connection = getConnection();

        H2DbService dbServiceMock = createMockH2DbService(connection);

        AtomicInteger resets = new AtomicInteger(0);
        H2DbWireRecordStore store = new H2DbWireRecordStore() {

            @Override
            protected void restartDbServiceTracker() {
                bindDbService(dbServiceMock);

                resets.set(resets.get() + 1);
            }
        };

        WireHelperService whsMock = mock(WireHelperService.class);
        WireSupport wireSupportMock = mock(WireSupport.class);
        when(whsMock.newWireSupport(store, null)).thenReturn(wireSupportMock);

        store.bindWireHelperService(whsMock);

        ComponentContext ctx = mock(ComponentContext.class);
        Map<String, Object> props = new HashMap<String, Object>();
        String tableName = "H2_STORE_TEST";
        props.put("table.name", tableName);

        // init
        store.activate(ctx, props);

        assertEquals(1, resets.get());

        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<WireRecord>();
        Map<String, TypedValue<?>> recordProps = new HashMap<String, TypedValue<?>>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);
        WireEnvelope wireEvelope = new WireEnvelope(emitterPid, wireRecords);

        ResultSet resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        int startCount = resultSet.getInt(1);

        // store one record
        store.onWireReceive(wireEvelope);

        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tables = metaData.getTables(null, null, tableName, null);
        tables.first();
        String dbTableName = tables.getString("TABLE_NAME");

        // only one table was created with the correct name
        assertTrue("Only one table was expected", tables.isLast());
        assertEquals(tableName, dbTableName);

        resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        assertEquals("Unexpected number of records in the database.", startCount + 1, count);

        resultSet = connection.prepareStatement("SELECT * FROM " + tableName).executeQuery();
        resultSet.next();
        long tim = resultSet.getLong(1);
        String strval = resultSet.getString("key");

        assertTrue(resultSet.isLast());
        assertTrue(tim <= System.currentTimeMillis());
        assertEquals("val", strval);

        // add couple more
        store.onWireReceive(wireEvelope);

        recordProps = new HashMap<String, TypedValue<?>>();
        val = new ByteArrayValue("val".getBytes());
        recordProps.put("blobkey", val);
        record = new WireRecord(recordProps);
        wireRecords.add(record);
        recordProps = new HashMap<String, TypedValue<?>>();
        val = new BooleanValue(true);
        recordProps.put("boolkey", val);
        val = new DoubleValue(1.234);
        recordProps.put("dblkey", val);
        val = new IntegerValue(1234);
        recordProps.put("intkey", val);
        val = new LongValue(1234L);
        recordProps.put("longkey", val);
        val = new FloatValue(123.2f);
        recordProps.put("floatkey", val);
        record = new WireRecord(recordProps);
        wireRecords.add(record);
        store.onWireReceive(wireEvelope); // adds 3, now

        resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        count = resultSet.getInt(1);
        assertEquals("Unexpected number of records", 5, count);

        // change one of the column types
        recordProps = new HashMap<String, TypedValue<?>>();
        val = new FloatValue(1234.5f);
        recordProps.put("blobkey", val);
        record = new WireRecord(recordProps);
        wireRecords = new ArrayList<WireRecord>();
        wireRecords.add(record);
        wireEvelope = new WireEnvelope(emitterPid, wireRecords);
        store.onWireReceive(wireEvelope); // adds 1 more

        resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        count = resultSet.getInt(1);
        assertEquals("Unexpected number of records", 6, count);

        // update the configuration
        store.updated(props);

        assertEquals(1, resets.get()); // reset didn't happen

        // update the configuration with a new DB service pid
        props.put("db.service.pid", "newPid"); // trigger tracker reset

        store.updated(props);

        assertEquals(2, resets.get()); // reset happened

        // deinit
        store.deactivate(null);
        connection.prepareStatement("SHUTDOWN").execute();
    }

    @Test
    public void testCleanupSequence() throws SQLException {
        // create DB, insert a few wire records, check they are actually in there and clean the DB
        Connection connection = getConnection();

        H2DbService dbServiceMock = createMockH2DbService(connection);

        H2DbWireRecordStore store = new H2DbWireRecordStore() {

            @Override
            protected void restartDbServiceTracker() {
                bindDbService(dbServiceMock);
            }
        };

        WireHelperService whsMock = mock(WireHelperService.class);
        WireSupport wireSupportMock = mock(WireSupport.class);
        when(whsMock.newWireSupport(store, null)).thenReturn(wireSupportMock);

        store.bindWireHelperService(whsMock);

        ComponentContext ctx = mock(ComponentContext.class);
        Map<String, Object> props = new HashMap<String, Object>();
        String tableName = "H2_STORE_TEST";
        props.put("table.name", tableName);
        props.put("cleanup.records.keep", 3);
        props.put("maximum.table.size", 5);

        // init
        store.activate(ctx, props);

        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<WireRecord>();
        Map<String, TypedValue<?>> recordProps = new HashMap<String, TypedValue<?>>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);
        WireEnvelope wireEvelope = new WireEnvelope(emitterPid, wireRecords);

        // store a few records
        for (int i = 0; i < 3; i++) {
            store.onWireReceive(wireEvelope);
        }
        // wait for the executor to do its duty and DB operation to finish
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // OK
        }

        ResultSet resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        assertEquals("Unexpected number of records", 3, count);

        // store a few records
        for (int i = 0; i < 5; i++) {
            store.onWireReceive(wireEvelope);
        }
        // wait for the executor to do its duty and DB operation to finish
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // OK
        }

        resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        count = resultSet.getInt(1);
        assertEquals("Unexpected number of records", 4, count);

        // deinit
        store.deactivate(null);
        connection.prepareStatement("SHUTDOWN").execute();
    }

    @Test
    public void testCleanupSequenceLarge() throws SQLException {
        // create DB, insert many wire records, check they are actually in there and clean the DB

        Connection connection = getConnection();

        H2DbService dbServiceMock = createMockH2DbService(connection);

        H2DbWireRecordStore store = new H2DbWireRecordStore() {

            @Override
            protected void restartDbServiceTracker() {
                bindDbService(dbServiceMock);
            }
        };

        WireHelperService whsMock = mock(WireHelperService.class);
        WireSupport wireSupportMock = mock(WireSupport.class);
        when(whsMock.newWireSupport(store, null)).thenReturn(wireSupportMock);

        store.bindWireHelperService(whsMock);

        ComponentContext ctx = mock(ComponentContext.class);
        Map<String, Object> props = new HashMap<String, Object>();
        String tableName = "H2_STORE_TEST";
        props.put("table.name", tableName);
        props.put("cleanup.records.keep", 1100);
        props.put("maximum.table.size", 1200);

        // init
        store.activate(ctx, props);

        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<WireRecord>();
        Map<String, TypedValue<?>> recordProps = new HashMap<String, TypedValue<?>>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);
        WireEnvelope wireEvelope = new WireEnvelope(emitterPid, wireRecords);

        // store a few records
        for (int i = 0; i < 1100; i++) {
            store.onWireReceive(wireEvelope);
        }
        // wait for the executor to do its duty and DB operation to finish
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // OK
        }

        ResultSet resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        assertEquals("Unexpected number of records", 1100, count);

        // store a few records
        for (int i = 0; i < 150; i++) {
            store.onWireReceive(wireEvelope);
        }
        // wait for the executor to do its duty and DB operation to finish
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // OK
        }

        resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        count = resultSet.getInt(1);
        assertEquals("Unexpected number of records", 1150, count);

        // deinit
        store.deactivate(null);
        connection.prepareStatement("SHUTDOWN").execute();
    }

    @Test
    public void testTruncateSequence() throws SQLException {
        // create DB, insert many wire records truncating the DB in the process, check that enough are in there after

        Connection connection = getConnection();

        H2DbService dbServiceMock = createMockH2DbService(connection);

        H2DbWireRecordStore store = new H2DbWireRecordStore() {

            @Override
            protected void restartDbServiceTracker() {
                bindDbService(dbServiceMock);
            }
        };

        WireHelperService whsMock = mock(WireHelperService.class);
        WireSupport wireSupportMock = mock(WireSupport.class);
        when(whsMock.newWireSupport(store, null)).thenReturn(wireSupportMock);

        store.bindWireHelperService(whsMock);

        ComponentContext ctx = mock(ComponentContext.class);
        Map<String, Object> props = new HashMap<String, Object>();
        String tableName = "H2_STORE_TEST";
        props.put("table.name", tableName);
        props.put("cleanup.records.keep", 0);
        props.put("maximum.table.size", 1100);

        // init
        store.activate(ctx, props);

        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<WireRecord>();
        Map<String, TypedValue<?>> recordProps = new HashMap<String, TypedValue<?>>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);
        WireEnvelope wireEvelope = new WireEnvelope(emitterPid, wireRecords);

        // store a few records
        for (int i = 0; i < 1000; i++) {
            store.onWireReceive(wireEvelope);
        }
        // wait for the executor to do its duty and DB operation to finish
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // OK
        }

        ResultSet resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        assertEquals("Unexpected number of records", 1000, count);

        // store a few records
        for (int i = 0; i < 500; i++) {
            store.onWireReceive(wireEvelope);
        }
        // wait for the executor to do its duty and DB operation to finish
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // OK
        }

        resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        count = resultSet.getInt(1);
        assertEquals("Unexpected number of records", 400, count);

        // deinit
        store.deactivate(null);
        connection.prepareStatement("SHUTDOWN").execute();
    }

}
