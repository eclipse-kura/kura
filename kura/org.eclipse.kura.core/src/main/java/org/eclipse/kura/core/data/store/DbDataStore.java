/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.data.store;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraStoreCapacityReachedException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.core.data.DataMessage;
import org.eclipse.kura.core.data.DataStore;
import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the DataStore which stores messages into an embedded H2 instance.
 */
public class DbDataStore implements DataStore {

    private static final Logger logger = LoggerFactory.getLogger(DbDataStore.class);

    private static final String DATA_SERVICE_REPAIR_ENABLED_PROPNAME = "db.store.repair.enabled";

    private H2DbService dbService;
    private final Calendar utcCalendar;
    private ScheduledExecutorService houseKeeperExecutor;
    private ScheduledFuture<?> houseKeeperTask;
    private int capacity;

    private final String table;

    private final String sqlCreateTable;
    private final String sqlCreateIndex;
    private final String sqlMessageCount;
    private final String sqlResetId;
    private final String sqlStore;
    private final String sqlGetMessage;
    private final String sqlGetNextMessage;
    private final String sqlSetPublished;
    private final String sqlSetPublished2;
    private final String sqlSetConfirmed;
    private final String sqlAllUnpublishedMessages;
    private final String sqlAllInFlightMessages;
    private final String sqlAllDroppedInFlightMessages;
    private final String sqlUnpublishAllInFlightMessages;
    private final String sqlDropAllInFlightMessages;
    private final String sqlDeleteDroppedMessages;
    private final String sqlDeleteConfirmedMessages;
    private final String sqlDeletePublishedMessages;
    private final String sqlDuplicateCount;
    private final String sqlDropPrimaryKey;
    private final String sqlDeleteDuplicates;
    private final String sqlCreatePrimaryKey;

    // package level constructor to be invoked only by the factory
    public DbDataStore(String table) {
        // do not make this static as it may not be thread safe
        this.utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        this.table = table;

        this.sqlCreateTable = "CREATE TABLE IF NOT EXISTS " + this.table
                + " (id INTEGER IDENTITY PRIMARY KEY, topic VARCHAR(32767 CHAR), qos INTEGER, retain BOOLEAN, createdOn TIMESTAMP, publishedOn TIMESTAMP, publishedMessageId INTEGER, confirmedOn TIMESTAMP, payload VARBINARY(16777216), priority INTEGER, sessionId VARCHAR(32767 CHAR), droppedOn TIMESTAMP);";
        this.sqlCreateIndex = "CREATE INDEX IF NOT EXISTS " + this.table + "_nextMsg ON " + this.table
                + " (publishedOn ASC NULLS FIRST, priority ASC, createdOn ASC, qos);";
        this.sqlMessageCount = "SELECT COUNT(*) FROM " + this.table + ";";
        this.sqlResetId = "ALTER TABLE " + this.table + " ALTER COLUMN id RESTART WITH 1;";
        this.sqlStore = "INSERT INTO " + this.table
                + " (topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, payload, priority, sessionId, droppedOn) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        this.sqlGetMessage = "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, payload, priority, sessionId, droppedOn FROM "
                + this.table + " WHERE id = ?";
        this.sqlGetNextMessage = "SELECT a.id, a.topic, a.qos, a.retain, a.createdOn, a.publishedOn, a.publishedMessageId, a.confirmedOn, a.payload, a.priority, a.sessionId, a.droppedOn FROM "
                + this.table + " AS a JOIN (SELECT id, publishedOn FROM " + this.table
                + " ORDER BY publishedOn ASC NULLS FIRST, priority ASC, createdOn ASC LIMIT 1) AS b WHERE a.id = b.id AND b.publishedOn IS NULL;";
        this.sqlSetPublished = "UPDATE " + this.table
                + " SET publishedOn = ?, publishedMessageId = ?, sessionId = ? WHERE id = ?;";
        this.sqlSetPublished2 = "UPDATE " + this.table + " SET publishedOn = ? WHERE id = ?;";
        this.sqlSetConfirmed = "UPDATE " + this.table + " SET confirmedOn = ? WHERE id = ?;";
        this.sqlAllUnpublishedMessages = "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, priority, sessionId, droppedOn FROM "
                + this.table + " WHERE publishedOn IS NULL ORDER BY priority ASC, createdOn ASC;";
        this.sqlAllInFlightMessages = "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, priority, sessionId, droppedOn FROM "
                + this.table
                + " WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL AND droppedOn IS NULL ORDER BY priority ASC, createdOn ASC";
        this.sqlAllDroppedInFlightMessages = "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, priority, sessionId, droppedOn FROM "
                + this.table + " WHERE droppedOn IS NOT NULL ORDER BY priority ASC, createdOn ASC;";
        this.sqlUnpublishAllInFlightMessages = "UPDATE " + this.table
                + " SET publishedOn = NULL WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;";
        this.sqlDropAllInFlightMessages = "UPDATE " + this.table
                + " SET droppedOn = ? WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;";
        this.sqlDeleteDroppedMessages = "DELETE FROM " + this.table
                + " WHERE droppedOn <= DATEADD('ss', -?, ?) AND droppedOn IS NOT NULL;";
        this.sqlDeleteConfirmedMessages = "DELETE FROM " + this.table
                + " WHERE confirmedOn <= DATEADD('ss', -?, ?) AND confirmedOn IS NOT NULL;";
        this.sqlDeletePublishedMessages = "DELETE FROM " + this.table
                + " WHERE qos = 0 AND publishedOn <= DATEADD('ss', -?, ?) AND publishedOn IS NOT NULL;";
        this.sqlDuplicateCount = "SELECT count(*) FROM (SELECT id, COUNT(id) FROM " + this.table
                + " GROUP BY id HAVING (COUNT(id) > 1)) dups;";
        this.sqlDropPrimaryKey = "ALTER TABLE " + this.table + " DROP PRIMARY KEY;";
        this.sqlDeleteDuplicates = "DELETE FROM " + this.table + " WHERE id IN (SELECT id FROM " + this.table
                + " GROUP BY id HAVING COUNT(*) > 1);";
        this.sqlCreatePrimaryKey = "ALTER TABLE " + this.table + " ADD PRIMARY KEY (id);";
    }

    // ----------------------------------------------------------
    //
    // Start/Stop, ServiceId
    //
    // ----------------------------------------------------------

    @Override
    public synchronized void start(H2DbService dbService, int houseKeeperInterval, int purgeAge, int capacity)
            throws KuraStoreException {
        this.dbService = dbService;

        this.houseKeeperExecutor = Executors.newSingleThreadScheduledExecutor();

        //
        // Set up the schema tables required by the DataStore
        update(houseKeeperInterval, purgeAge, capacity);
    }

    @Override
    public synchronized void stop() {
        logger.info("Canceling the Housekeeper Task...");
        if (this.houseKeeperTask != null) {
            this.houseKeeperTask.cancel(true);
        }
        this.houseKeeperExecutor.shutdownNow();
        dbService = null;
    }

    private boolean isRepairEnabled() {
        final BundleContext context = FrameworkUtil.getBundle(DbDataStore.class).getBundleContext();
        ServiceReference<SystemService> reference = context.getServiceReference(SystemService.class);
        SystemService systemService = context.getService(reference);
        if (systemService == null) {
            return false;
        }
        try {
            final String isRepairEnabled = systemService.getProperties()
                    .getProperty(DATA_SERVICE_REPAIR_ENABLED_PROPNAME);
            return "true".equalsIgnoreCase(isRepairEnabled);
        } finally {
            context.ungetService(reference);
        }
    }

    @Override
    public synchronized void update(int houseKeeperInterval, int purgeAge, int capacity) {
        this.capacity = capacity;

        try {
            if (this.houseKeeperTask != null) {
                this.houseKeeperTask.cancel(true);
            }

            execute(this.sqlCreateTable);

            execute(this.sqlCreateIndex);

            createIndex(this.table + "_PUBLISHEDON", this.table, "(PUBLISHEDON DESC)");
            createIndex(this.table + "_CONFIRMEDON", this.table, "(CONFIRMEDON DESC)");
            createIndex(this.table + "_DROPPEDON", this.table, "(DROPPEDON DESC)");

            // Start the Housekeeper task
            this.houseKeeperTask = this.houseKeeperExecutor.scheduleWithFixedDelay(
                    new HouseKeeperTask(this, purgeAge, isRepairEnabled()), 1,    // start in one second
                    houseKeeperInterval,   // repeat every retryInterval until we stopped.
                    TimeUnit.SECONDS);
        } catch (KuraStoreException e) {
            logger.warn("got exception while creating tables", e);
        }

    }

    // ----------------------------------------------------------
    //
    // Message APIs
    //
    // ----------------------------------------------------------

    private synchronized int getMessageCount() throws KuraStoreException {
        int count = -1;
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(this.sqlMessageCount);
                ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (Exception e) {
            throw new KuraStoreException(e, "Cannot get message count");
        }

        return count;
    }

    private synchronized void resetIdentityGenerator() throws KuraStoreException {
        execute(this.sqlResetId);
    }

    @Override
    public synchronized DataMessage store(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException {
        if (dbService == null) {
            throw new KuraStoreException("DbService instance not attached");
        }
        if (topic == null || topic.trim().length() == 0) {
            throw new IllegalArgumentException("topic");
        }

        // Priority 0 are used for life-cycle messages like birth and death certificates.
        // Priority 1 are used for remove management by Cloudlet applications.
        // For those messages, bypass the max message count check of the DB cache;
        // we want to publish those message even if the db is full, so allow their storage.
        if (priority != 0 && priority != 1) {
            int count = getMessageCount();
            logger.debug("Store message count: {}", count);
            if (count >= this.capacity) {
                logger.error("Store capacity exceeded");
                throw new KuraStoreCapacityReachedException("Store capacity exceeded");
            }
        }

        DataMessage message = null;
        try {
            message = storeInternal(topic, payload, qos, retain, priority);
        } catch (KuraStoreException e) {
            // Try to reset the sequence generator and store the message again.
            // FIXME: it doesn't work but if we restart Kura the sequence generator restarts from 0!
            Throwable cause = e.getCause();
            if (cause instanceof SQLException) {
                SQLException sqle = (SQLException) cause;
                int errorCode = sqle.getErrorCode();
                if (errorCode == 22003) {
                    logger.warn("Identity generator limit exceeded. Resetting it...");
                    resetIdentityGenerator();
                    message = storeInternal(topic, payload, qos, retain, priority);
                } else {
                    throw e;
                }
            } else {
                throw e;
            }
        }

        return message;
    }

    private synchronized DataMessage storeInternal(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException {
        if (topic == null || topic.trim().length() == 0) {
            throw new IllegalArgumentException("topic");
        }

        Timestamp now = new Timestamp(new Date().getTime());

        int messageId = -1;
        Connection conn = null;
        try {

            conn = getConnection();

            // store message
            try (PreparedStatement pstmt = conn.prepareStatement(this.sqlStore)) {
                pstmt.setString(1, topic);              // topic
                pstmt.setInt(2, qos);               // qos
                pstmt.setBoolean(3, retain);                // retain
                pstmt.setTimestamp(4, now, this.utcCalendar); // createdOn
                pstmt.setTimestamp(5, null);                // publishedOn
                pstmt.setInt(6, -1);                 // publishedMessageId
                pstmt.setTimestamp(7, null);                // confirmedOn
                pstmt.setBytes(8, payload);         // payload
                pstmt.setInt(9, priority);            // priority
                pstmt.setString(10, null);               // sessionId
                pstmt.setTimestamp(11, null);               // droppedOn
                pstmt.execute();
            }

            // retrieve message id
            try (PreparedStatement cstmt = conn.prepareStatement("CALL IDENTITY();");
                    ResultSet rs = cstmt.executeQuery()) {
                if (rs != null && rs.next()) {
                    messageId = rs.getInt(1);
                }
            }

            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            logger.error("SQL error code: {}", e.getErrorCode());
            throw new KuraStoreException(e, "Cannot store message");
        } finally {
            close(conn);
        }
        return get(messageId);
    }

    @Override
    public synchronized DataMessage get(int msgId) throws KuraStoreException {
        DataMessage msg = null;
        ResultSet rs = null;
        Connection conn = null;
        try {

            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(this.sqlGetMessage)) {
                stmt.setInt(1, msgId);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    msg = buildDataMessage(rs);
                }
            }
        } catch (Exception e) {
            throw new KuraStoreException(e, "Cannot get message by ID: " + msgId);
        } finally {
            close(rs);
            close(conn);
        }
        return msg;
    }

    @Override
    public synchronized DataMessage getNextMessage() throws KuraStoreException {
        DataMessage msg = null;
        Connection conn = null;
        try {

            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(this.sqlGetNextMessage);
                    ResultSet rs = stmt.executeQuery()) {
                if (rs != null && rs.next()) {
                    msg = buildDataMessage(rs);
                }
            }
        } catch (Exception e) {
            throw new KuraStoreException(e, "Cannot get message next message");
        } finally {
            close(conn);
        }
        return msg;
    }

    @Override
    public synchronized void published(int msgId, int publishedMsgId, String sessionId) throws KuraStoreException {
        Timestamp now = new Timestamp(new Date().getTime());

        Connection conn = null;
        PreparedStatement stmt = null;
        try {

            conn = getConnection();
            stmt = conn.prepareStatement(this.sqlSetPublished);
            stmt.setTimestamp(1, now, this.utcCalendar); // timestamp
            stmt.setInt(2, publishedMsgId);
            stmt.setString(3, sessionId);
            stmt.setInt(4, msgId);

            stmt.execute();
            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new KuraStoreException(e, "Cannot update timestamp");
        } finally {
            close(stmt);
            close(conn);
        }
    }

    @Override
    public synchronized void published(int msgId) throws KuraStoreException {
        updateTimestamp(this.sqlSetPublished2, msgId);
    }

    @Override
    public synchronized void confirmed(int msgId) throws KuraStoreException {
        updateTimestamp(this.sqlSetConfirmed, msgId);
    }

    @Override
    public synchronized List<DataMessage> allUnpublishedMessagesNoPayload() throws KuraStoreException {
        // Order by priority, createdOn
        return listMessages(this.sqlAllUnpublishedMessages);
    }

    @Override
    public synchronized List<DataMessage> allInFlightMessagesNoPayload() throws KuraStoreException {
        // Order by priority, createdOn
        return listMessages(this.sqlAllInFlightMessages);
    }

    @Override
    public synchronized List<DataMessage> allDroppedInFlightMessagesNoPayload() throws KuraStoreException {
        // Order by priority, createdOn
        return listMessages(this.sqlAllDroppedInFlightMessages);
    }

    @Override
    public synchronized void unpublishAllInFlighMessages() throws KuraStoreException {
        execute(this.sqlUnpublishAllInFlightMessages);
    }

    @Override
    public synchronized void dropAllInFlightMessages() throws KuraStoreException {
        updateTimestamp(this.sqlDropAllInFlightMessages);
    }

    @Override
    public synchronized void deleteStaleMessages(int purgeAge) throws KuraStoreException {
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        // Delete dropped messages (published with QoS > 0)
        executeDeleteMessagesQuery(this.sqlDeleteDroppedMessages, now, purgeAge);

        // Delete stale confirmed messages (published with QoS > 0)
        executeDeleteMessagesQuery(this.sqlDeleteConfirmedMessages, now, purgeAge);

        // Delete stale published messages with QoS == 0
        executeDeleteMessagesQuery(this.sqlDeletePublishedMessages, now, purgeAge);
    }

    @Override
    public synchronized void repair() throws KuraStoreException {
        // See:
        // https://sourceforge.net/p/hsqldb/discussion/73674/thread/a08046eb/#7960
        Connection conn = null;
        int count = -1;
        try {

            conn = getConnection();
            // Get the count of IDs for which duplicates exist
            try (PreparedStatement pstmt = conn.prepareStatement(this.sqlDuplicateCount);
                    ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
            if (count <= 0) {
                return;
            }

            logger.error(
                    "Found messages with duplicate ID. Count of IDs for which duplicates exist: {}. Attempting to repair...",
                    count);

            try (Statement stmt = conn.createStatement()) {

                stmt.execute(this.sqlDropPrimaryKey);
                logger.info("Primary key dropped");

                stmt.execute(this.sqlDeleteDuplicates);
                logger.info("Duplicate messages deleted");

                stmt.execute(this.sqlCreatePrimaryKey);
                logger.info("Primary key created");

            }
            conn.commit();

            execute("CHECKPOINT");
            logger.info("Checkpoint");

            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new KuraStoreException(e, "Cannot repair database");
        } finally {
            close(conn);
        }
    }

    // ------------------------------------------------------------------
    //
    // Private Methods
    //
    // ------------------------------------------------------------------

    private synchronized void updateTimestamp(String sql, Integer... msgIds) throws KuraStoreException {
        Timestamp now = new Timestamp(new Date().getTime());

        Connection conn = null;
        PreparedStatement stmt = null;
        try {

            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setTimestamp(1, now, this.utcCalendar); // timestamp

            for (int i = 0; i < msgIds.length; i++) {
                stmt.setInt(2 + i, msgIds[i]);  // messageId
            }
            stmt.execute();
            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new KuraStoreException(e, "Cannot update timestamp");
        } finally {
            close(stmt);
            close(conn);
        }
    }

    private synchronized List<DataMessage> listMessages(String sql, Integer... params) throws KuraStoreException {
        List<DataMessage> msgs = new ArrayList<>();

        ResultSet rs = null;
        Connection conn = null;
        try {

            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        stmt.setInt(2 + i, params[i]);  // timeInterval
                    }
                }

                rs = stmt.executeQuery();
                msgs = buildDataMessagesNoPayload(rs);
            }
        } catch (Exception e) {
            throw new KuraStoreException(e, "Cannot list messages");
        } finally {
            close(rs);
            close(conn);
        }

        return msgs;
    }

    private synchronized void execute(String sql, Integer... params) throws KuraStoreException {
        if (dbService == null) {
            throw new KuraStoreException("DbService instance not attached");
        }
        Connection conn = null;
        PreparedStatement stmt = null;
        try {

            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setInt(1 + i, params[i]);
            }
            stmt.execute();
            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new KuraStoreException(e, "Cannot execute query");
        } finally {
            close(stmt);
            close(conn);
        }
    }

    private synchronized void executeDeleteMessagesQuery(String sql, Timestamp timestamp, int purgeAge)
            throws KuraStoreException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {

            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, purgeAge);
            stmt.setTimestamp(2, timestamp, this.utcCalendar);

            stmt.execute();
            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new KuraStoreException(e, "Cannot execute query");
        } finally {
            close(stmt);
            close(conn);
        }
    }

    private void createIndex(String indexname, String table, String order) throws KuraStoreException {
        execute("CREATE INDEX IF NOT EXISTS " + indexname + " ON " + table + " " + order + ";");
        logger.debug("Index {} created, order is {}", indexname, order);
    }

    // ------------------------------------------------------------------
    //
    // Private Methods: Connection Management
    //
    // ------------------------------------------------------------------

    private List<DataMessage> buildDataMessagesNoPayload(ResultSet rs) throws SQLException, IOException {
        List<DataMessage> messages = new ArrayList<DataMessage>();
        while (rs.next()) {
            messages.add(buildDataMessageNoPayload(rs));
        }
        return messages;
    }

    private DataMessage buildDataMessageNoPayload(ResultSet rs) throws SQLException {
        DataMessage.Builder builder = buildDataMessageBuilder(rs);
        return builder.build();
    }

    private DataMessage buildDataMessage(ResultSet rs) throws SQLException {
        DataMessage.Builder builder = buildDataMessageBuilder(rs);
        builder = builder.withPayload(rs.getBytes("payload"));
        return builder.build();
    }

    private DataMessage.Builder buildDataMessageBuilder(ResultSet rs) throws SQLException {
        DataMessage.Builder builder;
        builder = new DataMessage.Builder(rs.getInt("id")).withTopic(rs.getString("topic")).withQos(rs.getInt("qos"))
                .withRetain(rs.getBoolean("retain")).withCreatedOn(rs.getTimestamp("createdOn", this.utcCalendar))
                .withPublishedOn(rs.getTimestamp("publishedOn", this.utcCalendar))
                .withPublishedMessageId(rs.getInt("publishedMessageId"))
                .withConfirmedOn(rs.getTimestamp("confirmedOn", this.utcCalendar)).withPriority(rs.getInt("priority"))
                .withSessionId(rs.getString("sessionId")).withDroppedOn(rs.getTimestamp("droppedOn"));
        return builder;
    }

    private Connection getConnection() throws SQLException {
        return this.dbService.getConnection();
    }

    private void rollback(Connection conn) {
        this.dbService.rollback(conn);
    }

    private void close(ResultSet... rss) {
        this.dbService.close(rss);
    }

    private void close(Statement... stmts) {
        this.dbService.close(stmts);
    }

    private void close(Connection conn) {
        this.dbService.close(conn);
    }
}
