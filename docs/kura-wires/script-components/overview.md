# Script Components Overview

The Script Components enable advanced data processing operations on Wire Envelopes through execution of JavaScript code within the Wire Composer framework.

These components leverage the [GraalVM&trade; JavaScript Engine](https://www.graalvm.org/22.1/reference-manual/js/) and consist of the following:

- [GraalVM&trade; Filter Component](graalvm-filter-component.md): provides comprehensive scripting capabilities for general-purpose Wire Envelope manipulation
- [GraalVM&trade; Conditional Component](graalvm-conditional-component.md): a multiport component that implements conditional branching logic within the Wire Composer

!!! warning "Compatibility Notice"
    Scripts developed for the [*Script Filter*](https://eclipse-kura.github.io/kura/docs-release-5.6/kura-wires/script-components/nashorn-script-filter/) and [*Conditional Component*](https://eclipse-kura.github.io/kura/docs-release-5.6/kura-wires/script-components/nashorn-conditional-component/) Wire Components in Kura 5.x are not compatible with these GraalVM-based components. Refer to [GraalVM&trade; Filter Component](graalvm-filter-component.md) and [GraalVM&trade; Conditional Component](graalvm-conditional-component.md) documentation for the updated syntax specifications.
