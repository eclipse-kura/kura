# Service Listing V1 REST APIs
!!! note

    This API can also be accessed via the RequestHandler with app-id: `SVCLIST-V1`.

The SVCLIST-V1 cloud request handler and the corresponding REST APIs allow to retrieve the identifiers (kura.service.pid) of the service running on the system that match user provided search criteria.


  * [Request definitions](#request-definitions)
    * [GET/servicePids](#getservicepids)
    * [POST/servicePids/byInterface](#postservicepidsbyinterface)
    * [POST/servicePids/byFilter](#postservicepidsbyfilter)
    * [POST/servicePids/satisfyingReference](#postservicepidssatisfyingreference)
    * [GET/factoryPids](#getfactorypids)
    * [POST/factoryPids/byInterface](#postfactorypidsbyinterface)
    * [POST/factoryPids/byFilter](#postfactorypidsbyfilter)
  * [JSON definitions](#json-definitions)
    * [InterfaceNames](#interfacenames)
    * [Reference](#reference)
    * [Filter](#filter)
    * [PropertyFilter](#propertyfilter)
    * [NotFilter](#notfilter)
    * [AndFilter](#andfilter)
    * [OrFilter](#orfilter)
    * [PidSet](#pidset)
    * [GenericFailureReport](#genericfailurereport)

## Request definitions
### GET/servicePids
  * **REST API path** : /services/serviceListing/v1/servicePids
  * **description** : Returns the pid of all services running in the framework.
  * **responses** :
      * **200**
          * **description** : An object reporting the pid of all services running in the framework
          * **response body** :
              * [PidSet](#pidset)
      * **500**
          * **description** : If an unexpected failure occurs while retrieving the service pid list.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### POST/servicePids/byInterface
  * **REST API path** : /services/serviceListing/v1/servicePids/byInterface
  * **description** : Returns the pid of the services providing all of the service interfaces specified in the request
  * **request body** :
      * [InterfaceNames](#interfacenames)
  * **responses** :
      * **200**
          * **description** : An object reporting the pid of the matching services.
          * **response body** :
              * [PidSet](#pidset)
      * **400**
          * **description** : If the request body is not syntactically correct.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)
      * **500**
          * **description** : If an unexpected failure occurs while retrieving the service pid list.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### POST/servicePids/byFilter
  * **REST API path** : /services/serviceListing/v1/servicePids/byFilter
  * **description** : Returns the pid of the services whose properties match the specified filter.
  * **request body** :
      * [Filter](#filter)
  * **responses** :
      * **200**
          * **description** : An object reporting the pid of the matching services.
          * **response body** :
              * [PidSet](#pidset)
      * **400**
          * **description** : If the request body is not syntactically correct.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)
      * **500**
          * **description** : If an unexpected failure occurs while retrieving the service pid list.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### POST/servicePids/satisfyingReference
  * **REST API path** : /services/serviceListing/v1/servicePids/satisfyingReference
  * **description** : Returns the pid of the services that provide an interface compatible with a Declarative Service reference. Reference examples are `KeystoreService` and `TruststoreKeystoreService` defined by the `org.eclipse.kura.ssl.SslManagerService` component.
  * **request body** :
      * [Reference](#reference)
  * **responses** :
      * **200**
          * **description** : An object reporting the pid of the matching services.
          * **response body** :
              * [PidSet](#pidset)
      * **400**
          * **description** : If the request body is not syntactically correct.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)
      * **500**
          * **description** : If an unexpected failure occurs while retrieving the service pid list.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### GET/factoryPids
  * **REST API path** : /services/serviceListing/v1/factoryPids
  * **description** : Returns the factory pids defined in the framework.
  * **responses** :
      * **200**
          * **description** : An object reporting the factory pids defined in the framework.
          * **response body** :
              * [PidSet](#pidset)
      * **500**
          * **description** : If an unexpected failure occurs while retrieving the factory pid list.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### POST/factoryPids/byInterface
  * **REST API path** : /services/serviceListing/v1/factoryPids/byInterface
  * **description** : Returns the factory pid of the services that provide all of the specified interfaces.
  * **request body** :
      * [InterfaceNames](#interfacenames)
  * **responses** :
      * **200**
          * **description** : An object reporting the matching factory pids.
          * **response body** :
              * [PidSet](#pidset)
      * **400**
          * **description** : If the request body is not syntactically correct.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)
      * **500**
          * **description** : If an unexpected failure occurs while retrieving the factory pid list.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### POST/factoryPids/byFilter
  * **REST API path** : /services/serviceListing/v1/factoryPids/byFilter
  * **description** : Returns the list of factory pids whose properties match the specified filter.
  * **request body** :
      * [Filter](#filter)
  * **responses** :
      * **200**
          * **description** : An object reporting the matching factory pids.
          * **response body** :
              * [PidSet](#pidset)
      * **400**
          * **description** : If the request body is not syntactically correct.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)
      * **500**
          * **description** : If an unexpected failure occurs while retrieving the factory pid list.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

## JSON definitions
### InterfaceNames
A list of serviceinterface names.

<br>**Properties**:

  * **interfaceNames**: `array` 
      The list of service interface names.

      * array element type: `string`
          A service interface name.

  

```json
{
  "interfaceNames": [
    "org.eclipse.kura.security.keystore.KeystoreService",
    "org.eclipse.kura.configuration.ConfigurableComponent"
  ]
}
```
### Reference
A object specifying a service pid and a reference name.

<br>**Properties**:

  * **pid**: `string` 
      The pid of the service containing the reference

  
  * **referenceName**: `string` 
      The reference name

  

```json
{
  "pid": "org.eclipse.kura.ssl.SslManagerService",
  "referenceName": "KeystoreService"
}
```
### PropertyFilter
A filter matching property keys and values. If the `value` property omitted the filter will match if the property is set, regardless of the value. If the service property value is of array or list type, the filter will match if at least one of the elements match.

<br>**Properties**:

  * **name**: `string` 
      The property name that should be matched, it must not contain spaces

  
  * **value**: `string` (**optional**)
      The property value that should be matched.

  

```json
{
  "name": "foo",
  "value": "bar"
}
```
```json
{
  "name": "foo"
}
```
### NotFilter
A filter that matches if filter specified by the "not" propertiy does not match.

<br>**Properties**:

  * **not**: `object` 
      * [Filter](#filter)
  

```json
{
  "not": {
    "name": "foo",
    "value": "bar"
  }
}
```
### AndFilter
A filter that matches if all filters specified by the "and" propertiy match.

<br>**Properties**:

  * **and**: `array` 
      A list of filters that should be combined with the and operator

      * array element type: `object`
          * [Filter](#filter)
  

```json
{
  "and": [
    {
      "name": "foo",
      "value": "bar"
    },
    {
      "name": "baz"
    }
  ]
}
```
### OrFilter
A filter that matches if any of the filters specified by the "or" propertiy match.

<br>**Properties**:

  * **or**: `array` 
      A list of filters that should be combined with the or operator

      * array element type: `object`
          * [Filter](#filter)
  

```json
{
  "or": [
    {
      "name": "foo",
      "value": "bar"
    },
    {
      "name": "baz"
    }
  ]
}
```
### Filter
A filter that operates on service properties. This object allow to specify basic property key/value matchers and the `and` `or` and `not` operators.

* **Variants**:
  * **object**
      * [PropertyFilter](#propertyfilter)
  * **object**
      * [NotFilter](#notfilter)
  * **object**
      * [AndFilter](#andfilter)
  * **object**
      * [OrFilter](#orfilter)

### PidSet
Represents a set of pids or factory pids.

<br>**Properties**:

  * **pids**: `array` 
      The set of pids

      * array element type: `string`
          The pid

  

```json
{
  "pids": [
    "org.eclipse.kura.cloud.app.command.CommandCloudApp",
    "org.eclipse.kura.cloud.CloudService",
    "org.eclipse.kura.cloud.publisher.CloudNotificationPublisher",
    "org.eclipse.kura.container.orchestration.provider.ContainerOrchestrationService",
    "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport",
    "org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2",
    "org.eclipse.kura.crypto.CryptoService",
    "org.eclipse.kura.data.DataService",
    "org.eclipse.kura.db.H2DbService",
    "org.eclipse.kura.deployment.agent"
  ]
}
```
### GenericFailureReport
An object reporting a failure message.

<br>**Properties**:

  * **message**: `string` 
      A message describing the failure.