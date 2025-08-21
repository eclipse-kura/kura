# Target platform dependencies

Eclipse Kura is distributed with a well-defined set of [Java APIs](https://download.eclipse.org/kura/docs/api/) and components that provide implementations of such APIs. Being based on OSGi, it extensively leverages its modularity by interconnecting each Kura service (or bundle) through the explicit description of required imports and provided exports in the MANIFEST file, following the [semantic versioning](https://github.com/eclipse-kura/kura/wiki/Kura-Semantic-Versioning) standard.

An Eclipse Kura installation is composed of:

- Kura APIs
- Core components that provide implementations of key APIs
- Core components that provide consumers of key APIs
- Optionally, additional components that implement advanced functionalities
- External dependencies

The focus of this section is on the **set of distributed external dependencies**.

Dependencies are usually required by multiple bundles. Instead of embedding the dependency artifacts in each bundle that is requiring it, causing lots of duplications, the JAR of the dependency is usually made available to the whole framework by either embedding it directly in the target platform (if it is already a OSGi bundle), or by [wrapping it as a OSGi bundle](https://felix.apache.org/documentation/subprojects/apache-felix-maven-bundle-plugin-bnd.html).

The set of external target platform bundles is built in the [Kura Target Platform project](https://github.com/eclipse-kura/kura/tree/develop/target-platform) and embedded in the final installation through the [Kura distrib project](https://github.com/eclipse-kura/kura/tree/develop/kura/distrib).

The Kura target platform artifacts that are not core Kura components must not be assumed to be stable across Kura versions. Read the following box.

!!! tip "Target platform stability"
    The artifacts that are distributed in Eclipse Kura through its target platform **are not API**. This means that, although a custom component could leverage these dependencies (like `guava`), it is not guaranteed that the next Kura version will maintain compatibility with the installed dependencies. For instance, a dependency `example` in version `1.2.3` can be available in Kura 5.6.0, but be missing or with a major version bump (like `2.0.0`, making the old APIs break) in the next Kura release.
