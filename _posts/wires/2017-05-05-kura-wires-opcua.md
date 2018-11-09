---
layout: page
title:  "OPC/UA application in Kura Wires"
categories: [wires]
---

This tutorial will present how to collect data from a OPC/UA device and publish them on a cloud platform using Kura Wires. The OPC/UA server device will be emulated using a bundle running on Kura.

## Configure OPC/UA server simulator

1. Download the [OPC/UA server simulator](https://s3.amazonaws.com/kura-resources/opcua_demo_server.dp) bundle and install it on Kura. It will create a simulated OPC/UA server that exposes some sensors (light, temperature and water sensor) and some actuators (buzzer, led and fan).
2. On the Kura web interface, select "OPCUA Server demo" under "Services" and set "server.port" to 1234. Click "Apply" button. This will start a OPCUA server on port 1234.

## Configure Kura Wires OPC/UA application

1. Install the OPC/UA driver from the Eclipse Kura Marketplace ([here](https://marketplace.eclipse.org/content/opc-ua-driver-eclipse-kura-3xy) or [here](https://marketplace.eclipse.org/content/opc-ua-driver-eclipse-kura-4xy))
2. On the Kura web interface, add the OPC/UA driver:
  * Under "Services", click the "+" button
  * Select "org.eclipse.kura.driver.opcua", type in a name and click "Apply": a new service will show up under Services.
  * Configure the new service as follows:
    * endpoint.ip : localhost
    * endpoint.port : 1234
    * server.name : leave blank

![opcua_driver]({{ site.baseurl }}/assets/images/wires/OPCUADriver.png)

{:start="3"}
3. Click on "Wires" under "System"
4. Add a new "Timer" component and configure the interval at which the OPC/UA server will be sampled
5. Add a new "Asset" with the previously added OPC/UA driver
6. Configure the new OPC/UA asset, adding new Channels as shown in the following image.

![opcua_driver_config]({{ site.baseurl }}/assets/images/wires/OPCUADriverConfig.png)

{:start="7"}
7. Add a new "Publisher" component and configure the chosen cloud platform stack in "cloud.service.pid" option
8. Add "Logger" component
9. Connect the "Timer" to the "Asset", and the "Asset" to the "Publisher" and "Logger".
10. Click on "Apply" and check on the logs and cloud platform that that data are correctly published.

![opcua_wires]({{ site.baseurl }}/assets/images/wires/OPCUAWires.png)
