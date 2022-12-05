# Introduction to the Script Components

The Script Components allow for performing more complex operations on the received Wire Envelopes using a JavaScript Engine.

Depending if the target device is running on Java 8 or Java 17, there are different implementations available for these components.

For **Java 8 devices**, a *Script Filter* and a *Conditional Component* are provided and based on Nashorn JS Engine (which comes by default with Java 8 JRE):

- [Nashorn-based Script Filter](nashorn-script-filter.md)
- [Nashorn-based Conditional Component](nashorn-conditional-component.md)

The above components will run only on Java 8 since the Nashorn dependency is not included in the DP. The two components are available in the Eclipse Marketplace as two separate entries.

For devices that run **Java 17**, the following components are available instead:

- [GraalVM&trade; Filter Component](graalvm-filter-component.md)
- [GraalVM&trade; Conditional Component](graalvm-conditional-component.md)

These components are based on the [GraalVM&trade; JavaScript Engine](https://www.graalvm.org/22.1/reference-manual/js/) and the input scripts are not compatible with the Nashorn implementation. Both components are shipped as a single DP named *org.eclipse.kura.wire.script.tools*. Since the JS engine dependency is shipped along with the DP, these components will work on both Java 8 and Java 17 devices but the DP is bigger in size (~18,6 MB).