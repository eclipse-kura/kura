# Deploy and Debug Applications

## Overview

This section describes how to test and manage OSGi bundles in a Kura environment. These instructions use the "Hello World" project created in the [previous section](./hello-world-application.md). In this example, you will learn how to perform the following functions:

* Run the bundle inside the integration-test runtime of the project

* Deploy a bundle to a target device running Kura

* Manage OSGi bundles on a target device from the OSGi console

* Set bundle Logger levels in Kura

### Prerequisites

* [Kura Addon Archetype](./kura-addon-archetype.md)

* [Hello World Application](./hello-world-application.md)

## Testing the Bundle Locally

A project generated with the archetype contains a `tests` module whose integration tests run inside a real OSGi framework, started by the `bnd-testing-maven-plugin` with the Kura emulator bundles resolved from the Kura bill of materials. This replaces the Eclipse "emulator" launch of the previous Kura versions: the framework is started on the development machine (Linux, Mac or Windows) by the build, with the bundle under test installed and started.

The runtime is described by `tests/<package>.test/integration-test.bndrun`: the `-runrequires` section lists the bundles it must contain (the Kura emulator set plus the bundle under test), `-runproperties` sets `org.eclipse.kura.mode=emulator` and points the framework at the fake Kura home of `tests/test-env` (framework properties, log4j configuration, an initial snapshot). Any change to `-runrequires` must be followed by a build with the `resolve-integration-tests` profile, which rewrites the `-runbundles` list.

The generated `ExampleComponentItTest` shows the pattern: the test class is itself a Declarative Services component that receives the component under test through a `@Reference` with a `kura.service.pid` target filter, waits for it and asserts on it. Every public, concrete class named `*Test` under `src/main/java` of the test module is executed this way, so add one for `HelloOsgi` and run:

```shell
mvn -f tests/pom.xml clean verify
```

The framework log (the `log4j.xml` of `tests/test-env` writes to the console) is printed in the Maven output, with the activation message of the bundle among the Kura startup messages. To debug the test with an IDE, add the JDWP agent to the `-runvm` section of the bndrun (`-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:8000`) and attach a *Remote Java Application* debugger to port 8000.

## Deploying to a Target Device

The supported way to deploy an add-on is the Debian package produced by the `distrib` module: install it on the device and restart Kura, as described in [Install and run the generated packages](./kura-addon-archetype.md#install-and-run-the-generated-packages). The package copies the bundle into `/opt/eclipse/kura/plugins`, and the startup script regenerates the framework configuration from that folder at every boot, so the installation is permanent.

While developing, the bundle jar can also be installed without rebuilding the package, from the OSGi console of the device described below:

```text
install file:///tmp/org.eclipse.kura.example.hello_osgi-1.0.0-SNAPSHOT.jar
start <bundle id>
```

A bundle installed this way lives in the framework storage and is not reinstalled after Kura is upgraded or its storage is cleaned: use the package for anything beyond a quick test. The Kura web UI **Packages** section and the [Deploy REST APIs](../references/rest-apis/rest-deploy-api.md) can install OSGi Deployment Packages (`.dp`) as well.

## Connect to OSGi on Target Device

You can manage the OSGi framework on a target device by logging into a console on the device using a connected keyboard and monitor or over a network connection using SSH.

At the command prompt, display the Kura log file with:

```shell
tail -f /var/log/kura.log
```

The OSGi console is opened only when Kura runs in debug mode. Stop the service and start Kura in the foreground with the debug option of the startup script:

```shell
sudo systemctl stop kura
sudo /opt/eclipse/kura/bin/start_kura.sh --debug
```

Besides the console on the terminal, debug mode listens for console connections on TCP port 5002 and for JDWP debugger connections on port 8000 (see [Remote debugging on target platform](./remote-debugging-on-target-platform.md)). From another shell on the device, connect to the console with:

```shell
telnet localhost 5002
```

There are many commands available in the OSGi console for managing bundles. Following are just a few useful commands:

| Command        | Description |
|----------------|-------------|
| ss             | Lists names and ID of bundles |
| help           | Displays the help menu of OSGi commands |
| lb             | Lists all installed bundles and IDs |
| headers [id]   | Displays bundle headers (i.e., Bundle Manifest Version, Name, Symbolic Name, Version, Import Package, Service Component) |
| scr:list       | Lists the Declarative Services components and their state |
| exit           | Exits the OSGi console and stops Kura |
| disconnect     | Exits the OSGi console, but leaves Kura running |

### Manage Bundles on Target Device

From the OSGi command line, you can display a list of bundles with the `ss` command:

```text
osgi> ss org.eclipse.kura.example
"Framework is launched."

id      State       Bundle
64      ACTIVE      org.eclipse.kura.example.hello_osgi_1.0.0.202609151200
```

In this example, the `org.eclipse.kura.example.hello_osgi` bundle ID is 64.

You can run the `start ##` or `stop ##` commands to start or stop a bundle, where the `##` is either the bundle ID number or the bundle name (such as `start org.eclipse.kura.example.hello_osgi`). To verify that the bundle is stopped, you can issue the `ss` command. If the bundle is stopped, the state will show RESOLVED. If the bundle is started, the state will show ACTIVE. Note that the INFO messages for both the `activate()` and `deactivate()` methods appear in the log when the bundle is started or stopped.

A bundle can be removed with `uninstall ##`; once uninstalled, it can only be installed again from its jar file with the `install` command shown above.

### Set Kura Logger Levels

Kura logger levels are defined in the log4j configuration file `/opt/eclipse/kura/log4j/log4j.xml`. The messages that appear require a log statement in the application and that the log level of the statement matches the log level of the application (such as `logger.info` or `logger.debug`).

The file is reloaded automatically when it changes (its `monitorInterval` is 30 seconds), so no restart is needed. Add a `Logger` element for the package of the application inside the `Loggers` section, for instance to show the debug message of the Hello World example:

```xml
<Logger name="org.eclipse.kura.example" level="debug" />
```

The logger levels are hierarchical: a logger applies to all the classes whose fully qualified name starts with its name, and the most specific logger wins over the more general ones (such as the `org.eclipse.kura` logger, set to INFO by default). The complete procedure and the available levels are described in [Increase the log level](../administration/logging-level.md).

In conclusion, this section described how to run a bundle in the integration-test runtime of the project, how to install it on a target device with its package or from the OSGi console, and how to manage bundles and log levels on the device.
