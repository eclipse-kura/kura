---
layout: page
title:  "Eclipse Kapua&trade; platform"
categories: [cloud-stacks]
---

{% include alerts.html message='The content of this document is preliminary and some links are not updated.' %}

Eclipse Kapua™ is a modular platform providing the services required to manage IoT gateways and smart edge devices. Kapua provides a core integration framework and an initial set of core IoT services including a device registry, device management services, messaging services, data management, and application enablement. More information can be found <a href="http://www.eclipse.org/kapua/" about="_blank">here</a>. This document outlines how to connect to Eclipse Kapua using the ESF Gateway Administrative Console.


## Using the Kura Gateway Administrative Console

The Kura Gateway Administrative Console exposes all services necessary for connecting to Eclipse Kapua. The reference links listed below outline each service involved in the Everyware Cloud connection. It is recommended that each section be reviewed.

- [CloudService](kura-cloud.html#cloudservice)

- [DataService](kura-cloud.html#dataservice)

- [MqttDataTransport](kura-cloud.html#mqttdatatransport)

### CloudService

The default settings for the CloudService are typically adequate for connecting to Everyware Cloud. The screen capture shown below displays the default settings for the CloudService. For details about each setting, please refer to [CloudService](kura-cloud.html#cloudservice).

![]({{ site.baseurl }}/assets/images/kapua/kapuaCloudService.png)

### DataService

The majority of default settings in the DataService can be left unchanged. A screen capture of the DataService configuration is shown below. For complete details about the DataService configuration parameters, please refer to [DataService](kura-cloud.html#dataservice).

In order for Kura to connect to Eclipse Kapua on startup, the *connect.auto-on-startup* option must be set to *true.* If this value is changed from false to true, Kura will immediately begin the connection process. It is recommended that the CloudService and MqttDataTransport are configured before setting the *connect.auto-on-startup* option to true.

{% include alerts.html message='Changing the value of *connect.auto-on-startup* from true to false **will not** disconnect the client from the broker. This setting simply implies that Kura will not automatically connect on the next start of Kura.' %}

![]({{ site.baseurl }}/assets/images/kapua/kapuaDataService.png)

### MqttDataTransport

While the majority of default settings in the MqttDataTransport can be left unchanged, the following parameters must be modified:

- **broker-url** - defines the MQTT broker URL that was provided when the Eclipse Kapua account was established. Information on how to obtain the broker URL can be found [here](????).

- **topic.context.account-name** - defines the account name of the account to which the device is attempting to connect.

- **username** - identifies the user to be used when creating the connection.

For complete details about the MqttDataTransport configuration parameters, please refer to [MqttDataTransport](kura-cloud.html#mqttdatatransport).

![]({{ site.baseurl }}/assets/images/kapua/KapuaMQTTDataTransport.png)

## Connect/Disconnect

The status panel can be used to manually connect or disconnect the client while Kura is running. The main button toolbar has a connect and disconnect button that may be used to control connectivity.

{% include alerts.html message='Connecting or disconnecting the client via the status panel has no impact on Kura automatically connecting at startup. This capability is only controlled via the *connect.auto-on-startup* DataService setting.' %}
