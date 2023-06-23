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

public final class JdbcMessageStoreQueries {

    private final String sqlCreateTable;
    private final String sqlMessageCount;
    private final String sqlStore;
    private final String sqlGetMessage;
    private final String sqlGetNextMessage;
    private final String sqlSetPublishedQoS1;
    private final String sqlSetPublishedQoS0;
    private final String sqlSetConfirmed;
    private final String sqlAllUnpublishedMessages;
    private final String sqlAllInFlightMessages;
    private final String sqlAllDroppedInFlightMessages;
    private final String sqlUnpublishAllInFlightMessages;
    private final String sqlDropAllInFlightMessages;
    private final String sqlDeleteDroppedMessages;
    private final String sqlDeleteConfirmedMessages;
    private final String sqlDeletePublishedMessages;
    private final String sqlCreateNextMessageIndex;
    private final String sqlCreatePublishedOnIndex;
    private final String sqlCreateConfirmedOnIndex;
    private final String sqlCreateDroppedOnIndex;

    private JdbcMessageStoreQueries(Builder builder) {
        this.sqlCreateTable = requireNonNull(builder.sqlCreateTable);
        this.sqlMessageCount = requireNonNull(builder.sqlMessageCount);
        this.sqlStore = requireNonNull(builder.sqlStore);
        this.sqlGetMessage = requireNonNull(builder.sqlGetMessage);
        this.sqlGetNextMessage = requireNonNull(builder.sqlGetNextMessage);
        this.sqlSetPublishedQoS1 = requireNonNull(builder.sqlSetPublishedQoS1);
        this.sqlSetPublishedQoS0 = requireNonNull(builder.sqlSetPublishedQoS0);
        this.sqlSetConfirmed = requireNonNull(builder.sqlSetConfirmed);
        this.sqlAllUnpublishedMessages = requireNonNull(builder.sqlAllUnpublishedMessages);
        this.sqlAllInFlightMessages = requireNonNull(builder.sqlAllInFlightMessages);
        this.sqlAllDroppedInFlightMessages = requireNonNull(builder.sqlAllDroppedInFlightMessages);
        this.sqlUnpublishAllInFlightMessages = requireNonNull(builder.sqlUnpublishAllInFlightMessages);
        this.sqlDropAllInFlightMessages = requireNonNull(builder.sqlDropAllInFlightMessages);
        this.sqlDeleteDroppedMessages = requireNonNull(builder.sqlDeleteDroppedMessages);
        this.sqlDeleteConfirmedMessages = requireNonNull(builder.sqlDeleteConfirmedMessages);
        this.sqlDeletePublishedMessages = requireNonNull(builder.sqlDeletePublishedMessages);
        this.sqlCreateNextMessageIndex = requireNonNull(builder.sqlCreateNextMessageIndex);
        this.sqlCreatePublishedOnIndex = requireNonNull(builder.sqlCreatePublishedOnIndex);
        this.sqlCreateConfirmedOnIndex = requireNonNull(builder.sqlCreateConfirmedOnIndex);
        this.sqlCreateDroppedOnIndex = requireNonNull(builder.sqlCreateDroppedOnIndex);
    }

    public String getSqlCreateTable() {
        return sqlCreateTable;
    }

    public String getSqlMessageCount() {
        return sqlMessageCount;
    }

    public String getSqlStore() {
        return sqlStore;
    }

    public String getSqlGetMessage() {
        return sqlGetMessage;
    }

    public String getSqlGetNextMessage() {
        return sqlGetNextMessage;
    }

    public String getSqlSetPublishedQoS1() {
        return sqlSetPublishedQoS1;
    }

    public String getSqlSetPublishedQoS0() {
        return sqlSetPublishedQoS0;
    }

    public String getSqlSetConfirmed() {
        return sqlSetConfirmed;
    }

    public String getSqlAllUnpublishedMessages() {
        return sqlAllUnpublishedMessages;
    }

    public String getSqlAllInFlightMessages() {
        return sqlAllInFlightMessages;
    }

    public String getSqlAllDroppedInFlightMessages() {
        return sqlAllDroppedInFlightMessages;
    }

    public String getSqlUnpublishAllInFlightMessages() {
        return sqlUnpublishAllInFlightMessages;
    }

    public String getSqlDropAllInFlightMessages() {
        return sqlDropAllInFlightMessages;
    }

    public String getSqlDeleteDroppedMessages() {
        return sqlDeleteDroppedMessages;
    }

    public String getSqlDeleteConfirmedMessages() {
        return sqlDeleteConfirmedMessages;
    }

    public String getSqlDeletePublishedMessages() {
        return sqlDeletePublishedMessages;
    }

    public String getSqlCreateNextMessageIndex() {
        return sqlCreateNextMessageIndex;
    }

    public String getSqlCreatePublishedOnIndex() {
        return sqlCreatePublishedOnIndex;
    }

    public String getSqlCreateConfirmedOnIndex() {
        return sqlCreateConfirmedOnIndex;
    }

    public String getSqlCreateDroppedOnIndex() {
        return sqlCreateDroppedOnIndex;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String sqlCreateTable;
        private String sqlMessageCount;
        private String sqlStore;
        private String sqlGetMessage;
        private String sqlGetNextMessage;
        private String sqlSetPublishedQoS1;
        private String sqlSetPublishedQoS0;
        private String sqlSetConfirmed;
        private String sqlAllUnpublishedMessages;
        private String sqlAllInFlightMessages;
        private String sqlAllDroppedInFlightMessages;
        private String sqlUnpublishAllInFlightMessages;
        private String sqlDropAllInFlightMessages;
        private String sqlDeleteDroppedMessages;
        private String sqlDeleteConfirmedMessages;
        private String sqlDeletePublishedMessages;
        private String sqlCreateNextMessageIndex;
        private String sqlCreatePublishedOnIndex;
        private String sqlCreateConfirmedOnIndex;
        private String sqlCreateDroppedOnIndex;

        public Builder withSqlCreateTable(String sqlCreateTable) {
            this.sqlCreateTable = sqlCreateTable;
            return this;
        }

        public Builder withSqlMessageCount(String sqlMessageCount) {
            this.sqlMessageCount = sqlMessageCount;
            return this;
        }

        public Builder withSqlStore(String sqlStore) {
            this.sqlStore = sqlStore;
            return this;
        }

        public Builder withSqlGetMessage(String sqlGetMessage) {
            this.sqlGetMessage = sqlGetMessage;
            return this;
        }

        public Builder withSqlGetNextMessage(String sqlGetNextMessage) {
            this.sqlGetNextMessage = sqlGetNextMessage;
            return this;
        }

        public Builder withSqlSetPublishedQoS1(String sqlSetPublishedQoS1) {
            this.sqlSetPublishedQoS1 = sqlSetPublishedQoS1;
            return this;
        }

        public Builder withSqlSetPublishedQoS0(String sqlSetPublishedQoS0) {
            this.sqlSetPublishedQoS0 = sqlSetPublishedQoS0;
            return this;
        }

        public Builder withSqlSetConfirmed(String sqlSetConfirmed) {
            this.sqlSetConfirmed = sqlSetConfirmed;
            return this;
        }

        public Builder withSqlAllUnpublishedMessages(String sqlAllUnpublishedMessages) {
            this.sqlAllUnpublishedMessages = sqlAllUnpublishedMessages;
            return this;
        }

        public Builder withSqlAllInFlightMessages(String sqlAllInFlightMessages) {
            this.sqlAllInFlightMessages = sqlAllInFlightMessages;
            return this;
        }

        public Builder withSqlAllDroppedInFlightMessages(String sqlAllDroppedInFlightMessages) {
            this.sqlAllDroppedInFlightMessages = sqlAllDroppedInFlightMessages;
            return this;
        }

        public Builder withSqlUnpublishAllInFlightMessages(String sqlUnpublishAllInFlightMessages) {
            this.sqlUnpublishAllInFlightMessages = sqlUnpublishAllInFlightMessages;
            return this;
        }

        public Builder withSqlDropAllInFlightMessages(String sqlDropAllInFlightMessages) {
            this.sqlDropAllInFlightMessages = sqlDropAllInFlightMessages;
            return this;
        }

        public Builder withSqlDeleteDroppedMessages(String sqlDeleteDroppedMessages) {
            this.sqlDeleteDroppedMessages = sqlDeleteDroppedMessages;
            return this;
        }

        public Builder withSqlDeleteConfirmedMessages(String sqlDeleteConfirmedMessages) {
            this.sqlDeleteConfirmedMessages = sqlDeleteConfirmedMessages;
            return this;
        }

        public Builder withSqlDeletePublishedMessages(String sqlDeletePublishedMessages) {
            this.sqlDeletePublishedMessages = sqlDeletePublishedMessages;
            return this;
        }

        public Builder withSqlCreateNextMessageIndex(String sqlCreateNextMessageIndex) {
            this.sqlCreateNextMessageIndex = sqlCreateNextMessageIndex;
            return this;
        }

        public Builder withSqlCreatePublishedOnIndex(String sqlCreatePublishedOnIndex) {
            this.sqlCreatePublishedOnIndex = sqlCreatePublishedOnIndex;
            return this;
        }

        public Builder withSqlCreateConfirmedOnIndex(String sqlCreateConfirmedOnIndex) {
            this.sqlCreateConfirmedOnIndex = sqlCreateConfirmedOnIndex;
            return this;
        }

        public Builder withSqlCreateDroppedOnIndex(String sqlCreateDroppedOnIndex) {
            this.sqlCreateDroppedOnIndex = sqlCreateDroppedOnIndex;
            return this;
        }

        public JdbcMessageStoreQueries build() {
            return new JdbcMessageStoreQueries(this);
        }
    }

}
