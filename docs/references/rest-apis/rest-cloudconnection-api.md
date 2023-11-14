# Rest CloudConnection v1 API
!!! note

    This API can also be accessed via the RequestHandler with app-id: `CC-V1`.


The `CloudConnectionRestService` APIs provides methods to manage Cloud Endpoints and their managed components.
Identities with `rest.cloudconnection` permissions can access these APIs.

## GET methods

#### Find Cloud Component Instances

- Description: This method returns all the Cloud Component instances, including [CloudEndpoint](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/CloudEndpoint.html), [CloudConnectionManager](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/CloudConnectionManager.html), [CloudService](https://download.eclipse.org/kura/docs/api/<version/apidocs/org/eclipse/kura/cloud/CloudService.html) [CloudPublisher](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/publisher/CloudPublisher.html), [CloudSubscriber](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/subscriber/CloudSubscriber.html)
- Method: GET
- API PATH: `services/cloudconnection/v1/instances`


##### Responses
- 200 Ok Status
```JSON
{
    "cloudEndpointInstances": [
        {
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloud.CloudService",
            "cloudEndpointPid": "org.eclipse.kura.cloud.CloudService",
            "state": "DISCONNECTED",
            "cloudEndpointType": "CLOUD_CONNECTION_MANAGER"
        },
        {
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloud.CloudService-todelete",
            "cloudEndpointPid": "org.eclipse.kura.cloud.CloudService",
            "state": "DISCONNECTED",
            "cloudEndpointType": "CLOUD_CONNECTION_MANAGER"
        },
        {
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloud.CloudService-2",
            "cloudEndpointPid": "org.eclipse.kura.cloud.CloudService",
            "state": "DISCONNECTED",
            "cloudEndpointType": "CLOUD_CONNECTION_MANAGER"
        },
        {
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloud.CloudService-3",
            "cloudEndpointPid": "org.eclipse.kura.cloud.CloudService",
            "state": "DISCONNECTED",
            "cloudEndpointType": "CLOUD_CONNECTION_MANAGER"
        },
        {
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloud.CloudService-test",
            "cloudEndpointPid": "org.eclipse.kura.cloud.CloudService",
            "state": "DISCONNECTED",
            "cloudEndpointType": "CLOUD_CONNECTION_MANAGER"
        }
    ],
    "pubsubInstances": [
        {
            "cloudEndpointPid": "org.eclipse.kura.cloud.CloudService",
            "pid": "testPub",
            "factoryPid": "org.eclipse.kura.cloud.publisher.CloudPublisher",
            "type": "PUBLISHER"
        },
        {
            "cloudEndpointPid": "org.eclipse.kura.cloud.CloudService",
            "pid": "testPub",
            "factoryPid": "org.eclipse.kura.cloud.publisher.CloudPublisher",
            "type": "SUBSCRIBER"
        }
    ]
}
```
  - `cloudEndpointType` CLOUD_ENDPOINT if the component implements [CloudEndpoint](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/CloudEndpoint.html), CLOUD_CONNECTION_MANAGER otherwise.
- 500 Internal Server Error

#### Get CloudComponent Factories

- Description: This method returns all the Factories able to create Component for [CloudEndpoint](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/CloudEndpoint.html), including [CloudConnectionFactory](), [CloudServiceFactor](), [CloudPublisher](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/publisher/CloudPublisher.html), [CloudSubscriber](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/subscriber/CloudSubscriber.html)
- Method: GET
- API PATH: `services/cloudconnection/v1/factories`

##### Responses

- 200 Ok Status
```JSON
{
    "cloudConnectionFactories": [
        {
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloud.CloudService",
            "defaultCloudEndpointPid": "org.eclipse.kura.cloud.CloudService-2",
            "cloudEndpointPidRegex": "^org.eclipse.kura.cloud.CloudService\\-[a-zA-Z0-9]+$"
        },
        {
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager",
            "defaultCloudEndpointPid": "org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager",
            "cloudEndpointPidRegex": "^org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager(\\-[a-zA-Z0-9]+)?$"
        },
        {
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloudconnection.raw.mqtt.cloud.RawMqttCloudEndpoint",
            "defaultCloudEndpointPid": "org.eclipse.kura.cloudconnection.raw.mqtt.CloudEndpoint",
            "cloudEndpointPidRegex": "^org.eclipse.kura.cloudconnection.raw.mqtt.CloudEndpoint(\\-[a-zA-Z0-9]+)?$"
        },
        {
            "cloudConnectionFactoryPid": "org.eclipse.kura.camel.cloud.factory.CamelFactory",
            "defaultCloudEndpointPid": "org.eclipse.kura.camel.cloud.factory.CamelFactory",
            "cloudEndpointPidRegex": "^org.eclipse.kura.camel.cloud.factory.CamelFactory(\\-[a-zA-Z0-9]+)?$"
        }
    ],
    "pubSubFactories": [
        {
            "factoryPid": "org.eclipse.kura.cloudconnection.raw.mqtt.subscriber.RawMqttSubscriber",
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloudconnection.raw.mqtt.cloud.RawMqttCloudEndpoint"
        },
        {
            "factoryPid": "org.eclipse.kura.cloudconnection.eclipseiot.mqtt.CloudPublisher",
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloudconnection.eclipseiot.mqtt.ConnectionManager",
            "defaultPid": "org.eclipse.kura.cloudconnection.eclipseiot.mqtt.CloudPublisher",
            "defaultPidRegex": "^org.eclipse.kura.cloudconnection.eclipseiot.mqtt.CloudPublisher(\\-[a-zA-Z0-9]+)?$"
        },
        {
            "factoryPid": "org.eclipse.kura.cloud.subscriber.CloudSubscriber",
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloud.CloudService"
        },
        {
            "factoryPid": "org.eclipse.kura.cloud.publisher.CloudPublisher",
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloud.CloudService"
        },
        {
            "factoryPid": "org.eclipse.kura.cloudconnection.raw.mqtt.publisher.RawMqttPublisher",
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloudconnection.raw.mqtt.cloud.RawMqttCloudEndpoint"
        },
        {
            "factoryPid": "org.eclipse.kura.event.publisher.EventPublisher",
            "cloudConnectionFactoryPid": "org.eclipse.kura.cloud.CloudService"
        }
    ]
}
```
  - `cloudConnectionFactoryPid/factoryPid`: the PID of the factory
  - `defaultPid`: the default PID for an instance of a new component suggested by the factory.
  - `defaultPidRegex`: the regular expression to which a PID component must correspond.
  - in pubSubFactories `cloudConnectionFactoryPid`: the CloudEndpoint Factory to which the publisher or subscriber component is linked.
- 500 Internal Server error

## POST methods

#### Get StackComponents Pids

- Description: This method retrieves all all PIDs of Component instances that make up the stack for a specific [CloudEndpoint](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/CloudEndpoint.html). For example its [CloudService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloud/CloudService.html), [DataService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/data/DataService.html), [DataTransportService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/data/DataTransportService.html)
- Method: POST
- API PATH: `services/cloudconnection/v1/cloudEndpoint/stackComponentPids`

##### Request
```JSON
{
    "cloudConnectionFactoryPid" : "org.eclipse.kura.cloud.CloudService",
    "cloudEndpointPid": "org.eclipse.kura.cloud.CloudService"
}
```

##### Responses

- 200 Ok Status
```JSON
{
    "pids": [
        "org.eclipse.kura.cloud.CloudService",
        "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport",
        "org.eclipse.kura.data.DataService"
    ]
}
```
- 500 Internal Server Error

#### Create Cloud Endpoint

- Description: This method create a new [CloudEndpoint](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/CloudEndpoint.html), and the related stack components, for example [CloudService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloud/CloudService.html), [DataService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/data/DataService.html), [DataTransportService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/data/DataTransportService.html)
- Method: POST
- API PATH: `services/cloudconnection/v1/cloudEndpoint`

##### Request
```JSON
{
    "cloudConnectionFactoryPid" : "org.eclipse.kura.cloud.CloudService",
    "cloudEndpointPid" : "org.eclipse.kura.cloud.CloudService-1"
}
```

##### Responses

- 204 Ok Status
- 400 malformed `cloudConnectionFactoryPid` or `cloudEndpointPid`.
- 404 Wrong `cloudConnectionFactoryPid`
- 500 Internal Server Error

#### Create Publisher/Subscriber instance

- Description: his method create a new [CloudPublisher](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/publisher/CloudPublisher.html) or a [CloudSubscriber](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/subscriber/CloudSubscriber.html) instance for a specific CloudEndpoint [CloudEndpoint](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/CloudEndpoint.html). The type of the instance depends from the type of the specified factory. 
- Method: POST
- API PATH: `services/cloudconnection/v1/pubSub`

##### Request
```JSON
{
    "pid" : "testPub",
    "factoryPid" : "org.eclipse.kura.cloud.publisher.CloudPublisher",
    "cloudEndpointPid" : "org.eclipse.kura.cloud.CloudService"
}
```

##### Responses

- 204 Ok Status
- 400 malformed `pid`, `factoryPid` or `cloudEndpointPid` 
- 404 If `factoryPid` or `cloudEndpointPid` are wrong.

#### Get Configurations

- Description: This method retrieves the complete configuration, including the metatype description, of the component instance, for example [CloudPublisher](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/publisher/CloudPublisher.html) or a [CloudSubscriber](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/subscriber/CloudSubscriber.html) or [CloudService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloud/CloudService.html), [DataService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/data/DataService.html), [DataTransportService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/data/DataTransportService.html)
- Method: POST
- API PATH: `services/cloudconnection/v1/configurations`

##### Request
```JSON
{
    "pids" : ["testPub", "org.eclipse.kura.cloud.CloudService"]
}
```

##### Responses
```JSON
{
    "configs": [
        {
            "pid": "testPub",
            "definition": {
                "ad": [
                    {
                        "name": "Application Id",
                        "description": "The application id used to publish messages.",
                        "id": "appId",
                        "type": "STRING",
                        "cardinality": 0,
                        "defaultValue": "W1",
                        "isRequired": true
                    },
                    {
                        "name": "Application Topic",
                        "description": "Follows the application Id and specifies the rest of the publishing topic. Wildcards can be defined in the topic by specifing a $value in the field. The publisher will try to match \"value\" with a corresponding property in the received KuraMessage. If possible, the $value placeholder will be substituted with the real value specified in the KuraMessage received from the user application.",
                        "id": "app.topic",
                        "type": "STRING",
                        "cardinality": 0,
                        "defaultValue": "A1/$assetName",
                        "isRequired": false
                    },
                    {
                        "option": [
                            {
                                "label": "0",
                                "value": "0"
                            },
                            {
                                "label": "1",
                                "value": "1"
                            }
                        ],
                        "name": "Qos",
                        "description": "The desired quality of service for the messages that have to be published. If Qos is 0, the message is delivered at most once, or it is not delivered at all. If Qos is set to 1, the message is always delivered at least once.",
                        "id": "qos",
                        "type": "INTEGER",
                        "cardinality": 0,
                        "defaultValue": "0",
                        "isRequired": true
                    },
                    {
                        "name": "Retain",
                        "description": "Default retaing flag for the published messages.",
                        "id": "retain",
                        "type": "BOOLEAN",
                        "cardinality": 0,
                        "defaultValue": "false",
                        "isRequired": true
                    },
                    {
                        "option": [
                            {
                                "label": "Data",
                                "value": "data"
                            },
                            {
                                "label": "Control",
                                "value": "control"
                            }
                        ],
                        "name": "Kind of Message",
                        "description": "Type of message to be published.",
                        "id": "message.type",
                        "type": "STRING",
                        "cardinality": 0,
                        "defaultValue": "data",
                        "isRequired": true
                    },
                    {
                        "name": "Priority",
                        "description": "Message priority. Priority level 0 (highest) should be used sparingly and reserved for messages that should be sent with the minimum latency. Default is set to 7.",
                        "id": "priority",
                        "type": "INTEGER",
                        "cardinality": 0,
                        "min": "0",
                        "defaultValue": "7",
                        "isRequired": true
                    }
                ],
                "name": "CloudPublisher",
                "description": "The CloudPublisher allows to define publishing parameters and provide a simple endpoint where the applications can attach to publish their messages.",
                "id": "org.eclipse.kura.cloud.publisher.CloudPublisher"
            },
            "properties": {
                "app.topic": {
                    "value": "A1/$assetName",
                    "type": "STRING"
                },
                "message.type": {
                    "value": "data",
                    "type": "STRING"
                },
                "qos": {
                    "value": 0,
                    "type": "INTEGER"
                },
                "appId": {
                    "value": "W1",
                    "type": "STRING"
                },
                "retain": {
                    "value": false,
                    "type": "BOOLEAN"
                },
                "priority": {
                    "value": 7,
                    "type": "INTEGER"
                },
                "service.factoryPid": {
                    "value": "org.eclipse.kura.cloud.publisher.CloudPublisher",
                    "type": "STRING"
                },
                "cloud.endpoint.service.pid": {
                    "value": "org.eclipse.kura.cloud.CloudService",
                    "type": "STRING"
                },
                "kura.service.pid": {
                    "value": "testPub",
                    "type": "STRING"
                },
                "service.pid": {
                    "value": "org.eclipse.kura.cloud.publisher.CloudPublisher-1699623980455-20",
                    "type": "STRING"
                }
            }
        },
        {
            "pid": "org.eclipse.kura.cloud.CloudService",
            "definition": {
                "ad": [
                    {
                        "option": [
                            {
                                "label": "Set display name as device name",
                                "value": "device-name"
                            },
                            {
                                "label": "Set display name from hostname",
                                "value": "hostname"
                            },
                            {
                                "label": "Custom",
                                "value": "custom"
                            },
                            {
                                "label": "Server defined",
                                "value": "server"
                            }
                        ],
                        "name": "Device Display-Name",
                        "description": "Friendly name of the device. Device name is the common name of the device (eg: Reliagate 20-25, Raspberry Pi, etc.). Hostname will use the linux hostname utility.                  Custom allows for defining a unique string. Server defined relies on the remote management server to define a name.",
                        "id": "device.display-name",
                        "type": "STRING",
                        "cardinality": 0,
                        "defaultValue": "device-name",
                        "isRequired": true
                    },
                    {
                        "name": "Device Custom-Name",
                        "description": "Custom name for the device. This value is applied ONLY if device.display-name is set to \"Custom\"",
                        "id": "device.custom-name",
                        "type": "STRING",
                        "cardinality": 0,
                        "isRequired": false
                    },
                    {
                        "name": "Topic Control-Prefix",
                        "description": "Topic prefix for system and device management messages.",
                        "id": "topic.control-prefix",
                        "type": "STRING",
                        "cardinality": 0,
                        "defaultValue": "$EDC",
                        "isRequired": true
                    },
                    {
                        "name": "Encode gzip",
                        "description": "Compress message payloads before sending them to the remote server to reduce the network traffic.",
                        "id": "encode.gzip",
                        "type": "BOOLEAN",
                        "cardinality": 0,
                        "defaultValue": "true",
                        "isRequired": false
                    },
                    {
                        "name": "Republish Mqtt Birth Cert On Gps Lock",
                        "description": "Whether or not to republish the MQTT Birth Certificate on GPS lock event",
                        "id": "republish.mqtt.birth.cert.on.gps.lock",
                        "type": "BOOLEAN",
                        "cardinality": 0,
                        "defaultValue": "false",
                        "isRequired": true
                    },
                    {
                        "name": "Republish Mqtt Birth Cert On Modem Detect",
                        "description": "Whether or not to republish the MQTT Birth Certificate on modem detection event",
                        "id": "republish.mqtt.birth.cert.on.modem.detect",
                        "type": "BOOLEAN",
                        "cardinality": 0,
                        "defaultValue": "false",
                        "isRequired": true
                    },
                    {
                        "name": "Republish Mqtt Birth Cert On Tamper Event",
                        "description": "Whether or not to republish the MQTT Birth Certificate on a tamper event. This has effect only if a TamperDetectionService is available in the framework.",
                        "id": "republish.mqtt.birth.cert.on.tamper.event",
                        "type": "BOOLEAN",
                        "cardinality": 0,
                        "defaultValue": "true",
                        "isRequired": true
                    },
                    {
                        "name": "Enable Default Subscriptions",
                        "description": "Manages the default subscriptions to the gateway management MQTT topics. When disabled, the gateway will not be remotely manageable.",
                        "id": "enable.default.subscriptions",
                        "type": "BOOLEAN",
                        "cardinality": 0,
                        "defaultValue": "true",
                        "isRequired": true
                    },
                    {
                        "option": [
                            {
                                "label": "Kura Protobuf",
                                "value": "kura-protobuf"
                            },
                            {
                                "label": "Simple JSON",
                                "value": "simple-json"
                            }
                        ],
                        "name": "Payload Encoding",
                        "description": "Specify the message payload encoding.",
                        "id": "payload.encoding",
                        "type": "STRING",
                        "cardinality": 0,
                        "defaultValue": "kura-protobuf",
                        "isRequired": true
                    }
                ],
                "icon": [
                    {
                        "resource": "CloudService",
                        "size": 32
                    }
                ],
                "name": "CloudService",
                "description": "The CloudService allows for setting a user friendly name for the current device. It also provides the option to compress message payloads to reduce network traffic.",
                "id": "org.eclipse.kura.cloud.CloudService"
            },
            "properties": {
                "topic.control-prefix": {
                    "value": "$EDC",
                    "type": "STRING"
                },
                "republish.mqtt.birth.cert.on.tamper.event": {
                    "value": true,
                    "type": "BOOLEAN"
                },
                "device.custom-name": {
                    "value": "Intel UP²",
                    "type": "STRING"
                },
                "device.display-name": {
                    "value": "device-name",
                    "type": "STRING"
                },
                "payload.encoding": {
                    "value": "kura-protobuf",
                    "type": "STRING"
                },
                "republish.mqtt.birth.cert.on.modem.detect": {
                    "value": false,
                    "type": "BOOLEAN"
                },
                "service.factoryPid": {
                    "value": "org.eclipse.kura.cloud.CloudService",
                    "type": "STRING"
                },
                "kura.service.pid": {
                    "value": "org.eclipse.kura.cloud.CloudService",
                    "type": "STRING"
                },
                "service.pid": {
                    "value": "org.eclipse.kura.cloud.CloudService-1699623980404-13",
                    "type": "STRING"
                },
                "enable.default.subscriptions": {
                    "value": true,
                    "type": "BOOLEAN"
                },
                "republish.mqtt.birth.cert.on.gps.lock": {
                    "value": false,
                    "type": "BOOLEAN"
                },
                "encode.gzip": {
                    "value": true,
                    "type": "BOOLEAN"
                },
                "DataService.target": {
                    "value": "(kura.service.pid=org.eclipse.kura.data.DataService)",
                    "type": "STRING"
                },
                "kura.cloud.service.factory.pid": {
                    "value": "org.eclipse.kura.core.cloud.factory.DefaultCloudServiceFactory",
                    "type": "STRING"
                }
            }
        }
    ]
}
```
- 200 Status OK
- 500 Internal Error

#### Connect CloudEndpoint

- Description: This method allows to trigger the connection of the specified [CloudEndpoint](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/CloudEndpoint.html)
- Method: POST
- API PATH: `services/cloudconnection/v1/cloudEndpoint/connect`

##### Request
```JSON
{
    "cloudEndpointPid" : "org.eclipse.kura.cloud.CloudService"
}
```

##### Responses

- 204 Status OK
- 404 Wrong `cloudEndpointPid`
- 500 Internal Server Error in case of connection problems.

#### Disconnect CloudEndpoint

- Description: This method allows to trigger the disconnection of the specified [CloudEndpoint](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/CloudEndpoint.html)
- Method: POST
- API PATH: `services/cloudconnection/v1/cloudEndpoint/disconnect`

##### Request
```JSON
{
    "cloudEndpointPid" : "org.eclipse.kura.cloud.CloudService"
}
```

##### Responses

- 204 Status OK
- 404 Wrong `cloudEndpointPid`
- 500 Internal Server Error in case of connection problems.

#### Check CloudEndpoint connection status

- Description: This method allows to check the status of the connection of the specified [CloudEndpoint](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/CloudEndpoint.html)
- Method: POST
- API PATH: `services/cloudconnection/v1/cloudEndpoint/isConnected`

##### Request
```JSON
{
    "cloudEndpointPid" : "org.eclipse.kura.cloud.CloudService"
}
```

##### Responses
```JSON
{
    "connected": false
}
```
- 200 Status OK
- 404 Wrong `cloudEndpointPid`
- 500 Internal Server Error

## PUT methods

#### Update CloudEndpoint stack component configurations

- Description: This method allows to update the configuration of one or more components instance member of a stack configuration, for example. [CloudPublisher](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/publisher/CloudPublisher.html) or a [CloudSubscriber](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/subscriber/CloudSubscriber.html) or [CloudService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloud/CloudService.html), [DataService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/data/DataService.html), [DataTransportService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/data/DataTransportService.html)
- Method: PUT
- API PATH: `services/cloudconnection/v1/configurations`

##### Request
- takeSnapshot set to true o false to specify if the [ConfigurationService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/configuration/ConfigurationService.html) must save a snapshot with the updated configuration.
```JSON
{
    "configs": [
        {
            "pid": "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport",
            "properties": {
                "broker-url": {
                    "type": "STRING",
                    "value": "mqtt://mqtt.eclipseprojects.io:1883"
                },
                "topic.context.account-name": {
                    "type": "STRING",
                    "value": "account-name-testX2"
                },
                "username": {
                    "type": "STRING",
                    "value": "username"
                },
                "password": {
                    "type": "PASSWORD",
                    "value": "Placeholder"
                },
                "client-id": {
                    "type": "STRING",
                    "value": ""
                },
                "keep-alive": {
                    "type": "INTEGER",
                    "value": 30
                },
                "timeout": {
                    "type": "INTEGER",
                    "value": 20
                },
                "clean-session": {
                    "type": "BOOLEAN",
                    "value": true
                },
                "lwt.topic": {
                    "type": "STRING",
                    "value": "$EDC/#account-name/#client-id/MQTT/LWT"
                },
                "lwt.payload": {
                    "type": "STRING",
                    "value": ""
                },
                "lwt.qos": {
                    "type": "INTEGER",
                    "value": 0
                },
                "lwt.retain": {
                    "type": "BOOLEAN",
                    "value": false
                },
                "in-flight.persistence": {
                    "type": "STRING",
                    "value": "memory"
                },
                "protocol-version": {
                    "type": "INTEGER",
                    "value": 4
                },
                "SslManagerService.target": {
                    "type": "STRING",
                    "value": "(kura.service.pid=org.eclipse.kura.ssl.SslManagerService)"
                }
            }
        }
    ],
    "takeSnapshot" : true
}
```

##### Responses

- 204 Status OK
- 400 Bad request if `pid` is wrong or non existent.

## DELETE methods

#### Delete CloudEndpoint
- Description: This method allow to delete a [CloudEndpoint](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/CloudEndpoint.html) instance
- Method: DELETE
- API PATH: `services/cloudconnection/v1/cloudEndpoint`

##### Request
```JSON
{
    "cloudConnectionFactoryPid" : "org.eclipse.kura.cloud.CloudService",
    "cloudEndpointPid" : "org.eclipse.kura.cloud.CloudService-1"
}
```

##### Responses

- 204 Status OK
- 404 Wrong `cloudConnectionFactoryPid` or `cloudEndpointPid`.

#### Delete Publisher/Subscriber instance

- Description: This method allows to delete a [CloudPublisher](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/publisher/CloudPublisher.html) or [CloudSubscriber](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/cloudconnection/subscriber/CloudSubscriber.html) instance
- Method: DELETE
- API PATH: `services/cloudconnection/v1/pubSub`

##### Request
```JSON
{
    "pid" : "testPub"
}
```
##### Responses

- 204 Status OK
- 404 Wrong `cloudConnectionFactoryPid` or `cloudEndpointPid`.