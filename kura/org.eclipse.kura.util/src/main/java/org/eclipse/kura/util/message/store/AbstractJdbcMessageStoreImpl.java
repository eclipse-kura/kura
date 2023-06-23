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

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.util.jdbc.JdbcUtil.getFirstColumnValue;
import static org.eclipse.kura.util.jdbc.JdbcUtil.getFirstColumnValueOrEmpty;

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
import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.util.jdbc.SQLFunction;

public abstract class AbstractJdbcMessageStoreImpl implements MessageStore {

    private static final String TOPIC_ELEMENT = "topic";

    protected final String tableName;
    protected final String escapedTableName;
    protected final JdbcMessageStoreQueries queries;
    protected final ConnectionProvider connectionProvider;
    protected final Calendar utcCalendar;

    protected AbstractJdbcMessageStoreImpl(final ConnectionProvider connectionProvider, final String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty.");
        }
        this.tableName = tableName;
        this.connectionProvider = requireNonNull(connectionProvider, "Connection provider cannot be null");
        this.escapedTableName = escapeIdentifier(tableName);
        this.utcCalendar = buildUTCCalendar();
        this.queries = buildSqlMessageStoreQueries();
    }

    protected abstract JdbcMessageStoreQueries buildSqlMessageStoreQueries();

    protected String escapeIdentifier(final String string) {
        final String sanitizedName = string.replace("\"", "\"\"");
        return "\"" + sanitizedName + "\"";
    }

    protected void createTable() throws KuraStoreException {
        execute(this.queries.getSqlCreateTable());
    }

    protected void createIndexes() throws KuraStoreException {
        execute(this.queries.getSqlCreateNextMessageIndex());
        execute(this.queries.getSqlCreatePublishedOnIndex());
        execute(this.queries.getSqlCreateConfirmedOnIndex());
        execute(this.queries.getSqlCreateDroppedOnIndex());
    }

    @Override
    public synchronized int getMessageCount() throws KuraStoreException {

        return (int) getMessageCountInternal();
    }

    protected long getMessageCountInternal() throws KuraStoreException {

        return this.connectionProvider.withPreparedStatement(this.queries.getSqlMessageCount(),
                (c, stmt) -> getFirstColumnValue(stmt::executeQuery, ResultSet::getLong), "Cannot get message count");

    }

    protected void validate(String topic) throws KuraStoreException {
        if (topic == null || topic.trim().length() == 0) {
            throw new KuraStoreException(null, "topic must be not null and not empty");
        }
    }

    protected long storeInternal(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException {
        validate(topic);

        final Timestamp now = new Timestamp(new Date().getTime());

        return this.connectionProvider.withConnection(c -> {

            final long result;

            try (PreparedStatement pstmt = c.prepareStatement(this.queries.getSqlStore(),
                    new String[] { "id" })) {

                pstmt.setString(1, topic);
                pstmt.setInt(2, qos);
                pstmt.setBoolean(3, retain);
                pstmt.setTimestamp(4, now, this.utcCalendar);
                pstmt.setTimestamp(5, null);
                pstmt.setInt(6, -1);
                pstmt.setTimestamp(7, null);
                pstmt.setBytes(8, payload);
                pstmt.setInt(9, priority);
                pstmt.setString(10, null);
                pstmt.setTimestamp(11, null);
                pstmt.execute();

                result = getFirstColumnValue(pstmt::getGeneratedKeys, ResultSet::getLong);
            }

            if (isExplicitCommitEnabled()) {
                c.commit();
            }

            return result;
        }, "Cannot store message");

    }

    @Override
    public Optional<StoredMessage> get(int msgId) throws KuraStoreException {

        return get(msgId, rs -> buildStoredMessageBuilder(rs, true).build());
    }

    protected Optional<StoredMessage> get(int msgId, final SQLFunction<ResultSet, StoredMessage> messageBuilder)
            throws KuraStoreException {

        return this.connectionProvider.withPreparedStatement(this.queries.getSqlGetMessage(), (c, stmt) -> {
            stmt.setInt(1, msgId);

            return getFirstColumnValueOrEmpty(stmt::executeQuery, (rs, i) -> messageBuilder.call(rs));

        }, "Cannot get message by ID: " + msgId);
    }

    @Override
    public Optional<StoredMessage> getNextMessage() throws KuraStoreException {

        return getNextMessage(rs -> buildStoredMessageBuilder(rs, true).build());

    }

    protected Optional<StoredMessage> getNextMessage(final SQLFunction<ResultSet, StoredMessage> messageBuilder)
            throws KuraStoreException {

        return this.connectionProvider.withPreparedStatement(this.queries.getSqlGetNextMessage(),
                (c, stmt) -> getFirstColumnValueOrEmpty(stmt::executeQuery, (rs, i) -> messageBuilder.call(rs)),
                "Cannot get message next message");
    }

    @Override
    public void markAsPublished(int msgId, DataTransportToken token) throws KuraStoreException {
        final Timestamp now = new Timestamp(new Date().getTime());

        this.connectionProvider.withPreparedStatement(this.queries.getSqlSetPublishedQoS1(), (c, stmt) -> {

            stmt.setTimestamp(1, now, this.utcCalendar);
            stmt.setInt(2, token.getMessageId());
            stmt.setString(3, token.getSessionId());
            stmt.setInt(4, msgId);

            stmt.execute();

            if (isExplicitCommitEnabled()) {
                c.commit();
            }
            return null;

        }, "Cannot update timestamp");

    }

    @Override
    public void markAsPublished(int msgId) throws KuraStoreException {
        updateTimestamp(this.queries.getSqlSetPublishedQoS0(), msgId);
    }

    @Override
    public void markAsConfirmed(int msgId) throws KuraStoreException {
        updateTimestamp(this.queries.getSqlSetConfirmed(), msgId);
    }

    @Override
    public List<StoredMessage> getUnpublishedMessages() throws KuraStoreException {

        return listMessages(this.queries.getSqlAllUnpublishedMessages());
    }

    @Override
    public List<StoredMessage> getInFlightMessages() throws KuraStoreException {

        return listMessages(this.queries.getSqlAllInFlightMessages());
    }

    @Override
    public synchronized List<StoredMessage> getDroppedMessages() throws KuraStoreException {

        return listMessages(this.queries.getSqlAllDroppedInFlightMessages());
    }

    @Override
    public synchronized void unpublishAllInFlighMessages() throws KuraStoreException {
        execute(this.queries.getSqlUnpublishAllInFlightMessages());
    }

    @Override
    public synchronized void dropAllInFlightMessages() throws KuraStoreException {
        updateTimestamp(this.queries.getSqlDropAllInFlightMessages());
    }

    @Override
    public synchronized void deleteStaleMessages(int purgeAgeSeconds) throws KuraStoreException {
        final long timestamp = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(purgeAgeSeconds);

        deleteStaleMessages(timestamp);
    }

    @Override
    public void close() {
        // nothing to close
    }

    protected void deleteStaleMessages(final Object timestamp) throws KuraStoreException {

        execute(this.queries.getSqlDeleteDroppedMessages(), timestamp);

        execute(this.queries.getSqlDeleteConfirmedMessages(), timestamp);

        execute(this.queries.getSqlDeletePublishedMessages(), timestamp);
    }

    protected void updateTimestamp(String sql, Integer... msgIds) throws KuraStoreException {
        final Timestamp now = new Timestamp(new Date().getTime());

        this.connectionProvider.withPreparedStatement(sql, (c, stmt) -> {
            stmt.setTimestamp(1, now, this.utcCalendar);

            for (int i = 0; i < msgIds.length; i++) {
                stmt.setInt(2 + i, msgIds[i]);
            }
            stmt.execute();

            if (isExplicitCommitEnabled()) {
                c.commit();
            }
            return null;

        }, "Cannot update timestamp");
    }

    protected List<StoredMessage> listMessages(String sql, Integer... params) throws KuraStoreException {
        return this.connectionProvider.withPreparedStatement(sql, (c, stmt) -> {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setInt(2 + i, params[i]);
                }
            }

            try (final ResultSet rs = stmt.executeQuery()) {
                return buildStoredMessagesNoPayload(rs);
            }
        }, "Cannot list messages");
    }

    protected void execute(String sql, Object... params) throws KuraStoreException {
        this.connectionProvider.withPreparedStatement(sql, (c, stmt) -> {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(1 + i, params[i]);
            }

            stmt.execute();

            if (isExplicitCommitEnabled()) {
                c.commit();
            }
            return null;

        }, "Cannot execute query");
    }

    protected List<StoredMessage> buildStoredMessagesNoPayload(ResultSet rs) throws SQLException {
        List<StoredMessage> messages = new ArrayList<>();
        while (rs.next()) {
            messages.add(buildStoredMessageBuilder(rs, false).build());
        }
        return messages;
    }

    protected StoredMessage.Builder buildStoredMessageBuilder(ResultSet rs, final boolean includePayload)
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

    protected Calendar buildUTCCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    protected boolean isExplicitCommitEnabled() {
        return false;
    }

    protected JdbcMessageStoreQueries getQueries() {
        return queries;
    }

    protected ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }
}
