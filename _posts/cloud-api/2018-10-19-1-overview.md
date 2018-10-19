---
layout: page
title:  "Overview"
categories: [cloud-api]
---

This section describes the new cloud related concepts and APIs introduced in Kura 4.0.

* [Motivations](#motivations)
* [Concepts](#concepts)
  * [CloudEndpoint](#cloudendpoint)
  * [CloudConnectionManager](#cloudconnectionmanager)
  * [Publihsers and Subscribers](#publihsers-and-subscribers)
  * [Command and control](#command-and-control)
* [Cloud connection lifecycle](#cloud-connection-lifecycle)
* [Backwards compatibility](#backwards-compatibility)

## Motivations

Before Kura 4.0, Cloud APIs were quite tied to Kapua messaging conventions and to the MQTT protocol. Defining custom stacks that support other cloud platforms was possible, but the resulting implementations were affected by the following limitations:

* The legacy APIs assume that the underlying messaging protocol is MQTT. This assumption spans across all API layers, from the low level `MQTTDataTrasport` to the high level `CloudClient`. This makes quite difficult to implement cloud stacks that use other protocols like AMQP or HTTP.

* The `CloudClient` API, which was the recommended way for applications to interface with a cloud stack, enforce the following MQTT topic structure:

  ```
  #account-name/#device-id/#app-id/<app-topic>
  ```

  This topic hierarchy, which is Kapua related, might be too restrictive or too loose for other cloud platforms, for example:

  * The **Eclipse IoT** working group namespace allows authenticated devices to omit the `accont-name` and `device-id` parameters in the topic. Moreover, telemetry, alert and event message topics must start respectively the `t/`, `a/` and `e/` prefixes. Adhering to this specification is not possible for a cloud stack that implements the legacy APIs.

  * The **AWS** cloud platform allows publishing on virtually any topic, using a `CloudClient` would be quite restrictive in this case. A way for overcoming this limitation for an application might be using the DataService layer directly, adversely affecting portability.

  * The **Cumulocity** cloud platform allows publishing only on a limited set of topics, and most of the application generated information is placed in the payload encoded in CSV. Using `CloudClient` in this case makes difficult for the cloud stack to enforce that the messages are published on the correct topics.
  Moreover, the cloud stack in this case must also convert from `KuraPayload` to CSV, this can be currently achieved only by introducing rigid conversion rules, that might not be enough to support all message formats.

* Applications that use the current APIs are not portable across cloud platforms. For example if an appliaction intends to publish on Cumulocity or AWS, it should be probably aware of the underlying cloud platform.

## Concepts

The main interfaces of the new set of APIs and their interactions are depicted in the diagram below:

![Overview](https://s3-us-west-2.amazonaws.com/kura-repo/kura-github-wiki-images/generic-cloud-services/Overview.jpg)

As shown in the above diagram new APIs introduce the concept of **Cloud Connection**, a set of related services that allow to manage the communication to/from a remote cloud platform.

The services that compose a **Cloud Connection** can implement the following cloud-specific interfaces:

* **CloudEndpoint** (required): Each Cloud Connection is identified by a single instance of **CloudEndpoint** that implements low level specificities for the communication with the remote cloud platform.

* **CloudConnectionManager** (optional): Exposes methods that allow to manage long-lived connections. The implementor of **CloudEndpoint** can implement this interface as well if the cloud platform support long-lived connections (for example by using the MQTT protocol).

* **RequestHandlerRegistry** (optional): Manages the command and control functionalities if supported by the cloud platform.

* **CloudPublisher** (optional): Allows applications to publish messages to the cloud platform in a portable way.

* **CloudSubscriber** (optional): Allows applications to receive messages from the cloud platform in a portable way.

* **CloudConnectionFactory** (required): Manages the lifecycle of Cloud Connections.

A **Cloud Connection** can also include services that do not provide any of the interfaces above but compose the internal implementation.

### CloudEndpoint

Every Cloud Connection must contain a single `CloudEndpoint` instance.
The `kura.service.pid` of the `CloudEndpoint` identifies the whole Cloud Connection.

The `CloudEndpoint` provides some low level methods that can be used to interact with the remote cloud platform.

For example the interface provides the `publish()` and `subscribe()` methods that allow to publish or receive messages from the cloud platform in form of `KuraMessage`s.
Those methods are designed for internal use and are not intended to be used by end-user applications.

The format of the `KuraMessage` provided to/received from a `CloudEndpoint` is implementation specific: the `CloudEndpoint` expects some properties to be set in the KuraMessage to be able to correctly publish a message (e.g. MQTT topic). These properties are specified by the particular `CloudEndpoint`, and should be documented by the implementor.

The recommended way for applications to publish and receive messages involves using the Publisher and Subscriber APIs described below.
If an application directly uses the methods above, it will lose portability and will be tied to the specific Cloud Connection implementation.

### CloudConnectionManager

If the messaging protocol implemented by a Cloud Connection supports long-lived connection, then its `CloudEndpoint` can also implement and provide the `CloudConnectionManager` interface.

This interface exposes some methods that can be used to manage the connection like `connect()`, `disconnect()` and `isConnected()`; it also supports monitoring connection state using the `CloudConnectionListener` interface.

### Publishers and Subscribers

The limitations of the current model described above are addressed by the introduction of the `CloudPublisher` and `CloudSubscriber` APIs, that replace the `CloudClient` as the recommended interface between applications and cloud stacks. `CloudPublisher` and `CloudSubscriber` are service interfaces defined as follows:

```java
public interface CloudPublisher {

    public String publish(KuraMessage message) throws KuraException;

    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener);

    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener);

    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener);

    public void unregisterCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener);

}
```

```java
public interface CloudSubscriber {

    public void registerCloudSubscriberListener(CloudSubscriberListener listener);

    public void unregisterCloudSubscriberListener(CloudSubscriberListener listener);

    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener);

    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener);

}
```

#### CloudPublisher

The `CloudPublisher` interface should be used by applications for publishing messages using the single `publish()` method. This method accepts a `KuraMessage` which is basically a `KuraPayload` that can be enriched with metadata.

The main difference with the `CloudClient` APIs is that the `publish()` method does not require the application to specify any information related to message destinations.
This allows to write portable applications that are unaware of the low level details of the destination cloud platform, such as the message format and the transport protocol.

#### CloudSubscriber

An application designed to receive messages from the cloud must now attach a listener  (`CloudSubscriberListener`) to a `CloudSubscriber` instance.

In this case, the message source cannot be specified by the application but is defined by the subscriber instance, in the same way as the `CloudPublisher` defines destination for published messages.

The low level details necessary for message delivery and reception (e.g. the MQTT topic and the conversion between `KuraMessage` and the message format used on the wire) are managed by the publisher/subscriber, typically these details are stored in the service configuration.

While in the previous model an application was responsible to actively obtain a `CloudClient` instance from a `CloudService`, now the relation between the application and a CloudPublisher or CloudSubscriber instance is represented as an OSGi service reference. Applications should allow the user to modify this reference in configuration, making it easy to switch between different cloud publisher/subscriber instances and different cloud platforms.

Publisher/subscriber instances are now typically instantiated and configured by the end user using the Web UI.

Publisher/subscriber instances are related to a `CloudEnpoint` instance using an OSGi service reference encoded in well known configuration property specified in the APIs (`CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME`). This allows the user to create those instances in a dedicated section of the Web UI.

### Command and control

Another field in which the current Kura cloud related APIs can be generalized is related to command and control. In the previous model this aspect was covered by the `Cloudlet` APIs that are now replaced by `RequestHandler` APIs

Legacy `Cloudlet` implementations are defined by extending a base class, `Cloudlet`, which takes care of handling the invocation of the `doGet()`, `doPut()`, `doPost()` ... methods, and of correlating request and response messages. Messages were sent and received through a `CloudClient`.

More explicitly, `Cloudlet` only works with control topics whose structure is
```
$EDC/<account-name>/<device-id>/<app-id>/<method>/<resource-path>
```
and also expects the identifier of the sender and the correlation identifier in the `KuraPayload`.

In the previous model, there is no way for a cloud stack implementor to customize the aspects above, which are hardcoded in the `Cloudlet` base class.

The new model delegates these aspects to some component of the cloud stack, and requires applications that want to support command and control to register themselves as `RequestHandler` to a `RequestHandlerRegistry` instance.

In order to ease porting old applications to the new model, some of the concepts of the old Cloudlet APIs are still present, this can be seen by looking at the `RequestHandler` interface definition:

```java
public interface RequestHandler {

    public KuraMessage doGet(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException;

    public KuraMessage doPut(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException;

    public KuraMessage doPost(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException;

    public  KuraMessage doDel(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException;

    public KuraMessage doExec(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException;
}
```

A `RequestHandler` invocation involves the following parameters:

Request parameters:

* **method**: (GET, PUT, POST, DEL, EXEC) that identifies the `RequestHandler` method to be called
* **request message**: A set of key-value pairs and/or binary body contained in the `KuraPayload` wrapped inside the `KuraMessage`.
* **resources**: A `List<String>` of positional parameters available under the well known `args` key in the provided `KuraMessage` properties.

Response parameters:

* **response message**: A set of key-value pairs and/or binary body contained in the `KuraPayload` wrapped inside the returned `KuraMessage`.
* **status**: A numeric code reporting operation completion state, determined as follows:
  * 200, if `RequestHandler` methods returns without throwing exceptions.
  * 400, if `RequestHandler` methods throws a `KuraException` with `KuraErrorCode` == BAD_REQUEST
  * 404, if `RequestHandler` methods throws a `KuraException` with `KuraErrorCode` == NOT_FOUND
  * 500, if `RequestHandler` methods throws a `KuraException` with other error codes.
* **exception message**: The message of the `KuraException` thrown by the `RequestHandler` methods, if any.
* **exception stack trace**: The stack trace of the `KuraException` thrown by the `RequestHandler` methods, if any.

The parameters above are the same involved in current Cloudlet calls. The request id and requester client id parameters are no longer part of the API, because are related to the to the way Kapua correlates requests and response. In the new API, request and response identifiers are not specified and not forwarded to the Cloudlet, this allows the CloudletService implementation to adopt the platform specific conventions for message correlation.

The Cloudlet parameters must be present in the request and response messages encoded in some format. A user that intends to call a Kura Cloudlet, for example through platform-specific REST APIs must be aware of these parameters. The user must supply request parameters in request message and must be able to extract response data from received message. The actual encoding of these parameters inside the messages depends on the particular platform.

The fact that set of Cloudlet parameters are roughly the same involved in current Cloudlet calls allows existing Cloudlet based applications to continue to work without changes to the protocol.

## Cloud Connection lifecycle

`CloudEndpoint` instance lifecycle is managed by a `CloudConnectionFactory` instance.
A cloud connection implementor must register a `CloudConnectionFactory` instance in the framework that is responsible of creating and destroying the `CloudEndpoint` instances.

The `CloudConnectionFactory` will be typically invoked by the Web UI, and is defined as follows:

```java
public interface CloudConnectionFactory {

    public static final String KURA_CLOUD_CONNECTION_FACTORY_PID = "kura.cloud.connection.factory.pid";

    public String getFactoryPid();

    public void createConfiguration(String pid) throws KuraException;

    public List<String> getStackComponentsPids(String pid) throws KuraException;

    public void deleteConfiguration(String pid) throws KuraException;

    public Set<String> getManagedCloudConnectionPids() throws KuraException;

}
```

The `createConfiguration()` and `deleteConfiguration()` methods are responsible of creating/destroying a `CloudEndpoint` instance, specified by the provided `kura.service.pid`, and all the related services.

The `getManagedCloudConnectionPids()` returns the set of `kura.service.pid` managed by the factory.

The `getStackComponentsPids(String pid)` returns the list of the `kura.service.pid`s of the `ConfigurableComponent`s that are associated with the `CloudEndpoint` with the specified pid. The Web Ui will render the configuration of those components in separated tabs, in the dedicated CloudConnections section.

## Backwards compatibility

In order to ease the transition to the new model, legacy APIs like `CloudService` and `CloudClient` are still supported in Kura 4.0.0, even if deprecated.

The default Kapua oriented `CloudService` implementation is still available and can be used by legacy applications without changes.
The default `CloudService` instance in Kura 4.0 also implements the new `CloudEndpoint` and `CloudConnectionManager` interfaces.
