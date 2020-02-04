Eclipse Kura™
=============
Travis-ci:
[![Build Status](https://travis-ci.org/eclipse/kura.svg?branch=master)](https://travis-ci.org/eclipse/kura)
Hudson:
[![Hudson](https://img.shields.io/jenkins/build/https/ci.eclipse.org/kura/job/kura-develop.svg)](https://ci.eclipse.org/kura/)

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

Eclipse Kura is compatible with Java 8 (`Bundle-RequiredExecutionEnåvironment: JavaSE-1.8`) and OSGi R6.


Development Model
-----------------
Development on Kura follows a [variant of the gitflow model](https://github.com/eclipse/kura/wiki/New-Kura-Git-Workflow).  Development is made on the [develop branch](/eclipse/kura/tree/develop). The master branch is not used anymore.


Getting Started
-----------------

Development for Kura can be done in Eclipse IDE using the Kura Development Environment, in a gateway or in a Docker container.

## Development Environment

### Supported Development Platforms
The Eclipse Installer based setup works for the main used platforms like Linux, Mac Os and Windows.

### Prerequisites
Before installing Kura, you need to have the following programs installed in your OS
* JDK 1.8
* Maven 3.5.x

#### Installing Prerequisites in Mac OS 
Using [Brew](https://brew.sh/) you can easily install both Java and Maven from the command line.

Use the following commands in a terminal

For Java  
```
brew tap adoptopenjdk/openjdk 
brew cask install adoptopenjdk8   
```
Run `java -version` to make sure it is installed correctly.  

For Maven
```
brew install maven@3.5
```
Run `mvn -version`to ensure that Maven has been added to the PATH.
If Maven cannot be found, try running `brew link maven@3.5 --force`.  

#### Installing Prerequisites in Linux
For Java
```
sudo apt install openjdk-8-jdk
```
For Maven
You can follow the tutorial from the official [Maven](http://maven.apache.org/install.html) site. Remember that you need to install the 3.5.x version.

### Eclipse IDE
The simplest way to start developing on Eclipse Kura is to use an Eclipse Installer based setup.
To correctly setup the environment, proceed as follows:
- Start the Eclipse Installer
- Switch to advanced mode
- Select "Eclipse for Committers" and configure the "Product Version", then select a JRE 1.8+ and press the Next button
- Select the Eclipse Kura installer from the list. If this is not available, add a new installer from https://raw.githubusercontent.com/eclipse/kura/develop/kura/setups/kura.setup, then check and press the Next button
- Select the "Developer Type":
  - "User": if you want to develop applications or bundles running on Kura, select this option. It will install only the APIs and the examples.
  - "Developer" : if you are a framework developer, select this option. It will download and configure the Eclipse Kura framework.
- Update Eclipse Kura Git repository username and customize further settings if you like (e.g. Root install folder, Installation folder name)
- Leave all Bootstrap Tasks selected and press the Finish button
- Accept all the licenses and wait for the installation to finish

At first startup Eclipse IDE will checkout the code, perform a full build and configure a few Working Sets. Now you are ready to develop on Eclipse Kura.

To raise an issue, please report a bug on [GitHub issues](https://github.com/eclipse/kura/issues/new).



### Known Issues
Currently, the emulator web ui is not properly working on Windows so, with your setup, you will be able to build and deploy you applications, but not be able to use the Eclipse IDE based Kura emulator.

The full build of Kura is only supported for Linux and Mac Os based systems.

Currently the maven build on Windows requires to disable the tests and will fail when it tries to create the installers for the target platforms.


## Target Gateways Installers
Eclipse Kura provides pre-built installers for common development boards.
Check the following [link](https://www.eclipse.org/kura/downloads.php) to download the desired installers.


## Docker Image
Eclipse Kura is available also as a [Docker container](https://hub.docker.com/r/eclipse/kura/)
To easily run, use: `docker run -d -p 8080:8080 -t eclipse/kura`.
