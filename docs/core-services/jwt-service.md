# JSON Web Token (JWT) Service

`JwtService` issues and verifies JSON Web Tokens (JWT). Tokens are signed with **RS256** using an RSA private key held in a `KeystoreService`, and verified against the trusted certificates of that same keystore.

Contents:

- [Introduction to JWT](#introduction-to-jwt): how JWT works and main characteristics
- [Configuration](#configuration): service parameters and their explanation
- [Issuing a token](#issuing-a-token): how to issue a token, how claims will be populated by this service, how to set the expiration time correctly
- [Verifying a token](#verifying-a-token): how to create a verification request and conditions for deciding if a token is valid or not
- [Hardening](#hardening): notes on how to tighten the security of this service
- [Operational notes](#operational-notes): general notes about the service behaviour

## Introduction to JWT

JWT (JSON Web Token, RFC 7519) is a compact, URL-safe way to carry a set of claims (statements about a subject) between two parties. In practice it's the format behind most bearer tokens: a client presents one, and the recipient decides who the caller is and what they may do.

!!! warning "About JWT Security"
    The common form (JWS) protects integrity and authenticity. The payload is merely base64 encoded, so anyone holding the token can read it: **never put secrets in a JWT**.

    JWT is stateless, so it is **hard to revoke**. A valid token stays valid until it expires; there's no natural "log out". This is why short lifetimes matter, and why the service has a maximum-lifetime cap.

A JWT is structured as follows, with three base64 encoded sections separated by a dot (`.`):

![Example of an encoded token](./images/jwt-example.png)

- Header: `alg` (RS256), `typ` (JWT), and usually `kid`, naming which key signed it so the verifier can pick the right certificate
- Payload: the claims. Seven are registered by the RFC (anything else is a custom claim), and they're exactly the ones the service manages:
    - `iss`: issuer, who generated it
    - `sub`: subject, who it's about
    - `aud`: audience, who it's for
    - `exp`/`nbf`: not valid after/before
    - `iat`: issued at
    - `jti`: unique token id
- Signature: computed over the ASCII of `base64UrlEncode(header)` + `.` + `base64UrlEncode(payload)`. Change one byte of either and it stops matching



## Configuration

- *Keystore Target Filter*: OSGi filter selecting the `KeystoreService` holding the signing key and the trusted certificates. Defaults to `(kura.service.pid=changeme)`, so the service does nothing until it is pointed at a real keystore
- *Signing Key Alias*: alias of the RSA private key entry used to sign. Published as the token's `kid` header. Leave empty for a verify-only service
- *Issuer*: value asserted in the `iss` claim of issued tokens. Always accepted when verifying
- *Trusted Issuers*: comma-separated `iss` values accepted in addition to *Issuer*
- *Verification Key Aliases*: comma-separated trust-store aliases allowed to verify. Empty means every RSA certificate in the store
- *Maximum Token Lifetime (sec)*: upper bound on `exp`. `0` means no bound
- *Clock Skew Tolerance (sec)*: tolerance window applied to `exp`, `nbf` and `iat`
- *Require Valid Certificate*: when enabled, a certificate outside its validity period cannot verify tokens



## Issuing a token

```java
@Reference
private TokenIssuingService tokenIssuingService;

final String token = this.tokenIssuingService.issue(TokenIssueRequest.builder()
        .identityName("admin")
        .intendedConsumer("rest-api")
        .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
        .claim("roles", List.of("operator"))
        .build());
```

A request says **what to assert**, never **how to protect it**. The service always controls:

- `iss`: taken from the Issuer setting; the caller cannot override it
- `iat`: the issuing instant
- `jti`: a freshly generated unique id; `issue()` is not idempotent, so two equal requests yield two distinct, independently valid tokens
- `kid`: the *Signing Key Alias*, so verifiers can select the right certificate
- the algorithm (RS256) and the signing key

The `identityName` is mandatory and becomes the `sub` claim: it is the Kura identity the token is issued for. Everything else is optional: intended consumers (`aud`), `nbf`, `exp`, and custom claims. Call `intendedConsumer(...)` once per consumer to declare several of them; `aud` is omitted entirely when none is declared.

!!! warning
    The returned string is a credential. Do not log it, do not put it in a URL, and transport it only over a secured channel.

### How `exp` is decided

| Maximum lifetime | Requested `exp` | Resulting `exp` |
| --- | --- | --- |
| `0` | none | **caution** no `exp` claim; the token never expires |
| `0` | any | exactly as requested |
| `> 0` | none | now + maximum lifetime |
| `> 0` | within the maximum | exactly as requested |
| `> 0` | beyond the maximum | capped to now + maximum lifetime |

The cap only applies upwards. A requested `exp` in the past is honoured, producing a token that is already invalid. JWT timestamps are whole seconds, so sub-second precision is truncated.

Capping is silent: no error is raised when a requested lifetime is shortened. Call `getMaximumLifetime()` to discover the configured bound before requesting, or read `getExpiresAt()` back from the verification proof, rather than assuming the requested window was honoured.



## Verifying a token

```java
@Reference
private TokenVerificationService tokenVerificationService;

final VerificationProof proof = this.tokenVerificationService.verify(TokenVerifyRequest.builder()
        .token(encodedToken)
        .intendedConsumer("rest-api")
        .build());

final String identityName = proof.getIdentityName();
```

Holding a `JwtVerificationProof` is the proof that verification succeeded. It exposes methods to access the verified token's claims.

!!! warning
    `getExpiresAt()` is empty for a token issued without `exp`. Such a proof must not be cached indefinitely on the strength of a missing expiration.

A token is accepted only when all of the following hold:

1. it is a well-formed JWT;
2. its `kid` names a certificate allowed by *Verification Key Aliases*. A token carrying no `kid` may be verified by any of the allowed certificates;
3. that certificate is within its validity period (unless *Require Valid Certificate* is disabled);
4. the RS256 signature verifies against it;
5. `iss` is the configured *Issuer* or one of the *Trusted Issuers*, a token without `iss` is rejected;
6. `exp`, `nbf` and `iat` are satisfied within the clock-skew tolerance;
7. `sub` is present and not blank, so the proof always carries an identity;
8. the intended consumer check passes (see [next section](#the-intended-consumer-check)).

### The intended consumer check

Why is the intended consumer a request parameter, and nothing else is? Every other check the service performs, it can decide on its own:

- the signature and `kid` against the keystore
- `iss` against the configured trust set
- `exp` / `nbf` / `iat` against the clock

The audience is the exception. RFC 7519 requires each principal processing a token to identify itself with a value in `aud`, and only the caller knows which principal it is, so the expected audience cannot be configuration. Several libraries accept the issuer (`iss`) at verification time too; here it is configuration, which puts the trust set in an administrator's hands rather than in application code.

When an intended consumer is set, the token is accepted only if its `aud` contains that value, and a token with no `aud` at all is rejected. When it is left unset no audience constraint is enforced, and a token minted for a different consumer verifies happily.



## Hardening

### Do not leak sensitive information

A JWT is not encrypted, so never leak sensitive information in the JWT claim values or custom claim names. For example, this is **bad**:

```
Signing Key Alias = <customer name> // will be published as 'kid'
Issuer = <customer name> // will be published under 'iss'
```

Example of issuing a token with **bad** custom claims, that leak personal information:

```json
{
    "firstname": "Mario",
    "lastname": "Rossi",
    "birthDate": "1990-01-09",
    "email": "username@example.com",
    "country": "IT",
    "roles": ["user"],
    "exp": 1763210697
}
```

### Restrict Verification Key Aliases

Restrict *Verification Key Aliases* whenever the keystore is shared with TLS. With the list empty, every entry in the store that yields an X.509 certificate with an RSA public key is accepted for verification. Anyone holding a private key whose certificate is in that store can then mint tokens this service will accept. Naming the aliases you actually trust closes that path. When the list is non-empty the **signing key alias is added to it automatically, so an instance never loses the ability to verify the tokens it issues

### A verify-only instance is a valid deployment

Leave the signing key alias empty and the instance will verify tokens from its peers while being unable to issue any. A misconfigured signing alias disables verification too, so an alias resolving to a certificate-only entry, or to a non-RSA key, makes the whole key material fail to load, and the instance can then neither issue nor verify.

### Configure Maximum Token Lifetime (sec)

Always configure the service with a defined *Maximum Token Lifetime (sec)*, preferably as short as possible. This prevents issuing of tokens that never expire and keeps the token alive for the minimum required time to perform some operations.

### Always declare the intended consumer when verifying

The API leaves it optional, so nothing fails if you forget it, and nothing warns you either. Make it a habit in every verifier, and give each consumer a distinct name so tokens cannot be replayed sideways.



## Operational notes

### An expired signing certificate breaks verification, not issuing

When *Require Valid Certificate* is enabled, which is the default, an expired signing certificate breaks just verification. The private key still signs, so `issue()` keeps succeeding while every verifier rejects the result. Issuing looks healthy and the failure surfaces at the consumer.

### Clock drift is bidirectional

`iat` is verified, so a gateway whose clock runs ahead of a verifier by more than the skew tolerance issues tokens that verifier refuses as "not yet usable".

### Verification proves the token, not the identity

A successful verification says a trusted key signed that identity name at some point, within the token's validity window. It does not say the identity still exists, is still enabled, or holds any permission, and there is no way to revoke a token before its `exp`. Look the identity up and check its permissions before acting on the proof.