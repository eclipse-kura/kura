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

##### Request Body

```json
{
  "url": "deploymentPackageUrl"
}
```

Example:

```json
{
  "url": "http://download.eclipse.org/kura/releases/4.1.0/org.eclipse.kura.demo.heater_1.0.500.dp"
}
```

Please note that the url can refer to a `.dp` already in the device filesystem.

##### Responses
- 200 OK status
- 400 Bad request

```
"REQUEST_RECEIVED"
```

#### Install package from upload
- Description: Upload and install a Deployment Package.
- Method: POST
- API PATH: `/deploy/v2/_upload`

##### Request Body

The POST request body should be encoded in the `multipart/form-data` `enctype`, thus allowing for the upload of the Deployment Package file. The uploaded file is expected to be added in the `file` field of the form.

###### Headers

- `Content-Type`: `multipart/form-data`

###### Body (formdata)

- `file`


###### Example

Example using `curl`:

```bash
curl -X POST -k -u $USERNAME:$PASSWORD \
    --header 'Content-Type: multipart/form-data' \
    --form 'file=@"/path/to/your/file.dp"' \
    https://$ADDRESS/services/deploy/v2/_upload
```

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

#### Get Eclipse Marketplace Package Descriptor
- Description: Provides the Eclipse Marketplace Package Descriptor information of the deployment package identified by URL passed in the request.
- Method: PUT
- API PATH: `/deploy/v2/_packageDescriptor`

##### Request Body

```json
{
  "url": "deploymentPackageUrl"
}
```

Example:

```json
{
  "url": "http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=5514714"
}
```

##### Responses
- 200 OK status
- 400 Bad request

```json
{
   "nodeId":"5514714",
   "url":"https://marketplace.eclipse.org/content/ai-wire-component-eclipse-kura-5",
   "dpUrl":"https://download.eclipse.org/kura/releases/5.3.0/org.eclipse.kura.wire.ai.component.provider-1.2.0.dp",
   "minKuraVersion":"5.1.0",
   "maxKuraVersion":"",
   "currentKuraVersion":"5.4.0",
   "isCompatible":true
}
```
