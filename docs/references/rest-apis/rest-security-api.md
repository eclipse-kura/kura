# Rest Security v1 API
!!! note

    This API can also be accessed via the RequestHandler with app-id: `SEC-V1`.
    The standard Kura distribution doesn't provide a [SecurityService](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/security/SecurityService.html) implementation required by this API.


The `SecurityRestService` APIs provides methods to manage the system security.
Identities with `rest.security` permissions can access these APIs.

## POST methods

#### Security policy fingerprint reload

- Description: This method allows the reload of the security policy's fingerprint
- Method: POST
- API PATH: `services/security/v1/security-policy-fingerprint/reload`

##### Responses

- 200 OK status
- 500 Internal Server Error (also returned when no `SecurityService` implementation is provided)

#### Reload command line fingerprint

- Description: This method allows the reload of the command line fingerprint
- Method: POST
- API PATH: `services/security/v1/command-line-fingerprint/reload`

##### Responses

- 200 OK status
- 500 Internal Server Error (also returned when no `SecurityService` implementation is provided)

## GET methods

#### Debug enabled 
!!! note

    Access to this resource doesn't require the `rest.security` permission.

- Description: This method allows you to check whether debug mode is enabled in the system.
- Method: GET
- API PATH: `services/security/v1/debug-enabled`

##### Responses

- 200 OK status
```JSON
{
    "enabled":true
}
```
- 500 Internal Server Error (also returned when no `SecurityService` implementation is provided)
