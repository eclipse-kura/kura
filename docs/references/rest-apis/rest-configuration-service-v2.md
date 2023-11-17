# Configuration V2 REST APIs and CONF-V2 Request Handler

This page describes the CONF-V2 request handler and `configuration/v2` rest APIs. Accessing the REST APIs requires to use an identity with the `rest.configuration` permission assigned.

- [Request definitions](#request-definitions)
    - [GET/snapshots](#getsnapshots)
    - [GET/factoryComponents](#getfactorycomponents)
    - [POST/factoryComponents](#postfactorycomponents)
    - [DEL/factoryComponents/byPid](#delfactorycomponentsbypid)
    - [GET/factoryComponents/ocd](#getfactorycomponentsocd)
    - [POST/factoryComponents/ocd/byFactoryPid](#postfactorycomponentsocdbyfactorypid)
    - [GET/configurableComponents](#getconfigurablecomponents)
    - [GET/configurableComponents/pidsWithFactory](#getconfigurablecomponentspidswithfactory)
    - [GET/configurableComponents/configurations](#getconfigurablecomponentsconfigurations)
    - [POST/configurableComponents/configurations/byPid](#postconfigurablecomponentsconfigurationsbypid)
    - [POST/configurableComponents/configurations/byPid/_default](#postconfigurablecomponentsconfigurationsbypid_default)
    - [PUT/configurableComponents/configurations/_update](#putconfigurablecomponentsconfigurations_update)
    - [EXEC/snapshots/_write](#execsnapshots_write)
    - [EXEC/snapshots/_rollback](#execsnapshots_rollback)
    - [EXEC/snapshots/byId/_rollback](#execsnapshotsbyid_rollback)
- [JSON definitions](#json-definitions)
    - [PidAndFactoryPidSet](#pidandfactorypidset)
    - [SnapshotIdSet](#snapshotidset)
    - [PropertyType](#propertytype)
    - [ConfigurationProperty](#configurationproperty)
    - [ConfigurationProperties](#configurationproperties)
    - [Option](#option)
    - [AttributeDefinition](#attributedefinition)
    - [ObjectClassDefinition](#objectclassdefinition)
    - [ComponentConfiguration](#componentconfiguration)
    - [ComponentConfigurationList](#componentconfigurationlist)
    - [CreateFactoryComponentConfigurationsRequest](#createfactorycomponentconfigurationsrequest)
    - [UpdateComponentConfigurationRequest](#updatecomponentconfigurationrequest)
    - [DeleteFactoryComponentConfigurationsRequest](#deletefactorycomponentconfigurationsrequest)
    - [PidSet](#pidset)
    - [SnaphsotId](#snaphsotid)
    - [GenericFailureReport](#genericfailurereport)
    - [BatchFailureReport](#batchfailurereport)

## Request definitions
#### GET/snapshots
  * **REST API path** : `/services/configuration/v2/snapshots`
  * **description** : Returns the ids of the snapshots currently stored on the device.
  * **responses** :
    * **200**
      * **description** : The snapshot id set.
      * **response body** :
        * [SnapshotIdSet](#snapshotidset)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)

#### GET/factoryComponents
  * **REST API path** : `/services/configuration/v2/factoryComponents`
  * **description** : Returns the ids of the component factories available on the device.
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
  * **REST API path** : `/services/configuration/v2/factoryComponents`
  * **description** : This is a batch request that allows to create one or more factory component instances and optionally create a new snapshot.
  * **request body** :
    * [CreateFactoryComponentConfigurationsRequest](#createfactorycomponentconfigurationsrequest)
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
  * **REST API path** : `/services/configuration/v2/factoryComponents/byPid`
  * **description** : This is a batch request that allows to delete one or more factory component instances and optionally create a new snapshot.
  * **request body** :
    * [DeleteFactoryComponentConfigurationsRequest](#deletefactorycomponentconfigurationsrequest)
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
  * **REST API path** : `/services/configuration/v2/factoryComponents/ocd`
  * **description** : Returns the OCD of the components created by the factories available on the device without the need of creating an instance. This request returns the information related to all available factories.
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
  * **REST API path** : `/services/configuration/v2/factoryComponents/ocd/byFactoryPid`
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

#### GET/configurableComponents
  * **REST API path** : `/services/configuration/v2/configurableComponents`
  * **description** : Returns the list of the pids available on the system.
  * **responses** :
    * **200**
      * **description** : The request succeeded.
      * **response body** :
        * [PidSet](#pidset)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)

#### GET/configurableComponents/pidsWithFactory
  * **REST API path** : `/services/configuration/v2/configurableComponents/pidsWithFactory`
  * **description** : Returns the list of the pids available on the system, reporting also the factory pid where applicable.
  * **responses** :
    * **200**
      * **description** : The request succeeded.
      * **response body** :
        * [PidAndFactoryPidSet](#pidandfactorypidset)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)

#### GET/configurableComponents/configurations
  * **REST API path** : `/services/configuration/v2/configurableComponents/configurations`
  * **description** : Returns all of component configurations available on the system. This request will return the `pid`, `ocd` and `properties`.
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
  * **REST API path** : `/services/configuration/v2/configurableComponents/configurations/byPid`
  * **description** : Returns a user selected set of configurations. This request will return the `pid`, `ocd` and `properties`.
  * **request body** :
    * [PidSet](#pidset)
  * **responses** :
    * **200**
      * **description** : The request succeeded. If the configuration for a given pid cannot be found, it will not be included in the result.
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
  * **REST API path** : `/services/configuration/v2/configurableComponents/configurations/byPid/_default`
  * **description** : Returns the default configuration for a given set of component pids. The default configurations are generated basing on component definition only, user applied modifications will not be taken into account. This request will return the `pid`, `ocd` and `properties`.
  * **request body** :
    * [PidSet](#pidset)
  * **responses** :
    * **200**
      * **description** : The request succeeded. If the configuration for a given pid cannot be found, it will not be included in the result.
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
  * **REST API path** : `/services/configuration/v2/configurableComponents/configurations/_update`
  * **description** : Updates a given set of component configurations. This request can be also used to apply a configuration snapshot.
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

#### EXEC/snapshots/_write
  * **REST API path** : `/services/configuration/v2/snapshots/_write`
  * **description** : Requests the device to create a new snasphot based on the current defice configuration. If this request is used through REST API, the POST method must be used.
  * **responses** :
    * **200**
      * **description** : The request succeeded. The result is the identifier of the new snsapsot
      * **response body** :
        * [SnaphsotId](#snaphsotid)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)

#### EXEC/snapshots/_rollback
  * **REST API path** : `/services/configuration/v2/snapshots/_rollback`
  * **description** : Rollbacks the framework to the last saved snapshot if available. If this request is used through REST API, the POST method must be used.
  * **responses** :
    * **200**
      * **description** : The request succeeded. The result contains the id of the snapshot used for rollback.
      * **response body** :
        * [SnaphsotId](#snaphsotid)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)

#### EXEC/snapshots/byId/_rollback
  * **REST API path** : `/services/configuration/v2/snapshots/byId/_rollback`
  * **description** : Performs a rollback to the snapshot id specified by the user.
  * **responses** :
    * **200**
      * **description** : The request succeeded.
    * **400**
      * **description** : The request body is not valid JSON or it contains invalid parameters.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)
    * **500**
      * **description** : An unexpected internal error occurred.
      * **response body** :
        * [GenericFailureReport](#genericfailurereport)

## JSON definitions
#### PidAndFactoryPidSet
Represents a set of pids with the corresponding factory pid.
<br>**Properties**:

* **components**: `array`
    The set of pids and factory pids
    * array elements: `object`
      The pid and factory pid
      <br>**Properties**:
      
      * **pid**: `string`
          The component pid.
      * **factoryPid**: `string`
        * **optional** Can be missing if the described component is not a factory instance.
          The factory pid

```json
{
  "components": [
    {
      "factoryPid": "org.eclipse.kura.core.db.H2DbService",
      "pid": "org.eclipse.kura.db.H2DbService"
    },
    {
      "factoryPid": "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport",
      "pid": "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport"
    },
    {
      "pid": "org.eclipse.kura.web.Console"
    }
  ]
}
```
#### SnapshotIdSet
An object decribing a set of configuration snapshot ids
<br>**Properties**:

* **ids**: `array`
    The set of snapshot ids.
    * array elements: `number`
      A snapshot id.

```json
{
  "ids": [
    0,
    1638438049921,
    1638438146960,
    1638439710944,
    1638439717931,
    1638439734077,
    1638439767252,
    1638521986953,
    1638521993692,
    1638522572822
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
  "KeystoreService.target": {
    "type": "STRING",
    "value": "(kura.service.pid=HttpsKeystore)"
  },
  "https.client.auth.ports": {
    "type": "INTEGER",
    "value": [
      4443
    ]
  },
  "https.client.revocation.soft.fail": {
    "type": "BOOLEAN",
    "value": false
  },
  "https.ports": {
    "type": "INTEGER",
    "value": [
      443
    ]
  },
  "https.revocation.check.enabled": {
    "type": "BOOLEAN",
    "value": false
  },
  "kura.service.pid": {
    "type": "STRING",
    "value": "org.eclipse.kura.http.server.manager.HttpService"
  },
  "service.pid": {
    "type": "STRING",
    "value": "org.eclipse.kura.http.server.manager.HttpService"
  },
  "ssl.revocation.mode": {
    "type": "STRING",
    "value": "PREFER_OCSP"
  }
}
```
#### Option
An object describing an allowed element for a multi choiche field.
<br>**Properties**:

* **label**: `string`
  * **optional** This parameter may not be specified by component configuration.
    An user friendly label for the option.
* **value**: `string`
    The option value encoded as a string.

```json
{
  "label": "Value 1",
  "value": "1"
}
```
```json
{
  "value": "foo"
}
```
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
  "defaultValue": "false",
  "description": "Specifies whether the DB server is enabled or not.",
  "id": "db.server.enabled",
  "isRequired": true,
  "name": "db.server.enabled",
  "type": "BOOLEAN"
}
```
```json
{
  "defaultValue": "TCP",
  "description": "Specifies the server type, see http://www.h2database.com/javadoc/org/h2/tools/Server.html for more details.",
  "id": "db.server.type",
  "isRequired": true,
  "name": "db.server.type",
  "option": [
    {
      "label": "WEB",
      "value": "WEB"
    },
    {
      "label": "TCP",
      "value": "TCP"
    },
    {
      "label": "PG",
      "value": "PG"
    }
  ],
  "type": "STRING"
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
      "defaultValue": "false",
      "description": "The WatchdogService monitors CriticalComponents and reboots the system if one of them hangs. Once enabled the WatchdogService starts refreshing the watchdog device, which will reset the system if WatchdogService hangs.",
      "id": "enabled",
      "isRequired": true,
      "name": "Watchdog enable",
      "type": "BOOLEAN"
    },
    {
      "defaultValue": "10000",
      "description": "WatchdogService's refresh interval in ms of the Watchdog device. The value can be set between 1 and 60 seconds and should not be set to a value greater or equal to the Watchdog device's timeout value",
      "id": "pingInterval",
      "isRequired": true,
      "max": "60000",
      "name": "Watchdog refresh interval",
      "type": "INTEGER"
    },
    {
      "defaultValue": "/dev/watchdog",
      "description": "Watchdog device path e.g. /dev/watchdog.",
      "id": "watchdogDevice",
      "isRequired": true,
      "name": "Watchdog device path",
      "type": "STRING"
    },
    {
      "defaultValue": "/opt/eclipse/kura/data/kura-reboot-cause",
      "description": "The path for the file that will contain the reboot cause information.",
      "id": "rebootCauseFilePath",
      "isRequired": true,
      "name": "Reboot Cause File Path",
      "type": "STRING"
    }
  ],
  "description": "The WatchdogService handles the hardware watchdog of the platform.  The parameter define the ping periodicity of the hardware watchdog to ensure it does not reboot. The WatchdogService will reset the watchdog timeout, can disable it (where supported) with the Magic Character, but cannot set the refresh rate of a watchdog device.",
  "icon": [
    {
      "resource": "WatchdogService",
      "size": 32
    }
  ],
  "id": "org.eclipse.kura.watchdog.WatchdogService",
  "name": "WatchdogService"
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
  "definition": {
    "ad": [
      {
        "cardinality": 3,
        "description": "If set to a non empty list, REST API access will be allowed only on the specified ports. If set to an empty list, access will be allowed on all ports. Please make sure that the allowed ports are open in HttpService and Firewall configuration.",
        "id": "allowed.ports",
        "isRequired": false,
        "max": "65535",
        "min": "1",
        "name": "Allowed ports",
        "type": "INTEGER"
      }
    ],
    "description": "This service allows to configure settings related to Kura REST APIs",
    "id": "org.eclipse.kura.internal.rest.provider.RestService",
    "name": "RestService"
  },
  "pid": "org.eclipse.kura.internal.rest.provider.RestService",
  "properties": {
    "kura.service.pid": {
      "type": "STRING",
      "value": "org.eclipse.kura.internal.rest.provider.RestService"
    },
    "service.pid": {
      "type": "STRING",
      "value": "org.eclipse.kura.internal.rest.provider.RestService"
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
      "definition": {
        "ad": [
          {
            "defaultValue": "true",
            "description": "Whether or not to enable the ClockService",
            "id": "enabled",
            "isRequired": true,
            "name": "enabled",
            "type": "BOOLEAN"
          },
          {
            "defaultValue": "true",
            "description": "Whether or not to sync the system hardware clock after the system time gets set",
            "id": "clock.set.hwclock",
            "isRequired": true,
            "name": "clock.set.hwclock",
            "type": "BOOLEAN"
          },
          {
            "defaultValue": "java-ntp",
            "description": "Source for setting the system clock. Verify the availabiliy of the selected provider before activate it.",
            "id": "clock.provider",
            "isRequired": true,
            "name": "clock.provider",
            "option": [
              {
                "label": "java-ntp",
                "value": "java-ntp"
              },
              {
                "label": "ntpd",
                "value": "ntpd"
              },
              {
                "label": "chrony-advanced",
                "value": "chrony-advanced"
              }
            ],
            "type": "STRING"
          },
          {
            "defaultValue": "0.pool.ntp.org",
            "description": "The hostname that provides the system time via NTP",
            "id": "clock.ntp.host",
            "isRequired": true,
            "name": "clock.ntp.host",
            "type": "STRING"
          },
          {
            "defaultValue": "123",
            "description": "The port number that provides the system time via NTP",
            "id": "clock.ntp.port",
            "isRequired": true,
            "max": "65535",
            "min": "1",
            "name": "clock.ntp.port",
            "type": "INTEGER"
          },
          {
            "defaultValue": "10000",
            "description": "The NTP timeout in milliseconds",
            "id": "clock.ntp.timeout",
            "isRequired": true,
            "min": "1000",
            "name": "clock.ntp.timeout",
            "type": "INTEGER"
          },
          {
            "defaultValue": "0",
            "description": "The maximum number of retries for the initial synchronization (with interval clock.ntp.retry.interval). If set to 0 the service will retry forever.",
            "id": "clock.ntp.max-retry",
            "isRequired": true,
            "min": "0",
            "name": "clock.ntp.max-retry",
            "type": "INTEGER"
          },
          {
            "defaultValue": "5",
            "description": "When sync fails, interval in seconds between each retry.",
            "id": "clock.ntp.retry.interval",
            "isRequired": true,
            "min": "1",
            "name": "clock.ntp.retry.interval",
            "type": "INTEGER"
          },
          {
            "defaultValue": "3600",
            "description": "Whether or not to sync the clock and if so, the frequency in seconds.  If less than zero - no update, if equal to zero - sync once at startup, if greater than zero - the frequency in seconds to perform a new clock sync",
            "id": "clock.ntp.refresh-interval",
            "isRequired": true,
            "name": "clock.ntp.refresh-interval",
            "type": "INTEGER"
          },
          {
            "defaultValue": "/dev/rtc0",
            "description": "The RTC File Name. It defaults to /dev/rtc0. This option is not used if chrony-advanced option is selected in clock.provider.",
            "id": "rtc.filename",
            "isRequired": true,
            "name": "RTC File Name",
            "type": "STRING"
          },
          {
            "description": "Chrony configuration file.|TextArea",
            "id": "chrony.advanced.config",
            "isRequired": false,
            "name": "Chrony Configuration",
            "type": "STRING"
          }
        ],
        "description": "ClockService Configuration",
        "icon": [
          {
            "resource": "ClockService",
            "size": 32
          }
        ],
        "id": "org.eclipse.kura.clock.ClockService",
        "name": "ClockService"
      },
      "pid": "org.eclipse.kura.clock.ClockService",
      "properties": {
        "clock.ntp.host": {
          "type": "STRING",
          "value": "0.pool.ntp.org"
        },
        "clock.ntp.max-retry": {
          "type": "INTEGER",
          "value": 0
        },
        "clock.ntp.port": {
          "type": "INTEGER",
          "value": 123
        },
        "clock.ntp.refresh-interval": {
          "type": "INTEGER",
          "value": 3600
        },
        "clock.ntp.retry.interval": {
          "type": "INTEGER",
          "value": 5
        },
        "clock.ntp.timeout": {
          "type": "INTEGER",
          "value": 10000
        },
        "clock.provider": {
          "type": "STRING",
          "value": "java-ntp"
        },
        "clock.set.hwclock": {
          "type": "BOOLEAN",
          "value": true
        },
        "enabled": {
          "type": "BOOLEAN",
          "value": true
        },
        "kura.service.pid": {
          "type": "STRING",
          "value": "org.eclipse.kura.clock.ClockService"
        },
        "rtc.filename": {
          "type": "STRING",
          "value": "/dev/rtc0"
        },
        "service.pid": {
          "type": "STRING",
          "value": "org.eclipse.kura.clock.ClockService"
        }
      }
    },
    {
      "definition": {
        "ad": [
          {
            "defaultValue": "(kura.service.pid=org.eclipse.kura.ssl.SslManagerService)",
            "description": "Specifies, as an OSGi target filter, the pid of the SslManagerService used to create SSL connections for downloading packages.",
            "id": "SslManagerService.target",
            "isRequired": true,
            "name": "SslManagerService Target Filter",
            "type": "STRING"
          }
        ],
        "description": "This service is responsible of managing the deployment packages installed on the system.",
        "id": "org.eclipse.kura.deployment.agent",
        "name": "DeploymentAgent"
      },
      "pid": "org.eclipse.kura.deployment.agent",
      "properties": {
        "SslManagerService.target": {
          "type": "STRING",
          "value": "(kura.service.pid=org.eclipse.kura.ssl.SslManagerService)"
        },
        "kura.service.pid": {
          "type": "STRING",
          "value": "org.eclipse.kura.deployment.agent"
        },
        "service.pid": {
          "type": "STRING",
          "value": "org.eclipse.kura.deployment.agent"
        }
      }
    }
  ]
}
```
#### CreateFactoryComponentConfigurationsRequest
An object describing a factory component instance creation request.
<br>**Properties**:

* **configs**: `array`
    The set of configurations to be created
    * array elements: `object`
      An object describing a factory component confguration.
      <br>**Properties**:
      
      * **pid**: `string`
          The component pid.
      * **factoryPid**: `string`
          The component factory pid
      * **properties**: `object`
        * **optional** If omitted, the component innstance will be created with default configuration.
          * [ConfigurationProperties](#configurationproperties)
* **takeSnapshot**: `bool`
  * **optional** The `true` value will be used as default if not explicitly specified
    Defines whether a new snapshot should be created after that the factory component configuration instances have been created.

```json
{
  "configs": [
    {
      "factoryPid": "org.eclipse.kura.core.db.H2DbServer",
      "pid": "testComponent",
      "properties": {
        "db.server.type": {
          "type": "STRING",
          "value": "WEB"
        }
      }
    },
    {
      "factoryPid": "org.eclipse.kura.core.db.H2DbServer",
      "pid": "thirdComponent"
    }
  ],
  "takeSnapshot": true
}
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
      "pid": "org.eclipse.kura.cloud.app.command.CommandCloudApp",
      "properties": {
        "command.enable": {
          "type": "BOOLEAN",
          "value": true
        },
        "command.timeout": {
          "type": "INTEGER",
          "value": 60
        }
      }
    },
    {
      "pid": "org.eclipse.kura.position.PositionService",
      "properties": {
        "parity": {
          "type": "STRING",
          "value": 0
        }
      }
    }
  ],
  "takeSnapshot": true
}
```
#### DeleteFactoryComponentConfigurationsRequest
An object describing a factory component instance delete request.
<br>**Properties**:

* **pids**: `array`
    The list of the pids of the factory component instances to be deleted.
    * array elements: `string`
      The component pid.
* **takeSnapshot**: `bool`
  * **optional** The `true` value will be used as default if not explicitly specified
    Defines whether a new snapshot should be created after that the factory component configuration instances have been deleted.

```json
{
  "pids": [
    "testComponent",
    "otherComponent"
  ],
  "takeSnapshot": true
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
    "org.eclipse.kura.deployment.agent",
    "org.eclipse.kura.clock.ClockService"
  ]
}
```
#### SnaphsotId
An object describing the identifier of a configuration snapshot.
<br>**Properties**:

* **id**: `number`
    The snapshot id

```json
{
  "id": 163655959932
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