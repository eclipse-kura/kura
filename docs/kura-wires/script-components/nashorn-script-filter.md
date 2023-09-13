# Nashorn Script Filter

!!! warning
    This component is deprecated as of Kura version 5.3 since no more available on Java 17.

The **Script Filter Component** provides scripting functionalities in Kura Wires using the Nashorn Javascript engine:

* The script execution is triggered when a wire envelope is received by the filter component.
* It is possible to access to the received envelope and inspect the wire records contained in it form the script.
* The script can optionally emit a wire envelope containing one or more wire records for each wire envelope received.
* The script context is persisted across multiple executions, allowing to perform stateful computations like running a counter, performing time averages etc.
* A slf4j Logger is available to the script for debug purposes.
* The script context is restricted in order to allow only Wires related processing. Any attempt to load additional Java classes will fail.
* The default configuration contains an example script describing the component usage, it can be executed connecting a Timer and a Logger component.



## Usage

The following global variables are available to the script:

* [`input`](#received-envelope): an object that represents the received wire envelope.
* [`output`](#creating-and-emitting-wire-records): an object that allows to emit wire records.
* `logger`: a slf4j logger

The following utility functions are available (see [Creating and emitting wire records](#creating-and-emitting-wire-records) for usage):

* `newWireRecord(void) -> WireRecordWrapper`
* `newByteArray(void) -> byte[]`
* `newBooleanValue(boolean) -> TypedValue`
* `newByteArrayValue(byte[])` -> `TypedValue`
* `newDoubleValue(number) -> TypedValue`
* `newFloatValue(number) -> TypedValue`
* `newIntegerValue(number) -> TypedValue`
* `newLongValue(number) -> TypedValue`
* `newStringValue(object) -> TypedValue`

The following global constants expose the `org.eclipse.kura.type.DataType` enum variants:

* `BOOLEAN`
* `BYTE_ARRAY`
* `DOUBLE`
* `FLOAT`
* `INTEGER`
* `LONG`
* `STRING`



## Received envelope

The received envelope is represented by the **input** global variable and it has the following properties:

* **emitterPid**: the emitter pid of the received envelope as a String.
* **records**: an immutable array that represents the Wire Records contained in the Wire Envelope.

Each element of the **records** array is an immutable object that represents a received wire record. Wire record properties are directly mapped to Javascript object properties, and are instances of the `org.eclipse.kura.type.TypedValue` class. Each Wire Record property has the following methods available:

* `getType(void) -> DataType`: Returns the type of the value, as a DataType enum variant. Can be matched against the data type constants described above.
* `getValue(void) -> Object`: Returns the actual value.

The javascript objects referred as WireRecords in this guide are not instances of the `org.eclipse.kura.wire.WireRecord` class, but are wrappers that map WireRecord properties into javascript properties. The following code is a simple example script that show how to use the filter:

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



## Creating and emitting wire records

New mutable `WireRecord` instances can be created using the `newWireRecord(void) -> WireRecordWrapper` function. The properties of a mutable `WireRecord` can be modified by setting Javascript object properties. The properties of a `WireRecord` object must be instances of the [`TypedValue`](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/type/TypedValue.java) class created using the `new<type>Value()` family of functions. Setting different kind of objects as properties of a `WireRecord` will result in an exception.

The **output** global variable is an object that can be used for emitting WireRecords.
This object contains a list of WireRecords that will be emitted when the script execution finishes, if no exceptions are thrown. New records can be added to the list using the `add(WireRecordWrapper)` function. It is also possible to emit records contained in the received WireEnvelope.

The script filter will emit a wire envelope only if the WireRecord list is not empty when the script execution completes. The following code is an example about how to emit a value:

```javascript
var record = newWireRecord()

var byteArray = newByteArray()
byteArray[0] = 1
byteArray[1] = 2
byteArray[2] = 0xaa
byteArray[3] = 0xbb

record.LED1 = newBooleanValue(true)
record.foo = newStringValue('bar')
record['myprop'] = newDoubleValue(123.456)
record['example.long'] = newLongValue(100)
record['example.float'] = newFloatValue(1.014)
record['example.byte.array'] = newByteArrayValue(byteArray)

output.add(record)
```
## Script context

The **script.context.drop** option allows to reset the script context. If set to `true` the script context will be dropped every time the component configuration is updated, resetting the value of any persisted variable.

In the example below, with **script.context.drop=false** the following script will preserve the value of `counter` across executions. Setting **script.context.drop=true** will cause `counter` to be `undefined` every time the component is triggered.

```javascript
counter = typeof(counter) === 'undefined'
 ? 0 // counter is undefined, initialise it to zero
 : counter; // counter is already defined, keep the previous value
```
