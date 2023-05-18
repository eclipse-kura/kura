Eclipse Kura™
=============

<p align="center">
<img src="https://www.eclipse.org/kura/content/images/kura_logo_400.png" alt="Kura™ logo" width="500"/>
</p>

<div align="center">

![GitHub Tag](https://img.shields.io/github/v/tag/eclipse/kura?label=Latest%20Tag)
![GitHub](https://img.shields.io/github/license/eclipse/kura?label=License)

![GitHub Issues](https://img.shields.io/github/issues-raw/eclipse/kura?label=Open%20Issues)
![GitHub Pull Requests](https://img.shields.io/github/issues-pr/eclipse/kura?label=Pull%20Requests&color=blue)
![GitHub Contributors](https://img.shields.io/github/contributors/eclipse/kura?label=Contributors)
![GitHub Forks](https://img.shields.io/github/forks/eclipse/kura?label=Forks)

![Jenkins](https://img.shields.io/jenkins/build?jobUrl=https:%2F%2Fci.eclipse.org%2Fkura%2Fjob%2Fmultibranch%2Fjob%2Fdevelop&label=Jenkins%20Build&logo=jenkins)
![Jenkins](https://img.shields.io/jenkins/tests?compact_message&failed_label=%E2%9D%8C&jobUrl=https:%2F%2Fci.eclipse.org%2Fkura%2Fjob%2Fmultibranch%2Fjob%2Fdevelop%2F&label=Jenkins%20CI&passed_label=%E2%9C%85&skipped_label=%E2%9D%95&logo=jenkins) <br/>
  
[![Gitter](https://badges.gitter.im/eclipse/kura.svg)](https://gitter.im/eclipse/kura?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
  
</div>

An OSGi-based Application Framework for M2M Service Gateways

Background
----------
Until recently, machine-to-machine projects have been approached as embedded systems designed around custom hardware, custom software, and custom network connectivity. The challenge of developing such projects was given by the large customization and integration costs and the small re-usability across similar engagements. The results were often proprietary systems leveraging proprietary protocols.

The emergence of the service gateway model, which operates on the edge of an M2M deployment as an aggregator and controller, has opened up new possibilities. Cost effective service gateways are now capable of running modern software stacks opening the world of M2M to enterprise technologies and programming languages. Advanced software frameworks, which isolate the developer from the complexity of the hardware and the networking sub-systems, can now be offered to complement the service gateway hardware into an integrated hardware and software solution.


Description
-----------
Kura aims at offering a Java/OSGi-based container for M2M applications running in service gateways. Kura provides or, when available, aggregates open source implementations for the most common services needed by M2M applications. Kura components are designed as configurable OSGi Declarative Service exposing service API and raising events. While several Kura components are in pure Java, others are invoked through JNI and have a dependency on the Linux operating system.

For more information, see the [Eclipse project proposal](http://www.eclipse.org/proposals/technology.kura/).


System Requirements
-------------------

Eclipse Kura is compatible with Java 8 (`Bundle-RequiredExecutionEnvironment: JavaSE-1.8`) and OSGi R6.


Development Model
-----------------
Development on Kura follows a [variant of the gitflow model](https://github.com/eclipse/kura/wiki/New-Kura-Git-Workflow).  Development is made on the [develop branch](/eclipse/kura/tree/develop). The master branch is not used anymore.


Getting Started
-----------------

Development for Kura can be done in Eclipse IDE using the Kura Development Environment, in a gateway or in a Docker container.

## Development Environment

### Supported Development Platforms
The Eclipse Installer based setup works for the main used platforms like Linux, Mac OS and Windows.

### Prerequisites
Before installing Kura, you need to have the following programs installed in your system
* JDK 1.8
* Maven 3.5.x

#### Installing Prerequisites in Mac OS 

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

#### Installing Prerequisites in Linux
For Java
```bash
sudo apt install openjdk-8-jdk
```
For Maven   

You can follow the tutorial from the official [Maven](http://maven.apache.org/install.html) site. Remember that you need to install the 3.5.x version.

### Build Kura

Change to the new directory and clone the Kura repo:

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

> *Note*: You can skip tests by adding `-Dmaven.test.skip=true` in the commands above and you can compile a specific target by specifying the profile (e.g. `-Praspberry-pi-armhf`).

#### Build scripts

Alternatively you can use the build scripts available in the root directory.

```bash
./build-all.sh
```

or

```bash
./build-menu.sh
```

and select the profiles you want to build.

### Building Eclipse Kura Containers

The kura container build process currently only supports x86 containers. Following the instructions below will build two containers. One based on Alpine Linux ```kura-alpine-x86_64```, and another on Ubi8 ```kura-ubi8-x86_64```.

Build Kura as per [our instructions](https://github.com/eclipse/kura#build-kura). To build the containers you'll need to change the target of the "Build the target profiles" step like the following:

```bash
mvn -f kura/distrib/pom.xml clean install -DbuildAllContainers
```
> *Note*: this build step requires 'docker' to be a executable command on your system. For Instance, if you are using podman please follow the [Emulating Docker Cli Guide](https://podman-desktop.io/docs/migrating-from-docker/emulating-docker-cli-with-podman) before running the command above.

After this command runs, images can be found in your preferred container engine image list.

### Eclipse IDE
The simplest way to start developing on Eclipse Kura is to use an [Eclipse Installer](https://www.eclipse.org/downloads/) based setup. A detailed installation and setup guide is available on the [official documentation](http://eclipse.github.io/kura/dev/kura-setup.html). Here you'll find a brief explaination of the required steps.

To correctly setup the environment, proceed as follows:
- Install a jdk-8 distribution like [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=8) for your specific CPU architecture and OS.
- Start the Eclipse Installer
- Switch to advanced mode (top right hamburger menu > Advanced Mode)
- Select "Eclipse IDE for Eclipse Committers" and configure the "Product Version" to be the version **2022-06 or older**.
- Set the Java 1.8+ VM to the installed local jdk-8 VM, and press the Next button
- Select the Eclipse Kura installer from the list. If this is not available, add a new installer from https://raw.githubusercontent.com/eclipse/kura/develop/kura/setups/kura.setup, then check and press the Next button
- Select the "Developer Type":
  - "User": if you want to develop applications or bundles running on Kura, select this option. It will install only the APIs and the examples.
  - "Developer" : if you are a framework developer, select this option. It will download and configure the Eclipse Kura framework.
- Update Eclipse Kura Git repository username and customize further settings if you like (e.g. Root install folder, Installation folder name). To show these options, make sure that the "Show all variables" checkbox is enabled
- Leave all Bootstrap Tasks selected and press the Finish button
- Accept all the licenses and wait for the installation to finish
At first startup Eclipse IDE will checkout the code, perform a full build and configure a few Working Sets. 
- When the tasks are completed. In the IDE open (double click) Target Platform > Target-Definition > Kura Target Platform Equinox 3.16.0, and press "Set as Target Platform" located at the top right of the window

Now you are ready to develop on Eclipse Kura.

To raise an issue, please report a bug on [GitHub issues](https://github.com/eclipse/kura/issues/new).

### Known Issues
Currently, the emulator web ui is not properly working on Windows so, with your setup, you will be able to build and deploy you applications, but not be able to use the Eclipse IDE based Kura emulator.

The full build of Kura is only supported for Linux and Mac Os based systems.

Currently the maven build on Windows requires to disable the tests and will fail when it tries to create the installers for the target platforms.

## Contributing

Contributing to Eclipse Kura is fun and easy! To start contributing you can follow our guide [here](CONTRIBUTING.md).


## Target Gateways Installers
Eclipse Kura provides pre-built installers for common development boards.
Check the following [link](https://www.eclipse.org/kura/downloads.php) to download the desired installers.


## Docker Image
Eclipse Kura is available also as a [Docker container](https://hub.docker.com/r/eclipse/kura/)
To easily run, use: `docker run -d -p 443:443 -t eclipse/kura`.
