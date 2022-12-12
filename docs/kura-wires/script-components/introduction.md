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