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
import java.sql.Statement;

import org.eclipse.kura.internal.db.sqlite.provider.SqliteDbServiceOptions.JournalMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqliteUtil {

    private static final String VACUUM_STATEMENT = "VACUUM;";
    private static final String WAL_CHECKPOINT_STATEMENT = "PRAGMA wal_checkpoint(TRUNCATE);";

    private static final Logger logger = LoggerFactory.getLogger(SqliteUtil.class);

    private SqliteUtil() {
    }

    public static void walCeckpoint(final Connection connection, final SqliteDbServiceOptions options) {
        logger.info("performing WAL checkpoint on database with url: {}...", options.getDbUrl());

        try (final Statement statement = connection.createStatement()) {
            statement.executeUpdate(WAL_CHECKPOINT_STATEMENT);
        } catch (final Exception e) {
            logger.warn("WAL checkpoint failed", e);
        }

        logger.info("performing WAL checkpoint on database with url: {}...done", options.getDbUrl());
    }

    public static void vacuum(final Connection connection, final SqliteDbServiceOptions options) {
        logger.info("defragmenting database with url: {}...", options.getDbUrl());

        try (final Statement statement = connection.createStatement()) {

            statement.executeUpdate(VACUUM_STATEMENT);

        } catch (final Exception e) {
            logger.warn("VACUUM command failed", e);
        }

        if (options.getJournalMode() == JournalMode.WAL) {
            walCeckpoint(connection, options);
        }

        logger.info("defragmenting database with url: {}...done", options.getDbUrl());
    }
}
