/*******************************************************************************
 * Copyright (c) 2022, 2023 Eurotech and/or its affiliates and others
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

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.util.configuration.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig.HexKeyMode;

class SqliteDbServiceOptions {

    private static final String ENCRYPTION_KEY = "Encryption Key";
    private static final Logger logger = LoggerFactory.getLogger(SqliteDbServiceOptions.class);

    private static final Pattern HEX_PATTERN = Pattern.compile("[0-9a-fA-F]+");

    public enum Mode {
        IN_MEMORY,
        PERSISTED
    }

    public enum JournalMode {
        ROLLBACK_JOURNAL,
        WAL
    }

    public enum EncryptionKeyFormat {

        ASCII,
        HEX_SSE,
        HEX_SQLCIPHER;

        public HexKeyMode toHexKeyMode() {
            if (this == EncryptionKeyFormat.ASCII) {
                return HexKeyMode.NONE;
            } else if (this == EncryptionKeyFormat.HEX_SSE) {
                return HexKeyMode.SSE;
            } else if (this == EncryptionKeyFormat.HEX_SQLCIPHER) {
                return HexKeyMode.SQLCIPHER;
            } else {
                throw new IllegalStateException();
            }
        }
    }

    public static class EncryptionKeySpec {

        private final String key;
        private final EncryptionKeyFormat format;

        public EncryptionKeySpec(final String key, final EncryptionKeyFormat format) {
            this.key = key;
            this.format = format;
        }

        public String getKey() {
            return this.key;
        }

        public EncryptionKeyFormat getFormat() {
            return this.format;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.format, this.key);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof EncryptionKeySpec)) {
                return false;
            }
            EncryptionKeySpec other = (EncryptionKeySpec) obj;
            return this.format == other.format && Objects.equals(this.key, other.key);
        }

    }

    private static final Property<String> MODE_PROPERTY = new Property<>("db.mode", Mode.IN_MEMORY.name());
    private static final Property<String> PATH_PROPERTY = new Property<>("db.path", "/opt/mydb.sqlite");
    private static final Property<Integer> CONNECTION_POOL_MAX_SIZE_PROPERTY = new Property<>(
            "db.connection.pool.max.size", 10);
    private static final Property<String> JOURNAL_MODE_PROPERTY = new Property<>("db.journal.mode",
            JournalMode.WAL.name());
    private static final Property<Boolean> DEFRAG_ENABLED_PROPERTY = new Property<>("db.defrag.enabled", true);
    private static final Property<Long> DEFRAG_INTERVAL_SECONDS_PROPERTY = new Property<>("db.defrag.interval.seconds",
            900L);
    private static final Property<Boolean> WAL_CHECHPOINT_ENABLED_PROPERTY = new Property<>("db.wal.checkpoint.enabled",
            true);

    private static final Property<Long> WAL_CHECKPOINT_INTERVAL_SECONDS_PROPERTY = new Property<>(
            "db.wal.checkpoint.interval.seconds", 600L);
    private static final Property<Boolean> DEBUG_SHELL_ACCESS_ENABLED_PROPERTY = new Property<>(
            "debug.shell.access.enabled", false);
    private static final Property<String> ENCRYPTION_KEY_PROPERTY = new Property<>("db.key", String.class);
    private static final Property<String> ENCRYPTION_KEY_FORMAT_PROPERTY = new Property<>("db.key.format",
            EncryptionKeyFormat.ASCII.name());
    private static final Property<Boolean> DELETE_DB_FILES_ON_FAILURE = new Property<>("delete.db.files.on.failure",
            true);
    private static final Property<String> KURA_SERVICE_PID_PROPERTY = new Property<>(
            ConfigurationService.KURA_SERVICE_PID, "sqlitedb");

    private final Mode mode;
    private final String path;
    private final String kuraServicePid;
    private final boolean isDebugShellAccessEnabled;
    private final boolean defragEnabled;
    private final long defragIntervalSeconds;
    private final boolean walCheckpointEnabled;
    private final long walCheckpointIntervalSeconds;
    private final int maxConnectionPoolSize;
    private final JournalMode journalMode;
    private final Optional<String> encryptionKey;
    private final EncryptionKeyFormat encryptionKeyFormat;
    private final boolean deleteDbFilesOnFailure;

    public SqliteDbServiceOptions(Map<String, Object> properties) {
        this.mode = extractMode(properties);
        this.path = sanitizePath(PATH_PROPERTY.get(properties));
        this.kuraServicePid = KURA_SERVICE_PID_PROPERTY.get(properties);
        this.maxConnectionPoolSize = CONNECTION_POOL_MAX_SIZE_PROPERTY.get(properties);
        this.defragEnabled = DEFRAG_ENABLED_PROPERTY.get(properties);
        this.defragIntervalSeconds = DEFRAG_INTERVAL_SECONDS_PROPERTY.get(properties);
        this.walCheckpointEnabled = WAL_CHECHPOINT_ENABLED_PROPERTY.get(properties);
        this.walCheckpointIntervalSeconds = WAL_CHECKPOINT_INTERVAL_SECONDS_PROPERTY.get(properties);
        this.journalMode = extractJournalMode(properties);
        this.isDebugShellAccessEnabled = DEBUG_SHELL_ACCESS_ENABLED_PROPERTY.get(properties);
        this.encryptionKey = ENCRYPTION_KEY_PROPERTY.getOptional(properties).filter(s -> !s.trim().isEmpty());
        this.encryptionKeyFormat = extractEncryptionKeyFormat(properties);
        this.deleteDbFilesOnFailure = DELETE_DB_FILES_ON_FAILURE.get(properties);
    }

    public Mode getMode() {
        return this.mode;
    }

    public String getPath() {
        return this.path;
    }

    public int getConnectionPoolMaxSize() {
        return this.maxConnectionPoolSize;
    }

    public boolean isDebugShellAccessEnabled() {
        return this.isDebugShellAccessEnabled;
    }

    public long getDefragIntervalSeconds() {
        return this.defragIntervalSeconds;
    }

    public JournalMode getJournalMode() {
        return this.journalMode;
    }

    public String getKuraServicePid() {
        return this.kuraServicePid;
    }

    public long getWalCheckpointIntervalSeconds() {
        return this.walCheckpointIntervalSeconds;
    }

    public EncryptionKeyFormat getEncryptionKeyFormat() {
        return this.encryptionKeyFormat;
    }

    public boolean isDeleteDbFilesOnFailure() {
        return this.deleteDbFilesOnFailure;
    }

    public Optional<EncryptionKeySpec> getEncryptionKey(final CryptoService cryptoService) throws KuraException {
        if (this.encryptionKey.isPresent()) {
            String decrypted = new String(cryptoService.decryptAes(this.encryptionKey.get().toCharArray()));

            final EncryptionKeyFormat format = getEncryptionKeyFormat();

            decrypted = format == EncryptionKeyFormat.ASCII ? expectAscii(decrypted) : expectHexString(decrypted);

            return Optional.of(new EncryptionKeySpec(decrypted, format));
        } else {
            return Optional.empty();
        }
    }

    public boolean isPeriodicWalCheckpointEnabled() {
        return this.mode != Mode.IN_MEMORY && this.journalMode == JournalMode.WAL && this.walCheckpointEnabled;
    }

    public boolean isPeriodicDefragEnabled() {
        return this.mode != Mode.IN_MEMORY && this.defragEnabled;
    }

    private String expectAscii(final String value) throws KuraException {
        if (StandardCharsets.US_ASCII.newEncoder().canEncode(value)) {
            return value;
        } else {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, ENCRYPTION_KEY, "",
                    "Encryption key contains non ASCII characters");
        }
    }

    private String expectHexString(final String string) throws KuraException {
        if (string.length() % 2 != 0) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, ENCRYPTION_KEY, "",
                    "Hex encryption key length must be a multiple of 2");
        }

        if (!HEX_PATTERN.matcher(string).matches()) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, ENCRYPTION_KEY, "",
                    "Hex encryption must contain only digits and or letters from \"a\" to \"f\"");
        }

        return string.toUpperCase();
    }

    private static Mode extractMode(final Map<String, Object> properties) {
        try {
            return Mode.valueOf(MODE_PROPERTY.get(properties));
        } catch (final Exception e) {
            logger.warn("failed to parse db mode, falling back to IN_MEMORY", e);
            return Mode.IN_MEMORY;
        }
    }

    private static JournalMode extractJournalMode(final Map<String, Object> properties) {
        try {
            return JournalMode.valueOf(JOURNAL_MODE_PROPERTY.get(properties));
        } catch (final Exception e) {
            logger.warn("failed to parse db journal mode, falling back to ROLLBACK_JOURNAL", e);
            return JournalMode.ROLLBACK_JOURNAL;
        }
    }

    public String getDbUrl() {
        if (this.mode == Mode.PERSISTED) {
            return "jdbc:sqlite:file:" + this.path;
        } else {
            return "jdbc:sqlite:file:" + this.kuraServicePid + "?mode=memory&cache=shared";
        }
    }

    private static EncryptionKeyFormat extractEncryptionKeyFormat(final Map<String, Object> properties) {
        try {
            return EncryptionKeyFormat.valueOf(ENCRYPTION_KEY_FORMAT_PROPERTY.get(properties));
        } catch (final Exception e) {
            return EncryptionKeyFormat.ASCII;
        }
    }

    private static final String sanitizePath(final String path) {
        final int index = path.indexOf('?');

        if (index != -1) {
            return path.substring(0, index);
        } else {
            return path;
        }
    }

    public boolean isDefragEnabled() {
        return this.defragEnabled;
    }

    public boolean isWalCheckpointEnabled() {
        return this.walCheckpointEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.defragEnabled, this.defragIntervalSeconds, this.deleteDbFilesOnFailure,
                this.encryptionKey, this.encryptionKeyFormat, this.isDebugShellAccessEnabled, this.journalMode,
                this.kuraServicePid, this.maxConnectionPoolSize, this.mode, this.path, this.walCheckpointEnabled,
                this.walCheckpointIntervalSeconds);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        SqliteDbServiceOptions other = (SqliteDbServiceOptions) obj;
        return this.defragEnabled == other.defragEnabled && this.defragIntervalSeconds == other.defragIntervalSeconds
                && this.deleteDbFilesOnFailure == other.deleteDbFilesOnFailure
                && Objects.equals(this.encryptionKey, other.encryptionKey)
                && this.encryptionKeyFormat == other.encryptionKeyFormat
                && this.isDebugShellAccessEnabled == other.isDebugShellAccessEnabled
                && this.journalMode == other.journalMode && Objects.equals(this.kuraServicePid, other.kuraServicePid)
                && this.maxConnectionPoolSize == other.maxConnectionPoolSize && this.mode == other.mode
                && Objects.equals(this.path, other.path) && this.walCheckpointEnabled == other.walCheckpointEnabled
                && this.walCheckpointIntervalSeconds == other.walCheckpointIntervalSeconds;
    }

}
