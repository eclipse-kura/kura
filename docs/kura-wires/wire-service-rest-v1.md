# Wire Service V1 REST APIs and MQTT Request Handler

The WIRE-V1 cloud request handler and the corresponding REST APIs allow to update, delete and get the current Wire Graph status.
The request handler also supports creating, updating and deleting Asset and Driver instances and retrieving the metadata required for supporting Wire Graph editing applications.

The [GET/graph/shapshot](#getgraphshapshot) and [PUT/graph/snapshot](#putgraphsnapshot) requests use the same format as the Wire Graph snapshot functionality of the Kura Web UI.

A Wire Graph snapshot can be obtained by navigating to the **Wires** section of Kura Web UI, clicking the **Download** button and selecting the JSON format.

Accessing the REST APIs requires to use an identity with the `rest.wires.admin` permission assigned.

- [Wire Service V1 REST APIs and MQTT Request Handler](#wire-service-v1-rest-apis-and-mqtt-request-handler)
  - [Request definitions](#request-definitions)
    - [GET/graph/shapshot](#getgraphshapshot)
    - [PUT/graph/snapshot](#putgraphsnapshot)
    - [DEL/graph](#delgraph)
    - [GET/drivers/pids](#getdriverspids)
    - [GET/assets/pids](#getassetspids)
    - [GET/graph/topology](#getgraphtopology)
    - [POST/configs/byPid](#postconfigsbypid)
    - [DEL/configs/byPid](#delconfigsbypid)
    - [PUT/configs](#putconfigs)
    - [GET/metadata](#getmetadata)
    - [GET/metadata/wireComponents/factoryPids](#getmetadatawirecomponentsfactorypids)
    - [GET/metadata/wireComponents/definitions](#getmetadatawirecomponentsdefinitions)
    - [POST/metadata/wireComponents/definitions/byFactoryPid](#postmetadatawirecomponentsdefinitionsbyfactorypid)
    - [GET/metadata/drivers/factoryPids](#getmetadatadriversfactorypids)
    - [GET/metadata/driver/ocds](#getmetadatadriverocds)
    - [POST/metadata/drivers/ocds/byFactoryPid](#postmetadatadriversocdsbyfactorypid)
    - [GET/metadata/drivers/channelDescriptors](#getmetadatadriverschanneldescriptors)
    - [POST/metadata/drivers/channelDescriptors/byPid](#postmetadatadriverschanneldescriptorsbypid)
    - [GET/metadata/assets/channelDescriptor](#getmetadataassetschanneldescriptor)
  - [JSON definitions](#json-definitions)
    - [WireComponentDefinition](#wirecomponentdefinition)
    - [DriverChannelDescriptor](#driverchanneldescriptor)
    - [WireGraphMetadata](#wiregraphmetadata)
  - [Wire Graph snapshot example](#wire-graph-snapshot-example)



## Request definitions

### GET/graph/shapshot
  * **REST API path** : /services/wire/v1/graph/snapshot
  * **description** : Returns the current Wire Graph Configuration. The received configuration includes the [WireGraphService](doc:wiregraphservice-configuration-format) configuration containing the graph layout, the configuration of the components currently referenced by the Wire Graph, and the configuration of the existing Driver instances.
  * **responses** :
    * **200**
      * **description** : The current wire graph configuration.
      * **response body** :
        * [ComponentConfigurationList](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#componentconfigurationlist)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### PUT/graph/snapshot
  * **REST API path** : /services/wire/v1/graph/snapshot
  * **description** : Updates the current Wire Graph.
  * **request body** :
    * [ComponentConfigurationList](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#componentconfigurationlist)
  * **responses** :
    * **200**
      * **description** : The current Wire Graph has been updated.
    * **400**
      * **description** : The request body is not valid JSON or it contains invalid parameters.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)
    * **500**
      * **description** : In case of processing errors, the device will attempt to return a detailed error response containing a message describing the failure reason for each operation. The operation ids are the following: `updateGraph` for the graph update operation, `update:$pid` or `delete:$pid` for update or delete operations performed on configurations not referenced by the Wire Graph, and `snapshot`, for the snapshot creation operation. In case of an unexpected failure, a generic error response will be returned.
      * **response body** :
        
        **Variants**:
          
          * **object**
            * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)
          * **object**
            * [BatchFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#batchfailurereport)

This request will replace the current graph topology with the received one.
The received configuration must satisfy the following requirements. If any of the requirements is not met, the operation will fail and no changes will be applied to the system:
  * The configuration of the `org.eclipse.kura.wire.graph.WireGraphService` component must be specified.
  * The configuration of the `org.eclipse.kura.wire.graph.WireGraphService` component must contain a property named `WireGraph` of `STRING` type containing the graph layout as described in the [WireGraphService](doc:wiregraphservice-configuration-format) document.
  * The `inputPortCount` and `outputPortCount` properties must be specified for all components in **WireGraphService** configuration.
  * The configuration of all components referenced by **WireGraphService** configuration that do not exist on target device must be specified.
  * The configuration of all components referenced by **WireGraphService** configuration that do not exist on target device must specify the `service.factoryPid` configuration property reporting the component factory pid.

If a component already exists on the system and its configuration is supplied as part of the request, the component configuration will be updated. In this case the usual configuration merge semantics will be applied, the set of received properties will be merged with the existing one. The properties in the request body will overwrite the existing ones with the same name.

WireAsset configurations are treated sligtly differently, an update to a WireAsset configuration is performed by deleting the existing component and creating a new instance with the received configuration. This behavior is necessary in order to allow channel removal.

It is also allowed to specify Driver or Asset configurations that are not referenced by the Wire Graph included in the request body.

### DEL/graph
  * **REST API path** : /services/wire/v1/graph
  * **description** : Deletes the current Wire Graph.
  * **responses** :
    * **200**
      * **description** : The current wire graph has been deleted.
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### GET/drivers/pids
  * **REST API path** : /services/wire/v1/drivers/pids
  * **description** : Returns the list of existing Driver pids.
  * **responses** :
    * **200**
      * **description** : The list of driver pids
      * **response body** :
        * [PidAndFactoryPidSet](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#pidandfactorypidset)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### GET/assets/pids
  * **REST API path** : /services/wire/v1/assets/pids
  * **description** : Returns the list of existing Asset pids. The returned pids may or may not be referenced by the current Wire Graph.
  * **responses** :
    * **200**
      * **description** : The list of driver pids
      * **response body** :
        * [PidAndFactoryPidSet](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#pidandfactorypidset)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### GET/graph/topology
  * **REST API path** : /services/wire/v1/graph/topology
  * **description** : Returns the current Wire Graph topology as a [WireGraph](doc:wiregraphservice-configuration-format#wiregraph) object. The returned object is the current value of the WireGraph property in [Wire Graph Service](doc:wiregraphservice-configuration-format) configuration. This request allows to inspect the Wire Graph topology without downloading the entire Wire Graph snapshot with [GET/graph/shapshot](#getgraphshapshot).
  * **responses** :
    * **200**
      * **description** : The current wire graph topology.
      * **response body** :
        * [WireGraph](doc:wiregraphservice-configuration-format#wiregraph)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### POST/configs/byPid
  * **REST API path** : /services/wire/v1/configs/byPid
  * **description** : Returns the list of configurations referenced by the provided pids. This request can only be used to retrieve Wire Component, Driver or Asset configurations.
  * **request body** :
    * [PidSet](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#pidset)
  * **responses** :
    * **200**
      * **description** : The returned configurations. If a configuration cannot be found, it will not be included in the response. The returned configuration list can be empty.
      * **response body** :
        * [ComponentConfigurationList](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#componentconfigurationlist)
    * **400**
      * **description** : The request body is not valid JSON or it contains invalid parameters.
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### DEL/configs/byPid
  * **REST API path** : /services/wire/v1/configs/byPid
  * **description** : Deletes the configurations referenced by the provided pids. This request can only be used to delete Wire Component, Driver or Asset configurations. This request does not allow to delete configurations that are referenced by the current Wire Graph.
  * **request body** :
    * [PidSet](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#pidset)
  * **responses** :
    * **200**
      * **description** : The request succeeded.
    * **400**
      * **description** : The request body is not valid JSON or it contains invalid parameters. This status will be returned also if the request references pids that are currenly part of the Wire Graph, or components that are not Wire Components, Driver or Asset instances. If this status is retured, no changes will be applied to the system.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)
    * **500**
      * **description** : In case of processing errors, the device will attempt to return a detailed error response containing a message describing the failure reason for each operation. The operation ids are the following: `delete:$pid` for delete operations, and `snapshot`, for the snapshot creation operation. In case of an unexpected failure, a generic error response will be returned.
      * **response body** :
        
        **Variants**:
          
          * **object**
            * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)
          * **object**
            * [BatchFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#batchfailurereport)

### PUT/configs
  * **REST API path** : /services/wire/v1/configs
  * **description** : Updates or creates the provided configurations. This request can only be used to process Wire Component, Driver or Asset configurations. This request does not allow to create Wire Component configurations that are not referenced by the current Wire Graph, except from WireAsset instances. The component creation/update semantics are the same as [PUT/graph/snapshot](#putgraphsnapshot), this request can be used to perform configuration updates whithout knowing or specifying the Wire Graph topology.
  * **request body** :
    * [ComponentConfigurationList](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#componentconfigurationlist)
  * **responses** :
    * **200**
      * **description** : The request succeeded
    * **400**
      * **description** : The request body is not valid JSON or it contains invalid parameters. This status will be returned also if the request involves the creation of components that are not Wire Components, Driver or Asset instances, or the creation of Wire Components that are not referenced by the current Wire Graph. If this status is retured, no changes will be applied to the system.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)
    * **500**
      * **description** : In case of processing errors, the device will attempt to return a detailed error response containing a message describing the failure reason for each operation. The operation ids are the following: `update:$pid` or `delete:$pid` for update or delete operations, and `snapshot`, for the snapshot creation operation. In case of an unexpected failure, a generic error response will be returned.
      * **response body** :
        
        **Variants**:
          
          * **object**
            * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)
          * **object**
            * [BatchFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#batchfailurereport)

### GET/metadata
  * **REST API path** : /services/wire/v1/metadata
  * **description** : Returns all available Wire Component, Asset and Driver metadata in a single request.
  * **responses** :
    * **200**
      * **description** : The request succeeded. Single fields in the response can be missing if the corresponding list is empty (e.g. `driverDescriptors` can be missing if no Driver instances exist on the system)
      * **response body** :
        * [WireGraphMetadata](#wiregraphmetadata)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### GET/metadata/wireComponents/factoryPids
  * **REST API path** : /services/wire/v1/metadata/wireComponents/factoryPids
  * **description** : Return the list of available Wire Component factory pids
  * **responses** :
    * **200**
      * **description** : The request succeeded.
      * **response body** :
        * [PidSet](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#pidset)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### GET/metadata/wireComponents/definitions
  * **REST API path** : /services/wire/v1/metadata/wireComponents/definitions
  * **description** : Returns all available Wire Component definitions
  * **responses** :
    * **200**
      * **description** : The available Wire Component definitions. All fields except at most `wireComponentDefinitions` will be missing.
      * **response body** :
        * [WireGraphMetadata](#wiregraphmetadata)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### POST/metadata/wireComponents/definitions/byFactoryPid
  * **REST API path** : /services/wire/v1/metadata/wireComponents/definitions/byFactoryPid
  * **description** : Returns the Wire Component definitions for the given set of factory pids
  * **request body** :
    * [PidSet](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#pidset)
  * **responses** :
    * **200**
      * **description** : The available Wire Component definitions. All fields except at most `wireComponentDefinitions` will be missing. If the metadata for a given factoryPid is not found, the request will succeed and it will not be included in the list.
      * **response body** :
        * [WireGraphMetadata](#wiregraphmetadata)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### GET/metadata/drivers/factoryPids
  * **REST API path** : /services/wire/v1/metadata/drivers/factoryPids
  * **description** : Return the list of available Driver factory pids
  * **responses** :
    * **200**
      * **description** : The request succeeded.
      * **response body** :
        * [PidSet](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#pidset)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### GET/metadata/driver/ocds
  * **REST API path** : /services/wire/v1/metadata/drivers/ocds
  * **description** : Returns all available Driver OCDs
  * **responses** :
    * **200**
      * **description** : The available Driver OCDs. All fields except at most `driverOCDs` will be missing.
      * **response body** :
        * [WireGraphMetadata](#wiregraphmetadata)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### POST/metadata/drivers/ocds/byFactoryPid
  * **REST API path** : /services/wire/v1/metadata/drivers/ocds/byFactoryPid
  * **description** : Returns the Driver OCDSs for the given set of factory pids
  * **request body** :
    * [PidSet](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#pidset)
  * **responses** :
    * **200**
      * **description** : The requested Driver OCDs. All fields except at most `driverOCDs` will be missing. If the metadata for a given factoryPid is not found, the request will succeed and it will not be included in the list.
      * **response body** :
        * [WireGraphMetadata](#wiregraphmetadata)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### GET/metadata/drivers/channelDescriptors
  * **REST API path** : /wire/v1/metadata/drivers/channelDescriptors
  * **description** : Returns the list of all available Driver channel descriptors
  * **responses** :
    * **200**
      * **description** : The list of Driver channel descriptors. All fields except at most `driverChannelDescriptors` will be missing.
      * **response body** :
        * [WireGraphMetadata](#wiregraphmetadata)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### POST/metadata/drivers/channelDescriptors/byPid
  * **REST API path** : /services/wire/v1/metadata/drivers/channelDescriptors/byPid
  * **description** : Returns the Driver channel descriptors for the given set of pids
  * **request body** :
    * [PidSet](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#pidset)
  * **responses** :
    * **200**
      * **description** : The requested Driver channel descriptors. All fields except at most `driverChannelDescriptors` will be missing. If the metadata for a given pid is not found, the request will succeed and it will not be included in the list.
      * **response body** :
        * [WireGraphMetadata](#wiregraphmetadata)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)

### GET/metadata/assets/channelDescriptor
  * **REST API path** : /wire/v1/metadata/assets/channelDescriptor
  * **description** : Returns the Asset channel descriptor
  * **responses** :
    * **200**
      * **description** : The Asset channel descriptors. All fields except assetChannelDescriptor will be missing.
      * **response body** :
        * [WireGraphMetadata](#wiregraphmetadata)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#genericfailurereport)



## JSON definitions

### WireComponentDefinition
A Wire Component definition object

**Properties**:

* **factoryPid**: `string`
    The component factory pid
* **minInputPorts**: `number`
    The minimum input port count
* **maxInputPorts**: `number`
    The maximum number of input ports
* **defaultInputPorts**: `number`
    The default number of input ports
* **minOutputPorts**: `number`
    The minimum number of output ports
* **maxOutputPorts**: `number`
    The maximum number of output ports
* **defaultOutputPorts**: `number`
    The default number of output ports
* **inputPortNames**: `object`
  * **optional** If no custom input port names are defined
    * [PortNameList](doc:wiregraphservice-configuration-format#portnamelist)
* **outputPortNames**: `object`
  * **optional** If no custom output port names are defined
    * [PortNameList](doc:wiregraphservice-configuration-format#portnamelist)
* **componentOcd**: `array`
  * **optional** If the component OCD is empty
    The component OCD
    * array elements: `object`
      * [AttributeDefinition](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#attributedefinition)

```json
{
  "componentOCD": [
    {
      "cardinality": 0,
      "defaultValue": "50",
      "description": "The maximum number of envelopes that can be stored in the queue of this FIFO component",
      "id": "queue.capacity",
      "isRequired": true,
      "name": "queue.capacity",
      "type": "INTEGER"
    },
    {
      "cardinality": 0,
      "defaultValue": "false",
      "description": "Defines the behavior in case of full queue: if set to true new envelopes will be dropped,              otherwise, if an emitter delivers an envelope to this component it will block until the envelope can be successfully enqueued.",
      "id": "discard.envelopes",
      "isRequired": true,
      "name": "discard.envelopes",
      "type": "BOOLEAN"
    }
  ],
  "defaultInputPorts": 1,
  "defaultOutputPorts": 1,
  "factoryPid": "org.eclipse.kura.wire.Fifo",
  "maxInputPorts": 1,
  "maxOutputPorts": 1,
  "minInputPorts": 1,
  "minOutputPorts": 1
}
```

### DriverChannelDescriptor

An object that describes Driver specific channel configuration properties

**Properties**:

* **pid**: `string`
    The Driver pid
* **factoryPid**: `string`
    The Driver factory pid
* **channelDescriptor**: `array`
  * **optional** If the driver does not define any channel property
    The list of Driver specific channel configuration properties
    * array elements: `object`
      * [AttributeDefinition](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#attributedefinition)

```json
{
  "channelDescriptor": [
    {
      "cardinality": 0,
      "defaultValue": "AA:BB:CC:DD:EE:FF",
      "description": "sensortag.address",
      "id": "sensortag.address",
      "isRequired": true,
      "name": "sensortag.address",
      "type": "STRING"
    },
    {
      "cardinality": 0,
      "defaultValue": "TEMP_AMBIENT",
      "description": "sensor.name",
      "id": "sensor.name",
      "isRequired": true,
      "name": "sensor.name",
      "option": [
        {
          "label": "TEMP_AMBIENT",
          "value": "TEMP_AMBIENT"
        },
        {
          "label": "TEMP_TARGET",
          "value": "TEMP_TARGET"
        },
        {
          "label": "HUMIDITY",
          "value": "HUMIDITY"
        },
        {
          "label": "ACCELERATION_X",
          "value": "ACCELERATION_X"
        },
        {
          "label": "ACCELERATION_Y",
          "value": "ACCELERATION_Y"
        },
        {
          "label": "ACCELERATION_Z",
          "value": "ACCELERATION_Z"
        },
        {
          "label": "MAGNETIC_X",
          "value": "MAGNETIC_X"
        },
        {
          "label": "MAGNETIC_Y",
          "value": "MAGNETIC_Y"
        },
        {
          "label": "MAGNETIC_Z",
          "value": "MAGNETIC_Z"
        },
        {
          "label": "GYROSCOPE_X",
          "value": "GYROSCOPE_X"
        },
        {
          "label": "GYROSCOPE_Y",
          "value": "GYROSCOPE_Y"
        },
        {
          "label": "GYROSCOPE_Z",
          "value": "GYROSCOPE_Z"
        },
        {
          "label": "LIGHT",
          "value": "LIGHT"
        },
        {
          "label": "PRESSURE",
          "value": "PRESSURE"
        },
        {
          "label": "GREEN_LED",
          "value": "GREEN_LED"
        },
        {
          "label": "RED_LED",
          "value": "RED_LED"
        },
        {
          "label": "BUZZER",
          "value": "BUZZER"
        },
        {
          "label": "KEYS",
          "value": "KEYS"
        }
      ],
      "type": "STRING"
    },
    {
      "cardinality": 0,
      "defaultValue": "1000",
      "description": "notification.period",
      "id": "notification.period",
      "isRequired": true,
      "name": "notification.period",
      "type": "INTEGER"
    }
  ],
  "factoryPid": "org.eclipse.kura.driver.ble.sensortag",
  "pid": "sensortag"
}
```

### WireGraphMetadata

An object contiaining metatada describing Wire Components, Drivers and Assets

**Properties**:

* **wireComponentDefinitions**: `array`
  * **optional** See request specific documentation
    The list of Wire Component definitions
    * array elements: `object`
      * [WireComponentDefinition](#wirecomponentdefinition)
* **driverOCDs**: `array`
  * **optional** See request specific documentation
    The list of Driver factory component OCDs
    * array elements: `object`
      * [ComponentConfiguration](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#componentconfiguration)
* **driverChannelDescriptors**: `array`
  * **optional** See request specific documentation
    The list of Driver channel descriptors
    * array elements: `object`
      * [DriverChannelDescriptor](#driverchanneldescriptor)
* **assetChannelDescriptor**: `array`
  * **optional** See request specific documentation
    The list of Asset specific channel configuration properties
    * array elements: `object`
      * [AttributeDefinition](doc:configuration-v2-rest-apis-and-conf-v2-request-handler#attributedefinition)

```json
{
  "assetChannelDescriptor": [
    {
      "cardinality": 0,
      "defaultValue": "true",
      "description": "Determines if the channel is enabled or not",
      "id": "+enabled",
      "isRequired": true,
      "name": "enabled",
      "type": "BOOLEAN"
    },
    {
      "cardinality": 0,
      "defaultValue": "Channel-1",
      "description": "Name of the Channel",
      "id": "+name",
      "isRequired": true,
      "name": "name",
      "type": "STRING"
    },
    {
      "cardinality": 0,
      "defaultValue": "READ",
      "description": "Type of the channel",
      "id": "+type",
      "isRequired": true,
      "name": "type",
      "option": [
        {
          "label": "READ",
          "value": "READ"
        },
        {
          "label": "READ_WRITE",
          "value": "READ_WRITE"
        },
        {
          "label": "WRITE",
          "value": "WRITE"
        }
      ],
      "type": "STRING"
    },
    {
      "cardinality": 0,
      "defaultValue": "INTEGER",
      "description": "Value type of the channel",
      "id": "+value.type",
      "isRequired": true,
      "name": "value.type",
      "option": [
        {
          "label": "BOOLEAN",
          "value": "BOOLEAN"
        },
        {
          "label": "BYTE_ARRAY",
          "value": "BYTE_ARRAY"
        },
        {
          "label": "DOUBLE",
          "value": "DOUBLE"
        },
        {
          "label": "INTEGER",
          "value": "INTEGER"
        },
        {
          "label": "LONG",
          "value": "LONG"
        },
        {
          "label": "FLOAT",
          "value": "FLOAT"
        },
        {
          "label": "STRING",
          "value": "STRING"
        }
      ],
      "type": "STRING"
    },
    {
      "cardinality": 0,
      "description": "Scale to be applied to the numeric value of the channel",
      "id": "+scale",
      "isRequired": false,
      "name": "scale",
      "type": "DOUBLE"
    },
    {
      "cardinality": 0,
      "description": "Offset to be applied to the numeric value of the channel",
      "id": "+offset",
      "isRequired": false,
      "name": "offset",
      "type": "DOUBLE"
    },
    {
      "cardinality": 0,
      "defaultValue": "",
      "description": "Unit associated to the value of the channel",
      "id": "+unit",
      "isRequired": false,
      "name": "unit",
      "type": "STRING"
    },
    {
      "cardinality": 0,
      "defaultValue": "false",
      "description": "Specifies if WireAsset should emit envelopes on Channel events",
      "id": "+listen",
      "isRequired": true,
      "name": "listen",
      "type": "BOOLEAN"
    }
  ],
  "driverChannelDescriptors": [
    {
      "channelDescriptor": [
        {
          "cardinality": 0,
          "defaultValue": "MyNode",
          "description": "node.id",
          "id": "node.id",
          "isRequired": true,
          "name": "node.id",
          "type": "STRING"
        },
        {
          "cardinality": 0,
          "defaultValue": "2",
          "description": "node.namespace.index",
          "id": "node.namespace.index",
          "isRequired": true,
          "name": "node.namespace.index",
          "type": "INTEGER"
        },
        {
          "cardinality": 0,
          "defaultValue": "DEFINED_BY_JAVA_TYPE",
          "description": "opcua.type",
          "id": "opcua.type",
          "isRequired": true,
          "name": "opcua.type",
          "option": [
            {
              "label": "DEFINED_BY_JAVA_TYPE",
              "value": "DEFINED_BY_JAVA_TYPE"
            },
            {
              "label": "BOOLEAN",
              "value": "BOOLEAN"
            },
            {
              "label": "SBYTE",
              "value": "SBYTE"
            },
            {
              "label": "INT16",
              "value": "INT16"
            },
            {
              "label": "INT32",
              "value": "INT32"
            },
            {
              "label": "INT64",
              "value": "INT64"
            },
            {
              "label": "BYTE",
              "value": "BYTE"
            },
            {
              "label": "UINT16",
              "value": "UINT16"
            },
            {
              "label": "UINT32",
              "value": "UINT32"
            },
            {
              "label": "UINT64",
              "value": "UINT64"
            },
            {
              "label": "FLOAT",
              "value": "FLOAT"
            },
            {
              "label": "DOUBLE",
              "value": "DOUBLE"
            },
            {
              "label": "STRING",
              "value": "STRING"
            },
            {
              "label": "BYTE_STRING",
              "value": "BYTE_STRING"
            },
            {
              "label": "BYTE_ARRAY",
              "value": "BYTE_ARRAY"
            },
            {
              "label": "SBYTE_ARRAY",
              "value": "SBYTE_ARRAY"
            }
          ],
          "type": "STRING"
        },
        {
          "cardinality": 0,
          "defaultValue": "STRING",
          "description": "node.id.type",
          "id": "node.id.type",
          "isRequired": true,
          "name": "node.id.type",
          "option": [
            {
              "label": "NUMERIC",
              "value": "NUMERIC"
            },
            {
              "label": "STRING",
              "value": "STRING"
            },
            {
              "label": "GUID",
              "value": "GUID"
            },
            {
              "label": "OPAQUE",
              "value": "OPAQUE"
            }
          ],
          "type": "STRING"
        },
        {
          "cardinality": 0,
          "defaultValue": "Value",
          "description": "attribute",
          "id": "attribute",
          "isRequired": true,
          "name": "attribute",
          "option": [
            {
              "label": "NodeId",
              "value": "NodeId"
            },
            {
              "label": "NodeClass",
              "value": "NodeClass"
            },
            {
              "label": "BrowseName",
              "value": "BrowseName"
            },
            {
              "label": "DisplayName",
              "value": "DisplayName"
            },
            {
              "label": "Description",
              "value": "Description"
            },
            {
              "label": "WriteMask",
              "value": "WriteMask"
            },
            {
              "label": "UserWriteMask",
              "value": "UserWriteMask"
            },
            {
              "label": "IsAbstract",
              "value": "IsAbstract"
            },
            {
              "label": "Symmetric",
              "value": "Symmetric"
            },
            {
              "label": "InverseName",
              "value": "InverseName"
            },
            {
              "label": "ContainsNoLoops",
              "value": "ContainsNoLoops"
            },
            {
              "label": "EventNotifier",
              "value": "EventNotifier"
            },
            {
              "label": "Value",
              "value": "Value"
            },
            {
              "label": "DataType",
              "value": "DataType"
            },
            {
              "label": "ValueRank",
              "value": "ValueRank"
            },
            {
              "label": "ArrayDimensions",
              "value": "ArrayDimensions"
            },
            {
              "label": "AccessLevel",
              "value": "AccessLevel"
            },
            {
              "label": "UserAccessLevel",
              "value": "UserAccessLevel"
            },
            {
              "label": "MinimumSamplingInterval",
              "value": "MinimumSamplingInterval"
            },
            {
              "label": "Historizing",
              "value": "Historizing"
            },
            {
              "label": "Executable",
              "value": "Executable"
            },
            {
              "label": "UserExecutable",
              "value": "UserExecutable"
            }
          ],
          "type": "STRING"
        },
        {
          "cardinality": 0,
          "defaultValue": "1000",
          "description": "listen.sampling.interval",
          "id": "listen.sampling.interval",
          "isRequired": true,
          "name": "listen.sampling.interval",
          "type": "DOUBLE"
        },
        {
          "cardinality": 0,
          "defaultValue": "10",
          "description": "listen.queue.size",
          "id": "listen.queue.size",
          "isRequired": true,
          "name": "listen.queue.size",
          "type": "LONG"
        },
        {
          "cardinality": 0,
          "defaultValue": "true",
          "description": "listen.discard.oldest",
          "id": "listen.discard.oldest",
          "isRequired": true,
          "name": "listen.discard.oldest",
          "type": "BOOLEAN"
        },
        {
          "cardinality": 0,
          "defaultValue": "false",
          "description": "listen.subscribe.to.children",
          "id": "listen.subscribe.to.children",
          "isRequired": true,
          "name": "listen.subscribe.to.children",
          "type": "BOOLEAN"
        }
      ],
      "factoryPid": "org.eclipse.kura.driver.opcua",
      "pid": "opcuaDriver"
    }
  ],
  "driverOCDs": [
    {
      "definition": {
        "ad": [
          {
            "cardinality": 0,
            "defaultValue": "default-server",
            "description": "OPC-UA Endpoint IP Address",
            "id": "endpoint.ip",
            "isRequired": true,
            "name": "Endpoint IP",
            "type": "STRING"
          },
          {
            "cardinality": 0,
            "defaultValue": "53530",
            "description": "OPC-UA Endpoint Port",
            "id": "endpoint.port",
            "isRequired": true,
            "min": "1",
            "name": "Endpoint port",
            "type": "INTEGER"
          },
          {
            "cardinality": 0,
            "defaultValue": "OPC-UA-Server",
            "description": "OPC-UA Server Name",
            "id": "server.name",
            "isRequired": false,
            "name": "Server Name",
            "type": "STRING"
          },
          {
            "cardinality": 0,
            "defaultValue": "false",
            "description": "If set to true the driver will use the hostname, port, and server name parameters specified in the configuration instead of the values contained in endpoint descriptions fetched from the server.",
            "id": "force.endpoint.url",
            "isRequired": false,
            "name": "Force endpoint URL",
            "type": "BOOLEAN"
          },
          {
            "cardinality": 0,
            "defaultValue": "120",
            "description": "Session timeout (in seconds)",
            "id": "session.timeout",
            "isRequired": true,
            "name": "Session timeout",
            "type": "INTEGER"
          },
          {
            "cardinality": 0,
            "defaultValue": "60",
            "description": "Request timeout (in seconds)",
            "id": "request.timeout",
            "isRequired": true,
            "name": "Request timeout",
            "type": "INTEGER"
          },
          {
            "cardinality": 0,
            "defaultValue": "40",
            "description": "The time to wait for the server response to the 'Hello' message (in seconds)",
            "id": "acknowledge.timeout",
            "isRequired": true,
            "name": "Acknowledge timeout",
            "type": "INTEGER"
          },
          {
            "cardinality": 0,
            "defaultValue": "opc-ua client",
            "description": "OPC-UA application name",
            "id": "application.name",
            "isRequired": true,
            "name": "Application name",
            "type": "STRING"
          },
          {
            "cardinality": 0,
            "defaultValue": "urn:kura:opcua:client",
            "description": "OPC-UA application uri",
            "id": "application.uri",
            "isRequired": true,
            "name": "Application URI",
            "type": "STRING"
          },
          {
            "cardinality": 0,
            "defaultValue": "1000",
            "description": "The publish interval in milliseconds for the subscription created by the driver.",
            "id": "subscription.publish.interval",
            "isRequired": true,
            "name": "Subscription publish interval",
            "type": "LONG"
          },
          {
            "cardinality": 0,
            "defaultValue": "PFX or JKS Keystore",
            "description": "Absolute path of the PKCS or JKS keystore that contains the OPC-UA client certificate, private key and trusted server certificates",
            "id": "certificate.location",
            "isRequired": true,
            "name": "Keystore path",
            "type": "STRING"
          },
          {
            "cardinality": 0,
            "defaultValue": "0",
            "description": "Security Policy",
            "id": "security.policy",
            "isRequired": true,
            "name": "Security policy",
            "option": [
              {
                "label": "None",
                "value": "0"
              },
              {
                "label": "Basic128Rsa15",
                "value": "1"
              },
              {
                "label": "Basic256",
                "value": "2"
              },
              {
                "label": "Basic256Sha256",
                "value": "3"
              }
            ],
            "type": "INTEGER"
          },
          {
            "cardinality": 0,
            "description": "OPC-UA server username",
            "id": "username",
            "isRequired": false,
            "name": "Username",
            "type": "STRING"
          },
          {
            "cardinality": 0,
            "description": "OPC-UA server password",
            "id": "password",
            "isRequired": false,
            "name": "Password",
            "type": "PASSWORD"
          },
          {
            "cardinality": 0,
            "defaultValue": "client-ai",
            "description": "Alias for the client certificate in the keystore",
            "id": "keystore.client.alias",
            "isRequired": true,
            "name": "Client certificate alias",
            "type": "STRING"
          },
          {
            "cardinality": 0,
            "defaultValue": "false",
            "description": "Specifies whether to enable or not server certificate verification",
            "id": "authenticate.server",
            "isRequired": true,
            "name": "Enable server authentication",
            "type": "BOOLEAN"
          },
          {
            "cardinality": 0,
            "defaultValue": "PKCS12",
            "description": "Keystore type",
            "id": "keystore.type",
            "isRequired": true,
            "name": "Keystore type",
            "option": [
              {
                "label": "PKCS11",
                "value": "PKCS11"
              },
              {
                "label": "PKCS12",
                "value": "PKCS12"
              },
              {
                "label": "JKS",
                "value": "JKS"
              }
            ],
            "type": "STRING"
          },
          {
            "cardinality": 0,
            "defaultValue": "password",
            "description": "Configurable Property to set keystore password (default set to password)",
            "id": "keystore.password",
            "isRequired": true,
            "name": "Keystore password",
            "type": "PASSWORD"
          },
          {
            "cardinality": 0,
            "defaultValue": "200",
            "description": "Maximum number of items that will be included in a single request to the server.",
            "id": "max.request.items",
            "isRequired": true,
            "name": "Max request items",
            "type": "INTEGER"
          },
          {
            "cardinality": 0,
            "defaultValue": "BROWSE_PATH",
            "description": "The format to be used for channel name for subtree subscriptions.     If set to BROWSE_PATH, the channel name will contain the browse path of the source node relative to the subscription root.     If set to NODE_ID, the name will contain the node id of the source node.",
            "id": "subtree.subscription.name.format",
            "isRequired": true,
            "name": "Subtree subscription events channel name format",
            "option": [
              {
                "label": "BROWSE_PATH",
                "value": "BROWSE_PATH"
              },
              {
                "label": "NODE_ID",
                "value": "NODE_ID"
              }
            ],
            "type": "STRING"
          }
        ],
        "description": "OPC-UA Driver",
        "id": "org.eclipse.kura.driver.opcua",
        "name": "OpcUaDriver"
      },
      "pid": "org.eclipse.kura.driver.opcua"
    }
  ],
  "wireComponentDefinitions": [
    {
      "componentOCD": [
        {
          "cardinality": 0,
          "defaultValue": "records[0].TIMER !== null && records[0].TIMER.getValue() > 10 && records[0]['TIMER'].getValue() < 30;\n",
          "description": "The boolean expression to be evaluated by this component when a wire envelope is              received.",
          "id": "condition",
          "isRequired": true,
          "name": "condition",
          "type": "STRING"
        }
      ],
      "defaultInputPorts": 1,
      "defaultOutputPorts": 2,
      "factoryPid": "org.eclipse.kura.wire.Conditional",
      "inputPortNames": {
        "0": "if"
      },
      "maxInputPorts": 1,
      "maxOutputPorts": 2,
      "minInputPorts": 1,
      "minOutputPorts": 2,
      "outputPortNames": {
        "0": "then",
        "1": "else"
      }
    }
  ]
}
```



## Wire Graph snapshot example

```json
{
  "configs": [
    {
      "pid": "org.eclipse.kura.wire.graph.WireGraphService",
      "properties": {
        "WireGraph": {
          "type": "STRING",
          "value": "{\"components\":[{\"pid\":\"timer\",\"inputPortCount\":0,\"outputPortCount\":1,\"renderingProperties\":{\"position\":{\"x\":-300,\"y\":-20},\"inputPortNames\":{},\"outputPortNames\":{}}},{\"pid\":\"logger\",\"inputPortCount\":1,\"outputPortCount\":0,\"renderingProperties\":{\"position\":{\"x\":-100,\"y\":-20},\"inputPortNames\":{},\"outputPortNames\":{}}}],\"wires\":[{\"emitter\":\"timer\",\"emitterPort\":0,\"receiver\":\"logger\",\"receiverPort\":0}]}"
        }
      }
    },
    {
      "pid": "timer",
      "properties": {
        "componentDescription": {
          "type": "STRING",
          "value": "A wire component that fires a ticking event on every configured interval"
        },
        "componentId": {
          "type": "STRING",
          "value": "timer"
        },
        "componentName": {
          "type": "STRING",
          "value": "Timer"
        },
        "cron.interval": {
          "type": "STRING",
          "value": "0/10 * * * * ?"
        },
        "emitter.port.count": {
          "type": "INTEGER",
          "value": 1
        },
        "factoryComponent": {
          "type": "BOOLEAN",
          "value": false
        },
        "factoryPid": {
          "type": "STRING",
          "value": "org.eclipse.kura.wire.Timer"
        },
        "kura.service.pid": {
          "type": "STRING",
          "value": "timer"
        },
        "receiver.port.count": {
          "type": "INTEGER",
          "value": 0
        },
        "service.factoryPid": {
          "type": "STRING",
          "value": "org.eclipse.kura.wire.Timer"
        },
        "service.pid": {
          "type": "STRING",
          "value": "org.eclipse.kura.wire.Timer-1642493602000-13"
        },
        "simple.custom.first.tick.interval": {
          "type": "INTEGER",
          "value": 0
        },
        "simple.first.tick.policy": {
          "type": "STRING",
          "value": "DEFAULT"
        },
        "simple.interval": {
          "type": "INTEGER",
          "value": 10
        },
        "simple.time.unit": {
          "type": "STRING",
          "value": "SECONDS"
        },
        "type": {
          "type": "STRING",
          "value": "SIMPLE"
        }
      }
    },
    {
      "pid": "logger",
      "properties": {
        "componentDescription": {
          "type": "STRING",
          "value": "A wire component which logs data as received from upstream connected Wire Components"
        },
        "componentId": {
          "type": "STRING",
          "value": "logger"
        },
        "componentName": {
          "type": "STRING",
          "value": "Logger"
        },
        "emitter.port.count": {
          "type": "INTEGER",
          "value": 0
        },
        "factoryComponent": {
          "type": "BOOLEAN",
          "value": false
        },
        "factoryPid": {
          "type": "STRING",
          "value": "org.eclipse.kura.wire.Logger"
        },
        "kura.service.pid": {
          "type": "STRING",
          "value": "logger"
        },
        "log.verbosity": {
          "type": "STRING",
          "value": "QUIET"
        },
        "receiver.port.count": {
          "type": "INTEGER",
          "value": 1
        },
        "service.factoryPid": {
          "type": "STRING",
          "value": "org.eclipse.kura.wire.Logger"
        },
        "service.pid": {
          "type": "STRING",
          "value": "org.eclipse.kura.wire.Logger-1642493602046-14"
        }
      }
    }
  ]
}
```