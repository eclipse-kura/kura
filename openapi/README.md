# Kura OpenAPI

The normal Maven build generates an OpenAPI 3.0.3 document for all operations in
the system v1, configuration v2, session v1, identity v1/v2, and security v1/v2 REST resources.
This module is a build tool;
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
Source annotations define stable operation IDs, summaries, request fields, and responses.
The reader assigns an ID from the HTTP method and path when an operation has none,
and for inherited security operations shared across API versions.

HTTP Basic is available when enabled. For a password session, call
`POST /session/v1/login/password`, retain the session cookie, then call
`GET /session/v1/xsrfToken`. Send the cookie and `X-XSRF-Token` together on
subsequent requests, including GET. The token endpoint alone is exempt from
the token requirement. A session locked for password change permits only
the token endpoint and `POST /session/v1/changePassword`; obtain a new token
after changing the password. Kura also supports certificate authentication
on configured mutual TLS ports. OpenAPI 3.0 cannot express that security
scheme, so the specification describes the certificate route in prose.
System endpoints require `rest.system`, configuration endpoints require
`rest.configuration`, protected identity endpoints require `rest.identity`,
and protected security endpoints require `rest.security`; `kura.admin` also
grants access. Identity permission and password-requirement discovery endpoints
have no identity-role restriction. Security debug-mode status requires an
authenticated principal but no security role. Missing or invalid
authentication returns an empty 401 after the audit filter. An authenticated
identity without permission receives an empty 403.

The documentation-only reader uses private fields instead of getters to match
Kura's Gson serialization. Java `Object` values remain unconstrained, including
configuration property values. This is not a general-purpose Gson adapter:
future custom serializers or field-naming annotations need additional handling
and payload tests before expanding coverage.

Tests compare JSON and YAML, check endpoint coverage and reference resolution,
validate the document with the [official OpenAPI 3.0 schema](https://spec.openapis.org/oas/3.0/schema/2021-09-28)
and Swagger Parser, and check request requirements and Gson payloads with an
OpenAPI-aware schema validator. Embedded HTTP tests exercise the actual REST
resources, authentication filters, authorization, and Gson serializer, then
compare responses with the generated contract for the previously covered system,
configuration, and session resources. Identity and security currently have generated
schema and authorization-shape checks, but not embedded HTTP payload tests.
The vendored schema is supplied
by the OpenAPI Initiative under the [Apache 2.0 license](https://github.com/OAI/OpenAPI-Specification/blob/main/LICENSE).

## Known gaps

The specification describes these resources in the source release, not
the capabilities of a particular device. Installed bundles determine actual
availability. Component-specific configuration constraints must still be
obtained from component metadata. The embedded HTTP tests use a local servlet
server and mocked services; they do not verify a full OSGi installation or
the deployment's TLS and certificate configuration.

Next steps are additional REST bundles, compatibility checks, and hosted documentation.
