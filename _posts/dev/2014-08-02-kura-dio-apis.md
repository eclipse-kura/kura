---
layout: page
title:  "Device I/O"
categories: [dev]
---

[Overview](#overview)

[Security Policy](#security-policy)

[Default Configuration](#default-configuration)

[APIs](#apis)

[GPIO](#gpio)

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

The current Device I/O implementation supports the following hardware platforms:

  *  ARMv6

  *  ARMv7

and the following devices:

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
### Use GPIO Pin with GPIOService for a switch a Led. 

This example was developed for a **Raspberry Pi 3**.

Inside the **jdk.dio.properties** file:

```
...
6 = deviceType: gpio.GPIOPin, pinNumber:6, name:GPIO06
...
```

Then in the code:

```java

private GPIOService myservice;

KuraGPIOPin pin = this.myservice.getPinByTerminal(6);

// to allow pin configuration use
//pin = this.myservice.getPinByTerminal(this.options.isConfigPin());

``` 
In a **component.xml** enter a new service. Select a **Service** and in a **Referenced Service** open **Add** for add  the GPIOService. Select the **GPIOService** and use edit button for set in the bind field to **bindGPIOService** and set the unbind filed to **unbindGPIOService**.
Created methods bind and unbind in a principale software.


<img src="{{ site.baseurl }}/assets/images/drivers_and_assets/component_gpio_led.png"/>

Then in the code:

```java
protected synchronized void bindGPIOService(final GPIOService gpioService) {
        this.myservice = gpioService;
    }

protected synchronized void unbindGPIOService(final GPIOService gpioService) {
        this.myservice = null;
    }
``` 


#### Accessing a GPIO Pin by its index


```java
// Accessing the GPIO Pin number 6. The default behaviour is defined in the
// jdk.dio.properties file
//
// i.e.:
// gpio.GPIOPin = initValue:0, deviceNumber:0, direction:3, mode:-1, trigger:3
// 6 = deviceType: gpio.GPIOPin, pinNumber:6, name:GPIO06

close(); //Close the pin

    	
pin = this.myservice.getPinByTerminal(this.options.isConfigPin());
    	    
if (pin == null) {
return;
}

open(); //Open the pin

setValue(this.options.isEnableLed()); //Turns the LED on and of

```


The metods for using the Pin:

```java
private void open() {
    	try {
            pin.open();
        } catch (KuraGPIODeviceException | KuraUnavailableDeviceException | IOException e) {
        	logger.error("Exception GPIOService ", e);
        }
    }
    
    private void close() {
    	
    	if (pin != null) {
    		try {
    			pin.close();
    		} catch (IOException e) {
    			logger.error("Exception GPIOService ", e);
    		}
    	}
    }
    
    private void setValue(boolean bool) {
    	try {
            pin.setValue(bool);
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException | KuraUnavailableDeviceException | IOException | KuraClosedDeviceException e) {
            logger.error("Exception GPIOService ", e);
        }
    }
```


### Create Options Led Class.

```java

package org.eclipse.kura.example.gpio.led;

import static java.util.Objects.requireNonNull;

import java.util.Map;

public class LedOptions {
	
	private static final String PROPRTY_PIN_NOME = "configurePin";
	private static final int PROPRTY_PIN_DEFAULT = 6;

    private static final String PROPRTY_LED_NOME = "switchLed";
    private static final boolean PROPRTY_LED_DEFAULT = false;
    
    private final int configPin;  
    private final boolean enableLed;

    public LedOptions(Map<String, Object> properties) {

        requireNonNull(properties, "Required not null");
        this.configPin = getProperty(properties, PROPRTY_PIN_NOME, PROPRTY_PIN_DEFAULT);
        this.enableLed = getProperty(properties, PROPRTY_LED_NOME, PROPRTY_LED_DEFAULT);

    }
    
    public int isConfigPin() {
    	return this.configPin;
    }

    public boolean isEnableLed() {
        return this.enableLed;
    }
    

    @SuppressWarnings("unchecked")
    private <T> T getProperty(Map<String, Object> properties, String propertyName, T defaultValue) {
        Object prop = properties.getOrDefault(propertyName, defaultValue);
        if (prop != null && prop.getClass().isAssignableFrom(defaultValue.getClass())) {
            return (T) prop;
        } else {
            return defaultValue;
        }
    }

}

```

### Metatype Definition

At this point, you have to write the ‘metatype’ file that defines the parameters. 

```
<?xml version="1.0" encoding="UTF-8"?>
<MetaData xmlns="http://www.osgi.org/xmlns/metatype/v1.2.0" localization="en_us">
    <OCD id="org.eclipse.kura.example.gpio.led.LedExample"
        name="Gpio Led Example"
        description="This example allows to configure and manage the state of a LED connected to a configurable GPIO pin">

        <AD id="configurePin"
            name="configurePin"
            type="Integer"
            cardinality="0"
            required="true"
            default="6"
            min = "0"
            description="Configure the Pin to use."/>
            
            
        <AD id="switchLed"
            name="switchLed"
            type="Boolean"
            cardinality="0"
            required="true"
            default="false"
            description="Switches the selected GPIO port state."/>
            
    </OCD>

    <Designate pid="org.eclipse.kura.example.gpio.led.LedExample">
        <Object ocdref="org.eclipse.kura.example.gpio.led.LedExample"/>
    </Designate>
</MetaData>
```

<img src="{{ site.baseurl }}/assets/images/drivers_and_assets/metatype_gpio_led.png"/>

### Accessing a I2C device with OpenJDK Device I/O

An SPI device can be accessed in the same way as GPIO.

{% include alerts.html message="The default **jdk.dio.properties** file doesn't contain any default configuration for I2C devices. When accessing a I2C device through its index, a new default configuration for the device must be added to the **jdk.dio.properties** file." %}

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
