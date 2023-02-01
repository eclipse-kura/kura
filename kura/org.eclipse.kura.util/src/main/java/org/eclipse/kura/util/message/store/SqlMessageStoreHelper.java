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
package org.eclipse.kura.util.message.store;

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
import java.util.function.UnaryOperator;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.message.store.StoredMessage;
import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.util.jdbc.SQLFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SqlMessageStoreHelper {

    private static final String TOPIC_ELEMENT = "topic";

    private static final Logger logger = LoggerFactory.getLogger(SqlMessageStoreHelper.class);

    private final ConnectionProvider connectionProvider;
    private final Calendar utcCalendar;

    private final String tableName;
    private final UnaryOperator<String> sanitizer;
    private final String sanitizedTableName;
    private final SqlMessageStoreQueries queries;

    public SqlMessageStoreHelper(final ConnectionProvider connectionProvider, final String table,
            final SqlMessageStoreQueries queries,
            final UnaryOperator<String> sanitizer) {
        this.utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        this.connectionProvider = connectionProvider;
        this.tableName = table;
        this.queries = queries;
        this.sanitizer = sanitizer;
        this.sanitizedTableName = sanitizer.apply(tableName);
    }

    public void createTableAndIndexes() throws KuraStoreException {
        execute(this.queries.getSqlCreateTable());
        execute(this.queries.getSqlCreateIndex());

        createIndex(this.sanitizer.apply(this.tableName + "_PUBLISHEDON"), this.sanitizedTableName,
                "(PUBLISHEDON DESC)");
        createIndex(this.sanitizer.apply(this.tableName + "_CONFIRMEDON"), this.sanitizedTableName,
                "(CONFIRMEDON DESC)");
        createIndex(this.sanitizer.apply(this.tableName + "_DROPPEDON"), this.sanitizedTableName, "(DROPPEDON DESC)");
    }

    public int getMessageCount() throws KuraStoreException {

        return this.connectionProvider.withPreparedStatement(this.queries.getSqlMessageCount(), (c, stmt) -> {
            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("result set is empty");
                }
            }
        }, "Cannot get message count");
    }

    protected void validate(String topic) throws KuraStoreException {
        if (topic == null || topic.trim().length() == 0) {
            throw new KuraStoreException(null, "topic must be not null and not empty");
        }
    }

    public long store(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException {
        validate(topic);

        final Timestamp now = new Timestamp(new Date().getTime());

        return this.connectionProvider.withConnection(c -> {

            long result = -1;

            // store message
            try (PreparedStatement pstmt = c.prepareStatement(this.queries.getSqlStore(),
                    new String[] { "id" })) {
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

    public Optional<StoredMessage> get(int msgId) throws KuraStoreException {

        return get(msgId, rs -> buildStoredMessageBuilder(rs, true).build());
    }

    public Optional<StoredMessage> get(int msgId, final SQLFunction<ResultSet, StoredMessage> messageBuilder)
            throws KuraStoreException {

        return this.connectionProvider.withPreparedStatement(this.queries.getSqlGetMessage(), (c, stmt) -> {
            stmt.setInt(1, msgId);
            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(messageBuilder.call(rs));
                } else {
                    return Optional.empty();
                }
            }

        }, "Cannot get message by ID: " + msgId);
    }

    public Optional<StoredMessage> getNextMessage() throws KuraStoreException {

        return getNextMessage(rs -> buildStoredMessageBuilder(rs, true).build());

    }

    public Optional<StoredMessage> getNextMessage(final SQLFunction<ResultSet, StoredMessage> messageBuilder)
            throws KuraStoreException {

        return this.connectionProvider.withPreparedStatement(this.queries.getSqlGetNextMessage(), (c, stmt) -> {
            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(messageBuilder.call(rs));
                } else {
                    return Optional.empty();
                }
            }
        }, "Cannot get message next message");
    }

    public void markAsPublished(int msgId, DataTransportToken token) throws KuraStoreException {
        final Timestamp now = new Timestamp(new Date().getTime());

        this.connectionProvider.withPreparedStatement(this.queries.getSqlSetPublishedQoS1(), (c, stmt) -> {

            stmt.setTimestamp(1, now, this.utcCalendar);
            stmt.setInt(2, token.getMessageId());
            stmt.setString(3, token.getSessionId());
            stmt.setInt(4, msgId);

            stmt.execute();
            c.commit();
            return null;

        }, "Cannot update timestamp");

    }

    public void markAsPublished(int msgId) throws KuraStoreException {
        updateTimestamp(this.queries.getSqlSetPublishedQoS0(), msgId);
    }

    public void markAsConfirmed(int msgId) throws KuraStoreException {
        updateTimestamp(this.queries.getSqlSetConfirmed(), msgId);
    }

    public List<StoredMessage> getUnpublishedMessages() throws KuraStoreException {
        // Order by priority, createdOn
        return listMessages(this.queries.getSqlAllUnpublishedMessages());
    }

    public List<StoredMessage> getInFlightMessages() throws KuraStoreException {
        // Order by priority, createdOn
        return listMessages(this.queries.getSqlAllInFlightMessages());
    }

    public synchronized List<StoredMessage> getDroppedMessages() throws KuraStoreException {
        // Order by priority, createdOn
        return listMessages(this.queries.getSqlAllDroppedInFlightMessages());
    }

    public void unpublishAllInFlighMessages() throws KuraStoreException {
        execute(this.queries.getSqlUnpublishAllInFlightMessages());
    }

    public void dropAllInFlightMessages() throws KuraStoreException {
        updateTimestamp(this.queries.getSqlDropAllInFlightMessages());
    }

    public void deleteStaleMessages(int purgeAge) throws KuraStoreException {
        final long timestamp = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(purgeAge);
        // Delete dropped messages (published with QoS > 0)
        execute(this.queries.getSqlDeleteDroppedMessages(), timestamp);

        // Delete stale confirmed messages (published with QoS > 0)
        execute(this.queries.getSqlDeleteConfirmedMessages(), timestamp);

        // Delete stale published messages with QoS == 0
        execute(this.queries.getSqlDeletePublishedMessages(), timestamp);
    }

    public void updateTimestamp(String sql, Integer... msgIds) throws KuraStoreException {
        final Timestamp now = new Timestamp(new Date().getTime());

        this.connectionProvider.withPreparedStatement(sql, (c, stmt) -> {
            stmt.setTimestamp(1, now, this.utcCalendar); // timestamp

            for (int i = 0; i < msgIds.length; i++) {
                stmt.setInt(2 + i, msgIds[i]); // messageId
            }
            stmt.execute();
            c.commit();
            return null;

        }, "Cannot update timestamp");
    }

    public List<StoredMessage> listMessages(String sql, Integer... params) throws KuraStoreException {
        return this.connectionProvider.withPreparedStatement(sql, (c, stmt) -> {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setInt(2 + i, params[i]); // timeInterval
                }
            }

            try (final ResultSet rs = stmt.executeQuery()) {
                return buildStoredMessagesNoPayload(rs);
            }
        }, "Cannot list messages");
    }

    public void execute(String sql, Object... params) throws KuraStoreException {
        this.connectionProvider.withPreparedStatement(sql, (c, stmt) -> {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(1 + i, params[i]);
            }
            stmt.execute();
            c.commit();
            return null;

        }, "Cannot execute query");
    }

    public void createIndex(String indexname, String table, String order) throws KuraStoreException {
        execute("CREATE INDEX IF NOT EXISTS " + indexname + " ON " + table + " " + order + ";");
        logger.debug("Index {} created, order is {}", indexname, order);
    }

    public List<StoredMessage> buildStoredMessagesNoPayload(ResultSet rs) throws SQLException {
        List<StoredMessage> messages = new ArrayList<>();
        while (rs.next()) {
            messages.add(buildStoredMessageBuilder(rs, false).build());
        }
        return messages;
    }

    public StoredMessage.Builder buildStoredMessageBuilder(ResultSet rs, final boolean includePayload)
            throws SQLException {
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

        return builder;
    }

    public SqlMessageStoreQueries getQueries() {
        return queries;
    }

    public ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

}
