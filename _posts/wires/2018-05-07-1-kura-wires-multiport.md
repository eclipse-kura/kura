---
layout: page
title:  "Multiport wire components"
categories: [wires]
---

In order to allow a better routing of data through the Wire Graph, Kura introduces a new class of Wire components named **Multiport Wire Components**.
With the addition of this new functionality, a compatible component instance can be defined with an arbitrary number of input and output ports.

![conditional_join_components_graph]({{ site.baseurl }}/assets/images/wires/ConditionalJoinComponents.png)

In the example provided, we have two components in the Wire Composer:
- the **[Conditional](2-kura-wires-conditional.html)** component that implements the if-then-else logic
- the **[Join](3-kura-wires-join.html)** component that joins into a single Wire the data processed by two separate branches of a graph

Those components are available in every default installation of Kura.

In order to show the potentialities of the new APIs, in the Eclipse Kura Marketplace, are available few more multiport-enabled components for **[Mathematical](4-kura-wires-math.html)** processing.

### Convert a Component to a Multiport Component

#### Component Configuration Changes
The following properties need to be specified in the component configuration:
- **input.cardinality.minimum**: an integer that specifies the minimum number of input ports that the component can be configured to manage.
- **input.cardinality.maximum**: an integer that specifies the maximum number of input ports that the component can be configured to manage.
- **input.cardinality.default**: an integer that specifies the default number of input ports that the component will be created with, if not specified in a different way.
- **output.cardinality.minimum**: an integer that specifies the minimum number of output ports that the component can be configured to manage.
- **output.cardinality.maximum**: an integer that specifies the maximum number of output ports that the component can be configured to manage.
- **output.cardinality.default**: an integer that specifies the default number of output ports that the component will be created with, if not specified in a different way.
- **input.port.names**: an optional mapping between input ports and friendly names
- **output.port.names**: an optional mapping between output ports and friendly names

The component should also provide service interface **org.eclipse.kura.wire.WireComponent**

#### Code Changes
To leverage all the new Multiport functionalities, a Multiport-enabled component must use the newly introduced MultiportWireSupport APIs that provide support to get the list of Emitter and Receiver Ports, as well as to create a new Wire Envelope from a list of Wire Records.

For the conditional component, that has two output ports, the following code allows to get the proper Wire Support from the Wire Helper Service and to get the then and else ports to be used to push the processed envelopes. 

```java
this.wireSupport = (MultiportWireSupport) 
this.wireHelperService.newWireSupport(this);
        final List<EmitterPort> emitterPorts = this.wireSupport.getEmitterPorts();
        this.thenPort = emitterPorts.get(0);
        this.elsePort = emitterPorts.get(1);
```

To emit the result, the code has to be adapted to use the Wire Support to create the Wire Envelope that has to be sent. Effectively, the envelope is sent to the corresponding wire invoking the **emit** method of the corresponding Port, as shown below:

```java
final WireEnvelope outputEnvelope = this.wireSupport.createWireEnvelope(inputRecords);

if ((Boolean) decision) {
    this.thenPort.emit(outputEnvelope);
} else {
    this.elsePort.emit(outputEnvelope);
}
````
