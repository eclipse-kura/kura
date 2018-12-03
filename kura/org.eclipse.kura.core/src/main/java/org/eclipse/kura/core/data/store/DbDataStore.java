/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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

    private static final String TOPIC_ELEMENT = "topic";

    private static final String UPDATE = "UPDATE ";

    private static final String DELETE_FROM = "DELETE FROM ";

    private static final String SELECT_MESSAGE_METADATA_FROM = "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, priority, sessionId, droppedOn FROM ";

    private static final String ALTER_TABLE = "ALTER TABLE ";

    private static final Logger logger = LoggerFactory.getLogger(DbDataStore.class);

    private static final String DATA_SERVICE_REPAIR_ENABLED_PROPNAME = "db.store.repair.enabled";

    private H2DbService dbService;
    private final Calendar utcCalendar;
    private ScheduledExecutorService houseKeeperExecutor;
    private ScheduledFuture<?> houseKeeperTask;
    private int capacity;

    private final String tableName;
    private final String sanitizedTableName;

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

        this.tableName = table;
        this.sanitizedTableName = sanitizeSql(table);

        this.sqlCreateTable = "CREATE TABLE IF NOT EXISTS " + this.sanitizedTableName
                + " (id INTEGER IDENTITY PRIMARY KEY, topic VARCHAR(32767 CHAR), qos INTEGER, retain BOOLEAN, createdOn TIMESTAMP, publishedOn TIMESTAMP, publishedMessageId INTEGER, confirmedOn TIMESTAMP, payload VARBINARY(16777216), priority INTEGER, sessionId VARCHAR(32767 CHAR), droppedOn TIMESTAMP);";
        this.sqlCreateIndex = "CREATE INDEX IF NOT EXISTS " + sanitizeSql(this.tableName + "_nextMsg") + " ON "
                + this.sanitizedTableName + " (publishedOn ASC NULLS FIRST, priority ASC, createdOn ASC, qos);";
        this.sqlMessageCount = "SELECT COUNT(*) FROM " + this.sanitizedTableName + ";";
        this.sqlResetId = ALTER_TABLE + this.sanitizedTableName + " ALTER COLUMN id RESTART WITH 1;";
        this.sqlStore = "INSERT INTO " + this.sanitizedTableName
                + " (topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, payload, priority, sessionId, droppedOn) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        this.sqlGetMessage = "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, payload, priority, sessionId, droppedOn FROM "
                + this.sanitizedTableName + " WHERE id = ?";
        this.sqlGetNextMessage = "SELECT a.id, a.topic, a.qos, a.retain, a.createdOn, a.publishedOn, a.publishedMessageId, a.confirmedOn, a.payload, a.priority, a.sessionId, a.droppedOn FROM "
                + this.sanitizedTableName + " AS a JOIN (SELECT id, publishedOn FROM " + this.sanitizedTableName
                + " ORDER BY publishedOn ASC NULLS FIRST, priority ASC, createdOn ASC LIMIT 1) AS b WHERE a.id = b.id AND b.publishedOn IS NULL;";
        this.sqlSetPublished = UPDATE + this.sanitizedTableName
                + " SET publishedOn = ?, publishedMessageId = ?, sessionId = ? WHERE id = ?;";
        this.sqlSetPublished2 = UPDATE + this.sanitizedTableName + " SET publishedOn = ? WHERE id = ?;";
        this.sqlSetConfirmed = UPDATE + this.sanitizedTableName + " SET confirmedOn = ? WHERE id = ?;";
        this.sqlAllUnpublishedMessages = SELECT_MESSAGE_METADATA_FROM + this.sanitizedTableName
                + " WHERE publishedOn IS NULL ORDER BY priority ASC, createdOn ASC;";
        this.sqlAllInFlightMessages = SELECT_MESSAGE_METADATA_FROM + this.sanitizedTableName
                + " WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL AND droppedOn IS NULL ORDER BY priority ASC, createdOn ASC";
        this.sqlAllDroppedInFlightMessages = SELECT_MESSAGE_METADATA_FROM + this.sanitizedTableName
                + " WHERE droppedOn IS NOT NULL ORDER BY priority ASC, createdOn ASC;";
        this.sqlUnpublishAllInFlightMessages = UPDATE + this.sanitizedTableName
                + " SET publishedOn = NULL WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;";
        this.sqlDropAllInFlightMessages = UPDATE + this.sanitizedTableName
                + " SET droppedOn = ? WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;";
        this.sqlDeleteDroppedMessages = DELETE_FROM + this.sanitizedTableName
                + " WHERE droppedOn <= DATEADD('ss', -?, ?) AND droppedOn IS NOT NULL;";
        this.sqlDeleteConfirmedMessages = DELETE_FROM + this.sanitizedTableName
                + " WHERE confirmedOn <= DATEADD('ss', -?, ?) AND confirmedOn IS NOT NULL;";
        this.sqlDeletePublishedMessages = DELETE_FROM + this.sanitizedTableName
                + " WHERE qos = 0 AND publishedOn <= DATEADD('ss', -?, ?) AND publishedOn IS NOT NULL;";
        this.sqlDuplicateCount = "SELECT count(*) FROM (SELECT id, COUNT(id) FROM " + this.sanitizedTableName
                + " GROUP BY id HAVING (COUNT(id) > 1)) dups;";
        this.sqlDropPrimaryKey = ALTER_TABLE + this.sanitizedTableName + " DROP PRIMARY KEY;";
        this.sqlDeleteDuplicates = DELETE_FROM + this.sanitizedTableName + " WHERE id IN (SELECT id FROM "
                + this.sanitizedTableName + " GROUP BY id HAVING COUNT(*) > 1);";
        this.sqlCreatePrimaryKey = ALTER_TABLE + this.sanitizedTableName + " ADD PRIMARY KEY (id);";
    }

    private String sanitizeSql(final String string) {
        final String sanitizedName = string.replaceAll("\"", "\"\"");
        return "\"" + sanitizedName + "\"";
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
            this.houseKeeperExecutor.shutdownNow();
            this.houseKeeperTask = null;
        }
        this.dbService = null;
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

            createIndex(sanitizeSql(this.tableName + "_PUBLISHEDON"), this.sanitizedTableName, "(PUBLISHEDON DESC)");
            createIndex(sanitizeSql(this.tableName + "_CONFIRMEDON"), this.sanitizedTableName, "(CONFIRMEDON DESC)");
            createIndex(sanitizeSql(this.tableName + "_DROPPEDON"), this.sanitizedTableName, "(DROPPEDON DESC)");

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

        return withConnection(c -> {
            try (final PreparedStatement stmt = c.prepareStatement(this.sqlMessageCount);
                    final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    return -1;
                }
            }
        }, "Cannot get message count");
    }

    private synchronized void resetIdentityGenerator() throws KuraStoreException {
        execute(this.sqlResetId);
    }

    @Override
    public synchronized DataMessage store(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException {
        if (this.dbService == null) {
            throw new KuraStoreException("DbService instance not attached");
        }
        if (topic == null || topic.trim().length() == 0) {
            throw new IllegalArgumentException(TOPIC_ELEMENT);
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
            throw new IllegalArgumentException(TOPIC_ELEMENT);
        }

        final Timestamp now = new Timestamp(new Date().getTime());

        final int msgId = withConnection(c -> {

            int result = -1;

            // store message
            try (PreparedStatement pstmt = c.prepareStatement(this.sqlStore)) {
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
            try (PreparedStatement cstmt = c.prepareStatement("CALL IDENTITY();");
                    ResultSet rs = cstmt.executeQuery()) {
                if (rs != null && rs.next()) {
                    result = rs.getInt(1);
                }
            }

            c.commit();

            return result;
        }, "Cannot store message");

        return get(msgId);
    }

    @Override
    public synchronized DataMessage get(int msgId) throws KuraStoreException {

        return withConnection(c -> {
            try (PreparedStatement stmt = c.prepareStatement(this.sqlGetMessage)) {
                stmt.setInt(1, msgId);
                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return buildDataMessage(rs);
                    } else {
                        return null;
                    }
                }
            }
        }, "Cannot get message by ID: " + msgId);
    }

    @Override
    public synchronized DataMessage getNextMessage() throws KuraStoreException {

        return withConnection(c -> {
            try (PreparedStatement stmt = c.prepareStatement(this.sqlGetNextMessage);
                    ResultSet rs = stmt.executeQuery()) {
                if (rs != null && rs.next()) {
                    return buildDataMessage(rs);
                } else {
                    return null;
                }
            }
        }, "Cannot get message next message");
    }

    @Override
    public synchronized void published(int msgId, int publishedMsgId, String sessionId) throws KuraStoreException {
        final Timestamp now = new Timestamp(new Date().getTime());

        withConnection(c -> {
            try (final PreparedStatement stmt = c.prepareStatement(this.sqlSetPublished)) {
                stmt.setTimestamp(1, now, this.utcCalendar); // timestamp
                stmt.setInt(2, publishedMsgId);
                stmt.setString(3, sessionId);
                stmt.setInt(4, msgId);

                stmt.execute();
                c.commit();
                return (Void) null;
            }
        }, "Cannot update timestamp");

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

        withConnection(c -> {
            int count = -1;

            try (PreparedStatement pstmt = c.prepareStatement(this.sqlDuplicateCount);
                    ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
            if (count <= 0) {
                return (Void) null;
            }

            logger.error(
                    "Found messages with duplicate ID. Count of IDs for which duplicates exist: {}. Attempting to repair...",
                    count);

            try (Statement stmt = c.createStatement()) {

                stmt.execute(this.sqlDropPrimaryKey);
                logger.info("Primary key dropped");

                stmt.execute(this.sqlDeleteDuplicates);
                logger.info("Duplicate messages deleted");

                stmt.execute(this.sqlCreatePrimaryKey);
                logger.info("Primary key created");

            }
            c.commit();

            try (final PreparedStatement stmt = c.prepareStatement("CHECKPOINT")) {
                stmt.execute();
                logger.info("Checkpoint");
            }

            c.commit();

            return (Void) null;
        }, "Cannot repair database");

    }

    // ------------------------------------------------------------------
    //
    // Private Methods
    //
    // ------------------------------------------------------------------

    private synchronized void updateTimestamp(String sql, Integer... msgIds) throws KuraStoreException {
        final Timestamp now = new Timestamp(new Date().getTime());

        withConnection(c -> {
            try (final PreparedStatement stmt = c.prepareStatement(sql)) {
                stmt.setTimestamp(1, now, this.utcCalendar); // timestamp

                for (int i = 0; i < msgIds.length; i++) {
                    stmt.setInt(2 + i, msgIds[i]);  // messageId
                }
                stmt.execute();
                c.commit();
                return (Void) null;
            }
        }, "Cannot update timestamp");
    }

    private synchronized List<DataMessage> listMessages(String sql, Integer... params) throws KuraStoreException {
        return withConnection(c -> {
            try (final PreparedStatement stmt = c.prepareStatement(sql)) {
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        stmt.setInt(2 + i, params[i]);  // timeInterval
                    }
                }

                try (final ResultSet rs = stmt.executeQuery()) {
                    return buildDataMessagesNoPayload(rs);
                }
            }
        }, "Cannot list messages");
    }

    private synchronized void execute(String sql, Integer... params) throws KuraStoreException {
        withConnection(c -> {
            try (final PreparedStatement stmt = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setInt(1 + i, params[i]);
                }
                stmt.execute();
                c.commit();
                return (Void) null;
            }
        }, "Cannot execute query");
    }

    private synchronized void executeDeleteMessagesQuery(String sql, Timestamp timestamp, int purgeAge)
            throws KuraStoreException {
        withConnection(c -> {
            try (final PreparedStatement stmt = c.prepareStatement(sql)) {
                stmt.setInt(1, purgeAge);
                stmt.setTimestamp(2, timestamp, this.utcCalendar);

                stmt.execute();
                c.commit();
                return (Void) null;
            }
        }, "Cannot execute query");
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

    private List<DataMessage> buildDataMessagesNoPayload(ResultSet rs) throws SQLException {
        List<DataMessage> messages = new ArrayList<>();
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
        builder = new DataMessage.Builder(rs.getInt("id")).withTopic(rs.getString(TOPIC_ELEMENT))
                .withQos(rs.getInt("qos")).withRetain(rs.getBoolean("retain"))
                .withCreatedOn(rs.getTimestamp("createdOn", this.utcCalendar))
                .withPublishedOn(rs.getTimestamp("publishedOn", this.utcCalendar))
                .withPublishedMessageId(rs.getInt("publishedMessageId"))
                .withConfirmedOn(rs.getTimestamp("confirmedOn", this.utcCalendar)).withPriority(rs.getInt("priority"))
                .withSessionId(rs.getString("sessionId")).withDroppedOn(rs.getTimestamp("droppedOn"));
        return builder;
    }

    private <T> T withConnection(final H2DbService.ConnectionCallable<T> callable, final String exceptionMessage)
            throws KuraStoreException {
        if (this.dbService == null) {
            throw new KuraStoreException("DbService instance not attached");
        }

        try {
            return this.dbService.withConnection(callable);
        } catch (final Exception e) {
            throw new KuraStoreException(e, exceptionMessage);
        }
    }
}
