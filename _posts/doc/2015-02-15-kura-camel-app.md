---
layout: page
title:  "Apache Camel™ as application"
categories: [doc]
---

# Camel as Kura application

As of 2.1.0 Kura provides a set of different ways to implement an application backed by Camel:

 * Simple XML route configuration
 * Custom XML route configuration
 * Custom Java DSL definition 

## Simple XML routes

Eclipse Kura 2.1.0 introduces a new "out-of-the-box" component which
allows to configure a set of XML based routes. The component is called "Camel XML router"
and can be configured with a simple set of XML routes.

The following example logs all messages received on the topic `foo/bar` to a logger named
`MESSAGE_FROM_CLOUD`:

    <routes xmlns="http://camel.apache.org/schema/spring">
      <route id="cloudConsumer">
        <from uri="kura-cloud:foo/bar"/>
        <to uri="log:MESSAGE_FROM_CLOUD"/>
      </route>
    </routes>
    
But it is also possible to generate data and push to upstream to the cloud service:

    <route id="route1">
      <from uri="timer:foo"/>
      <setBody>
        <method ref="payloadFactory" method="create('random',${random(10)})"/>
      </setBody>
      <bean ref="payloadFactory" method="append('foo','bar')"/>
      <to uri="stream:out"/>
      <to uri="kura-cloud:myapp/test"/>
    </route>
    
This example to run a timer named "foo" every second. It uses the "Payload Factory" bean,
which is pre-registered, to create a new payload structure and then append a second elemen
to it.

The output is first sent to a logger and then to the cloud source.

### Defining dependencies on components

It is possible to use the Web UI to define a list of Camel components which must be present in
order for this configuration to work. For example if the routes make use of the "milo-server"
adapter for providing OPC UA support then "milo-server" can be added and the setup will wait for
this component to be registered with OSGi before the Camel context gets started.

The field contains a list of comma separated component names: e.g. `milo-server, timer, log`

### Selecting a cloud service

It is also possible to define a map of cloud services which will be available for upstream connectivity.
This makes use of Kura's "multi cloud client" feature. CloudService instances will get mapped from either
a Kura Service PID (`kura.service.pid`, as shown in the Web UI) or a full OSGi filter. The string is
a comma seperated, `key=value` string, where the key is the name of the Camel cloud the instance will be
registered as and the value is the Kura service PID or the OSGi filter string.

For example: `cloud=org.eclipse.kura.cloud.CloudService, cloud-2=foobar`

## Custom Camel routers

If a standard XML route configuration is not enough then it is possible to use XML routes
in combination with a custom OSGi bundle or a Java DSL based Camel approach.
For this to work a Kura development setup is required, please see [Getting started](kura-setup.html)
for more information.

The implementation of such Camel components follow the standard Kura guides for developing components,
like, for example, the `ConfigurableComponent` pattern. This section only describes the Camel
specifics.

Of course it is also possible to follow a very simple approach and directly use the Camel OSGi
functionalities like `org.apache.camel.core.osgi.OsgiDefaultCamelContext`.

{% include note.html message="Kura currently doesn't support the OSGi Blueprint approach" %}

Kura support for Camel is split up in two layers. There is a more basic support, which helps
in running a raw Camel router. This is `CamelRunner` which is located in the
package `org.eclipse.kura.camel.router`. And then there are a few abstract basic components
in the package `org.eclipse.kura.camel.component` which help in creating Kura components based
on Camel.

## Camel components

The base classes in `org.eclipse.kura.camel.component` are intended to help creating new OSGi DS
components base on Camel.

### XML based component

For an XML based approach which can be configured through the Kura `ConfigurationService` the base
class `AbstractXmlCamelComponent` can be used. The constructor expectes the name of a property
which will contain the Camel XML router information when it gets configured through the configuration
service. It will automatically parse and apply the Camel routes.

The method `void beforeStart(CamelContext camelContext)` may be used in order to configure
the Camel context before it gets started.

Every time the routes get updated using the `modified(Map<String, Object>)` method, the route XML
will be re-parsed and routes will be added, removed or updated according to the new XML.

### Java DSL based component

In order to create a Java DSL based router setup the base class `AbstractJavaCamelComponent` may
be used, which implements and `RouteBuilder` class, a simple setup might look like:

    import org.eclipse.kura.camel.component.AbstractJavaCamelComponent;
    
    class MyRouter extends AbstractJavaCamelComponent {
       public void configure() throws Exception {
          from("direct:test")
             .to("mock:test");
       }
    }
    
### Using the CamelRunner

The `CamelRunner` class is not derived from any OSGi or Kura base class and can be used
in scenarios where more flexibility is required. It allows to define a set of pre-requisites
for the Camel context. It is for example possible to define a dependency on a Kura cloud service
instance and a Camel component provider. Once the runner is started it will listen for OSGi
services resolving those dependencies and then starting up the Camel context.

The following example shows how to set up a Camel context using the `CamelRunner`:

    // create a new camel Builder
    
    Builder builder = new CamelRunner.Builder();
    
    // add service dependency
    
    builder.cloudService("kura.service.pid", "my.cloud.service.pid");
    
    // add Camel component dependency to 'milo-server'
    
    builder.requireComponent("milo-server");
    
    CamelRunner runner = builder.build();
    
    // set routes
    
    runner.setRoutes ( new RouteBuilder() {
      public void configure() throws Exception {
        from("direct:test")
          .to("mock:test");
      }
    } );
    
    // set routes
    
    runner.start ();
    
It is also possible to later update routes with a call to `setRoutes`:
    
    // maybe update routes at a later time
    
    runner.setRoutes ( /* different routes */ );
    
# Examples

The following examples can help in getting started.

## Kura Camel example publisher

The Camel example publisher (`org.eclipse.kura.example.camel.publisher`) can be used as an reference for starting.
The final OSGi bundle can be dropped into a Kura application an be started. It allows to configure dynamically
during runtime and is capable of switching CloudService instances dynamically.

## Kura Camel quickstart

The Camel quickstart project (`org.eclipse.kura.example.camel.quickstart`) shows two components, Java and XML based,
working together. The bundle can also be dropped into Kura for testing.

## Kura Camel aggregation

The Camel quickstart project (`org.eclipse.kura.example.camel.aggregation`) shows a simple data aggregation pattern
with Camel by processing data and publishing the result.
