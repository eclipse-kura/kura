# Eclipse Kapua&trade; platform

Eclipse Kapua™ is a modular platform providing the services required to manage IoT gateways and smart edge devices. Kapua provides a core integration framework and an initial set of core IoT services including a device registry, device management services, messaging services, data management, and application enablement. More information can be found [here](https://www.eclipse.org/kapua/). This document outlines how to connect to Eclipse Kapua using the Kura Gateway Administrative Console.

## Using the Kura Gateway Administrative Console

The Kura Gateway Administrative Console exposes all services necessary for connecting to Eclipse Kapua. The reference links listed below outline each service involved in the cloud connection. It is recommended that each section be reviewed.

- [CloudService](#cloudservice)

- [DataService](#dataservice)

- [MqttDataTransport](#mqttdatatransport)

### CloudService

The default settings for the CloudService are typically adequate for connecting to a Kapua instance. The screen capture shown below displays the default settings for the CloudService. For details about each setting, please refer to [CloudService](#cloudservice).

![](images/cloudService.png)

!!! warning
    The "*Simple JSON*" payload encoding is not supported by Kapua. Use the default "*Kura Protobuf*" encoding instead.

### DataService

The majority of default settings in the DataService can be left unchanged. A screen capture of the DataService configuration is shown below. For complete details about the DataService configuration parameters, please refer to [DataService](#dataservice).

In order for Kura to connect to Eclipse Kapua on startup, the *connect.auto-on-startup* option must be set to `true`. If this value is changed from `false` to `true`, Kura will immediately begin the connection process. It is recommended that the CloudService and MqttDataTransport are configured before setting the *connect.auto-on-startup* option to `true`.

!!! note
    Changing the value of *connect.auto-on-startup* from `true` to `false` **will not** disconnect the client from the broker. This setting simply implies that Kura will not automatically connect on the next start of Kura.

![](images/dataService.png)

### MqttDataTransport

While the majority of default settings in the MqttDataTransport can be left unchanged, the following parameters must be modified:

- **broker-url** - defines the MQTT broker URL that was provided when the Kapua account was established.
- **topic.context.account-name** - defines the account name of the account to which the device is attempting to connect. In the MqttDataTransport configuration screen capture shown below, the *account name* is `account-name`
- **username** - identifies the user to be used when creating the connection. In the MqttDataTransport configuration screen capture shown below, the *username* is `username`.

For complete details about the MqttDataTransport configuration parameters, please refer to [MqttDataTransport](#mqttdatatransport).

![](images/dataTransport.png)

## Connect/Disconnect

The status panel can be used to manually connect or disconnect the client while Kura is running. The main button toolbar has a connect and disconnect button that may be used to control connectivity.

!!! note
    Connecting or disconnecting the client via the status panel has no impact on Kura automatically connecting at startup. This capability is only controlled via the *connect.auto-on-startup* DataService setting.
