# Configuration Service

The Configuration Service is responsible to manage the framework configuration by creating and persisting the framework snapshot. Built on top of the OSGi Configuration Admin and Metatype services, it is also responsible to track and manage the creation and deletion of service instances as well as OSGi component factories.

The ConfigurationService is accessible using the following REST APIs and cloud request handlers:

- [Configuration v1 REST APIs](../references/rest-apis/rest-configuration-service-v1.md) (deprecated) and [CONF-V1 MQTT Request Handler](../references/mqtt-namespace.md#remote-osgi-configurationadmin-interactions-via-mqtt) (deprecated)
- [Configuration v2 REST APIs and CONF-V2 MQTT Request Handler](../references/rest-apis/rest-configuration-service-v2.md)