# Introduction to the Script Components

The Script Components enable advanced data processing operations on Wire Envelopes through execution of JavaScript code within the Wire Composer framework.

These components leverage the [GraalVM&trade; JavaScript Engine](https://www.graalvm.org/22.1/reference-manual/js/) and consist of the following:

- [GraalVM&trade; Filter Component](graalvm-filter-component.md): provides comprehensive scripting capabilities for general-purpose Wire Envelope manipulation
- [GraalVM&trade; Conditional Component](graalvm-conditional-component.md): a multiport component that implements conditional branching logic within the Wire Composer

The components can be installed through the *Wires Script Tools* deployment package available on the Eclipse Marketplace at [this link](https://marketplace.eclipse.org/content/wires-script-tools-kura-5).

!!! warning "Compatibility Notice"
    Scripts developed for the [*Script Filter*](https://eclipse-kura.github.io/kura/docs-release-5.6/kura-wires/script-components/nashorn-script-filter/) and [*Conditional Component*](https://eclipse-kura.github.io/kura/docs-release-5.6/kura-wires/script-components/nashorn-conditional-component/) Wire Components in Kura 5.x are not compatible with these GraalVM-based components. Refer to [GraalVM&trade; Filter Component](graalvm-filter-component.md) and [GraalVM&trade; Conditional Component](graalvm-conditional-component.md) documentation for the updated syntax specifications.

## Legacy installations support

For legacy installations running on a JRE **with Nashorn JS Engine** (Java < 15, [JEP 372](https://openjdk.org/jeps/372)), a *Script Filter* and a *Conditional Component* are still provided:

- [Nashorn-based Script Filter (Deprecated)](nashorn-script-filter.md)
- [Nashorn-based Conditional Component (Deprecated)](nashorn-conditional-component.md)

The above components will run only on Java < 15 since the Nashorn dependency is not included in the DP. The two components are available in the Eclipse Marketplace as two separate entries. **These components are deprecated as of Kura version 5.3.**