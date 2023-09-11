# Rest Security v1 API
!!! note

    This API can also be accessed via the RequestHandler with app-id: `SEC-V1`.


The `SecurityRestService` APIs provides methods to manage the system security.
Identities with `rest.security` permissions can access these APIs.

## POST methods

#### Security policy fingerprint reload

- Description: This method allows the reload of the security policy's fingerprint
- Method: POST
- API PATH: `services/security/v1/security-policy-fingerprint/reload`

##### Responses

- 200 OK status
- 500 Internal Server Error

#### Reload command line fingerprint

- Description: This method allows the reload of the command line fingerprint
- Method: POST
- API PATH: `services/security/v1/command-line-fingerprint/reload`

##### Responses

- 200 OK status
- 500 Internal Server Error

## GET methods

#### Debug enabled

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
- 500 Internal Server Error
