/*******************************************************************************
 * Copyright (c) 2022, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.data.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.core.db.H2DbMessageStoreImpl;
import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.message.store.StoredMessage;
import org.eclipse.kura.message.store.provider.MessageStore;
import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.util.jdbc.SQLFunction;
import org.junit.Before;
import org.junit.Test;

public class DbDataStoreStorageTest {

    private static final String TOPIC = "TEST-TOPIC";
    private static final int QOS0 = 0;
    private static final int QOS1 = 1;
    private static final int QOS2 = 2;
    private static final int PRIORITY_HIGH = 0;
    private static final int PRIORITY_MEDIUM = 1;
    private static final int PRIORITY_LOW = 2;
    private static final String TABLE_NAME = "test-table";
    private static final int H2_MAX_ID_VALUE = 2147483647;
    private byte[] payload;
    private Exception occurredException = null;

    private MessageStore dataStore;
    private int messageId;

    /*
     * Scenarios
     */

    @Test
    public void shouldStoreNullPayload() {
        givenNullPayload();
        givenDbDataStore(10000, 10000);

        whenStore(TOPIC, this.payload, QOS2, true, PRIORITY_LOW);

        thenNoExceptionsOccurred();
        thenStoredMessageIs(TOPIC, this.payload, QOS2, true, PRIORITY_LOW);
    }

    @Test
    public void shouldStoreSmallPayload() {
        givenSmallPayload();
        givenDbDataStore(10000, 10000);

        whenStore(TOPIC, this.payload, QOS2, true, PRIORITY_LOW);

        thenNoExceptionsOccurred();
        thenStoredMessageIs(TOPIC, this.payload, QOS2, true, PRIORITY_LOW);
    }

    @Test
    public void shouldStoreLargePayload() {
        givenLargePayload();
        givenDbDataStore(10000, 10000);

        whenStore(TOPIC, this.payload, QOS2, true, PRIORITY_LOW);

        thenNoExceptionsOccurred();
        thenStoredMessageIs(TOPIC, this.payload, QOS2, true, PRIORITY_LOW);
    }

    @Test
    public void idsAreResetIfOverflown() {
        givenSmallPayload();
        givenDbDataStore(H2_MAX_ID_VALUE, H2_MAX_ID_VALUE);

        whenOverflowingIds();

        thenNoExceptionsOccurred();
        thenLastIdIsCorrect();
    }

    @Test
    public void storeWithPriority0ShouldUpdateDbEvenIfDbIsFull() {
        givenSmallPayload();
        givenDbDataStore(H2_MAX_ID_VALUE, H2_MAX_ID_VALUE);

        whenStore(TOPIC, this.payload, QOS0, true, PRIORITY_HIGH);

        thenStoredMessageIs(TOPIC, this.payload, QOS0, true, PRIORITY_HIGH);
    }

    @Test
    public void storeWithPriority1ShouldUpdateDbEvenIfDbIsFull() {
        givenSmallPayload();
        givenDbDataStore(H2_MAX_ID_VALUE, H2_MAX_ID_VALUE);

        whenStore(TOPIC, this.payload, QOS1, true, PRIORITY_MEDIUM);

        thenStoredMessageIs(TOPIC, this.payload, QOS1, true, PRIORITY_MEDIUM);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenNullPayload() {
        this.payload = null;
    }

    private void givenSmallPayload() {
        this.payload = new byte[200];
        for (int i = 0; i < 199; i++) {
            this.payload[i] = 1;
        }
    }

    private void givenLargePayload() {
        this.payload = new byte[300];
        for (int i = 0; i < 300; i++) {
            this.payload[i] = 2;
        }
    }

    @SuppressWarnings("restriction")
    private void givenDbDataStore(int houseKeeperInterval, int purgeAge) {
        H2DbService h2Service = new MockH2DbService();

        try {
            this.dataStore = new H2DbMessageStoreImpl(new ConnectionProvider() {

                @Override
                public <T> T withConnection(SQLFunction<Connection, T> task) throws SQLException {
                    return h2Service.withConnection(task::call);
                }
            }, TABLE_NAME);
        } catch (KuraStoreException e) {
            this.occurredException = e;
        }
    }

    /*
     * When
     */

    private void whenStore(String topic, byte[] payload, int qos, boolean retain, int priority) {
        try {
            this.messageId = this.dataStore.store(topic, payload, qos, retain, priority);
        } catch (KuraStoreException e) {
            this.occurredException = e;
        }
    }

    private void whenOverflowingIds() {
        try {
            for (int i = 0; i < H2_MAX_ID_VALUE + 1; i++) {
                this.messageId = this.dataStore.store(TOPIC, this.payload, QOS2, true, PRIORITY_LOW);

                Class.forName("org.h2.Driver");
                Connection c = DriverManager.getConnection("jdbc:h2:mem:testdb;", "sa", "");

                // keep just one message at every moment
                for (int j = 0; j < i; j++) {
                    PreparedStatement stmt = c.prepareStatement("DELETE FROM ? WHERE ID=?;");
                    stmt.setString(1, TABLE_NAME);
                    stmt.setInt(2, i);
                    stmt.execute();
                }

                c.commit();

            }

            this.messageId = this.dataStore.store(TOPIC, this.payload, QOS2, true, PRIORITY_LOW);
            this.messageId = this.dataStore.store(TOPIC, this.payload, QOS2, true, PRIORITY_LOW);
            this.messageId = this.dataStore.store(TOPIC, this.payload, QOS2, true, PRIORITY_LOW);
        } catch (KuraStoreException | SQLException | ClassNotFoundException e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenNoExceptionsOccurred() {
        assertNull(this.occurredException);
    }

    private void thenStoredMessageIs(String topic, byte[] payload, int qos, boolean retain, int priority) {
        StoredMessage message;
        try {
            message = this.dataStore.get(this.messageId)
                    .orElseThrow(() -> new IllegalStateException("no message with the given id"));
        } catch (KuraStoreException e1) {
            fail("Unable to retrieve last message");
            throw new IllegalStateException();
        }

        assertEquals(topic, message.getTopic());
        assertTrue(Arrays.equals(payload, message.getPayload()));
        assertEquals(qos, message.getQos());
        assertEquals(retain, message.isRetain());
        assertEquals(priority, message.getPriority());

        // also inspect the database
        try {

            Class.forName("org.h2.Driver");
            Connection c = DriverManager.getConnection("jdbc:h2:mem:testdb;", "sa", "");
            PreparedStatement stmt = c.prepareStatement("SELECT * FROM ?;", Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, TABLE_NAME);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                if (rs.getInt("id") == message.getId()) {
                    String rowTopic = rs.getString("topic");
                    int rowQos = rs.getInt("qos");
                    boolean rowRetain = rs.getBoolean("retain");
                    byte[] smallPayload = rs.getBytes("smallPayload");
                    byte[] largePayload = rs.getBytes("largePayload");
                    int rowPriority = rs.getInt("priority");

                    assertEquals(topic, rowTopic);
                    assertEquals(qos, rowQos);
                    assertEquals(retain, rowRetain);
                    assertEquals(priority, rowPriority);
                    if (message.getPayload().length < 200) {
                        assertTrue(Arrays.equals(payload, smallPayload));
                        assertNull(largePayload);
                    } else {
                        assertTrue(Arrays.equals(payload, largePayload));
                        assertNull(smallPayload);
                    }
                }
            }

        } catch (SQLException | ClassNotFoundException e) {
            this.occurredException = e;
        }

    }

    private void thenStoreCapacityExceededException() {
        assertNotNull(this.occurredException);
    }

    private void thenLastIdIsCorrect() {
        assertEquals(3, this.messageId);
    }

    /*
     * Utilities
     */

    @Before
    public void cleanUp() {
        this.occurredException = null;
    }

    private final class MockH2DbService implements H2DbService {

        @Override
        public Connection getConnection() throws SQLException {
            try {
                Class.forName("org.h2.Driver");
                return DriverManager.getConnection("jdbc:h2:mem:testdb;", "sa", "");
            } catch (ClassNotFoundException e) {
                DbDataStoreStorageTest.this.occurredException = e;
            }
            return null;
        }

        @Override
        public void close(Connection conn) {
            // ignore

        }

        @Override
        public void rollback(Connection conn) {
            // ignore

        }

        @Override
        public void close(ResultSet... rss) {
            // ignore

        }

        @Override
        public void close(Statement... stmts) {
            // ignore

        }

        @Override
        public <T> T withConnection(ConnectionCallable<T> task) throws SQLException {
            return task.call(this.getConnection());
        }

    }

}
