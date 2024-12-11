# SQLite Db Service

Starting from version 5.3, Kura provides provides an integration of the SQLite database, based on the org.xerial:sqlite-jdbc wrapper .

The database integration is not included in the official distribution, but it can be downloaded from Eclipse Marketplace as a deployment package.

## Supported Features

Kura supports the following SQLite database features:

- **Persistence modes**: The implementation currently supports in-memory and file-based database instances.

- **Multiple database instances**: It is possible to create and configure multiple database instances from the Kura Administration UI, these instances can be selectively consumed by applications.

- **Journaling modes**: The implementation currently supports the WAL and rollback journal journaling modes.

- **Periodic database defrag and checkpoint**: The implementations currently supports periodic defrag (VACUUM) and periodic WAL checkpoint.

## Usage

### Creating a new SQLite database instance

To create a new SQLite database instance, use the following procedure:

1. Open the Administrative UI and press the **+** button in the side menu, under the Services section. A pop-up dialog should appear.
2. Select ```org.eclipse.kura.db.SQLiteDbService``` from the **Factory** drop-down list, enter an arbitrary name for the new instance and click **Apply**.
3. An entry for the newly created instance should appear in the side menu under **Services**, click on it to review its configuration. It is not possible to create different DB instances that manage the same database file.

### Configuration Parameters
The SQLite DB provides the following configuration parameters:

* **Database Mode**: Defines the database mode. If `In Memory` is selected, the database will not be persisted on the filesystem, all data will be lost if Kura is restarted and/or the database instance is deleted. If `Persisted` is selected, the database will be stored on disk in the location specified by the **Persisted Database Path** parameter.

* **Persisted Database Path**: Defines the path to the database file (it must include the database file name). This parameter is only relevant for persisted databases.

* **Encryption Key**: Allows to specify a key/passphrase for encrypting the database file. This feature requires a SQLite binary with an encryption extension, and is only relevant for persisted databases. The key format can be specified using the **Encryption Key Format** parameter. If the value of this parameter is changed, the encryption key of the database will be updated accordingly. This parameter can be left empty to create an unencrypted database or to decrypt an encrypted one.

!!! note
    The sqlite-jdbc version distributed with Kura does not contain any encryption extension, encryption features will not be available out of the box. See [sqlite-jdbc documentaton](https://github.com/xerial/sqlite-jdbc/blob/master/USAGE.md#how-to-use-encrypted-databases) for instructions about how to use a security extension.

* **Encryption Key Format**: Allows to specify the format of the Encryption Key parameter value. The possible values are ASCII (an ASCII string), Hex SSE (the key is an hexadecimal string to be used with the SSE extension) or Hex SQLCipher (the key is an hexadecimal string to be used with the SQLCipher extension).

* **Journal Mode**: The database journal mode. The following options are available:
  
    * **Rollback Journal**: The database instance will use the [rollback journal](https://www.sqlite.org/atomiccommit.html) for more details. More specifically, the DELETE argument will be passed to the [pragma journal_mode](https://www.sqlite.org/pragma.html#pragma_journal_mode) command.
    
    * **WAL**: The database instance will use [WAL mode](https://www.sqlite.org/wal.html). This is the default mode.

    The [WAL mode](https://www.sqlite.org/wal.html) description page contains a comparison of the two modes.
    
* **Defrag enabled**: Enables or disables the database defragmentation. Use the Defrag Interval parameter to specify the interval.


* **Defrag interval (seconds)**: The implementation supports running periodic defragmentation using the [VACUUM command](https://www.sqlite.org/lang_vacuum.html). This parameter specifies the interval in minutes between two successive checkpoints, set to zero to disable. This parameter is only relevant for persisted databases.

!!! warning
    The total disk space used by SQLite database files might temporarily increase during defragmentation and/or if transactions that modify a lot of data are performed (including deletion). In particular:

    * Defragmentation is implemented by copying all non-free database pages to a new file and then deleting the original file.
    * If transactions that involve a lot of data are performed, in case of rollback journal the old content of the modified pages will be stored in the journal until the transaction is completed, and in WAL mode the new content of modified pages will be stored to the WAL file until the next checkpoint is performed.
    
    It is recommended to perform some tests to determine the maximum database files size that will be used by the application and ensure that the size of the partition containing the database is at least twice as the expected db size.

* **Checkpoint enabled**: Enables or disables checkpoints in WAL journal mode. Use the WAL Checkpoint Interval parameter to specify the interval.
* **WAL Checkpoint Interval (Seconds)**: The implementation supports running periodic periodic [WAL checkpoints](https://www.sqlite.org/pragma.html#pragma_wal_checkpoint). Checkpoints will be performed in TRUNCATE mode. This parameter specifies the interval in seconds between two consecutive checkpoints, set to zero to disable. This parameter is only relevant for persisted databases in WAL Journal Mode. In WAL mode a checkpoint will be performed after a periodic defragmentation regardless of the value of this parameter.

* **Connection pool max size**: The implementation manages connections using a connection pool. This parameter defines the maximum number of connections for the pool. If a new connection is requested 

* **Delete Database Files On Failure**: If set to true, the database files will be deleted in case of failure in opening a persisted database. This is intended as a last resort measure for keeping the database service operational, especially in the case when it is used as a cloud connection message store.

* **Debug Shell Access Enabled**: Enables or disables the interaction with this database instance using the `sqlitedbg:excuteQuery` OSGi console command. (see #debug-shell)

### Selecting a database instance for existing components

A database instance is identified by its **Kura service PID**. The PID for instances created using the Web UI is the string entered in the **Name** field at step 2 of the previous section.

The built-in components that use database functionalities allow to specify which instance to use in their configuration. These components are the **DataService** component of the cloud stack, the **Wire Record Store** and **Wire Record Query** wire components. The configuration of each component contains a property that allows to specify the Kura Service PID of the desired instance.

!!! warning
    Using SQLite database instances through the deprecated **DbFilter** and **DbStore** components is not supported.

### Usage through Wires

It is possible to store and extract Wire Records into/from a SQLite database instance using the **Wire Record Store** and **Wire Record Query** wire components.

When a Wire Record is received by a **Wire Record Store** attached to a SQLite based database instance, the data will be stored in a table whose name is the current value of the **Record Collection Name** configuration parameter of the Wire Component.

Each property contained in a Wire Record will be appended to a column with the same name as the property key. A new column will be created if it doesn't already exists.

Since it is not possible to establish a one to one mapping between [SQLite storage classes](https://www.sqlite.org/datatype3.html) and the data types available on the Wires, the implementation will assign a custom type name to the created columns in order to keep track of the inserted Wire Record property type.

The custom types will be assigned according to the following table:

| Wires Data Type | Column Type Name | Storage Class Type Affinity |
|-|-|-|
| BOOLEAN | BOOLEAN | INTEGER |
| INTEGER | INT | INTEGER |
| LONG | BIGINT | INTEGER |
| FLOAT | FLOAT | REAL |
| DOUBLE | DOUBLE | REAL |
| STRING | TEXT | TEXT |
| BYTE_ARRAY | BLOB | BLOB |

The custom column type makes it possible to preserve the original type when data is extracted with the **Wire Record Query** component. Please note that the resulting type may change in case of queries that build computed columns.

!!! warning
    It is not recommended to store Wire Records having properties with the same key and different value type.
    If the value type changes, the target column will be dropped and recreated with the type derived from the last received record. All existing data in the target column will be lost.
    The purpose of this is to allow changing the type of a column with a Wire Graph configuration update.

### Debug shell

It is possible to inspect the contents of the database file using the OSGi console with the following command:

```
sqlitedbg:executeQuery dbServicePid 'query'
```

or more simply 

```
executeQuery dbServicePid 'query'
```

where `dbServicePid` is the user defined pid of the target database instance and `query` is an arbitrary SQL query to be executed (make sure to properly quote the query string with the `'` character). The command will print the result set or changed row count on the console.

It is only possible to access database instances whose **Debug Shell Access Enabled** configuration parameter is set to `true`. The default of this parameter is `false`.

The OSGi console is disabled by default, it can be accessed by starting the framework with the `/opt/eclipse/kura/bin/start_kura_debug.sh` script and running the following command on the gateway: 

```
telnet localhost 5002
```

!!! warning
    This feature is intended for debug purposes only, the **Debug Shell Access Enabled** parameter as well as the OSGi console should not be left enabled on production systems.