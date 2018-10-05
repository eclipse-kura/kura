---
layout: page
title:  "BLE Beacon Drivers"
categories: [devices]
---

Eclipse Kura provides a set of drivers specifically developed to manage BLE Beacons. In particular, the framework offers support for iBeacon&trade; and Eddystone&trade; protocols.
The drivers are available only for gateways that support the new Bluetooth LE APIs. They can be used in to the Wires framework, the Asset model or directly using the Driver itself.

## Features

The iBeacon&trade; and Eddystone&trade; drivers are designed to listen for incoming beacon packets and to recognize the specific protocols. Of course it's not possible to write data to the beacons, since this is outside the protocol sprcification. On the Eddystone&trade; driver, moreover, the frame format to be filtered can be chosen from the channel definition. For more information about Eddystone&trade; frame format, see [here](https://developers.google.com/beacons/eddystone).
 
## Installation

As the others Drivers supported by ESF, it is distributed as a deployment package on the Eclipse Marketplace [here](https://marketplace.eclipse.org/content/ibeacon-driver-eclipse-kura) and [here](https://marketplace.eclipse.org/content/eddystone-driver-eclipse-kura). It can be installed following the instructions provided [here](../admin/application-management.html#section-eclipse-kura-marketplace).

## Instance creation

A new iBeacon or Eddystone Driver instance can be created either by clicking the **New Driver** button in the dedicated **Drivers and Assets** Web UI section or by clicking on the **+** button under **Services**. In both cases, the `org.eclipse.kura.driver.ibeacon` or `org.eclipse.kura.driver.eddsytone` factory must be selected and a unique name must be provided for the new instance. 
Once instantiated, the Driver has to be configured setting the Bluetooth interface name (i.e. hci0) that will be used to connect to the device.

## Channel configuration

The iBeacon Driver channel can be configured with the following parameters:

- **enabled**: it allows to enable/disable the channel. If it isn't selected the channel will be ignored.
- **name**: the channel name.
- **type**: the channel type, (`READ`, `WRITE`, or `READ_WRITE`).
- **value.type**: the Java type of the channel value. The value read by the Driver will be converted to the **value.type**.
- **listen**: when selected, a listener will be attached to this channel. Any event on the channel will be reported using a callback and the value will be emitted.

The Eddystone Driver is similar to the iBeacon one, but the frame format can be selected:

- **eddystone.type**: the type of the frame. Currently only `UID` and `URL` typed are supported.