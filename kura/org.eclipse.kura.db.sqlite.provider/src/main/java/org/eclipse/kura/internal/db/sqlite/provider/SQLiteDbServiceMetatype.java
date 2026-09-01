/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.db.sqlite.provider;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.db.SQLiteDbService", name = "SqliteDbService", description = "SQLite based database service.")
public @interface SQLiteDbServiceMetatype {

    @AttributeDefinition(name = "Database Mode", options = { @Option(label = "In Memory", value = "IN_MEMORY"), @Option(label = "Persisted", value = "PERSISTED") }, description = "Defines the database mode. If set to In Memory, the database data will be stored in RAM only, and it will be lost if the service instance is removed or if the framework is restarted. If set to Persisted, the database data will be stored on the file system, in the location defined by the Persisted Database Path parameter.")
    String db_mode() default "IN_MEMORY";

    @AttributeDefinition(name = "Persisted Database Path", description = "Defines the database path. The parameter value should be set to the absolute path to the database file (it should end with the database file name, it is not enough to specify only the parent directory). This parameter is only relevant for persisted databases.")
    String db_path() default "/opt/mydb.sqlite";

    @AttributeDefinition(name = "Encryption Key", type = AttributeType.PASSWORD, required = false, description = "Allows to specify a key/passphrase for encrypting the database file. This feature requires a SQLite binary with an encryption extension, and is only relevant for persisted databases. The key format can be specified using the Encryption Key Format parameter. If the value of this parameter is changed, the encryption key of the database will be updated accordingly. This parameter can be left empty to create an unencrypted database or to decrypt an encrypted one.")
    String db_key();

    @AttributeDefinition(name = "Encryption Key Format", options = { @Option(label = "ASCII", value = "ASCII"), @Option(label = "Hex SSE", value = "HEX_SSE"), @Option(label = "Hex SQLCipher", value = "HEX_SQLCIPHER") }, description = "Allows to specify the format of the Encryption Key parameter value. The possible values are ASCII (an ASCII string), Hex SSE (the key is an hexadecimal string to be used with the SSE extension) or Hex SQLCipher (the key is an hexadecimal string to be used with the SQLCipher extension)")
    String db_key_format() default "ASCII";

    @AttributeDefinition(name = "Journal Mode", options = { @Option(label = "Rollback Journal", value = "ROLLBACK_JOURNAL"), @Option(label = "WAL", value = "WAL") }, description = "The database journal mode (see https://www.sqlite.org/pragma.html#pragma_journal_mode for more details). If set to Rollback Journal the DELETE journal mode will be used. This parameter is only relevant for persisted databases.")
    String db_journal_mode() default "WAL";

    @AttributeDefinition(name = "Defrag enabled", description = "Enables or disables the database defragmentation. Use the Defrag Interval parameter to specify the interval.")
    boolean db_defrag_enabled() default true;

    @AttributeDefinition(name = "Defrag Interval (seconds)", required = false, min = "60", description = "SqliteDbService instances support running periodic defragmentation using the VACUUM command (https://www.sqlite.org/lang_vacuum.html). This parameter specifies the interval in seconds beetween two consecutive defragmentations. This parameter is only relevant for persisted databases.")
    long db_defrag_interval_seconds() default 900L;

    @AttributeDefinition(name = "Checkpoint enabled", description = "Enables or disables checkpoints in WAL journal mode. Use the WAL Checkpoint Interval parameter to specify the interval.")
    boolean db_wal_checkpoint_enabled() default true;

    @AttributeDefinition(name = "WAL Checkpoint Interval (Seconds)", required = false, min = "60", description = "SqliteDbService instances support running periodic periodic WAL checkpoints (https://www.sqlite.org/pragma.html#pragma_wal_checkpoint). Checkpoints will be performed in TRUNCATE mode. This parameter specifies the interval in seconds beetween two consecutive checkpoints. This parameter is only relevant for persisted databases in WAL Journal Mode.")
    long db_wal_checkpoint_interval_seconds() default 600L;

    @AttributeDefinition(name = "Connection Pool Max Size", min = "1", description = "The SqliteDbService manages connections using a connection pool. This parameter defines the maximum number of connections for the pool. Only 1 connection is available in In Memory mode.")
    int db_connection_pool_max_size() default 10;

    @AttributeDefinition(name = "Delete Database Files On Failure", description = "If set to true, the database files will be deleted in case of failure in opening a persisted database. This is intended as a last resort measure for keeping the database service operational, especially in the case when it is used as a cloud connection message store.")
    boolean delete_db_files_on_failure() default true;

    @AttributeDefinition(name = "Debug Shell Access Enabled", description = "Enables or disables the interaction with this database instance using the sqlitedbg OSGi console command")
    boolean debug_shell_access_enabled() default false;

}


