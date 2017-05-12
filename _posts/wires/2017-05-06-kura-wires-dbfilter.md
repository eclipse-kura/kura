---
layout: page
title:  "How to use DB Store and Filter components in Kura Wires"
categories: [wires]
---

This tutorial will present how to use DB Store and DB Filter components in Kura Wires using the OPC/UA simulated server already used in [OPC/UA application in Kura Wires](kura-wires-opcua.html).

The DB Store component allows the wire graphs to interact with the database installed by Kura. It stores in a user-defined table all the envelops received by the component. The component can be configured as follows:

* table.name : the name of the table to be created.
* maximum.table.size : the size of the table.
* cleanup.records.keep : the number of records in the table to keep while performing a cleanup operation.

The DB Filter component, instead, can run a custom SQL query on the Kura database. It can be configured as follows:

* sql.view : SQL to be executed to build a view.
* cache.expiration.interval : cache validity in seconds. When cache expires, it will cause a new read in the database.

The following procedure will create a wire graph that collects data from a simulated OPC/UA server, stores them in table using the DB Store component and publishes them on the cloud platform. Moreover, the DB Filter is used to read from the database and write data to the OPC/UA server based on the read values.

## Configure OPC/UA server simulator

1. Download the [OPC/UA server simulator](http://???) bundle and install it on Kura. It will create a simulated OPC/UA server that exposes some sensors (light, temperature and water sensor) and some actuators (buzzer, led and fan).
2. On the Kura web interface, select "OPCUA Server demo" under "Services" and set "server.port" to 1234. Click "Apply" button. This will start a OPCUA server on port 1234.

## Configure Kura Wires OPC/UA application

1. Install the OPC/UA driver from [Eclipse Kura Marketplace](https://marketplace.eclipse.org/content/opc-ua-driver)
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
6. Configure the new OPC/UA asset, adding new Channels as shown in the following image. Be sure that all channel are set to READ.

![opcua_driver_config1]({{ site.baseurl }}/assets/images/wires/OPCUADriverConfig1.png)

{:start="7"}
7. Add a new "DBStore" component and configure it as follows:
  * table.name : WR_data
  * maximum.table.size : 10000
  * cleanup.records.keep : 0
8. Add a new "Publisher" component and configure the chosen cloud platform stack in "cloud.service.pid" option
9. Add "Logger" component
10. Add another instance of "Timer"
11. Add a new "DBFilter" component and configure as follows. The query will get the values from the light sensor and if they are less than 200, the fan is activated.
  * sql.view : SELECT (CASE WHEN "light" < 200 THEN 1 ELSE 0 END) AS "led" FROM "WR_data" ORDER BY TIMESTAMP DESC LIMIT 1;
  * cache.expiration.interval : 0
12. Add another "Asset" with the OPC/UA driver, configured as shown in the following image. Be sure that all channel are set to WRITE.

![opcua_driver_config2]({{ site.baseurl }}/assets/images/wires/OPCUADriverConfig2.png)

{:start="13"}
13. Connect the components ad shown in the following image, then click on "Apply" and check on the logs and cloud platform that that data are correctly published.

![opcua_wires_db]({{ site.baseurl }}/assets/images/wires/OPCUAWiresDB.png)
