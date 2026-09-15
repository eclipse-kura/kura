# Application developer guide

This guide will provide information on how an application developer can leverage the new Generic Cloud Services APIs, in order to be able to properly use the CloudPublisher/CloudSubscriber API, publish a message, being notified of message delivery and of connection status changes.

The Kura [ExamplePublisher](https://github.com/eclipse-kura/kura-apps/tree/develop/kura-examples/bundles/publishers/org.eclipse.kura.example.publisher) will be used as a reference.

The application should bind itself to a `CloudPublisher` or `CloudSubscriber` instance, this can be done in different ways, such as using OSGi `ServiceTracker`s or by leveraging the Declarative Service layer.

The recommended way to perform this operation is choosing the latter and allowing the user to customize the service references through component configuration.

If the component declaration and metatype are structured as described below, the Kura Web UI will show a dedicated widget in component configuration that helps the user to pick compatible `CloudPublisher` or `CloudSubscriber` instances.

1. **Declare the component and its references**

    The first step involves declaring the Publisher or Subscriber references in the component class, with the Declarative Services annotations (bnd generates the `OSGI-INF` descriptor from them at build time):

    ```java
    @Component(name = "org.eclipse.kura.example.publisher.ExamplePublisher",
            immediate = true,
            configurationPolicy = ConfigurationPolicy.REQUIRE,
            // If the component is configurable through the Kura ConfigurationService, it must provide the service.
            service = ConfigurableComponent.class)
    @Designate(ocd = ExamplePublisherOCD.class, factory = true)
    public class ExamplePublisher implements ConfigurableComponent, ... {

        @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
        public void setCloudPublisher(CloudPublisher cloudPublisher) {
        ...
        }

        public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
        ...
        }

        @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
        public void setCloudSubscriber(CloudSubscriber cloudSubscriber) {
        ...
        }

        public void unsetCloudSubscriber(CloudSubscriber cloudSubscriber) {
        ...
        }

        @Activate
        protected void activate(Map<String, Object> properties) { ... }

        @Modified
        public void updated(Map<String, Object> properties) { ... }

        @Deactivate
        protected void deactivate() { ... }
    }
    ```

    The snippet above shows the declaration of the Kura ExamplePublisher, this component is capable of sending and receiving messages, and therefore defines two references, the first to a `CloudPublisher` and the second to a `CloudSubscriber`. The name of a reference defaults to the name of its bind method without the `set` prefix (`CloudPublisher` and `CloudSubscriber` here) and the unbind method to the same name with the `unset` prefix; both can be set explicitly with the `name` and `unbind` attributes of `@Reference`.

    In order to allow the user to customize the bindings at runtime, the `target` attribute of the references should not be specified at this point in the component declaration, as it will be set by the Web UI through the `<reference name>.target` configuration property.

    Reference cardinality should use the `OPTIONAL` or `MULTIPLE` form (`0..1` or `0..n`), as it is not guaranteed that the references will point to a valid service instance during all the lifetime of the application component. For example, references can not be bound if the application has not been configured by the user yet or if the target service is missing.

2. **Create the component metatype**

    The application metatype should declare an attribute for each Publisher/Subscriber reference declared in the component. It is written as an `@ObjectClassDefinition` annotated interface, which bnd turns into the `OSGI-INF/metatype` file:

    ```java
    @ObjectClassDefinition(id = "org.eclipse.kura.example.publisher.ExamplePublisher",
            name = "ExamplePublisher",
            description = "Example of a Configuring Kura Application.")
    public @interface ExamplePublisherOCD {

        // ...

        @AttributeDefinition(name = "CloudPublisher Target Filter",
                type = AttributeType.STRING,
                cardinality = 0,
                required = true,
                defaultValue = "(kura.service.pid=changeme)",
                description = "Specifies, as an OSGi target filter, the pid of the Cloud Publisher used to publish messages to the cloud platform.")
        String CloudPublisher_target();

        @AttributeDefinition(name = "CloudSubscriber Target Filter",
                type = AttributeType.STRING,
                cardinality = 0,
                required = true,
                defaultValue = "(kura.service.pid=changeme)",
                description = "Specifies, as an OSGi target filter, the pid of the Cloud Subscriber used to receive messages from the cloud platform.")
        String CloudSubscriber_target();

        // ...
    }
    ```

    It is important to respect the following rules for some of the attribute definitions:

    * `id`

    The id of the attribute is derived from the method name, with `_` mapped to `.`, and must have the following form:

    ```
    <reference name>.target
    ```

    where `<reference name>` should match the name of the corresponding `@Reference` in the component declaration: the `CloudPublisher_target()` method above produces the `CloudPublisher.target` attribute.

    * `required` must be set to `true`

    * `defaultValue` must not be empty and must be a valid OSGi filter.

    The Web UI will render a dedicated widget for picking `CloudPublisher` and `CloudSubscriber` instances:

    ![cloud-connections](https://s3-us-west-2.amazonaws.com/kura-repo/kura-github-wiki-images/generic-cloud-services/cloud-connections-user-7.png)

3. **Write the bind/unbind methods in application code**

    The last step involves implementing the bind/unbind methods annotated above. The bind method carries the `@Reference` annotation; the unbind method is the one named by its `unbind` attribute, by default the bind method name with `set` replaced by `unset`. The `DYNAMIC` policy lets the user change the target at runtime without reactivating the component.

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

    As stated above, since reference cardinality is declared as optional, the application must be prepared to handle the cases where references are not satisfied, and therefore `CloudPublisher` and `CloudSubscriber` instances are not available.

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

6. **Receiving connection state notifications**

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

7. **Receiving message delivery notifications**

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
