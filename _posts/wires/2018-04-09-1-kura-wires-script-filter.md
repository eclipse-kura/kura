---
layout: page
title:  "Script Filter Wire component"
categories: [wires]
---

## Features

The Script Filter component provides scripting functionalities in Kura Wires using the Nashorn Javascript engine:

* The script execution is triggered when a wire envelope is received by the filter component.
* It is possible to access to the received envelope and inspect the wire records contained in it form the script.
* The script can optionally emit a wire envelope containing one or more wire records for each wire envelope received.
* The script context is persisted across multiple executions, allowing to perform stateful computations like running a counter, performing time averages etc.
* A slf4j Logger is available to the script for debugging purposes.
* The script context is restricted in order to allow only Wires related processing. Any attempt to load additional Java classes will fail.

## Usage

The following global variables are available to the script:

* [input](#received-envelope): An object that represents the received wire envelope.
* [output](#creating-and-emitting-wire-records): An object that allows to emit wire records.
* [logger](#logging): A slf4j logger

The following utility functions are available:

* [newWireRecord(void) -> WireRecordWrapper](#utility-functions)
* [newByteArray(void) -> byte\[\]](#utility-functions)
* [newBooleanValue(boolean) -> TypedValue](#utility-functions)
* [newByteArrayValue(byte\[\]) -> TypedValue](#utility-functions)
* [newDoubleValue(number) -> TypedValue](#utility-functions)
* [newFloatValue(number) -> TypedValue](#utility-functions)
* [newIntegerValue(number) -> TypedValue](#utility-functions)
* [newLongValue(number) -> TypedValue](#utility-functions)
* [newStringValue(object) -> TypedValue](#utility-functions)

The following global constants expose the `org.eclipse.kura.type.DataType` enum variants:

* BOOLEAN
* BYTE_ARRAY
* DOUBLE
* FLOAT
* INTEGER
* LONG
* STRING

### Received envelope

The received envelope is represented by the **input** global variable and it has the following properties:

* **emitterPid**: The emitter pid of the received envelope as a String.
* **records**: An immutable array that represents the Wire Records contained in the Wire Envelope.

Each element of the **records** array is an immutable object that represents a received wire record. Wire record properties are directly mapped to Javascript object properties, and are instances of the `org.eclipse.kura.type.TypedValue` class. Each Wire Record property has the following methods available:

* `getType(void) -> DataType`: Returns the type of the value, as a DataType enum variant. Can be matched against the data type constants described above.
* `getValue(void) -> Object`: Returns the actual value.

The javascript objects referred to as WireRecords in this guide are not instances of the `org.eclipse.kura.wire.WireRecord` class, but are wrappers that map WireRecord properties into javascript properties.
The following code is a simple example script that show how to use the filter:

```javascript
// get the first record from the envelope
var record = input.records[0]
// let's assume it contains the LED boolean property and the TEMPERATURE double property
record.LED1.getType() === BOOLEAN // evaluates to true
if (record.LED1.getValue()) {
// LED1 is on
}
record.LED1.getType() === DOUBLE // evaluates to true
if (record.TEMPERATURE.getValue() > 50) {
// temperature is high, do something
}
```

### Creating and emitting wire records

New mutable Wire Record instances can be created using the `newWireRecord(void) -> WireRecordWrapper` function.

The properties of a mutable WireRecord can be modified by setting Javascript object properties.

The properties of a WireRecord object must be instances of the TypedValue
 class created using the `new<type>Value()` family of functions. Setting different kinds of objects as properties of a WireRecord will result in an exception.

The **output** global variable is an object that can be used for emitting WireRecords.
This object contains a list of WireRecords that will be emitted when the script execution finishes, if no exceptions are thrown.

New records can be added to the list using the `add(WireRecordWrapper)` function.
It is also possible to emit records contained in the received WireEnvelope.

The script filter will emit a wire envelope only if the WireRecord list is not empty when the script execution completes. The following code is an example of how to emit a value:

```javascript
// create a new record
var record = newWireRecord()

// set some properties on it
record.LED1 = newBooleanValue(true)
record.foo = newStringValue('bar')
record['myprop'] = newDoubleValue(123.456)

// add the wire record to the output envelope
output.add(record)
```
