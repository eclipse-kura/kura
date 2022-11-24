/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.db.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.internal.wire.db.store.DbWireRecordStore;
import org.eclipse.kura.internal.wire.db.store.DbWireRecordStoreOptions;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

public class DbWireRecordStoreTest {

    private ComponentContext ctx;
    private Connection dbConnection;
    private H2DbService dbServiceMock;
    private DbWireRecordStore storeWireComp;
    private Map<String, Object> properties = new HashMap<>();
    private WireEnvelope wireEnvelope;

    private static final Map<String, TypedValue<?>> SIMPLE_DATA = new HashMap<String, TypedValue<?>>() {

        private static final long serialVersionUID = 1L;

        {
            put("key", new StringValue("value"));
        }
    };

    private static final Map<String, TypedValue<?>> ALL_TYPES_DATA = new HashMap<String, TypedValue<?>>() {

        private static final long serialVersionUID = 2L;

        {
            put("k_str", new StringValue("v_str"));
            put("k_int", new IntegerValue(11));
            put("k_blob", new ByteArrayValue(new byte[] { 0x00, 0x01, 0x02 }));
            put("k_bool", new BooleanValue(true));
            put("k_double", new DoubleValue((double) 2.0));
            put("k_long", new LongValue((long) 0.3));
            put("k_float", new FloatValue((float) 13));
        }
    };

    private static final String TEST_TABLE_NAME = "WR_DATA";

    /*
     * Scenarios
     */

    @Test
    public void shouldStoreSimpleWireEnvelope() {
        givenProperties(TEST_TABLE_NAME, 10, 0);
        givenActivatedStoreComponent();
        givenWireEnvelope(SIMPLE_DATA);

        whenWireEnvelopesReceived(1);

        thenOneTableExists(TEST_TABLE_NAME);
        thenTableEntriesCountIs(TEST_TABLE_NAME, 1);
        thenTableContainsData(TEST_TABLE_NAME, SIMPLE_DATA);
    }

    @Test
    public void shouldStoreAllTypesWireEnvelope() {
        givenProperties(TEST_TABLE_NAME, 10, 0);
        givenActivatedStoreComponent();
        givenWireEnvelope(ALL_TYPES_DATA);

        whenWireEnvelopesReceived(1);

        thenOneTableExists(TEST_TABLE_NAME);
        thenTableEntriesCountIs(TEST_TABLE_NAME, 7);
        thenTableContainsData(TEST_TABLE_NAME, ALL_TYPES_DATA);
    }

    @Test
    public void truncateShouldKeepNoRecords() {
        givenProperties(TEST_TABLE_NAME, 10, 0);
        givenActivatedStoreComponent();
        givenWireEnvelope(SIMPLE_DATA);

        whenWireEnvelopesReceived(11);

        thenOneTableExists(TEST_TABLE_NAME);
        thenTableEntriesCountIs(TEST_TABLE_NAME, 1);
    }

    @Test
    public void truncateShouldNotTrigger() {
        givenProperties(TEST_TABLE_NAME, 10, 0);
        givenActivatedStoreComponent();
        givenWireEnvelope(SIMPLE_DATA);

        whenWireEnvelopesReceived(10);

        thenOneTableExists(TEST_TABLE_NAME);
        thenTableEntriesCountIs(TEST_TABLE_NAME, 10);
    }

    @Test
    public void truncateShouldKeepMaxSizeRecords() {
        givenProperties(TEST_TABLE_NAME, 10, 11);
        givenActivatedStoreComponent();
        givenWireEnvelope(SIMPLE_DATA);

        whenWireEnvelopesReceived(20);

        thenOneTableExists(TEST_TABLE_NAME);
        thenTableEntriesCountIs(TEST_TABLE_NAME, 10);
    }

    @Test
    public void truncateShouldKeepMaxSizeRecords2() {
        givenProperties(TEST_TABLE_NAME, 10, 10);
        givenActivatedStoreComponent();
        givenWireEnvelope(SIMPLE_DATA);

        whenWireEnvelopesReceived(20);

        thenOneTableExists(TEST_TABLE_NAME);
        thenTableEntriesCountIs(TEST_TABLE_NAME, 10);
    }

    @Test
    public void truncateShouldRespectNRecordsToKeep() {
        givenProperties(TEST_TABLE_NAME, 10, 5);
        givenActivatedStoreComponent();
        givenWireEnvelope(SIMPLE_DATA);

        whenWireEnvelopesReceived(11);

        thenOneTableExists(TEST_TABLE_NAME);
        thenTableEntriesCountIs(TEST_TABLE_NAME, 5);
    }

    @Test
    public void limitCaseTest() {
        givenProperties(TEST_TABLE_NAME, 1, 1);
        givenActivatedStoreComponent();
        givenWireEnvelope(SIMPLE_DATA);

        whenWireEnvelopesReceived(10);

        thenOneTableExists(TEST_TABLE_NAME);
        thenTableEntriesCountIs(TEST_TABLE_NAME, 1);
    }

    @Test
    public void limitCaseTest2() {
        givenProperties(TEST_TABLE_NAME, 1, 0);
        givenActivatedStoreComponent();
        givenWireEnvelope(SIMPLE_DATA);

        whenWireEnvelopesReceived(10);

        thenOneTableExists(TEST_TABLE_NAME);
        thenTableEntriesCountIs(TEST_TABLE_NAME, 1);
    }

    @Test
    public void updateMaxTableSize() {
        givenProperties(TEST_TABLE_NAME, 10, 0);
        givenActivatedStoreComponent();
        givenWireEnvelope(SIMPLE_DATA);
        givenWireEnvelopesReceived(10);
        givenProperties(TEST_TABLE_NAME, 5, 0);

        whenUpdate();
        whenWireEnvelopesReceived(1);

        thenOneTableExists(TEST_TABLE_NAME);
        thenTableEntriesCountIs(TEST_TABLE_NAME, 1);
    }

    @Test
    public void updateNonExistentTableName() {
        givenProperties(TEST_TABLE_NAME, 10, 0);
        givenActivatedStoreComponent();
        givenWireEnvelope(SIMPLE_DATA);
        givenWireEnvelopesReceived(10);
        givenProperties("NEW_TABLE", 5, 0);

        whenUpdate();

        thenTableEntriesCountIs(TEST_TABLE_NAME, 10);
        thenTableEntriesCountIs("NEW_TABLE", 0);
    }

    @Test
    public void truncateShouldRemoveOldest() {
        givenProperties(TEST_TABLE_NAME, 10, 0);
        givenActivatedStoreComponent();
        givenWireEnvelope(SIMPLE_DATA);

        whenWireEnvelopesReceived(11);

        thenOneTableExists(TEST_TABLE_NAME);
        thenTableEntriesCountIs(TEST_TABLE_NAME, 1);
        thenLastInsertedIdIs(TEST_TABLE_NAME, 11);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenProperties(String tableName, int maxTableSize, int nRecordsToKeep) {
        this.properties.put(DbWireRecordStoreOptions.TABLE_NAME, tableName);
        this.properties.put(DbWireRecordStoreOptions.MAXIMUM_TABLE_SIZE, maxTableSize);
        this.properties.put(DbWireRecordStoreOptions.CLEANUP_RECORDS_KEEP, nRecordsToKeep);
    }

    private void givenActivatedStoreComponent() {
        this.storeWireComp = new DbWireRecordStore();

        WireHelperService whsMock = mock(WireHelperService.class);
        WireSupport wireSupportMock = mock(WireSupport.class);
        when(whsMock.newWireSupport(this.storeWireComp, null)).thenReturn(wireSupportMock);

        this.storeWireComp.bindWireHelperService(whsMock);

        this.ctx = mock(ComponentContext.class);
        this.storeWireComp.activate(this.ctx, this.properties);
        this.storeWireComp.bindDbService(dbServiceMock);
    }

    private void givenWireEnvelope(Map<String, TypedValue<?>> data) {

        String emitterPid = "emitter-example";
        List<WireRecord> wireRecords = new ArrayList<WireRecord>();
        Map<String, TypedValue<?>> recordProps;
        WireRecord record;

        for (Entry<String, TypedValue<?>> entry : data.entrySet()) {
            recordProps = new HashMap<String, TypedValue<?>>();
            recordProps.put(entry.getKey(), entry.getValue());
            record = new WireRecord(recordProps);
            wireRecords.add(record);
        }

        this.wireEnvelope = new WireEnvelope(emitterPid, wireRecords);
    }

    private void givenWireEnvelopesReceived(int nWireEnvelopes) {
        for (int i = 0; i < nWireEnvelopes; i++) {
            this.storeWireComp.onWireReceive(this.wireEnvelope);
        }
    }

    /*
     * When
     */

    private void whenWireEnvelopesReceived(int nWireEnvelopes) {
        for (int i = 0; i < nWireEnvelopes; i++) {
            this.storeWireComp.onWireReceive(this.wireEnvelope);
        }
    }

    private void whenColumnTypeChanged(String key, DataType newType, String newValue) {
        Map<String, TypedValue<?>> recordProps = new HashMap<>();
        recordProps.put("blobkey", TypedValues.parseTypedValue(newType, newValue));

        WireRecord record = new WireRecord(recordProps);
        List<WireRecord> wireRecords = new ArrayList<WireRecord>();
        wireRecords.add(record);
        this.wireEnvelope = new WireEnvelope("pid", wireRecords);

        this.storeWireComp.onWireReceive(this.wireEnvelope);
    }

    private void whenUpdate() {
        this.storeWireComp.updated(this.properties);
    }

    /*
     * Then
     */

    private void thenOneTableExists(String tableName) {
        try {
            DatabaseMetaData metaData = this.dbConnection.getMetaData();
            ResultSet tables = metaData.getTables(null, null, tableName, null);
            tables.first();
            String dbTableName = tables.getString("TABLE_NAME");

            assertTrue("Only one table was expected", tables.isLast());
            assertEquals(tableName, dbTableName);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void thenTableEntriesCountIs(String tableName, int expectedNumberOfEntries) {
        try {
            ResultSet resultSet = this.dbConnection.prepareStatement("SELECT count(*) FROM " + tableName)
                    .executeQuery();
            resultSet.next();
            int count = resultSet.getInt(1);
            assertEquals(expectedNumberOfEntries, count);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void thenTableContainsData(String tableName, Map<String, TypedValue<?>> data) {
        try {
            ResultSet resultSet = this.dbConnection.prepareStatement("SELECT * FROM " + tableName).executeQuery();

            // each line contains the inserted values
            for (Entry<String, TypedValue<?>> entry : data.entrySet()) {
                resultSet.next();

                long tim = resultSet.getLong(1);
                assertTrue(tim <= System.currentTimeMillis());

                Object dbValue = resultSet.getObject(entry.getKey());
                Object entryValue = entry.getValue().getValue();

                if (dbValue instanceof Blob) {
                    // comparing byte arrays is always tricky
                    Blob blob = (Blob) dbValue;
                    byte[] expected = (byte[]) entryValue;
                    assertTrue(Arrays.equals(expected, blob.getBytes(1, expected.length)));
                } else if (entryValue instanceof Float) {
                    assertEquals(entryValue.toString(), dbValue.toString()); // toString for avoiding conversions
                } else {
                    assertEquals(entryValue, dbValue);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void thenLastInsertedIdIs(String tableName, int lastId) {
        try {
            ResultSet resultSet = this.dbConnection.prepareStatement("SELECT ID FROM " + tableName).executeQuery();

            int id = 0;
            while (resultSet.next()) {
                id = resultSet.getInt("ID");
            }

            assertEquals(lastId, id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Utilities
     */

    @BeforeClass
    public static void setup() {
        try {
            DriverManager.registerDriver(new org.h2.Driver());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void clean() {
        try {
            this.dbConnection = DriverManager.getConnection("jdbc:h2:mem:kuradb", "SA", "");
            this.dbServiceMock = mock(H2DbService.class);

            when(this.dbServiceMock.withConnection(any())).thenAnswer(invocation -> {
                return invocation.getArgument(0, H2DbService.ConnectionCallable.class).call(this.dbConnection);
            });

            when(this.dbServiceMock.getConnection()).thenReturn(this.dbConnection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        this.properties.clear();
        this.properties.put(DbWireRecordStoreOptions.TABLE_NAME, TEST_TABLE_NAME);
    }

    @After
    public void closeDbConnection() {
        this.storeWireComp.deactivate(this.ctx);
        this.storeWireComp.unbindDbService(this.dbServiceMock);

        try {
            this.dbConnection.prepareStatement("SHUTDOWN").execute();
            Thread.sleep(500); // wait until db has been closed
            this.dbConnection.close();
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
