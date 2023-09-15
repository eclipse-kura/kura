# Cloud Service Configuration

The **CloudService** provides an easy-to-use API layer for the M2M application to communicate with a remote server. It operates as a decorator for the **DataService**, providing add-on features over the management of the **DataTransport** layer.

In addition to simple publish/subscribe, the Cloud Connection API simplifies the implementation of more complex interaction flows like request/response or remote resource management. The Cloud Connection abstracts the developers from the complexity of the transport protocol and payload format used in the communication.

The Cloud Connection allows a single connection to a remote server to be shared across more than one application in the gateway, providing the necessary topic partitioning. Its functions include:

- Adds application topic prefixes to allow a single remote server connection to be shared across applications.

- Defines a payload data model and provides default encoding/decoding serializers.

- Publishes life-cycle messages when the device and applications start and stop.

To use this service, select the **CloudService** option located in the **Cloud Services** area as shown in the screen capture below.

![Cloud Service](./images/cloud-service.png)

The **CloudService** provides the following configuration parameters:

- **device.display-name**: defines the device display name given by the system. (Required field).

- **device.custom-name**: defines the custom device display name if the _device.display-name_ parameter is set to "Custom".

- **topic.control-prefix**: defines the topic prefix used for system and device management messages.

- **encode.gzip**: defines if the message payloads are sent compressed.

- **republish.mqtt.birth.cert.on.gps.lock**: when set to true, forces a republish of the MQTT Birth Certificate when a GPS correct position lock is received. The device is then registered with its real coordinates. (Required field).

- **republish.mqtt.birth.cert.on.modem.detect**: when set to true, forces a republish of the MQTT Birth Certificate when the service receives a modem detection event. (Required field).

- **enable.default.subscriptions**: manages the default subscriptions to the gateway management MQTT topics. When disabled, the gateway will not be remotely manageable.

- **payload.encoding**: specifies the encoding for the messages sent by the specific CloudService instance. 
    - **Kura Protobuf** - when this option is selected, the Kura Protobuf encoding will be used
    - **Simple JSON** - the simple JSON encoding will be used instead. More information is available [here](https://github.com/eclipse/kapua/wiki/K-Payload-JSON-Format). An example below.

```json
{
"sentOn" : 1491298822,
"position" : {
    "latitude" : 45.234,
    "longitude" : -7.3456,
    "altitude" : 1.0,
    "heading" : 5.4,
    "precision" : 0.1,
    "speed" : 23.5,
    "timestamp" : 1191292288,
    "satellites" : 3,
    "status" : 2
},
"metrics": {
    "code" : "A23D44567Q",
    "distance" : 0.26456E+4,
    "temperature" : 27.5,
    "count" : 12354,
    "timestamp" : 23412334545,
    "enable" : true,
    "rawBuffer" : "cGlwcG8gcGx1dG8gcGFwZXJpbm8="
},
"body": "UGlwcG8sIHBsdXRvLCBwYXBlcmlubywgcXVpLCBxdW8gZSBxdWEu"
}
```

The default CloudService implementations publishes the following [lifecycle messages](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.core.cloud/src/main/java/org/eclipse/kura/core/cloud/LifecycleMessage.java):

1. [BIRTH message](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/message/KuraBirthPayload.java): sent immediately when device is connected to the cloud platform;
2. [DISCONNECT message](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/message/KuraDisconnectPayload.java): sent immediately before device is disconnected from the cloud platform;
3. delayed [BIRTH message](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.api/src/main/java/org/eclipse/kura/message/KuraBirthPayload.java): sent when new cloud application handler becomes available, a DP is installed or removed, GPS position is locked (can be disabled), or when modem status changes (can be disabled). These messages are cached for 30 seconds before sending. If no other message of such type arrives the message is sent; otherwise the BIRTH is cached and the timeout restarts. This is to avoid sending multiple messages when the framework starts.