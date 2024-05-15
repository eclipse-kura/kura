# Rest Security V2 API
!!! note

    This API can also be accessed via the RequestHandler with app-id: `SEC-V2`.

    This REST API requires a [SecurityService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/security/SecurityService.html) implementation to be registered on the framework, which is not provided by the standard Kura distribution.


The `SecurityRestService` APIs provides methods to manage the system security.
Identities with `rest.security` permissions can access these APIs.

## POST methods

#### Security policy fingerprint reload

- Description: This method allows the reload of the security policy's fingerprint
- Method: POST
- API PATH: `services/security/v2/security-policy-fingerprint/reload`

##### Responses

- 200 OK status
- 500 Internal Server Error (also returned when no `SecurityService` implementation is available)

#### Reload command line fingerprint

- Description: This method allows the reload of the command line fingerprint
- Method: POST
- API PATH: `services/security/v2/command-line-fingerprint/reload`

##### Responses

- 200 OK status
- 500 Internal Server Error (also returned when no `SecurityService` implementation is available)

#### Apply default production security policy

- Description: This method allows to apply the default production security policy available in the system
- Method: POST
- API PATH: `services/security/v2/security-policy/apply-default-production`

##### Responses

- 200 OK status
- 500 Internal Server Error (also returned when no `SecurityService` implementation is available)

#### Apply security policy

- Description: This method allows to apply the user provided security policy
- Method: POST
- API PATH: `services/security/v2/security-policy/apply`

##### Request

```
<plain text security policy>
```

##### Responses

- 200 OK status
- 500 Internal Server Error (also returned when no `SecurityService` implementation is available)

## GET methods

#### Debug enabled 
!!! note

    Access to this resource doesn't require the `rest.security` permission.

- Description: This method allows you to check whether debug mode is enabled in the system.
- Method: GET
- API PATH: `services/security/v2/debug-enabled`

##### Responses

- 200 OK status
```JSON
{
    "enabled":true
}
```
- 500 Internal Server Error (also returned when no `SecurityService` implementation is available)
