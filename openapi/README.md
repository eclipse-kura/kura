# Kura OpenAPI

The normal Maven build generates an OpenAPI 3.1.2 document for all operations in
the system v1, configuration v2, session v1, identity v1/v2, security v1/v2,
service listing v1, cloud connection v1, keystore v1/v2, tamper v1, and
inventory v1 REST resources. This module is a build tool; it is not an OSGi
bundle and is not included in device distributions.

From the repository root, using JDK 21:

```sh
mvn -pl openapi -am verify
```

The generated documents are `openapi/target/openapi/openapi.json` and
`openapi/target/openapi/openapi.yaml`. Both are attached to
`org.eclipse.kura:org.eclipse.kura.openapi` with classifier `openapi` and their
respective file extensions during `package`, so `install` and `deploy` publish
them with the Kura version. Generated files are not committed. Consumers must
support OpenAPI 3.1 and JSON Schema 2020-12; the same artifacts now contain 3.1.2
and no separate 3.0 artifact is published. This migration changes the documentation
contract only; HTTP behavior is unchanged.

Generation runs at `process-classes`, including with `-DskipTests`. That flag
skips the verification tests, not generation. Neither a running Kura instance
nor an OSGi framework is required. No runtime OpenAPI endpoint or UI is added.
The tests read `target/openapi/openapi.json` through the `openapi.directory`
system property set by Surefire, so running them from an IDE requires a prior
`mvn process-classes` on this module and configuring that property.

The scanner uses an explicit resource-class allowlist in `swagger-config.yaml`.
The relative server URL is `/services`; paths retain their API versions.
Source annotations define stable operation IDs, summaries, request fields, and responses.
The reader assigns an ID from the HTTP method and path when an operation does not
declare an explicit one, including operations inherited from base classes and
exposed under multiple API versions.

HTTP Basic is available when enabled. For a password session, call
`POST /session/v1/login/password`, retain the session cookie, then call
`GET /session/v1/xsrfToken`. Send the cookie and `X-XSRF-Token` together on
subsequent requests, including GET. The token endpoint alone is exempt from
the token requirement. A session locked for password change permits only
the token endpoint and `POST /session/v1/changePassword`; obtain a new token
after changing the password. The global security alternatives are HTTP Basic,
session cookie **and** XSRF token, or `clientCertificate` (`type: mutualTLS`).
Certificate authentication requires a configured mutual TLS port, trusted client
certificates and certificate-to-identity mapping. `POST /session/v1/login/certificate`
requires `clientCertificate` and creates a session cookie; obtain an XSRF token
before making subsequent session-authenticated requests. Public operations retain
empty security overrides, and XSRF retrieval requires only the session cookie.
System endpoints require `rest.system`, configuration endpoints require
`rest.configuration`, protected identity endpoints require `rest.identity`,
protected security endpoints require `rest.security`, cloud connections require
`rest.cloudconnection`, keystores require `rest.keystores`, tamper detection
requires `rest.tamper.detection`, and inventory requires `rest.inventory`.
`kura.admin` also grants access. Service-listing endpoints require an
authenticated principal without a specific REST role. Identity permission
and password-requirement discovery endpoints have no identity-role restriction.
Security debug-mode status requires an authenticated principal but no security
role. Missing or invalid authentication returns a 401 after the audit filter,
and a missing permission returns a 403. Entity-less JAX-RS errors now return
JSON `Error` objects, including when the client does not request JSON.

The documentation-only reader uses private fields instead of getters to match
Kura's Gson serialization. Java `Object` values remain unconstrained, including
configuration property values. This is not a general-purpose Gson adapter:
future custom serializers or field-naming annotations need additional handling
and payload tests before expanding coverage. Inventory and keystore responses
are documented with payload-accurate schemas defined in `swagger-config.yaml`:
inventory responses are marshalled by Kura's JSON marshaller with its own
field names, and keystore entries are polymorphic (`anyOf` trusted-certificate
and private-key entries). Embedded HTTP contract tests check both.

Tests compare JSON and YAML, check endpoint coverage and reference resolution,
validate the document and its schemas with the pinned
[official OpenAPI 3.1 schema](https://spec.openapis.org/oas/3.1/schema-base/2025-09-15)
and Swagger Parser, and check request requirements and Gson payloads with networknt's
OpenAPI 3.1 dialect and JSON Schema 2020-12. Tests also check 3.1 serialization
round-trips, nullable type unions, and unconstrained dynamic values including null.
Embedded HTTP tests exercise the actual REST
resources, authentication filters, authorization, and Gson serializer, then
compare responses with the generated contract for the previously covered system,
configuration, session, identity, security, and service-listing resources, and
for the inventory and keystore resources.
The official schemas and their OpenAPI dialect references are vendored unchanged
in `src/test/resources/oas-3.1`, with the upstream Apache 2.0 `LICENSE`. The document
schemas are pinned to 2025-09-15 and the dialect/meta schemas to 2024-11-10.
References resolve to these local resources and the JSON Schema 2020-12 resources
bundled in networknt; schema validation does not require network access.

## Known gaps

The specification describes these resources in the source release, not
the capabilities of a particular device. Installed bundles determine actual
availability. Component-specific configuration constraints must still be
obtained from component metadata. The embedded HTTP tests use a local servlet
server and mocked services; they do not verify a full OSGi installation or
the deployment's TLS and certificate configuration. Errors outside the JAX-RS
pipeline, such as servlet or proxy failures, are not normalized by this filter.
Keystore request validation errors are documented as plain text, while malformed
JSON returns a JSON `Error`. Inventory container/image actions return a JSON
`Error` for a missing resource or when orchestration is unavailable.
The unavailable-orchestration scenario exercises
the real inventory handler; the successful inventory payload tests still use
mocked handler responses and do not exercise the real JSON marshaller.
Keystore services are mocked, but the REST entry conversion and serializer are real.

The identity v2 default-configuration endpoint returns 400 for unknown
`configurationComponents`, consistently with the identity-configuration endpoint.
This validation fix changes runtime behavior.

## Error-body compatibility

The separate `ErrorEntityFilter` runtime change replaces missing error entities
with `application/json` bodies such as `{"message":"Not Found"}`. This is a
breaking change for clients relying on empty bodies or servlet-generated HTML
or JSON fields. It applies regardless of the request's `Accept` header.
Existing error entities (including plain-text validation errors), status codes,
and response headers are preserved. The filter runs after the audit filter so
the message reflects the final authentication status. HEAD responses remain
bodyless according to HTTP semantics.

Next steps are embedded HTTP checks for the cloud connection and tamper
resources, compatibility checks, and hosted documentation.
