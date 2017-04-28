---
layout: page
title:  "Kura Wires overview"
categories: [wires]
---

[Introduction](#introduction)

[Wire Components provided in Kura 3.0.0](#wire-components-provided-in-kura-300)

[Wire Drivers](#wire-drivers)

[Additional information](#additional-information)

* [DZONE](#dzone)

* [Master Thesis](#master-thesis)

* [Conferences and slides](#conferences-and-slides)

* [Youtube](#youtube)

## Introduction

Eclipse Kura Wires provides in Kura environment an implementation of the Dataflow Programming model. The Application logic is expressed as a directed graph:
![Wire graph](https://s3.amazonaws.com/kura-resources/Webminar-resources/Wire_graph.png)
The nodes in the graph have inputs and/or outputs, abstract the underlying logics and are highly reusable and portable.

In this way, the developer can easily prototype its solution without sacrificing flexibility and working at a high level of abstraction: the graph is made of nodes and connections that can be extended adding new nodes or drawing new connections.
Furthermore, the developer can take advantage of the Eclipse Marketplace integration, being able to use open source or commercial building blocks into the final solution, by simply dragging and dropping a link to the Eclipse Marketplace in the Eclipse Kura Administrative Web UI.

![Eclipse Kura Architecture with Wires](https://s3.amazonaws.com/kura-resources/Webminar-resources/Wires_kura_architecture.png)

Kura Wires is an application built on top of two new Kura features:

  * Drivers, the low-level components responsible for the communication with the sensors and actuators;
  * Assets, an abstraction on top of Kura Drivers that allow the interaction with the underneath drivers in an easy way, by simply specifying "channels". The channel is a logical entity that eases the way to specify what has to be transferred to/from the driver.

![From asset to Driver to Transducers](https://s3.amazonaws.com/kura-resources/Webminar-resources/asset_driver_device.png)

## Wire Components provided in Kura 3.0.0

The following components will be provided out of the box with the Kura 3.0.0 installer:

  * **Timer**, tick every x seconds and start the graph;
  * **Publisher**, publishes every message received from a Wire (Wire Message). It is configurable in order to use a specific Cloud Service;
  * **Subscriber**, subscribes to a configurable topic via a specific Cloud Service. It receives a message from a Cloud Platform, wraps it as a Wire Message and sends it through the connected wires to the other components that are part of the Wire Graph;
  * **DB Store**, allows the storage of Wire Messages into a specific DB table. It has rules for message cleanup and retention;
  * **DB Filter**, allows the filtering of messages residing in a DB via a proper SQL query. The corresponding messages are sent as Wire Messages to the connected Wire Components;
  * **Logger**, logs the received messages;
  * **Asset**, allows the definition of Wire Channels that will be used to communicate with the associated driver instance.

## Wire Drivers
Wire drivers will be made available in form of Deployment Packages (dp) in the Eclipse Marketplace, in order to ease the deployment in the final Kura devices.
For some of them, the source code will be available in the [Kura Github Repository](https://github.com/eclipse/kura).

As initial contributions, the following drivers are available in Eclipse Marketplace:

  * **Modbus Driver**
  * **OPC-UA Driver**

## Additional information
Additional information about Kura Wires is available at the following resources:

### DZONE
  * [Kura Wires Can Help Overcome Challenges of Industrial IoT](https://dzone.com/articles/kura-wires)
  * [Kura Wires: A Sneak Peek](https://dzone.com/articles/kura-wires-a-sneak-peek)
  * [Kura Wires: A Different Perspective to Develop IIoT Applications](https://dzone.com/articles/kura-wires-a-different-perspective-to-develop-iiot)
  * [Different Dataflow Programming Approaches and Comparison With Kura Wires](https://dzone.com/articles/different-dataflow-programming-approaches-and-comp)

### Master Thesis
  * [Kura Wires: Design and Development of a Component for managing Devices and Drivers in Eclipse Kura 2.0 by Amit Kumar Mondal](https://osf.io/s3agq/)

### Conferences and slides
  * [Building IoT Mashups for Industry 4.0 with Eclipse Kura and Kura Wires](https://www.slideshare.net/eclipsekura/building-iot-mashups-for-industry-40-with-eclipse-kura-and-kura-wires)
  * [Industry 4.0 with Eclipse Kura](https://www.eclipsecon.org/europe2016/session/industry-40-eclipse-kura)

### Youtube
  * [Kura Wires - A Mashup in Eclipse Kura for Industry 4.0](https://youtu.be/hIy-Nnt7Etg)
  * [Kura Wires: Industry 4.0 with Eclipse Kura - EclipseCon Europe 2016 IoT Day](https://youtu.be/Td5923B26-Q)
