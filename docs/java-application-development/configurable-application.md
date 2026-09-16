# Configurable Application

## Overview

This section provides a simple example of how to create an OSGi bundle that implements the ConfigurableComponent interface in Kura. This bundle will interact with the Kura ConfigurationService via the ConfigurableComponent interface, which allows for a local configuration mechanism using the Kura web user-interface (UI) and a remote one through the cloud platform. In this example, you will learn how to perform the following functions:

* Generate an add-on project with the Kura Addon Archetype

* Implement the ConfigurableComponent interface

* Describe the configuration with the OSGi Metatype annotations

* Use the Kura web UI to modify the bundle's configuration

### Prerequisites

* [Hello World Application](./hello-world-application.md): the project generation and the build are the same

* Implements the use of the Kura web user-interface (UI)

## Configurable Component Example

### Generate the Project

Generate the project with the [Kura Addon Archetype](./kura-addon-archetype.md) using `org.eclipse.kura.example` as groupId, `kura-configurable` as artifactId and `org.eclipse.kura.example.configurable` as package. The generated bundle already depends on `org.eclipse.kura.api`, `slf4j-api` and the Declarative Services and Metatype annotations, which is all this example needs.

The archetype generates a configurable component (`ExampleComponent`, its `ExampleComponentOCD` configuration description and the `ExampleComponentOptions` wrapper): this tutorial builds the same thing from scratch, so delete the generated classes of `src/main/java/org/eclipse/kura/example/configurable` and the test classes that refer to them.

### Create the Configuration Description

A configurable component publishes the description of its parameters, types and defaults through the OSGi Metatype service: the Kura web UI renders that description as a form, and the ConfigurationService validates the values against it. The description is written as an annotated Java interface, which bnd turns into the `OSGI-INF/metatype/<pid>.xml` file at build time. Create `ConfigurableExampleOCD.java`:

```java
package org.eclipse.kura.example.configurable;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.example.configurable.ConfigurableExample", name = "ConfigurableExample",
        description = "This is a sample metatype for a simple configurable component")
public @interface ConfigurableExampleOCD {

    @AttributeDefinition(name = "param1.string", type = AttributeType.STRING, required = true,
            description = "String configuration parameter")
    String param1_string() default "Some Text";

    @AttributeDefinition(name = "param2.float", type = AttributeType.FLOAT, required = false, min = "5.0", max = "40.0",
            description = "Float configuration parameter")
    float param2_float() default 20.5f;

    @AttributeDefinition(name = "param3.integer", type = AttributeType.INTEGER, required = true, min = "1",
            description = "Integer configuration parameter")
    int param3_integer() default 2;
}
```

The `id` of the `@ObjectClassDefinition` is the persistent identifier (PID) of the configuration: it must match the name of the component that receives it. The method names become the property ids, with `_` mapped to `.`: `param1_string` is the property `param1.string`. Each `@AttributeDefinition` carries the type, whether the value is required, the optional range and the description shown in the web UI; the default value is the default of the method.

### Create the Component Class

Create `ConfigurableExample.java` with the following code:

```java
package org.eclipse.kura.example.configurable;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = ConfigurableExampleOCD.class)
@Component(name = "org.eclipse.kura.example.configurable.ConfigurableExample", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, service = ConfigurableComponent.class)
public class ConfigurableExample implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurableExample.class);
    private static final String APP_ID = "org.eclipse.kura.example.configurable.ConfigurableExample";

    private Map<String, Object> properties;

    @Activate
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("Bundle {} has started with config!", APP_ID);
        updated(properties);
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        logger.info("Bundle {} has stopped!", APP_ID);
    }

    @Modified
    public void updated(Map<String, Object> properties) {
        this.properties = properties;
        if (properties != null && !properties.isEmpty()) {
            for (Entry<String, Object> entry : properties.entrySet()) {
                logger.info("New property - {} = {} of type {}", entry.getKey(), entry.getValue(),
                        entry.getValue().getClass());
            }
        }
    }
}
```

The annotations replace the component definition and metatype files that were written by hand in older Kura versions:

* `@Component` declares the Declarative Services component. Its `name` is the PID that the ConfigurationService uses to store the configuration in the snapshots and to show the component in the web UI: it must be the same as the `id` of the `@ObjectClassDefinition`. `configurationPolicy = ConfigurationPolicy.REQUIRE` makes the component wait for its configuration before activating, so the `@Activate` method always receives the properties; the ConfigurationService creates the initial configuration from the metatype defaults. `immediate = true` activates it as soon as the configuration is available.

* `service = ConfigurableComponent.class` registers the component as a `ConfigurableComponent` service. The ConfigurationService tracks only the components that **provide** this service (or `SelfConfiguringComponent`): implementing the interface alone is not enough.

* `@Designate(ocd = ...)` links the component to its configuration description.

* `@Modified` marks the method called when the configuration changes at runtime. The `@Activate` method receives the initial configuration, the `@Deactivate` method is called when the component is stopped.

!!! warning "Legacy Configurable Components"
    Components that implement `ConfigurableComponent` without providing it as a service are no longer tracked by the ConfigurationService, unless the property `org.eclipse.kura.core.configuration.legacyServiceTracking` is set to `true` in `/opt/eclipse/kura/framework/kura.properties`.

### Build and Install the Bundle

Build the project as in the [Hello World Application](./hello-world-application.md): after the first `mvn clean install -Presolve-integration-tests`, a plain `mvn clean install` produces the bundle and the Debian installer. The bundle jar now contains the generated `OSGI-INF/org.eclipse.kura.example.configurable.ConfigurableExample.xml` descriptor and the `OSGI-INF/metatype/org.eclipse.kura.example.configurable.ConfigurableExample.xml` metatype: open them in `target/classes` to check what the annotations produced.

Install the package on the device and restart Kura (`systemctl restart kura`). The framework log shows the activation with the default configuration:

```text
INFO  o.e.k.e.c.ConfigurableExample - Bundle org.eclipse.kura.example.configurable.ConfigurableExample has started with config!
INFO  o.e.k.e.c.ConfigurableExample - New property - param1.string = Some Text of type class java.lang.String
INFO  o.e.k.e.c.ConfigurableExample - New property - param2.float = 20.5 of type class java.lang.Float
INFO  o.e.k.e.c.ConfigurableExample - New property - param3.integer = 2 of type class java.lang.Integer
```

## View the Bundle Configuration in the Local Web UI

With the bundle running, open a browser window and browse to the Kura web UI of the device at `https://<device address>` (or [https://localhost](https://localhost) for the Docker image). Once connected to the Kura web UI, a log in window appears prompting you to enter the Name and Password.

Enter the appropriate name and password (default is admin/admin) and click **Log in**. The Kura Admin web UI appears with the **ConfigurableExample** in the Services area on the left side of the browser window.

From the Kura Admin web UI, you can change the parameters that are used by the Kura configuration manager and in turn call the `updated()` method of the newly created bundle. To do so, click **ConfigurableExample** and the configurable component parameters will be displayed as described by the metatype: a text field for `param1.string`, numeric fields with the declared ranges for the other two.

Make any necessary changes and click the **Apply** button near the top left of the configuration pane for the modifications to take effect. Every time a change is made to the configuration, a new snapshot is generated along with an ID, and the new values are logged by the `updated()` method.
