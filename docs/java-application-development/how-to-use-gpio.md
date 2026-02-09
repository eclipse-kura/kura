# How to use GPIOs

Eclipse Kura provides a **GPIO Service** that allows applications to interact with the system’s GPIOs.

## GPIO Service

Access to GPIO resources is granted by the [`GPIOService`](https://github.com/eclipse-kura/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/gpio/GPIOService.java). Once obtained, this service can be used to list the GPIOs available in the system and to acquire sets or individual GPIO Pins, configuring them either as digital inputs or digital outputs.

To retrieve the list of GPIOs available on the system, this method is provided by the `GPIOService`:

```java
public List<KuraGPIODescription> getAvailablePinDescriptions();
```

The **KuraGPIODescription** class contains a map of GPIO properties (such as name, controller, line, etc.) and provides a human-readable name for the pin through the `getDisplayName` method.

To acquiore one or more GPIO pins, use one of the following methods:

```java
public List<KuraGPIOPin> getPins(Map<String, String> description);
public List<KuraGPIOPin> getPins(Map<String, String> description, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger);
```

The description map includes the properties used to identify the pin and they are implementation specific. The second method additionally attempts to configure the pin using the given [direction](https://github.com/eclipse-kura/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/gpio/KuraGPIODirection.java), [mode](https://github.com/eclipse-kura/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/gpio/KuraGPIOMode.java) and [trigger](https://github.com/eclipse-kura/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/gpio/KuraGPIOTrigger.java).

The [`KuraGpioPin`](https://github.com/eclipse-kura/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/gpio/KuraGPIOPin.java) object is used to manipulate GPIO Pins and exposes methods to read the status of an input, or set the status of digital output as shown below.
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

!!! warning
    Starting from Kura 6.0.0, the following `GPIOService` methods have been deprecated: `getPinByName`, `getPinByTerminal` and `getAvailablePins`.

## LibGpiod GPIOService implementation

Beginning with Kura 6.0.0, the default `GPIOService` implementation is based on the [**LibGpiod**](https://libgpiod.readthedocs.io/en/latest/) linux library. It provides a low-level library, bindings and tools for interacting with the GPIO lines on Linux systems. Both versions 1.6.x and 2.x.x are supported.

Kura implements the `GPIOService` interface using a [JNA](https://github.com/java-native-access/jna) wrapper of the **LibGpiod** C APIs. 

The properties used to describe and identify a GPIO are listed below:

- _controller_: the _gpiochip_ the pin belongs to
- _line_: the _line_ offset of the pin in the gpiochip
- _name_: the optional pin name. If not provided, it defaults to "unknown"
- _displayName_: the pin name used to present the pin, with the default format "_name_:_controller_:_line_"

Applications can use the `getAvailablePinDescriptions` method to retrieve all the available GPIO descriptions and use it to get the desired `KuraGPIOPin` or directly fill the properties above and pass the map to the `getPins` method.
To get the **gpiochip**, **line** numbers and names, use the following command that lists all the GPIOS available on the system:

```
gpioinfo
```

For example, to get the `KuraGPIOPin` of the gpio with controller 3, line 5 and name "PIN1" use the method below:

```java
Map<String, String> description = new HashMap<>();
description.put("name", "PIN1");
description.put("controller", "3");
description.put("line", 5);

List<KuraGPIOPin> pins = this.gpioService.getPins(description);
```

If one or more properties (_controller_, _line_ or _name_) are omitted, all GPIOs matching the specified criteria are returned. For example, if only the _controller_ property is set, all the GPIOs belonging to that gpiochip are retrieved.

For backword compatibility, the deprecated `getPinByTerminal` can be still used. In this case, the pin is identified by a numeric value calculated as follows:

```
terminal = gpiochip * 1000 + line
```