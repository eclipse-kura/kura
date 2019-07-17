---
layout: page
title:  "Xdk Driver"
categories: [devices]
---

Eclipse Kura provides a specific driver that can be used to interact with Bosch Xdk110 devices. The driver is available only for gateways that support the new Bluetooth LE APIs. 
It can be used in to the Wires framework, the Asset model or directly using the Driver itself.

{% include alerts.html message='The Xdk driver can only be used with Xdk110 with VirtualXdkDemo installed and with firmware version> 3.5.0. If your device has an older firmware, please update it.' %}

## Features

The Xdk Driver can be used to get the values from all the sensor installed on the xdk110 (both in polling mode and notification):
- three-axis acceleration (or four-axis rotecion-quaternion) 
- three-axis gyroscope (or four-axis rotecion-quaternion)
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


When a notification is enabled for a specific channel (sensor), the notification period can't be set. Use the Timer in Wire Graph to set notification period.

## Documentation

All information on the Xdk110 is in Xdk Bosch Connectivity [here](https://xdk.bosch-connectivity.com/home). XDK-Workbench is tool to develop software for Xdk based on Eclipse platform, for XDK-Workbench download [here](https://xdk.bosch-connectivity.com/it/software-downloads). XDK-Workbench is required to install VirtualXdkDemo.

{% include alerts.html message='XDK-Workbench version 3.6.0 have a problem, i raccomend installing the version 3.5.0' %}

All information on the VirtualXdkDemo is in Virtual XDK application user guide [here](http://xdk.bosch-connectivity.com/xdk_docs/html/_x_d_k__v_i_r_t_u_a_l__x_d_k__a_p_p__u_s_e_r__g_u_i_d_e.html), in  Virtual XDK application user guide you will find the references to the UUID and data format. 

{% include alerts.html message='In Virtual XDK application user guide for Xdk API version 3.4.0 a Characteristic of a Service is missing. For Control XDK Service the Characteristic: 55b741d5-7ada-11e4-82f8-0800200c9a66 is missing. This Characteristic is used to enable or disable the Rotations Quaternions. If you write 0x00 then enable Accelemoter and Gyroscope, else you write 0x01 then enable Rotaton Quaternions.' %}

if enable Rotation Quaternions  then the interpretation of the data is:

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
Once instantiated, the Xdk Driver has to be configured setting the Bluetooth interface name (i.e. hci0) that will be used to connect to the device, if you want to enable Quaternion (i.e. true) or enable Acceleration and Gyroscope (i.e. false) and setting the sample rate in Hz (i.e. 10). 

## Channel configuration

The SensorTag Driver channel can be configured with the following parameters:

- **enabled**: it allows to enable/disable the channel. If it isn't selected the channel will be ignored.
- **name**: the channel name.
- **type**: the channel type, (`READ`, `WRITE`, or `READ_WRITE`).

{% include alerts.html message='The Xdk driver can only be used with READ.' %}

- **value.type**: the Java type of the channel value. The value read by the Driver will be converted to the **value.type**.
- **listen**: when selected, a listener will be attached to this channel. Any event on the channel will be reported using a callback and the value will be emitted.
- **xdk.address**: the address of the specific xdk (i.e. aa:bb:cc:dd:ee:ff)
- **sensor.name**: the name of the sensor. It can be selected from a drop-down list.

