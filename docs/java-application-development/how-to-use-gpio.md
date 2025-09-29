# How to use GPIO

GPIO resources can be accessed either using the **GPIO Service** provided by Kura, or directly using the **OpenJDK Device I/O** embedded library.

## GPIO Service

Access to GPIO resources is granted by the **GPIOService**. Once retrieved, the service can be used to acquire a GPIO Pin and use it as a digital output or a digital input.

The GPIO Service exposes methods to retrieve a GPIO Pin via its name or index as shown below.

```java
KuraGpioPin thePin = gpioServiceInstance.getPinByTerminal(18);
KuraGpioPin thePin = gpioServiceInstance.getPinByName("IgnitionPin");
```

The KuraGpioPin object is used to manipulate GPIO Pins and exposes methods to read the status of an input, or set the status of digital output as shown below.
```java
//sets digital output value to high
thePin.setValue(true);

//get value of a digital input pin
boolean active = thePin.getValue();

//listen for status change on a digital input pin
try {  
      thePin.addPinStatusListener(new PinStatusListener() {
          @Override    
          public void pinStatusChange(boolean value) {      
          // Perform tasks when pin status changes    
          }  
      });
} catch (KuraClosedDeviceException e) {
  // Here if GPIO cannot be acquired
  } catch (IOException e) {
    // Here on I/O error
  }
```
## Pin Configuration

Pin names, indexes, and configuration are defined in the **jdk.dio.properties** file.

Although GPIO pins can be accessed with their default configuration, the settings of each pin can be changed when acquiring it with the GPIO Service as shown below.
```java
KuraGpioPin customInputPin = gpioServiceInstance.getPinByTerminal(
  14, 
  KuraGPIODirection.INPUT, 
  KuraGPIOMode.INPUT_PULL_UP, 
  KuraGPIOTrigger.BOTH_LEVELS);
```

### Default Configuration

Default hardware configuration for the hardware platform is defined in the **jdk.dio.properties** file. Standard configuration for complex devices can be added on a per-device basis as shown below.

```text
#Default PIN configuration. To be overwritten in the following lines
gpio.GPIOPin = initValue:0, deviceNumber:0, direction:3, mode:-1, trigger:3

#Standard PIN configuration
64 = deviceType: gpio.GPIOPin, pinNumber:64, name:RELAY1
```

Starting from Kura 5.5.0, the default pin configuration can be described in the form of symbolic links. The syntax is as follows:

```text
/dev/digital_in1 = deviceType: gpio.GPIOPin, direction:0, mode=1, name:digital_in1
```

where `/dev/digital_in1` points to `/sys/class/gpio/gpioXYZ`. The pin number is set to XYZ and it can be retrieved by name.

## OpenJDK Device I/O

Linux-level access in Kura is granted through OpenJDK Device I/O, a third-party library that leverages standard Java ME Device I/O APIs to Java SE. Kura is distributed with the relevant native libraries, together with the default hardware configuration, for each platform on which it runs.

I2C, SPI, and GPIO resources can be directly accessed through the jdk.dio library present in the target platform. 

### APIs

Kura supports the full set of APIs for the listed device types. Refer to [References](/references/javadoc/) for further API information.

### Accessing a GPIO Pin with OpenJDK Device I/O

A GPIO Pin can be accessed by referencing its index in the properties file, or by creating a Pin configuration object and feeding it to the DeviceManager as shown in the code examples below.

####  Accessing a GPIO Pin by its Index

```java
#Accessing the GPIO Pin number 17. The default behaviour is defined in the
#jdk.dio.properties file
#
#i.e.:
# gpio.GPIOPin = initValue:0, deviceNumber:0, direction:3, mode:-1, trigger:3
# 17 = deviceType: gpio.GPIOPin, pinNumber:17, name:GPIO_USER_1

GPIOPin led = (GPIOPin)DeviceManager.open(17);

led.setValue(true) //Turns the LED on
led.setValue(false) //Turns the LED off
boolean status = led.getValue() //true if the LED is on
```

#### Accessing a GPIO Pin Using a Device Configuration Object
```java
#Accessing the Pin number 17 with custom configuration

GPIOPinConfig pinConfig = new GPIOPinConfig(
    DeviceConfig.DEFAULT,                       //GPIO Controller number or name
    17,                                                 //GPIO Pin number
    GPIOPinConfig.DIR_INPUT_ONLY,               //Pin direction
    GPIOPinConfig.MODE_INPUT_PULL_DOWN,     //Pin resistor
    GPIOPinConfig.TRIGGER_BOTH_EDGES,       //Triggers
    false                                           //initial value (for outputs)
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

## LibGpiod

In version 5.6.1, Eclipse Kura introduces an experimental support for [**LibGpiod**](https://libgpiod.readthedocs.io/en/latest/). It provides a low-level library, bindings and tools for interacting with the GPIO lines on Linux systems.

Kura implements the **GPIOService** interface using a [JNA](https://github.com/java-native-access/jna) interface of the **LibGpiod** C APIs. The user applications can seamlessly continue to use the **GPIOService** APIs without changes on the code. The only difference is the way the GPIO pin number is managed by the new library. **LibGpiod** has not the concept of terminal (a.k.a. a number that identifies the GPIO pin), but it selects the pins using **gpiochip** and **line**. To correctly use the `getPinByTerminal` method, a pin is identified by a number calculated as follows:

```
terminal = gpiochip * 1000 + line
```

The `getPinByName` method can be used as in the previous implementation.

There isn't any correlation between the terminal number and name used in **LibGpiod** and **OpenJDK Device I/O**. To get the new **gpiochip**, **line** numbers and names, use the following command that lists all the gpio available on the system:

```
gpioinfo
```

!!! warning
    If a GPIO on a Linux system is exported in `/sys/class/gpio`, it cannot be directly used by **LibGpiod**. The user has to unexport the pin with this command: `echo <pin-number> > /sys/class/gpio/unexport`.

Since the support is experimental, the **LibGpiod** bundle is installed, but not activated. To enable it, edit the `/opt/eclipse/kura/framework/config.ini` and modify this entry:

```
reference\:file\:/opt/eclipse/kura/plugins/org.eclipse.kura.linux.gpio_1.6.0.jar@5\:start
```

with

```
reference\:file\:/opt/eclipse/kura/plugins/org.eclipse.kura.linux.gpio.libgpiod_1.0.0-SNAPSHOT.jar@5\:start
```

This is a bash script that edit the `config.ini` to enable libgpiod.

```bash
#!/bin/bash
CONFIG_FILE="/opt/eclipse/kura/framework/config.ini"
sed -i.bak -E 's/org\.eclipse\.kura\.linux\.gpio_[^,]*/org.eclipse.kura.linux.gpio.libgpiod_1.0.0-SNAPSHOT.jar@5\:start/g' "$CONFIG_FILE"
```