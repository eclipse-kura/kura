/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.db.sqlite.provider.test;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraException;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;

public class SqliteDbServiceImplTest extends SqliteDbServiceTestBase {

    public SqliteDbServiceImplTest() throws InterruptedException, ExecutionException, TimeoutException {
        super();
    }

    @Test
    public void shouldCreateDbServiceWithDefaultConfig()
            throws InterruptedException, ExecutionException, TimeoutException {
        givenSqliteDbService(Collections.emptyMap());

        whenAConnectionIsRequested();

        thenNoExceptionIsThrown();
    }

    @Test
    public void shouldSupportRollbackJournalMode()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "ROLLBACK_JOURNAL", //
                "db.path", temporaryDirectory() + "/test.sqlite" //
        ));

        whenQueryIsPerformed("CREATE TABLE FOO (BAR INTEGER);");

        thenNoExceptionIsThrown();
        thenFileExists(temporaryDirectory() + "/test.sqlite");
        thenFileDoesNotExist(temporaryDirectory() + "/test.sqlite-shm");
        thenFileDoesNotExist(temporaryDirectory() + "/test.sqlite-wal");
    }

    @Test
    public void shouldSupportWalJournalMode()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "WAL", //
                "db.path", temporaryDirectory() + "/test.sqlite" //
        ));

        whenQueryIsPerformed("CREATE TABLE FOO (BAR INTEGER);");

        thenNoExceptionIsThrown();
        thenFileExists(temporaryDirectory() + "/test.sqlite");
        thenFileExists(temporaryDirectory() + "/test.sqlite-shm");
        thenFileExists(temporaryDirectory() + "/test.sqlite-wal");
    }

    @Test
    public void shouldSupportWalCheckpoint()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "WAL", //
                "db.path", temporaryDirectory() + "/test.sqlite", //
                "db.wal.checkpoint.interval.seconds", 5L //
        ));

        givenExecutedQuery("CREATE TABLE FOO (BAR INTEGER);");
        givenExecutedQuery("INSERT INTO FOO VALUES (1);");
        givenExecutedQuery("INSERT INTO FOO VALUES (1);");
        givenExecutedQuery("INSERT INTO FOO VALUES (2);");
        givenExecutedQuery("INSERT INTO FOO VALUES (3);");

        whenTimePasses(10, TimeUnit.SECONDS);

        thenFileExists(temporaryDirectory() + "/test.sqlite-wal");
        thenFileSizeIs(temporaryDirectory() + "/test.sqlite-wal", 0);
    }

    @Test
    public void shouldSupporDefragInRollbackJournalMode()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "ROLLBACK_JOURNAL", //
                "db.path", temporaryDirectory() + "/test.sqlite", //
                "db.defrag.interval.seconds", 5L //
        ));

        givenExecutedQuery("CREATE TABLE FOO (BAR TEXT);");
        givenExecutedQuery("INSERT INTO FOO VALUES ('" + largeText(20000) + "');");
        givenExecutedQuery("INSERT INTO FOO VALUES ('" + largeText(20000) + "');");
        givenExecutedQuery("INSERT INTO FOO VALUES ('" + largeText(20000) + "');");
        givenExecutedQuery("INSERT INTO FOO VALUES ('" + largeText(20000) + "');");
        givenInitialFileSize(temporaryDirectory() + "/test.sqlite");
        givenExecutedQuery("DELETE FROM FOO;");

        whenTimePasses(10, TimeUnit.SECONDS);

        thenFileSizeDecreased(temporaryDirectory() + "/test.sqlite");
    }

    @Test
    public void shouldSupporDefragInWalMode()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "WAL", //
                "db.path", temporaryDirectory() + "/waldefrag.sqlite", //
                "db.defrag.interval.seconds", 5L //
        ));

        givenExecutedQuery("CREATE TABLE FOO (BAR TEXT);");
        givenExecutedQuery("INSERT INTO FOO (BAR) VALUES ('" + largeText(2000) + "');");
        givenExecutedQuery("INSERT INTO FOO (BAR) VALUES ('" + largeText(2000) + "');");
        givenExecutedQuery("INSERT INTO FOO (BAR) VALUES ('" + largeText(2000) + "');");
        givenExecutedQuery("INSERT INTO FOO (BAR) VALUES ('" + largeText(2000) + "');");
        givenExecutedQuery("PRAGMA wal_checkpoint(TRUNCATE);");
        givenInitialFileSize(temporaryDirectory() + "/waldefrag.sqlite");
        givenExecutedQuery("DELETE FROM FOO;");
        givenExecutedQuery("PRAGMA wal_checkpoint(TRUNCATE);");

        whenTimePasses(10, TimeUnit.SECONDS);

        thenFileSizeDecreased(temporaryDirectory() + "/waldefrag.sqlite");
    }

    @Test
    public void shouldSupportSwitchFromRollbackJournalToWal() throws InterruptedException, ExecutionException,
            TimeoutException, IOException, KuraException, InvalidSyntaxException {
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "ROLLBACK_JOURNAL", //
                "db.path", temporaryDirectory() + "/test.sqlite" //
        ));
        givenExecutedQuery("CREATE TABLE FOO (BAR INTEGER);");
        givenExecutedQuery("INSERT INTO FOO VALUES (1);");

        whenSqliteDbServiceIsUpdated(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "WAL", //
                "db.path", temporaryDirectory() + "/test.sqlite", //
                "db.wal.checkpoint.interval.seconds", 5L //
        ));
        whenQueryIsPerformed("INSERT INTO FOO VALUES (2);");

        thenNoExceptionIsThrown();
        thenFileExists(temporaryDirectory() + "/test.sqlite");
        thenFileExists(temporaryDirectory() + "/test.sqlite-shm");
        thenFileExists(temporaryDirectory() + "/test.sqlite-wal");
    }

    @Test
    public void shouldSupportSwitchFromWalToRollbackJournal() throws InterruptedException, ExecutionException,
            TimeoutException, IOException, KuraException, InvalidSyntaxException {
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "WAL", //
                "db.path", temporaryDirectory() + "/test.sqlite", //
                "db.wal.checkpoint.interval.seconds", 5L //
        ));

        givenExecutedQuery("CREATE TABLE FOO (BAR INTEGER);");
        givenExecutedQuery("INSERT INTO FOO VALUES (1);");

        whenSqliteDbServiceIsUpdated(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "ROLLBACK_JOURNAL", //
                "db.path", temporaryDirectory() + "/test.sqlite" //
        ));
        whenQueryIsPerformed("INSERT INTO FOO VALUES (2);");

        thenNoExceptionIsThrown();
        thenFileExists(temporaryDirectory() + "/test.sqlite");
        thenFileDoesNotExist(temporaryDirectory() + "/test.sqlite-shm");
        thenFileDoesNotExist(temporaryDirectory() + "/test.sqlite-wal");
    }

    @Test
    public void shoulNotSupportTwoIntancesOnSameDbPath()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "ROLLBACK_JOURNAL", //
                "db.path", temporaryDirectory() + "/test.sqlite" //
        ));
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "ROLLBACK_JOURNAL", //
                "db.path", temporaryDirectory() + "/test.sqlite" //
        ));

        whenAConnectionIsRequested();

        thenExceptionIsThrown();
    }

}
