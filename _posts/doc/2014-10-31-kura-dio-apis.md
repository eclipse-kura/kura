---
layout: page
title:  "Device I/O"
date:   2014-08-10 12:28:21
categories: [doc]
---

[Overview](#overview)

[Security Policy](#security-policy)

[Default Configuration](#default-configuration)

[APIs](#apis)

[Code Examples](#code-examples)

## Overview

OpenJDK Device I/O is a third-party library which leverages standard Java ME
Device I/O APIs to Java SE.
Kura ships the relevant native libraries together with the default hardware
configuration for each platforms it runs on.

When using some devices, the hardware configuration may change according to
user needs (i.e. BeagleBone Black Device-Tree Overlay).
In such cases the default configuration can be changed by modifying the
default configuration files shipped with Kura.

The current Device I/O implementation supports the following:

*  Hardware Platforms

    *  ARMv6

    *  ARMv7

*  Devices
	* GPIO

	* I2C

## Security Policy

Standard Java Security Policy is used in order to restrict access to specific devices.
The default Kura distribution allows access to all the peripherals through the
**jdk.dio.policy**

```
grant {
	permission jdk.dio.DeviceMgmtPermission "*:*", "open";

	permission jdk.dio.gpio.GPIOPinPermission "*:*", "open,setdirection";
	permission jdk.dio.gpio.GPIOPortPermission "*:*";
	permission jdk.dio.i2cbus.I2CPermission "*:*";
	permission jdk.dio.spi.SPIPermission "*:*";
};
```

Custom permissions may be specified editing the file.

## Default Configuration

Default hardware configuration for the hardware platform is defined in the **jdk.dio.properties** file.
Standard configuration for complex devices can be added on a per-device basis:

```
#Default PIN configuration. To be overwritten in the following lines
gpio.GPIOPin = initValue:0, deviceNumber:0, direction:3, mode:-1, trigger:3

#Standard PIN configuration
5 = deviceType: gpio.GPIOPin, pinNumber:5, name:GPIO05

#Standard I2C device configuration
41 = deviceType: i2cbus.I2CDevice, address:0x29, addressSize:7, clockFrequency:400000

```

## APIs

Kura supports the full set of APIs for the listed device types.
Refere to the [API Reference](../ref/api-ref.html) for further information on the APIs.

## Code Examples

### Accessing a GPIO Pin with OpenJDK Device I/O

A GPIO Pin can be accessed by referencing it's index in the properties file,
or by creating a Pin configuration object and feeding it to the DeviceManager:

#### Accessing a GPIO Pin by its index

```java
// Accessing the GPIO Pin number 17. The default behaviour is defined in the
// jdk.dio.properties file
//
// i.e.:
// gpio.GPIOPin = initValue:0, deviceNumber:0, direction:3, mode:-1, trigger:3
// 17 = deviceType: gpio.GPIOPin, pinNumber:17, name:GPIO_USER_1

GPIOPin led = (GPIOPin)DeviceManager.open(17);

led.setValue(true) //Turns the LED on
led.setValue(false) //Turns the LED off
boolean status = led.getValue() //true if the LED is on

```

#### Accessing a GPIO Pin using a device configuration object

```java
// Accessing the Pin number 17 with custom configuration

GPIOPinConfig pinConfig = new GPIOPinConfig(
	DeviceConfig.DEFAULT, 						//GPIO Controller number or name
	17, 												//GPIO Pin number
	GPIOPinConfig.DIR_INPUT_ONLY,				//Pin direction
	GPIOPinConfig.MODE_INPUT_PULL_DOWN, 	//Pin resistor
	GPIOPinConfig.TRIGGER_BOTH_EDGES, 		//Triggers
	false 											//initial value (for outputs)
);

GPIOPin button = (GPIOPin) DeviceManager.open(GPIOPin.class, pinConfig);

button.setInputListener(new PinListener(){
		@Override
		public void valueChanged(PinEvent event) {
			System.out.println("PIN Status Changed!");
			System.out.println(event.getLastTimeStamp() + " - " + event.getValue());
		}
});

```

### Accessing a I2C device with OpenJDK Device I/O

An SPI device can be accessed in the same way as GPIO.

{% include alerts.html message="The default **jdk.dio.properties** file doesn't contain any default
configuration for I2C devices. When accessing a I2C device through its index, a new default configuration for the device
must be added to the **jdk.dio.properties** file." %}

####  Accessing a I2C Device using its peripheral index

Inside the **jdk.dio.properties** file:

```
...
41 = deviceType: i2cbus.I2CDevice, address:0x29, addressSize:7, clockFrequency:400000
...
```

Then in the code:

```java
I2CDevice aDevice = (I2CDevice) DeviceManager.open(41);

```

#### Accessing a I2C Device using a Device Configuration object:

```java
I2CDeviceConfig config = new I2CDeviceConfig(
	1,									//I2C bus index
	41, 								//I2C device address
	7, 								//Number of bits in the address
	400000							//I2C Clock Frequency
);

I2CDevice aDevice = (I2CDevice) DeviceManager.open(I2CDevice.class, config);

```

#### Reading and writing data

```java
/*
 * OpenJDK Device I/O can manage transactional reads/writes.
 * Data sent between a begin() and end() block will create
 * a single I2C combined message that will be sent after a
 * call to end(), thus assuring data serialization and native
 * buffers optimization
 */

// I2C transaction example.
// Init sequence for a Digital Light Sensor
digitalLightSensor.begin();
digitalLightSensor.write(0x80);
digitalLightSensor.write(0x03);

digitalLightSensor.write(0x81);
digitalLightSensor.write(0x11);

digitalLightSensor.write(0x86);
digitalLightSensor.write(0x00);

digitalLightSensor.end();

//I2C normal write
digitalLightSensor.write(value);

//I2C normal read
int value = digitalLightSensor.read();

```
