# Rest Identity V2 API
!!! note

    This API can also be accessed via the RequestHandler with app-id: `IDN-V2`.


The `IdentityRestService` APIs provides methods to manage the system identities.
Unless otherwise specified, identities with `rest.identity` permissions can access these APIs.

## POST methods

#### Create User

- Description: This method allows to create a new identity in the system. Identity name must respect the [requirements enforced by the IdentityService](/gateway-configuration/authentication-and-authorization/#identities).
- Method: POST
- API PATH: `services/identity/v2/identities`

##### Request
```JSON
{
    "name": "username"
}
```

##### Responses

- 200 OK status
- 400 Missing or mispelled field `name` (eg: `nme`)
- 409 Conflict (Identity already exists)
- 500 Internal Server Error

#### Get User by Name

- Description: This method allows to get data about an identity in the system. The request body's `identity` field is used to get only the name of specific identity. It is also possible to retrieve information about the specific user's component configuration, specifying the type of interest.
- Method: POST
- API PATH: `services/identity/v2/identities/byName`

##### Request

```JSON
{
    "identity": {
        "name": "test"
    }
}
```

##### Response
```JSON
{
    "identity": {
        "name": "username"
    }
}
```

##### Request

```JSON
{
    "identity": {
        "name": "username"
    }, 
    "configurationComponents": ["AdditionalConfigurations", "AssignedPermissions", "PasswordConfiguration"]
}
```

##### Response
```JSON
{
    "identity": {
        "name": "username"
    },
    "permissionConfiguration": {
        "permissions": [
            {
                "name": "rest.identity"
            }
        ]
    },
    "passwordConfiguration": {
        "passwordChangeNeeded": false,
        "passwordAuthEnabled": true
    },
    "additionalConfigurations": {
        "configurations": []
    }
}
```

- 200 OK status
- 400 Missing or mispelled field `name` (eg: `nme`)
- 404 Identity does not exist
- 500 Internal Server Error

#### Get User Default Configuration by Name

- Description: This method allows to get the default configuration data about an identity in the system. The request body's `identity` field is used to get only the name of specific identity. It is also possible to retrieve information about the specific user's component default configuration, specifying the type of interest. This method accepts also non-existing user's name as input: in this way it's possible to retrieve which is the default configuration applied when a user is created with the `name` field only.
- Method: POST
- API PATH: `services/identity/v2/identities/default/byName`

##### Request

```JSON
{
"identity": {
        "name": "username"
    }
}
```

##### Response
```JSON
{
    "identity": {
        "name": "username"
    }
}
```

##### Request

```JSON
{
    "identity": {
        "name": "username"
    }, 
    "configurationComponents": ["AdditionalConfigurations", "AssignedPermissions", "PasswordConfiguration"]
}
```

##### Response
```JSON
{
    "identity": {
        "name": "username"
    },
    "permissionConfiguration": {
        "permissions": []
    },
    "passwordConfiguration": {
        "passwordChangeNeeded": false,
        "passwordAuthEnabled": false
    },
    "additionalConfigurations": {
        "configurations": []
    }
}
```

- 200 OK status
- 400 Missing or mispelled field `name` (eg: `nme`)
- 500 Internal Server Error

#### Create Permission

- Description: This method allows to create a new permission in the system. Permission name must respect the [requirements enforced by the IdentityService](/gateway-configuration/authentication-and-authorization/#permissions).
- Method: POST
- API PATH: `services/identity/v2/permissions`

##### Request
```JSON
{
    "name": "permission"
}
```

##### Responses

- 200 OK status
- 400 Bad Request (Permission name not valid)
- 409 Conflict (Permission already exists)
- 500 Internal Server Error

#### Validate Identity Configuration

- Description: Validates the provided list of identity configurations without performing any change to the system. It is possible to specify only the `identity` body field, or also the `configurationComponents` one.
- Method: POST
- API PATH: `services/identity/v2/identities/validate`

##### Request
```JSON
{
    "identity": {
        "name": "username"
    }, 
    "configurationComponents": ["AdditionalConfigurations", "AssignedPermissions", "PasswordConfiguration"]
}
```

##### Responses

- 200 OK status
- 400 Missing or mispelled field `name` (eg: `nme`)
- 500 Internal Server Error

## GET methods

#### Get defined permissions

- Description: This method allows you to get the list of the permissions defined in the system
- Method: GET
- API PATH: `services/identity/v2/definedPermissions`

No specific permission is required to access this resource.

##### Responses

```JSON
[
    {
        "name": "rest.identity"
    },
    {
        "name": "rest.wires.admin"
    },
    {
        "name": "kura.wires.admin"
    },
    {
        "name": "kura.network.admin"
    },
    {
        "name": "rest.network.status"
    },
    {
        "name": "test-permission"
    },
    {
        "name": "rest.keystores"
    },
    {
        "name": "rest.assets"
    },
    {
        "name": "rest.network.configuration"
    },
    {
        "name": "kura.admin"
    },
    {
        "name": "rest.cloudconnection"
    },
    {
        "name": "kura.device"
    },
    {
        "name": "rest.system"
    },
    {
        "name": "kura.maintenance"
    },
    {
        "name": "kura.packages.admin"
    },
    {
        "name": "rest.tamper.detection"
    },
    {
        "name": "rest.deploy"
    },
    {
        "name": "rest.configuration"
    },
    {
        "name": "kura.cloud.connection.admin"
    },
    {
        "name": "rest.command"
    },
    {
        "name": "rest.inventory"
    },
    {
        "name": "rest.position"
    },
    {
        "name": "rest.security"
    }
]
```

- 200 OK status
- 500 Internal Server Error

#### Get users configuration

- Description: This method allows you to get the list of the users and their configuration on the system.
- Method: GET
- API PATH: `services/identity/v2/identities`

##### Responses

```JSON
[
    {
        "identity": {
            "name": "admin"
        },
        "permissionConfiguration": {
            "permissions": [
                {
                    "name": "kura.admin"
                }
            ]
        },
        "passwordConfiguration": {
            "passwordChangeNeeded": false,
            "passwordAuthEnabled": true
        },
        "additionalConfigurations": {
            "configurations": []
        }
    },
    {
        "identity": {
            "name": "appadmin"
        },
        "permissionConfiguration": {
            "permissions": [
                {
                    "name": "kura.packages.admin"
                },
                {
                    "name": "kura.cloud.connection.admin"
                },
                {
                    "name": "kura.wires.admin"
                }
            ]
        },
        "passwordConfiguration": {
            "passwordChangeNeeded": true,
            "passwordAuthEnabled": true
        },
        "additionalConfigurations": {
            "configurations": []
        }
    },
    {
        "identity": {
            "name": "netadmin"
        },
        "permissionConfiguration": {
            "permissions": [
                {
                    "name": "kura.device"
                },
                {
                    "name": "kura.network.admin"
                },
                {
                    "name": "kura.cloud.connection.admin"
                }
            ]
        },
        "passwordConfiguration": {
            "passwordChangeNeeded": true,
            "passwordAuthEnabled": true
        },
        "additionalConfigurations": {
            "configurations": []
        }
    }
]
```

- 200 OK status
- 500 Internal Server Error

#### Get Password Strenght Requirements

- Description: This method allows you to get the password requirements.
- Method: GET
- API PATH: `services/identity/v2/passwordStrenghtRequirements`

No specific permission is required to access this resource.

##### Responses

```JSON
{
    "passwordMinimumLength": 8,
    "digitsRequired": false,
    "specialCharactersRequired": false,
    "bothCasesRequired": false
}
```

- 200 OK status
- 500 Internal Server Error

## PUT methods

#### Update Identity

- Description: This method allows to update an existing identity in the system. New passwords must respect the [requirements enforced by the IdentityService](/gateway-configuration/authentication-and-authorization/#identities).
- Method: PUT
- API PATH: `services/identity/v2/identities`

##### Request

```JSON
{
    "identity": {
        "name": "username"
    },
    "permissionConfiguration": {
        "permissions": [
            {
                "name": "rest.identity"
            }
        ]
    },
    "passwordConfiguration": {
        "passwordChangeNeeded": false,
        "passwordAuthEnabled": true,
        "password": "password123"
    }
}
```

##### Responses

- 200 OK status
- 400 Missing or mispelled field `name` (eg: `nme`)
- 500 Internal Server Error

## DELETE methods

#### Delete User

- Description: This method allows to delete an existing user in the system. The only considered field is the `name`.
- Method: DELETE
- API PATH: `services/identity/v2/identities`

##### Request
```JSON
{
    "name": "username"
}
```

##### Responses

- 200 OK status
- 404 `username` does not exist
- 400 Missing or mispelled field `name` (eg: `nme`)
- 500 Internal Server Error

#### Delete Permission

- Description: This method allows to delete an existing permission in the system. The only considered field is the `name`.
- Method: DELETE
- API PATH: `services/identity/v2/permissions`

##### Request
```JSON
{
    "name": "permission"
}
```

##### Responses

- 200 OK status
- 404 `permission` does not exist
- 400 Missing or mispelled field `name` (eg: `nme`)
- 500 Internal Server Error
