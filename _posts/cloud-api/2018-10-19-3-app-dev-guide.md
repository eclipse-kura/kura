---
layout: page
title:  "Application developer guide"
categories: [cloud-api]
---

This guide will provide information on how an application developer can leverage the new Generic Cloud Services APIs, in order to be able to properly use the CloudPublisher/CloudSubscriber API, publish a message, being notified of message delivery and of connection status changes.

The Kura [ExamplePublisher](https://github.com/eclipse/kura/tree/develop/kura/examples/org.eclipse.kura.example.publisher) will be used as a reference.

The application should bind itself to a `CloudPublisher` or `CloudSubscriber` instance, this can be done in different ways, such as using OSGi `ServiceTracker`s or by leveraging the Declarative Service layer.

The recommended way to perform this operation is choosing the latter and allowing the user to customize the service references through component configuration.

If the component metatype and definition are structured as described below, the Kura Web UI will show a dedicated widget in component configuration that helps the user to pick compatible `CloudPublisher` or `CloudSubscriber` instances.

1. **Write component definition**

  The first step involves declaring the Publisher or Subscriber references in component definition:

```xml
  <scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.1.0"
    name="org.eclipse.kura.example.publisher.ExamplePublisher"
    activate="activate"
    deactivate="deactivate"
    modified="updated"
    enabled="true"
    immediate="true"
    configuration-policy="require">
  <implementation class="org.eclipse.kura.example.publisher.ExamplePublisher"/>

   <!-- If the component is configurable through the Kura ConfigurationService, it must expose a Service. -->
   <property name="service.pid" type="String" value="org.eclipse.kura.example.publisher.ExamplePublisher"/>
   <service>
       <provide interface="org.eclipse.kura.configuration.ConfigurableComponent"/>
   </service>

   <reference name="CloudPublisher"
           policy="static"
           bind="setCloudPublisher"
           unbind="unsetCloudPublisher"
           cardinality="0..1"
           interface="org.eclipse.kura.cloudconnection.publisher.CloudPublisher"/>
   <reference name="CloudSubscriber"
           policy="static"
           bind="setCloudSubscriber"
           unbind="unsetCloudSubscriber"
           cardinality="0..1"
           interface="org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber"/>
  </scr:component>
```

  The snipped above shows the definition of Kura ExamplePublisher, this component is capable of sending and receiving messages, and therefore defines two references, the first to a `CloudPublisher` and the second to a `CloudSubscriber`.

  In order to allow the user to customize the bindings at runtime, the `target` attribute of the references should not be specified at this point in component definition, as it will be set by the Web UI.

  Reference cardinality should be use the `0..1` or `0..n` form, as it is not guaranteed that the references will point to a valid service instance during all the lifetime of the application component. For example, references can not be bound if the application has not been configured by the user yet or if the target service is missing.

2. **Create component metatype**

  Application metatype should declare an `AD` for each Publisher/Subscriber reference declared in component definition:

```xml
  <MetaData xmlns="http://www.osgi.org/xmlns/metatype/v1.2.0" localization="en_us">
      <OCD id="org.eclipse.kura.example.publisher.ExamplePublisher"
           name="ExamplePublisher"
           description="Example of a Configuring Kura Application.">

       <!-- ... -->

        <AD id="CloudPublisher.target"
            name="CloudPublisher Target Filter"
            type="String"
            cardinality="0"
            required="true"
            default="(kura.service.pid=changeme)"
            description="Specifies, as an OSGi target filter, the pid of the Cloud Publisher used to publish messages to the cloud platform.">
        </AD>

        <AD id="CloudSubscriber.target"
            name="CloudSubscriber Target Filter"
            type="String"
            cardinality="0"
            required="true"
            default="(kura.service.pid=changeme)"
            description="Specifies, as an OSGi target filter, the pid of the Cloud Subscriber used to receive messages from the cloud platform.">
        </AD>

        <!-- ... -->

        </OCD>
    <Designate pid="org.eclipse.kura.example.publisher.ExamplePublisher" factoryPid="org.eclipse.kura.example.publisher.ExamplePublisher">
        <Object ocdref="org.eclipse.kura.example.publisher.ExamplePublisher"/>
    </Designate>
  </MetaData>
```

  It is important to respect the following rules for some of the `AD` attributes:

  * `id`

    This attribute must have the following form:

    ```
    <reference name>.target
    ```

    where `<reference name>` should match the value of the `name` attribute of the corresponding reference in component definition.

  * `required` must be set to `true`

  * `default` must not be empty and must be a valid OSGi filter.

  The Web UI will renderer a dedicated widget for picking `CloudPublisher` and `CloudSubscriber` instances:

  ![cloud-connections](https://s3-us-west-2.amazonaws.com/kura-repo/kura-github-wiki-images/generic-cloud-services/cloud-connections-user-7.png)

3. **Write the bind/unbind methods in applicaiton code**

  The last step involves defining some `bind...()`/`unbind...()` methods with a name that matches the values of the `bind`/`unbind` attributes of the references in component definition.

  ```java
  public void setCloudPublisher(CloudPublisher cloudPublisher) {
  ...
  }

  public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
  ...
  }

  public void setCloudSubscriber(CloudSubscriber cloudSubscriber) {
  ...
  }

  public void unsetCloudSubscriber(CloudSubscriber cloudSubscriber) {
  ...
  }
  ```

  As stated above, since reference cardinality is declared as `0..`, the application must be prepared to handle the cases where references are not satisfied, and therefore `CloudPublisher` and `CloudSubscriber` instances are not available.

4. **Publish a message**

  If a `CloudPublisher` instance is bound, the application can publish messages using its `publish()` method:

```java
  if (nonNull(this.cloudPublisher)) {
    KuraMessage message = new KuraMessage(payload);
    String messageId = this.cloudPublisher.publish(message);
  }
```

5. **Receiving messages using a CloudSubscriber**

  In order to receive messages from a `CloudSubscriber`, the application must implement and attach a `CloudSubscriberListener` to it.

  This can be done for example during `CloudSubscriber` binding:

```java
  public class ExamplePublisher implements CloudSubscriberListener, ... {

  ...

   public void setCloudSubscriber(CloudSubscriber cloudSubscriber) {
    this.cloudSubscriber = cloudSubscriber;
    this.cloudSubscriber.registerCloudSubscriberListener(ExamplePublisher.this);
    ...
  }

  public void unsetCloudSubscriber(CloudSubscriber cloudSubscriber) {
    this.cloudSubscriber.unregisterCloudSubscriberListener(ExamplePublisher.this);
    ...
    this.cloudSubscriber = null;
  }

  ...

  @Override
  public void onMessageArrived(KuraMessage message) {
    logReceivedMessage(message);
  }

  ...

  }
```

  The CloudSubscriber will invoke the `onMessageArrived()` method when new messages are received.

5. **Receiving connection state notifications**

  If an application is interested in cloud connection status change events (connected, disconnected, etc), it can implement and attach a `CloudConnectionListener` to a `CloudPublisher` or `CloudSubscriber` instance.

```java
  public class ExamplePublisher implements CloudConnectionListener, ... {

  ...

  public void setCloudPublisher(CloudPublisher cloudPublisher) {
    this.cloudPublisher = cloudPublisher;
    this.cloudPublisher.registerCloudConnectionListener(ExamplePublisher.this);
    ...
  }

  public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
    this.cloudPublisher.unregisterCloudConnectionListener(ExamplePublisher.this);
    ...
    this.cloudPublisher = null;
  }

  public void setCloudSubscriber(CloudSubscriber cloudSubscriber) {
    this.cloudSubscriber = cloudSubscriber;
    ...
    this.cloudSubscriber.registerCloudConnectionListener(ExamplePublisher.this);
  }

  public void unsetCloudSubscriber(CloudSubscriber cloudSubscriber) {
    ...
    this.cloudSubscriber.unregisterCloudConnectionListener(ExamplePublisher.this);
    this.cloudSubscriber = null;
  }

  ...

  @Override
  public void onConnectionEstablished() {
    logger.info("Connection established");
  }

  @Override
  public void onConnectionLost() {
    logger.warn("Connection lost!");
  }

  @Override
  public void onDisconnected() {
    logger.warn("On disconnected");
  }

  ...

  }
```

6. **Receiving message delivery notifications**

  If an application is interested in message confirmation events and the underlying cloud connection supports it, it can implement and attach a `CloudDeliveryListener` to a `CloudPublisher` instance.

```java
  public class ExamplePublisher implements CloudDeliveryListener, ... {

  ...

  public void setCloudPublisher(CloudPublisher cloudPublisher) {
    this.cloudPublisher = cloudPublisher;
    ...
    this.cloudPublisher.registerCloudDeliveryListener(ExamplePublisher.this);
  }

  public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
    ...
    this.cloudPublisher.registerCloudDeliveryListener(ExamplePublisher.this);
    this.cloudPublisher = null;
  }

  ...

  @Override
  public void onMessageConfirmed(String messageId) {
    logger.info("Confirmed message with id: {}", messageId);
  }

  ...

  }
```

  The CloudSubscriber will invoke the `onMessageConfirmed()` method when a published message is confirmed.

  In order to determine which message has been confirmed, the provided `messageId` can be compared with the id returned by the `publish()` call that published the message.

  Please note that if the underlying cloud connection is not able to provide message confirmation for the published message, the id returned by `publish()` will be `null`.
