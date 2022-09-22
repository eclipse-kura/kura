# S7comm Driver

This Driver implements the s7comm protocol and can be used to interact with Siemens S7 PLCs using different abstractions, such as the Wires framework, the Asset model or by directly using the Driver itself.

The Driver is distributed as a deployment package on the Eclipse Marketplace for Kura [3.x](https://marketplace.eclipse.org/content/s7-driver-eclipse-kura-3xy) and [4.x/5.x](https://marketplace.eclipse.org/content/s7-driver-eclipse-kura-4xy).
It can be installed following the instructions provided [here](/kura/administration/application-management.md).

## Features

The S7comm Plc Driver features include:

 - Support for the s7comm protocol over TCP.
 - Support for reading and writing data from the Data Blocks (DB) memory areas.
 - The driver is capable of automatically aggregating reads/writes for contiguous data in larger bulk requests in order to reduce IO times.

## Instance creation

A new S7comm driver instance can be created either by clicking the **New Driver** button in the dedicated **Drivers and Assets** Web UI section or by clicking on the **+** button under **Services**. In both cases, the `org.eclipse.kura.driver.s7plc` factory must be selected and a unique name must be provided for the new instance.

## Channel configuration

The S7 Driver channel configuration is composed of the following parameters:

 - **name**: the channel name.
 - **type**: the channel type, (`READ`, `WRITE`, or `READ_WRITE`).
 - **value type**: the Java type of the channel value.
 - **s7.data.type**: the S7 data type involved in channel operation.
 - **data.block.no**: the data block number involved in channel operation.
 - **offset**: the start address of the data.
 - **byte.count**: the size in bytes of the transferred data. This parameter is required only if the **value type** parameter is set to `STRING` or `BYTE_ARRAY`. In the other cases, this parameter is ignored and the data size is automatically derived from the **s7.data.type**.
 - **bit.index**: the index of the bit involved in channel operation inside its containing byte, index 0 is the least significant bit. This parameter is required only if the **value type** parameter is set to `BOOLEAN` and **s7.data.type** is set to `BOOL`. In the other cases, this parameter is ignored.

## Data Types

When performing operations that deal with numeric data, two data types are involved:

1. The Java primitive type that is used in the ChannelRecords exchanged between the driver and  Java applications. (the Java type of the value received/supplied by external applications from/to the Driver in case of a read/write operation). This value type is specified by the **value type** configuration property.

2. The S7 type of the data on the PLC. This value type is specified by the **s7.data.type** configuration property. The following S7 data types are supported:

    | S7 Data Type   | Size                                                                   | Sign       |
    |----------------|------------------------------------------------------------------------|------------|
    | `INT`          | 16 bits                                                                | `signed`   |
    | `DINT`         | 32 bits                                                                | `signed`   |
    | `WORD`         | 16 bits                                                                | `unsigned` |
    | `DWORD`        | 32 bits                                                                | `unsigned` |
    | `REAL`         | 32 bits                                                                | `signed`   |
    | `BOOL`         | 1 bit                                                                  | n.d.       |
    | `BYTE`         | 1 byte                                                                 | `unsigned` |
    | `CHAR`         | 1 byte (only supported as char arrays using the String Java data type) | n.d.       |

The Driver automatically adapts the data type used by external applications and the S7 device depending on the value of the two configuration properties mentioned above.

The adaptation process involves the following steps:

- Each device data type is internally converted by the driver from/to a Java type large enough to represent the value of the device data without losing precision. The type mappings are the following:

    | S7 Data Type   | Java Type   |
    |----------------|-------------|
    | `INT`          | `int`       |
    | `DINT`         | `int`       |
    | `WORD`         | `int`       |
    | `DWORD`        | `long`      |
    | `REAL`         | `float`     |
    | `BOOL`         | `boolean`   |
    | `BYTE`         | `int`       |

- If the **value type** of the channel does not match the Java type specified in mapping above, a conversion is performed by the Driver to convert it to/from the matching type, choosing appropriately between the `Number.toInt()`, `Number.toLong()`, `Number.toFloat()` or `Number.toDouble()` methods.
Precision losses may occur if the Java type used by the external application is not suitable to represent all possible values of the device data type.

## Array Data

The driver supports transferring data as raw byte arrays or ASCII strings:

 - **Byte arrays**: For transferring data as byte arrays the channel **value type** property must be set to `BYTE_ARRAY`, the **data.type** configuration property must be set to `BYTE` and the **byte.count** property must be set to the data length in bytes.
 - **Strings**: For transferring data as ASCII strings the channel **value type** property must be set to `STRING`, the **data.type** configuration property must be set to `CHAR` and the **array.data.length** property must be set to the data length in bytes.

## Writing Single Bits

The Driver supports setting the value of single bits without overwriting the other bits of the same byte.
This operation can be performed defining a channel having `BOOLEAN` as **value type**, `BOOL` as **s7.data.type** and the proper index set to the **bit.index** property.

The Driver will fetch the byte containing the bit to be written, update its contents and then write back the obtained value. If multiple bits on the same byte need to be modified, the driver will perform only one read and write for that byte. If bits that need to be changed are located in contiguous bytes, the driver will perform only one bulk read and one bulk write transferring all the required data in a single request.
