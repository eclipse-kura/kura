# Network Configuration V1 REST APIs

This section describes the `networkConfiguration/v1` REST APIs, that allow to retrieve information about the network configuration of the running system and in particular:

- General network configuration
- Ip4 firewall configuration
- Ip6 firewall configuration

To access these REST APIs, an identity with `rest.network.configuration` permission assigned is required.

- [Request definitions](#request-definitions)
    - [GET/configurableComponents](#getconfigurablecomponents)
    - [GET/configurableComponents/configurations](#getconfigurablecomponentsconfigurations)
    - [POST/configurableComponents/configurations/byPid](#postconfigurablecomponentsconfigurationsbypid)
    - [POST/configurableComponents/configurations/byPid/_default](#postconfigurablecomponentsconfigurationsbypid_default)
    - [PUT/configurableComponents/configurations/_update](#putconfigurablecomponentsconfigurations_update)
- [JSON definitions](#json-definitions)
    - [AttributeDefinition](#attributedefinition)
    - [BatchFailureReport](#batchfailurereport)
    - [ComponentConfiguration](#componentconfiguration)
    - [ComponentConfigurationList](#componentconfigurationlist)
    - [ConfigurationProperty](#configurationproperty)
    - [ConfigurationProperties](#configurationproperties)
    - [GenericFailureReport](#genericfailurereport)
    - [ObjectClassDefinition](#objectclassdefinition)
    - [PidSet](#pidset)
    - [PropertyType](#propertytype)
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


## JSON definitions

#### AttributeDefinition
A descriptor of a configuration property.
<br>**Properties**:

* **id**: `string`
    The id of the attribute definition. This field corresponds to the configuration property name.
* **type**: `string (enumerated)`
    * [PropertyType](#propertytype)
* **name**: `string`
  * **optional** This parameter may not be specified by component configuration.
    An user friendly name for the property.
* **description**: `string`
  * **optional** This parameter may not be specified by component configuration.
    An user friendly description for the property.
* **cardinality**: `number`
    An integer describing the property cardinality.
            If the value is 0, then the property is a singleton value (not an array), if it is > 0, then the configuration property is an array and this property specifies the maximum allowed array length.
* **min**: `string`
  * **optional** This parameter may not be specified by component configuration. If not specified, the property does not have a minimum value.
    Specifies the minimum value for this property as a string.
* **max**: `string`
  * **optional** This parameter may not be specified by component configuration. If not specified, the property does not have a maximum value.
    Specifies the maximum value for this property as a string.
* **isRequired**: `bool`
    Specifies whether the configuration parameter is required or not.
* **defaultValue**: `string`
  * **optional** This parameter may not be specified by component configuration.
    Specifies the default value for this property as a string.
* **option**: `array`
  * **optional** If specified, describes a set of allowed values for the configuration property.
    The allowed values for this configuration properties
    * array elements: `object`
      * [Option](#option)

```json
{
    "name": "firewall.nat",
    "description": "The list of firewall NAT rules.",
    "id": "firewall.nat",
    "type": "STRING",
    "cardinality": 0,
    "defaultValue": "",
    "isRequired": true
}
```

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

#### ComponentConfiguration
Describes a component configuration.
<br>**Properties**:

* **pid**: `string`
    The identifier of this configuration.
* **ocd**: `object`
  * **optional** Can be omitted in some requests and responses, see request documentation for more information.
    * [ObjectClassDefinition](#objectclassdefinition)
* **properties**: `object`
  * **optional** Can be omitted in some requests and responses, see request documentation for more information.
    * [ConfigurationProperties](#configurationproperties)

```json
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
}
```

#### ComponentConfigurationList
Represents a list of component configurations.
<br>**Properties**:

* **configs**: `array`
    The component configurations
    * array elements: `object`
      * [ComponentConfiguration](#componentconfiguration)

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

#### ConfigurationProperty
An object describing a configuration property.
<br>**Properties**:

* **type**: `string (enumerated)`
    * [PropertyType](#propertytype)
* **value**: `variant`
  * **optional** In requests, this field can be omitted or set to `null` to assign the `null` value to a non required configuration property.
    Describes the property value. The value type depends on the **type** property.
    **Variants**:
      
      * **number**
        If type is `LONG`, `DOUBLE`, `FLOAT`, `INTEGER`, `BYTE` or `SHORT` and the property does not represent an array.
      * **string**
        If type is `STRING`, `PASSWORD` or `CHAR` and the property is not an array. In case of `CHAR` type, the value must have a length of 1.
      * **bool**
        If type is `BOOLEAN` and the property is not an array
      * **array**
        If type is `LONG`, `DOUBLE`, `FLOAT`, `INTEGER`, `BYTE` or `SHORT` and the property represents an array.
        * array elements: `number`
          The property values as numbers.
      * **array**
        If type is `STRING`, `PASSWORD` or `CHAR` and the property is an array.
        * array elements: `string`
          The property values as strings. In case of `CHAR` type, the values must have a length of 1.
      * **array**
        If type is `BOOLEAN` and the property is an array
        * array elements: `bool`
          The property values as booleans.

```json
{
  "type": "STRING",
  "value": "foo"
}
```
```json
{
  "type": "STRING",
  "value": [
    "foo",
    "bar"
  ]
}
```
```json
{
  "type": "INTEGER",
  "value": 12
}
```
```json
{
  "type": "LONG",
  "value": [
    1,
    2,
    3,
    4
  ]
}
```
```json
{
  "type": "PASSWORD",
  "value": "myPassword"
}
```
```json
{
  "type": "PASSWORD",
  "value": [
    "my",
    "password",
    "array"
  ]
}
```

#### ConfigurationProperties
An object representing a set of configuration properties. The members of this object represent configuration property, the member names represent the configuration property ids. This object can have a variable number of members.
<br>**Properties**:

* **propertyName**: `object`
    * [ConfigurationProperty](#configurationproperty)

```json
{
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

#### ObjectClassDefinition
Provides some metadata information about a component configuration.
<br>**Properties**:

* **ad**: `array`
    The metadata about the configuration properties.
    * array elements: `object`
      * [AttributeDefinition](#attributedefinition)
* **icon**: `array`
  * **optional** Can be missing if the OCD does not define icons.
    A list of icons that visually represent the configuration.
    * array elements: `object`
      
      <br>**Properties**:
      
      * **resource**: `string`
          An identifier of the icon image resource.
      * **size**: `number`
          The icon width and height in pixels.
* **name**: `string`
    A user friendly name for the component configuration.
* **description**: `string`
    A user friendly description of the component configuration.
* **id**: `string`
    An identifier of the component configuration.

```json
{
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

#### PropertyType
A string that describes the type of a configuration property.
* Possible values
  * `STRING`
  * `LONG`
  * `DOUBLE`
  * `FLOAT`
  * `INTEGER`
  * `BYTE`
  * `CHAR`
  * `BOOLEAN`
  * `SHORT`
  * `PASSWORD`

```json
"STRING"
```

#### UpdateComponentConfigurationRequest
An object that describes a set of configurations that need to be updated.
<br>**Properties**:

* **configs**: `array`
    The configurations to be updated. The `ocd` field can be omitted, it will be ignored if specified.
    * array elements: `object`
      * [ComponentConfiguration](#componentconfiguration)
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