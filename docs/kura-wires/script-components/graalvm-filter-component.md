# GraalVM&trade; Filter Component

The *Filter Component* provides scripting functionalities in Kura Wires using the GraalVM&trade; JavaScript engine:

* The script execution is triggered when a wire envelope is received by the filter component.
* It is possible to access the received envelope and inspect the wire records contained in it from the script.
* The script can optionally emit a wire envelope containing one or more wire records for each wire envelope received.
* The script context is persisted across multiple executions, allowing to perform stateful computations like running a counter, performing time averages etc.
* A slf4j Logger is available to the script for debugging purposes.
* The script context is restricted to allow only Wires-related processing. Any attempt to load additional Java classes will fail.
* The default configuration contains an example script describing the component usage, it can be executed connecting a Timer and a Logger component.



## Usage

The following global variables are available to the script:

* [`input`](#received-envelope): an object that represents the received wire envelope.
* [`output`](#creating-and-emitting-wire-records): an object that allows to emit wire records.
* `logger`: a slf4j logger.

The following utility functions are available (see [Creating and emitting wire records](#creating-and-emitting-wire-records) for usage):

* `newWireRecord(Map<String, TypedValue<?>) -> WireRecord`
* `newByteArray(int) -> byte[]`
* `newBooleanValue(boolean) -> TypedValue`
* `newByteArrayValue(byte[]) -> TypedValue`
* `newDoubleValue(number) -> TypedValue`
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

The received envelope is represented by the **input** global variable and is mapped in Javascript as a [`WireEnvelope`](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/wire/WireEnvelope.java) object.

GraalVM Javascript Engine allows a 1:1 mapping of Java Objects to JavaScript ones. Hence, it is possible to access the `WireRecord`s of the envelope and the emitter pid using the methods specified in the [`WireEnvelope` Kura API](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/wire/WireEnvelope.java):

```javascript
logger.info('Emitter pid is {}', input.getEmitterPid())
var records = input.getRecords()
```

The `records` array can be iterated to extract the `WireRecord` properties (reference the [`WireRecord` Kura API](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/wire/WireRecord.java)):

```javascript
for(let i=0; i<records.length; i++) {
 // WireRecord.getProperties() returns a map of String - TypedValue
 for (const [keyString, typedValue] of records[i].getProperties()) {
 logger.info('The {}-th record contains:'\, String(i))
 logger.info('{}={}\n', keyString, typedValue.getValue())
 }
}
```

As in the example above, `wireRecord.getProperties` returns a map of `String, TypedValue`. Refer to the [`TypedValue` Kura API](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/type/TypedValue.java) for the accessible public methods list.



## Creating and emitting wire records

New mutable `WireRecord` instances can be created using the `newWireRecord(Map<String, TypedValue<?>)` function. The properties of a mutable `WireRecord` can be modified by setting Javascript object properties. The properties of a `WireRecord` object must be instances of the [`TypedValue`](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/type/TypedValue.java) class created using the `new<type>Value()` family of functions. Setting different kind of objects as properties of a WireRecord will result in an exception.

The **output** global variable is an object that can be used for emitting `WireRecord`s. This object contains a list of `WireRecord`s that will be emitted when the script execution finishes if no exceptions are thrown. The following code is an example of how to emit a list containing a single `WireRecord`:

```javascript
var output = new Array()
var outputMap = new Object()

var byteArray = newByteArray(4)
byteArray[0] = 1
byteArray[1] = 2
byteArray[2] = 0xaa
byteArray[3] = 0xbb

outputMap['example.integer'] = newIntegerValue(10)
outputMap['example.long'] = newLongValue(100)
outputMap['example.float'] = newFloatValue(1.014)
outputMap['example.double'] = newDoubleValue(10.12)
outputMap['example.boolean'] = newBooleanValue(true)
outputMap['example.string'] = newStringValue('Hello World!')
outputMap['example.byte.array'] = newByteArrayValue(byteArray)

output[0] = newWireRecord(outputMap)
```



## Script context

The **script.context.drop** option allows to reset the script context. If set to `true` the script context will be dropped every time the component configuration is updated, resetting the value of any persisted variable.

In the example below, with **script.context.drop=false** the following script will preserve the value of `counter` across executions. Setting **script.context.drop=true** will cause `counter` to be `undefined` every time the component is triggered.

```javascript
counter = typeof(counter) === 'undefined'
 ? 0 // counter is undefined, initialise it to zero
 : counter; // counter is already defined, keep the previous value
```
