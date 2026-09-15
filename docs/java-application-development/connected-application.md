# Connected Application

## Overview

This section describes the heater demo bundle of the [kura-apps](https://github.com/eclipse-kura/kura-apps) repository and demonstrates how to perform the following functions:

* Bind an application to a cloud connection through a `CloudPublisher`

* Connect to the Cloud

* Gain an understanding of ConfigurableComponents in Kura

* Modify configurations of custom bundles

### Prerequisites

* [Configurable Application](./configurable-application.md)

* Using the Kura web UI

* A device running Kura 6 (or the [Kura Docker image](../getting-started/docker-quick-start.md)) with Internet access, so it can publish to an MQTT broker

## Heater Demo Introduction

The `org.eclipse.kura.demo.heater` bundle is a simple OSGi bundle that represents a thermostat and heater combination. The application utilizes the Kura ConfigurableComponent interface to be able to receive configuration updates through the local Kura web UI. In addition, this bundle utilizes OSGi Declarative Services and the Kura `CloudPublisher` API to publish its telemetry. This tutorial demonstrates how to modify configurations of custom bundles and shows how those configuration changes can dynamically impact the behavior of the bundle through the Kura web UI.

The source code is in the `kura-examples/bundles/heater/org.eclipse.kura.demo.heater` module of the kura-apps repository; the `kura-apps-distrib/kura-examples/heater` module builds its Debian installer.

## Code Walkthrough

The following sections highlight the API layers involved when creating an application that publishes to the cloud. These layers are:

* DataTransportService
    * Available for standard MQTT messaging. Allows consumers of the service to connect to brokers, publish messages, and receive messages on subscribed topics
* DataService
    * Delegates data transport to the DataTransportService
    * Provides extended features for managing broker connections, buffering of published messages, and priority based delivery of messages
* CloudEndpoint and CloudConnectionManager
    * Further extend the functionality of DataService
    * Manage a single broker connection across multiple applications
    * Provide the payload data model with encoding/decoding serializers
    * Publish life cycle messages for devices and applications
* CloudPublisher and CloudSubscriber
    * The application-facing API: a publisher (or subscriber) instance is created by the user in the web UI for a given cloud connection, and the application only binds to it

An application never talks to the cloud connection directly: it declares a reference to a `CloudPublisher` and the user chooses, in the web UI, which publisher of which cloud connection the application uses. The [Application developer guide](../cloud-api/app-dev-guide.md) describes the conventions in detail.

!!! info "Legacy CloudService and CloudClient"
    The `CloudService` and `CloudClient` API used by older versions of this demo are deprecated: they are tied to a single cloud connection implementation. New applications must use the `CloudPublisher`/`CloudSubscriber` API described here.

## Acquiring a CloudPublisher

The heater declares an optional, dynamic reference to a `CloudPublisher`. The reference has no `target`: the ConfigurationService sets it from the `CloudPublisher.target` property of the component configuration, chosen by the user in the web UI. When the publisher is bound, the heater registers itself to receive connection and delivery notifications. The relevant code is shown below (omitted sections are denoted by `==OMITTED==`):

```java
==OMITTED==

@Component(immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = { ConfigurableComponent.class, CloudConnectionListener.class, CloudDeliveryListener.class },
        name = "org.eclipse.kura.demo.heater.Heater")
@Designate(ocd = HeaterOCD.class)
public class Heater implements ConfigurableComponent, CloudConnectionListener, CloudDeliveryListener {

    private CloudPublisher cloudPublisher;

    ==OMITTED==

    @Reference(name = "CloudPublisher",
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetCloudPublisher",
            cardinality = ReferenceCardinality.OPTIONAL)
    public void setCloudPublisher(final CloudPublisher cloudPublisher) {
        this.cloudPublisher = cloudPublisher;
        this.cloudPublisher.registerCloudConnectionListener(Heater.this);
        this.cloudPublisher.registerCloudDeliveryListener(Heater.this);
    }

    public void unsetCloudPublisher(final CloudPublisher cloudPublisher) {
        this.cloudPublisher.unregisterCloudConnectionListener(Heater.this);
        this.cloudPublisher.unregisterCloudDeliveryListener(Heater.this);
        this.cloudPublisher = null;
    }
```

The configuration description (`HeaterOCD`) declares the matching attribute, whose id is the reference name followed by `.target`, so that the web UI renders the publisher picker:

```java
@AttributeDefinition(name = "CloudPublisher Target Filter",
        description = "Specifies, as an OSGi target filter, the pid of the Cloud Publisher used to publish messages to the cloud platform.",
        defaultValue = "(kura.service.pid=changeme)")
String cloudpublisher_target_filter();
```

The cardinality is `OPTIONAL` because the publisher may not exist yet when the heater is configured: the application must be prepared to run without it.

## Publishing

The private `doPublish` method is used to publish messages at a fixed rate. The method demonstrates how to use the `CloudPublisher` and `KuraPayload` to publish MQTT messages.

```java
==OMITTED==

// Allocate a new payload
KuraPayload payload = new KuraPayload();

// Timestamp the message
payload.setTimestamp(new Date());

// Add the temperature as a metric to the payload
payload.addMetric("temperatureInternal", this.temperature);
payload.addMetric("temperatureExternal", 5.0F);
payload.addMetric("temperatureExhaust", 30.0F);

int code = this.random.nextInt();
if ((this.random.nextInt() % 5) == 0) {
    payload.addMetric("errorCode", code);
} else {
    payload.addMetric("errorCode", 0);
}

// Publish the message
KuraMessage message = new KuraMessage(payload);
try {
    String messageId = this.cloudPublisher.publish(message);
    logger.info("Published message with id {}: {}", messageId, payload);
} catch (Exception e) {
    logger.error("Cannot publish message: {}", message, e);
}
```

The topic, the QoS and the retain flag are not chosen by the application: they belong to the configuration of the `CloudPublisher` instance the user created in the web UI. Similarly, a `CloudSubscriber` reference can be used to receive MQTT messages by registering a `CloudSubscriberListener` on it.

### Callback Methods

The example class implements `CloudConnectionListener` and `CloudDeliveryListener`, which provide the callbacks registered on the publisher in `setCloudPublisher`. The available methods for implementation are:

* onConnectionEstablished: called when the cloud connection establishes a connection with the broker.

* onConnectionLost: called when the cloud connection has lost the connection with the broker.

* onDisconnected: called when the cloud connection is closed on purpose.

* onMessageConfirmed: called when a published message has been fully acknowledged by the broker (not applicable for QoS 0 messages); the argument is the message id returned by `publish()`.

For more information on the various Kura APIs, please review the [Kura APIs](../references/javadoc.md).

## Run the Bundle

Build the kura-apps repository (`mvn clean install` from its root, JDK 21 and Maven 3.9.9+ as for Kura) and install the heater package produced under `kura-apps-distrib/kura-examples/heater/target` on the device, then restart Kura:

```shell
apt install ./kura-heater_<version>_all.deb
systemctl restart kura
```

The bundle is configured with `configurationPolicy = REQUIRE` and appears in the Services area of the web UI as **Heater** as soon as it is activated with its default configuration.

## Configure the Cloud Connection

Open a browser and browse to the Kura web UI of the device at `https://<device address>` (or [https://localhost](https://localhost) for the Docker image). Enter the appropriate name and password (default is admin/admin) and click **Log in**.

In the **Cloud Connections** section, select the default cloud connection and open its **MqttDataTransport** tab. Fill in the following fields then click the **Apply** button:

| Field                       | Value |
|-----------------------------|-------|
| broker-url                  | The url for the MQTT broker, for instance `mqtt://broker.hivemq.com:1883/` for a public test broker |
| topic.context.account-name  | Your [account_name] |
| username                    | Typically [account_name]_broker |
| password                    | The password for your user |
| client-id                   | The client identifier to be used when connecting to the MQTT broker (optional) |

Now that the account credentials are set in the MqttDataTransport service, the DataService needs to be configured to connect by default. To do so, open the **DataService** tab and set `connect.auto-on-startup` to **true**, then click **Connect** in the Cloud Connections toolbar. The connection status turns to *Connected*.

Finally, create a publisher for the heater: with the cloud connection selected, click **New Pub/Sub**, choose the `CloudPublisher` factory of the connection and give it a name, such as `heater-publisher`. Its configuration defines the application id and the semantic topic the messages are published to, the QoS and the retain flag.

## Modify Bundle Configuration in Local Web UI

From the Kura web UI, select the **Heater** bundle from the configurable services on the left. In the **CloudPublisher Target Filter** field pick the `heater-publisher` created above: the ConfigurationService updates the `CloudPublisher.target` property of the component and Declarative Services binds the publisher to the heater, which starts publishing. Then modify the other parameters as needed. By default, the heater demo is configured according to the following characteristics and assumptions about its operational environment:

* Start operation is at 6:00am (06:00).

* End operation is at 10:00pm (22:00).

* It is colder outside than inside the heated chamber (hard-coded to 5 degrees in the application).

* Output of the heater is constant at 30 degrees (hard-coded).

* When in operational mode, the temperature will drop inside if the heater is off.

* The heater turns off when it is about to exceed the setPoint defined in the configuration.

* After the temperature drops to four times the increment point (a made-up value to show dropping temperature, hard-coded in the application), the heater turns back on, and the temperature starts increment at the rate of the `temperature.increment` rate.

Click **Apply** for changes to take effect. The `updated()` method is called after settings are applied for the new configuration.

After completing this tutorial, it is highly recommended that you review the heater demo source code to see how it is put together. Kura automatically generates the user configuration interface through the implementation of the ConfigurableComponent interface and the Metatype annotations of `HeaterOCD`. This powerful feature provides both a local and remote configuration user interface with no additional development requirements.
