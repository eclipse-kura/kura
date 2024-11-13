## Raw MQTT Cloud Connection

The Raw MQTT cloud connector is a cloud stack that exposes a MQTT cloud connection without applying any restriction on topic structure or performing any particular message encoding.

The Raw MQTT cloud connector is composed of the following components:

### Cloud stack components

The cloud stack is composed by the default [DataService](../gateway-configuration/data-service-configuration.md) and [MqttDataTransport](../gateway-configuration/data-transport-service-configuration.md) plus the following component:

#### org.eclipse.kura.cloudconnection.raw.mqtt.cloud.RawMqttCloudEndpoint

The [CloudEndpoint](https://download.eclipse.org/kura/docs/api/5.5.0/apidocs/org/eclipse/kura/cloudconnection/CloudEndpoint.html) implementation, this component does not have any configuration property.

### Publishers and subscriber

The following `CloudPublisher` and `CloudSubscriber` factories are available:

#### org.eclipse.kura.cloudconnection.raw.mqtt.publisher.RawMqttPublisher

A [CloudPublisher](https://download.eclipse.org/kura/docs/api/5.5.0/apidocs/org/eclipse/kura/cloudconnection/publisher/CloudPublisher.html) implemetation that can be attached to a `RawMqttCloudEndpoint`.

The `RawMqttPublisher` provides the following configuration options:

* **Topic**: The MQTT topic to publish messages on.

* **Qos**: The desired quality of service for the messages that have to be published. If Qos is 0, the message is delivered at most once, or it is not delivered at all. If Qos is set to 1, the message is always delivered at least once. If set to 2, the message will be delivered exactly once.

* **Retain**: The MQTT retain flag for the published messages.

* **Priority**: The priority of the messages. 0 is highest priority. This parameter is related to the DataService component of the cloud stack.

The `RawMqttPublisher` will consider only the body field of the [KuraPayload](https://download.eclipse.org/kura/docs/api/5.5.0/apidocs/org/eclipse/kura/message/KuraPayload.html)s submitted for publishing. All other KuraPayload fields like metrics or position information will be discarded.

The received KuraPayload body field will be used as the payload of the published messages without any modification.

#### org.eclipse.kura.cloudconnection.raw.mqtt.subscriber.RawMqttSubscriber

A [CloudSubscriber](https://download.eclipse.org/kura/docs/api/5.5.0/apidocs/org/eclipse/kura/cloudconnection/subscriber/CloudSubscriber.html) implementation that can be attached to a `RawMqttCloudEndpoint`.

The `RawMqttSubscriber` provides the following configuration options:

* **Topic Filter**: The MQTT subscription topic filter. For example `foo/bar/baz`, `foo/+/bar`, `#`, `foo/#`.

* **Qos**: The desired quality of service for the subscription messages.

The `RawMqttSubscriber` will popolate the body field of the KuraPayload provided to consumers using the payload of received MQTT messages without any modification.

## Using the Raw MQTT Cloud Conection from Kura Wires

Using the Raw MQTT Cloud Conection from Kura Wires is possible but requires a few extra configuration steps.

### Publishig messages

As said previously, the `RawMqttPublisher` component will only consider the body of received KuraPayloads. The default behaviour of the Wires **CloudPublisher** component is to populate KuraPayload metrics using the received wire record properties, without filling the body. It is possible to configure it to populate the body in the following way:

* Create a **CloudPublisher** wire component and attach it to a `RawMqttPublisher` instance.

* Set the value of the **Set body from envelope property** of the **CloudPublisher** instance to the name of an input wire record property of STRING or BYTE_ARRAY type that the component will receive.

In this way every time that the **CloudPublisher** will receive an envelope with a property whose name matches the configured one, it will use its value as the body of the produced KuraPayload.

The input property must be of STRING or BYTE_ARRAY type:

  * if the type is BYTE_ARRAY the value will be used as KuraPayload body without modifications.

  * if the type is STRING the value will be ecoded in UTF-8.

### Receiveing messages

It is also possible to receive the messages originating form a `RawMqttSubscriber` using a Wires **CloudSubscriber** component with additional configuration. By default the **CloudSubscriber** component will not emit KuraPayload body on the wires but it is possible to change this behaviour in the following way:

* Create a **CloudSubscriber** wire component and attach it to a `RawMqttSubscriber` instance.

* Set the value of the **Set property from message body** configuration property to the name of a wire record property that will be used to store the KuraPayload body

* Set the value of the **Body property type** to STRING or BYTE_ARRAY:

    * If set to BYTE_ARRAY, the body will be emitted without modifications.

    * If set to STRING, the body will be decoded from UTF-8 and emitted as a string.
