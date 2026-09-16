# Target platform dependencies

Eclipse Kura is distributed with a well-defined set of [Java APIs](https://download.eclipse.org/kura/docs/api/) and components that provide implementations of such APIs. Being based on OSGi, it extensively leverages its modularity by interconnecting each Kura service (or bundle) through the explicit description of required imports and provided exports in the bundle manifest, following the [semantic versioning](https://github.com/eclipse-kura/kura/wiki/Kura-Semantic-Versioning) standard. Since Kura 6 those headers are generated at build time by [bnd](https://bnd.bndtools.org/) from the code and from the versions the bundle was compiled against.

An Eclipse Kura installation is composed of:

- Kura APIs
- Core components that provide implementations of key APIs
- Core components that provide consumers of key APIs
- Optionally, additional components that implement advanced functionalities
- External dependencies

The focus of this section is on the **set of distributed external dependencies**, historically called the *target platform* of the framework.

Dependencies are usually required by multiple bundles. Instead of embedding the dependency artifacts in each bundle that is requiring it, causing lots of duplications, the JAR of the dependency is made available to the whole framework: it is installed in the Kura runtime as is when it is already an OSGi bundle, or wrapped into an OSGi bundle when it is not. The wrapper bundles are built by the Kura repository itself (the `kura/org.eclipse.kura.*` wrapper modules, such as the serial, USB and BLE native library wrappers), with the same bnd tooling as the framework bundles.

The set of external bundles of the runtime is declared, with its versions, by the Kura bill of materials `org.eclipse.kura:kura-bom` ([bom/pom.xml](https://github.com/eclipse-kura/kura/tree/develop/bom) in the Kura repository), which lists the Kura bundles and the third-party libraries of the runtime (Equinox, Felix SCR, Jetty, Jersey, Gson, Paho, BouncyCastle, ...) and is embedded in the final installation by the [Kura distrib project](https://github.com/eclipse-kura/kura/tree/develop/distrib). Every add-on generated with the [Kura Addon Archetype](./kura-addon-archetype.md) imports the bill of materials in its root `pom.xml`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.eclipse.kura</groupId>
            <artifactId>kura-bom</artifactId>
            <version>${kuraVersion}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

An add-on declares a runtime dependency in the `pom.xml` of its bundle without a version and bnd generates the corresponding `Import-Package` entry, with a range derived from the version found in the bill of materials; the JAR is not packaged into the add-on. A library that is **not** part of the runtime must instead be declared with its version and embedded in the bundle, for instance with the `-includeresource` instruction of bnd and a `Bundle-ClassPath` entry, or installed on the device as a separate bundle.

The Kura target platform artifacts that are not core Kura components must not be assumed to be stable across Kura versions. Read the following box before consuming them in your project.

!!! tip "Target platform stability"
    The third-party artifacts that are distributed with Eclipse Kura **are not API**. This means that, although a custom component could leverage these dependencies (like `gson`), it is not guaranteed that the next Kura version will maintain compatibility with the installed dependencies. For instance, a dependency `example` in version `1.2.3` can be available in Kura 6.0.0, but be missing or with a major version bump (like `2.0.0`, making the old APIs break) in the next Kura release. The bill of materials of each release is the reference for what is available: a library that is not listed there must be embedded in the add-on.
