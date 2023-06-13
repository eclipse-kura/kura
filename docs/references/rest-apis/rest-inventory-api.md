# Rest Inventory v1 API

## Global
#### GET Inventory Summary
- Method: GET
- Description: returns a list of All Inventory Items
- API PATH: `/services/inventory/v1/inventory`
##### Response
- 200 OK
```JSON
{
    "inventory":[
        {
            "name":"adduser",
            "version":"3.118",
            "type":"DEB"
        },
        {
            "name":"com.eclipsesource.jaxrs.provider.gson",
            "version":"2.3.0.201602281253",
            "type":"BUNDLE"
        },
              {
            "name":"org.eclipse.kura.example.beacon",
            "version":"1.0.500",
            "type":"DP"
        }
    ]
}
```
 --- 


## Bundles
#### GET bundles
- Method: GET
- Description: returns a list of bundles.
- API PATH: `/services/inventory/v1/bundles`
##### Response
- 200 OK
```JSON
{
    "bundles":[
        {
            "name":"org.eclipse.osgi",
            "version":"3.16.0.v20200828-0759",
            "id":0,
            "state":"ACTIVE",
            "signed":true
        },
        {
            "name":"org.eclipse.equinox.cm",
            "version":"1.4.400.v20200422-1833",
            "id":1,
            "state":"ACTIVE",
            "signed":false
        }
    ]
}
```

#### Start bundle
- Method: POST
- API PATH: `/services/inventory/v1/bundles/_start`
##### Request Body
``` JSON
{ 
"name":"org.eclipse.osgi",
}
```
##### Responses
- 200 OK status
- 400 Bad Request (Malformed Client JSON)
- 404 Resource Not Found
- 500 Internal Server Error

#### Stop bundle
- Method: POST
- API PATH: `/services/inventory/v1/bundles/_stop`
##### Request Body
``` JSON
{ 
"name":"org.eclipse.osgi",
}
```
##### Responses
- 200 OK status
- 400 Bad Request (Malformed Client JSON)
- 404 Resource Not Found
- 500 Internal Server Error

---



## Deployment Packages
#### GET packages
- Method: GET
- Description: returns a list of deployment packages.
- API PATH: `/services/inventory/v1/deploymentPackages`
##### Response
- 200 OK
```JSON
{
    "deploymentPackages":[
        {
            "name":"org.eclipse.kura.example.beacon",
            "version":"1.0.500",
            "signed":false,
            "bundles":[
                {
                    "name":"org.eclipse.kura.example.beacon",
                    "version":"1.0.500",
                    "id": 171,
                    "state": "ACTIVE",
                    "signed": false
                }
            ]
        }
    ]
}
```


## System Packages (DEB/RPM/APK)
#### GET System Packages
- Method: GET
- Description: returns a list of system packages.
- API PATH: `/services/inventory/v1/systemPackages`
##### Response
- 200 OK
```JSON
{
    "systemPackages":[
        {
            "name":"adduser",
            "version":"3.118",
            "type":"DEB"
        },
        {
            "name":"alsa-utils",
            "version":"1.1.8-2",
            "type":"DEB"
        },
        {
            "name":"ansible",
            "version":"2.7.7+dfsg-1",
            "type":"DEB"
        },
        {
            "name":"apparmor",
            "version":"2.13.2-10",
            "type":"DEB"
        },
        {
            "name":"apt",
            "version":"1.8.2.1",
            "type":"DEB"
        },
        {
            "name":"apt-listchanges",
            "version":"3.19",
            "type":"DEB"
        },
        {
            "name":"apt-transport-https",
            "version":"1.8.2.2",
            "type":"DEB"
        },
        {
            "name":"apt-utils",
            "version":"1.8.2.1",
            "type":"DEB"
        }
    ]
}
```

 --- 
## Containers
#### GET Containers
- Method: Get
- Description: returns a list of Containers.
- API PATH: `/services/inventory/v1/containers`
##### Response
- 200 OK
```JSON
{
  "containers":
  [
    {
      "name":"container_1",
      "version":"nginx:latest",
      "type":"DOCKER",
      "state":"active"
    }
  ]
}
```

#### Start container
- Method: POST
- API PATH: `/services/inventory/v1/containers/_start`
##### Request Body
``` JSON
{
    "name":"container_1"
}
```
##### Responses
- 200 OK
- 400 Bad Request (Malformed Client JSON)
- 404 Resource Not Found
- 500 Internal Server Error

#### Stop container
- Method: POST
- API PATH: `/services/inventory/v1/containers/_stop`
##### Request Body
``` JSON
{
    "name":"container_1"
}
```
##### Responses
- 200 OK
- 400 Bad Request (Malformed Client JSON)
- 404 Resource Not Found
- 500 Internal Server Error

 --- 
## Images
#### Images Images
- Method: Get
- Description: returns a list of Images.
- API PATH: `/services/inventory/v1/images`
##### Response
- 200 OK
```JSON
{
  "containers":
  [
    {
      "name":"nginx",
      "version":"latest",
      "type":"ContainerImage",
    }
  ]
}
```

#### Delete Image
- Method: POST
- API PATH: `/services/inventory/v1/images/_delete`
##### Request Body
``` JSON
{
      "name":"nginx",
      "version":"latest",
}
```
##### Responses
- 200 OK
- 400 Bad Request (Malformed Client JSON)
- 404 Resource Not Found
- 500 Internal Server Error
 
--- 
## States

%%We should probably unify these states to the same case.%%

### Bundle States
```
-   `ACTIVE`: Container is running
-   `INSTALLED`: Container is starting
-   `UNINSTALLED`: Container has failed, or is stopped
-   `UNKNOWN`: Container state can not be determined
```

### Container States
```
-   `active`: Container is running
-   `installed`: Container is starting
-   `uninstalled`: Container has failed, or is stopped
-   `unknown`: Container state can not be determined
```