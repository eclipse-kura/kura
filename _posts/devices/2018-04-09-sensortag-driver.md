---
layout: page
title:  "TI SensorTag Driver"
categories: [devices]
---

Eclipse Kura provides a specific driver that can be used to interact with Texas Instruments SensorTag devices. The driver is available only for gateways that support the new Bluetooth LE APIs. 
It can be used in to the Wires framework, the Asset model or directly using the Driver itself.

{% include alerts.html message='The SensorTag driver can be used only with TI SensorTags with firmware version >1.20. If your device has an older firmware, please update it.' %}

## Features

The SensorTag Driver can be used to get the values from all the sensor installed on the tag (both in polling mode and notification):
- ambient and target temperature
- humidity
- pressure
- three-axis acceleration
- three-axis magnetic field
- three-axis orientation
- light
- push buttons

Moreover, the following resources can be written by the driver:
- read and green leds
- buzzer

When a notification is enabled for a specific channel (sensor), the notification period can be set.
 
## Installation

As the others Drivers supported by Kura, it is distributed as a deployment package on the Eclipse Marketplace [here](https://marketplace.eclipse.org/content/ti-sensortag-driver-eclipse-kura-3xy) and [here](https://marketplace.eclipse.org/content/ti-sensortag-driver-eclipse-kura-4xy). It can be installed following the instructions provided [here](../admin/application-management.html#section-eclipse-kura-marketplace).

## Instance creation

A new TiSensorTag Driver instance can be created either by clicking the **New Driver** button in the dedicated **Drivers and Assets** Web UI section or by clicking on the **+** button under **Services**. In both cases, the `org.eclipse.kura.driver.ble.sensortag` factory must be selected and a unique name must be provided for the new instance. 
Once instantiated, the SensorTag Driver has to be configured setting the Bluetooth interface name (i.e. hci0) that will be used to connect to the device.

## Channel configuration

The SensorTag Driver channel can be configured with the following parameters:

- **enabled**: it allows to enable/disable the channel. If it isn't selected the channel will be ignored.
- **name**: the channel name.
- **type**: the channel type, (`READ`, `WRITE`, or `READ_WRITE`).
- **value.type**: the Java type of the channel value. The value read by the Driver will be converted to the **value.type**. Conversely, in write operations the Driver will accept value of this kind.
- **listen**: when selected, a listener will be attached to this channel. Any event on the channel will be reported using a callback and the value will be emitted.
- **sensortag.address**: the address of the specific SensorTag (i.e. aa:bb:cc:dd:ee:ff)
- **sensor.name**: the name of the sensor. It can be selected from a drop-down list
- **notification.period**: the period in milliseconds used to receive notification for a specific sensor. The value will be ignored it the **listen** option is not checked.
