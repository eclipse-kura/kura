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



## Usage

The following global variables are available to the script:

* [`input`](#received-envelope): an object that represents the received wire envelope.
* [`output`](#creating-and-emitting-wire-records): an object that allows to emit wire records.
* [logger](#logging): A slf4j logger

The following utility functions are available:

* [`newWireRecord(void) -> WireRecordWrapper`](#utility-functions)
* [`newByteArray(void) -> byte\[\`]](#utility-functions)
* [`newBooleanValue(boolean) -> TypedValue`](#utility-functions)
* [`newByteArrayValue(byte\[\`]) -> TypedValue](#utility-functions)
* [`newDoubleValue(number) -> TypedValue`](#utility-functions)
* [`newFloatValue(number) -> TypedValue`](#utility-functions)
* [`newIntegerValue(number) -> TypedValue`](#utility-functions)
* [`newLongValue(number) -> TypedValue`](#utility-functions)
* [`newStringValue(object) -> TypedValue`](#utility-functions)

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

New mutable Wire Record instances can be created using the `newWireRecord(void) -> WireRecordWrapper` function. The properties of a mutable WireRecord can be modified by setting Javascript object properties. The properties of a WireRecord object must be instances of the TypedValue class created using the `new<type>Value()` family of functions. Setting different kind of objects as properties of a WireRecord will result in an exception.

The **output** global variable is an object that can be used for emitting WireRecords.
This object contains a list of WireRecords that will be emitted when the script execution finishes, if no exceptions are thrown. New records can be added to the list using the `add(WireRecordWrapper)` function. It is also possible to emit records contained in the received WireEnvelope.

The script filter will emit a wire envelope only if the WireRecord list is not empty when the script execution completes. The following code is an example about how to emit a value:

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

#### Utility functions

* `newWireRecord(void) -> WireRecordWrapper`: Creates a new mutable wire record. New properties can be added to the record as shown above. In order to emit the new record use the `output.add(WireRecordWrapper)` function.

* `newByteArray(number) -> byte[]`: Creates a new mutable byte array of given length. The resulting array can be modified and the converted to a TypedValue using the `newByteArray(byte[]) -> TypedValue`.

The following functions can be used for creating TypedValues. The resulting objects can be used for WireRecord property values. Some of these functions perform numeric conversions that could cause precision losses.

* `newBooleanValue(boolean) -> TypedValue`: Creates a new TypedValue representing a boolean.

* `newByteArrayValue(byte[]) -> TypedValue`: Creates a new TypedValue representing a byte array.

* `newDoubleValue(number) -> TypedValue`: Creates a new TypedValue representing a double. The number provided as argument value will be coerced into a double.

* `newFloatValue(number) -> TypedValue`: Creates a new TypedValue representing a float. The number provided as argument value will be coerced into a float.

* `newIntegerValue(number) -> TypedValue`: Creates a new TypedValue representing an integer. The number provided as argument value will be coerced into an integer.

* `newLongValue(number) -> TypedValue`: Creates a new TypedValue representing a long. The number provided as argument value will be coerced into a long.

* `newStringValue(object) -> TypedValue`: Creates a new TypedValue representing a String. The object provided as argument value will be converted into a String using the `toString()` method.


#### Logging

An slf4j logger is available to the script as the `logger` global variable, it can be used for debug purposes more or less like in Java code.

#### Script context

As said above the script context is persisted across different script executions, this allow to define persistent variables, for example in this way:

```javascript
counter = typeof(counter) === 'undefined' // check if the counter variable is not defined
 ? 0 // counter is undefined, initialise it to zero
 : counter; // counter is already defined, keep the previous value
```

It is possible to reset the script context using the **script.context.drop** configuration property.
If this property is set to `true`, the script context will be dropped every time the component configuration is updated, resetting the value of any
persisted variable.

#### Example script

The Script Filter configuration contains an example script describing the component usage, it can be executed connecting a Timer component to a Script Filter.
