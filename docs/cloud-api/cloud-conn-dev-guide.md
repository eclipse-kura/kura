# Cloud connection developer guide

This guide will provide information on how a cloud connection developer can leverage the new Generic Cloud Services APIs.

As reference, this guide will use the Eclipse IoT WG namespace implementation bundle available [here](https://github.com/eclipse-kura/kura/tree/develop/kura/org.eclipse.kura.cloudconnection.eclipseiot.mqtt.provider)

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

The class is declared as a Declarative Services component with the `@Component` annotation, exposing the implementation of `CloudEndpoint` and `CloudConnectionManager` (and of `ConfigurableComponent`, since the endpoint is configurable) in the `service` attribute. bnd generates the `OSGI-INF` component descriptor from the annotations at build time.

```java
@Component(
    name = "org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    service = { ConfigurableComponent.class,
            CloudConnectionManager.class,
            CloudEndpoint.class,
            /* ... */ },
    property = {
        "kura.ui.service.hide:Boolean=true",
        "kura.ui.factory.hide:Boolean=true" })
@Designate(ocd = ConnectionManagerOptions.class, factory = true)
public class CloudConnectionManagerImpl
        implements CloudConnectionManager, CloudEndpoint, ConfigurableComponent, ... {

    @Activate
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) { ... }

    @Modified
    public void updated(Map<String, Object> properties) { ... }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) { ... }
}
```

The `@Designate(..., factory = true)` annotation links the component to its Metatype description (`ConnectionManagerOptions`, an `@ObjectClassDefinition` annotated interface) and declares it as a factory component, so that the user can create several cloud connection instances.

In order to be fully compliant with the Web UI requirements, the CloudConnection component declaration should provide two properties `kura.ui.service.hide` and `kura.ui.factory.hide` to hide the component from the left side part of the UI dedicated to display the services list.

## Implement the CloudConnectionFactory interface

The CloudConnectionFactory is responsible to manage the cloud connection instance lifecycle by creating the CloudEndpoint instance and all the required services needed to publish or receive messages from the cloud platform.

As a reference, please have a look at the [CloudConnectionFactory](https://github.com/eclipse-kura/kura/blob/develop/kura/org.eclipse.kura.cloudconnection.eclipseiot.mqtt.provider/src/main/java/org/eclipse/kura/internal/cloudconnection/eclipseiot/mqtt/cloud/factory/DefaultCloudConnectionFactory.java) defined for the Eclipse IoT WG namespace implementation.

In particular, the `getFactoryPid()` method returns the PID of the CloudEndpoint factory.
The `createConfiguration()` method receives a PID that will be used for the instantiation of the CloudEndpoint and for all the related services required to communicate with the cloud platform. In the example above, the factory creates the CloudEnpoint, and a DataService and MqttDataTransport instances internally needed to communicate with a remote cloud platform. As can be seen [here](https://github.com/eclipse-kura/kura/blob/develop/kura/org.eclipse.kura.cloudconnection.eclipseiot.mqtt.provider/src/main/java/org/eclipse/kura/internal/cloudconnection/eclipseiot/mqtt/cloud/factory/DefaultCloudConnectionFactory.java#L215), the CloudEndpoint instance configuration is enriched with the reference to the CloudConnectionFactory that generated it. This step is required by the Web UI in order to properly relate the instances with the corresponding factories.

The `deleteConfiguration()` method deletes from the framework the CloudEndpoint instance identified by the PID passed as argument and all the related services. In the Eclipse IOT WG example, it not only deletes the CloudEndpoint instance but also the corresponding DataService and MqttDataTransport instances.

The `getStackComponentsPids()` method return a List of String that represent the kura.service.pid of the configurable components that are part of a Cloud Connection instance. This method is used by the Web UI to get the list of configurable components that need to be displayed to the end user.

The `getManagedCloudConnectionPids()` method will return the list of kura.service.pid of all the CloudEndpoints managed by the factory.

The factory component should be declared as follows:

```java
@Component(
    name = "org.eclipse.kura.cloudconnection.eclipseiot.mqtt.DefaultCloudConnectionFactory",
    service = { CloudConnectionFactory.class },
    property = {
        "osgi.command.scope=kura.cloud",
        "osgi.command.function=createConfiguration",
        "kura.ui.csf.pid.default=org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager",
        "kura.ui.csf.pid.regex=^org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager(\\-[a-zA-Z0-9]+)?$" })
public class DefaultCloudConnectionFactory implements CloudConnectionFactory {

    @Reference
    public void setConfigurationService(ConfigurationService configurationService) { ... }

    public void unsetConfigurationService(ConfigurationService configurationService) { ... }

    /* ... */
}
```

In particular, it should expose in the `service` attribute the fact that the factory implements `org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory`:
```java
service = { CloudConnectionFactory.class }
```

Important properties that need to be specified to have a better Web UI experience are the following:
```java
"kura.ui.csf.pid.default=org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager",
"kura.ui.csf.pid.regex=^org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager(\\-[a-zA-Z0-9]+)?$"
```
those allow to specify the form of the expected PID that the end user should provide when creating a new cloud connection.

## Provide a CloudPublisher implementation
To provide a CloudPublisher implementation, other than implementing the CloudPublisher API in a Java class, the developer must declare the component as follows:

```java
@Component(
    name = "org.eclipse.kura.cloudconnection.eclipseiot.mqtt.CloudPublisher",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    service = { CloudPublisher.class, ConfigurableComponent.class },
    property = {
        "cloud.connection.factory.pid=org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager",
        "kura.ui.service.hide:Boolean=true",
        "kura.ui.factory.hide=true",
        "kura.ui.csf.pid.default=org.eclipse.kura.cloudconnection.eclipseiot.mqtt.CloudPublisher",
        "kura.ui.csf.pid.regex=^org.eclipse.kura.cloudconnection.eclipseiot.mqtt.CloudPublisher(\\-[a-zA-Z0-9]+)?$" })
@Designate(ocd = CloudPublisherMetatype.class, factory = true)
public class CloudPublisherImpl implements CloudPublisher, ConfigurableComponent, ... {
    /* ... */
}
```

As can be seen in the previous snippet, the Publisher exposes itself in the framework as a `ConfigurableComponent` and as a `CloudPublisher`.

The component must declare the following well-known properties:

- `cloud.connection.factory.pid`: this property must be set to the kura.service.pid of the factory that created the cloud connection which the publisher belongs. It is used by the Web UI to enforce that the correct cloud publisher implementation is used in a specific cloud endpoint.  
- `kura.ui.service.hide`: as specified before for the Cloud Endpoint
- `kura.ui.factory.hide`: as specified before for the Cloud Endpoint
- `kura.ui.csf.pid.default`: as specified before for the Cloud Factory. It is an optional property.
- `kura.ui.csf.pid.regex`: as specified before for the Cloud Factory. It is an optional property.

The relation between the CloudPublisher instance and the CloudEndpoint is defined by a [configuration property](https://github.com/eclipse-kura/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/cloudconnection/CloudConnectionConstants.java#L30) set by the Web UI at CloudPublisher creation.

## Provide a CloudSubscriber implementation
The CloudSubscriber implementation and component declaration is similar to the one described for the CloudPublisher.

## Implement RequestHandler support
In order to support Command and Control, the cloud connection bundle should provide a service that registers itself as RequestHandlerRegistry. In this way all the RequestHandler instances could be able to discover the different Registry and subscribe for command and control messages received from the cloud platform.
As an example, for the Eclipse IoT WG bundle, the CloudEndpoint registers itself also as RequestHandlerRegistry.
