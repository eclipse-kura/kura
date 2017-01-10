Eclipse Kura can connect to the Azure IoT Hub using the MQTT protocol. When doing so, Kura applications can send device-to-cloud and receive cloud-to-device messages. More information on the Azure IoT Hub and its support for the MQTT protocol can be found <a href="https://docs.microsoft.com/en-us/azure/iot-hub/iot-hub-mqtt-support" about="_blank">here</a>. This document outlines how to configure and connect a Kura application to the Azure IoT Hub.

# Configuring a Kura Cloud Stack for Azure IoT Hub

The Kura Gateway Administrative Console exposes all services necessary to configure a connection to the Azure IoT Hub.
You can follow the steps outlined below to configure the connection to the Azure IoT Hub.

The first step is to create a new Kura Cloud stack. From the Kura Gateway Administrative Console:

- Select *Cloud Services* in the navigation on the left and click *New* to create a new Cloud stack
- In the dialog, select `org.eclipse.kura.cloud.CloudService` as the Cloud Service Factory
- Enter a *Cloud Service Pid* name like `org.eclipse.kura.cloud.CloudService-Azure`
- Press the *Create* button to create the new Cloud stack

Now review and update the configuration of each Kura Cloud stack component as outline below.

- [MqttDataTransport](doc:built-in-services#section-mqttdatatransport)

- [DataService](doc:built-in-services#section-dataservice)

- [CloudService](doc:built-in-services#section-cloudservice)



## MqttDataTransport

Make sure to review the Azure documentation on how to connect to the Azure IoT Hub using the MQTT protocol.
You will need the hostname of the Azure IoT Hub, referred below as `{iothubhostname}`, the Id and the SAS Token of the device, referred as `{device_id}` and `{device_SAS_token}`.
Modify the service configuration parameters as follows:

- **broker-url** - defines the URL of the Azure IoT MQTT broker. The URL value should be set as `mqtts://{iothubhostname}:8883/`
[block:callout]
{
  "type": "warning",
  "body": "An SSL connection (mqtts on port 8883) is required to connect to Azure IoT Hub."
}
[/block]

- **topic.context.account-name** - insert `devices` as the MQTT topic prefix for the device-to-cloud and cloud-to-device messages

- **username** - insert `{iothubhostname}/{device_id}` as username for the MQTT connection

- **password** - insert `{device_SAS_token}` as password for the MQTT connection
[block:callout]
{
  "type": "Info",
  "body": "The format of the SAS Token is like `SharedAccessSignature sig={signature-string}&se={expiry}&sr={URL-encoded-resourceURI}`."
}
[/block]

- **client-id** - insert `{device_id}` as Client ID for the MQTT connection

- **clean-session** - make sure it is set to `true`

- **lwt.topic** - set the Will Topic to something `#account-name/#client-id/messages/events/LWT`

You can keep the default values of the remaining parameters,
 so save your changes by clicking the *Apply* button.
 A screen capture of the MqttDataTransport configuration is shown below. For complete details about the MqttDataTransport configuration parameters, please refer to [MqttDataTransport](doc:built-in-services#section-mqttdatatransport).

## DataService

The majority of default settings in the DataService can be left unchanged. A screen capture of the DataService configuration is shown below. For complete details about the DataService configuration parameters, please refer to [DataService](doc:built-in-services#section-dataservice).

In order for Kura to connect to Azure IoT Hub on startup, the *connect.auto-on-startup* option must be set to *true.* If this value is changed from false to true, Kura will immediately begin the connection process. It is recommended that the CloudService and MqttDataTransport are configured before setting the *connect.auto-on-startup* option to true.
[block:callout]
{
  "type": "warning",
  "body": "Changing the value of *connect.auto-on-startup* from true to false **will not** disconnect the client from the broker. This setting simply implies that Kura will not automatically connect on the next start of Kura."
}
[/block]

[block:image]
{
  "images": [
    {
      "image": [
        "https://files.readme.io/WGGRAdp7SiaPjmiC6K68_dataservice.png",
        "dataservice.png",
        "1299",
        "937",
        "#1e99d7",
        ""
      ]
    }
  ]
}
[/block]

## CloudService

The default settings for the CloudService should be modified as follow to allow a connection to Azure IoT Hub .

- **topic.control-prefix** - insert `devices` as the MQTT topic prefix for the device-to-cloud and cloud-to-device messages

- **encode.gzip** - should be set `false` to avoid compression of the message payloads

- **republish.mqtt.birth.cert.on.gps.lock** - should be set `false` to avoid sending additional messages on GPS position lock

- **republish.mqtt.birth.cert.on.modem.detect** - should be set `false` to avoid sending additional messages on cellular modem update

- **disable.default.subscriptions** - should be set `false` to avoid subscriptions on Kura control topics for cloud-to-device

- **disable.republish.birth.cert.on.reconnect** - should be set `false` to avoid sending additional device profile messages on MQTT connect

The screen capture shown below displays the default settings for the CloudService. For details about each setting, please refer to [CloudService](doc:built-in-services#section-cloudservice).
[block:image]
{
  "images": [
    {
      "image": [
        "https://files.readme.io/NFlpo4xR8Whxe2Z7P9sr_cloudservice.png",
        "cloudservice.png",
        "1307",
        "941",
        "#1798d8",
        ""
      ]
    }
  ]
}
[/block]


# Connect/Disconnect

The status panel can be used to manually connect or disconnect the client while Kura is running. The main button toolbar has a connect and disconnect button that may be used to control connectivity.
[block:callout]
{
  "type": "warning",
  "body": "Connecting or disconnecting the client via the status panel has no impact on Kura automatically connecting at startup. This capability is only controlled via the *connect.auto-on-startup* DataService setting."
}
[/block]


# Kura Application Connecting to Azure IoT Hub
