# Kura OpenAPI generation pilot

The normal Maven build generates an OpenAPI 3.0.3 document for all operations in
the system v1 and configuration v2 REST resources. This module is a build tool;
it is not an OSGi bundle and is not included in device distributions.

From the repository root, using JDK 21:

```sh
mvn -pl openapi -am verify
```

The generated documents are `openapi/target/openapi/openapi.json` and
`openapi/target/openapi/openapi.yaml`. Both are attached to
`org.eclipse.kura:org.eclipse.kura.openapi` with classifier `openapi` and their
respective file extensions during `package`, so `install` and `deploy` publish
them with the Kura version. Generated files are not committed.

Generation runs at `process-classes`, including with `-DskipTests`. That flag
skips the verification tests, not generation. Neither a running Kura instance
nor an OSGi framework is required. No runtime OpenAPI endpoint or UI is added.

The scanner uses an explicit resource-class allowlist in `swagger-config.yaml`.
The relative server URL is `/services`; paths retain their API versions.
Operation IDs combine the HTTP method and normalized versioned resource path.
Changing a method or path consequently changes its operation ID.

The documentation-only reader uses private fields instead of getters to match
Kura's Gson serialization. Java `Object` values remain unconstrained, including
configuration property values. This is not a general-purpose Gson adapter:
future custom serializers or field-naming annotations need additional handling
and payload tests before expanding coverage.

Tests compare JSON and YAML, check endpoint coverage and reference resolution,
and validate representative Gson payloads using JSON Schema draft 4 for the
structural subset exercised by this pilot. They also check an invalid payload
is rejected. This does not replace full OpenAPI semantic validation or tests
against a running device.

## Known gaps

This is a preliminary generated contract, not complete API documentation.
Authentication, XSRF handling, permissions, required request properties, error
responses, and bodies hidden behind generic JAX-RS `Response` return types need
explicit documentation. Inferred responses currently use the generator's
`default` response, rather than asserting specific HTTP status codes.
The absence of a security declaration does not mean an endpoint is public.

The specification describes these two resources in the source release, not
the capabilities of a particular device. Installed bundles determine actual
availability. Component-specific configuration constraints must still be
obtained from component metadata.

Next steps are selective annotation enrichment, additional REST bundles,
complete contract validation and compatibility checks, and hosted documentation.
