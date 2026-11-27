# Configuration Service

The Configuration Service is responsible to manage the framework configuration by creating and persisting the framework snapshot. Built on top of the OSGi Configuration Admin and Metatype services, it is also responsible to track and manage the creation and deletion of service instances as well as OSGi component factories.

The ConfigurationService is accessible using the following REST APIs and cloud request handlers:

- [Configuration v1 REST APIs](../references/rest-apis/rest-configuration-service-v1.md) (deprecated) and [CONF-V1 MQTT Request Handler](../references/mqtt-namespace.md#remote-osgi-configurationadmin-interactions-via-mqtt) (deprecated)
- [Configuration v2 REST APIs and CONF-V2 MQTT Request Handler](../references/rest-apis/rest-configuration-service-v2.md)

## Configuration validation

Starting with Kura 6.0 and 5.7.0, the Configuration Service checks each configuration value against the metatype of its component before applying it. A value is accepted when it matches its attribute definition:

- **Type**: the value has the exact Java type declared by the attribute definition, for example `Integer` or `Boolean` (string representations such as `"20"` for an `Integer` attribute are rejected).
- **Minimum and maximum**: a numeric value is within the bounds. For a `String` attribute, the bounds apply to the length of the value.
- **Options**: if the attribute declares a list of options, each value is one of them.
- **Cardinality**: an attribute with cardinality `0` takes a single scalar value; positive cardinality takes an array of the declared type; negative cardinality takes an array or a `Collection` of the declared type. The number of values cannot exceed the absolute cardinality, while `-2147483648` or `2147483647` sets no limit.
- **Required**: an update cannot set a required attribute to `null`. An attribute left out of the update keeps its current value, even when it has none.

`Password` attributes are only checked for type and cardinality, and their values are masked as `********` in error messages and logs. An empty value is accepted for an optional `String` attribute even when the attribute has a minimum length, since the Gateway Administration Console sends an empty field as an empty string. The options of an attribute that is not a `String` are compared by value, so the option `1` of a `Double` attribute matches `1.0`.

When strict validation is enabled (the default), it applies to the following operations:

- **Configuration update**: `updateConfiguration` and `updateConfigurations`, which the Gateway Administration Console, the REST APIs and the MQTT request handlers listed above rely on, reject a component that has an invalid value with a `CONFIGURATION_ATTRIBUTE_INVALID` or `CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING` error, for example `The configuration attribute device.display-name cannot accept value My Gateway: the value is not one of the allowed options.` None of the values of that component are applied, and the new snapshot keeps its previous configuration; when every component of the call is rejected, no snapshot is taken. When a single component is updated, the call fails with that error; when several components are updated together, the valid ones are applied and the call fails with a partial-success error that holds the error of each rejected component. A factory component instance that the update has to create is checked as described below, and its error is reported in the same way. Updating a component that is not tracked or has no registered metatype fails with a `CONFIGURATION_UPDATE` error instead of being ignored, and `null` update inputs fail with `INVALID_PARAMETER`. The `CONF-V1` request handler checks all the configurations of a `PUT` before replying, so if any component is invalid the whole request is rejected in the reply and no update is applied. Self-configuring components, such as assets, are checked against the metatype they return, without the required-attribute check.
- **Factory component creation**: `createFactoryConfiguration` rejects an invalid value with a `CONFIGURATION_ATTRIBUTE_INVALID` error and creates no instance. Required attributes are not checked at this stage, since a new instance is usually configured after its creation.
- **Rollback**: the entire target snapshot is validated before ConfigurationAdmin is modified. If any component contains an invalid value, every violation is logged at `ERROR` level and the whole rollback is rejected with a `CONFIGURATION_ATTRIBUTE_INVALID` error, leaving the running configuration and snapshots unchanged. See [Snapshot Management](../administration/snapshot-management.md#rollback) for the rollback from the Gateway Administration Console.
- **Start-up**: before the latest snapshot is pushed to ConfigurationAdmin, all snapshot configurations are validated against the metatypes of installed bundles. If any component contains an invalid value, every violation is logged at `ERROR` level with the component PID and attribute details, no configuration is applied, and the OSGi framework is shut down so Kura does not start with an invalid configuration. Configurations whose metatype is registered after start-up are checked when their OCD is tracked and rejected with a `KuraException` (without shutting down the framework) if invalid.

A metatype minimum or maximum that is not valid for the attribute type, and a metatype default that is not valid for the type or does not satisfy the bounds and options of its attribute, are ignored and logged at `ERROR` level.

### Strict validation property (`kura.configuration.validation`)

Strict configuration validation is controlled by the `kura.configuration.validation` property in `kura.properties` (which can be overridden in `kura_custom.properties` or via the `-Dkura.configuration.validation` JVM system property) and exposed by `SystemService.isConfigurationValidationEnabled()`:

- **`true` (default for new installations)**: invalid configurations are rejected as described above, and an invalid start-up snapshot stops the OSGi framework.
- **`false`**: metatype validation still runs across start-up, configuration updates, factory creation, and rollback, logging every violation at `WARN` level without rejecting configurations or stopping the framework. Structural API checks (`null` inputs, untracked PIDs, missing metatypes) remain active.

!!! warning "Enabling strict validation is strongly recommended"
    Running with `kura.configuration.validation=false` is intended only as a temporary compatibility measure when migrating legacy snapshots whose values do not yet conform to component metatypes. Check the `WARN` messages logged by `ConfigurationServiceImpl`, fix any invalid attribute values, and set `kura.configuration.validation=true` so invalid configurations cannot be applied.
