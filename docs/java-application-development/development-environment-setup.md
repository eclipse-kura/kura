# Development Environment Setup

This page covers the steps required to build Eclipse Kura from source and to work on the framework itself. If you want to develop applications or bundles that run on Eclipse Kura, refer to the [Eclipse Kura Addon Archetype guide](./kura-addon-archetype.md) instead.

Eclipse Kura is a plain [Maven](https://maven.apache.org/) project built with the [bnd](https://bndtools.org/) tooling (`bnd-maven-plugin`): every bundle's OSGi manifest is generated at build time from its `bnd.bnd`, and Declarative Services and Metatype descriptors are generated from annotations. No Tycho, PDE or Eclipse-specific tooling is involved, so the project builds and imports like any other Maven project on Linux, macOS and Windows.

## Requirements

- JDK 21
- Maven 3.9.x
- Git

On Linux (Debian/Ubuntu):

```bash
sudo apt install openjdk-21-jdk maven git
```

On macOS, install a JDK 21 from [Adoptium](https://adoptium.net/temurin/releases?version=21) and Maven with [Homebrew](https://brew.sh/) (`brew install maven`). Check the installation with `java -version` and `mvn -version`.

## Build

Clone the repository and build everything from its root:

```bash
git clone -b develop https://github.com/eclipse-kura/kura.git
cd kura
mvn clean install
```

The repository is a single Maven reactor:

- `bom/` — the Bill of Materials with the managed versions of every dependency;
- `kura/` — the framework bundles, including the third-party and native wrapper bundles;
- `distrib/` — the distribution packaging that assembles the framework into the installable `.deb` packages;
- `test/` — the OSGi integration tests, added to the reactor by the `tests` profile.

Maven orders the modules by their dependencies, so a single command builds the bundles and then the packages, which end up under `distrib/*/target/`. Add `-DskipTests` to skip the tests: this also drops the `test/` modules from the reactor.

To rebuild a single bundle after a change, run Maven in its directory:

```bash
mvn -f kura/org.eclipse.kura.core.configuration/pom.xml clean install
```

## Tests

Unit tests live in each bundle under `src/test/java` and run with surefire as part of the build. The OSGi integration tests live under `test/`: each module is a fragment of the bundle it tests and is executed inside a real OSGi framework by `bnd-testing-maven-plugin`, driven by its `integration-test.bndrun`. To run one integration-test module:

```bash
mvn -f test/org.eclipse.kura.core.configuration.test/pom.xml clean verify
```

The set of runtime bundles (`-runbundles`) of every `integration-test.bndrun` is resolved and committed, so a fresh checkout needs no extra step. After changing the runtime dependencies of an integration test, re-resolve and commit the updated files:

```bash
mvn -f test/pom.xml clean verify -Presolve-integration-tests
```

## IDE setup

No IDE-specific configuration is required: import the repository as a Maven project in the IDE of your choice. Run `mvn clean install -DskipTests` once before importing, so that the reactor artifacts are available in the local Maven repository.

- **Eclipse IDE**: _File | Import | Maven | Existing Maven Projects_ on the repository root (the [m2e](https://eclipse.dev/m2e/) tooling is included in the Java packages). The optional [Bndtools](https://bndtools.org/) plug-in adds editing support for the `bnd.bnd` and `.bndrun` files.
- **IntelliJ IDEA**: open the repository root and select the root `pom.xml` when asked; see the [IntelliJ Maven documentation](https://www.jetbrains.com/help/idea/maven-support.html).
- **Visual Studio Code**: open the repository root with the [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) installed; see the [VS Code Java documentation](https://code.visualstudio.com/docs/java/java-project).

The Eclipse Kura code style is defined by the formatter and clean-up profiles in `kura/setups/formatting/` (`KuraFormatter.xml`, `KuraCleanupProfile.xml`): import them in your IDE so that contributions keep the project formatting.

## Running your build

Eclipse Kura runs on a gateway or in a container, not inside the IDE: install one of the `.deb` packages produced under `distrib/*/target/` on a supported device or virtual machine, or build a [Docker image](https://github.com/eclipse-kura/kura-docker) from them. See [Deploy and Debug Applications](./deploy-and-debug-applications.md) for how to deploy bundles to a running instance and attach a remote debugger.

## Kura examples

To get inspiration and become familiar with development on Eclipse Kura, some example bundles are available in the [kura-apps repository](https://github.com/eclipse-kura/kura-apps).
