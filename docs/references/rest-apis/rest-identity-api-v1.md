# Rest Identity V1 API
!!! warning

    This API id deprecated and superseded by the [Identity V2 REST APIs](./rest-identity-api-v2.md).

!!! note

    This API can also be accessed via the RequestHandler with app-id: `IDN-V1`.


The `IdentityRestService` APIs provides methods to manage the system identities.
Unless otherwise specified, identities with `rest.identity` permissions can access these APIs.

!!! note
    Starting from Kura 5.6.3, an identity holding the `rest.identity` permission, but not `kura.admin`, can only create, update or delete identities whose permissions are a subset of its own permissions, and can only assign permissions that it holds; requests that do not satisfy these rules are rejected with the `403 Forbidden` status. Requests received through the request handler are not affected. See [Identity and permission management through the REST APIs](../../gateway-configuration/authentication-and-authorization.md#identity-and-permission-management-through-the-rest-apis) for details and for the startup property that restores the previous behaviour.

## POST methods

#### Create User

- Description: This method allows to create a new user in the system.
- Method: POST
- API PATH: `services/identity/v1/identities`

##### Request
```JSON
{
    "userName": "username",
    "password": "password",
    "passwordChangeNeeded": false,
    "passwordAuthEnabled": true,
    "permissions": [
        "rest.identity"
    ]
}
```

##### Responses

- 200 OK status
- 400 Bad Request (Password strenght requirements not satisfied)
- 403 Forbidden (since Kura 5.6.3, the target identity or the assigned permissions are not within the permissions of the requesting identity)
- 500 Internal Server Error

#### Get User by Name

- Description: This method allows to get data about an user in the system. The only considered field is the userName.
- Method: POST
- API PATH: `services/identity/v1/identities/byName`

##### Request
```JSON
{
    "userName": "username"
}
```

##### Responses
```JSON
{
    "userName": "kura.user.username",
    "passwordAuthEnabled": false,
    "passwordChangeNeeded": false,
    "permissions": []
}
```

- 200 OK status
- 404 `userName` does not exist
- 500 Internal Server Error

## GET methods

#### Get defined permissions

- Description: This method allows you to get the list of the permissions defined in the system
- Method: GET
- API PATH: `services/identity/v1/definedPermissions`

No specific permission is required to access this resource.

##### Responses

```JSON
{
    "permissions": [
        "rest.command",
        "rest.inventory",
        "rest.configuration",
        "rest.tamper.detection",
        "rest.security",
        "kura.cloud.connection.admin",
        "rest.position",
        "kura.packages.admin",
        "kura.device",
        "rest.wires.admin",
        "kura.admin",
        "rest.keystores",
        "rest.assets",
        "rest.system",
        "kura.maintenance",
        "kura.wires.admin",
        "rest.identity"
    ]
}
```

- 200 OK status
- 500 Internal Server Error

#### Get users configuration

- Description: This method allows you to get the list of the users and their configuration on the system.
- Method: GET
- API PATH: `services/identity/v1/identities`

##### Responses

```JSON
{
    "userConfig": [
        {
            "userName": "admin",
            "passwordAuthEnabled": true,
            "passwordChangeNeeded": false,
            "permissions": [
                "kura.admin"
            ]
        },
        {
            "userName": "appadmin",
            "passwordAuthEnabled": true,
            "passwordChangeNeeded": true,
            "permissions": [
                "kura.cloud.connection.admin",
                "kura.packages.admin",
                "kura.wires.admin"
            ]
        }
    ]
}
```

- 200 OK status
- 500 Internal Server Error

#### Get password requirements

- Description: This method allows you to get the password requirements.
- Method: GET
- API PATH: `services/identity/v1/passwordRequirements`

No specific permission is required to access this resource.

##### Responses

```JSON
{
    "passwordMinimumLength": 8,
    "passwordRequireDigits": false,
    "passwordRequireSpecialChars": false,
    "passwordRequireBothCases": false
}
```

- 200 OK status
- 500 Internal Server Error

## PUT methods

#### Update User

- Description: This method allows to update an existing user in the system.
- Method: PUT
- API PATH: `services/identity/v1/identities`

##### Request

```JSON
{
    "userName": "username",
    "password": "password",
    "passwordChangeNeeded": false,
    "passwordAuthEnabled": true,
    "permissions": [
        "rest.identity"
    ]
}
```

##### Responses

- 200 OK status
- 400 Bad Request (Password strenght requirements not satisfied)
- 403 Forbidden (since Kura 5.6.3, the target identity or the assigned permissions are not within the permissions of the requesting identity)
- 500 Internal Server Error

## DELETE methods

#### Delete User

- Description: This method allows to delete an existing user in the system. The only considered field is the userName.
- Method: DELETE
- API PATH: `services/identity/v1/identities`

##### Request
```JSON
{
    "userName": "username"
}
```

##### Responses

- 200 OK status
- 403 Forbidden (since Kura 5.6.3, the permissions of the target identity are not within the permissions of the requesting identity)
- 404 `userName` does not exist
- 500 Internal Server Error
