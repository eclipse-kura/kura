---
layout: page
title:  "REST Service"
categories: [builtin]
---

Eclipse Kura provides a built-in REST Service based on the [osgi-jax-rs-connector](https://github.com/hstaudacher/osgi-jax-rs-connector) project.

By default, REST service providers register their services using the context path ```/services```.

The REST service provides the **BASIC** Authentication support and HTTPS client certificate authentication support.

REST API access is available on all HTTP ports defined in the **Security -> Http Service** section, unless access is restricted to dedicated ports using the corresponding configuration parameter (see below).

Certificate authentication support is only available on the **HTTPS With Certificate Authentication Ports** configured in **Security -> Http Service** section.

 Kura Identity names and passwords can be used for **BASIC** Authentication. Certificate authentication follows the same rules as Kura Web Console access.

> If the forced password change feature for a  given identity is enabled, REST API password authentication will be blocked for that identity until the password is updated by the user or the feature is manually disabled.
> Certificate authentication will continue to be allowed even if the forced password change feature is enabled

JAX-RS roles are mapped to ESF permissions, the name of a permission associated with a JAX-RS role is the _rest._ prefix followed by the role name.
For example the _assets_ role is mapped to the _rest.assets_ permission.
REST related permissions can be assigned to an identity using the ESF Gateway Administration Console in the **Identities** section.

# RestService configuration

![rest_service]({{ site.baseurl }}/assets/images/builtin/rest_service.png)

The RestService configuration contains an **Allowed Ports** parameter that can be used to restrict REST API access to specific ports. If the port list is left empty, access will be enabled on all available ports.

Starting from Kura 5.2.0, the **RestService** configuration provides options to disable the built-in authentication methods.

# Custom authentication methods

Starting from Kura 5.2.0, it is also possible to develop custom REST authentication method providers by registering an implementation of the `org.eclipse.kura.rest.auth.AuthenticationProvider` interface as an OSGi service. The `org.eclipse.kura.example.rest.authentication.provider` bundle in Kura repository provides an example on how to implement a custom authentication method.

The System Administrator is required to specify:

- **user.name** - Specifies the list of users allowed to use the REST APIs
- **password** - Specifies the password for each user
- **roles** - The list of roles for each user as a list separated by the ';' character
of the users allowed to interact with the REST APIs provided by this ESF bundle.

By default, ESF comes pre-configured with the following credentials:

- **user.name** - admin
- **password** - admin
- **roles** - assets

## Assets REST APIs
ESF exposes REST APIs for the Asset instances instantiated in the framework. Assets REST APIs are available in the context path ```/services/assets```. Following, the supported REST endpoints:

Method | Path&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | Roles Allowed | Encoding | Request Parameters | Description |
-------|---------------|---------------|----------|--------------------|-------------|
GET    | /             | assets        | JSON     | None               | Returns the list of available assets. |
GET    | /{pid}        | assets        | JSON     | None               | Returns the list of available channels for the selected asset (specified by the corresponding PID) |
GET    | /{pid}/_read  | assets        | JSON     | None               | Returns the read for all the READ channels in the selected Asset |
POST   | /{pid}/_read  | assets        | JSON     | The list of channels where the READ operation should be performed. The expected format is: <br/>```{```<br/>&nbsp;&nbsp;```"channels":[```<br/>&nbsp;&nbsp;&nbsp;&nbsp;```"channel-1",```<br/>&nbsp;&nbsp;&nbsp;&nbsp;```"channel-2"```<br/>&nbsp;&nbsp;```]```<br/>```}``` | Returns the result of the read operation for the specified channels. |
POST   | /{pid}/_write | assets        | JSON     | The list of channels and associated values that will be used for the WRITE operation. The expected format is: <br/>```{```<br/> &nbsp;&nbsp;```"channels":[```<br/>&nbsp;&nbsp;&nbsp;&nbsp;```{```<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;```"name":"channel-1",```<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;```"type":"INTEGER",```<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;```"value":10```<br/>&nbsp;&nbsp;&nbsp;&nbsp;```}```<br/>&nbsp;&nbsp;```]```<br>```}``` | Performs the write operation for the specified channels returning the result of the operation. |