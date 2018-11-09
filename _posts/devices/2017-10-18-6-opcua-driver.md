---
layout: page
title:  "OPC UA Driver"
categories: [devices]
---

This Driver implements the client side of the OPC UA protocol using the Driver model. The Driver can be used to interact as a client with OPC UA servers using different abstractions, such as the Wires framework, the Asset model or by directly using the Driver itself.

The Driver is distributed as a deployment package on Eclipse Marketplace for Kura [3.x](https://marketplace.eclipse.org/content/opc-ua-driver-eclipse-kura-3xy) and [4.x](https://marketplace.eclipse.org/content/opc-ua-driver-eclipse-kura-4xy).
It can be installed following the instructions provided [here](/kura/admin/application-management.html#installation-from-eclipse-marketplace).

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
 - **node.id.type**: The type of the node id (see below)

## Node ID types

The Driver supports the following node id types:

| Node ID Type | Format of node.id                                                                                                                                                                                         |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NUMERIC      | **node.id** must be parseable into an integer                                                                                                                                                             |
| STRING       | **node.id** can be any string                                                                                                                                                                             |
| OPAQUE       | Opaque node ids are represented by raw byte arrays. In this case **node.id** must be the base64 encoding of the node id.                                                                                  |
| GUID         | **node.id** must be a string conforming to the format described in the documentation of the [java.util.UUID.toString()](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html#toString--) method. |
