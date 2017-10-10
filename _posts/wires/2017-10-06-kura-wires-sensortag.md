---
layout: page
title:  "TI SensorTag driver in Kura Wires"
categories: [wires]
---

Eclipse Kura provides a specific driver that can be used to interact with TI SensorTag devices. The driver is available only for gateways that support the new Kura BLE APIs.

This tutorial will explain how to configure a Wire graph that connects with a SensorTag, reads sensor values and publishes data the a cloud platform.

## Configure Kura Wires TI SensorTag application

1. Install the TI SensorTag driver from the [Eclipse Kura Marketplace](https://marketplace.eclipse.org/content/???)
2. On the Kura web interface, instantiate a SensorTag Driver:
  * Under "System", select "Drivers and Assets" and click on the "New Driver" button.
  * Select "org.eclipse.kura.driver.ble.sensortag" as "Driver Factory", type a name in to "Driver Name" and click "Apply": a new driver will be instantiated and shown up under the "Drivers and Assets" tab.
  * Configure the new driver setting the bluetooth interface name (i.e. hci0).

![sensortag_driver]({{ site.baseurl }}/assets/images/wires/SensorTagDriver.png)

{:start="3"}
3. From the "Drivers and Assets" tab, add a new asset binded to the SensorTag driver:
  * Click on the "New Asset" button and fill the form with the "Asset Name" and selecting the driver created at point 2. as "Driver Name". Click "Apply" and a new asset will be listed under the SensorTag driver.

  ![sensortag_asset]({{ site.baseurl }}/assets/images/wires/SensorTagAsset.png)

  * Click on the new asset and configure it, adding the channels. Each channel represents a single sensor on the SensorTag and can be choosen from the "sensor.name" menu. Fill the "sensortag.address" with the DB address of the SensorTag you want to connect to. The "value.type" should be set to double, but also the other choices are possible.
  * Click "Apply".

  ![sensortag_asset_config]({{ site.baseurl }}/assets/images/wires/SensorTagAssetConfig.png)

{:start="4"}
4. Click on "Wires" under "System".
5. Add a new "Timer" component and configure the interval at which the sensors will be sampled.
6. Add a new "Asset" with the previously added SensorTag asset.
7. Add a new "Publisher" component and configure the chosen cloud platform stack in "cloud.service.pid" option
8. Add "Logger" component
9. Connect the "Timer" to the "Asset", and the "Asset" to the "Publisher" and "Logger".
10. Click on "Apply" and check on the logs and cloud platform that that data are correctly published.

![sensortag_wires]({{ site.baseurl }}/assets/images/wires/SensorTagWires.png)
