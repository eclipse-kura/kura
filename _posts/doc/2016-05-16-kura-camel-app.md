---
layout: page
title:  "Apache Camel™ as application"
date:   2014-05-16 11:31:11
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

## Custom XML routes

If a standard XML route configuration is not enough then it is possible to use XML routes
in combination with a custom OSGi bundle. For this to work a Kura development setup
is required, please see [Getting started](kura-setup.html) for more information. 

## Maven dependency

In order to start using `CamelRouter`, Maven users should add the following dependency to their POM file:

    <dependency>
      <groupId>org.eclipse.kura</groupId>
      <artifactId>org.eclipse.kura.camel</artifactId>
      <version>${kura.version}</version>
    </dependency>

Adding `org.eclipse.kura.camel` module to your project, imports transitive Kura dependencies. This is big advantage over Apache
Camel camel-kura module, which doesn't rely on Kura API and therefore doesn't import Kura jars.

## Usage

The principle of using `CamelRouter` is the same as using `KuraRouter` i.e. just extend `CamelRouter` class:

    import org.eclipse.kura.camel.router.CamelRouter;
    
    class TestKuraRouter extends CamelRouter {

      @Override
	  public void configure() throws Exception {
	    from("direct:test")
	     .to("mock:test");
	  }

	}

## Loading XML routes using SCR property

`CamelRouter` comes with a `CamelRouter#updated(Map<String, Object>)` method. The primary purpose of this callback
is to allow a router to be a SCR component configured using the Kura Web UI and EuroTech 
[Everyware Cloud](http://www.eurotech.com/en/products/software+services/everyware+cloud+m2m+platform/m2m+what+it+is),
however you can use this callback outside the web UI and Everyware Cloud context.

Whenever `CamelRouter#updated(Map<String, Object>)` callback is executed, `CamelRouter` tries to read `camel.route.xml`
property value (`RouterConstants.XML_ROUTE_PROPERTY` key constant), to parse its value and load it as an XML Camel routes. 
For example if the `camel.route.xml` property will be set to the following value...

    <routes xmlns="http://camel.apache.org/schema/spring">
        <route id="mqttLogger">
            <from uri="paho:topic?brokerUrl=tcp:brokerhost:1883"/>
            <to uri="log:messages"/>
        </route>
    </routes>
    
...new route will be automatically started (or updated if route with ID equal to `mqttLogger` already exists).

### Managing XML Camel routes using web UI

All `CamelRouter` instances implements Kura's `ConfigurableComponent` interface. It means that those can be
configured using Kura web UI.

We highly recommend to use our Kura Camel quickstart (see below) as a template for creating Kura Camel routers. Our quickstart is configured as SCR component, so you can just deploy it to the Kura server and see your gateway route module deployed as a configurable service. To specify the route XML that should be loaded by a Camel context running in a deployed module, edit the `camel.route.xml` service property and click `Apply` button. As soon as `Apply` button is clicked, the route will be parsed and loaded.


![]({{ site.baseurl }}/assets/images/camel/media/kura_camel_routes_webui.png)

A xml route sample :

```
<routes xmlns="http://camel.apache.org/schema/spring">
 <route id="bar">
   <from uri="timer:helloTriger" />
   <to uri="log:HelloKura"/>
  </route>
</routes>
```

Our Kura Camel quickstart can be also used from the 
[EuroTech Everyware Cloud (EC)](http://www.eurotech.com/en/products/software+services/everyware+cloud+m2m+platform/m2m+what+it+is).

![]({{ site.baseurl }}/assets/images/camel/media/kura_camel_routes_ec.png)

# Kura Camel quickstart

The Kura Camel quickstart can be used to create Camel router OSGi bundle project deployable into the
[Eclipse Kura](https://www.eclipse.org/kura) gateway.

## Creating a Kura Camel project

In order to create the Kura Camel project execute the following commands:

    git clone git@github.com:eclipse/kura.git
    cp -r kura/kura/examples/org.eclipse.kura.example.camel.quickstart org.eclipse.kura.example.camel.quickstart
    cd org.eclipse.kura.example.camel.quickstart
    mvn install

## Prerequisites

### Find your device

We presume that you have [Eclipse Kura](https://wiki.eclipse.org/Kura/Raspberry_Pi) already installed on your target device. And that you know the IP address of that device.

Then export address of your Raspberry Pi device to RPBI IP environment variable:

    export RBPI_IP=192.168.1.100

### Configure Kura

**Note: ** Starting with Kura 2.0.1 is no longer required to set a special OSGi boot delegation for the Camel integration.

Keep in mind that `/opt/eclipse/kura/kura/config.ini` file on your target device should have OSGi boot delegation
enabled for packages `sun`. A boot delegation of `sun` packages is required to make Camel work smoothly in
[Eclipse Equinox](http://www.eclipse.org/equinox/). In order to enable boot delegation, just add the following line to
the `/opt/eclipse/kura/kura/config.ini`:

    org.osgi.framework.bootdelegation=sun.*,com.sun.*

## Deployment

In order to deploy Camel application to a Kura server, you have to copy necessary Camel jars and a bundle containing your application. Your bundle can be deployed into the target device by executing an `scp` command. For example:

    scp target/org.eclipse.kura.example.camel.quickstart-1.0.0-SNAPSHOT.jar pi@${RBPI_IP}:

The command above will copy your bundle to the `/home/pi/org.eclipse.kura.example.camel.quickstart-1.0.0-SNAPSHOT.jar` location on a target device.

Now log into your target device using SSH. Then, from a remote SSH shell, log into Kura shell using telnet:

    ssh pi@${RBPI_IP}
    ...
    telnet localhost 5002

And install the bundles you previously scp-ed into the telnet session :

    install file:///home/pi/org.eclipse.kura.example.camel.quickstart-1.0.0-SNAPSHOT.jar

Finally start your application using the following command:

    start < ID_OF_org.eclipse.kura.example.camel.quickstart-1.0.0-SNAPSHOT_BUNDLE >

You can retrieve ID of your bundle using the following command:

    ss | grep org.eclipse.kura.example.camel.quickstart

Keep in mind that bundles you deployed using the recipe above are not installed permanently and will be reverted after the server restart. Please read Kura documentation for more details regarding
[permanent deployments](http://eclipse.github.io/kura/doc/deploying-bundles.html#making-deployment-permanent).

## What the quickstart is actually doing?

This quickstart triggers [Camel timer](http://camel.apache.org/timer.html) generate random temperature value every second and sends it to the system
logger using [Camel Log](http://camel.apache.org/log) component. This is fairy simple functionality, but enough to
demonstrate the Camel Kura project is actually working and processing messages.

In order to see messages logged by Camel router to Kura log file execute the following command on your remote device:

    tail -f /var/log/kura.log

# Kura Camel aggregation example

The Kura Camel aggregation example can be used to create Camel router OSGi bundle project deployable into the
[Eclipse Kura](https://www.eclipse.org/kura) gateway. The project demonstrates how to levarage Camel aggregation feature
to periodically calculate a value from the sensor data received within the given time frame.

## Creating a Kura Camel aggregation example project

In order to create the Kura Camel project execute the following commands:

    git clone git@github.com:eclipse/kura.git
    cp -r kura/kura/examples/org.eclipse.kura.example.camel.aggregation org.eclipse.kura.example.camel.aggregation
    cd org.eclipse.kura.example.camel.aggregation
    mvn install

## Deployment

In order to deploy the example, follow instructions from the general Camel Kura quickstart.
