---
layout: page
title:  "Xdk Driver"
categories: [devices]
---

Eclipse Kura provides a specific driver that can be used to interact with Bosch Xdk110 devices. The driver is available only for gateways that support the new Bluetooth LE APIs. 
It can be used in to the Wires framework, the Asset model or directly using the Driver itself.

{% include alerts.html message='The Xdk driver can only be used with Xdk110 with VirtualXdkDemo installed and with firmware version > 3.5.0. If your device has an older firmware, please update it.' %}

## Features

The Xdk Driver can be used to get the values from all the sensor provider by the xdk110 (both in polling mode and notification):
- three-axis acceleration (or four-axis quaternion rappresentation) 
- three-axis gyroscope (or four-axis quaternion rappresentation)
- light
- noise
- pressure
- ambient temperature
- humidity
- sd-card detect status
- push buttons
- three-axis magnetic field
- magnetometer resistence
- led status
- rms voltage of LEM sensor

| Resource | Unit | Description |
|----------|------|-------------|
| **ACCELERATION_X**, **ACCELERATION_Y**, **ACCELERATION_Z** | G | The proper acceleration for each axis* |
| **GYROSCOPE_X**, **GYROSCOPE_Y**, **GYROSCOPE_Z** | rad/S | The angular acceleration for each axis* |
| **LIGHT** | lux | The light value |
| **NOISE** | DpSpl | The acustic pressure value |
| **PRESSURE** | Pa | The pressure value |
| **TEMPERATURE** | °C | The temperature value |
| **HUMIDITY** | %rH | The relative humidity |
| **SD_CARD_DETECTION_STATUS** | boolean | SD-Card detect status |
| **PUSH_BUTTONS** | bit | Button status, encoded as bit field: Bit 0, Button 1; Bit 1, Button 2 |
| **MAGNETOMETER_X**, **MAGNETOMETER_Y**, **MAGNETOMETERE_Z** | uT | The magnetometer value for each axis |
| **MAGNETOMETER_RESISTENCE** | ohm | The magnetometer resistence value |
| **LED_STATUS** | bit | Led status, encoded as a bit field: Bit 0: yellow Led; Bit 1: orange Led; Bit 2: red Led|
| **VOLTAGE_LEM** | mV | RMS voltage of LEM sensor |

*If the Quaternion rappresentation is selected, then:

| Resource | Unit | Description |
|----------|------|-------------|
|**QUATERNION_M**, **QUATERNION_X**, **QUATERNION_Y**, **QUATERNION_Z** | number | The rotation-quaternion for each axis |


When a notification is enabled for a specific channel (sensor), the notification period can't be set. Use the Timer in Wire Graph to set polling.

## Documentation

All the information regarding the Xdk110 is available in the Xdk Bosch Connectivity [here](https://xdk.bosch-connectivity.com/home) website.
The XDK-Workbench is the tool that can be used to develop software for the Xdk110. It can be downloaded [here](https://xdk.bosch-connectivity.com/it/software-downloads). XDK-Workbench is required to install VirtualXdkDemo.

{% include alerts.html message='We found connection problems with Xdk, probably XDK-Workbench version 3.6.0 have a problem, i raccomend installing the version 3.5.0' %}

The Virtual XDK application user guide contains all the information regarding the XDK110 UUIDs and data formats [here](http://xdk.bosch-connectivity.com/xdk_docs/html/_x_d_k__v_i_r_t_u_a_l__x_d_k__a_p_p__u_s_e_r__g_u_i_d_e.html). 

{% include alerts.html message='To switch between quaternion and sensor representation, the XDK110 needs to be instructed using the 55b741d5-7ada-11e4-82f8-0800200c9a66 UUID. Set it to 0x01 to enable Quaternions.' %}

If quaternion representation is enabled, the data format is as follows:

Bytes | Data | Type
---------|---------- |----------
0,1,2 & 3|**Rotation Quaternion M**|float
4,5,6 & 7|**Rotation Quaternion X**|float
8,9,10 & 11|**Rotation Quaternion Y**|float
12,13,14 & 15|**Rotation Quaternion Z**|float
 
 
## Installation

As the others Drivers supported by Kura, it is distributed as a deployment package on the Eclipse Marketplace [here](https://marketplace.eclipse.org) and [here](https://marketplace.eclipse.org). It can be installed following the instructions provided [here](../admin/application-management.html#section-eclipse-kura-marketplace).

## Instance creation

A new Xdk Driver instance can be created either by clicking the **New Driver** button in the dedicated **Drivers and Assets** Web UI section or by clicking on the **+** button under **Services**. In both cases, the `org.eclipse.kura.driver.ble.xdk` factory must be selected and a unique name must be provided for the new instance. 
Once instantiated, the Xdk Driver has to be configured setting the Bluetooth interface name (i.e. hci0). Other options available are related to the quaternion/sensor representation and the sensor sampling rate (in Hz). 

## Channel configuration

The Xdk Driver channel can be configured with the following parameters:

- **enabled**: it allows to enable/disable the channel. If it isn't selected the channel will be ignored.
- **name**: the channel name.
- **type**: the channel type, (`READ`, `WRITE`, or `READ_WRITE`).

{% include alerts.html message='The Xdk driver can only be used with READ.' %}

- **value.type**: the Java type of the channel value. The value read by the Driver will be converted to the **value.type**.
- **listen**: when selected, a listener will be attached to this channel. Any event on the channel will be reported using a callback and the value will be emitted.
- **xdk.address**: the address of the specific xdk (i.e. aa:bb:cc:dd:ee:ff)
- **sensor.name**: the name of the sensor. It can be selected from a drop-down list.

