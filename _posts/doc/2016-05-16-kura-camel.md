---
layout: page
title:  "Apache Camel integration"
date:   2016-05-16 11:31:11
categories: [doc]
---

# Camel Kura router

Kura provides `org.eclipse.kura.camel.router.CamelRouter` class, which extends 
`org.apache.camel.component.kura.KuraRouter` class from the Apache Camel 
[camel-kura](http://camel.apache.org/kura) module. While `KuraRouter` provides a generic base for Kura routes, it 
doesn't rely on the Kura-specific jars, because of limitations of the Apache Camel policy regarding adding 3rd parties repositories to the Camel (like Eclipse Kura repository). `CamelRouter` extends `KuraRouter` and enhances it with Kura-specific API.

In the near future we plan to migrate Kura related code from Apache Camel to Eclipse Kura, so all the code related to
Kura-Camel integration will be hosted here under Kura umbrella.

## Maven dependency

In order to start using `RhiotKuraRouter`, Maven users should add the following dependency to their POM file:

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

`CamelRouter` comes with a `RhiotKuraRouter#updated(Map<String, Object>)` method. The primary purpose of this callback 
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

We highly recommend to use our [Kura Camel quickstart](https://rhiot.gitbooks.io/rhiotdocumentation/content/quickstarts/kura_camel_quickstart.html) as a template for creating Kura Camel routers. Our quickstart is configured as SCR component, so you can just deploy it to the Kura server and see your gateway route module deployed as a configurable service. To specify the route XML that should be loaded by a Camel context running in a deployed module, edit the `camel.route.xml` service property and click `Apply` button. As soon as `Apply` button is clicked, the route will be parsed and loaded.


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
