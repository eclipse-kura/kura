---
layout: page
title:  "REST Service"
categories: [builtin]
---

Eclipse Kura provides a built-in REST Service based on the [osgi-jax-rs-connector](https://github.com/hstaudacher/osgi-jax-rs-connector) project.

By default, REST service providers register their services using the context path ```/services```.

The REST service provides the **BASIC** Authentication support. The allowed users and associated roles can be specified via the REST Service configuration as depicted in the image below:

![rest_service]({{ site.baseurl }}/assets/images/builtin/rest_service.png)

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