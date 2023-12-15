Eclipse Kura™
=============

<p align="center">
<img src="https://www.eclipse.org/kura/content/images/kura_logo_400.png" alt="Kura™ logo" width="500"/>
</p>

<div align="center">

[![GitHub Tag](https://img.shields.io/github/v/tag/eclipse/kura?label=Latest%20Tag)](https://github.com/eclipse/kura/tags)
[![GitHub](https://img.shields.io/github/license/eclipse/kura?label=License)](https://github.com/eclipse/kura/blob/develop/LICENSE)

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

- [**User Documentation**](https://eclipse.github.io/kura/latest/): here you'll find information on how to **use** Eclipse Kura™ i.e. installation instructions, informations on how to use the web UI and tutorials.
- [**Developer Documentation**](https://github.com/eclipse/kura/wiki): the Eclipse Kura™ Github Wiki serves as a reference for **developers** who want to contribute to the Eclipse Kura™ project and/or develop new add-ons. Here you'll find Eclipse Kura™ development/release model, guidelines on how to import internal packages, creating new bundles and development environment tips & tricks.
- [**Docker Containers Documentation**](https://hub.docker.com/r/eclipse/kura/): the Eclipse Kura™ team also provides Docker containers for the project. Information on how to build and run them are available at the project's Docker Hub page.
- [**Developer Quickstart Guide**](https://github.com/eclipse/kura#build): a quick guide on how to setup the development environment and build the project is also provided in this README.

Additionally, we provide two channels for reporting any issue you find with the project
- [**Github Issues**](https://github.com/eclipse/kura/issues): for bug reporting.
- [**Github Discussions**](https://github.com/eclipse/kura/discussions): for receiving feedback, asking questions, making new proposals and generally talking about the project.

Install
-------

Eclipse Kura™ is compatible with Java 8 and Java 17.

### Target Gateways Installers
Eclipse Kura™ provides pre-built installers for common development boards. Check the following [link](https://www.eclipse.org/kura/downloads.php) to download the desired installers.
Take a look at [our documentation](https://eclipse.github.io/kura/latest/getting-started/install-kura/) for further information on supported platforms and installer types.

### Docker Image
Eclipse Kura™ is also available as a [Docker container](https://hub.docker.com/r/eclipse/kura/).

Build
-----

### Prerequisites

In order to be able to build Eclipse Kura™ on your development machine, you need to have the following programs installed in your system:
* JDK 1.8
* Maven 3.5.x

<details>
<summary>

#### Installing Prerequisites in Mac OS 

</summary>

To install Java 8, download the JDK tar archive from the [Adoptium Project Repository](https://adoptium.net/releases.html?variant=openjdk8&jvmVariant=hotspot).

Once downloaded, copy the tar archive in `/Library/Java/JavaVirtualMachines/` and cd into it. Unpack the archive with the following command:

```bash
sudo tar -xzf <archive-name>.tar.gz
```

The tar archive can be deleted afterwards.

Depending on which terminal you are using, edit the profiles (.zshrc, .profile, .bash_profile) to contain:

```bash
# Adoptium JDK 8
export JAVA_8_HOME=/Library/Java/JavaVirtualMachines/<archive-name>/Contents/Home
alias java8='export JAVA_HOME=$JAVA_8_HOME'
java8 
```

Reload the terminal and run `java -version` to make sure it is installed correctly.

Using [Brew](https://brew.sh/) you can easily install Maven from the command line:

```bash
brew install maven@3.5
```
Run `mvn -version` to ensure that Maven has been added to the PATH. If Maven cannot be found, try running `brew link maven@3.5 --force` or manually add it to your path with:

```bash
export PATH="/usr/local/opt/maven@3.5/bin:$PATH"
```

</details>

<details>
<summary>

#### Installing Prerequisites in Linux

</summary>

For Java
```bash
sudo apt install openjdk-8-jdk
```
For Maven   

You can follow the tutorial from the official [Maven](http://maven.apache.org/install.html) site. Remember that you need to install the 3.5.x version.

</details>

### Build Eclipse Kura™

Change to the new directory and clone the Eclipse Kura™ repo:

```bash
git clone -b develop https://github.com/eclipse/kura.git
```

Move inside the newly created directory and build the target platform:

```bash
mvn -f target-platform/pom.xml clean install
```

Build the core components:

```bash
mvn -f kura/pom.xml clean install
```

Build the examples (optional):

```bash
mvn -f kura/examples/pom.xml clean install
```

Build the target profiles:

```bash
mvn -f kura/distrib/pom.xml clean install -DbuildAll
```

> [!TIP]
You can skip tests by adding `-Dmaven.test.skip=true` in the commands above and you can compile a specific target by specifying the profile (e.g. `-Praspberry-pi-armhf`).

#### Build scripts

Alternatively, you can use the build scripts available in the root directory.

```bash
./build-all.sh
```

or

```bash
./build-menu.sh
```

and select the profiles you want to build.

### Building Eclipse Kura™ Containers

The Eclipse Kura™ container build process currently only supports x86 containers. Following the instructions below will build two containers. One based on Alpine Linux `kura-alpine-x86_64`, and another on Ubi8 `kura-ubi8-x86_64`.

Build Eclipse Kura™ as per [our instructions](#build-kura). To build the containers you'll need to change the target of the "Build the target profiles" step like the following:

```bash
mvn -f kura/distrib/pom.xml clean install -DbuildAllContainers
```

> [!NOTE]
This build step requires 'docker' to be a executable command on your system. For Instance, if you are using Podman please follow the [Emulating Docker Cli Guide](https://podman-desktop.io/docs/migrating-from-docker/emulating-docker-cli-with-podman) before running the command above.

After this command completes, images can be found in your preferred container engine image list.

IDE Setups
----------

We currently support two setups for Eclipse Kura™ development:

- [**Eclipse Kura™ Development Environment Setup**](https://eclipse.github.io/kura/latest/java-application-development/development-environment-setup/): This is the full setup allowing you to contribute to the core Eclipse Kura™ project codebase. It will install all the IDE plugins and formatters to have a pleasant development experience and clone the Eclipse Kura™ source code on your workstation.
- [**Add-on Development Environment Setup**](https://eclipse.github.io/kura/latest/java-application-development/kura-workspace-setup/): This setup will allow you to develop applications or bundles running on Eclipse Kura™. It will install only the APIs and the examples and is best suited for developing Eclipse Kura™ add-ons.

Contributing
------------

Contributing to Eclipse Kura™ is fun and easy! To start contributing you can follow our guide [here](CONTRIBUTING.md).
