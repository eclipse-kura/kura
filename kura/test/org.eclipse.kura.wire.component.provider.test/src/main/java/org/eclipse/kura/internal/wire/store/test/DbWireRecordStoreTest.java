/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.wire.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.internal.wire.store.DbWireRecordStore;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireRecord;
import org.junit.BeforeClass;
import org.junit.Test;


public class DbWireRecordStoreTest {

    private static final int maxSize = 1200;
    private static final int cleanupSize = 1100;

    private static CountDownLatch dependencyLatch = new CountDownLatch(2);

    private static DbWireRecordStore dbstore; // DbWireRecordStore
    private static ConfigurationService cfgsvc;
    private static DbService dbsvc;

    private String tableName = "STORE_TEST";

    @BeforeClass
    public static void setup() throws KuraException {
        try {
            dependencyLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void activate() throws KuraException {
        if (cfgsvc != null) {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("table.name", tableName);
            props.put("maximum.table.size", maxSize);
            props.put("cleanup.records.keep", cleanupSize);

            cfgsvc.createFactoryConfiguration("org.eclipse.kura.wire.DbWireRecordStore", "foo", props, false);
        }
    }

    @Test
    public void testSvcs() {
        assertNotNull(cfgsvc);
        assertNotNull(dbsvc);
        assertNotNull(dbstore);
    }

    @Test
    public void testReceive() throws SQLException {
        Connection connection = dbsvc.getConnection();

        ResultSet resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        int startCount = resultSet.getInt(1);

        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<WireRecord>();
        Map<String, TypedValue<?>> recordProps = new HashMap<String, TypedValue<?>>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);
        WireEnvelope wireEvelope = new WireEnvelope(emitterPid, wireRecords);

        // store one record
        dbstore.onWireReceive(wireEvelope);

        // check the results
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

        assertTrue(tim <= System.currentTimeMillis());
        assertEquals("val", strval);

        // add couple more
        dbstore.onWireReceive(wireEvelope);

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
        dbstore.onWireReceive(wireEvelope); // adds 3, now

        resultSet = connection.prepareStatement("SELECT count(*) FROM " + tableName).executeQuery();
        resultSet.next();
        count = resultSet.getInt(1);
        assertEquals("Unexpected number of records", startCount + 5, count);
    }

    @Test
    public void testCleanupSequence() throws SQLException {
        // create DB, insert a few wire records, check they are actually in there and clean the DB
        Connection connection = dbsvc.getConnection();

        connection.prepareStatement("TRUNCATE TABLE " + tableName).execute();

        String emitterPid = "emitter";
        List<WireRecord> wireRecords = new ArrayList<WireRecord>();
        Map<String, TypedValue<?>> recordProps = new HashMap<String, TypedValue<?>>();
        TypedValue<?> val = new StringValue("val");
        recordProps.put("key", val);
        WireRecord record = new WireRecord(recordProps);
        wireRecords.add(record);
        WireEnvelope wireEvelope = new WireEnvelope(emitterPid, wireRecords);

        // store a few records
        for (int i = 0; i < maxSize; i++) {
            dbstore.onWireReceive(wireEvelope);

            // prevent key violations - timestamp only has a 1 ms resolution
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // OK
            }
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
        assertEquals("Unexpected number of records", maxSize, count);

        // store a few records
        for (int i = 0; i < 5; i++) {
            dbstore.onWireReceive(wireEvelope);

            // prevent key violations - timestamp only has a 1 ms resolution
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // OK
            }
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
        assertEquals("Unexpected number of records", cleanupSize + 5, count);
    }

    public void bindDbstore(DbWireRecordStore dbstore) {
        System.out.println(dbstore);
        DbWireRecordStoreTest.dbstore = dbstore;
    }

    public void unbindDbstore(DbWireRecordStore dbstore) {
        DbWireRecordStoreTest.dbstore = null;
    }

    public void bindCfgSvc(ConfigurationService cfgSvc) {
        System.out.println(cfgSvc);
        DbWireRecordStoreTest.cfgsvc = cfgSvc;
        dependencyLatch.countDown();
    }

    public void unbindCfgSvc(ConfigurationService cfgSvc) {
        DbWireRecordStoreTest.cfgsvc = null;
    }

    public void bindDbSvc(DbService dbSvc) {
        System.out.println(dbSvc);
        DbWireRecordStoreTest.dbsvc = dbSvc;
        dependencyLatch.countDown();
    }

    public void unbindDbSvc(DbService dbSvc) {
        DbWireRecordStoreTest.cfgsvc = null;
    }

}
