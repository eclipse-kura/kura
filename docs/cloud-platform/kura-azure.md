# Azure IoT Hub&trade; platform

Starting from release 3.0, Eclipse Kura can connect to the Azure IoT Hub using the MQTT protocol. When doing so, Kura applications can send device-to-cloud messages. More information on the Azure IoT Hub and its support for the MQTT protocol can be found [here](https://docs.microsoft.com/en-us/azure/iot-hub/iot-hub-mqtt-support). This document outlines how to configure and connect a Kura application to the Azure IoT Hub.

## Get Azure IoT Hub information

In order to properly configure Kura to connect to IoT Hub, some information are needed. You will need the hostname of the Azure IoT Hub, referred below as `{iothubhostname}`, the Id and the SAS Token of the device, referred as `{device_id}` and `{device_SAS_token}`.
The hostname is listed on the "Overview" tab on the IoT Hub main page, while the device ID is shown on the "Device Explorer" tab. Finally, the SAS token can be generated using the iothub-explorer application that can be found [here](https://github.com/Azure/iothub-explorer). To install the application, type on a shell:

```bash
npm install -g iothub-explorer
```

Then start a new session on your IoT Hub instance (it will expire in 1 hour):

```bash
iothub-explorer login "{your-connection-string}"
```

where `{your-connection-string}` is the connection string of your IoT Hub instance. It can be found on the "Shared access policies" tab under "Settings". Select the "iothubowner" policy and a tab will appear with the "Connection string—primary key" option.
Then list your devices:

```bash
iothub-explorer list
```

and get the SAS token for the `{device-name}` device:

```bash
iothub-explorer sas-token {device-name}
```

Be aware that the SAS token will expire in 1 hour by default, but using "-d" option it is possible to set a custom expiration time.

### SSL certificates

In order to connect to your IoT Hub instance, Kura should trust the remote broker through a SSL certificate. The simpler way to get the IotHub certificate is to run the following command on a shell:

```bash
openssl s_client -showcerts -tls1 -connect {iothubhostname}:8883
```

The result is the SSL certificate chain. Copy **all** the certificates in the format:

```
-----BEGIN CERTIFICATE-----
...
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
...
-----END CERTIFICATE-----
```

and paste them in to the "Server SSL Certificate" tab under "Settings" in Kura. Then click the Apply button and restart Kura to update the keystore.

## Configuring a Kura Cloud Stack for Azure IoT Hub

The Kura Gateway Administrative Console exposes all services necessary to configure a connection to the Azure IoT Hub. You can follow the steps outlined below to configure the connection to the Azure IoT Hub.

The first step is to create a new Kura Cloud stack. From the Kura Gateway Administrative Console:

- Select *Cloud Connections* in the navigation on the left and click *New Connection* to create a new Cloud connection
- In the dialog, select `org.eclipse.kura.cloud.CloudService` as the *Cloud Connection Factory PID*
- Enter a *Cloud Connection Service PID* name like `org.eclipse.kura.cloud.CloudService-Azure`
- Press the *Create* button to create the new Cloud stack

Now review and update the configuration of each Kura Cloud stack component as outline below.

- MqttDataTransport
- DataService
- CloudService

### MqttDataTransport

Modify the service configuration parameters as follows:

- **broker-url** - defines the URL of the Azure IoT MQTT broker. The URL value should be set as `mqtts://{iothubhostname}:8883/`

    !!! note
        An SSL connection (mqtts on port 8883) is required to connect to Azure IoT Hub&trade;.

- **topic.context.account-name** - insert `devices` as the MQTT topic prefix for the device-to-cloud and cloud-to-device messages
- **username** - insert `{iothubhostname}/{device_id}` as username for the MQTT connection
- **password** - insert `{device_SAS_token}` as password for the MQTT connection

    !!! note
        The format of the SAS Token is like:
        ```
        SharedAccessSignature sig={signature-string}&se={expiry}&sr={URL-encoded-resourceURI}
        ```

- **client-id** - insert `{device_id}` as Client ID for the MQTT connection
- **clean-session** - make sure it is set to `true`
- **lwt.topic** - set the Will Topic to something `#account-name/#client-id/messages/events/LWT`

You can keep the default values of the remaining parameters, so save your changes by clicking the *Apply* button. A screen capture of the MqttDataTransport configuration is shown below.

### DataService

The majority of default settings in the DataService can be left unchanged. A screen capture of the DataService configuration is shown below.

In order for Kura to connect to Azure IoT Hub on startup, the *connect.auto-on-startup* option must be set to *true.* If this value is changed from false to true, Kura will immediately begin the connection process. It is recommended that the CloudService and MqttDataTransport are configured before setting the *connect.auto-on-startup* option to true.

!!! note
    Changing the value of *connect.auto-on-startup* from true to false **will not** disconnect the client from the broker. This setting simply implies that Kura will not automatically connect on the next start of Kura.

![data_service](images/azureDataService.png)

## CloudService

The default settings for the CloudService should be modified as follow to allow a connection to Azure IoT Hub .

- **topic.control-prefix** - insert `devices` as the MQTT topic prefix for the device-to-cloud and cloud-to-device messages
- **encode.gzip** - should be set `false` to avoid compression of the message payloads
- **republish.mqtt.birth.cert.on.gps.lock** - should be set `false` to avoid sending additional messages on GPS position lock
- **republish.mqtt.birth.cert.on.modem.detect** - should be set `false` to avoid sending additional messages on cellular modem update
- **enable.default.subscriptions** - should be set `false` to avoid subscriptions on Kura control topics for cloud-to-device
- **payload.encoding** - should be set to `Simple JSON`

The screen capture shown below displays the default settings for the CloudService.

![cloud_service](images/azureCloudService.png)

## How to connect and disconnect from the cloud platform

The status panel can be used to manually connect or disconnect the client while Kura is running. The main button toolbar has a connect and disconnect button that may be used to control connectivity.

!!! note
    Connecting or disconnecting the client via the status panel has no impact on Kura automatically connecting at startup. This capability is only controlled via the *connect.auto-on-startup* DataService setting.

## Kura Application Connecting to Azure IoT Hub

The Kura example publisher can be used to publish to the IoT Hub. The example configuration should be modified as follows.

- **cloud.service.pid** - insert the name of the new Kura Cloud stack, `org.eclipse.kura.cloud.CloudService-Azure` in this tutorial
- **app.id** - insert `messages` as application id
- **publish.appTopic** - insert `events/` as publish topic

This configuration allows the publication on the default `messages/events` endpoint on the IoT Hub&trade;.
