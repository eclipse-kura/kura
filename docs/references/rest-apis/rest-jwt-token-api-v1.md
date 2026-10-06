# JWT Token V1 REST APIs

The JWT token REST APIs let a client exchange its credentials for a pair of [JSON Web Tokens](../../core-services/jwt-services.md) and then access the Kura REST APIs with a bearer token, without sending the identity password or keeping a session cookie on every request. They are suited to scripts, backend services and non-browser clients.

A token pair is made of:

- an **access token**: sent as `Authorization: Bearer <token>` to access any REST API. It is short lived and cannot be revoked.
- a **refresh token**: exchanged once at [POST/refresh](#postrefresh) for a new token pair. It is never accepted as an access token.

## Prerequisites

1. Configure a **JWT Issuing Service** and a **JWT Verification Service** as described in [JWT Services](../../core-services/jwt-services.md). The verification trust store must contain the certificate of the signing key and, if *Trusted Issuers* is set, it must include the *Issuer* of the issuing service.
2. In the [REST Service](../../core-services/rest-service.md#rest-service-configuration) configuration set **JWT Authentication Enabled** to true. If several issuing or verification services are available, select them with **Token Issuing Service Target** and **Token Verification Service Target**, for example `(kura.service.pid=org.eclipse.kura.core.token.jwt.issuer.JwtIssuingService)`.

While JWT authentication is disabled, both endpoints return 404 and bearer tokens are not accepted.

## Workflow

1. Call [POST/issue](#postissue) authenticating with any other enabled method: BASIC credentials, a session (with the `X-XSRF-Token` header, see [Session V1 REST APIs](rest-session-api.md)) or a client certificate. A caller authenticated with a JWT access token is refused, so a leaked access token cannot be used to obtain new tokens.
2. Access the REST APIs with the header `Authorization: Bearer <accessToken>`.
3. Before the access token expires, or when a request fails with 401, call [POST/refresh](#postrefresh) with the refresh token. Replace both stored tokens with the returned ones: the used refresh token can no longer be exchanged.
4. If the refresh fails with 401, the refresh token is expired, already used or no longer valid: go back to step 1.

### Example with `curl`

```bash
# 1. issue a token pair (BASIC authentication, requires Basic Authentication Enabled)
curl -k -X POST -u "$KURA_USER:$KURA_PASS" https://$ADDRESS/services/token/jwt/v1/issue
```

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJraWQiOi...",
  "accessTokenExpiresInSeconds": 60,
  "refreshToken": "eyJraWQiOi...",
  "refreshTokenExpiresInSeconds": 900
}
```

```bash
# 2. access a resource
curl -k -H "Authorization: Bearer $ACCESS_TOKEN" https://$ADDRESS/services/$RESOURCE_PATH

# 3. refresh: the body is the bare refresh token, no other credentials are needed
curl -k -X POST -H 'Content-Type: text/plain' --data "$REFRESH_TOKEN" \
    https://$ADDRESS/services/token/jwt/v1/refresh
```

## Behavior

- **Authorization**: tokens carry only the identity name. Permissions are read from the identity on every request, so permission changes apply immediately and a token never grants more than the identity currently has. No permission is needed to call the token endpoints.
- **Identity checks**: issuing, refreshing and every bearer request fail if the identity no longer exists or must [change its password](../../gateway-configuration/gateway-administration-console-authentication.md#forced-password-change-on-login).
- **Revocation**: a single token cannot be revoked. To cut off a client, delete the identity, force a password change, disable JWT authentication, or replace the signing key / trusted certificates.
- **Lifetimes**: set by *JWT Access Token Duration* and *JWT Refresh Token Duration*, silently capped by the *Maximum Token Lifetime* of the issuing service. The `...ExpiresInSeconds` fields report the effective values. Since each refresh token works once, the refresh token duration is the maximum inactivity time of a client.
- **Refresh token rotation and replay**: every successful refresh consumes the refresh token. Used tokens are tracked in memory until they expire plus *JWT Used Refresh Token Retention*, which must be greater than or equal to the *Clock Skew Tolerance* of the verification service. After a Kura restart, the tracking is lost and already used refresh tokens are accepted again until they expire. Serialize refreshes in the client: two concurrent refreshes with the same token make one of them fail with 401.
- **Failed issuing**: if a new pair cannot be issued during a refresh (500 or 503), the refresh token is not consumed and can be retried.
- **External refresh tokens**: [POST/refresh](#postrefresh) accepts any refresh token trusted by the verification service, including tokens issued outside Kura, provided `sub` is an existing Kura identity, `aud` contains `/token/jwt/v1/refresh`, and `iss` and `jti` are present. Access tokens are verified with the audience `/token/jwt/v1/access`.
- **Caching**: token responses carry `Cache-Control: no-store` and `Pragma: no-cache`. Store tokens as credentials: never log them or put them in URLs.
- **Provider order**: bearer authentication is attempted after the certificate, BASIC and session providers. The **Allowed Ports** restriction applies as for any other REST API.

## Reference

- [JWT Token V1 REST APIs](#jwt-token-v1-rest-apis)
  - [Prerequisites](#prerequisites)
  - [Workflow](#workflow)
    - [Example with `curl`](#example-with-curl)
  - [Behavior](#behavior)
  - [Reference](#reference)
  - [Request definitions](#request-definitions)
    - [POST/issue](#postissue)
    - [POST/refresh](#postrefresh)
  - [JSON definitions](#json-definitions)
    - [TokenPair](#tokenpair)

## Request definitions

### POST/issue
  * **REST API path** : /services/token/jwt/v1/issue
  * **description** : Issues a new token pair for the identity that authenticated the request. Authentication with a JWT access token is not accepted.
  * **responses** :
      * **200**
          * **description** : Request succeeded.
          * **response body** :
              * [TokenPair](#tokenpair)
      * **401**
          * **description** : The request is not authenticated, it is authenticated with a JWT access token, or the identity does not exist.
      * **403**
          * **description** : The identity must change its password.
      * **404**
          * **description** : JWT authentication is disabled or no token issuing service matches the configured target.
      * **500**
          * **description** : Token issuing failed, for example the signing key is not available.
      * **503**
          * **description** : The token issuing service is temporarily unavailable.

### POST/refresh
  * **REST API path** : /services/token/jwt/v1/refresh
  * **description** : Exchanges a refresh token for a new token pair. No other authentication is needed. The request body must be the refresh token as `text/plain`; surrounding whitespace is ignored and the body is limited to 8 KiB.
  * **responses** :
      * **200**
          * **description** : Request succeeded, the exchanged refresh token can no longer be used.
          * **response body** :
              * [TokenPair](#tokenpair)
      * **400**
          * **description** : The request body is empty.
      * **401**
          * **description** : The refresh token is invalid, expired, already used or not a refresh token, or its identity does not exist or must change its password.
      * **404**
          * **description** : JWT authentication is disabled or no token issuing or verification service matches the configured targets.
      * **413**
          * **description** : The request body is larger than 8 KiB.
      * **500**
          * **description** : Token issuing failed. The refresh token is not consumed.
      * **503**
          * **description** : The token issuing or verification service is temporarily unavailable. The refresh token is not consumed.

## JSON definitions

### TokenPair
A newly issued access and refresh token pair.

<br>**Properties**:

  * **tokenType**: `string`
      Always `Bearer`.
  * **accessToken**: `string`
      The access token, to be sent in the `Authorization: Bearer` header.
  * **accessTokenExpiresInSeconds**: `number`
      Effective lifetime of the access token, in seconds.
  * **refreshToken**: `string`
      The refresh token, to be sent to [POST/refresh](#postrefresh).
  * **refreshTokenExpiresInSeconds**: `number`
      Effective lifetime of the refresh token, in seconds.

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJraWQiOi...",
  "accessTokenExpiresInSeconds": 60,
  "refreshToken": "eyJraWQiOi...",
  "refreshTokenExpiresInSeconds": 900
}
```
