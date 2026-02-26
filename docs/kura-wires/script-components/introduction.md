# Introduction to the Script Components

The Script Components allow for performing more complex operations on the received Wire Envelopes using a JavaScript Engine.

Depending if the target device is running on Java 8 or Java 17, there are several components to choose from.

For devices running a JRE **with Nashorn JS Engine** (Java < 15), a *Script Filter* and a *Conditional Component* are provided:

- [Nashorn-based Script Filter](nashorn-script-filter.md)
- [Nashorn-based Conditional Component](nashorn-conditional-component.md)

The above components will run only on Java < 15 since the Nashorn dependency is not included in the DP. The two components are available in the Eclipse Marketplace as two separate entries. *These components are deprecated as of Kura version 5.3.*

The following components instead have the [GraalVM&trade; JavaScript Engine](https://www.graalvm.org/22.1/reference-manual/js/) included in the DP and therefore do not require a JRE with Nashorn JS Engine:

- [GraalVM&trade; Filter Component](graalvm-filter-component.md)
- [GraalVM&trade; Conditional Component](graalvm-conditional-component.md)

The input scripts for these components are not compatible with the Nashorn implementations *Script Filter* and *Conditional Component*. Both components are shipped as a single DP named *org.eclipse.kura.wire.script.tools*. Since the JS engine dependency is shipped along with the DP, these components will work on both Java 8 and Java 17 devices but the DP is bigger in size (~18,6 MB).

!!! warning
    Installing the deprecated components on a Java 17-based instance of Kura will cause the following error in the logs:

    ```
        !SESSION 2026-02-12 11:07:47.324 -----------------------------------------------
        eclipse.buildId=unknown
        java.version=17.0.12
        java.vendor=Azul Systems, Inc.
        BootLoader constants: OS=linux, ARCH=aarch64, WS=gtk, NL=en_US
        Command-line arguments:  -console 5002 -consoleLog
        !ENTRY org.eclipse.kura.wire.component.conditional.provider 4 0 2026-02-12 11:08:16.340
        !MESSAGE FrameworkEvent ERROR
        !STACK 0
        org.osgi.framework.BundleException: Could not resolve module: org.eclipse.kura.wire.component.conditional.provider [117]
        Unresolved requirement: Require-Capability: osgi.ee; filter:="(& (&(osgi.ee=JavaSE)(version=1.8)) (!(&(osgi.ee=JavaSE)(version>=15))) )"
                at org.eclipse.osgi.container.Module.start(Module.java:463)
                at org.eclipse.osgi.container.ModuleContainer$ContainerStartLevel$2.run(ModuleContainer.java:1845)
                at org.eclipse.osgi.internal.framework.EquinoxContainerAdaptor$1$1.execute(EquinoxContainerAdaptor.java:136)
                at org.eclipse.osgi.container.ModuleContainer$ContainerStartLevel.incStartLevel(ModuleContainer.java:1838)
                at org.eclipse.osgi.container.ModuleContainer$ContainerStartLevel.incStartLevel(ModuleContainer.java:1781)
                at org.eclipse.osgi.container.ModuleContainer$ContainerStartLevel.doContainerStartLevel(ModuleContainer.java:1743)
                at org.eclipse.osgi.container.ModuleContainer$ContainerStartLevel.dispatchEvent(ModuleContainer.java:1665)
                at org.eclipse.osgi.container.ModuleContainer$ContainerStartLevel.dispatchEvent(ModuleContainer.java:1)
                at org.eclipse.osgi.framework.eventmgr.EventManager.dispatchEvent(EventManager.java:234)
                at org.eclipse.osgi.framework.eventmgr.EventManager$EventThread.run(EventManager.java:345)

    ```

    This is expected, and doesn't compromise the stability of the Kura instance, but it is recommended to avoid installing the deprecated components on Java 17-based instances of Kura.