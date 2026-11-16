# Built-in Publisher and Subscriber

The default cloud connection of Eclipse Kura, used to connect to Eclipse Kapua&trade; and Eurotech Everyware Cloud&trade;, provides a `CloudPublisher` and a `CloudSubscriber` implementation:

- `org.eclipse.kura.cloud.publisher.CloudPublisher`, which publishes the messages of the applications attached to it;
- `org.eclipse.kura.cloud.subscriber.CloudSubscriber`, which subscribes to a topic and delivers the received messages to the applications attached to it.

Both are created from the **Cloud Connections** section of the Web UI, selecting the cloud connection and then the factory in the **Available Publisher/Subscriber factories** list, as described in the [User Guide](user-guide.md#creating-and-configuring-a-new-publisher). Each instance is bound to the cloud connection that was selected when it was created.

## Topic structure

The publisher and the subscriber build their MQTT topic from the configuration of the cloud connection and from their own **Application Id** and **Application Topic**:

```
[<control-prefix>/]<account-name>/<client-id>/<Application Id>/<Application Topic>
```

- `<control-prefix>` is the `topic.control-prefix` of the [CloudService](built-in-cloud.md#cloudservice), `$EDC` by default. It is added only when **Kind of Message** is set to **Control**.
- `<account-name>` and `<client-id>` are the `topic.context.account-name` and `client-id` of the [MqttDataTransport](built-in-cloud.md#mqttdatatransport).
- When the **Application Topic** is empty, the topic ends with the **Application Id**.

For example, a subscriber with the default configuration subscribes to `<account-name>/<client-id>/W1/A1/#`.

## CloudPublisher

The `CloudPublisher` provides the following configuration parameters:

- **Application Id** - the application id used to publish the messages. The default value is `W1`.
- **Application Topic** - the part of the topic that follows the **Application Id**. The default value is `A1/$assetName`. A topic level written as `$<name>` is a placeholder: the publisher replaces it with the value of the `<name>` property of the `KuraMessage` passed to `publish()`. If the message does not contain that property, the placeholder is published unchanged. For example, with `A1/$assetName` a message with the `assetName` property set to `asset1` is published on `<account-name>/<client-id>/W1/A1/asset1`.
- **Qos** - the MQTT quality of service of the published messages, `0` (at most once) or `1` (at least once). The default value is `0`.
- **Retain** - the MQTT retain flag of the published messages. The default value is `false`.
- **Kind of Message** - **Data** or **Control**. **Control** messages are published on the topic that starts with the `topic.control-prefix`. The default value is **Data**.
- **Priority** - the priority of the messages in the DataService queue. Priority level `0` is the highest and should be reserved for the messages that need to be sent with the minimum latency. The default value is `7`.

## CloudSubscriber

The `CloudSubscriber` provides the following configuration parameters:

- **Application Id** - the application id used to receive the messages. The default value is `W1`.
- **Application Topic** - the part of the subscription topic that follows the **Application Id**. It can contain the MQTT wildcards `+` and `#`. The default value is `A1/#`.
- **Qos** - the MQTT quality of service of the subscription, `0` or `1`. The default value is `0`.
- **Kind of Message** - **Data** or **Control**. With **Control**, the subscription topic starts with the `topic.control-prefix`. The default value is **Data**.

The subscriber always subscribes to the topics of its own device: the `<account-name>` and `<client-id>` levels are taken from the MqttDataTransport configuration.

When a message arrives, the subscriber decodes its payload according to the `payload.encoding` of the CloudService and calls the `onMessageArrived()` method of each attached `CloudSubscriberListener`. The `KuraMessage` passed to the listeners carries the following properties:

- `deviceId` - the `<client-id>` level of the topic on which the message was received;
- `appTopic` - the part of the topic that follows the **Application Id**, for example `A1/sensor1` for a message received on `<account-name>/<client-id>/W1/A1/sensor1`.

### Topic matching

The subscriber delivers to the listeners the messages whose topic matches its subscription topic, following the rules of the [MQTT 3.1.1 specification](https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html) (section 4.7.1). A subscription topic ending with the multi-level wildcard `#` also matches its parent level: with the default **Application Topic** `A1/#`, the listeners receive both the messages published on `<account-name>/<client-id>/W1/A1/sensor1` and those published on `<account-name>/<client-id>/W1/A1`. In the latter case the `appTopic` property of the message is `A1`.

!!! note
    Before Kura 6.0.0, the subscriber discarded the messages published on the parent level of a subscription topic ending with `#`: with the **Application Topic** `A1/#`, the messages published on `<account-name>/<client-id>/W1/A1` were not delivered to the listeners. Starting from Kura 6.0.0 they are delivered, so an application attached to a subscriber whose **Application Topic** ends with `#` may receive messages that it did not receive before.
