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

import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.message.store.StoredMessage;
import org.eclipse.kura.message.store.provider.MessageStore;
import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.util.message.store.SqlMessageStoreHelper;
import org.eclipse.kura.util.message.store.SqlMessageStoreQueries;

@SuppressWarnings("restriction")
public class SqliteMessageStoreImpl implements MessageStore {

    private static final String CREATE_INDEX_IF_NOT_EXISTS = "CREATE INDEX IF NOT EXISTS ";

    private static final String UPDATE = "UPDATE ";

    private static final String DELETE_FROM = "DELETE FROM ";

    private static final String SELECT_MESSAGE_METADATA_FROM = "SELECT id, topic, qos, retain, createdOn, publishedOn, "
            + "publishedMessageId, confirmedOn, priority, sessionId, droppedOn FROM ";

    private final String tableName;
    private final String sanitizedTableName;

    private final String sqlResetId;
    private final String sqlDeleteMessage;

    private final SqlMessageStoreHelper helper;

    public SqliteMessageStoreImpl(final ConnectionProvider provider, final String table) throws KuraStoreException {

        this.tableName = table;
        this.sanitizedTableName = sanitizeSql(table);

        this.sqlResetId = UPDATE + " sqlite_sequence SET seq = 0 WHERE name = " + this.sanitizedTableName + ";";
        this.sqlDeleteMessage = DELETE_FROM + this.sanitizedTableName + " WHERE id = ?;";

        final SqlMessageStoreQueries queries = SqlMessageStoreQueries.builder()
                .withSqlCreateTable("CREATE TABLE IF NOT EXISTS " + this.sanitizedTableName
                        + " (id INTEGER PRIMARY KEY AUTOINCREMENT, topic VARCHAR, qos INTEGER, retain BOOLEAN, "
                        + "createdOn DATETIME, publishedOn DATETIME, publishedMessageId INTEGER, confirmedOn DATETIME, "
                        + "payload BLOB, priority INTEGER, sessionId VARCHAR, droppedOn DATETIME);")
                .withSqlMessageCount("SELECT COUNT(*) FROM " + this.sanitizedTableName + ";")
                .withSqlStore("INSERT INTO " + this.sanitizedTableName
                        + " (topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, payload, priority, "
                        + "sessionId, droppedOn) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")
                .withSqlGetMessage(
                        "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, "
                                + "payload, priority, sessionId, droppedOn FROM " + this.sanitizedTableName
                                + " WHERE id = ?")
                .withSqlGetNextMessage("SELECT a.id, a.topic, a.qos, a.retain, a.createdOn, a.publishedOn, "
                        + "a.publishedMessageId, a.confirmedOn, a.payload, a.priority, a.sessionId, a.droppedOn FROM "
                        + this.sanitizedTableName + " AS a JOIN (SELECT id, publishedOn FROM " + this.sanitizedTableName
                        + " ORDER BY publishedOn ASC NULLS FIRST, priority ASC, createdOn ASC LIMIT 1) AS b "
                        + "WHERE a.id = b.id AND b.publishedOn IS NULL;")
                .withSqlSetPublishedQoS1(UPDATE + this.sanitizedTableName
                        + " SET publishedOn = ?, publishedMessageId = ?, sessionId = ? WHERE id = ?;")
                .withSqlSetPublishedQoS0(UPDATE + this.sanitizedTableName + " SET publishedOn = ? WHERE id = ?;")
                .withSqlSetConfirmed(UPDATE + this.sanitizedTableName + " SET confirmedOn = ? WHERE id = ?;")
                .withSqlAllUnpublishedMessages(SELECT_MESSAGE_METADATA_FROM + this.sanitizedTableName
                        + " WHERE publishedOn IS NULL ORDER BY priority ASC, createdOn ASC;")
                .withSqlAllInFlightMessages(SELECT_MESSAGE_METADATA_FROM + this.sanitizedTableName
                        + " WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL AND droppedOn IS NULL "
                        + "ORDER BY priority ASC, createdOn ASC")
                .withSqlAllDroppedInFlightMessages(SELECT_MESSAGE_METADATA_FROM + this.sanitizedTableName
                        + " WHERE droppedOn IS NOT NULL ORDER BY priority ASC, createdOn ASC;")
                .withSqlUnpublishAllInFlightMessages(UPDATE + this.sanitizedTableName
                        + " SET publishedOn = NULL WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;")
                .withSqlDropAllInFlightMessages(UPDATE + this.sanitizedTableName
                        + " SET droppedOn = ? WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;")
                .withSqlDeleteDroppedMessages(
                        DELETE_FROM + this.sanitizedTableName + " WHERE droppedOn <= ? AND droppedOn IS NOT NULL;")
                .withSqlDeleteConfirmedMessages(
                        DELETE_FROM + this.sanitizedTableName + " WHERE confirmedOn <= ? AND confirmedOn IS NOT NULL;")
                .withSqlDeletePublishedMessages(DELETE_FROM + this.sanitizedTableName
                        + " WHERE qos = 0 AND publishedOn <= ? AND publishedOn IS NOT NULL;")
                .withSqlCreateNextMessageIndex(CREATE_INDEX_IF_NOT_EXISTS + sanitizeSql(this.tableName + "_nextMsg")
                        + " ON " + this.sanitizedTableName + " (publishedOn ASC, priority ASC, createdOn ASC, qos);")
                .withSqlCreatePublishedOnIndex(CREATE_INDEX_IF_NOT_EXISTS + sanitizeSql(this.tableName + "_PUBLISHEDON")
                        + " ON " + this.sanitizedTableName + " (publishedOn DESC);")
                .withSqlCreateConfirmedOnIndex(CREATE_INDEX_IF_NOT_EXISTS + sanitizeSql(this.tableName + "_CONFIRMEDON")
                        + " ON " + this.sanitizedTableName + " (confirmedOn DESC);")
                .withSqlCreateDroppedOnIndex(CREATE_INDEX_IF_NOT_EXISTS + sanitizeSql(this.tableName + "_DROPPEDON")
                        + " ON " + this.sanitizedTableName + " (droppedOn DESC);")
                .build();

        this.helper = SqlMessageStoreHelper.builder() //
                .withConnectionProvider(provider) //
                .withQueries(queries) //
                .withExplicitCommitEnabled(false) //
                .build();

        this.helper.createTable();
        this.helper.createIndexes();
    }

    private String sanitizeSql(final String string) {
        final String sanitizedName = string.replace("\"", "\"\"");
        return "\"" + sanitizedName + "\"";
    }

    @Override
    public synchronized int getMessageCount() throws KuraStoreException {

        return this.helper.getMessageCount();
    }

    @Override
    public synchronized int store(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException {

        validate(topic);

        final long id = this.helper.store(topic, payload, qos, retain, priority);

        if (id > Integer.MAX_VALUE) {
            this.helper.execute(this.sqlDeleteMessage, id);
            this.helper.execute(this.sqlResetId);
            return (int) this.helper.store(topic, payload, qos, retain, priority);
        }

        return (int) id;

    }

    private void validate(String topic) throws KuraStoreException {
        if (topic == null || topic.trim().length() == 0) {
            throw new KuraStoreException(null, "topic must be not null and not empty");
        }
    }

    @Override
    public synchronized Optional<StoredMessage> get(int msgId) throws KuraStoreException {
        return this.helper.get(msgId);
    }

    @Override
    public synchronized Optional<StoredMessage> getNextMessage() throws KuraStoreException {
        return this.helper.getNextMessage();
    }

    @Override
    public synchronized void markAsPublished(int msgId, DataTransportToken token) throws KuraStoreException {
        this.helper.markAsPublished(msgId, token);
    }

    @Override
    public synchronized void markAsPublished(int msgId) throws KuraStoreException {
        this.helper.markAsPublished(msgId);
    }

    @Override
    public synchronized void markAsConfirmed(int msgId) throws KuraStoreException {
        this.helper.markAsConfirmed(msgId);
    }

    @Override
    public synchronized List<StoredMessage> getUnpublishedMessages() throws KuraStoreException {
        return this.helper.getUnpublishedMessages();
    }

    @Override
    public synchronized List<StoredMessage> getInFlightMessages() throws KuraStoreException {
        return this.helper.getInFlightMessages();
    }

    @Override
    public synchronized List<StoredMessage> getDroppedMessages() throws KuraStoreException {
        return this.helper.getDroppedMessages();
    }

    @Override
    public synchronized void unpublishAllInFlighMessages() throws KuraStoreException {
        this.helper.unpublishAllInFlighMessages();
    }

    @Override
    public synchronized void dropAllInFlightMessages() throws KuraStoreException {
        this.helper.dropAllInFlightMessages();
    }

    @Override
    public synchronized void deleteStaleMessages(int purgeAge) throws KuraStoreException {
        this.helper.deleteStaleMessages(purgeAge);
    }

    @Override
    public void close() {
        // nothing to close
    }

}
