# Kura OpenAPI

The Maven build generates OpenAPI 3.0.3 documents for the REST resources listed in
`src/main/resources/swagger-config.yaml`. This module is build-only: it is not an
OSGi bundle, adds no runtime endpoint or UI, and is not included in device distributions.

From the repository root, using JDK 21:

```sh
mvn -pl openapi -am verify
```

The generated files are `openapi/target/openapi/openapi.json` and
`openapi/target/openapi/openapi.yaml`; they are not committed. Generation runs at
`process-classes`, including with `-DskipTests`, and requires neither a running Kura
instance nor an OSGi framework.

To run the tests from an IDE, first run `mvn -pl openapi -am process-classes` and set
the `openapi.directory` system property to `openapi/target/openapi`. Surefire sets this
property automatically during Maven builds.

## Generation and verification

Source annotations define operations and payloads. The reader uses private fields to
match Kura's Gson serialization; Java `Object` values remain unconstrained. Custom
schemas cover payloads that use a different marshaller or polymorphic entries.

Tests compare JSON and YAML, check endpoint coverage and references, validate the
OpenAPI document, and exercise requests and responses through embedded HTTP servers
with the real REST resources, filters, and Gson serializer. The vendored OpenAPI schema
is provided by the OpenAPI Initiative under the
[Apache 2.0 license](https://github.com/OAI/OpenAPI-Specification/blob/main/LICENSE).

## Known limitations

The specification describes the source release, not a particular installation;
installed bundles determine endpoint availability. Component-specific configuration
constraints must be obtained from component metadata. Embedded tests use mocked
services and do not verify a full OSGi installation or deployment TLS configuration.
Container-generated bodies for entity-less error responses remain unspecified.
