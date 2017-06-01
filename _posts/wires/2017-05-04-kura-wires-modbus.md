---
layout: page
title:  "Modbus application in Kura Wires"
categories: [wires]
---

This tutorial will present how to collect data from a Modbus device and publish them on a cloud platform using Kura Wires. The Modbus device will be emulated using a software simulator, like [ModbusPal](http://modbuspal.sourceforge.net/).

## Configure Modbus device

1. Download [ModbusPal](http://modbuspal.sourceforge.net/) on a computer that will act as a Modbus slave.
2. Open ModbusPal application as root and click on the "Add" button under the "Modbus Slaves" tab to create a Modbus slave device. Select an address (i.e. 1) and put a name into the "Slave name" form.
3. Click on the button with the eye to edit the slave device. Once the window is opened, add a coil with address 1 and set a value (0 or 1).
4. Close the editor and on the main window, click on the "TCP/IP" button under the "Link Settings" tab. Set the "TCP port" to 502. Be sure that the selected TCP port is opened and reachable on the system.
5. Click on "Run" button to start the device.

![modbus_pal]({{ site.baseurl }}/assets/images/wires/ModbusPal.png)

## Configure Kura Wires Modbus application

1. Install the Modbus driver from [Eclipse Kura Marketplace](https://marketplace.eclipse.org/content/esf-modbus-driver-community-edition)
2. On the Kura web interface, add the Modbus driver:
  * Under "Services", click the "+" button
  * Select "org.eclipse.kura.driver.modbus", type in a name and click "Apply": a new service will show up under Services.
  * Configure the new service as follows:
    * access.type : TCP
    * modbus.tcp-udp.ip : IP address of the system where ModbusPal is running
    * modbus.tcp-udp.port : 502
3. Click on "Wires" under "System"
4. Add a new "Timer" component and configure the interval at which the modbus slave will be sampled
5. Add a new "Asset" with the previously added Modbus driver
6. Configure the new Modbus asset, adding a new Channel with the following configuration:
    * name : a custom cool name
    * type : READ_WRITE
    * value type : BOOLEAN
    * unit.id : the modbus slave address configured in ModbusPal (i.e. 1)
    * primary.table : COILS
    * memory.address : the modbus coil address configured in ModbusPal (i.e. 1)
7. Add a new "Publisher" component and configure the chosen cloud platform stack in "cloud.service.pid" option
8. Add "Logger" component
9. Connect the "Timer" to the "Asset", and the "Asset" to the "Publisher" and "Logger".
10. Click on "Apply" and check on the logs and cloud platform that that data are correctly published.

![modbus_wires]({{ site.baseurl }}/assets/images/wires/ModbusWires.png)
