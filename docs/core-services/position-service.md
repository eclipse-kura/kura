# Position Service

The PositionService provides the geographic position of the gateway if a GPS component is available and enabled.

When this service is enabled and provides a valid geographic position, this position is published in the gateway birth certificate with its location.

The GPS connection parameters must be defined in order to allow the service to receive the GPS frames. The PositionService supports direct access to gps device or the connection to that through gpsd.

For a device that is not connected to a GPS, it is possible to define a _static_ position by entering _latitude,_ _longitude,_, _altitude_ and _GNSS Type_. In this case, the position, date and time information is returned by the PositionService as if it were an actual GPS position. This may be useful when a gateway is installed in a known place and does not move.

To use this service, select the **PositionService** option located in the **Services** area as shown in the screen capture below.

![Position Service](./images/position-service.png)

This service provides the following configuration parameters:

- **enabled** - defines whether or not this service is enabled or disabled. (Required field.)

- **static** - specifies true or false whether to use a static position instead of a GPS. (Required field.)

- **provider** - species which position provider use, can be gpsd or serial. 
    - **gpsd** - gpsd service daemon if is available on the system. 
    - **serial** - direct access to gps device through serial or usb port.

- **gpsd.host** - host where gpsd service deamon is running. (required only if gpsd provider is selected.)

- **gpsd.port** - port where gpsd service is listening. (required only if gpsd provider is selected.)

- **latitude** - provides the static latitude value in degrees.

- **longitude** - provides the static longitude value in degrees.

- **altitude** - provides the static altitude value in meters.

- **GNSS Type** - provides the gnss system used to retrieve static information.

- **port** - supplies the USB or serial port of the GPS device.

- **baudRate** - supplies the baud rate of the GPS device.

- **bitsPerWord** - sets the number of bits per word (databits) for the serial communication to the GPS device.

- **stopbits** - sets the number of stop bits for the serial communication to the GPS device.

- **parity** - sets the parity for the serial communication to the GPS device.