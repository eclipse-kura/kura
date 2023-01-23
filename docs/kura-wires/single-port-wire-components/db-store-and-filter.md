# Wire Record Store and Wire Record Query

This tutorial will present how to use Wire Record Store and Wire Record Query components and provide an example based on the OPC-UA simulated server already used in [OPC-UA Application](../../connect-field-devices/opcua-driver.md).

The Wire Record Store component allows the wire graphs to interact with a persistend Wire Record store implementation, for example a SQL database. It stores in a user-defined collection all the envelopes received by the component.

The Wire Record Query component, instead, can run a custom query on the attached store and emit the result on the wire.

!!! note
    The Wire Record Store and Wire Record Query components have been introduced in Kura 5.3.0 as a replacement of the Db Store and Db Filter, that have been deprecated.

    The reason of the deprecation is the fact that Db Store and Db Filter only support databases that provide a JDBC interface. Moreover, the old Db Store interacts with the database using a set of hardcoded SQL queries.
    This fact makes it difficult to use it with different databases since the syntax of the queries is usually database-specific.
    
    The new components use the `org.eclipse.kura.wire.store.provider.WireRecordStoreProvider` and `org.eclipse.kura.wire.store.provider.QueryableWireRecordStoreProvider` APIs introduced in 5.3.0 allowing to use them with generic data store implementations.

    Please note that Wire Record Query component is not portable by nature, since it allows to execute an arbitrary user defined query. The new APIs allows to use it with non-JDBC data stores.

The Wire Record Store component can be configured as follows:

- **Record Collection Name**: The name of the record collection that should be used. The implementation of the collection depends on the Wire Record Store implementation, if it is a SQL database, the collection will likely be a table.
- **Maximum Record Collection Size**: The maximum number of records that is possible to store in the collection.
- **Cleanup Records Keep**: The number of records in the collection to keep while performing a cleanup operation (if set to 0 all the records will be deleted). The cleanup operation is performed when a new record is inserted and the current size of the record collection is greater than the configured **Maximum Record Collection Size**.
- **WireRecordStoreProvider Target Filter** : Specifies, as an OSGi target filter, the pid of the of the Wire Record Store instance to be used.

The Wire Record Query component can be configured as follows:

- **Query**: Query to be executed. The query syntax depends on the Queryable Wire Record Store implementation.
- **Cache Expiration Interval (Seconds)**: This component is capable of maintaining a cache of the records produced by the last query execution and emitting its contents on the Wire until it expires. This value specifies the cache validity interval in seconds. When cache expires, it will cause a new query execution. The query will be executed for every trigger received if the value is set to 0.
- **QueryableWireRecordStoreProvider Target Filter** : Specifies, as an OSGi target filter, the pid of the of the Queryable Wire Record Store instance to be used.
- **Emit On Empty Result** : Defines the behavior of the component if the result of the performed query is empty. If set to true, an empty envelope will be emitted in this case, if set to false no envelopes will be emitted.

The following procedure will create a wire graph that collects data from a simulated OPC-UA Server, stores it in a table in the database, using the Wire Record Store component, and publishes it in the cloud platform. Moreover, the Wire Record Query component is used to read from the database and write data to the OPC-UA Server based on the values read.


## Configure OPC-UA server simulator

1. Download the [OPC-UA server simulator](https://s3.amazonaws.com/kura-resources/opcua_demo_server.dp) bundle and install it on your Kura instance. It will create a simulated OPC-UA server that exposes some sensors (light, temperature and water sensor) and some actuators (buzzer, led and fan).
2. In the Kura Administrative Web Interface, select “OPCUA Server demo” in “Services” and set **server.port** to 1234. Click the **Apply** button. This will start an OPC-UA​ server on port 1234.



## Configure Wires OPC-UA application

1. Install the OPC-UA Driver from [Eclipse Kura Marketplace](https://marketplace.eclipse.org/content/opc-ua-driver-eclipse-kura-45).
2. Use the local Kura Administrative Web Interface to create a new OPC-UA driver instance:
    - Select **Drivers and Assets**, click the **New Driver** button
    - Select **org.eclipse.kura.driver.opcua**, type in a name, and click **Apply**: a new service will show up under Services.
3. Configure the new service as follows:
    - **endpoint.ip**: localhost
    - **endpoint.port**: 1234
    - **server.name**: leave blank
    ![WireAsset Opcua Example](./images/opcua-wire-asset-config.png)

4. Click on **Wires** under **System**
5. Add a new **Timer** component and configure the interval at which the OPC-UA server will be sampled
6. Add a new **Asset** with the previously added OPC-UA Driver
7. Configure the new OPC-UA asset, adding new Channels as shown in the following image. Make sure that all the channels are set to READ.
    ![WireAsset Opcua Example Read Mode](./images/opcua-wire-asset-config-read.png)

8. Add a new Wire Record Store named **DBStore** component and configure it as follows:
    - **Record Collection Name**: WR_data
    - **Maximum Record Collection Size**: 10000
    - **Cleanup Records Keep**: 0
    - **WireRecordStoreProvider Target Filter** : the Wire Record Store Provider pid to be used
9.  Add a new **Publisher** component and configure the chosen cloud platform stack in **cloud.service.pid** option
10. Add **Logger** component
11. Add another instance of **Timer**
12. Add a new Wire Record Query component named **DBFilter** component and configure it as follows. The query will get the values from the light sensor and if they are less than 200, the fan is activated.
    - **Query**: SELECT (CASE WHEN “light” < 200 THEN 1 ELSE 0 END) AS “led” FROM “WR_data” ORDER BY TIMESTAMP DESC LIMIT 1;
    - **Cache Expiration Interval (Seconds)**: 0
    - **QueryableWireRecordStoreProvider Target Filter** : the Queryable Wire Record Store Provider pid to be used
13. Add another **Asset** with the OPC-UA Driver, configured as shown in the following image. Be sure that all the channels are set to WRITE.
    ![WireAsset Opcua Example Write Mode](./images/opcua-wire-asset-config-write.png)

    !!! note
        Be aware that the **Query** syntax can vary accordingly to the dialect used by the database. For example, the MySQL dialect doesn't allow to surround the table or columns names with double-quotes. In the H2DB, this is mandatory instead.

14. Connect the components as shown in the following image, then click on “Apply” and check the logs and the cloud platform that the data is correctly published.
    ![WireAsset Opcua Example Graph](./images/opcua-wire-asset-graph.png)