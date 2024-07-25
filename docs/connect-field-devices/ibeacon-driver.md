# iBeacon&trade; Driver

Eclipse Kura provides a driver specifically developed to manage iBeacon&trade; protocol.
They can be used into the Wires framework, the Asset model or directly using the Driver itself.

## Features

The iBeacon&trade; driver is designed to listen for incoming beacon packets and to recognise the specific protocol. Of course it's not possible to write data to the beacons, since this is outside the protocol specification. 

## Installation

As the others Drivers supported by Eclipse Kura, it is distributed as a deployment package on the Eclipse Marketplace [here](https://marketplace.eclipse.org/content/ibeacon-driver-eclipse-kura-4xy). It can be installed following the instructions provided [here](../administration/application-management.md).

## Instance creation

A new iBeacon instance can be created either by clicking the **New Driver** button in the dedicated **Drivers and Assets** Web UI section or by clicking on the `+` button under **Services**. In both cases, the `org.eclipse.kura.driver.ibeacon` factory must be selected and a unique name must be provided for the new instance. 
Once instantiated, the Driver has to be configured setting the Bluetooth interface name (i.e. `hci0`) that will be used to connect to the device.

## Channel configuration

The iBeacon Driver channel can be configured with the following parameters:

- **enabled**: it allows to enable/disable the channel. If it isn't selected the channel will be ignored.
- **name**: the channel name.
- **type**: the channel type, (`READ`, `WRITE`, or `READ_WRITE`).
- **value.type**: the Java type of the channel value. The value read by the Driver will be converted to the **value.type**.
- **listen**: when selected, a listener will be attached to this channel. Any event on the channel will be reported using a callback and the value will be emitted.
