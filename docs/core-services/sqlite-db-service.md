# SQLite Db Service

Starting from version 5.3, Kura provides provides an integration of the SQLite database.

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
2. Select ```org.eclipse.kura.internal.db.sqlite.provider.SqliteDbServiceImpl``` from the **Factory** drop-down list, enter an arbitrary name for the new instance and click **Apply**.
3. An entry for the newly created instance should appear in the side menu under **Services**, click on it to review its configuration. It is not possible to create different DB instances that manage the same database file.

### Configuration Parameters
The SQLite DB provides the following configuration parameters:

* **Database Mode**: Defines the database mode. If `In Memory` is selected, the database will not be persisted on the filesystem, all data will be lost if Kura is restarted and/or the database instance is deleted. If `Persisted` is selected, the database will be stored on disk in the location specified by the **Persisted Database Path** parameter.

* **Persisted Database Path**: Defines the path to the database file (it must include the database file name). This parameter is only relevant for persisted databases.

* **Journal Mode**: The database journal mode. The following options are available:
    
    * **Rollback Journal**: The database instance will use the [rollback journal](https://www.sqlite.org/atomiccommit.html) for more details. More specifically, the DELETE argument will be passed to the [pragma journal_mode](https://www.sqlite.org/pragma.html#pragma_journal_mode) command.
    
    * **WAL**: The database instance will use [WAL mode](https://www.sqlite.org/wal.html). This is the default mode.

    The [WAL mode](https://www.sqlite.org/wal.html) description page contains a comparison of the two modes.


* **Defrag interval (seconds)**: The implementation supports running periodic defragmentation using the [VACUUM command](https://www.sqlite.org/lang_vacuum.html). This parameter specifies the interval in minutes between two successive checkpoints, set to zero to disable. This parameter is only relevant for persisted databases.

!!! warning
    The total disk space used by SQLite database files might temporarily increase during defragmentation and/or if transactions that modify a lot of data are performed (including deletion). In particular:

    * Defragmentation is implemented by copying all non-free database pages to a new file and then deleting the original file.
    * If transactions that involve a lot of data are performed, in case of rollback journal the old content of the modified pages will be stored in the journal until the transaction is completed, and in WAL mode the new content of modified pages will be stored to the WAL file until the next checkpoint is performed.

    It is recommended to perform some tests to determine the maximum database files size that will be used by the application and ensure that the size of the partition containing the database is at least twice as the expected db size.

* **WAL Checkpoint Interval (Seconds)**: The implementation supports running periodic periodic [WAL checkpoints](https://www.sqlite.org/pragma.html#pragma_wal_checkpoint). Checkpoints will be performed in TRUNCATE mode. This parameter specifies the interval in seconds between two consecutive checkpoints, set to zero to disable. This parameter is only relevant for persisted databases in WAL Journal Mode. In WAL mode a checkpoint will be performed after a periodic defragmentation regardless of the value of this parameter.

* **Connection pool max size**: The implementation manages connections using a connection pool. This parameter defines the maximum number of connections for the pool. If a new connection is requested 

* **Debug Shell Access Enabled**: Enables or disables the interaction with this database instance using the `sqlitedbg:excuteQuery` OSGi console command. (see #debug-shell)

### Selecting a database instance for existing components

TBD

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