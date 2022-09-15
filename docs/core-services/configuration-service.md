# Configuration Service

The Configuration Service is responsible to manage the framework configuration by creating and persisting the framework snapshot. Built on top of the OSGi Configuration Admin and Metatype services, it is also responsible to track and manage the creation and deletion of service instances as well as OSGi component factories.

The Configuration Service is accessible using the following REST APIs and cloud request handlers:

* [Configuration V1 REST APIs](./configuration-service-rest-v1.md) (deprecated)
* [Configuration V2 REST APIs and CONF-V2 request handler](./configuration-service-rest-v2.md)