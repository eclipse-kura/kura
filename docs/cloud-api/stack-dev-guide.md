---
layout: page
title:  "Cloud connection developer guide"
categories: [cloud-api]
---

This guide will provide information on how a cloud connection developer can leverage the new Generic Cloud Services APIs.

- [Implement CloudEndpoint and CloudConnectionManager](#implement-cloudendpoint-and-cloudconnectionmanager)
- [Implement the CloudConnectionFactory interface](#implement-the-cloudconnectionfactory-interface)
- [Provide a CloudPublisher implementation](#provide-a-cloudpublisher-implementation)
- [Provide a CloudSubscriber implementation](#provide-a-cloudsubscriber-implementation)
- [Implement RequestHandler support](#implement-requesthandler-support)  

As reference, this guide will use the Eclipse IoT WG namespace implementation bundle available [here](https://github.com/eclipse/kura/tree/develop/kura/org.eclipse.kura.cloudconnection.eclipseiot.mqtt.provider)

## Implement CloudEndpoint and CloudConnectionManager
In order to leverage the new APIs, and be managed by the Kura Web UI, the Cloud Connection implementation bundle must implement CloudEndpont and, if log-lived connections are supported, the CloudConnectionManager interface must be implemented as well.

The ending class should be something as follows:

```java

public class CloudConnectionManagerImpl
        implements CloudConnectionManager, CloudEndpoint, ... {

    @Override
    public boolean isConnected() {
        ...
    }

    @Override
    public void connect() throws KuraConnectException {
        ...
    }

    @Override
    public void disconnect() {
        ...
    }

    @Override
    public Map<String, String> getInfo() {
        ...
    }

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        ...
    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        ...
    }
}
```

A corresponding component definition should be provided in the OSGI-INF folder exposing the implementation of CloudEndpoint and CloudConnectionManager interfaces.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.1.0" activate="activate" configuration-policy="require" deactivate="deactivate" enabled="true" immediate="true" modified="updated" name="org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager">
   <implementation class="org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.CloudConnectionManagerImpl"/>
   <service>
      <provide interface="org.eclipse.kura.configuration.ConfigurableComponent"/>
      <provide interface="org.eclipse.kura.cloudconnection.CloudConnectionManager"/>
      <provide interface="org.eclipse.kura.cloudconnection.CloudEndpoint"/>
      <!-- ... -->
   </service>

   <!-- ... -->

   <property name="kura.ui.service.hide" type="Boolean" value="true"/>
   <property name="kura.ui.factory.hide" type="String" value="true"/>
</scr:component>
```

In order to be fully compliant with the Web UI requirements, the CloudConnection component definition should provide two properties `kura.ui.service.hide` and `kura.ui.factory.hide` to hide the component from the left side part of the UI dedicated to display the services list.

## Implement the CloudConnectionFactory interface

The CloudConnectionFactory is responsible to manage the cloud connection instance lifecycle by creating the CloudEndpoint instance and all the required services needed to publish or receive messages from the cloud platform.

As a reference, please have a look at the [CloudConnectionFactory](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.cloudconnection.eclipseiot.mqtt.provider/src/main/java/org/eclipse/kura/internal/cloudconnection/eclipseiot/mqtt/cloud/factory/DefaultCloudConnectionFactory.java) defined for the Eclipse IoT WG namespace implementation.

In particular, the `getFactoryPid()` method returns the PID of the CloudEndpoint factory.
The `createConfiguration()` method receives a PID that will be used for the instantiation of the CloudEndpoint and for all the related services required to communicate with the cloud platform. In the example above, the factory creates the CloudEnpoint, and a DataService and MqttDataTransport instances internally needed to communicate with a remote cloud platform. As can be seen [here](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.cloudconnection.eclipseiot.mqtt.provider/src/main/java/org/eclipse/kura/internal/cloudconnection/eclipseiot/mqtt/cloud/factory/DefaultCloudConnectionFactory.java#L215), the CloudEndpoint instance configuration is enriched with the reference to the CloudConnectionFactory that generated it. This step is required by the Web UI in order to properly relate the instances with the corresponding factories.

The `deleteConfiguration()` method deletes from the framework the CloudEndpoint instance identified by the PID passed as argument and all the related services. In the Eclipse IOT WG example, it not only deletes the CloudEndpoint instance but also the corresponding DataService and MqttDataTransport instances.

The `getStackComponentsPids()` method return a List of String that represent the kura.service.pid of the configurable components that are part of a Cloud Connection instance. This method is used by the Web UI to get the list of configurable components that need to be displayed to the end user.

The `getManagedCloudConnectionPids()` method will return the list of kura.service.pid of all the CloudEndpoints managed by the factory.

The factory component definition should be defined as follows:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.1.0" name="org.eclipse.kura.cloudconnection.eclipseiot.mqtt.DefaultCloudConnectionFactory">
   <implementation class="org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.factory.DefaultCloudConnectionFactory"/>
   <reference bind="setConfigurationService" cardinality="1..1" interface="org.eclipse.kura.configuration.ConfigurationService" name="ConfigurationService" policy="static" unbind="unsetConfigurationService"/>
   <service>
      <provide interface="org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory"/>
   </service>
   <property name="osgi.command.scope" type="String" value="kura.cloud"/>
   <property name="osgi.command.function" type="String">
      createConfiguration
   </property>
   <property name="kura.ui.csf.pid.default" type="String" value="org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager"/>
   <property name="kura.ui.csf.pid.regex" type="String" value="^org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager(\-[a-zA-Z0-9]+)?$"/>
   <property name="service.pid" type="String" value="org.eclipse.kura.cloudconnection.eclipseiot.mqtt.DefaultCloudConnectionFactory"/>
</scr:component>
```

In particular, it should expose in the `service` section the fact that the factory implements `org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory`
```xml
   <service>
      <provide interface="org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory"/>
   </service>
```

Important properties that need to be specified to have a better Web UI experience are the following:
```xml
<property name="kura.ui.csf.pid.default" type="String" value="org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager"/>
<property name="kura.ui.csf.pid.regex" type="String" value="^org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager(\-[a-zA-Z0-9]+)?$"/>
```
those allow to specify the form of the expected PID that the end user should provide when creating a new cloud connection.

## Provide a CloudPublisher implementation
To provide a CloudPublisher implementation, other than implementing CloudPublisher API in a java class, the developer must provide a component definition in the OSGI-INF folder that should be like the following:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.1.0" activate="activate" configuration-policy="require" deactivate="deactivate" enabled="true" immediate="true" modified="updated" name="org.eclipse.kura.cloudconnection.eclipseiot.mqtt.CloudPublisher">
   <implementation class="org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud.publisher.CloudPublisherImpl"/>
   <service>
      <provide interface="org.eclipse.kura.cloudconnection.publisher.CloudPublisher"/>
      <provide interface="org.eclipse.kura.configuration.ConfigurableComponent"/>
   </service>
   <property name="cloud.connection.factory.pid" type="String" value="org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager"/>
   <property name="service.pid" type="String" value="org.eclipse.kura.cloudconnection.eclipseiot.mqtt.CloudPublisher"/>
   <property name="kura.ui.service.hide" type="Boolean" value="true"/>
   <property name="kura.ui.factory.hide" type="String" value="true"/>
   <property name="kura.ui.csf.pid.default" type="String" value="org.eclipse.kura.cloudconnection.eclipseiot.mqtt.CloudPublisher"/>
   <property name="kura.ui.csf.pid.regex" type="String" value="^org.eclipse.kura.cloudconnection.eclipseiot.mqtt.CloudPublisher(\-[a-zA-Z0-9]+)?$"/>
</scr:component>
```

As can be seen in the previous snippet, the Publisher exposes itself in the framework as a `ConfigurableComponent` and as a `CloudPublisher`.

The component definition must contain the following well-known properties:

- **cloud.connection.factory.pid**: this property must be set to the kura.service.pid of the factory that created the cloud connection which the publisher belongs. It is used by the Web UI to enforce that the correct cloud publisher implementation is used in a specific cloud endpoint.  
- **kura.ui.service.hide**: as specified before for the Cloud Endpoint
- **kura.ui.factory.hide**: as specified before for the Cloud Endpoint
- **kura.ui.csf.pid.default**: as specified before for the Cloud Factory. It is an optional property.
- **kura.ui.csf.pid.regex**: as specified before for the Cloud Factory. It is an optional property.

The relation between the CloudPublisher instance and the CloudEndpoint is defined by a [configuration property](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/cloudconnection/CloudConnectionConstants.java#L30) set by the Web UI at CloudPublisher creation.

## Provide a CloudSubscriber implementation
The CloudSubscriber implementation and component definition is similar to the one described for the CloudPublisher.

## Implement RequestHandler support
In order to support Command and Control, the cloud connection bundle should provide a service that registers itself as RequestHandlerRegistry. In this way all the RequestHandler instances could be able to discover the different Registry and subscribe for command and control messages received from the cloud platform.
As an example, for the Eclipse IoT WG bundle, the CloudEndpoint registers itself also as RequestHandlerRegistry.
