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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.db.sqlite.provider.SqliteDbServiceOptions.EncryptionKeyFormat;
import org.eclipse.kura.internal.db.sqlite.provider.SqliteDbServiceOptions.EncryptionKeySpec;
import org.eclipse.kura.internal.db.sqlite.provider.SqliteDbServiceOptions.JournalMode;
import org.eclipse.kura.internal.db.sqlite.provider.SqliteDbServiceOptions.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

public class DatabaseLoader {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseLoader.class);

    private final SqliteDbServiceOptions newOptions;
    private final Optional<SqliteDbServiceOptions> oldOptions;
    private final CryptoService cryptoService;

    public DatabaseLoader(final SqliteDbServiceOptions newOptions, final Optional<SqliteDbServiceOptions> oldOptions,
            final CryptoService cryptoService) {
        this.newOptions = newOptions;
        this.oldOptions = oldOptions;
        this.cryptoService = cryptoService;
    }

    public SQLiteDataSource openDataSource() throws SQLException, KuraException {
        try {
            return openDataSourceInternal();
        } catch (final Exception e) {

            final boolean isConfigurationAttributeInvalidException = (e instanceof KuraException
                    && ((KuraException) e).getCode() == KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID);

            if (this.newOptions.isDeleteDbFilesOnFailure() && this.newOptions.getMode() != Mode.IN_MEMORY
                    && !isConfigurationAttributeInvalidException) {
                logger.warn("failed to open database, deleting database files and retrying", e);
                deleteDbFiles(newOptions.getPath());
                return openDataSourceInternal();
            }

            throw e;
        }

    }

    protected SQLiteDataSource openDataSourceInternal() throws SQLException, KuraException {

        final String dbUrl = this.newOptions.getDbUrl();

        if (newOptions.getMode() != Mode.PERSISTED) {
            return openDataSource(dbUrl, Optional.empty(), Optional.empty());
        }

        final List<Optional<EncryptionKeySpec>> applicableEncryptionKeys = new ArrayList<>();

        final Optional<EncryptionKeySpec> keyFromNewOptions = this.newOptions.getEncryptionKey(cryptoService);

        addEncryptionKey(applicableEncryptionKeys, keyFromNewOptions, "key from new options");

        if (this.oldOptions.isPresent()) {
            try {
                addEncryptionKey(applicableEncryptionKeys, this.oldOptions.get().getEncryptionKey(cryptoService),
                        "key from old options");
            } catch (final Exception e) {
                logger.warn("failed to get key from old options", e);
            }
        }

        addEncryptionKey(applicableEncryptionKeys, getCryptoServiceEntry(newOptions.getPath()),
                "key from CryptoService");

        applicableEncryptionKeys.add(Optional.empty());

        Exception lastException = null;
        final Optional<JournalMode> journalMode = Optional.of(this.newOptions.getJournalMode());

        for (final Optional<EncryptionKeySpec> encryptionKey : applicableEncryptionKeys) {
            try {
                SQLiteDataSource result = openDataSource(dbUrl, encryptionKey, journalMode);

                if (!encryptionKey.equals(keyFromNewOptions)) {
                    changeEncryptionKey(result, keyFromNewOptions, newOptions);
                    result = openDataSource(dbUrl, keyFromNewOptions, journalMode);
                }

                updateCryptoServiceEntry(newOptions.getPath(), keyFromNewOptions);

                return result;
            } catch (final Exception e) {
                logger.debug("database open attempt failed", e);
                lastException = e;
                // try next one
            }
        }

        throw new SQLException("Failed to open database", lastException);
    }

    private void addEncryptionKey(final List<Optional<EncryptionKeySpec>> applicableEncryptionKeys,
            final Optional<EncryptionKeySpec> keyFromNewOptions, final String type) {
        if (keyFromNewOptions.isPresent()) {
            logger.debug("adding {}", type);
            applicableEncryptionKeys.add(keyFromNewOptions);
        }
    }

    private void updateCryptoServiceEntry(final String dbPath, final Optional<EncryptionKeySpec> keyFromOptions)
            throws KuraException {
        final String entryKey = getCryptoServicePasswordEntryKey(dbPath);
        final char[] entryValue = encodeCrtpyoServicePasswordEntry(keyFromOptions).toCharArray();

        final char[] currentValue = this.cryptoService.getKeyStorePassword(entryKey);

        if (!Arrays.equals(entryValue, currentValue)) {
            this.cryptoService.setKeyStorePassword(entryKey, entryValue);
        }
    }

    private String getCryptoServicePasswordEntryKey(final String dbPath) {
        return "sqlite:db:" + dbPath;
    }

    private String encodeCrtpyoServicePasswordEntry(final Optional<EncryptionKeySpec> encryptionKey) {
        if (encryptionKey.isPresent()) {
            return encryptionKey.get().getFormat().name() + ":" + encryptionKey.get().getKey();
        } else {
            return "";
        }
    }

    private Optional<EncryptionKeySpec> getCryptoServiceEntry(final String dbPath) {
        try {
            final String raw = new String(
                    this.cryptoService.getKeyStorePassword(getCryptoServicePasswordEntryKey(dbPath)));

            final int index = raw.indexOf(':');

            final EncryptionKeyFormat format = EncryptionKeyFormat.valueOf(raw.substring(0, index));
            final String key = raw.substring(index + 1);

            return Optional.of(new EncryptionKeySpec(key, format));
        } catch (final Exception e) {
            return Optional.empty();
        }
    }

    protected SQLiteDataSource openDataSource(final String url, final Optional<EncryptionKeySpec> encryptionKey,
            final Optional<JournalMode> journalMode) throws SQLException {
        final SQLiteConfig config = new SQLiteConfig();

        if (encryptionKey.isPresent()) {
            config.setPragma(SQLiteConfig.Pragma.PASSWORD, encryptionKey.get().getKey());
            config.setHexKeyMode(encryptionKey.get().getFormat().toHexKeyMode());
        }

        if (journalMode.isPresent()) {
            config.setJournalMode(journalMode.get() == JournalMode.ROLLBACK_JOURNAL ? SQLiteConfig.JournalMode.DELETE
                    : SQLiteConfig.JournalMode.WAL);
        }

        final SQLiteDataSource dataSource = buildDataSource(config);
        dataSource.setUrl(url);

        dataSource.getConnection().close();
        return dataSource;
    }

    protected void changeEncryptionKey(final SQLiteDataSource dataSource,
            final Optional<EncryptionKeySpec> encryptionKey, final SqliteDbServiceOptions options) throws SQLException {
        logger.info("Updating encryption key for {}", dataSource.getUrl());

        try (final Connection connection = dataSource.getConnection()) {

            if (encryptionKey.isPresent()) {
                final EncryptionKeySpec encryptionKeySpec = encryptionKey.get();

                final String key = encryptionKey.get().getKey().replace("'", "''");

                if (encryptionKeySpec.getFormat() == EncryptionKeyFormat.HEX_SQLCIPHER) {
                    executeQuery(connection, "PRAGMA rekey = \"x'" + key + "'\";");
                } else if (encryptionKeySpec.getFormat() == EncryptionKeyFormat.HEX_SSE) {
                    executeQuery(connection, "PRAGMA hexrekey = '" + key + "';");
                } else {
                    executeQuery(connection, "PRAGMA rekey = '" + key + "';");
                }
            } else {
                executeQuery(connection, "PRAGMA rekey = '';");
            }

            SqliteUtil.vacuum(connection, options);
        }

    }

    protected void deleteDbFiles(final String dbPath) {
        final List<String> paths = Arrays.asList(dbPath, dbPath + "-wal", dbPath + "-journal", dbPath + "-shm");

        for (final String path : paths) {
            try {
                deleteFile(new File(path));
            } catch (final Exception e) {
                logger.warn("failed to delete database file", e);
            }
        }
    }

    protected void executeQuery(final Connection connection, final String query) throws SQLException {
        try (final Statement stmt = connection.createStatement()) {
            stmt.execute(query);
        }
    }

    protected SQLiteDataSource buildDataSource(final SQLiteConfig config) {
        return new SQLiteDataSource(config);
    }

    protected void deleteFile(final File file) throws IOException {
        if (file.exists()) {
            logger.info("deleting database file: {}", file.getAbsolutePath());
            Files.delete(file.toPath());
        }
    }

}
