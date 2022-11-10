# MQTT Namespace

This section provides guidelines on how to structure the MQTT topic namespace for messaging interactions with applications running on IoT gateway devices.

Interactions may be solicited by a remote server to the gateway using a request/response messaging model, or unsolicited when the gateway simply reports messages or events to a remote server based on periodic or event-driven patterns.

The table below defines some basic terms used in this document:

| Term | Description |
| --- | --- |
| **`account_name`** | Identifies a group of devices and users. It can be seen as partition of the MQTT topic namespace. For example, access control lists can be defined so that users are only given access to the child topics of a given `account_name`. |
| **`client_id`** | Identifies a single gateway device within an account (typically the MAC address of a gateway’s primary network interface). The **client_id** maps to the Client Identifier (Client ID) as defined in the MQTT specifications. |
| **`app_id`** | Unique string identifier for application (e.g., “CONF-V1”, “CONF-V2”, etc.). |
| **`resource_id`** | Identifies a resource(s) that is owned and managed by a particular application. Management of resources (e.g., sensors, actuators, local files, or configuration options) includes listing them, reading the latest value, or updating them to a new value. A **resource_id** may be a hierarchical topic, where, for example, “sensors/temp” may identify a temperature sensor and “sensor/hum” a humidity sensor. |


A gateway, as identified by a specific **client_id** and belonging to a particular **account_name**, may have one or more applications running
on it (e.g., “app_id1”, “app_id2”, etc.). Each application can manage one or more resources identified by a distinct **resource_id**(s).

Based on this criterion, an IoT application running on an IoT gateway may be viewed in terms of the resources it owns and manages as well as
the unsolicited events it reports.



## MQTT Request/Response Conversations

Solicited interactions require a request/response message pattern to be established over MQTT. To initiate a solicited conversation, a remote
server first sends a request message to a given application running on a specific device and then waits for a response.

To ensure the delivery of request messages, applications that support request/response conversations via MQTT should subscribe to the
following topic on startup:

```
$EDC/account_name/client_id/app_id/#
```

The **$EDC** prefix is used to mark topics that are used as *control topics* for remote management. This prefix distinguishes control topics
from data topics that are used in unsolicited reports and marks the associated messages as transient (not to be stored in the historical
data archive, if present).

!!! note
    While Kura currently requires “$EDC” as the prefix for control topics, this prefix may change in the future for the following reasons:

    * MQTT 3.1.1 discourages the use of topic starting with “$” for application purposes.

    * As a binding of LWM2M over MQTT is taking shape, it would make sense to use a topic prefix for management messages like “LWM2M” or similar abbreviations (e.g. "LW2”, “LWM”).
 
A requester (i.e., the remote server) initiates a request/response conversation through the following events:

1.  Generating a conversation identifier known as a **request.id** (e.g., by concatenating a random number to a timestamp)

2.  Subscribing to the topic where the response message will be published, where **requester.client.id** is the client ID of the requester, such as:

    ```
    $EDC/account_name/requester.client.id/app_id/REPLY/request.id
    ```

3.  Sending the request message to the appropriate application-specific topic with the following fields in the payload:
    * **request.id** (identifier used to match a response with a request)
    * **requester.client.id** (client ID of the requester)

The application receives the request, processes it, and responds on a REPLY topic structured as:

```
$EDC/account_name/requester.client.id/app_id/REPLY/request.id
```

!!! note
    While this recommendation does not mandate the format of the message payload, which is application-specific, it is important that the **request.id** and **requester.client.id** fields are included in the payload. Kura leverages an MQTT payload encoded through Google Protocol Buffers. Kura includes the **request.id** and the **requester.client.id** as two named metrics of the Request messages. The Kura payload definition can be found [here](https://github.com/eclipse/kura/blob/develop/kura/org.eclipse.kura.core.cloud/src/main/protobuf/kurapayload.proto).

Once the response for a given request is received, the requester unsubscribes from the REPLY topic.

### MQTT Request/Response Example

The following sample request/response conversation shows the device configuration being provided for an application:

```
account_name: guest
device client_id: F0:D2:F1:C4:53:DB
app_id: CONF-V1
Remote Service Requester client_id: 00:E0:C7:01:02:03
```

The remote server publishes a request message similar to the following:

* Request Topic:
    * **$EDC/guest/F0:D2:F1:C4:53:DB/CONF-V1/GET/configurations**
  
* Request Payload:
    * **request.id: 1363603920892-8078887174204257595**
    * **requester.client.id: 00:E0:C7:01:02:03**

The gateway device replies with a response message similar to the following:

* Response Topic:
    * **$EDC/guest/00:E0:C7:01:02:03/CONF-V1/REPLY/1363603920892-8078887174204257595**
  
* Response Payload, where the following properties are mandatory:
    * **response.code** </br>Possible response code values include:
        * **200 (RESPONSE_CODE_OK)**
        * **400 (RESPONSE_CODE_BAD_REQUEST)**
        * **404 (RESPONSE_CODE_NOTFOUND)**
        * **500 (RESPONSE_CODE_ERROR)**
        * **response.exception.message** (value is null or an exception
        message)
        **response.exception.message** (value is null or an exception stack
        trace)

!!! note
    In addition to the mandatory properties, the response payload may also have custom properties whose description is beyond the scope of this document.

It is recommended that the requester server employs a timeout to control the length of time that it waits for a response from the gateway device. If a response is not received within the timeout interval, the server can expect that either the device or the application is offline.



## MQTT Remote Resource Management

A remote server interacts with the application’s resources through *read*, *create* and *update*, *delete,* and *execute* operations. These operations are based on the previously described request/response conversations.

### Read Resources

An MQTT message published on the following topic is a read request for the resource identified by the **resource_id**:

```
$EDC/account_name/client_id/app_id/GET/resource_id
```

The receiving application responds with a REPLY message containing the latest value of the requested resource.

The **resource_id** is application specific and may be a hierarchical topic. It is recommended to design resource identifiers following the best practices established for REST API.

For example, if an application is managing a set of sensors, a read request issued to the topic "**$EDC/account_name/client_id/app_id/GET/sensors**" will reply with the latest values for all sensors.

Similarly, a read request issued to the topic "**$EDC/account_name/client_id/app_id/GET/sensors/temp**" will reply with the latest value for only a temperature sensor that is being managed by the application.

### Create or Update Resources

An MQTT message published on the following topic is a create or update request for the resource identified by the **resource_id**:

```
$EDC/account_name/client_id/app_id/PUT/resource_id
```

The receiving application creates the specified resource (or updates it if it already exists) with the value supplied in the message payload and responds with a REPLY message.

As in the read operations, the **resource_id** is application specific and may be a hierarchical topic. It is recommended to design resource identifiers following the best practices established for REST API. For example, to set the value for an actuator, a message can be published to the topic "**$EDC/account_name/client_id/app_id/PUT/actuator/1**" with the new value suplliied in the message payload.

### Delete Resources

An MQTT message published on the following topic is a delete request for the resource identified by the **resource_id**:

```
$EDC/account_name/client_id/app_id/DEL/resource_id
```

The receiving application deletes the specified resource, if it exists, and responds with a REPLY message.

### Execute Resources

An MQTT message published on the following topic is an execute request for the resource identified by the **resource_id**:

```
$EDC/account_name/client_id/app_id/EXEC/resource_id
```

The receiving application executes the specified resource, if it exists, and responds with a REPLY message. The semantics of the execute operation is application specific.

### Other Operations

The IoT application may respond to certain commands, such as taking a snapshot of its configuration or executing an OS-level command. The following topic namespace is recommended for command operations:

```
$EDC/account_name/client_id/app_id/EXEC/command_name
```

An MQTT message published with this topic triggers the execution of the associated command. The EXEC message may contain properties in the MQTT payload that can be used to parameterize the command execution.



## MQTT Unsolicited Events

IoT applications have the ability to send unsolicited messages to a remote server using events to periodically report data readings from their resources, or to report special events and observed conditions.

!!! tip
    It is recommended to *not* use MQTT control topics for unsolicited events, and subsequently, to avoid the $EDC topic prefix.

Event MQTT topics generally follow the pattern shown below to report unsolicited data observations for a given resource:

```
account_name/client_id/app_id/resource_id
```



## Discoverability

The MQTT namespace guidelines in this document do not address remote discoverability of a given device’s applications and its resources. The described interaction pattern can be easily adopted to define an application whose only responsibility is reporting the device profile in terms of installed applications and available resources.



## Remote OSGi Management via MQTT

The concepts previously described have been applied to develop a solution that allows for the remote management of certain aspects of an OSGi container through the MQTT protocol, including:

  * Remote deployment of application bundles
  
  * Remote start and stop of services
  
  * Remote read and update of service configurations

The following sections describe the MQTT topic namespaces and the application payloads used to achieve the remote management of an OSGi container via MQTT.

!!! note
    For the scope of this document, some aspects concerning the encoding and compressing of the payload are not included.

The applicability of the remote management solution, as inspired by the OSGi component model, can be extended beyond OSGi as the contract with the managing server based on MQTT topics and XML payloads.

### Remote OSGi ConfigurationAdmin Interactions via MQTT

An application bundle is installed in the gateway to allow for remote management of the configuration properties of the services running in the OSGi container.

For information about the OSGi Configuration Admin Service and the OSGi Meta Type Service, please refer to the [OSGi Service Platform Service R7 Specifications](http://docs.osgi.org/specification/osgi.core/7.0.0/).

The **app_id** for the remote configuration service of an MQTT application is “**CONF-V1**”. The resources it manages are the configuration properties of the OSGi services. Service configurations are represented in XML format.

The following service configuration XML message is an example of a watchdog service:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:configuration xmlns:ns2=<http://eurotech.com/esf/2.0
  xmlns=<http://www.osgi.org/xmlns/metatype/v1.2.0>
  pid="org.eclipse.kura.watchdog.WatchdogService">

  <OCD id="org.eclipse.kura.watchdog.WatchdogService"
    name="WatchdogService"
    description="WatchdogService Configuration">

  	<Icon resource="WatchdogService"/>

    <AD id="watchdog.timeout"
      name="watchdog.timeout"
      required="true"
      default="10000"
      cardinality="0"
      type="Integer"
      description=""/>
  </OCD>

  <ns2:properties>
    <ns2:property type="Integer" array="false" name="watchdog.timeout">
      <ns2:value>10000</ns2:value>
    </ns2:property>
  </ns2:properties>
</ns2:configuration>
```

The service configuration XML message is comprised of the following parts:

* The **Object Class Definition** (OCD), which describes the service attributes that may be configured. (The syntax of the OCD element is described in the [OSGi Service Platform Service R7 Specifications](http://docs.osgi.org/specification/osgi.core/7.0.0/))

* The **properties** element, which contains one or more properties with their associated type and values. The type name must match the name provided in the corresponding attribute definition identifier (AD id) contained in the OCD.

The “CONF-V1” application supports the read and update resource operations as described in the following sections.

#### Read All Configurations

This operation provides all service configurations for which remote administration is supported.

* Request Topic:
    * **$EDC/account_name/client_id/CONF-V1/GET/configurations**
  
* Request Payload:
    * Nothing application-specific beyond the request ID and requester client ID
  
* Response Payload:
    * Configurations of all the registered services serialized in XML format

#### Read Configuration for a Given Service

This operation provides configurations for a specific service that is identified by an OSGi service persistent identifier **pid**.

* Request Topic:
    * **$EDC/account_name/client_id/CONF-V1/GET/configurations/pid**
  
* Request Payload:
    * Nothing application-specific beyond the request ID and requester client ID
  
* Response Payload:
    * Configurations of the registered service identified by a **pid** serialized in XML format

#### Update All Configurations

This operation remotely updates the configuration of a set of services.

* Request Topic:
    * **$EDC/account_name/client_id/CONF-V1/PUT/configurations**
  
* Request Payload:
    * Service configurations serialized in XML format
  
* Response Payload:
    * Nothing application-specific beyond the response code

#### Update the Configuration of a Given Service

This operation remotely updates the configuration of the service identified by a **pid**.

* Request Topic:
    * **$EDC/account_name/client_id/CONF-V1/PUT/configurations/pid**
  
* Request Payload:
    * Service configurations serialized in XML format
  
* Response Payload:
    * Nothing application-specific

#### Example Management Web Application

The previously described read and update resource operations can be leveraged to develop a web application that allows for remote OSGi service configuration updates via MQTT though a web user-interface.

The screen capture that follows shows an example administration application where, for a given IoT gateway, a list of all configurable services is presented to the administrator.

![Kapua Device Configuration](./images/kapua_config.png)

When one such service is selected, a form is dynamically generated based on the metadata provided in the service OCD. This form includes logic to handle different attribute types, validate acceptable value ranges, and render optional values as drop-downs. When the form is submitted, the new values are communicated to the device through an MQTT resource update message.



## Remote OSGi DeploymentAdmin Interactions via MQTT

An application is installed in the gateway to allow for the remote management of the deployment packages installed in the OSGi container.

For information about the OSGi Deployment Admin Service, please refer to the [OSGi Service Platform Service R7 Specifications](http://docs.osgi.org/specification/osgi.core/7.0.0/).

The **app_id** for the remote deployment service of an MQTT application is “**DEPLOY-V2**”. The resources it manages are the packages deployed in the OSGi container. Deployment packages are represented in XML format.

The following XML message is an example of a service deployment:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>

<packages>
  <package>
    <name>esf</name>
    <version>1.0.0</version>
    <bundles>
      <bundle>
        <name>org.hsqldb.hsqldb</name>
        <version>2.2.9</version>
      </bundle>
      <bundle>
        <name>org.eclipse.kura.linux</name>
        <version>1.0.0.SNAPSHOT</version>
      </bundle>
      <bundle>
        <name>javax.bluetooth</name>
        <version>2.1.1</version>
      </bundle>
      <bundle>
        <name>org.eclipse.kura.protocol.modbus</name>
        <version>1.0.0.SNAPSHOT</version>
      </bundle>
      <bundle>
        <name>org.apache.commons.net</name>
        <version>3.1.0.v201205071737</version>
      </bundle>
      <bundle>
        <name>javax.usb.api</name>
        <version>1.0.2</version>
      </bundle>
      <bundle>
        <name>org.apache.servicemix.bundles.protobuf-java</name>
        <version>2.4.1.1</version>
      </bundle>
      <bundle>
        <name>org.eclipse.kura.protocol.pcn</name>
        <version>1.0.0.SNAPSHOT</version>
      </bundle>
      <bundle>
        <name>javax.usb.common</name>
        <version>1.0.2</version>
      </bundle>
      <bundle>
        <name>org.eclipse.kura.core</name>
        <version>1.0.0.SNAPSHOT</version>
      </bundle>
      <bundle>
        <name>org.eclipse.kura.api</name>
        <version>1.0.0.SNAPSHOT</version>
      </bundle>
      <bundle>
        <name>org.eclipse.kura.web</name>
        <version>1.0.0.SNAPSHOT</version>
      </bundle>
      <bundle>
        <name>javax.comm</name>
        <version>2.2.0</version>
      </bundle>
      <bundle>
        <name>org.eclipse.paho.mqtt-client</name>
        <version>1.0.1.SNAPSHOT</version>
      </bundle>
      <bundle>
        <name>edc-client</name>
        <version>2.1.0.SNAPSHOT</version>
      </bundle>
    </bundles>
  </package>
</packages>
```

The deployment package XML message is comprised of the following package elements:

* Symbolic name

* Version

* Bundles that are managed by the deployment package along with their symbolic name and version

The “DEPLOY-V2” application supports the *read*, *start/stop*, and *install/uninstall* resource operations as described in the following sections.

### Read All Deployment Packages

This operation provides the deployment packages installed in the OSGi framework.

* Request Topic:
    * **$EDC/account_name/client_id/DEPLOY-V2/GET/packages**
  
* Request Payload:
    * Nothing application-specific beyond the request ID and requester client ID
  
* Response Payload:
    * Installed deployment packages serialized in XML format

### Install a Deployment Package

This operation installs a deployment package in the OSGi framework.

* Request Topic:
    * **$EDC/account_name/client_id/DEPLOY-V2/EXEC/install**
  
* Request Payload:
    * The following application-specific properties in addition to the request ID and requester client ID:
        * A **deploy.url** property that provides the URL to be used by the receiving application to download the deployment package.
        * Alternatively, the deployment package is present in a body property of the MQTT payload. The **deploy.filename** property provides the filename of the deployment package on the receiving device.
    
* Response Payload:
    * **deploy.pkg.name** provides the symbolic name of the deployment package
    * **deploy.pkg.version** provides the version of the deployment package

### Uninstall a Deployment Package

This operation uninstalls a deployment package.

* Request Topic:
    * **$EDC/account_name/client_id/DEPLOY-V2/EXEC/uninstall**
  
* Request Payload:
    * **deploy.pkg.name** provides the symbolic name of the deployment package
  
* Response Payload:
    * Nothing application-specific beyond the response code

### Read All Bundles

This operation provides all the bundles installed in the OSGi framework.

* Request Topic:
    * **$EDC/account_name/client_id/DEPLOY-V2/GET/bundles**
  
* Request Payload:
    * Nothing application-specific beyond the request ID and requester client ID
  
* Response Payload:
    * Installed bundles serialized in XML format

The following XML message is an example of a bundle:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<bundles>
   <bundle>
     <name>org.eclipse.osgi</name>
     <version>3.8.1.v20120830-144521</version>
     <id>0</id>
     <state>ACTIVE</state>
   </bundle>
   <bundle>
     <name>org.eclipse.equinox.cm</name>
     <version>1.0.400.v20120522-1841</version>
     <id>1</id>
     <state>ACTIVE</state>
   </bundle>
</bundles>
```

The bundle XML message is comprised of the following bundle elements:

* Symbolic name

* Version

* ID

* State

### Start a Bundle

This operation starts a bundle identified by its ID.

* Request Topic:
    * **$EDC/account_name/client_id/DEPLOY-V2/EXEC/start/bundle_id**
  
* Request Payload:
    * Nothing application-specific beyond the request ID and requester client ID
  
* Response Payload:
    * Nothing application-specific beyond the response code

### Stop a Bundle

This operation stops a bundle identified by its ID.

* Request Topic:
    * **$EDC/account_name/client_id/DEPLOY-V2/EXEC/stop/bundle_id**
  
* Request Payload:
    * Nothing application-specific beyond the request ID and requester client ID
  
* Response Payload:
    * Nothing application-specific beyond the response code



## Remote Gateway Inventory via MQTT

An application is installed in the gateway to allow for the remote query of the resources installed in the OSGi container and the underlying OS.

The **app_id** for the remote inventory service of an MQTT application is “**INVENTORY-V1**”.  The service allows retrieving all the different resources available/installed on the gateway. The service supports the following resources:

- [BUNDLES](#inventory-bundles) : represents a OSGi Bundle
- [DP](#inventory-deployment-packages) : represents a OSGi Deployment Package
- [DEB](#inventory-system-packages-debrpmapk) : represents a Linux Debian package
- [RPM](#inventory-system-packages-debrpmapk) : represents a Linux RPM package
- [APK](#inventory-system-packages-debrpmapk) : represents a Linux Alpine APK package
- [DOCKER](#inventory-containers) : represents a container

The resources are represented in JSON format. The following message is an example of a service deployment:

```json
{
    "inventory":[
        {
            "name":"adduser",
            "version":"3.118",
            "type":"DEB"
        },
        {
            "name":"io.netty.transport-native-unix-common",
            "version":"4.1.34.Final",
            "type":"BUNDLE"
        },
    ]
}
```

The inventory JSON message is comprised of the following package elements:

* Name

* Version

* Type

The “INVENTORY-V1” application supports only the *read* resource operations as described in the following sections.

### Inventory Bundles

#### Read All Bundles

This operation provides all the bundles installed in the OSGi framework.

* Request Topic:
    * **$EDC/account_name/client_id/INVENTORY-V1/GET/bundles**
  
* Request Payload:
    * Nothing application-specific beyond the request ID and requester client ID
  
* Response Payload:
    * Installed bundles serialized in JSON format

The following JSON message is an example of a bundle:

```json
{
    "bundles":[
        {
            "name":"org.eclipse.osgi",
            "version":"3.16.0.v20200828-0759",
            "id":0,
            "state":"ACTIVE",
          	"signed":true
        },
        {
            "name":"org.eclipse.equinox.cm",
            "version":"1.4.400.v20200422-1833",
            "id":1,
            "state":"ACTIVE",
          	"signed":false
        }
    ]
}
```

The bundle JSON message is comprised of the following bundle elements:

* Symbolic name

* Version

* ID

* State

* Signed

#### Start Bundle

This operation allows to start a bundles installed in the OSGi framework.

* Request Topic:
    * **$EDC/account_name/client_id/INVENTORY-V1/EXEC/bundles/_start**
  
* Request Payload:
    * A [JSON object](#json-identifier-for-start-and-stop-requests) that identifies the target bundle must be specified in payload body.
  
* Response Payload:
    * Nothing application specific

#### Stop Bundle

* Request Topic:
    * **$EDC/account_name/client_id/INVENTORY-V1/EXEC/bundles/_stop**
  
* Request Payload:
    * A [JSON object](#json-identifier-for-start-and-stop-requests) that identifies the target bundle must be specified in payload body.
  
* Response Payload:
    * Nothing application specific

##### JSON identifier for start and stop requests

The requests for starting and stopping a bundle require the application to include a JSON object in request payload for selecting the target bundle, the defined properties are the following:

* **name**: The symbolic name of the bundle to be started/stopped. This parameter must be of string type and it is mandatory.
* **version**:  The version of the bundle to be stopped. This parameter must be of string type and it is optional.

If multiple bundles match the selection criteria, only one of them will be stopped/started, which one is not defined.

Examples:

```json
{
    "name":"org.eclipse.kura.example.beacon"
}
```

```json
{
    "name":"org.eclipse.kura.example.beacon",
    "version":"1.0.500"
}
```

### Inventory Deployment Packages

#### Read All Deployment Packages

This operation provides the deployment packages installed in the OSGi framework.

* Request Topic:
    * **$EDC/account_name/client_id/INVENTORY-V1/GET/packages**
  
* Request Payload:
    * Nothing application-specific beyond the request ID and requester client ID
  
* Response Payload:
    * Installed deployment packages serialized in JSON format

The following JSON message is an example of a bundle:

```json
{
    "deploymentPackages":[
        {
            "name":"org.eclipse.kura.example.beacon",
            "version":"1.0.500",
          	"signed":false,
            "bundles":[
                {
                    "name":"org.eclipse.kura.example.beacon",
                    "version":"1.0.500",
                  	"id": 171,
                    "state": "ACTIVE",
                    "signed": false
                }
            ]
        }
    ]
}
```

The deployment package JSON message is comprised of the following package elements:

* Symbolic name

* Version

* Signature: true if all the bundles in the deployment package are signed

* Bundles that are managed by the deployment package along with their symbolic name and version

### Inventory System Packages (DEB/RPM/APK)

#### Read All System Packages

This operation provides the Linux packages installed in OS.

* Request Topic:
    * **$EDC/account_name/client_id/INVENTORY-V1/GET/system.packages**
  
* Request Payload:
    * Nothing application-specific beyond the request ID and requester client ID
  
* Response Payload:
    * Installed Linux Packages serialized in JSON format

The following JSON message is an example of a bundle:

```json
{
    "systemPackages":[
        {
            "name":"adduser",
            "version":"3.118",
            "type":"DEB"
        },
        {
            "name":"alsa-utils",
            "version":"1.1.8-2",
            "type":"DEB"
        },
        {
            "name":"ansible",
            "version":"2.7.7+dfsg-1",
            "type":"DEB"
        },
        {
            "name":"apparmor",
            "version":"2.13.2-10",
            "type":"DEB"
        },
        {
            "name":"apt",
            "version":"1.8.2.1",
            "type":"DEB"
        },
        {
            "name":"apt-listchanges",
            "version":"3.19",
            "type":"DEB"
        },
        {
            "name":"apt-transport-https",
            "version":"1.8.2.2",
            "type":"DEB"
        },
        {
            "name":"apt-utils",
            "version":"1.8.2.1",
            "type":"DEB"
        }
    ]
}
```

The bundle JSON message is comprised of the following bundle elements:

* Name

* Version

* Type

### Inventory Containers

#### List All Containers

Using the API exposed by Inventory-V1, the user can manage containers via external applications such as Everywhere Cloud. This operation lists all the containers installed in the gateway.

* Request Topic:
    * **$EDC/account_name/client_id/INVENTORY-V1/GET/containers**
* Request Payload:
    * Nothing application-specific beyond the request ID and requester client ID
* Response Payload:
    * Installed containers serialized in JSON format

The following JSON message is an example of what this request outputs:

```json
{
  "containers":
  [
    {
      "name":"container_1",
      "version":"nginx:latest",
      "type":"DOCKER",
      "state":"active"
    }
  ]
}
```

The container JSON message is comprised of the following elements:

* Name: The name of the docker container.

* Version: describes both the container's respective image and tag separated by a colon.

* Type: denotes the type of inventory payload

* State: describes the container's current state
     * Active: Container is running
     * Installed: Container is starting
     * Uninstalled: Container has failed, or is stopped
     * Unknown: Container state can not be determined


#### Start a Container

This operation allows starting a container installed on the gateway.
* Request Topic
    * $EDC/account_name/client_id/INVENTORY-V1/EXEC/containers/_start
* Request Payload
    * A JSON object that identifies the target container must be specified in the payload body. This payload will be described in the following section
* Response Payload
    * Nothing application-specific

#### Stop a Container

* Request Topic
    * $EDC/account_name/client_id/INVENTORY-V1/EXEC/containers/_stop
* Request Payload
    * A JSON object that identifies the target container must be specified in the payload body. This payload will be described in the following section.
* Response Payload
    * Nothing application-specific

#### JSON identifier/payload for container start and stop requests

The requests for starting and stopping a container require the application to include a JSON object in the request payload for selecting the target container. Docker enforces unique container names on a gateway, and thus they can reliably be used as an identifier.

Examples:

```json
{
    "name":"container_1",
    "version":"nginx:latest",
    "type":"DOCKER",
    "state":"active"
}
```

```json
{
    "name":"container_1",
}
```

### Inventory Summary

#### Read All Resources

This operation provides a list of all the resources installed on the gateway

* Request Topic:
    * **$EDC/account_name/client_id/INVENTORY-V1/GET/inventory**
  
* Request Payload:
    * Nothing application-specific beyond the request ID and requester client ID
  
* Response Payload:
    * Installed Linux Packages serialized in JSON format

The following JSON message is an example of a bundle:

```json
{
    "inventory":[
        {
            "name":"adduser",
            "version":"3.118",
            "type":"DEB"
        },
        {
            "name":"com.eclipsesource.jaxrs.provider.gson",
            "version":"2.3.0.201602281253",
            "type":"BUNDLE"
        },
              {
            "name":"org.eclipse.kura.example.beacon",
            "version":"1.0.500",
            "type":"DP"
        }
    ]
}
```

The bundle JSON message is comprised of the following bundle elements:

* Name

* Version

* Type



## Remote Certificates and Keys management via MQTT (KEYS-V1)

The **KEYS-V1** app-id is exposed by the `org.eclipse.kura.core.keystore` bundle. This request handler allows the remote management platform to get a list of all the KeystoreService instances and corresponding keys managed by the framework in a given device.

The request handler allows, also, to install new trusted certificate and to generate new key pairs directly in the device. Finally, the remote platform can request, from a defined key pair, the generation of a CSR that can be countersigned remotely by a trusted CA.  

### Read All the KestoreServices

This operation returns the list of all the KeystoreServices instantiated in the framework.

* Request Topic:
    * **$EDC/account_name/client_id/KEYS-V1/GET/keystores**
  
* Request Payload:
    * Nothing application-specific beyond the request ID and requester client ID
  
* Response Payload:
    * List of all the managed KeystoreService instances with number of entries stored

The following JSON message is an example of an output provided:

```json
[
    {
        "id": "org.eclipse.kura.core.keystore.SSLKeystore",
        "type": "jks",
        "size": 4
    },
    {
        "id": "org.eclipse.kura.crypto.CryptoService",
        "type": "jks",
        "size": 3
    },
    {
        "id": "org.eclipse.kura.core.keystore.HttpsKeystore",
        "type": "jks",
        "size": 1
    },
    {
        "id": "org.eclipse.kura.core.keystore.DMKeystore",
        "type": "jks",
        "size": 1
    }
]
```
Each entry of the array is specified by the following values:

- **id**: the KeystoreService PID 
- **type**: the type of keystore managed by the given instance
- **size**: the number of entries in a given KeystoreService instance

### Read Key Entries

This operation returns the list of all the key entries managed by the framework. If a request payload is specified, the list of entries is filtered based on the parameters in the request

* Request Topic:
    * **$EDC/account_name/client_id/KEYS-V1/GET/keystores/entries**
  
* Request Payload:
    * Nothing application-specific beyond the request ID and requester client ID. In this case the response will contain all the entries in all the managed keystoreService instances.
    * A JSON object with one of the following:

    ```json
    {
        "keystoreServicePid": "org.eclipse.kura.core.keystore.SSLKeystore"
    }
    ```

    ```json
    {
    "alias": "ca-godaddyclass2ca"
    }
    ```

* Response Payload:
    * List of all the key entries managed by the framework eventually filtered based on the parameters in the request.

The following JSON message is an example of an output provided in the response body:

```json
[
    {
        "subjectDN": "OU=Go Daddy Class 2 Certification Authority, O=\"The Go Daddy Group, Inc.\", C=US",
        "issuer": "OU=Go Daddy Class 2 Certification Authority,O=The Go Daddy Group\\, Inc.,C=US",
        "startDate": "Tue, 29 Jun 2004 17:06:20 GMT",
        "expirationDate": "Thu, 29 Jun 2034 17:06:20 GMT",
        "algorithm": "SHA1withRSA",
        "size": 2048,
        "keystoreServicePid": "org.eclipse.kura.core.keystore.SSLKeystore",
        "alias": "ca-godaddyclass2ca",
        "type": "TRUSTED_CERTIFICATE"
    },
    {
        "algorithm": "RSA",
        "size": 4096,
        "keystoreServicePid": "org.eclipse.kura.core.keystore.HttpsKeystore",
        "alias": "localhost",
        "type": "PRIVATE_KEY"
    }
]
```

### Read Key Details

This operation returns the details associated to a specified key in a keystore

* Request Topic:
    * **$EDC/account_name/client_id/KEYS-V1/GET/keystores/entries/entry**
  
* Request Payload:
    * A JSON with the `keystoreServicePid` and the `alias` of the desired key 

```json
{
  "keystoreServicePid": "org.eclipse.kura.core.keystore.HttpsKeystore"
  "alias": "localhost"
}
```
* Response Payload:
    * List of all the details associated to a key managed by the framework

The following JSON message is an example of an output provided:

```json
{
    "algorithm": "RSA",
    "size": 4096,
    "certificateChain": [
        "-----BEGIN CERTIFICATE-----\nMIIFkTCCA3mgAwIBAgIECtXoiDANBgkqhkiG9w0BAQsFADBZMQswCQYDVQQGEwJJ\nVDELMAkGA1UECBMCVUQxDjAMBgNVBAcTBUFtYXJvMREwDwYDVQQKEwhFdXJvdGVj\naDEMMAoGA1UECxMDRVNGMQwwCgYDVQQDEwNFU0YwHhcNMjEwNDIyMTUxNTU1WhcN\nMjQwMTE3MTUxNTU1WjBZMQswCQYDVQQGEwJJVDELMAkGA1UECBMCVUQxDjAMBgNV\nBAcTBUFtYXJvMREwDwYDVQQKEwhFdXJvdGVjaDEMMAoGA1UECxMDRVNGMQwwCgYD\nVQQDEwNFU0YwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQC7iZ3fHUQa\nTPgnvSxGZK4f6MZYfLclD74yqaCCWAztNxPQoiBoSPGdsBGBLNeFbwY0Yzg3qwXw\nYvgzLJmoXV9rSix7LgXPzsSYfUGfu7PeYTy5bG9X2UVyw9LloUM5DKnw++5F7Xy7\nF0KQQi0z6/HbbPkZ2aGyNRtMCTh1iAGy3gDh/mMnjpUYuoq1luoX1x6I77X0C+NP\nTxldVYrTeQiswItAHZmkK1R8AYedbFBgjDuTrfRODxBwESn4kQSMLJ8yHYDRm8S6\ngVz5LdkcM48UiV5hhF+bCD3UvYA00ZgZm2oOG1ONchYrE7pJr7eQVCYaXkS1lALB\nKaVJzn03wiLJJv1FYLmGt5J/MwfqyCtBTLlieEVfwnxFCkymtews6SYK32e9q/uJ\nfcdpWH7tOoarnAf7j5mE84rRU3HqzghK0bMxntfrSH3t18ZUt1/4Qx78WfiM1Te3\nJtnWBqUNJtX6lgT8IxTWwyEqD183tyKIo8hPGyeJrzWA5RL5hYF5rCNTWzqz5Upi\n0b/YI5K09+Rn8XmEzzaWjFq5zu6/WpqwPRA8kc2RAEA2scnOT+3yl9Lof/M7BrfL\nMdjVOZ4MfXgl/fhFyd16AObXuZRUIeiWowKtEiNaiUn8paLDxG+LNV7p5wEQCZZI\n+MsXMMp6G8Te4yILLCcGov7OkO2wx4GPWQIDAQABo2EwXzAxBgNVHSUEKjAoBggr\nBgEFBQcDAQYIKwYBBQUHAwIGCCsGAQUFBwMDBggrBgEFBQcDCDALBgNVHQ8EBAMC\nAvwwHQYDVR0OBBYEFKM5PlHoe8qFC6w0quGacazGWE/LMA0GCSqGSIb3DQEBCwUA\nA4ICAQBvpXmbS9LN8n0A+uq+tM3CNtF3YotWRbQHIGJAFTvdq3003W3CVdmykFc8\n9Kz8PoY1swBJms7GKjQLkqgTHoq6jU/cIXw+CoLQWmvAugva5C1u/5AHJZqTC06J\nGZyn1Z9N5Lp0XcgogEyhxdbkHniv7jvcmbCurQijZc9nsd5St7e1pT0Co7KKI6Ff\nODdVP6kZYBzKo4t20tATdAZJ8t7YHNKNq7ZVs1ej9oYUmmQieNXuE4UoHe5hzVQw\n567cNHWcTHJoyPve03TSQV91wp5rRUKZm2p0WtFNuv22f5p5sQmttsJltzHCgTwE\nK0j6qYKnXiq+EQs0A3uF9uiIB/KEDLjxscstqsQGFCFOmjA3GSbmJiKCnss3HkNn\naknT7XCV6tqgDOfPnNzbWJODjYZ+V0DyNY5uqkG2cyREm/qGbH1kLEXhqdWbKqEs\nsdW6x8p0ImTaPuRl3XEmXbolavIq+FTtOSz8vW1PsdD3quO6krrwiQMXKv1ZMjup\nDGIZZ4hUUhN84efjlZyoFRvPRvZ8YvjjrHXLij0vcRxndlicevwl5ezlm0LBOpsT\nkI2uWrbSbxlue/XdgwFCbN0+mXX88fGj6cjhpvd/xnwHaDHfSG9UoU149LJb6ZIZ\nru+07QriQQxK8V7AdPr6bhmKPxbbFenvSQmsmgjAY93qtanbNg==\n-----END CERTIFICATE-----"
    ],
    "keystoreServicePid": "org.eclipse.kura.core.keystore.HttpsKeystore",
    "alias": "localhost",
    "type": "PRIVATE_KEY"
}
```

### Create CSR

This operation returns the CSR for a specific key pair managed by the framework

* Request Topic:
    * **$EDC/account_name/client_id/KEYS-V1/POST/keystores/entries/csr**
  
* Request Payload:
    * A JSON with the all the details necessary to generate the CSR 

    ```json
    { 
        "keystoreServicePid":"org.eclipse.kura.core.keystore.HttpsKeystore",
        "alias":"localhost",
        "signatureAlgorithm" : "SHA256withRSA",
        "attributes" : "CN=Kura, OU=IoT, O=Eclipse, C=US"
    }
    ```

* Response Payload:
    * The generated CSR in the body of the message

    ```text
    -----BEGIN CERTIFICATE REQUEST-----
    MIIEgTCCAmkCAQAwPDELMAkGA1UEBhMCVVMxEDAOBgNVBAoTB0VjbGlwc2UxDDAK
    BgNVBAsTA0lvVDENMAsGA1UEAxMES3VyYTCCAiIwDQYJKoZIhvcNAQEBBQADggIP
    ADCCAgoCggIBAICTNbBm2wIV/TvddB3OW2s2WJmhAOBxwDSdpxGpgWDzmFAydCt5
    SfWCIeC0kmQfrJpcvcIB7IoE2I7HWtIOxV9c+E+n6R76NvdBQzB8enFfZu4ahIKy
    ul2VXQSj0VtYLZvG3yx6af4j8UFWsf2AuAe5Fd1dSBq9aEoRU/D5/uNQOQJi45Hk
    ds1KK0FcTkfPjugUCLf1Uf0xXnK1V7yZGrgDpPDbZAYCrcsGomdziO8zkE88gKaa
    oC1madGL44yz5tiHTKvbf+O+fKc31N4iDvnIg8f87IMF0D4afDF+3AJjVfcFtp3Q
    xWP3zpKqzPzpzWagTzsW446YMxamZgkDxLsVLitQtesom4ON3HT8s+jxHQhCO5LR
    83Ge10+6viJtkp20GYCqANO85c3TaD9njOE0y8P/T7Nk8MwnBbVgwa15QEWRqjEd
    HB6dF5jKdxlfZhPe2AVnLWAd/W96tCIBSqYu6TTH8npprp/S4t10tRkpaLGa+24c
    VlsjR6AFUX4KksvE/mbXd9QsvKgw/h3g4Jly4W/Ourt1LAH19tzGwULNCS7Ft9rp
    IXUsbmUUwb0V3B3ptcJUDzPUw8LdbItPnXzaPegxmkHO8IllcrdRBXrpcTwJl1ug
    MTMWKW/UjUwKcNQ0mGIxQ18aS0mHk8x8bVTnYLcCnGq3NeiFWvOiJIJpAgMBAAGg
    ADANBgkqhkiG9w0BAQsFAAOCAgEATsHVZAEjkMSpwozWbVvDw4iJOSYaQ7ZJXhGZ
    n81puMy/kcdNVD2hfG2c4ern8KPib6hYd1mbQpyNtsbJ68VOPIYOdiaqFd7+lbtM
    IVNETBA9ezXzzXwPCtiJYpmeDYz6HfIzRRzuoJhZtOrgyw8v5wiM0NkenDbTQs4l
    Od/YPFlHnEDkTNM+B/ZJJxRIg3sPhAAgj5HH0Mj2053z66hLDYAo4Tos98MwUcuA
    dY1pcs3brxg6z7xz4vbNKyj0Lh8Gua92OSbl1AFZYb6KXm/7+Md0la/YD+K/E2n6
    hUAcHkr3ayNuTI6lkQFptCHzb4Zr8rdbu63JRno9PFTnW+fa/0xi35DoHD2SAhwA
    CUGXTR+HQXkzB/9NE9X0TxS8SwyrE8sfw4usZm25tACdZ33xziqJXOmbChETyL2b
    J1IcbsHaeN2Shjnj7UQj+hQFnjVwRLTd0zWMN/l7mPj6TiW9ehubE8ce5siHW7NO
    mqJU1bklxTefefSNHTXrvTInuDXT81gLBRE3x+6uqU2kkJnL8jkrkebDDBhYF+qO
    6dB4W5WGbEHxorX2qfjImvy2Ohsl3rL/DqJgqECZaubTz1Xcj/kl9bdxs0pfa6IY
    Inre5iom9bGcA6W6U34jRsrE2pobi6c9Yimrbr/R2O/8Oy2k94FQta8tg8jbAxBi
    Z0Vd1nM=
    -----END CERTIFICATE REQUEST-----
    ```

### Store Trusted Certificate

This operation stores the provided certificate as a Trusted Certificate Entry managed by the framework

* Request Topic:
    * **$EDC/account_name/client_id/KEYS-V1/POST/keystores/entries/certificate**
  
* Request Payload:
    * A JSON with the all the details necessary to generate create the new entry

    ```json
    {
        "keystoreServicePid":"MyKeystore",
        "alias":"myCertTest99",
        "certificate":"-----BEGIN CERTIFICATE-----
            MIIDdzCCAl+gAwIBAgIEQsO0gDANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV
            bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD
            VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du
            MB4XDTIxMDQxNDA4MDIyOFoXDTIxMDcxMzA4MDIyOFowbDEQMA4GA1UEBhMHVW5r
            bm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE
            ChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCC
            ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJSWJDxu8UNC4JGOgK31WCvz
            NKy2ONH+jTVKnBY7Ckb1hljJY0sKO55aG1HNDfkev2lJTsPIz0nJjNsqBvB1flvf
            r6XVCxdN0yxvU5g9SpRxE/iiPX0Qt7463OfzyKW97haJrrhF005RHYNcORMY/Phj
            hFDnZhtAwpbQLzq2UuIZ7okJsx0IgRbjH71ZZuvYCqG7Ct/bp1D7w3tT7gTbIKYH
            ppQyG9rJDEh9+cr9Hyk8Gz7aAbPT/wMH+/vXDjH2j/M1Tmed0ajuGCJumaTQ4eHs
            9xW3B3ugycb6e7Osl/4ESRO5RQL1k2GBONv10OrKDoZ5b66xwSJmC/w3BRWQ1cMC
            AwEAAaMhMB8wHQYDVR0OBBYEFPospETb5HNeD/DmS9mwt+v/AYq/MA0GCSqGSIb3
            DQEBCwUAA4IBAQBxMe1xQVQKt36A5qVlEZyxI9eb6eQRlYzorOgP2tFaOsvDPpRI
            CALhPmxgQl/5QvKFfCXKoxWj1Spg4sF6fJp6jhSjLpmChS9lf5fRaWS20/pxIddM
            10diq3r6HxLKSxCYK7Pf5scOeZquvwfo8Kxye01bvCMFf1s1K3ZEZszk5Oo2MnWU
            U22YnXfZm1C0h2WMUcou35A7CeVAHPWI0Rvefojv1qYlQScJOkCN5lO6C/1qvRhq
            nDQdQN/m1HQbpfh2DD6F33nBjkyLQyMRF8uMnspLrLLj8lecSTJZO4fGJOaIXh3O
            44da9A02FAf5nRRQpwP2x/4IZ5RTRBzrqbqD
            -----END CERTIFICATE-----"
    }
    ```

* Response Payload:
    * Nothing

### Generate KeyPair

This operation will generate a new key pair directly in the device, based on the parameters received from the request

* Request Topic:
    * **$EDC/account_name/client_id/KEYS-V1/POST/keystores/entries/keypair**
  
* Request Payload:
    * A JSON with the all the details necessary to create the new key pair

    ```json
    {
        "keystoreServicePid":"MyKeystore",
        "alias":"keypair1",
        "algorithm" : "RSA",
        "size": 1024,
        "signatureAlgorithm" : "SHA256WithRSA",
        "attributes" : "CN=Kura, OU=IoT, O=Eclipse, C=US"
    }
    ```

* Response Payload:
    * Nothing

### Delete Entry

This operation will delete the specified entry from the framework managed keystores

* Request Topic:
    * **$EDC/account_name/client_id/KEYS-V1/DEL/keystores/entries**
  
* Request Payload:
    * A JSON with the all the details necessary to identify the entry to be deleted

    ```json
    {
        "keystoreServicePid" : "MyKeystore",
        "alias" : "mycerttestec"
    }
    ```

* Response Payload:
    * Nothing