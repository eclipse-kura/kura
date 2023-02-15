# OPC UA Driver

This Driver implements the client side of the OPC UA protocol using the Driver model. The Driver can be used to interact as a client with OPC UA servers using different abstractions, such as the Wires framework, the Asset model or by directly using the Driver itself.

The Driver is distributed as a deployment package on Eclipse Marketplace for Kura [3.x](https://marketplace.eclipse.org/content/opc-ua-driver-eclipse-kura-3xy) and [4.x/5.x](https://marketplace.eclipse.org/content/opc-ua-driver-eclipse-kura-4xy).
It can be installed following the instructions provided [here](../administration/application-management.md).

## Features

The OPC UA Driver features include:

 - Support for the OPC UA protocol over TCP.
 - Support for reading and writing OPC UA variable nodes by node ID.

## Instance creation

A new OPC UA instance can be created either by clicking the **New Driver** button in the dedicated **Drivers and Assets** Web UI section or by clicking on the **+** button under **Services**. In both cases, the `org.eclipse.kura.driver.opcua` factory must be selected and a unique name must be provided for the new instance.

## Channel configuration

The OPC UA Driver channel configuration is composed of the following parameters:

 - **name**: the channel name.
 - **type**: the channel type, (`READ`, `WRITE`, or `READ_WRITE`).
 - **value type**: the Java type of the channel value.
 - **node.id**: The node id of the variable node to be used, the format of the node id depends on the value of the **node.id.type** property.
 - **node.namespace.index**: The namespace index of the variable node to be used.
 - **opcua.type**: The OPC-UA built-in type of the attribute to be read/written. If set to DEFINED_BY_JAVA_TYPE (default), the driver will attempt to determine the OPC-UA type basing on the **value type** parameter value. If the read/write operation fails, it may be necessary to use one of the other values of this configuration parameter to explicitly select the type.

    This parameter also lists the OPC-UA types currently supported by the driver.

    Not all **value type** and **opcua.type** combinations are valid, the allowed ones are the following:

    | **opcua.type** | Allowed **value.type**s | Recommended **value.type** |
    |-|-|-|
    | BOOLEAN | BOOLEAN | BOOLEAN |
    | SBYTE | INTEGER, LONG, FLOAT, DOUBLE, STRING | INTEGER |
    | INT16 | INTEGER, LONG, FLOAT, DOUBLE, STRING | INTEGER |
    | INT32 | INTEGER, LONG, FLOAT, DOUBLE, STRING | INTEGER |
    | INT64 | INTEGER, LONG, FLOAT, DOUBLE, STRING | LONG |
    | BYTE | INTEGER, LONG, FLOAT, DOUBLE, STRING | INTEGER |
    | UINT16 | INTEGER, LONG, FLOAT, DOUBLE, STRING | INTEGER |
    | UINT32 | INTEGER, LONG, FLOAT, DOUBLE, STRING | LONG |
    | UINT64 | INTEGER, LONG, FLOAT, DOUBLE, STRING | STRING |
    | FLOAT | FLOAT, STRING | FLOAT |
    | DOUBLE | DOUBLE, STRING | DOUBLE |
    | STRING | STRING | STRING |
    | BYTE_STRING | BYTE_ARRAY | BYTE_ARRAY |
    | BYTE_ARRAY | BYTE_ARRAY | BYTE_ARRAY |
    | SBYTE_ARRAY | BYTE_ARRAY | BYTE_ARRAY |

    Using a non allowed **value.type**  will result in read/write operation failures.
    It should be noted that there is not a one to one match between the **opcua.type** and Java **value.type**. It is recommended to compare the allowed ranges for numeric types specified in [OPC-UA Reference](https://reference.opcfoundation.org/v104/Core/docs/Part6/5.1.2/) and [Java reference](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html) for selecting the best match.

 - **node.id.type**: The type of the node id (see the [Node Id types](#node-id-types) section)
 - **attribute**: The attribute of the referenced variable node.

If the **listen** flag is enabled for an OPC-UA channel, the driver will request the server to send notifications if it detects changes in the referenced attribute value.

In order to enable this, the driver will create a global _subscription_ (one per Driver instance), and a _monitored item_ for each channel. See [[1](https://reference.opcfoundation.org/v104/Core/docs/Part4/5.12.1/)] for more details.
The **Subscription publish interval** global configuration parameter can be used to tune the _subscription publishing interval_.

 - **listen.sampling.interval**: The sampling interval for the _monitored item_. See the **Sampling interval** section of [[1](https://reference.opcfoundation.org/v104/Core/docs/Part4/5.12.1/)] for more details.
 - **listen.queue.size**: The queue size for the _monitored item_. See the **Queue parameters** section of [[1](https://reference.opcfoundation.org/v104/Core/docs/Part4/5.12.1/)] for more details.
 - **listen.discard.oldest**: The value of the _discardOldest_ flag for the _monitored item_. See the **Queue parameters** section of [[1](https://reference.opcfoundation.org/v104/Core/docs/Part4/5.12.1/)] for more details.

The **listen.subscribe.to.children** parameter can be used to enable the [Subtree Subscription](#substree-subscription) feature.

[1] [MonitoredItem model](https://reference.opcfoundation.org/v104/Core/docs/Part4/5.12.1/)

## Node ID types

The Driver supports the following node id types:

| Node ID Type | Format of node.id                                                                                                                                                                                         |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NUMERIC      | **node.id** must be parseable into an integer                                                                                                                                                             |
| STRING       | **node.id** can be any string                                                                                                                                                                             |
| OPAQUE       | Opaque node ids are represented by raw byte arrays. In this case **node.id** must be the base64 encoding of the node id.                                                                                  |
| GUID         | **node.id** must be a string conforming to the format described in the documentation of the [java.util.UUID.toString()](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html#toString--) method. |

## Certificate setup

In order to use settings for **Security Policy** different than **None**, the OPCUA driver must be configured to trust the server certificate and a new client certificate - private key pair must be generated for the driver. 
These items can be placed in a Java keystore that can be referenced from driver configuration.
The keystore does not exist by default and without it connections that use **Security Policy** different than **None** will fail.

The following steps can be used to generate the keystore:

1. Download the certificate used by the server, usually this can be done from server management UI.
2. Copy the downloaded certificate to the gateway using SSH.
3. Copy the following example script to the device using SSH, it can be used to import the server certificate and generate the client key pair. It can be modified if needed:

    ```bash
    #!/bin/bash

    # the alias for the imported server certificate
    SERVER_ALIAS="server-cert"

    # the file name of the generated keystore
    KEYSTORE_FILE_NAME="opcua-keystore.ks"
    # the password of the generated keystore and private keys, it is recommended to change it
    KEYSTORE_PASSWORD="changeit"

    # server certificate to be imported is expected as first argument
    SERVER_CERTIFICATE_FILE="$1"

    # import existing certificate
    keytool -import \
            -alias "${SERVER_ALIAS}" \
            -file "${SERVER_CERTIFICATE_FILE}" \
            -keystore "${KEYSTORE_FILE_NAME}" \
            -noprompt \
            -storepass "${KEYSTORE_PASSWORD}"

    # alias for client certificate
    CLIENT_ALIAS="client-cert"
    # client certificate distinguished name, it is recommended to change it 
    CLIENT_DN="CN=MyCn, OU=MyOu, O=MyOrganization, L=Amaro, S=UD, C=IT"
    # the application id, must match the corresponding parameter in driver configuration
    APPLICATION_ID="urn:kura:opcua:client"

    # generate the client private key and certificate
    keytool -genkey \
            -alias "${CLIENT_ALIAS}" \
            -keyalg RSA \
            -keysize 4096 \
            -keystore "${KEYSTORE_FILE_NAME}" \
            -dname "${CLIENT_DN}" \
            -ext ku=digitalSignature,nonRepudiation,keyEncipherment,dataEncipherment \
            -ext eku=clientAuth \
            -ext "san=uri:${APPLICATION_ID}" \
            -validity 1000 \
            -noprompt \
            -storepass ${KEYSTORE_PASSWORD} \
            -keypass ${KEYSTORE_PASSWORD}
    ```

4. Update the following parameters in driver configuration:
  * **Keystore Path **: Set the absolute path of the `opcua-keystore.ks` file created at step 3.
  * **Security Policy ** -> Set the desired option
  * **Client Certificate Alias** -> Set the value of the `CLIENT_ALIAS` script variable (the default is `client-cert`)  
  * **Enable Server Authentication** -> true
  * **Keystore type** -> JKS
  * **Keystore Password** -> Set the value of the  `KEYSTORE_PASSWORD` script variable (the default value is `changeit`)
  * **Application URI **-> Set the value of the `APPLICATION_ID` script variable (default value should be already ok).

5. Configurare the server to trust the client certificate generated at step 3. The steps required to do this vary depending on the server. Usually the following steps are needed:
  * Make a connection attempt using OPC-UA driver, this will likely fail because the server does not trust client certificate.
  * After the failed connection attempt, the server should display the certificate used by the driver in the administration UI. The server UI should allow to set it as trusted.
  * Make another connection attempt once the certificate has been set to trusted, this connection attempt should succeed.

## Substree Subscription

The driver can be configured to recursively visit the children of a folder node and create a Monitored Item for the value of each discovered variable node with a single channel in Asset configuration.

> Warning: This feature should be used with care since it can cause high load on both the gateway and the server if the referenced folder contains a large number of nodes and/or the notification rate is high.

### Channel configuration

In order to configure the driver to perform the discovery operation, a single channel can be defined with the following configuration:

* **type**: `READ`
* **value.type**: `STRING` (see below)
* **listen**: `true`
* **node.id**: the node id of the root of the subtree to visit
* **node.namespace.index**: the namespace index of the root of the subtree to visit
* **node.id.type** the node id type of the root of the subtree to visit
* **listen.subscribe.to.children** (**NEW**): `true`

The rest of the configuration parameters can be specified in the same way as for the single node subscription use case.

The **listen.sampling.interval**, **listen.queue.size** and **listen.discard.oldest** parameters of the root will be used for all subscriptions on the subtree.

### Discovery procedure

The driver will consider as folders to visit all nodes that whose type definition is `FolderType`, or more precisely all nodes with the following reference:

`HasTypeDefinition`:
  * namespace index: 0
  * node id: 61 (numeric)
  * URN: `http://opcfoundation.org/UA/`

The driver will subscribe to all the variable nodes found.

### Event reporting

If the Driver is used by a Wire Asset, it will emit on the wire a single message per received event.

All emitted events will contain a single property. Depending on the value of the **Subtree subscription events channel name format** global configuration parameter, the name of this property is the node id or the browsed path of the source OPCUA node relative to the root folder defined in the channel configuration.

### Type conversion

The current version of the driver tries to convert the values received for all the events on a subtree to the type defined in the **value.type** configuration parameter.

Since the value types of the discovered nodes are heterogeneous, the conversion might fail if the types are not compatible (e.g. if **value.type** is set to `INTEGER` and the received value is a string).

Setting **value.type** to `STRING` should allow to perform safe conversions for most data types.