/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.db.sqlite.provider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.message.store.StoredMessage;
import org.eclipse.kura.message.store.provider.MessageStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqliteMessageStoreImpl implements MessageStore {

    private static final String TOPIC_ELEMENT = "topic";

    private static final String UPDATE = "UPDATE ";

    private static final String DELETE_FROM = "DELETE FROM ";

    private static final String SELECT_MESSAGE_METADATA_FROM = "SELECT id, topic, qos, retain, createdOn, publishedOn, "
            + "publishedMessageId, confirmedOn, priority, sessionId, droppedOn FROM ";

    private static final Logger logger = LoggerFactory.getLogger(SqliteMessageStoreImpl.class);

    private SqliteDbServiceImpl dbService;
    private final Calendar utcCalendar;

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
    private final String sqlDeleteMessage;

    // package level constructor to be invoked only by the factory
    public SqliteMessageStoreImpl(final SqliteDbServiceImpl dbService, final String table) throws KuraStoreException {
        // do not make this static as it may not be thread safe
        this.utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        this.dbService = dbService;
        this.tableName = table;
        this.sanitizedTableName = sanitizeSql(table);

        this.sqlCreateTable = "CREATE TABLE IF NOT EXISTS " + this.sanitizedTableName
                + " (id INTEGER PRIMARY KEY AUTOINCREMENT, topic VARCHAR, qos INTEGER, retain BOOLEAN, "
                + "createdOn DATETIME, publishedOn DATETIME, publishedMessageId INTEGER, confirmedOn DATETIME, "
                + "payload BLOB, priority INTEGER, sessionId VARCHAR, droppedOn DATETIME);";
        this.sqlCreateIndex = "CREATE INDEX IF NOT EXISTS " + sanitizeSql(this.tableName + "_nextMsg") + " ON "
                + this.sanitizedTableName + " (publishedOn ASC, priority ASC, createdOn ASC, qos);";
        this.sqlMessageCount = "SELECT COUNT(*) FROM " + this.sanitizedTableName + ";";
        this.sqlResetId = UPDATE + " sqlite_sequence SET seq = 0 WHERE name = " + this.sanitizedTableName + ";";

        this.sqlStore = "INSERT INTO " + this.sanitizedTableName
                + " (topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, payload, priority, "
                + "sessionId, droppedOn) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        this.sqlGetMessage = "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, "
                + "payload, priority, sessionId, droppedOn FROM " + this.sanitizedTableName + " WHERE id = ?";
        this.sqlGetNextMessage = "SELECT a.id, a.topic, a.qos, a.retain, a.createdOn, a.publishedOn, "
                + "a.publishedMessageId, a.confirmedOn, a.payload, a.priority, a.sessionId, a.droppedOn FROM "
                + this.sanitizedTableName + " AS a JOIN (SELECT id, publishedOn FROM " + this.sanitizedTableName
                + " ORDER BY publishedOn ASC NULLS FIRST, priority ASC, createdOn ASC LIMIT 1) AS b "
                + "WHERE a.id = b.id AND b.publishedOn IS NULL;";

        this.sqlSetPublished = UPDATE + this.sanitizedTableName
                + " SET publishedOn = ?, publishedMessageId = ?, sessionId = ? WHERE id = ?;";
        this.sqlSetPublished2 = UPDATE + this.sanitizedTableName + " SET publishedOn = ? WHERE id = ?;";
        this.sqlSetConfirmed = UPDATE + this.sanitizedTableName + " SET confirmedOn = ? WHERE id = ?;";
        this.sqlAllUnpublishedMessages = SELECT_MESSAGE_METADATA_FROM + this.sanitizedTableName
                + " WHERE publishedOn IS NULL ORDER BY priority ASC, createdOn ASC;";
        this.sqlAllInFlightMessages = SELECT_MESSAGE_METADATA_FROM + this.sanitizedTableName
                + " WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL AND droppedOn IS NULL "
                + "ORDER BY priority ASC, createdOn ASC";
        this.sqlAllDroppedInFlightMessages = SELECT_MESSAGE_METADATA_FROM + this.sanitizedTableName
                + " WHERE droppedOn IS NOT NULL ORDER BY priority ASC, createdOn ASC;";
        this.sqlUnpublishAllInFlightMessages = UPDATE + this.sanitizedTableName
                + " SET publishedOn = NULL WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;";
        this.sqlDropAllInFlightMessages = UPDATE + this.sanitizedTableName
                + " SET droppedOn = ? WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;";
        this.sqlDeleteDroppedMessages = DELETE_FROM + this.sanitizedTableName
                + " WHERE droppedOn <= ? AND droppedOn IS NOT NULL;";
        this.sqlDeleteConfirmedMessages = DELETE_FROM + this.sanitizedTableName
                + " WHERE confirmedOn <= ? AND confirmedOn IS NOT NULL;";
        this.sqlDeletePublishedMessages = DELETE_FROM + this.sanitizedTableName
                + " WHERE qos = 0 AND publishedOn <= ? AND publishedOn IS NOT NULL;";
        this.sqlDeleteMessage = DELETE_FROM + this.sanitizedTableName + " WHERE id = ?;";

        execute(this.sqlCreateTable);
        execute(this.sqlCreateIndex);

        createIndex(sanitizeSql(this.tableName + "_PUBLISHEDON"), this.sanitizedTableName, "(PUBLISHEDON DESC)");
        createIndex(sanitizeSql(this.tableName + "_CONFIRMEDON"), this.sanitizedTableName, "(CONFIRMEDON DESC)");
        createIndex(sanitizeSql(this.tableName + "_DROPPEDON"), this.sanitizedTableName, "(DROPPEDON DESC)");
    }

    private String sanitizeSql(final String string) {
        final String sanitizedName = string.replace("\"", "\"\"");
        return "\"" + sanitizedName + "\"";
    }

    // ----------------------------------------------------------
    //
    // Start/Stop, ServiceId
    //
    // ----------------------------------------------------------

    // ----------------------------------------------------------
    //
    // Message APIs
    //
    // ----------------------------------------------------------

    @Override
    public synchronized int getMessageCount() throws KuraStoreException {

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
    public synchronized int store(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException {

        validate(topic);

        final long id = storeInternal(topic, payload, qos, retain, priority);

        if (id > Integer.MAX_VALUE) {
            this.execute(this.sqlDeleteMessage, id);
            resetIdentityGenerator();
            return (int) storeInternal(topic, payload, qos, retain, priority);
        }

        return (int) id;

    }

    private void validate(String topic) throws KuraStoreException {
        if (topic == null || topic.trim().length() == 0) {
            throw new KuraStoreException(null, "topic must be not null and not empty");
        }
    }

    private synchronized long storeInternal(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException {
        if (topic == null || topic.trim().length() == 0) {
            throw new IllegalArgumentException(TOPIC_ELEMENT);
        }

        final Timestamp now = new Timestamp(new Date().getTime());

        return withConnection(c -> {

            long result = -1;

            // store message
            try (PreparedStatement pstmt = c.prepareStatement(this.sqlStore, new String[] { "id" })) {
                pstmt.setString(1, topic); // topic
                pstmt.setInt(2, qos); // qos
                pstmt.setBoolean(3, retain); // retain
                pstmt.setTimestamp(4, now, this.utcCalendar); // createdOn
                pstmt.setTimestamp(5, null); // publishedOn
                pstmt.setInt(6, -1); // publishedMessageId
                pstmt.setTimestamp(7, null); // confirmedOn
                if (payload != null) {
                    pstmt.setBytes(8, payload);
                }
                pstmt.setInt(9, priority); // priority
                pstmt.setString(10, null); // sessionId
                pstmt.setTimestamp(11, null); // droppedOn
                pstmt.execute();
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    result = rs.getLong(1);
                }
            }

            c.commit();

            return result;
        }, "Cannot store message");

    }

    @Override
    public synchronized Optional<StoredMessage> get(int msgId) throws KuraStoreException {

        return withConnection(c -> {
            try (PreparedStatement stmt = c.prepareStatement(this.sqlGetMessage)) {
                stmt.setInt(1, msgId);
                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(buildStoredMessage(rs, true));
                    } else {
                        return Optional.empty();
                    }
                }
            }
        }, "Cannot get message by ID: " + msgId);
    }

    @Override
    public synchronized Optional<StoredMessage> getNextMessage() throws KuraStoreException {

        return withConnection(c -> {
            try (PreparedStatement stmt = c.prepareStatement(this.sqlGetNextMessage);
                    ResultSet rs = stmt.executeQuery()) {
                if (rs != null && rs.next()) {
                    return Optional.of(buildStoredMessage(rs, true));
                } else {
                    return Optional.empty();
                }
            }
        }, "Cannot get message next message");
    }

    @Override
    public synchronized void markAsPublished(int msgId, DataTransportToken token) throws KuraStoreException {
        final Timestamp now = new Timestamp(new Date().getTime());

        withConnection(c -> {
            try (final PreparedStatement stmt = c.prepareStatement(this.sqlSetPublished)) {
                stmt.setTimestamp(1, now, this.utcCalendar); // timestamp
                stmt.setInt(2, token.getMessageId());
                stmt.setString(3, token.getSessionId());
                stmt.setInt(4, msgId);

                stmt.execute();
                c.commit();
                return null;
            }
        }, "Cannot update timestamp");

    }

    @Override
    public synchronized void markAsPublished(int msgId) throws KuraStoreException {
        updateTimestamp(this.sqlSetPublished2, msgId);
    }

    @Override
    public synchronized void markAsConfirmed(int msgId) throws KuraStoreException {
        updateTimestamp(this.sqlSetConfirmed, msgId);
    }

    @Override
    public synchronized List<StoredMessage> getUnpublishedMessages() throws KuraStoreException {
        // Order by priority, createdOn
        return listMessages(this.sqlAllUnpublishedMessages);
    }

    @Override
    public synchronized List<StoredMessage> getInFlightMessages() throws KuraStoreException {
        // Order by priority, createdOn
        return listMessages(this.sqlAllInFlightMessages);
    }

    @Override
    public synchronized List<StoredMessage> getDroppedMessages() throws KuraStoreException {
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
        final long timestamp = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(purgeAge);
        // Delete dropped messages (published with QoS > 0)
        execute(this.sqlDeleteDroppedMessages, timestamp);

        // Delete stale confirmed messages (published with QoS > 0)
        execute(this.sqlDeleteConfirmedMessages, timestamp);

        // Delete stale published messages with QoS == 0
        execute(this.sqlDeletePublishedMessages, timestamp);
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
                    stmt.setInt(2 + i, msgIds[i]); // messageId
                }
                stmt.execute();
                c.commit();
                return null;
            }
        }, "Cannot update timestamp");
    }

    private synchronized List<StoredMessage> listMessages(String sql, Integer... params) throws KuraStoreException {
        return withConnection(c -> {
            try (final PreparedStatement stmt = c.prepareStatement(sql)) {
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        stmt.setInt(2 + i, params[i]); // timeInterval
                    }
                }

                try (final ResultSet rs = stmt.executeQuery()) {
                    return buildStoredMessagesNoPayload(rs);
                }
            }
        }, "Cannot list messages");
    }

    private synchronized void execute(String sql, Object... params) throws KuraStoreException {
        withConnection(c -> {
            try (final PreparedStatement stmt = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(1 + i, params[i]);
                }
                stmt.execute();
                c.commit();
                return null;
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

    private List<StoredMessage> buildStoredMessagesNoPayload(ResultSet rs) throws SQLException {
        List<StoredMessage> messages = new ArrayList<>();
        while (rs.next()) {
            messages.add(buildStoredMessage(rs, false));
        }
        return messages;
    }

    private StoredMessage buildStoredMessage(ResultSet rs, final boolean includePayload) throws SQLException {
        StoredMessage.Builder builder = new StoredMessage.Builder(rs.getInt("id"))
                .withTopic(rs.getString(TOPIC_ELEMENT)).withQos(rs.getInt("qos")).withRetain(rs.getBoolean("retain"))
                .withCreatedOn(rs.getTimestamp("createdOn", this.utcCalendar))
                .withPublishedOn(rs.getTimestamp("publishedOn", this.utcCalendar))
                .withConfirmedOn(rs.getTimestamp("confirmedOn", this.utcCalendar)).withPriority(rs.getInt("priority"))
                .withDroppedOn(rs.getTimestamp("droppedOn"));

        if (includePayload) {
            builder = builder.withPayload(rs.getBytes("payload"));
        }

        final String sessionId = rs.getString("sessionId");

        if (sessionId != null) {
            builder = builder
                    .withDataTransportToken(new DataTransportToken(rs.getInt("publishedMessageId"), sessionId));
        }

        return builder.build();
    }

    private <T> T withConnection(final ConnectionCallable<T> callable, final String exceptionMessage)
            throws KuraStoreException {
        if (this.dbService == null) {
            throw new KuraStoreException("DbService instance not attached");
        }

        try (final Connection conn = this.dbService.getConnection()) {
            return callable.call(conn);
        } catch (final Exception e) {
            throw new KuraStoreException(e, exceptionMessage);
        }
    }

    @Override
    public void close() {
        // nothing to close
    }

    private interface ConnectionCallable<T> {

        public T call(Connection c) throws SQLException;
    }

}
