# Eddystone&trade; Driver

Eclipse Kura offers support for Eddystone&trade; protocol via a specific driver.
It can be used into the Wires framework, the Asset model or directly using the Driver itself.

## Features

The Eddystone&trade; driver is designed to listen for incoming beacon packets and to recognise the specific protocol. Of course it's not possible to write data to the beacons, since this is outside the protocol specification. The frame format to be filtered can be chosen from the channel definition. For more information about Eddystone&trade; frame format, see [here](https://developers.google.com/beacons/eddystone).
 
## Installation

As the others Drivers supported by Eclipse Kura, it is distributed as a deployment package on the Eclipse Marketplace [here](https://marketplace.eclipse.org/content/eddystone-driver-eclipse-kura-4xy). It can be installed following the instructions provided [here](../administration/application-management.md).

## Instance creation

A new Eddystone Driver instance can be created either by clicking the **New Driver** button in the dedicated **Drivers and Assets** Web UI section or by clicking on the `+` button under **Services**. In both cases, the `org.eclipse.kura.driver.eddsytone` factory must be selected and a unique name must be provided for the new instance. 
Once instantiated, the Driver has to be configured setting the Bluetooth interface name (i.e. `hci0`) that will be used to connect to the device.

## Channel configuration

The Eddystone Driver channel can be configured with the following parameters:

- **enabled**: it allows to enable/disable the channel. If it isn't selected the channel will be ignored.
- **name**: the channel name.
- **type**: the channel type, (`READ`, `WRITE`, or `READ_WRITE`).
- **value.type**: the Java type of the channel value. The value read by the Driver will be converted to the **value.type**.
- **listen**: when selected, a listener will be attached to this channel. Any event on the channel will be reported using a callback and the value will be emitted.
- **eddystone.type**: the type of the frame. Currently only `UID` and `URL` typed are supported.
