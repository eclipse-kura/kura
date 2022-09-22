# How to Use Modbus

ModbusProtocolDevice is a service that provides a connection to a device or a network of devices over Serial Line (RS-232/RS-485) or Ethernet using the Modbus protocol. This service implements a subset of the Modbus Application Protocol as defined by Modbus Organization (for more information, refer to <http://www.modbus.org/specs.php>).

The ModbusProtocolDevice service needs to receive a valid Modbus configuration including the following parameters:

* **Modbus protocol mode** - defines the protocol mode as RTU or ASCII (only RTU mode for Ethernet connections).

* **Timeout** - sets the timeout in order to detect a disconnected device.

The ModbusProtocolDevice service also requires a valid Serial Line or Ethernet connection configuration including the following parameters:

* **Serial Line**  
  * port name
  * baudrate
  * bits
  * stops
  * parity
  
* **Ethernet**  
  * ip address
  * port number

When a valid configuration is received, the ModbusProtocolDevice service tries to open the communication port. Serial Line communication uses the CommConnection class; Ethernet communication is based on java.net.Socket. When the communication is established, the client makes direct calls to the Modbus functions. The first parameter of each method is the Modbus address of the queried unit. This address must be in the range of 1 - 247.

## Function Codes

The following function codes are implemented within the ModbusProtocolDevice service:

* **01 (0x01) readCoils(int unitAddr,  int dataAddress, int count)** - read 1 to 2000 maximum contiguous status of coils from the attached field device with address "unitAddr". An array of booleans representing the requested data points is returned.

* **02 (0x02) readDiscreteInputs(int unitAddr,  int dataAddress, int count)** - read 1 to 2000 maximum contiguous status of discrete inputs from the attached field device with address "unitAddr". An array of booleans representing the requested data points is returned.

* **03 (0x03) readHoldingRegisters(int unitAddr,  int dataAddress, int count)** - read contents of 1 to 125 maximum contiguous block of holding registers from the attached field device with address "unitAddr". An array of int representing the requested data points (data registers on 2 bytes) is returned.

* **04 (0x04) readInputRegisters(int unitAddr,  int dataAddress, int count)** - read contents of 1 to 125 maximum contiguous block of input registers from the attached field device with address "unitAddr". An array of int representing the requested data points (data registers  on 2 bytes) is returned.

* **05 (0x05) writeSingleCoil(int unitAddr,  int dataAddress, boolean data)** -  write a single output to either ON or OFF in the attached field device with address "unitAddr".

* **06 (0x06) writeSingleRegister(int unitAddr,  int dataAddress, int data)** - write a single holding register in the attached field device with address "unitAddr".

* **15 (0x0F) writeMultipleCoils(int unitAddr,  int dataAddress, boolean[ ] data)** - write multiple coils in a sequence of coils to either ON or OFF in the attached field device with address "unitAddr".

* **16 (0x10) writeMultipleRegister(int unitAddr,  int dataAddress, int[ ] data)** - write a block of contiguous registers (1 to 123) in the attached field device with address "unitAddr".

All functions throw a **ModbusProtocolException**. Valid exceptions include:

* INVALID_CONFIGURATION

* NOT_AVAILABLE

* NOT_CONNECTED

* TRANSACTION_FAILURE

## Code Examples

The ModbusProtocolDeviceService is an OSGi declarative service referenced in the client XML definition file:
```xml
<reference bind="setModbusProtocolDeviceService"
       cardinality="1..1"
       interface="org.eclipse.kura.protocol.modbus.ModbusProtocolDeviceService"
       name="ModbusProtocolDeviceService"
       policy="static"
       unbind="unsetModbusProtocolDeviceService"/>
```
```java
public void setModbusProtocolDeviceService(ModbusProtocolDeviceService modbusService) {
  this.m_protocolDevice = modbusService;
}

public void unsetModbusProtocolDeviceService(ModbusProtocolDeviceService modbusService) {
  this.m_protocolDevice = null;
}
```
```java
if(m_protocolDevice!=null){
  m_protocolDevice.disconnect();
  m_protocolDevice.configureConnection(modbusSerialProperties);
}
```
If no exception occurs, the ModbusProtocolDevice can then be used to exchange data:
```java
boolean[] digitalInputs = m_protocolDevice.readDiscreteInputs(1, 2048, 8);
int[] analogInputs = m_protocolDevice.readInputRegisters(1, 512, 8);
boolean[] digitalOutputs = m_protocolDevice.readCoils(1, 2048, 6); // LEDS

// to set LEDS
m_protocolDevice.writeSingleCoil(1, 2047 + LED, On?TurnON:TurnOFF);
```