# Keys and Certificates

The framework manages directly different key pairs and trusted certificates from different keystores.
To simplify the management of such complex objects, the framework provides a dedicated section of its Administrative Web UI, a set of REST APIs for local management and a request handler (KEYS-V1 and KEYS-V2) for cloud remote interaction.

## Web UI

The **Certificates List** tab in the **Security** section of the Kura Web UI provides a simple way for the user to get the list of all the managed keys and certificates of the framework:

![Keys and Certificates](./images/keys-and-certificates.png)

The page allows the user to add a new Keypair or trusted certificate or to delete an existing element.

Every key pair or trusted certificate is listed by its alias, identified by the corresponding type and further identified by the keystore that is managing that element.

If the user needs to add a new entry to one of the managed KeystoreService instances, can click on the **Add** button on the top left part of the page. The user will be guided through a process that will allow to identify the type of entry to add:

![Keys and Certificates New Entry](./images/keys-and-certificates-new-entry.png)

It can be either a:

- **Private/Public Key Pair**
- **Trusted Certificate**

If the user decides to add a key pair, then the wizard will provide a page like the following:

![Keys and Certificates New Keypair](./images/keys-and-certificates-new-keypair.png)

Here the user can specify:
- **Key Store**: the KeystoreService instance that will store and maintain the key pair
- **Storage Alias**: the alias that will be used to identify the key pair
- **Private Key**: the private key part of the key pair
- **Certificate**: the public key part of the key pair

After clicking on the **Apply** button, the new entry will be stored in the selected Keystore and listed along the other entries managed by the framework.

The following cryptographic algorithms are supported for **Key Pairs**:
- **RSA**
- **DSA**

Instead, if the user wants to load a Trusted Certificate, the Ui will change as follows:

![Keys and Certificates New Certificate](./images/keys-and-certificates-new-certificate.png)

Here the user can specify:
- **Key Store**: the KeystoreService instance that will store and maintain the trusted certificate
- **Storage Alias**: the alias that will be used to identify the trusted certificate
- **Certificate**: the trusted certificate

The following cryptographic algorithms are supported for **Trusted Certificates**:
- **RSA**
- **DSA**
- **EC**

## REST APIs

## keystores/v1

The `org.eclipse.kura.core.keystore` bundle exposes a REST endpoint under the `/services/keystores/v1` path.
The Kura REST APIs for Keys and Certificates support the following calls and are allowed to any user with `rest.keystores` permission.

| Method | Path | Roles allowed | Encoding | Request parameters | Description |
| ------ | ---- | ------------- | -------- | ------------------ | ----------- |
| GET | `/` | keystores | JSON | None | Returns the list of all the KeystoreService instances. |
| GET | `/entries` | keystores | JSON | None | Returns the list of all the entries managed by the KeystoreService instances. |
| GET | `/entries?keystoreServicePid={keystoreServicePid}` | keystores| JSON| keystoreServicePid | Returns the list of all the entries managed by the specified KeystoreService instance. |
| GET | `/entries?alias={alias}` | keystores | JSON | alias | Returns the list of all the entries specified by the defined alias and managed in all the available  KeystoreService instances in the framework. |
| GET | `/entries/entry?keystoreServicePid={keystoreServicePid}&alias={alias}` | keystores | JSON | keystoreServicePid and alias | Returns the entry identified by the specified keystoreServicePid and alias. |
| POST | `/entries/csr` | keystores | JSON | The reference to the key pair in a specified KeystoreService instance that will be used to generate the CSR. The request has to be associated with additional parameters that identify the algorithm used to compute and sign the CSR and the DN or the corresponding public key that needs to be countersigned. | Generates a CSR for the specified key pair in the specified KeystoreService instance, based on the parameters provided in the request. |
| POST | `/entries/certificate` | keystores | JSON | The reference to the KeystoreService instance and the alias that will be used for storage. A type filed identifies the type of key that needs to be managed. | This request allows the user to upload a TrustedCertificate. |
| POST | `/entries/keypair` | keystores | JSON | To generate a new KeyPair directly in the device, the request format need to follow the references in the following paragraphs. | This request allows the user to generate a new KeyPair into the device. |
| DELETE | `/entries` | keystores | JSON | A JSON identifying the resource to delete. The format of the request is described in in one of the following sections. | Deletes the entry in the specified KeystoreService instance. |

### List All the KeystoreServices

**Request**: URL - `https://<gateway-ip>/services/keystores/v1`

**Response**:

```JSON
[
    {
        "keystoreServicePid": "org.eclipse.kura.core.keystore.SSLKeystore",
        "type": "jks",
        "size": 4
    },
    {
        "keystoreServicePid": "org.eclipse.kura.crypto.CryptoService",
        "type": "jks",
        "size": 3
    },
    {
        "keystoreServicePid": "org.eclipse.kura.core.keystore.HttpsKeystore",
        "type": "jks",
        "size": 1
    },
    {
        "keystoreServicePid": "org.eclipse.kura.core.keystore.DMKeystore",
        "type": "jks",
        "size": 1
    }
]
```

### Get all the Managed Entries

**Request**: URL - `https://<gateway-ip>/services/keystores/v1/entries`

**Response**:

```JSON
[
    {
        "subjectDN": "OU=Go Daddy Class 2 Certification Authority, O=\"The Go Daddy Group, Inc.\", C=US",
        "issuer": "OU=Go Daddy Class 2 Certification Authority,O=The Go Daddy Group\\, Inc.,C=US",
        "startDate": "Tue, 29 Jun 2004 17:06:20 GMT",
        "expirationDate": "Thu, 29 Jun 2034 17:06:20 GMT",
        "algorithm": "SHA1withRSA",
        "size": 2048,
        "keystoreServicePid": "org.eclipse.kura.core.keystore.SSLKeystore",
        "alias": "ca-godaddyclass2ca",
        "type": "TRUSTED_CERTIFICATE"
    },
    {
        "algorithm": "RSA",
        "size": 4096,
        "keystoreServicePid": "org.eclipse.kura.core.keystore.HttpsKeystore",
        "alias": "localhost",
        "type": "PRIVATE_KEY"
    }
]
```

### Get All the Entries by KeystoreService

**Request**: URL - `https://<gateway-ip>/services/keystores/v1/entries?keystoreServicePid=org.eclipse.kura.core.keystore.HttpsKeystore`

**Response**:

```JSON
[
    {
        "algorithm": "RSA",
        "size": 4096,
        "certificateChain": [
            "-----BEGIN CERTIFICATE-----\n<CERTIFICATE>\n-----END CERTIFICATE-----"
        ],
        "keystoreServicePid": "org.eclipse.kura.core.keystore.HttpsKeystore",
        "alias": "localhost",
        "type": "PRIVATE_KEY"
    }
]
```

### Get All the Entries by Alias

**Request**: URL - `https://<gateway-ip>/services/keystores/v1/entries?alias=localhost`

**Response**:

```JSON
[
    {
        "algorithm": "RSA",
        "size": 4096,
        "certificateChain": [
            "-----BEGIN CERTIFICATE-----\n<CERTIFICATE>\n-----END CERTIFICATE-----"
        ],
        "keystoreServicePid": "org.eclipse.kura.core.keystore.HttpsKeystore",
        "alias": "localhost",
        "type": "PRIVATE_KEY"
    }
]
```

### Get Specific Entry

**Request**: URL - `https://<gateway-ip>/services/keystores/v1/entries/entry?keystoreServicePid=org.eclipse.kura.core.keystore.HttpsKeystore&alias=localhost`

**Response**:

```JSON
{
    "algorithm": "RSA",
    "size": 4096,
    "certificateChain": [
        "-----BEGIN CERTIFICATE-----\n<CERTIFICATE>-----END CERTIFICATE-----"
    ],
    "keystoreServicePid": "org.eclipse.kura.core.keystore.HttpsKeystore",
    "alias": "localhost",
    "type": "PRIVATE_KEY"
}
```

### Get the CSR for a KeyPair

**Request**: URL - `https://<gateway-ip>/services/keystores/v1/entries/csr`keystoreServicePid=org.eclipse.kura.core.keystore.HttpsKeystore&alias=localhost`

**Request body**:

```JSON
{ 
    "keystoreServicePid":"org.eclipse.kura.core.keystore.HttpsKeystore",
    "alias":"localhost",
    "signatureAlgorithm" : "SHA256withRSA",
    "attributes" : "CN=Kura, OU=IoT, O=Eclipse, C=US"
}
```

### Store Trusted Certificate

**Request**: URL - `https://<gateway-ip>/services/keystores/v1/entries/certificate`

**Request body**:

```JSON
{
    "keystoreServicePid":"MyKeystore",
    "alias":"myCertTest99",
    "certificate":"-----BEGIN CERTIFICATE-----
        <CERTIFICATE>
        -----END CERTIFICATE-----"
}
```

### Generate KeyPair

**Request**: URL - `https://<gateway-ip>/services/keystores/v1/entries/keypair`

**Request body**:

```JSON
{
    "keystoreServicePid":"MyKeystore",
    "alias":"keypair1",
    "algorithm" : "RSA",
    "size": 1024,
    "signatureAlgorithm" : "SHA256WithRSA",
    "attributes" : "CN=Kura, OU=IoT, O=Eclipse, C=US"
}
```

### Delete Entry

**Request**: URL - `https://<gateway-ip>/services/keystores/v1/entries`

**Request body**:

```JSON
{
    "keystoreServicePid" : "MyKeystore",
    "alias" : "mycerttestec"
}
```

## KEYS-V1 Request Handler

Mapping the previously defined REST APIs, the framework exposed to the remote cloud platforms a request handler named **KEYS-V1** that allows the remote user to list and manage the keystores, the keys and the certificates in the framework.

The request handler exposes also the capability to generate on the edge a CSR that can be countersigned remotely by a trusted CA.

## keystores/v2

Starting from Kura 5.4.0, the `keystores/v2` REST API is also available, it supports all of the request endpoints of `keystores/v1` plus an additional endpoint that allows to upload and modify private key entries:

| Method | Path | Roles allowed | Encoding | Request parameters | Description |
| ------ | ---- | ------------- | -------- | ------------------ | ----------- |
| POST | `/entries/privatekey` | keystores | JSON | To upload a new private key entry directly in the device, the request format need to follow the references in the following paragraphs. | This request allows the user to upload a new private key entry into the device or to modify the certificate chain of an existing one. |

### Uploading a Private Key Entry

**Request**: URL - `https://<gateway-ip>/services/keystores/v1/entries/privatekey`

**Request body**:

The request should include the private key in unencrypted PEM format and the certificate chain in PEM format, the first certificate in the `certificateChain` list must use the public key associated with the private key supplied as the `privateKey` parameter.

The device will overwrite the entry with the provided alias if it already exists.

WARINING: Please use this endpoint through a secure connection.

```JSON
{
    "keystoreServicePid":"MyKeystore",
    "alias":"keypair1",
    "privateKey":"-----BEGIN RSA PRIVATE KEY-----\n...\n-----END RSA PRIVATE KEY-----",
    "certificateChain":[
        "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
        "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
    ]
}
```

### Updating a Private Key Entry

**Request**: URL - `https://<gateway-ip>/services/keystores/v1/entries/privatekey`

**Request body**:

In order to update the certificate chain associated to a specific private key entry it is possible to use the same format as previous request and omit the `privateKey` parameter.

In this case the certificate chain of the existing entry will be replaced with the one specified in the request and the existing private key will be retained.

This request can be useful for example to create a CSR on the device, sign it externally and then updating the corresponding entry with the resulting certificate.

```JSON
{
    "keystoreServicePid":"MyKeystore",
    "alias":"keypair1",
    "certificateChain":[
        "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
        "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
    ]
}
```