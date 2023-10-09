# Rest Identity v1 API
!!! note

    This API can also be accessed via the RequestHandler with app-id: `ID-V1`.


The `IdentityRestService` APIs provides methods to manage the system identities.
Identities with `rest.identity` permissions can access these APIs.

## POST methods

#### Set users configuration

- Description: This method allows to overwrite the users configuration in the system.
- Method: POST
- API PATH: `services/identity/v1/users/configs`

##### Request
```JSON
{
    "userConfig": [
        {
            "userName": "admin",
            "passwordChangeNeeded": false,
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
        },
        {
            "userName": "kura.user.example",
            "passwordChangeNeeded": true,
            "permissions": [
                "rest.assets"
            ]
        }
    ]
}
```

##### Responses

- 200 OK status
- 500 Internal Server Error

#### Create User

- Description: This method allows to create a new user in the system. The only accepted parameter is the userName.
- Method: POST
- API PATH: `services/identity/v1/users`

##### Request
```JSON
{
    "userName": "example"
}
```

##### Responses

- 200 OK status
- 500 Internal Server Error

## GET methods

#### Get User

- Description: This method allows to get data about an user in the system. The only accepted parameter is the userName.
- Method: GET
- API PATH: `services/identity/v1/users`

##### Request
```JSON
{
    "userName": "example"
}
```

##### Responses
```JSON
{
    "userName": "kura.user.example",
    "passwordAuthEnabled": false,
    "passwordChangeNeeded": false,
    "permissions": []
}
```

- 200 OK status
- 500 Internal Server Error

#### Get defined permissions

- Description: This method allows you to get the list of the permissions defined in the system
- API PATH: `services/identity/v1/defined-permissions`

##### Responses

- 200 OK status
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
- 500 Internal Server Error

#### Get users configuration

- Description: This method allows you to get the list of the users and their configuration on the system.
- API PATH: `services/identity/v1/users/configs`

##### Responses

- 200 OK status
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
- 500 Internal Server Error

## DELETE methods

#### Delete User

- Description: This method allows to delete an existing user in the system. The only accepted parameter is the userName.
- Method: DELETE
- API PATH: `services/identity/v1/users`

##### Request
```JSON
{
    "userName": "example",
}
```

##### Responses

- 200 OK status
- 500 Internal Server Error
