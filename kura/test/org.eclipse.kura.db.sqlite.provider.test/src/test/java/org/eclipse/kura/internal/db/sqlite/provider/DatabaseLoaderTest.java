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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.db.sqlite.provider.SqliteDbServiceOptions.EncryptionKeyFormat;
import org.eclipse.kura.internal.db.sqlite.provider.SqliteDbServiceOptions.EncryptionKeySpec;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.HexKeyMode;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteDataSource;

public class DatabaseLoaderTest {

    private static final String DB_KEY_FORMAT = "db.key.format";

    @Test
    public void shouldOpenInMemoryDatabase() {
        givenNewOptionsProperty(DB_MODE, "IN_MEMORY");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();
        thenOpenDataSourceInvocationCountIs(1);
        thenOpenDataSourceInvocationUrlIs(0, "jdbc:sqlite:file:foo?mode=memory&cache=shared");
        thenOpenDataSourceInvocationKeyIs(0, Optional.empty());

        thenThereAreNoEntriesInCryptoService();
    }

    @Test
    public void shouldOpenUnencryptedDatabaseIfNoPasswordsAreStored() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenUnencryptedDatabase();

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();

        thenOpenDataSourceInvocationCountIs(1);
        thenOpenDataSourceInvocationUrlIs(0, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(0, JournalMode.WAL);
        thenOpenDataSourceInvocationKeyIs(0, Optional.empty());

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY, "");
        thenCryptoServiceUpdateCountIs(1);
    }

    @Test
    public void shouldSupportRollbackJournalMode() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenNewOptionsProperty("db.journal.mode", "ROLLBACK_JOURNAL");
        givenUnencryptedDatabase();

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();

        thenOpenDataSourceInvocationCountIs(1);
        thenOpenDataSourceInvocationUrlIs(0, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(0, JournalMode.DELETE);
        thenOpenDataSourceInvocationKeyIs(0, Optional.empty());

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY, "");
        thenCryptoServiceUpdateCountIs(1);
    }

    @Test
    public void shouldOpenUnencryptedDatabaseWithPasswordInOldOptions() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");

        givenOldOptionsProperty(DB_KEY, TEST_ENCRYPTION_KEY);

        givenUnencryptedDatabase();

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();
        thenOpenDataSourceInvocationCountIs(2);

        thenOpenDataSourceInvocationUrlIs(0, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(0, JournalMode.WAL);
        thenOpenDataSourceInvocationKeyIs(0,
                Optional.of(new EncryptionKeySpec(TEST_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII)));

        thenOpenDataSourceInvocationUrlIs(1, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(1, JournalMode.WAL);
        thenOpenDataSourceInvocationKeyIs(1, Optional.empty());

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY, "");
    }

    @Test
    public void shouldOpenUnencryptedDatabaseWithPasswordInCrypto() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");

        givenCryptoServiceEntry(TEST_DB_CRYPTO_ENTRY_KEY, "ASCII:" + TEST_ENCRYPTION_KEY);

        givenUnencryptedDatabase();

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();
        thenOpenDataSourceInvocationCountIs(2);

        thenOpenDataSourceInvocationUrlIs(0, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(0, JournalMode.WAL);
        thenOpenDataSourceInvocationKeyIs(0,
                Optional.of(new EncryptionKeySpec(TEST_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII)));

        thenOpenDataSourceInvocationUrlIs(1, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(1, JournalMode.WAL);
        thenOpenDataSourceInvocationKeyIs(1, Optional.empty());

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY, "");
    }

    @Test
    public void shouldOpenEncryptedDatabaseIfNoPasswordsAreStoredWithAsciiKey() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenNewOptionsProperty(DB_KEY, TEST_ENCRYPTION_KEY);

        givenEncryptedDatabase(new EncryptionKeySpec(TEST_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII));

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();

        thenOpenDataSourceInvocationCountIs(1);
        thenOpenDataSourceInvocationUrlIs(0, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(0, JournalMode.WAL);
        thenOpenDataSourceInvocationKeyIs(0,
                Optional.of(new EncryptionKeySpec(TEST_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII)));

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY,
                EncryptionKeyFormat.ASCII.name() + ":" + TEST_ENCRYPTION_KEY);
    }

    @Test
    public void shouldOpenEncryptedDatabaseIfNoPasswordsAreStoredWithSSEHekKey() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenNewOptionsProperty(DB_KEY, TEST_HEX_ENCRYPTION_KEY);
        givenNewOptionsProperty(DB_KEY_FORMAT, EncryptionKeyFormat.HEX_SSE.name());

        givenEncryptedDatabase(
                new EncryptionKeySpec(TEST_HEX_ENCRYPTION_KEY.toUpperCase(), EncryptionKeyFormat.HEX_SSE));

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();

        thenOpenDataSourceInvocationCountIs(1);
        thenOpenDataSourceInvocationUrlIs(0, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(0, JournalMode.WAL);
        thenOpenDataSourceInvocationKeyIs(0,
                Optional.of(new EncryptionKeySpec(TEST_HEX_ENCRYPTION_KEY.toUpperCase(), EncryptionKeyFormat.HEX_SSE)));

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY,
                EncryptionKeyFormat.HEX_SSE.name() + ":" + TEST_HEX_ENCRYPTION_KEY.toUpperCase());
    }

    @Test
    public void shouldOpenEncryptedDatabaseIfNoPasswordsAreStoredWithSQLCipherHekKey() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenNewOptionsProperty(DB_KEY, TEST_HEX_ENCRYPTION_KEY);
        givenNewOptionsProperty(DB_KEY_FORMAT, EncryptionKeyFormat.HEX_SQLCIPHER.name());

        givenEncryptedDatabase(
                new EncryptionKeySpec(TEST_HEX_ENCRYPTION_KEY.toUpperCase(), EncryptionKeyFormat.HEX_SQLCIPHER));

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();

        thenOpenDataSourceInvocationCountIs(1);
        thenOpenDataSourceInvocationUrlIs(0, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(0, JournalMode.WAL);
        thenOpenDataSourceInvocationKeyIs(0, Optional
                .of(new EncryptionKeySpec(TEST_HEX_ENCRYPTION_KEY.toUpperCase(), EncryptionKeyFormat.HEX_SQLCIPHER)));

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY,
                EncryptionKeyFormat.HEX_SQLCIPHER.name() + ":" + TEST_HEX_ENCRYPTION_KEY.toUpperCase());
    }

    @Test
    public void shouldDecryptEncryptedDatabaseWithPasswordInOldOptions() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenOldOptionsProperty(DB_KEY, TEST_ENCRYPTION_KEY);

        givenEncryptedDatabase(new EncryptionKeySpec(TEST_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII));

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();

        thenOpenDataSourceInvocationCountIs(2);
        thenOpenDataSourceInvocationUrlIs(0, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(0, JournalMode.WAL);
        thenOpenDataSourceInvocationKeyIs(0,
                Optional.of(new EncryptionKeySpec(TEST_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII)));

        thenOpenDataSourceInvocationUrlIs(1, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(1, JournalMode.WAL);
        thenOpenDataSourceInvocationKeyIs(1, Optional.empty());

        thenQueryIsExecuted(0, "PRAGMA rekey = '';");

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY, "");

    }

    @Test
    public void shouldDecryptEncryptedDatabaseWithPasswordInCrypto() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenOldOptionsProperty(DB_KEY, TEST_ENCRYPTION_KEY);

        givenCryptoServiceEntry(TEST_DB_CRYPTO_ENTRY_KEY, "ASCII:" + TEST_ENCRYPTION_KEY);

        givenEncryptedDatabase(new EncryptionKeySpec(TEST_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII));

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();

        thenOpenDataSourceInvocationCountIs(2);
        thenOpenDataSourceInvocationUrlIs(0, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(0, JournalMode.WAL);
        thenOpenDataSourceInvocationKeyIs(0,
                Optional.of(new EncryptionKeySpec(TEST_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII)));

        thenOpenDataSourceInvocationUrlIs(1, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(1, JournalMode.WAL);
        thenOpenDataSourceInvocationKeyIs(1, Optional.empty());

        thenQueryIsExecuted(0, "PRAGMA rekey = '';");

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY, "");
    }

    @Test
    public void shouldSupportRekeyWithAsciiKey() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenOldOptionsProperty(DB_KEY, TEST_ENCRYPTION_KEY);
        givenNewOptionsProperty(DB_KEY, "otherkey");

        givenEncryptedDatabase(new EncryptionKeySpec(TEST_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII));

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();

        thenOpenDataSourceInvocationCountIs(3);

        thenOpenDataSourceInvocationKeyIs(0, Optional.of(new EncryptionKeySpec("otherkey", EncryptionKeyFormat.ASCII)));
        thenOpenDataSourceInvocationKeyIs(1,
                Optional.of(new EncryptionKeySpec(TEST_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII)));
        thenOpenDataSourceInvocationKeyIs(2, Optional.of(new EncryptionKeySpec("otherkey", EncryptionKeyFormat.ASCII)));

        thenQueryIsExecuted(0, "PRAGMA rekey = 'otherkey';");

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY, "ASCII:otherkey");
    }

    @Test
    public void shouldSupportRekeyWithSseHexKey() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenOldOptionsProperty(DB_KEY, TEST_HEX_ENCRYPTION_KEY);
        givenNewOptionsProperty(DB_KEY, "cc");
        givenNewOptionsProperty(DB_KEY_FORMAT, EncryptionKeyFormat.HEX_SSE.name());

        givenEncryptedDatabase(new EncryptionKeySpec(TEST_HEX_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII));

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();

        thenOpenDataSourceInvocationCountIs(3);

        thenOpenDataSourceInvocationKeyIs(0, Optional.of(new EncryptionKeySpec("CC", EncryptionKeyFormat.HEX_SSE)));
        thenOpenDataSourceInvocationKeyIs(1,
                Optional.of(new EncryptionKeySpec(TEST_HEX_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII)));
        thenOpenDataSourceInvocationKeyIs(2, Optional.of(new EncryptionKeySpec("CC", EncryptionKeyFormat.HEX_SSE)));

        thenQueryIsExecuted(0, "PRAGMA hexrekey = 'CC';");

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY, "HEX_SSE:CC");
    }

    @Test
    public void shouldSupportRekeyWithSqlcipherHexKey() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenOldOptionsProperty(DB_KEY, TEST_HEX_ENCRYPTION_KEY);
        givenNewOptionsProperty(DB_KEY, "bb");
        givenNewOptionsProperty(DB_KEY_FORMAT, EncryptionKeyFormat.HEX_SQLCIPHER.name());

        givenEncryptedDatabase(new EncryptionKeySpec(TEST_HEX_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII));

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();

        thenOpenDataSourceInvocationCountIs(3);

        thenOpenDataSourceInvocationKeyIs(0,
                Optional.of(new EncryptionKeySpec("BB", EncryptionKeyFormat.HEX_SQLCIPHER)));
        thenOpenDataSourceInvocationKeyIs(1,
                Optional.of(new EncryptionKeySpec(TEST_HEX_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII)));
        thenOpenDataSourceInvocationKeyIs(2,
                Optional.of(new EncryptionKeySpec("BB", EncryptionKeyFormat.HEX_SQLCIPHER)));

        thenQueryIsExecuted(0, "PRAGMA rekey = \"x'BB'\";");

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY, "HEX_SQLCIPHER:BB");
    }

    @Test
    public void shouldNotUpdateCryptoServiceStoredKeysIfNotNeeded() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenNewOptionsProperty(DB_KEY, TEST_ENCRYPTION_KEY);

        givenEncryptedDatabase(new EncryptionKeySpec(TEST_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII));
        givenCryptoServiceEntry(TEST_DB_CRYPTO_ENTRY_KEY, EncryptionKeyFormat.ASCII.name() + ":" + TEST_ENCRYPTION_KEY);

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();

        thenOpenDataSourceInvocationCountIs(1);
        thenOpenDataSourceInvocationUrlIs(0, TEST_DB_URL);
        thenOpenDataSourceInvocationJournalModeIs(0, JournalMode.WAL);
        thenOpenDataSourceInvocationKeyIs(0,
                Optional.of(new EncryptionKeySpec(TEST_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII)));

        thenCryptoServiceUpdateCountIs(0);
    }

    @Test
    public void shouldFailIfCorrectEncryptionKeyIsUnknown() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenNewOptionsProperty(DB_KEY, "first");
        givenOldOptionsProperty(DB_KEY, "second");
        givenCryptoServiceEntry(TEST_DB_CRYPTO_ENTRY_KEY, EncryptionKeyFormat.ASCII.name() + ":third");

        givenEncryptedDatabase(new EncryptionKeySpec(TEST_ENCRYPTION_KEY, EncryptionKeyFormat.ASCII));

        whenDataSourceIsCreated();

        thenExceptionIsThrown(SQLException.class);
    }

    @Test
    public void shouldFailWithNonAsciiEncryptionKey() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenNewOptionsProperty(DB_KEY_FORMAT, EncryptionKeyFormat.ASCII.name());
        givenNewOptionsProperty(DB_KEY, "🐖");

        whenDataSourceIsCreated();

        thenExceptionIsThrown(KuraException.class);
        thenExceptionMessageContains("non ASCII");
    }

    @Test
    public void shouldFailWithHexEncryptionKeyLengthNotAMultipleOfTwo() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenNewOptionsProperty(DB_KEY_FORMAT, EncryptionKeyFormat.HEX_SSE.name());
        givenNewOptionsProperty(DB_KEY, "abc");

        whenDataSourceIsCreated();

        thenExceptionIsThrown(KuraException.class);
        thenExceptionMessageContains("multiple of 2");
    }

    @Test
    public void shouldFailWithHexEncryptionKeyWithNotAllowedCharacters() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenNewOptionsProperty(DB_KEY_FORMAT, EncryptionKeyFormat.HEX_SSE.name());
        givenNewOptionsProperty(DB_KEY, "foobar");

        whenDataSourceIsCreated();

        thenExceptionIsThrown(KuraException.class);
        thenExceptionMessageContains("only digits and or letters from \"a\" to \"f\"");
    }

    @Test
    public void shouldNotDeleteDatabaseFilesIfKeyIsInvalid() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenNewOptionsProperty(DB_KEY_FORMAT, EncryptionKeyFormat.HEX_SSE.name());
        givenNewOptionsProperty(DB_KEY, "foobar");

        whenDataSourceIsCreated();

        thenExceptionIsThrown(KuraException.class);
        thenDatabaseFilesHaveBeenDeleted(false);
    }

    @Test
    public void shouldRunVacuumAfterRekeyInRollbackJournalMode() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenNewOptionsProperty(DB_KEY, "otherkey");
        givenNewOptionsProperty(DB_KEY_FORMAT, DB_KEY);
        givenNewOptionsProperty(JOURNAL_MODE, "ROLLBACK_JOURNAL");

        givenUnencryptedDatabase();

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();

        thenOpenDataSourceInvocationCountIs(3);

        thenOpenDataSourceInvocationKeyIs(0, Optional.of(new EncryptionKeySpec("otherkey", EncryptionKeyFormat.ASCII)));
        thenOpenDataSourceInvocationKeyIs(1, Optional.empty());
        thenOpenDataSourceInvocationKeyIs(2, Optional.of(new EncryptionKeySpec("otherkey", EncryptionKeyFormat.ASCII)));

        thenExecutedQueryCountIs(2);
        thenQueryIsExecuted(0, "PRAGMA rekey = 'otherkey';");
        thenQueryIsExecuted(1, "VACUUM;");

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY, "ASCII:otherkey");
    }

    @Test
    public void shouldRunVacuumAfterRekeyWalMode() {
        givenNewOptionsProperty(DB_MODE, PERSISTED);
        givenNewOptionsProperty(DB_PATH, "foo");
        givenNewOptionsProperty(KURA_SERVICE_PID, "foo");
        givenNewOptionsProperty(DB_KEY, "otherkey");
        givenNewOptionsProperty(DB_KEY_FORMAT, DB_KEY);
        givenNewOptionsProperty(JOURNAL_MODE, "WAL");

        givenUnencryptedDatabase();

        whenDataSourceIsCreated();

        thenNoExceptionIsThrown();

        thenOpenDataSourceInvocationCountIs(3);

        thenOpenDataSourceInvocationKeyIs(0, Optional.of(new EncryptionKeySpec("otherkey", EncryptionKeyFormat.ASCII)));
        thenOpenDataSourceInvocationKeyIs(1, Optional.empty());
        thenOpenDataSourceInvocationKeyIs(2, Optional.of(new EncryptionKeySpec("otherkey", EncryptionKeyFormat.ASCII)));

        thenExecutedQueryCountIs(3);
        thenQueryIsExecuted(0, "PRAGMA rekey = 'otherkey';");
        thenQueryIsExecuted(1, "VACUUM;");
        thenQueryIsExecuted(2, "PRAGMA wal_checkpoint(TRUNCATE);");

        thenCryptoServiceEntryIs(TEST_DB_CRYPTO_ENTRY_KEY, "ASCII:otherkey");
    }

    private static final Logger logger = LoggerFactory.getLogger(DatabaseLoaderTest.class);

    private static final String TEST_DB_CRYPTO_ENTRY_KEY = "sqlite:db:foo";
    private static final String PERSISTED = "PERSISTED";
    private static final String TEST_ENCRYPTION_KEY = "foobar";
    private static final String TEST_HEX_ENCRYPTION_KEY = "aaBB33";
    private static final String DB_KEY = "db.key";
    private static final String DB_PATH = "db.path";
    private static final String KURA_SERVICE_PID = "kura.service.pid";
    private static final String DB_MODE = "db.mode";
    private static final String JOURNAL_MODE = "db.journal.mode";
    private static final String TEST_DB_URL = "jdbc:sqlite:file:foo";

    private final CryptoService cryptoService = mock(CryptoService.class);

    private Map<String, Object> currentProperties = new HashMap<>();
    private Optional<Map<String, Object>> oldProperties = Optional.empty();
    private Map<String, String> cryptoServiceEntries = new HashMap<>();

    private Optional<Exception> exception = Optional.empty();
    private DatabaseLoader loader;

    private Map<String, Optional<EncryptionKeySpec>> databaseEncryptionKeys = new HashMap<>();

    private final List<SQLiteConfig> sqliteConfigs = new ArrayList<>();
    private final List<SQLiteDataSource> dataSources = new ArrayList<>();
    private final List<String> queries = new ArrayList<>();
    private int cryptoServiceUpdateCount = 0;
    private boolean databaseFilesDeleted = false;

    public DatabaseLoaderTest() {
        when(cryptoService.getKeyStorePassword(any())).thenAnswer(i -> Optional
                .ofNullable(cryptoServiceEntries.get(i.getArgument(0))).map(String::toCharArray).orElse(null));

        try {
            when(cryptoService.decryptAes(any(char[].class))).then(i -> i.getArgument(0));

            Mockito.doAnswer(i -> {
                cryptoServiceUpdateCount++;
                cryptoServiceEntries.put(i.getArgument(0), new String(i.getArgument(1, char[].class)));
                return null;
            }).when(cryptoService).setKeyStorePassword(any(), any(char[].class));
        } catch (KuraException e) {
            throw new IllegalStateException();
        }
    }

    private void givenEncryptedDatabase(final EncryptionKeySpec encryptionKey) {
        givenEncryptedDatabase(TEST_DB_URL, encryptionKey);
    }

    private void givenUnencryptedDatabase() {
        givenUnencryptedDatabase(TEST_DB_URL);
    }

    private void givenEncryptedDatabase(final String path, final EncryptionKeySpec encryptionKey) {
        this.databaseEncryptionKeys.put(path, Optional.of(encryptionKey));
    }

    private void givenUnencryptedDatabase(final String path) {
        this.databaseEncryptionKeys.put(path, Optional.empty());
    }

    private void givenCryptoServiceEntry(final String key, final String value) {
        this.cryptoServiceEntries.put(key, value);
    }

    private void givenNewOptionsProperty(final String key, final String value) {
        currentProperties.put(key, value);
    }

    private void givenOldOptionsProperty(final String key, final String value) {

        if (!oldProperties.isPresent()) {
            oldProperties = Optional.of(new HashMap<>());
        }

        oldProperties.get().put(key, value);
    }

    private void givenDatabaseLoader() {

        this.loader = new DatabaseLoader(new SqliteDbServiceOptions(currentProperties),
                oldProperties.map(SqliteDbServiceOptions::new), cryptoService) {

            @Override
            protected void changeEncryptionKey(SQLiteDataSource dataSource, Optional<EncryptionKeySpec> encryptionKey,
                    final SqliteDbServiceOptions options) throws SQLException {
                super.changeEncryptionKey(dataSource, encryptionKey, options);
                databaseEncryptionKeys.put(dataSource.getUrl(), encryptionKey);
            }

            @Override
            protected SQLiteDataSource buildDataSource(final SQLiteConfig config) {

                try {
                    sqliteConfigs.add(config);

                    final Statement stmt = mock(Statement.class);

                    when(stmt.execute(any())).thenAnswer(i -> {
                        queries.add(i.getArgument(0));
                        return false;
                    });

                    when(stmt.executeUpdate(any())).then(i -> {
                        queries.add(i.getArgument(0));
                        return 0;
                    });

                    final Connection connection = mock(Connection.class);

                    when(connection.createStatement()).thenReturn(stmt);

                    final SQLiteDataSource dataSource = new SQLiteDataSource(config) {

                        @Override
                        public Connection getConnection() throws SQLException {

                            final String url = getUrl();

                            final Optional<EncryptionKeySpec> currentKey = databaseEncryptionKeys.get(url);

                            if (url.contains("mode=memory") || Objects.equals(currentKey, fromSqliteConfig(config))) {
                                return connection;
                            } else {
                                throw new SQLException();
                            }
                        }
                    };

                    dataSources.add(dataSource);

                    return dataSource;
                } catch (SQLException e) {
                    throw new IllegalStateException();
                }

            }

            @Override
            protected void deleteFile(File file) throws IOException {
                databaseFilesDeleted = true;
            }
        };

    }

    private void whenDataSourceIsCreated() {
        givenDatabaseLoader();
        try {
            this.loader.openDataSource();
        } catch (Exception e) {
            logger.info("got exception", e);
            this.exception = Optional.of(e);
        }
    }

    private void thenThereAreNoEntriesInCryptoService() {
        assertTrue(this.cryptoServiceEntries.isEmpty());
    }

    private void thenCryptoServiceEntryIs(final String key, final String value) {
        assertEquals(value, this.cryptoServiceEntries.get(key));
    }

    private void thenOpenDataSourceInvocationCountIs(final int count) {
        assertEquals(count, this.sqliteConfigs.size());
    }

    private void thenOpenDataSourceInvocationUrlIs(final int index, final String path) {
        assertEquals(path, this.dataSources.get(index).getUrl());
    }

    private void thenOpenDataSourceInvocationKeyIs(final int index, final Optional<EncryptionKeySpec> key) {
        assertEquals(key, fromSqliteConfig(this.sqliteConfigs.get(index)));
    }

    private void thenOpenDataSourceInvocationJournalModeIs(final int index, final JournalMode journalMode) {
        assertEquals(journalMode,
                JournalMode.valueOf(this.sqliteConfigs.get(index).toProperties().getProperty("journal_mode")));
    }

    private void thenDatabaseFilesHaveBeenDeleted(final boolean deleted) {
        assertEquals(deleted, this.databaseFilesDeleted);
    }

    private void thenNoExceptionIsThrown() {
        if (this.exception.isPresent()) {
            this.exception.get().printStackTrace();
        }

        assertEquals(Optional.empty(), this.exception);
    }

    private void thenQueryIsExecuted(final int index, final String query) {
        assertEquals(query, queries.get(index));
    }

    private void thenExecutedQueryCountIs(final int expectedCount) {
        assertEquals(expectedCount, queries.size());
    }

    private void thenCryptoServiceUpdateCountIs(final int expectedCount) {
        assertEquals(expectedCount, this.cryptoServiceUpdateCount);
    }

    private void thenExceptionIsThrown(final Class<? extends Exception> classz) {
        if (!this.exception.isPresent()) {
            fail("Exception expected");
        }

        assertEquals(classz, this.exception.get().getClass());
    }

    private void thenExceptionMessageContains(final String messageContent) {
        assertTrue(this.exception.get().getMessage().contains(messageContent));
    }

    private Optional<EncryptionKeySpec> fromSqliteConfig(final SQLiteConfig config) {
        final Properties properties = config.toProperties();

        final Optional<String> key = Optional.ofNullable(properties.getProperty("password"));
        final Optional<HexKeyMode> hexKeyMode = Optional.ofNullable(properties.getProperty("hexkey_mode"))
                .map(HexKeyMode::valueOf);

        if (key.isPresent() && hexKeyMode.isPresent()) {
            EncryptionKeyFormat format;

            if (hexKeyMode.get() == HexKeyMode.NONE) {
                format = EncryptionKeyFormat.ASCII;
            } else if (hexKeyMode.get() == HexKeyMode.SSE) {
                format = EncryptionKeyFormat.HEX_SSE;
            } else if (hexKeyMode.get() == HexKeyMode.SQLCIPHER) {
                format = EncryptionKeyFormat.HEX_SQLCIPHER;
            } else {
                throw new IllegalStateException();
            }

            return Optional.of(new EncryptionKeySpec(key.get(), format));
        } else {
            return Optional.empty();
        }

    }
}
