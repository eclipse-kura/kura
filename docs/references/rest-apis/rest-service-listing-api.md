!!! note

    This API can also be accessed via the RequestHandler with app-id: `SVCLIST-V1`.

#### Get Service List
- Method: GET
- API PATH: `/services/serviceListing/v1/sortedList`

##### Responses
- 200 OK status

```JSON
{
	//Alphabetically ordered list of services running on kura
    "sortedServicesList": [
        "HttpsKeystore",
        ...
        ...
        "org.eclipse.kura.clock.ClockService",
        ...
        ...
        "org.eclipse.kura.data.DataService",
        ...
        ...
    ]
}
```

---
#### Get Service List filtereb by interfaces 
- Method: POST
- API PATH: `/services/serviceListing/v1/sortedList/byAllInterfaces`

##### Responses
- 200 OK status

```JSON
Filter example

{
  "interfacesIds": [
    "org.eclipse.kura.security.keystore.KeystoreService"
  ]
}

```

```JSON
{
    //Alphabetically ordered list of services running on kura filtered by implemented interfaces
	"sortedServicesList": [
        "HttpsKeystore",
        "SSLKeystore"
    ]
}
```

If more than one interface is present in the `interfaceIds` request body, a logic AND of those are performed.
Example:

```JSON
Filter example

{
  "interfacesIds": [
    "org.eclipse.kura.configuration.ConfigurableComponent",
    "org.eclipse.kura.watchdog.WatchdogService"
  ]
}
```
```JSON
Response:
{
    "sortedServicesList": [
        "org.eclipse.kura.watchdog.WatchdogService"
    ]
}
```

- 500 Internal Server Error
    - Can occur if body json is not correctly written
```JSON
    Example:

    {
        "interfacesIds": 
    }
```

- 400 Bad Status
    - If body json is null
    ```JSON
    Body:
    {
    }

    Response:
    {
        "message": "Bad request. interfacesIds must not be null"
    }
    ```
    - If body json is empty
    ```JSON
    Body:
    {
        "interfacesIds": [
        ]
    }

    Response:
    {
        "message": "Bad request. interfacesIds must not be empty"
    }
    ```
    - If body json contains null entry
    ```JSON
    Body:
    {
        "interfacesIds": [
            "org.eclipse.kura.configuration.ComponentConfiguration",
    
        ]
    }

    Response:
    {
        "message": "Bad request. none of the interfacesIds can be null"
    }
    ```
    - If body json contains empty entry
    ```JSON
    Body:
    {
        "interfacesIds": [
            "org.eclipse.kura.configuration.ComponentConfiguration",
            ""
        ]
    }

    Response:
    {
        "message": "Bad request. none of the interfacesIds can be null"
    }
    ```