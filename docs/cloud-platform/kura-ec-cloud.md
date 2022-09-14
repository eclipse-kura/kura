# Eurotech Everyware Cloud&trade; platform

Everyware Cloud provides an easy mechanism for connecting cloud-ready devices to IT systems and/or applications; therefore, connecting to Everyware Cloud is an important step in creating and maintaining a complete M2M application. Information on Everyware Cloud and its features can be found [here](https://www.eurotech.com/edge-software/#iot-edge-management-platform). This document outlines how to connect to Everyware Cloud using the Kura Gateway Administrative Console.

## Using the Kura Gateway Administrative Console

The Kura Gateway Administrative Console exposes all services necessary for connecting to Everyware Cloud. The reference links listed below outline each service involved in the Everyware Cloud connection. It is recommended that each section be reviewed.

- [CloudService](#cloudservice)
- [DataService](}#dataservice)
- [MqttDataTransport](#mqttdatatransport)

### CloudService

The default settings for the CloudService are typically adequate for connecting to Everyware Cloud. The screen capture shown below displays the default settings for the CloudService. For details about each setting, please refer to [CloudService](#cloudservice).

![](images/cloudService.png)

!!! warning
    The "*Simple JSON*" payload encoding is not supported by Everyware Cloud. Use the default "*Kura Protobuf*" encoding instead.

### DataService

The majority of default settings in the DataService can be left unchanged. A screen capture of the DataService configuration is shown below. For complete details about the DataService configuration parameters, please refer to [DataService](#dataservice).

In order for Kura to connect to Everyware Cloud on startup, the *connect.auto-on-startup* option must be set to `true`. If this value is changed from `false` to `true`, Kura will immediately begin the connection process. It is recommended that the CloudService and MqttDataTransport are configured before setting the *connect.auto-on-startup* option to `true`.

!!! note
    Changing the value of *connect.auto-on-startup* from `true` to `false` **will not** disconnect the client from the broker. This setting simply implies that Kura will not automatically connect on the next start of Kura.

![](images/dataService.png)

### MqttDataTransport

While the majority of default settings in the MqttDataTransport can be left unchanged, the following parameters must be modified:

- **broker-url** - defines the MQTT broker URL that was provided when the Eurotech Everyware Cloud account was established. Information on how to obtain the broker URL can be found [here](http://everywarecloud.eurotech.com/doc/ECDevGuide/latest/2.02-Managing-Cloud-Users.asp). In the MqttDataTransport configuration screen capture shown below, the *broker-url* is `mqtt://broker-sbx.everyware.io:1883`
- **topic.context.account-name** - defines the account name of the account to which the device is attempting to connect. In the MqttDataTransport configuration screen capture shown below, the *account name* is `account-name`
- **username** - identifies the user to be used when creating the connection. In the MqttDataTransport configuration screen capture shown below, the *username* is `username`.

!!! note
    When connecting to Everyware Cloud, the *username* must have proper permissions. Information on users and permissions can be found [here](http://everywarecloud.eurotech.com/doc/ECDevGuide/latest/2.02-Managing-Cloud-Users.asp).

For complete details about the MqttDataTransport configuration parameters, please refer to [MqttDataTransport](#mqttdatatransport).

![](images/dataTransport.png)

## Connect/Disconnect

The status panel can be used to manually connect or disconnect the client while Kura is running. The main button toolbar has a connect and disconnect button that may be used to control connectivity.

!!! note
    Connecting or disconnecting the client via the status panel has no impact on Kura automatically connecting at startup. This capability is only controlled via the *connect.auto-on-startup* DataService setting.
