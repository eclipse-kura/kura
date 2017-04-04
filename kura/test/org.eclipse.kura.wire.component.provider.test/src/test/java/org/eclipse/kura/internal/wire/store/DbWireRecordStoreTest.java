/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.wire.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

import org.eclipse.kura.db.DbService;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.ByteValue;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.ShortValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

public class DbWireRecordStoreTest {

    @BeforeClass
    public static void setup() {
        try {
            DriverManager.registerDriver(new org.hsqldb.jdbcDriver());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:proto;hsqldb.lock_file=false", "SA", "");
        return connection;
    }

    @Test
    public void testSequence() throws SQLException {
        // create DB, insert a few wire records, check they are actually in there
        DbWireRecordStore store = new DbWireRecordStore();

        Connection connection = getConnection();

        DbService dbServiceMock = mock(DbService.class);
        when(dbServiceMock.getConnection()).thenReturn(connection);

        WireHelperService whsMock = mock(WireHelperService.class);
        WireSupport wireSupportMock = mock(WireSupport.class);
        when(whsMock.newWireSupport(store)).thenReturn(wireSupportMock);

        store.bindDbService(dbServiceMock);
        store.bindWireHelperService(whsMock);

        ComponentContext ctx = mock(ComponentContext.class);
        Map<String, Object> props = new HashMap<String, Object>();
        String tableName = "STORE_TEST";
        props.put("table.name", tableName);

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

        // store one record
        store.onWireReceive(wireEvelope);

        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tables = metaData.getTables(null, null, tableName, null);
        tables.first();
        String dbTableName = tables.getString("TABLE_NAME");

        // only one table was created with the correct name
        assertTrue("Only one table was expected", tables.isLast());
        assertEquals(tableName, dbTableName);

        ResultSet resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        assertEquals("Only 1 record was expected", 1, count);

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
        val = new ByteValue((byte) 0x12);
        recordProps.put("bytkey", val);
        val = new DoubleValue(1.234);
        recordProps.put("dblkey", val);
        val = new IntegerValue(1234);
        recordProps.put("intkey", val);
        val = new LongValue(1234L);
        recordProps.put("longkey", val);
        val = new ShortValue((short) 123);
        recordProps.put("shortkey", val);
        record = new WireRecord(recordProps);
        wireRecords.add(record);
        store.onWireReceive(wireEvelope); // adds 3, now

        resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        count = resultSet.getInt(1);
        assertEquals("Unexpected number of records", 5, count);

        // deinit
        store.deactivate(null);
        connection.prepareStatement("SHUTDOWN").execute();
    }

    @Test
    public void testCleanupSequence() throws SQLException {
        // create DB, insert a few wire records, check they are actually in there and clean the DB
        DbWireRecordStore store = new DbWireRecordStore();

        Connection connection = getConnection();

        DbService dbServiceMock = mock(DbService.class);
        when(dbServiceMock.getConnection()).thenReturn(connection);

        WireHelperService whsMock = mock(WireHelperService.class);
        WireSupport wireSupportMock = mock(WireSupport.class);
        when(whsMock.newWireSupport(store)).thenReturn(wireSupportMock);

        store.bindDbService(dbServiceMock);
        store.bindWireHelperService(whsMock);

        ComponentContext ctx = mock(ComponentContext.class);
        Map<String, Object> props = new HashMap<String, Object>();
        String tableName = "STORE_TEST";
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
            // wait for the executor to do its duty and DB operation to finish
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                // OK
            }
        }

        ResultSet resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        assertEquals("Unexpected number of records", 3, count);

        // store a few records
        for (int i = 0; i < 5; i++) {
            store.onWireReceive(wireEvelope);
            // wait for the executor to do its duty and DB operation to finish
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                // OK
            }
        }

        resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        count = resultSet.getInt(1);
        assertEquals("Unexpected number of records", 4, count);

        // deinit
        store.deactivate(null);
        connection.prepareStatement("SHUTDOWN").execute();
    }

}
