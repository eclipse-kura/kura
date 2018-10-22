---
layout: page
title:  "Eurotech Everyware Cloud&trade; platform"
categories: [cloud-stacks]
---

Everyware Cloud provides an easy mechanism for connecting cloud-ready devices to IT systems and/or applications; therefore, connecting to Everyware Cloud is an important step in creating and maintaining a complete M2M application. Information on Everyware Cloud and its features can be found <a href="http://www.eurotech.com/en/products/software+services/everyware+cloud+m2m+platform/m2m+what+it+is" about="_blank">here</a>. This document outlines how to connect to Everyware Cloud using the Kura Gateway Administrative Console.

## Using the Kura Gateway Administrative Console

The Kura Gateway Administrative Console exposes all services necessary for connecting to Everyware Cloud. The reference links listed below outline each service involved in the Everyware Cloud connection. It is recommended that each section be reviewed.

- [CloudService]({{ site.baseurl }}/cloud-api/5-stack-components.html#cloudservice)

- [DataService]({{ site.baseurl }}/cloud-api/5-stack-components.html#dataservice)

- [MqttDataTransport]({{ site.baseurl }}/cloud-api/5-stack-components.html#mqttdatatransport)

### CloudService

The default settings for the CloudService are typically adequate for connecting to Everyware Cloud. The screen capture shown below displays the default settings for the CloudService. For details about each setting, please refer to [CloudService]({{ site.baseurl }}/cloud-api/5-stack-components.html#cloudservice).

![]({{ site.baseurl }}/assets/images/cloud/cloudService.png)

### DataService

The majority of default settings in the DataService can be left unchanged. A screen capture of the DataService configuration is shown below. For complete details about the DataService configuration parameters, please refer to [DataService]({{ site.baseurl }}/cloud-api/5-stack-components.html#dataservice).

In order for Kura to connect to Everyware Cloud on startup, the *connect.auto-on-startup* option must be set to *true.* If this value is changed from false to true, Kura will immediately begin the connection process. It is recommended that the CloudService and MqttDataTransport are configured before setting the *connect.auto-on-startup* option to true.

{% include alerts.html message='Changing the value of *connect.auto-on-startup* from true to false **will not** disconnect the client from the broker. This setting simply implies that Kura will not automatically connect on the next start of Kura.' %}

![]({{ site.baseurl }}/assets/images/cloud/ECDataService.png)

### MqttDataTransport

While the majority of default settings in the MqttDataTransport can be left unchanged, the following parameters must be modified:

- **broker-url** - defines the MQTT broker URL that was provided when the Eurotech Everyware Cloud account was established. Information on how to obtain the broker URL can be found [here](http://everywarecloud.eurotech.com/doc/ECDevGuide/latest/2.02-Managing-Cloud-Users.asp). In the MqttDataTransport configuration screen capture shown below, the *broker-url* is **mqtts://broker-sandbox.everyware-cloud.com:8883/** or **mqtt://broker-sandbox.everyware-cloud.com:1883/**

- **topic.context.account-name** - defines the account name of the account to which the device is attempting to connect. In the MqttDataTransport configuration screen capture shown below, the *account name* is **ec-account-name**.

- **username** - identifies the user to be used when creating the connection. In the MqttDataTransport configuration screen capture shown below, the *username* is **ec-account-name-broker**.

{% include alerts.html message='When connecting to Everyware Cloud, the *username* must have proper permissions. Information on users and permissions can be found [here](http://everywarecloud.eurotech.com/doc/ECDevGuide/latest/2.02-Managing-Cloud-Users.asp).' %}

For complete details about the MqttDataTransport configuration parameters, please refer to [MqttDataTransport]({{ site.baseurl }}/cloud-api/5-stack-components.html#mqttdatatransport).

![]({{ site.baseurl }}/assets/images/cloud/ECMqttDataTransport.png)

## Connect/Disconnect

The status panel can be used to manually connect or disconnect the client while Kura is running. The main button toolbar has a connect and disconnect button that may be used to control connectivity.

{% include alerts.html message='Connecting or disconnecting the client via the status panel has no impact on Kura automatically connecting at startup. This capability is only controlled via the *connect.auto-on-startup* DataService setting.' %}
