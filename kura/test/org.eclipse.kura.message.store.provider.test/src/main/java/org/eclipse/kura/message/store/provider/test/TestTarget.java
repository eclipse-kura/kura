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
package org.eclipse.kura.message.store.provider.test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.db.BaseDbService;
import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.message.store.provider.MessageStoreProvider;

public interface TestTarget {

    public static final TestTarget H2 = new H2();
    public static final TestTarget SQLITE = new Sqlite();

    public String storeFactoryPid();

    public Map<String, Object> getConfigurationForPid(final String pid);

    public void setMessageId(final String pid, final String collection, final int value);

    public class H2 implements TestTarget {

        @Override
        public String storeFactoryPid() {
            return "org.eclipse.kura.core.db.H2DbService";
        }

        @Override
        public Map<String, Object> getConfigurationForPid(String pid) {

            return Collections.singletonMap("db.connector.url", "jdbc:h2:mem:testdb-" + pid);
        }

        @Override
        public void setMessageId(final String pid, final String collection, final int value) {

            try {
                final H2DbService dbService = ServiceUtil
                        .trackService(H2DbService.class, Optional.of("(kura.service.pid=" + pid + ")"))
                        .get(30, TimeUnit.SECONDS);

                try (final Connection c = dbService.getConnection(); final Statement stmt = c.createStatement()) {
                    stmt.executeUpdate(
                            "ALTER TABLE \"" + collection + "\" ALTER COLUMN id RESTART WITH " + value + ";");
                    c.commit();
                }

                ((MessageStoreProvider) dbService).openMessageStore(collection).store("foo", null, 1, false, 1);
            } catch (Exception e) {
                throw new IllegalStateException("cannot set next message id", e);
            }

        }

        @Override
        public String toString() {
            return "H2";
        }

    }

    public class Sqlite implements TestTarget {

        @Override
        public String storeFactoryPid() {

            return "org.eclipse.kura.db.SQLiteDbService";
        }

        @Override
        public Map<String, Object> getConfigurationForPid(String pid) {

            return Collections.emptyMap();
        }

        @Override
        public void setMessageId(final String pid, final String collection, final int value) {
            try {
                final BaseDbService dbService = ServiceUtil
                        .trackService(BaseDbService.class, Optional.of("(kura.service.pid=" + pid + ")"))
                        .get(30, TimeUnit.SECONDS);

                try (final Connection c = dbService.getConnection(); final Statement stmt = c.createStatement()) {
                    stmt.executeUpdate(
                            "UPDATE sqlite_sequence SET seq = " + value + " WHERE name = \"" + collection + "\";");
                }

            } catch (Exception e) {
                throw new IllegalStateException("cannot set next message id", e);
            }
        }

        @Override
        public String toString() {
            return "SQLite";
        }

    }
}
