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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
    public void shouldNotExtractNativeLibrariesInJavaTempdir()
            throws InterruptedException, ExecutionException, TimeoutException {
        givenSqliteDbService(Collections.emptyMap());

        whenAConnectionIsRequested();

        thenThereIsNoSqliteLibraryInJavaTempdir();
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
    public void shouldDisableWalCheckpoint()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "WAL", //
                "db.path", temporaryDirectory() + "/test.sqlite", //
                "db.wal.checkpoint.interval.seconds", 5L, //
                "db.wal.checkpoint.enabled", false));

        givenExecutedQuery("CREATE TABLE FOO (BAR INTEGER);");
        givenExecutedQuery("INSERT INTO FOO VALUES (1);");
        givenExecutedQuery("INSERT INTO FOO VALUES (1);");
        givenExecutedQuery("INSERT INTO FOO VALUES (2);");
        givenExecutedQuery("INSERT INTO FOO VALUES (3);");

        whenTimePasses(10, TimeUnit.SECONDS);

        thenFileExists(temporaryDirectory() + "/test.sqlite-wal");
        thenFileSizeIsNotZero(temporaryDirectory() + "/test.sqlite-wal");
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
    public void shouldDisableDefragInRollbackJournalMode()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "ROLLBACK_JOURNAL", //
                "db.path", temporaryDirectory() + "/test.sqlite", //
                "db.defrag.interval.seconds", 5L, //
                "db.defrag.enabled", false //
        ));

        givenExecutedQuery("CREATE TABLE FOO (BAR TEXT);");
        givenExecutedQuery("INSERT INTO FOO VALUES ('" + largeText(20000) + "');");
        givenExecutedQuery("INSERT INTO FOO VALUES ('" + largeText(20000) + "');");
        givenExecutedQuery("INSERT INTO FOO VALUES ('" + largeText(20000) + "');");
        givenExecutedQuery("INSERT INTO FOO VALUES ('" + largeText(20000) + "');");
        givenInitialFileSize(temporaryDirectory() + "/test.sqlite");
        givenExecutedQuery("DELETE FROM FOO;");

        whenTimePasses(10, TimeUnit.SECONDS);

        thenFileSizeDidNotChange(temporaryDirectory() + "/test.sqlite");
    }

    @Test
    public void shouldDisableDefragInWalMode()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "WAL", //
                "db.path", temporaryDirectory() + "/waldefrag.sqlite", //
                "db.defrag.interval.seconds", 5L, //
                "db.defrag.enabled", false //
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

        thenFileSizeDidNotChange(temporaryDirectory() + "/waldefrag.sqlite");
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

    @Test
    public void deleteDbFileIfItCannotBeOpenedRollbackJournal()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        givenFileWithContent(temporaryDirectory() + "/test.sqlite", "foobar");
        givenFileWithContent(temporaryDirectory() + "/test.sqlite-journal", "foobar");
        givenFileWithContent(temporaryDirectory() + "/test.sqlite-shm", "foobar");
        givenFileWithContent(temporaryDirectory() + "/test.sqlite-wal", "foobar");
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "ROLLBACK_JOURNAL", //
                "db.path", temporaryDirectory() + "/test.sqlite" //
        ));
        whenAConnectionIsRequested();

        thenNoExceptionIsThrown();
        thenFileContentIsNot(temporaryDirectory() + "/test.sqlite", "foobar");
        thenFileContentIsNot(temporaryDirectory() + "/test.sqlite-journal", "foobar");
        thenFileContentIsNot(temporaryDirectory() + "/test.sqlite-shm", "foobar");
        thenFileContentIsNot(temporaryDirectory() + "/test.sqlite-wal", "foobar");
    }

    @Test
    public void deleteDbFileIfItCannotBeOpenedWAL()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        givenFileWithContent(temporaryDirectory() + "/test.sqlite", "foobar");
        givenFileWithContent(temporaryDirectory() + "/test.sqlite-journal", "foobar");
        givenFileWithContent(temporaryDirectory() + "/test.sqlite-shm", "foobar");
        givenFileWithContent(temporaryDirectory() + "/test.sqlite-wal", "foobar");
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "WAL", //
                "db.path", temporaryDirectory() + "/test.sqlite" //
        ));
        whenAConnectionIsRequested();

        thenNoExceptionIsThrown();
        thenFileContentIsNot(temporaryDirectory() + "/test.sqlite", "foobar");
        thenFileContentIsNot(temporaryDirectory() + "/test.sqlite-journal", "foobar");
        thenFileContentIsNot(temporaryDirectory() + "/test.sqlite-shm", "foobar");
        thenFileContentIsNot(temporaryDirectory() + "/test.sqlite-wal", "foobar");
    }

    @Test
    public void deleteNotDbFileIfItCannotBeOpenedIFNotEnabledRollbackJournal()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        givenFileWithContent(temporaryDirectory() + "/test.sqlite", "foobar");
        givenFileWithContent(temporaryDirectory() + "/test.sqlite-journal", "foobar");
        givenFileWithContent(temporaryDirectory() + "/test.sqlite-shm", "foobar");
        givenFileWithContent(temporaryDirectory() + "/test.sqlite-wal", "foobar");
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "ROLLBACK_JOURNAL", //
                "db.path", temporaryDirectory() + "/test.sqlite", //
                "delete.db.files.on.failure", false) //
        );
        whenAConnectionIsRequested();

        thenExceptionIsThrown();
        thenFileContentIs(temporaryDirectory() + "/test.sqlite", "foobar");
    }

    @Test
    public void deleteNotDbFileIfItCannotBeOpenedIFNotEnabledWal()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        givenFileWithContent(temporaryDirectory() + "/test.sqlite", "foobar");
        givenFileWithContent(temporaryDirectory() + "/test.sqlite-journal", "foobar");
        givenFileWithContent(temporaryDirectory() + "/test.sqlite-shm", "foobar");
        givenFileWithContent(temporaryDirectory() + "/test.sqlite-wal", "foobar");
        givenSqliteDbService(map( //
                "db.mode", "PERSISTED", //
                "db.journal.mode", "WAL", //
                "db.path", temporaryDirectory() + "/test.sqlite", //
                "delete.db.files.on.failure", false) //
        );
        whenAConnectionIsRequested();

        thenExceptionIsThrown();
        thenFileContentIs(temporaryDirectory() + "/test.sqlite", "foobar");
    }

    private void givenFileWithContent(final String file, final String content) throws IOException {
        try (final FileOutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void thenThereIsNoSqliteLibraryInJavaTempdir() {
        final File javaTempDir = new File(System.getProperty("java.io.tmpdir"));

        assertArrayEquals(new String[] {}, javaTempDir.list((dir, name) -> name.startsWith("sqlite-")));
    }

    private void thenFileContentIs(final String path, final String content) throws IOException {
        final byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        final byte[] actualContent = new byte[contentBytes.length + 1];

        final int len = fillBuffer(new File(path), actualContent);

        assertEquals(contentBytes.length, len);
        assertArrayEquals(contentBytes, Arrays.copyOfRange(actualContent, 0, len));
    }

    private void thenFileContentIsNot(final String path, final String content) throws IOException {
        final byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        final byte[] actualContent = new byte[contentBytes.length + 1];

        final File file = new File(path);

        if (!file.exists()) {
            return;
        }

        final int len = fillBuffer(file, actualContent);

        if (contentBytes.length == len) {
            assertFalse(Arrays.equals(contentBytes, Arrays.copyOfRange(actualContent, 0, len)));
        }
    }

    private int fillBuffer(final File file, final byte[] dst) throws FileNotFoundException, IOException {
        int pos = 0;

        try (final FileInputStream in = new FileInputStream(file)) {

            int rd;

            while (pos < dst.length && (rd = in.read(dst, pos, dst.length - pos)) != -1) {
                pos += rd;
            }
        }

        return pos;
    }

}
