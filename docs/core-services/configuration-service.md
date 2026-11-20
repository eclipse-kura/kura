# Configuration Service

The Configuration Service is responsible to manage the framework configuration by creating and persisting the framework snapshot. Built on top of the OSGi Configuration Admin and Metatype services, it is also responsible to track and manage the creation and deletion of service instances as well as OSGi component factories.

The ConfigurationService is accessible using the following REST APIs and cloud request handlers:

- [Configuration v1 REST APIs](../references/rest-apis/rest-configuration-service-v1.md) (deprecated) and [CONF-V1 MQTT Request Handler](../references/mqtt-namespace.md#remote-osgi-configurationadmin-interactions-via-mqtt) (deprecated)
- [Configuration v2 REST APIs and CONF-V2 MQTT Request Handler](../references/rest-apis/rest-configuration-service-v2.md)

## Configuration validation

Starting with Kura 6.0 and 5.7.0, the Configuration Service checks each configuration value against the metatype of its component before applying it. A value is accepted when it matches its attribute definition:

- **Type**: the value can be converted to the attribute type, for example `Integer` or `Boolean`.
- **Minimum and maximum**: a numeric value is within the bounds. For a `String` attribute, the bounds apply to the length of the value.
- **Options**: if the attribute declares a list of options, each value is one of them.
- **Cardinality**: an attribute with cardinality `0` takes exactly one value; otherwise, the number of values cannot exceed the cardinality.
- **Required**: a required attribute has a value.

`Password` attributes are only checked for cardinality, and their values are masked as `********` in error messages and logs.

The validation applies to the following operations:

- **Configuration update**: `updateConfiguration` and `updateConfigurations`, which the Gateway Administration Console, the REST APIs and the MQTT request handlers listed above rely on, reject a component that has an invalid value with a `CONFIGURATION_ATTRIBUTE_INVALID` or `CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING` error, for example `The configuration attribute device.display-name cannot accept value My Gateway: the value is not one of the allowed options.` None of the values of that component are applied, and the new snapshot keeps its previous configuration. When a single component is updated, the call fails with that error; when several components are updated together, the valid ones are applied and the call fails with a partial-success error that holds the error of each rejected component. Self-configuring components, such as assets, are checked against the metatype they return, without the required-attribute check.
- **Factory component creation**: `createFactoryConfiguration` rejects an invalid value with a `CONFIGURATION_ATTRIBUTE_INVALID` error and creates no instance. Required attributes are not checked at this stage, since a new instance is usually configured after its creation.
- **Rollback**: `rollback` checks the whole target snapshot first. If the snapshot holds an invalid value, the rollback fails and no configuration is changed. See [Snapshot Management](../administration/snapshot-management.md#rollback) for the rollback from the Gateway Administration Console.
- **Start-up**: the values loaded from the latest snapshot are checked as soon as the metatype of their component is available. An invalid value is replaced with the metatype default, or removed if the attribute has no default, and logged at `ERROR` level with the component PID and the attribute name. The other values of the component are kept.

A metatype default, minimum or maximum that is not valid for the attribute type is ignored and logged at `ERROR` level.

!!! warning "Upgrading a device"
    The validation also covers the configuration stored before the upgrade. At the first start after the upgrade, a stored value that does not match its metatype is replaced with the default, and an `ERROR` log reports the component and the attribute: check the log and set those attributes again if the default does not fit. A value that the start-up check cannot reach, such as one described only by the metatype that a self-configuring component returns at run time, is kept until it is corrected, and the updates that carry it are rejected. Snapshots taken before the upgrade can hold invalid values as well, so a rollback to one of them can be refused.
