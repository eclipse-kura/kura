Eclipse Kura™
=============

<p align="center">
<img src="https://eclipse.dev/kura/images/kura.png" alt="Kura™ logo" width="500"/>
</p>

<div align="center">

[![GitHub](https://img.shields.io/github/license/eclipse/kura?label=License)](https://github.com/eclipse-kura/kura/blob/develop/LICENSE)
[![Jenkins](https://img.shields.io/jenkins/build?jobUrl=https:%2F%2Fci.eclipse.org%2Fkura%2Fjob%2Fmultibranch%2Fjob%2Fdevelop&label=Jenkins%20Build&logo=jenkins)](https://ci.eclipse.org/kura/job/multibranch/job/develop/)
[![Jenkins](https://img.shields.io/jenkins/tests?compact_message&failed_label=%E2%9D%8C&jobUrl=https:%2F%2Fci.eclipse.org%2Fkura%2Fjob%2Fmultibranch%2Fjob%2Fdevelop%2F&label=Jenkins%20CI&passed_label=%E2%9C%85&skipped_label=%E2%9D%95&logo=jenkins)](https://ci.eclipse.org/kura/job/multibranch/) <br/>
  
</div>

## What is Eclipse Kura™?
From [the maori word for tank/container](https://maoridictionary.co.nz/search/?keywords=kura), Eclipse Kura™ is a versatile software framework designed to supercharge your edge devices. With an intuitive web interface, Eclipse Kura™ streamlines the process of configuring your gateway, connecting sensors, and IoT devices to seamlessly collect, process, and send data to the cloud. Eclipse Kura™ provides an extensible Java API for developing custom plugins within the framework. Additionally, it offers a REST API, enabling the use of Eclipse Kura™ as a backend service in your application.
 
Eclipse Kura™ runs on an edge gateway, which can be anything from a small SBC(single-board computer) like a Raspberry Pi, or a powerful high-performance computer.

### What can Eclipse Kura™ do for me?
* **Kura™ Services:** Provision and set up features to run on your gateway, such as an MQTT broker.
* **Kura™ Networking:** Manage Network connectivity, including 
* **Kura™ Wires:** Design data flows and data processing streams effortlessly with a drag-and-drop visual editor.
* **Kura™ Cloud Connectors:** Extendable cloud connector system. 
* **Kura™ Drivers:** Extendable service that handles reading data off of external devices.
* **Kura™ Snapshots:** Securely store and re-apply gateway settings for convenience.
* **Kura™ Security**: Easily and safely store your secrets.
* **Kura™ Container Orchestrator**: Manage Docker or Podman containers on your gateway for ultimate flexibility.
* **Kura™ AI Inference**: Run Nvidia Triton Models on the edge.
* **Kura™ Plugins**: Add and Extend the framework by adding your own Services, and Drivers.
* **Kura™ REST Service**: Embed the framework as a backend in your own edge applications.
 
### I have used Eclipse Kura™ to make a small-scale Edge deployment, how do I scale now?
If you want to scale, and manage many instances of Eclipse Kura™, check out [**Eclipse Kapua™**](https://github.com/eclipse/kapua). [Eclipse Kapua™](https://github.com/eclipse/kapua) is a Eclipse Kura™ compatible cloud command and control service that allows you to aggregate data and configure many Eclipse Kura™ devices. 

Documentation
-------------------

- [**User Documentation**](https://eclipse-kura.github.io/kura/latest/): here you'll find information on how to **use** Eclipse Kura™ i.e. installation instructions, informations on how to use the web UI and tutorials.
- [**Developer Documentation**](https://github.com/eclipse-kura/kura/wiki): the Eclipse Kura™ Github Wiki serves as a reference for **developers** who want to contribute to the Eclipse Kura™ project and/or develop new add-ons. Here you'll find Eclipse Kura™ development/release model, guidelines on how to import internal packages, creating new bundles and development environment tips & tricks.
- [**Docker Containers Documentation**](https://hub.docker.com/r/eclipsekura/kura/): the Eclipse Kura™ team also provides Docker containers for the project. Information on how to build and run them are available at https://github.com/eclipse-kura/kura-metapackage.
- [**Developer Quickstart Guide**](https://github.com/eclipse-kura/kura#build): a quick guide on how to setup the development environment and build the project is also provided in this README.

Additionally, we provide two channels for reporting any issue you find with the project
- [**Github Issues**](https://github.com/eclipse-kura/kura/issues): for bug reporting.
- [**Github Discussions**](https://github.com/eclipse-kura/kura/discussions): for receiving feedback, asking questions, making new proposals and generally talking about the project.

Install
-------

Eclipse Kura™ is compatible with Java 21.

### Quick Linux installation

The APT repository provides packages for both **x86‑64** and **arm64** architectures.
To install the latest stable version of **Eclipse Kura™**, run the following commands in a terminal:

```bash
# Install required tools
sudo apt update
sudo apt install -y curl gpg

# Add the Eclipse Kura APT repository key
curl -fsSL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0xBA7E3DF5EDC3FC36" \
  | gpg --dearmor \
  | sudo tee /etc/apt/keyrings/kura.gpg > /dev/null

# Add the Eclipse Kura APT repository
sudo tee /etc/apt/sources.list.d/kura.sources > /dev/null << 'EOF'
Types: deb
URIs: https://repo.eclipse.org/repository/kura-apt/
Suites: stable
Components: main
Signed-By: /etc/apt/keyrings/kura.gpg
EOF

# Update package index and install Eclipse Kura
sudo apt update
sudo apt install -y kura
```
> **Note:** You may need `sudo` privileges to run these commands.

### Target Gateways Installers
Eclipse Kura™ provides pre-built installers for common development boards. Check the following [link](https://www.eclipse.org/kura/downloads.php) to download the desired installers.
Take a look at [our documentation](https://eclipse-kura.github.io/kura/latest/getting-started/install-kura/) for further information on supported platforms and installer types.

### Docker Image
Eclipse Kura™ is also available as a [Docker container](https://hub.docker.com/r/eclipsekura/kura/).

Build
-----

### Prerequisites

In order to be able to build Eclipse Kura™ on your development machine, you need to have the following programs installed in your system:
* JDK 21
* Maven 3.9.9+

<details>
<summary>

#### Installing Prerequisites in Mac OS 

</summary>

To install Java 21, download the JDK tar archive from the [Adoptium Project Repository](https://adoptium.net/temurin/releases?version=21&os=any&arch=any).

Once downloaded, copy the tar archive in `/Library/Java/JavaVirtualMachines/` and `cd` into it. Unpack the archive with the following command:

```bash
sudo tar -xzf <archive-name>.tar.gz
```

The tar archive can be deleted afterwards.

Depending on which terminal you are using, edit the profiles (`.zshrc`, `.profile`, `.bash_profile`) to contain:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/<archive-name>/Contents/Home
```

Reload the terminal and run `java -version` to make sure it is installed correctly.

Using [Brew](https://brew.sh/) you can easily install Maven from the command line:

```bash
brew install maven@3.9
```

Run `mvn -version` to ensure that Maven has been added to the PATH. If Maven cannot be found, try running `brew link maven@3.9 --force` or manually add it to your path with:

```bash
export PATH="/usr/local/opt/maven@3.9/bin:$PATH"
```

</details>

<details>
<summary>

#### Installing Prerequisites in Linux

</summary>

For Java

```bash
sudo apt install openjdk-21-jdk
```

To install Maven you can follow the tutorial from the official [Maven](http://maven.apache.org/install.html) site. Remember that you need to install the 3.9.9 version.

</details>

### Build Eclipse Kura™

Clone the Eclipse Kura™ repository:

```bash
git clone -b develop https://github.com/eclipse-kura/kura.git
```

Eclipse Kura™ is built with [Maven](https://maven.apache.org/) and the [bnd](https://bndtools.org/) tooling (`bnd-maven-plugin`): each bundle's OSGi manifest is generated at build time from its `bnd.bnd` descriptor, and all dependency/plugin versions are centralized in the root parent POM (`pom.xml`). The project is a single Maven reactor rooted at the top-level `pom.xml`, which aggregates two modules:

* `target-platform/` — third-party and native wrapper bundles;
* `kura/` — the Eclipse Kura™ framework bundles.

Move inside the cloned directory and build everything from the repository root:

```bash
mvn clean install
```

This builds the `target-platform` wrapper bundles first and then all the framework bundles under `kura/`.

To build the device installers and target profiles (e.g. the `.deb` packages):

```bash
mvn -f kura/distrib/pom.xml clean install -DbuildAll
```

> [!TIP]
> You can skip tests by adding `-Dmaven.test.skip=true` to the commands above, and you can compile a specific target by specifying its profile (e.g. `-Paarch64`).

To list the available installer profiles, run:

```bash
mvn -f kura/distrib/pom.xml help:all-profiles
```

### Testing

Unit tests run automatically as part of `mvn install` (every bundle keeps its unit tests under `src/test/java`).

The OSGi **integration tests** live under `kura/test/` and are executed inside a real OSGi framework by the bnd testing tooling (`bnd-testing-maven-plugin`). They are grouped under the `tests` Maven profile; each test module is driven by its own `integration-test.bndrun`, while the run configuration shared by all of them (framework, execution environment, runtime properties) lives in `kura/test/integration-test.bnd`.

Build and run the integration tests with:

```bash
mvn clean install -Ptests
```

The set of runtime bundles (`-runbundles`) of each `integration-test.bndrun` is computed by the bnd resolver. The first time, or after changing the runtime dependencies, re-resolve them with the `resolve-integration-tests` profile:

```bash
mvn -f kura/test/pom.xml clean verify -Ptests -Presolve-integration-tests
```

#### Build scripts

Alternatively, you can use the build scripts available in the root directory.

```bash
./build-all.sh
```

IDE Setups
----------

We currently support two setups for Eclipse Kura™ development:

- [**Eclipse Kura™ Development Environment Setup**](https://eclipse-kura.github.io/kura/latest/java-application-development/development-environment-setup/): This is the full setup allowing you to contribute to the core Eclipse Kura™ project codebase. It will install all the IDE plugins and formatters to have a pleasant development experience and clone the Eclipse Kura™ source code on your workstation.
- [**Kura Addon Archetype**](https://eclipse-kura.github.io/kura/docs-develop/java-application-development/kura-addon-archetype/): The Kura Addon Archetype will allow you to develop applications or bundles running on Eclipse Kura™. It will install only the APIs and is best suited for developing Eclipse Kura™ add-ons.

Contributing
------------

Contributing to Eclipse Kura™ is fun and easy! To start contributing you can follow our guide [here](CONTRIBUTING.md).

### Acknowledgments

![YourKit Logo](https://www.yourkit.com/images/yklogo.png)

Thanks to YourKit for providing us an open source license of YourKit Java Profiler!

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/),
[YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/),
and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).
