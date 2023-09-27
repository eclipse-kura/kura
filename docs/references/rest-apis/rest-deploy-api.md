# Rest Deploy v2 API

The `DeploymentRestService` APIs provides methods to manage the installed deployment packages.
Identities with `rest.deploy` permissions can access these APIs.

#### Get installed packages
- Description: Provides the list of all the deployment packages installed and tracked by the framework.
- Method: GET
- API PATH: `/deploy/v2/`

##### Responses
- 200 OK status

```JSON
[{ "name": "packageName", "version": "packageVersion"}]
```

#### Install package from URL
- Description: Installs the deployment package specified in the InstallRequest. If the request was already issued for the same InstallRequest, it returns the status of the installation process.
- Method: POST
- API PATH: `/deploy/v2/_install`

##### Responses
- 200 OK status
- 400 Bad request

```
"REQUEST_RECEIVED"
```

#### Uninstall a package
- Description: Uninstalls the deployment package identified by the specified name. If the request was already issued, it reports the status of the uninstallation operation.
- Method: DELETE
- API PATH: `/deploy/v2/{name}`

##### Responses
- 200 OK status

```
"REQUEST_RECEIVED"
```

