# Hello World Application

## Overview

This section provides a simple example of how to create a Kura "Hello World" OSGi bundle. With this example, you will learn how to perform the following functions:

* Generate an add-on project with the Kura Addon Archetype

* Consume the Kura Logger service

* Write a Declarative Services component

* Build the bundle and its installer

* Install the bundle on a device running Kura

## Prerequisites

* [Kura Addon Archetype](./kura-addon-archetype.md): JDK 21, Maven 3.9.9+ and git

* A device running Kura 6, or the [Kura Docker image](../getting-started/docker-quick-start.md), reachable from the development machine

## Hello World Using the Kura Logger

### Generate the Project

Kura add-ons are built with Maven and [bnd](https://bnd.bndtools.org/): the OSGi manifest and the Declarative Services descriptors are generated at build time from the `pom.xml` and the Java annotations, so no `MANIFEST.MF` or `OSGI-INF` file is written by hand. Generate the project from the archetype as described in [Kura Addon Archetype](./kura-addon-archetype.md):

```shell
mvn archetype:generate \
-DarchetypeGroupId=org.eclipse.kura \
-DarchetypeArtifactId=kura-addon-archetype \
-DarchetypeVersion="<kura-version>"
```

Answer the prompts with:

* **groupId**: `org.eclipse.kura.example`
* **artifactId**: `kura-hello-world`
* **package**: `org.eclipse.kura.example.hello_osgi`

The generated project contains the bundle module `org.eclipse.kura.example.hello_osgi`, its bill of materials, a `tests` module and the `distrib` module that packages the bundle as a Debian installer. Initialize the git repository (`git init && git add . && git commit -m "initial commit"`): the build reads the commit hash to stamp the package version.

### Add Dependencies

The dependencies of the bundle are declared in `org.eclipse.kura.example.hello_osgi/pom.xml`. The archetype already declares what this example needs: `slf4j-api` for the logger, `org.eclipse.kura.api` and the OSGi Declarative Services annotations. The versions are not written in the pom: they come from the Kura bill of materials (`org.eclipse.kura:kura-bom`) imported by the root `pom.xml`, so they always match the target Kura release.

```xml
<dependencies>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
    </dependency>
    <dependency>
        <groupId>org.eclipse.kura</groupId>
        <artifactId>org.eclipse.kura.api</artifactId>
    </dependency>
    <dependency>
        <groupId>org.osgi</groupId>
        <artifactId>org.osgi.service.component.annotations</artifactId>
    </dependency>
</dependencies>
```

This is the "OSGi way" of the classpath: bnd analyses the compiled classes and writes the `Import-Package` header of the manifest from the packages they reference, with the version ranges of the bundles that export them. See [Target platform dependencies](./target-platform-dependencies.md) for the libraries that are part of the Kura runtime.

### Create the Component Class

The archetype generates an `ExampleComponent` with its configuration and a dependency service: delete the generated classes in `src/main/java/org/eclipse/kura/example/hello_osgi` and create `HelloOsgi.java` in their place with the following code.

```java
package org.eclipse.kura.example.hello_osgi;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "org.eclipse.kura.example.hello_osgi.HelloOsgi", immediate = true)
public class HelloOsgi {

    private static final Logger logger = LoggerFactory.getLogger(HelloOsgi.class);

    private static final String APP_ID = "org.eclipse.kura.example.hello_osgi";

    @Activate
    protected void activate(ComponentContext componentContext) {
        logger.info("Bundle {} has started!", APP_ID);
        logger.debug("{}: This is a debug message.", APP_ID);
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        logger.info("Bundle {} has stopped!", APP_ID);
    }
}
```

The `@Component` annotation declares the class as a Declarative Services component: bnd turns it into the `OSGI-INF/org.eclipse.kura.example.hello_osgi.HelloOsgi.xml` descriptor at build time. `immediate = true` makes the framework activate the component as soon as the bundle starts, without waiting for someone to request its service. The `@Activate` method is the entry point when the component is started, the `@Deactivate` method when it is stopped: no Activator class is needed.

Notice the use of `LoggerFactory.getLogger()`. Kura logging is provided by the framework through the Simple Logging Facade for Java (slf4j), so the bundle only depends on its API. Logger methods include `error`, `warn`, `info`, `debug` and `trace`, which represent increasingly lower (more detailed) levels of log information. Logger levels should generally be used to represent the following conditions:

* ERROR - A serious problem has occurred that requires attention from the system administrator.

* WARNING - An action occurred or a condition was discovered that should be reviewed and may require action before an error occurs. It may also be used for transient issues.

* INFO - A report of a normal action or event. This could be a user operation, such as "login completed", or an automatic operation, such as a log file rotation.

* DEBUG - A debug message used for troubleshooting or performance monitoring. It typically contains detailed event data including things an application developer would need to know.

* TRACE - A fairly detailed output of diagnostic logging, such as actual bytes of a particular message being examined.

For more information on slf4j, see the [Logger API](http://www.slf4j.org/apidocs/org/slf4j/Logger.html).

The generated `tests` module refers to the example component: remove the generated test classes too, or adapt them to `HelloOsgi`. Every public, concrete class named `*Test` in `src/main/java` of the test module is run as an integration test inside an embedded Kura framework, while the classes under `src/test/java` are plain JUnit unit tests.

### Build the Bundle

The first build resolves the bundles of the integration-test runtime and writes them into the `integration-test.bndrun` file of the test module:

```shell
mvn clean install -Presolve-integration-tests
```

Afterwards a plain `mvn clean install` is enough. The build produces:

* the bundle `org.eclipse.kura.example.hello_osgi/target/org.eclipse.kura.example.hello_osgi-1.0.0-SNAPSHOT.jar`, whose `META-INF/MANIFEST.MF` and `OSGI-INF` descriptor were generated by bnd. Open the manifest to check the `Import-Package` header computed from the code: `org.osgi.service.component` and `org.slf4j`.

* the installer `kura-hello-world_<version>_all.deb` in `distrib/target/deb`.

!!! info "Deployment Packages"
    Kura still installs and manages OSGi Deployment Packages (`.dp`) through the Deployment Admin service, for instance the ones downloaded from the Eclipse Marketplace or uploaded from the Kura web UI. The archetype does not produce one: the Debian installer is the supported way to ship an add-on to a device, and Kura itself is installed as a Debian package.

### Install on the Device

Copy the installer to the device and install it as described in [Install and run the generated packages](./kura-addon-archetype.md#install-and-run-the-generated-packages), then restart Kura:

```shell
apt install ./kura-hello-world_1.0.0~git<timestamp>.<hash>-1_all.deb
systemctl restart kura
```

During startup Kura scans the plugins folder, installs the jar and starts it. The component is immediate, so the activation message appears in the framework log:

```shell
tail -f /var/log/kura.log
```

```text
INFO  o.e.k.e.h.HelloOsgi - Bundle org.eclipse.kura.example.hello_osgi has started!
```

The debug message is not shown because the default log level is INFO: raise it for the `org.eclipse.kura.example` logger as described in [Logging level](../administration/logging-level.md) to see it.

In conclusion, you were able to generate a project, write a Declarative Services component that consumes the Kura logger, and build the bundle and its installer without writing any manifest or descriptor by hand. The next step is to test and manage the bundle on the device: see [Deploy and Debug Applications](./deploy-and-debug-applications.md).
