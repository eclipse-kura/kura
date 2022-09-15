# WireGraphService Configuration Format

This document describes the configuration format for the WireGraphService component.

The WireGraphService configuration contains all the information related to the Wire Graph topology and rendering properties.
The pid of the WireGraphService configuration is `org.eclipse.kura.wire.graph.WireGraphService`.

The WireGraphService configuration represents the current graph layout as a single string typed property named `WireGraph` that represents a serialized JSON representation of a WireGraph object.



## JSON definitions

### Position

An object representing a Wire Component position.

**Properties**:

* **x**: `number`
  * **optional** If not specified, 0.0 will be used as default value
    The x coordinate of the Wire Component inside the graph canvas
* **y**: `number`
  * **optional** If not specified, 0.0 will be used as default value
    The y coordinate of the Wire Component inside the graph canvas

```json
{
  "x": 40,
  "y": 0
}
```
```json
{
  "x": 1.5
}
```
```json
{}
```
### PortNameList
An object that specifies custom names for Wire Component input and output ports. The properties name for this object must be represented as an integer starting from 0, matching the index of the port whose name needs to be assigned. If the property name is not specified, the default port name will be used.

**Properties**:

* **_portIndex**: `string`
    The name for the port of index _portIndex

```json
{
  "0": "foo",
  "1": "bar"
}
```
```json
{}
```

### RenderingProperties
An object describing some Wire Component rendering parameters like position and custom port names.

**Properties**:

* **position**: `object`
  * **optional** If not specified the component coordinates will be set to 0.0.
    * [Position](#position)
* **inputPortNames**: `object`
  * **optional** If not specified, the default input port names will be used.
    * [PortNameList](#portnamelist)
* **outputPortNames**: `object`
  * **optional** If not specified, the default output port names will be used.
    * [PortNameList](#portnamelist)

```json
{
  "inputPortNames": {},
  "outputPortNames": {
    "0": "foo",
    "1": "bar"
  },
  "position": {
    "x": 40,
    "y": 0
  }
}
```
```json
{
  "inputPortNames": {},
  "outputPortNames": {
    "0": "foo",
    "1": "bar"
  }
}
```
```json
{
  "position": {
    "x": 40,
    "y": 0
  }
}
```
```json
{}
```

### WireComponent
An object that describes a Wire Component that is part of a Wire Graph

**Properties**:

* **pid**: `string`
    The Wire Component pid
* **inputPortCount**: `number`
    An integer reporting the number of input ports of the Wire Component.
* **outputPortCount**: `number`
    An integer reporting the number of output ports of the Wire Component.
* **renderingProperties**: `object`
  * **optional** If not specified, the default rendering properties will be used
    * [RenderingProperties](#renderingproperties)

```json
{
  "inputPortCount": 1,
  "outputPortCount": 2,
  "pid": "cond",
  "renderingProperties": {
    "inputPortNames": {},
    "outputPortNames": {
      "0": "foo",
      "1": "bar"
    },
    "position": {
      "x": 40,
      "y": 0
    }
  }
}
```
```json
{
  "inputPortCount": 0,
  "outputPortCount": 1,
  "pid": "timer",
  "renderingProperties": {
    "inputPortNames": {},
    "outputPortNames": {},
    "position": {
      "x": -220,
      "y": -20
    }
  }
}
```

### Wire

An object that describes a Wire connecting two Wire Components.

**Properties**:

* **emitter**: `string`
    The pid of the emitter component.
* **emitterPort**: `number`
    The index of the output port of the emitter component that is connected to this Wire.
* **receiver**: `string`
    The pid of the receiver component.
* **receiverPort**: `number`
    The index of the input port  of the receiver component that is connected to this Wire.

```json
{
  "emitter": "timer",
  "emitterPort": 0,
  "receiver": "cond",
  "receiverPort": 0
}
```

### WireGraph

An object that describes the topology and rendering properties of a Wire Graph

**Properties**:

* **components**: `array`
    The list of the wire components contained in the Wire Graph
    * array elements: `object`
      * [WireComponent](#wirecomponent)
* **wires**: `array`
    The list of Wires contained in the Wire Graph
    * array elements: `object`
      * [Wire](#wire)

```json
{
  "components": [
    {
      "inputPortCount": 0,
      "outputPortCount": 1,
      "pid": "timer",
      "renderingProperties": {
        "inputPortNames": {},
        "outputPortNames": {},
        "position": {
          "x": -220,
          "y": -20
        }
      }
    },
    {
      "inputPortCount": 1,
      "outputPortCount": 2,
      "pid": "cond",
      "renderingProperties": {
        "inputPortNames": {},
        "outputPortNames": {
          "0": "foo",
          "1": "bar"
        },
        "position": {
          "x": 40,
          "y": 0
        }
      }
    }
  ],
  "wires": [
    {
      "emitter": "timer",
      "emitterPort": 0,
      "receiver": "cond",
      "receiverPort": 0
    }
  ]
}
```