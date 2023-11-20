# Network Configuration V1 REST APIs

This section describes the `networkConfiguration/v1` REST APIs, that allow to retrieve information about the network configuration of the running system. The services `pid` for which it's possible to obtain and update the configurations are:

- `org.eclipse.kura.net.admin.NetworkConfigurationService`: which manages the network configuration of the system, so the network interfaces along with their network configuration (Ip4/Ip6 settings, DHCP, Nat, and so on)
- `org.eclipse.kura.net.admin.FirewallConfigurationService`: which manages the Ip4 firewall configurations (open ports, port forwarding and masquerading for IPv4 protocol)
- `org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6`: which manages the Ip6 firewall configurations (open ports, port forwarding and masquerading for IPv6 protocol)

To access these REST APIs, an identity with `rest.network.configuration` permission assigned is required.

- [Request definitions](#request-definitions)
    - [GET/configurableComponents](#getconfigurablecomponents)
    - [GET/configurableComponents/configurations](#getconfigurablecomponentsconfigurations)
    - [POST/configurableComponents/configurations/byPid](#postconfigurablecomponentsconfigurationsbypid)
    - [POST/configurableComponents/configurations/byPid/_default](#postconfigurablecomponentsconfigurationsbypid_default)
    - [PUT/configurableComponents/configurations/_update](#putconfigurablecomponentsconfigurations_update)
- [JSON definitions](#json-definitions)
    - [BatchFailureReport](#batchfailurereport)
    - [ComponentConfigurationList](#componentconfigurationlist)
    - [GenericFailureReport](#genericfailurereport)
    - [PidSet](#pidset)
    - [UpdateComponentConfigurationRequest](#updatecomponentconfigurationrequest)

## Request definitions
#### GET/configurableComponents
  * **REST API path** : `/services/networkConfiguration/v1/configurableComponents`
  * **description** : Returns the list of the available services that manages the network configurations on the system.
  * **responses** :
    * **200**
      * **description** : The request succeeded.
      * **response body** :
        * [PidSet](#pidset)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)

#### GET/configurableComponents/configurations
  * **REST API path** : `/services/networkConfiguration/v1/configurableComponents/configurations`
  * **description** : Returns all of network component configurations available on the system. This request will return the `pid`, `ocd` and `properties`.
  * **responses** :
    * **200**
      * **description** : The request succeeded.
      * **response body** :
        * [ComponentConfigurationList](#componentconfigurationlist)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)

#### POST/configurableComponents/configurations/byPid
  * **REST API path** : `/services/networkConfiguration/v1/configurableComponents/configurations/byPid`
  * **description** : Returns a user selected set of network configurations. This request will return the `pid`, `ocd` and `properties`.
  * **request body** :
    * [PidSet](#pidset)
  * **responses** :
    * **200**
      * **description** : The request succeeded. If the network configuration for a given pid cannot be found, it will not be included in the result.
      * **response body** :
        * [ComponentConfigurationList](#componentconfigurationlist)
    * **400**
      * **description** : The request body is not valid JSON or it contains invalid parameters.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)

#### POST/configurableComponents/configurations/byPid/_default
  * **REST API path** : `/services/networkConfiguration/v1/configurableComponents/configurations/byPid/_default`
  * **description** : Returns the default network configuration for a given set of network component pids. The default configurations are generated basing on component definition only, user applied modifications will not be taken into account. This request will return the `pid`, `ocd` and `properties`.
  * **request body** :
    * [PidSet](#pidset)
  * **responses** :
    * **200**
      * **description** : The request succeeded. If the network configuration for a given pid cannot be found, it will not be included in the result.
      * **response body** :
        * [ComponentConfigurationList](#componentconfigurationlist)
    * **400**
      * **description** : The request body is not valid JSON or it contains invalid parameters.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)

#### PUT/configurableComponents/configurations/_update
  * **REST API path** : `/services/networkConfiguration/v1/configurableComponents/configurations/_update`
  * **description** : Updates a given set of network component configurations. This request can be also used to apply a network configuration snapshot.
  * **request body** :
    * [UpdateComponentConfigurationRequest](#updatecomponentconfigurationrequest)
  * **responses** :
    * **200**
      * **description** : The request succeeded.
    * **400**
      * **description** : The request body is not valid JSON or it contains invalid parameters.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)
    * **500**
      * **description** : In case of processing errors, the device will attempt to return a detailed error response containing a message describing the failure reason for each operation. The operation ids are the following: `update:$pid` for component update operations, where `$pid` is the pid of the instance, and `snapshot`, for the snapshot creation operation. In case of an unexpected failure, a generic error response will be returned.
      * **response body** :
        
        **Variants**:
          
          * **object**
            * [GenericFailureReport](#genericfailurereport)
          * **object**
            * [BatchFailureReport](#batchfailurereport)

!!! warning
    `factoryComponents` endopoints are available in the current version of Kura for future compatibility. Currently, as of Kura 5.4.0, there are no network related components that are factory components.

    The endopoints that should return a list of pids will always return an empty array, while those that should create, delete or update a component will always return a 500 error.

#### GET/factoryComponents
  * **REST API path** : `/services/networkConfiguration/v1/factoryComponents`
  * **description** : Returns the ids of the network component factories available on the device.
  * **responses** :
    * **200**
      * **description** : The factory pid set.
      * **response body** :
        * [PidSet](#pidset)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)

#### POST/factoryComponents
  * **REST API path** : `/services/networkConfiguration/v1/factoryComponents`
  * **description** : This is a batch request that allows to create one or more network factory component instances and optionally create a new snapshot.
  * **request body** :
    * [CreateFactoryComponentConfigurationsRequest](../../core-services/configuration-service-rest-v2.md#createfactorycomponentconfigurationsrequest)
  * **responses** :
    * **200**
      * **description** : The request succeeded.
    * **400**
      * **description** : The request body is not valid JSON or it contains invalid parameters.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)
    * **500**
      * **description** : In case of processing errors, the device will attempt to return a detailed error response containing a message describing the failure reason for each operation. The operation ids are the following: `create:$pid` for component creation operations, where `$pid` is the pid of the instance, and `snapshot`, for the snapshot creation operation. In case of an unexpected failure, a generic error response will be returned.

      * **response body** :
        
        **Variants**:
          
          * **object**
            * [GenericFailureReport](#genericfailurereport)
          * **object**
            * [BatchFailureReport](#batchfailurereport)

#### DEL/factoryComponents/byPid
  * **REST API path** : `/services/networkConfiguration/v1/factoryComponents/byPid`
  * **description** : This is a batch request that allows to delete one or more network factory component instances and optionally create a new snapshot.
  * **request body** :
    * [DeleteFactoryComponentConfigurationsRequest](../../core-services/configuration-service-rest-v2.md#deletefactorycomponentconfigurationsrequest)
  * **responses** :
    * **200**
      * **description** : The request succeeded.
    * **400**
      * **description** : The request body is not valid JSON or it contains invalid parameters.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)
    * **500**
      * **description** : In case of processing errors, the device will attempt to return a detailed error response containing a message describing the failure reason for each operation. The operation ids are the following: `delete:$pid` for component delete operations, where `$pid` is the pid of the instance, and `snapshot`, for the snapshot creation operation. In case of an unexpected failure, a generic error response will be returned.

      * **response body** :
        
        **Variants**:
          
          * **object**
            * [GenericFailureReport](#genericfailurereport)
          * **object**
            * [BatchFailureReport](#batchfailurereport)

#### GET/factoryComponents/ocd
  * **REST API path** : `/services/networkConfiguration/v1/factoryComponents/ocd`
  * **description** : Returns the OCD of the network components created by the factories available on the device without the need of creating an instance. This request returns the information related to all available network factories.
  * **responses** :
    * **200**
      * **description** : The request succeeded. The `pid` property of the received configurations will report the factory pid, the `ocd` field will contain the definition, the `properties` field will not be present.
      * **response body** :
        * [ComponentConfigurationList](#componentconfigurationlist)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)

#### POST/factoryComponents/ocd/byFactoryPid
  * **REST API path** : `/services/networkConfiguration/v1/factoryComponents/ocd/byFactoryPid`
  * **description** : Returns the OCD of the components created by the factories available on the device without the need of creating an instance. This request returns the information related to a user selected set of factories.
  * **request body** :
    * [PidSet](#pidset)
  * **responses** :
    * **200**
      * **description** : The request succeeded. The `pid` property of the received configurations will report the factory pid, the `ocd` field will contain the definition, the `properties` field will not be present. If the OCD for a given factory pid cannot be found, it will not be included in the result.
      * **response body** :
        * [ComponentConfigurationList](#componentconfigurationlist)
    * **400**
      * **description** : The request body is not valid JSON or it contains invalid parameters.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)


## JSON definitions

#### BatchFailureReport
An object that is returned by requests that involve multiple operations, when at least one operation failed. 
            This object report an error message for each of the operations that failed. The operations are identified by an id, see request documentation for more details.
            If an operation is not listed by this object, then the operation succeeded.
<br>**Properties**:

* **failures**: `array`
    The list of operations that failed.
    * array elements: `object`
      An object reporting details about an operation failure.
      <br>**Properties**:
      
      * **id**: `string`
          An identifier of the failed operation.
      * **message**: `string`
          A message describing the failure.

```json
{
  "failures": [
    {
      "id": "create:testComponent",
      "message": "Invalid parameter. pid testComponent already exists"
    },
    {
      "id": "create:otherComponent",
      "message": "Invalid parameter. pid otherComponent already exists"
    }
  ]
}
```

#### ComponentConfigurationList
Represents a list of component configurations.
<br>**Properties**:

* **configs**: `array`
    The component configurations
    * array elements: `object`
      * [ComponentConfiguration](../../core-services/configuration-service-rest-v2.md#componentconfiguration)

```json
{
    "configs": [
        {
            "pid": "org.eclipse.kura.net.admin.FirewallConfigurationService",
            "definition": {
                "ad": [
                    {
                        "name": "firewall.open.ports",
                        "description": "The list of firewall opened ports.",
                        "id": "firewall.open.ports",
                        "type": "STRING",
                        "cardinality": 0,
                        "defaultValue": "",
                        "isRequired": true
                    },
                    {
                        "name": "firewall.port.forwarding",
                        "description": "The list of firewall port forwarding rules.",
                        "id": "firewall.port.forwarding",
                        "type": "STRING",
                        "cardinality": 0,
                        "defaultValue": "",
                        "isRequired": true
                    },
                    {
                        "name": "firewall.nat",
                        "description": "The list of firewall NAT rules.",
                        "id": "firewall.nat",
                        "type": "STRING",
                        "cardinality": 0,
                        "defaultValue": "",
                        "isRequired": true
                    }
                ],
                "name": "FirewallConfigurationService",
                "description": "Firewall Configuration Service",
                "id": "org.eclipse.kura.net.admin.FirewallConfigurationService"
            },
            "properties": {
                "firewall.open.ports": {
                    "value": "22,tcp,0.0.0.0/0,eth0,,,,#;22,tcp,0.0.0.0/0,wlan0,,,,#;443,tcp,0.0.0.0/0,eth0,,,,#;443,tcp,0.0.0.0/0,wlan0,,,,#;4443,tcp,0.0.0.0/0,eth0,,,,#;4443,tcp,0.0.0.0/0,wlan0,,,,#;1450,tcp,0.0.0.0/0,eth0,,,,#;1450,tcp,0.0.0.0/0,wlan0,,,,#;5002,tcp,127.0.0.1/32,,,,,#;53,udp,0.0.0.0/0,eth0,,,,#;53,udp,0.0.0.0/0,wlan0,,,,#;67,udp,0.0.0.0/0,eth0,,,,#;67,udp,0.0.0.0/0,wlan0,,,,#;8000,tcp,0.0.0.0/0,eth0,,,,#;8000,tcp,0.0.0.0/0,wlan0,,,,#",
                    "type": "STRING"
                },
                "firewall.port.forwarding": {
                    "value": "",
                    "type": "STRING"
                },
                "firewall.nat": {
                    "value": "",
                    "type": "STRING"
                },
                "kura.service.pid": {
                    "value": "org.eclipse.kura.net.admin.FirewallConfigurationService",
                    "type": "STRING"
                },
                "service.pid": {
                    "value": "org.eclipse.kura.net.admin.FirewallConfigurationService",
                    "type": "STRING"
                }
            }
        },
        {
            "pid": "org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6",
            "definition": {
                "ad": [
                    {
                        "name": "firewall.ipv6.open.ports",
                        "description": "The list of firewall opened ports.",
                        "id": "firewall.ipv6.open.ports",
                        "type": "STRING",
                        "cardinality": 0,
                        "defaultValue": "",
                        "isRequired": true
                    },
                    {
                        "name": "firewall.ipv6.port.forwarding",
                        "description": "The list of firewall port forwarding rules.",
                        "id": "firewall.ipv6.port.forwarding",
                        "type": "STRING",
                        "cardinality": 0,
                        "defaultValue": "",
                        "isRequired": true
                    },
                    {
                        "name": "firewall.ipv6.nat",
                        "description": "The list of firewall NAT rules.",
                        "id": "firewall.ipv6.nat",
                        "type": "STRING",
                        "cardinality": 0,
                        "defaultValue": "",
                        "isRequired": true
                    }
                ],
                "name": "FirewallConfigurationServiceIPv6",
                "description": "Firewall Configuration Service IPV6",
                "id": "org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6"
            },
            "properties": {
                "firewall.ipv6.port.forwarding": {
                    "value": "",
                    "type": "STRING"
                },
                "firewall.ipv6.nat": {
                    "value": "",
                    "type": "STRING"
                },
                "firewall.ipv6.open.ports": {
                    "value": "1234,tcp,0:0:0:0:0:0:0:0/0,,,,,#",
                    "type": "STRING"
                },
                "kura.service.pid": {
                    "value": "org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6",
                    "type": "STRING"
                },
                "service.pid": {
                    "value": "org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6",
                    "type": "STRING"
                }
            }
        }
    ]
}
```

#### GenericFailureReport
An object reporting a failure message.
<br>**Properties**:

* **message**: `string`
    A message describing the failure.

```json
{
  "message": "An unexpected error occurred."
}
```

#### PidSet
Represents a set of pids or factory pids.
<br>**Properties**:

* **pids**: `array`
    The set of pids
    * array elements: `string`
      The pid

```json
{
    "pids": [
        "org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6",
        "org.eclipse.kura.net.admin.NetworkConfigurationService",
        "org.eclipse.kura.net.admin.FirewallConfigurationService"
    ]
}
```

#### UpdateComponentConfigurationRequest
An object that describes a set of configurations that need to be updated.
<br>**Properties**:

* **configs**: `array`
    The configurations to be updated. The `ocd` field can be omitted, it will be ignored if specified.
    * array elements: `object`
      * [ComponentConfiguration](../../core-services/configuration-service-rest-v2.md#componentconfiguration)
* **takeSnapshot**: `bool`
  * **optional** The `true` value will be used as default if not explicitly specified
    Defines whether a new snapshot should be created after that the component configurations have been applied.

```json
{
    "configs": [
        {
            "pid": "org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6",
            "properties": {
                "firewall.ipv6.open.ports": {
                    "value": "1234,tcp,0:0:0:0:0:0:0:0/0,,,,,#",
                    "type": "STRING"
                }
            }
        },
        {
            "pid": "org.eclipse.kura.net.admin.NetworkConfigurationService",
            "properties": {
                "net.interface.eth0.config.ip6.status": {
                    "value": "netIPv6StatusUnmanaged",
                    "type": "STRING"
                }
            }
        }
    ],
    "takeSnapshot": true
}
```