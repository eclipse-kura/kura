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

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.util.message.store.AbstractJdbcMessageStoreImpl;
import org.eclipse.kura.util.message.store.JdbcMessageStoreQueries;

@SuppressWarnings("restriction")
public class SqliteMessageStoreImpl extends AbstractJdbcMessageStoreImpl {

    private static final String CREATE_INDEX_IF_NOT_EXISTS = "CREATE INDEX IF NOT EXISTS ";

    private static final String UPDATE = "UPDATE ";

    private static final String DELETE_FROM = "DELETE FROM ";

    private static final String SELECT_MESSAGE_METADATA_FROM = "SELECT id, topic, qos, retain, createdOn, publishedOn, "
            + "publishedMessageId, confirmedOn, priority, sessionId, droppedOn FROM ";

    private final String sqlResetId;
    private final String sqlDeleteMessage;

    public SqliteMessageStoreImpl(final ConnectionProvider provider, final String table) throws KuraStoreException {
        super(provider, table);

        this.sqlResetId = UPDATE + " sqlite_sequence SET seq = 0 WHERE name = " + this.escapedTableName + ";";
        this.sqlDeleteMessage = DELETE_FROM + super.escapedTableName + " WHERE id = ?;";

        createTable();
        createIndexes();
    }

    @Override
    protected JdbcMessageStoreQueries buildSqlMessageStoreQueries() {

        return JdbcMessageStoreQueries.builder()
                .withSqlCreateTable("CREATE TABLE IF NOT EXISTS " + super.escapedTableName
                        + " (id INTEGER PRIMARY KEY AUTOINCREMENT, topic VARCHAR, qos INTEGER, retain BOOLEAN, "
                        + "createdOn DATETIME, publishedOn DATETIME, publishedMessageId INTEGER, confirmedOn DATETIME, "
                        + "payload BLOB, priority INTEGER, sessionId VARCHAR, droppedOn DATETIME);")
                .withSqlMessageCount("SELECT COUNT(*) FROM " + super.escapedTableName + ";")
                .withSqlStore("INSERT INTO " + escapedTableName
                        + " (topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, payload, priority, "
                        + "sessionId, droppedOn) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")
                .withSqlGetMessage(
                        "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, "
                                + "payload, priority, sessionId, droppedOn FROM " + super.escapedTableName
                                + " WHERE id = ?")
                .withSqlGetNextMessage("SELECT a.id, a.topic, a.qos, a.retain, a.createdOn, a.publishedOn, "
                        + "a.publishedMessageId, a.confirmedOn, a.payload, a.priority, a.sessionId, a.droppedOn FROM "
                        + escapedTableName + " AS a JOIN (SELECT id, publishedOn FROM " + super.escapedTableName
                        + " ORDER BY publishedOn ASC NULLS FIRST, priority ASC, createdOn ASC LIMIT 1) AS b "
                        + "WHERE a.id = b.id AND b.publishedOn IS NULL;")
                .withSqlSetPublishedQoS1(UPDATE + super.escapedTableName
                        + " SET publishedOn = ?, publishedMessageId = ?, sessionId = ? WHERE id = ?;")
                .withSqlSetPublishedQoS0(UPDATE + super.escapedTableName + " SET publishedOn = ? WHERE id = ?;")
                .withSqlSetConfirmed(UPDATE + escapedTableName + " SET confirmedOn = ? WHERE id = ?;")
                .withSqlAllUnpublishedMessages(SELECT_MESSAGE_METADATA_FROM + super.escapedTableName
                        + " WHERE publishedOn IS NULL ORDER BY priority ASC, createdOn ASC;")
                .withSqlAllInFlightMessages(SELECT_MESSAGE_METADATA_FROM + super.escapedTableName
                        + " WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL AND droppedOn IS NULL "
                        + "ORDER BY priority ASC, createdOn ASC")
                .withSqlAllDroppedInFlightMessages(SELECT_MESSAGE_METADATA_FROM + super.escapedTableName
                        + " WHERE droppedOn IS NOT NULL ORDER BY priority ASC, createdOn ASC;")
                .withSqlUnpublishAllInFlightMessages(UPDATE + super.escapedTableName
                        + " SET publishedOn = NULL WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;")
                .withSqlDropAllInFlightMessages(UPDATE + super.escapedTableName
                        + " SET droppedOn = ? WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;")
                .withSqlDeleteDroppedMessages(
                        DELETE_FROM + super.escapedTableName + " WHERE droppedOn <= ? AND droppedOn IS NOT NULL;")
                .withSqlDeleteConfirmedMessages(
                        DELETE_FROM + super.escapedTableName + " WHERE confirmedOn <= ? AND confirmedOn IS NOT NULL;")
                .withSqlDeletePublishedMessages(DELETE_FROM + super.escapedTableName
                        + " WHERE qos = 0 AND publishedOn <= ? AND publishedOn IS NOT NULL;")
                .withSqlCreateNextMessageIndex(
                        CREATE_INDEX_IF_NOT_EXISTS + super.escapeIdentifier(super.tableName + "_nextMsg") + " ON "
                                + this.escapedTableName + " (publishedOn ASC, priority ASC, createdOn ASC, qos);")
                .withSqlCreatePublishedOnIndex(
                        CREATE_INDEX_IF_NOT_EXISTS + super.escapeIdentifier(super.tableName + "_PUBLISHEDON") + " ON "
                                + this.escapedTableName + " (publishedOn DESC);")
                .withSqlCreateConfirmedOnIndex(
                        CREATE_INDEX_IF_NOT_EXISTS + super.escapeIdentifier(super.tableName + "_CONFIRMEDON") + " ON "
                                + this.escapedTableName + " (confirmedOn DESC);")
                .withSqlCreateDroppedOnIndex(
                        CREATE_INDEX_IF_NOT_EXISTS + super.escapeIdentifier(super.tableName + "_DROPPEDON") + " ON "
                                + this.escapedTableName + " (droppedOn DESC);")
                .build();
    }

    @Override
    public synchronized int store(String topic, byte[] payload, int qos, boolean retain, int priority)
            throws KuraStoreException {

        validate(topic);

        final long id = super.storeInternal(topic, payload, qos, retain, priority);

        if (id > Integer.MAX_VALUE) {
            super.execute(this.sqlDeleteMessage, id);

            if (super.getMessageCountInternal() >= Integer.MAX_VALUE) {
                throw new KuraStoreException("Table size is greater or equal than integer max value");
            }

            super.execute(this.sqlResetId);
            return (int) super.storeInternal(topic, payload, qos, retain, priority);
        }

        return (int) id;

    }

}
