---
layout: page
title:  "Keys and Certificates"
categories: [admin]
---

The framework manages directly different key pairs and trusted certificates from different keystores.
To simplify the management of such complex objects, the framework provides a dedicated section of its Administrative Web UI, a set of REST APIs for local management and a request handler (KEYS-V1) for cloud remote interaction.

## Web UI

The **Certificates List** tab in the **Security** section of the Web UI provides a simple way for the user to get the list of all the managed keys and certificates of the framework:

![]({{ site.baseurl }}/assets/images/admin/CertificateList.png)

The page allows the user to add a new Keypair or trusted certificate or to delete an existing element.

Every key pair or trusted certificate is listed by its alias, identified by the corresponding type and further identified by the keystore that is managing that element.

If the user needs to add a new entry to one of the managed KeystoreService instances, can click on the **Add** button on the top left part of the page.
The user will be guided through a process that will allow to identify the type of entry to add:

![]({{ site.baseurl }}/assets/images/admin/CertificateListAddEntry.png)

It can be either a:

- **Private/Public Key Pair**
- **Trusted Certificate**

If the user decides to add a key pair, then the wizard will provide a page like the following:

![]({{ site.baseurl }}/assets/images/admin/CertificateListAddKeyPair.png)

Here the user can specify:

- **Key Store**: the KeystoreService instance that will store and maintain the key pair
- **Storage Alias**: the alias that will be used to identify the key pair
- **Private Key**: the private key part of the key pair
- **Certificate**: the public key part of the key pair

After clicking on the Apply button, the new entry will be stored in the selected Keystore and listed along the other entries managed by the framework.

The following cryptographic algorithms are supported for Key Pairs:

- **RSA**
- **DSA**

Instead, if the user wants to load a Trusted Certificate, the Ui will change as follows:

![]({{ site.baseurl }}/assets/images/admin/CertificateListAddCertificate.png)

Here the user can specify:

- **Key Store**: the KeystoreService instance that will store and maintain the trusted certificate
- **Storage Alias**: the alias that will be used to identify the trusted certificate
- **Certificate**: the trusted certificate

The following cryptographic algorithms are supported for Trusted Certificates:

- **RSA**
- **DSA**
- **EC**

## REST APIs

The **org.eclipse.kura.core.keystore** bundle exposes a REST endpoint under the **/services/keystores** path.
The ESF REST APIs for Keys and Certificates support the following calls and are allowed to any user with **rest.keystores** permission.

Method | Path                                                                 | Roles Allowed | Encoding | Request Parameters | Description
------------------------------------------------------------------------------|---------------|------------------
GET    | /                                                                    | keystores     | JSON     | None                         | Returns the list of all the KeystoreService instances. |
GET    | /entries                                                             | keystores     | JSON     | None                         | Returns the list of all the entries managed by the KeystoreService instances. |
GET    | /entries?keystoreServicePid={keystoreServicePid}                     | keystores     | JSON     | keystoreServicePid           | Returns the list of all the entries managed by the specified KeystoreService instance. |
GET    | /entries?alias={alias}                                               | keystores     | JSON     | alias                        | Returns the list of all the entries specified by the defined alias and managed in all the available  KeystoreService instances in the framework. |
GET    | /entries/entry?keystoreServicePid={keystoreServicePid}&alias={alias} | keystores     | JSON     | keystoreServicePid and alias | Returns the entry identified by the specified keystoreServicePid and alias. |
POST   | /entries/csr                                                         | keystores     | JSON     | The reference to the key pair in a specified KeystoreService instance that will be used to generate the CSR. The request has to be associated with additional parameters that identify the algorithm used to compute and sign the CSR and the DN or the corresponding public key that needs to be countersigned. | Generates a CSR for the specified key pair in the specified KeystoreService instance, based on the parameters provided in the request |
POST   | /entries/certificate                                                 | keystores     | JSON     | The reference to the KeystoreService instance and the alias that will be used for storage. A type filed identifies the type of key that needs to be managed. | This request allows the user to upload a TrustedCertificate. |
POST   | /entries/keypair                                                     | keystores     | JSON     | To generate a new KeyPair directly in the device, the request format need to follow the references in the following paragraphs | This request allows the user to generate a new KeyPair into the device. |
DELETE | /entries                                                             | keystores     | JSON     | A JSON identifying the resource to delete. The format of the request is described in in one of the following sections | Deletes the entry in the specified KeystoreService instance |

### List All the KeystoreServices

#### Request

**URL**: https:/<gateway-ip>/services/keystores

#### Response

```json
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

#### Request

**URL**: https:/<gateway-ip>/services/keystores/entries

#### Response

```json
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

#### Request

**URL**: https:/<gateway-ip>/services/keystores/entries?keystoreServicePid=org.eclipse.kura.core.keystore.HttpsKeystore

#### Response

```json
[
    {
        "algorithm": "RSA",
        "size": 4096,
        "certificateChain": [
            "-----BEGIN CERTIFICATE-----\nMIIFkTCCA3mgAwIBAgIECtXoiDANBgkqhkiG9w0BAQsFADBZMQswCQYDVQQGEwJJ\nVDELMAkGA1UECBMCVUQxDjAMBgNVBAcTBUFtYXJvMREwDwYDVQQKEwhFdXJvdGVj\naDEMMAoGA1UECxMDRVNGMQwwCgYDVQQDEwNFU0YwHhcNMjEwNDIyMTUxNTU1WhcN\nMjQwMTE3MTUxNTU1WjBZMQswCQYDVQQGEwJJVDELMAkGA1UECBMCVUQxDjAMBgNV\nBAcTBUFtYXJvMREwDwYDVQQKEwhFdXJvdGVjaDEMMAoGA1UECxMDRVNGMQwwCgYD\nVQQDEwNFU0YwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQC7iZ3fHUQa\nTPgnvSxGZK4f6MZYfLclD74yqaCCWAztNxPQoiBoSPGdsBGBLNeFbwY0Yzg3qwXw\nYvgzLJmoXV9rSix7LgXPzsSYfUGfu7PeYTy5bG9X2UVyw9LloUM5DKnw++5F7Xy7\nF0KQQi0z6/HbbPkZ2aGyNRtMCTh1iAGy3gDh/mMnjpUYuoq1luoX1x6I77X0C+NP\nTxldVYrTeQiswItAHZmkK1R8AYedbFBgjDuTrfRODxBwESn4kQSMLJ8yHYDRm8S6\ngVz5LdkcM48UiV5hhF+bCD3UvYA00ZgZm2oOG1ONchYrE7pJr7eQVCYaXkS1lALB\nKaVJzn03wiLJJv1FYLmGt5J/MwfqyCtBTLlieEVfwnxFCkymtews6SYK32e9q/uJ\nfcdpWH7tOoarnAf7j5mE84rRU3HqzghK0bMxntfrSH3t18ZUt1/4Qx78WfiM1Te3\nJtnWBqUNJtX6lgT8IxTWwyEqD183tyKIo8hPGyeJrzWA5RL5hYF5rCNTWzqz5Upi\n0b/YI5K09+Rn8XmEzzaWjFq5zu6/WpqwPRA8kc2RAEA2scnOT+3yl9Lof/M7BrfL\nMdjVOZ4MfXgl/fhFyd16AObXuZRUIeiWowKtEiNaiUn8paLDxG+LNV7p5wEQCZZI\n+MsXMMp6G8Te4yILLCcGov7OkO2wx4GPWQIDAQABo2EwXzAxBgNVHSUEKjAoBggr\nBgEFBQcDAQYIKwYBBQUHAwIGCCsGAQUFBwMDBggrBgEFBQcDCDALBgNVHQ8EBAMC\nAvwwHQYDVR0OBBYEFKM5PlHoe8qFC6w0quGacazGWE/LMA0GCSqGSIb3DQEBCwUA\nA4ICAQBvpXmbS9LN8n0A+uq+tM3CNtF3YotWRbQHIGJAFTvdq3003W3CVdmykFc8\n9Kz8PoY1swBJms7GKjQLkqgTHoq6jU/cIXw+CoLQWmvAugva5C1u/5AHJZqTC06J\nGZyn1Z9N5Lp0XcgogEyhxdbkHniv7jvcmbCurQijZc9nsd5St7e1pT0Co7KKI6Ff\nODdVP6kZYBzKo4t20tATdAZJ8t7YHNKNq7ZVs1ej9oYUmmQieNXuE4UoHe5hzVQw\n567cNHWcTHJoyPve03TSQV91wp5rRUKZm2p0WtFNuv22f5p5sQmttsJltzHCgTwE\nK0j6qYKnXiq+EQs0A3uF9uiIB/KEDLjxscstqsQGFCFOmjA3GSbmJiKCnss3HkNn\naknT7XCV6tqgDOfPnNzbWJODjYZ+V0DyNY5uqkG2cyREm/qGbH1kLEXhqdWbKqEs\nsdW6x8p0ImTaPuRl3XEmXbolavIq+FTtOSz8vW1PsdD3quO6krrwiQMXKv1ZMjup\nDGIZZ4hUUhN84efjlZyoFRvPRvZ8YvjjrHXLij0vcRxndlicevwl5ezlm0LBOpsT\nkI2uWrbSbxlue/XdgwFCbN0+mXX88fGj6cjhpvd/xnwHaDHfSG9UoU149LJb6ZIZ\nru+07QriQQxK8V7AdPr6bhmKPxbbFenvSQmsmgjAY93qtanbNg==\n-----END CERTIFICATE-----"
        ],
        "keystoreServicePid": "org.eclipse.kura.core.keystore.HttpsKeystore",
        "alias": "localhost",
        "type": "PRIVATE_KEY"
    }
]
```

### Get All the Entries by Alias

#### Request

**URL**: https:/<gateway-ip>/services/keystores/entries?alias=localhost

#### Response

```json
[
    {
        "algorithm": "RSA",
        "size": 4096,
        "certificateChain": [
            "-----BEGIN CERTIFICATE-----\nMIIFkTCCA3mgAwIBAgIECtXoiDANBgkqhkiG9w0BAQsFADBZMQswCQYDVQQGEwJJ\nVDELMAkGA1UECBMCVUQxDjAMBgNVBAcTBUFtYXJvMREwDwYDVQQKEwhFdXJvdGVj\naDEMMAoGA1UECxMDRVNGMQwwCgYDVQQDEwNFU0YwHhcNMjEwNDIyMTUxNTU1WhcN\nMjQwMTE3MTUxNTU1WjBZMQswCQYDVQQGEwJJVDELMAkGA1UECBMCVUQxDjAMBgNV\nBAcTBUFtYXJvMREwDwYDVQQKEwhFdXJvdGVjaDEMMAoGA1UECxMDRVNGMQwwCgYD\nVQQDEwNFU0YwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQC7iZ3fHUQa\nTPgnvSxGZK4f6MZYfLclD74yqaCCWAztNxPQoiBoSPGdsBGBLNeFbwY0Yzg3qwXw\nYvgzLJmoXV9rSix7LgXPzsSYfUGfu7PeYTy5bG9X2UVyw9LloUM5DKnw++5F7Xy7\nF0KQQi0z6/HbbPkZ2aGyNRtMCTh1iAGy3gDh/mMnjpUYuoq1luoX1x6I77X0C+NP\nTxldVYrTeQiswItAHZmkK1R8AYedbFBgjDuTrfRODxBwESn4kQSMLJ8yHYDRm8S6\ngVz5LdkcM48UiV5hhF+bCD3UvYA00ZgZm2oOG1ONchYrE7pJr7eQVCYaXkS1lALB\nKaVJzn03wiLJJv1FYLmGt5J/MwfqyCtBTLlieEVfwnxFCkymtews6SYK32e9q/uJ\nfcdpWH7tOoarnAf7j5mE84rRU3HqzghK0bMxntfrSH3t18ZUt1/4Qx78WfiM1Te3\nJtnWBqUNJtX6lgT8IxTWwyEqD183tyKIo8hPGyeJrzWA5RL5hYF5rCNTWzqz5Upi\n0b/YI5K09+Rn8XmEzzaWjFq5zu6/WpqwPRA8kc2RAEA2scnOT+3yl9Lof/M7BrfL\nMdjVOZ4MfXgl/fhFyd16AObXuZRUIeiWowKtEiNaiUn8paLDxG+LNV7p5wEQCZZI\n+MsXMMp6G8Te4yILLCcGov7OkO2wx4GPWQIDAQABo2EwXzAxBgNVHSUEKjAoBggr\nBgEFBQcDAQYIKwYBBQUHAwIGCCsGAQUFBwMDBggrBgEFBQcDCDALBgNVHQ8EBAMC\nAvwwHQYDVR0OBBYEFKM5PlHoe8qFC6w0quGacazGWE/LMA0GCSqGSIb3DQEBCwUA\nA4ICAQBvpXmbS9LN8n0A+uq+tM3CNtF3YotWRbQHIGJAFTvdq3003W3CVdmykFc8\n9Kz8PoY1swBJms7GKjQLkqgTHoq6jU/cIXw+CoLQWmvAugva5C1u/5AHJZqTC06J\nGZyn1Z9N5Lp0XcgogEyhxdbkHniv7jvcmbCurQijZc9nsd5St7e1pT0Co7KKI6Ff\nODdVP6kZYBzKo4t20tATdAZJ8t7YHNKNq7ZVs1ej9oYUmmQieNXuE4UoHe5hzVQw\n567cNHWcTHJoyPve03TSQV91wp5rRUKZm2p0WtFNuv22f5p5sQmttsJltzHCgTwE\nK0j6qYKnXiq+EQs0A3uF9uiIB/KEDLjxscstqsQGFCFOmjA3GSbmJiKCnss3HkNn\naknT7XCV6tqgDOfPnNzbWJODjYZ+V0DyNY5uqkG2cyREm/qGbH1kLEXhqdWbKqEs\nsdW6x8p0ImTaPuRl3XEmXbolavIq+FTtOSz8vW1PsdD3quO6krrwiQMXKv1ZMjup\nDGIZZ4hUUhN84efjlZyoFRvPRvZ8YvjjrHXLij0vcRxndlicevwl5ezlm0LBOpsT\nkI2uWrbSbxlue/XdgwFCbN0+mXX88fGj6cjhpvd/xnwHaDHfSG9UoU149LJb6ZIZ\nru+07QriQQxK8V7AdPr6bhmKPxbbFenvSQmsmgjAY93qtanbNg==\n-----END CERTIFICATE-----"
        ],
        "keystoreServicePid": "org.eclipse.kura.core.keystore.HttpsKeystore",
        "alias": "localhost",
        "type": "PRIVATE_KEY"
    }
]
```

### Get Specific Entry

#### Request

**URL**: https:/<gateway-ip>/services/keystores/entries/entry?keystoreServicePid=org.eclipse.kura.core.keystore.HttpsKeystore&alias=localhost

#### Response

```json
{
    "algorithm": "RSA",
    "size": 4096,
    "certificateChain": [
        "-----BEGIN CERTIFICATE-----\nMIIFkTCCA3mgAwIBAgIECtXoiDANBgkqhkiG9w0BAQsFADBZMQswCQYDVQQGEwJJ\nVDELMAkGA1UECBMCVUQxDjAMBgNVBAcTBUFtYXJvMREwDwYDVQQKEwhFdXJvdGVj\naDEMMAoGA1UECxMDRVNGMQwwCgYDVQQDEwNFU0YwHhcNMjEwNDIyMTUxNTU1WhcN\nMjQwMTE3MTUxNTU1WjBZMQswCQYDVQQGEwJJVDELMAkGA1UECBMCVUQxDjAMBgNV\nBAcTBUFtYXJvMREwDwYDVQQKEwhFdXJvdGVjaDEMMAoGA1UECxMDRVNGMQwwCgYD\nVQQDEwNFU0YwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQC7iZ3fHUQa\nTPgnvSxGZK4f6MZYfLclD74yqaCCWAztNxPQoiBoSPGdsBGBLNeFbwY0Yzg3qwXw\nYvgzLJmoXV9rSix7LgXPzsSYfUGfu7PeYTy5bG9X2UVyw9LloUM5DKnw++5F7Xy7\nF0KQQi0z6/HbbPkZ2aGyNRtMCTh1iAGy3gDh/mMnjpUYuoq1luoX1x6I77X0C+NP\nTxldVYrTeQiswItAHZmkK1R8AYedbFBgjDuTrfRODxBwESn4kQSMLJ8yHYDRm8S6\ngVz5LdkcM48UiV5hhF+bCD3UvYA00ZgZm2oOG1ONchYrE7pJr7eQVCYaXkS1lALB\nKaVJzn03wiLJJv1FYLmGt5J/MwfqyCtBTLlieEVfwnxFCkymtews6SYK32e9q/uJ\nfcdpWH7tOoarnAf7j5mE84rRU3HqzghK0bMxntfrSH3t18ZUt1/4Qx78WfiM1Te3\nJtnWBqUNJtX6lgT8IxTWwyEqD183tyKIo8hPGyeJrzWA5RL5hYF5rCNTWzqz5Upi\n0b/YI5K09+Rn8XmEzzaWjFq5zu6/WpqwPRA8kc2RAEA2scnOT+3yl9Lof/M7BrfL\nMdjVOZ4MfXgl/fhFyd16AObXuZRUIeiWowKtEiNaiUn8paLDxG+LNV7p5wEQCZZI\n+MsXMMp6G8Te4yILLCcGov7OkO2wx4GPWQIDAQABo2EwXzAxBgNVHSUEKjAoBggr\nBgEFBQcDAQYIKwYBBQUHAwIGCCsGAQUFBwMDBggrBgEFBQcDCDALBgNVHQ8EBAMC\nAvwwHQYDVR0OBBYEFKM5PlHoe8qFC6w0quGacazGWE/LMA0GCSqGSIb3DQEBCwUA\nA4ICAQBvpXmbS9LN8n0A+uq+tM3CNtF3YotWRbQHIGJAFTvdq3003W3CVdmykFc8\n9Kz8PoY1swBJms7GKjQLkqgTHoq6jU/cIXw+CoLQWmvAugva5C1u/5AHJZqTC06J\nGZyn1Z9N5Lp0XcgogEyhxdbkHniv7jvcmbCurQijZc9nsd5St7e1pT0Co7KKI6Ff\nODdVP6kZYBzKo4t20tATdAZJ8t7YHNKNq7ZVs1ej9oYUmmQieNXuE4UoHe5hzVQw\n567cNHWcTHJoyPve03TSQV91wp5rRUKZm2p0WtFNuv22f5p5sQmttsJltzHCgTwE\nK0j6qYKnXiq+EQs0A3uF9uiIB/KEDLjxscstqsQGFCFOmjA3GSbmJiKCnss3HkNn\naknT7XCV6tqgDOfPnNzbWJODjYZ+V0DyNY5uqkG2cyREm/qGbH1kLEXhqdWbKqEs\nsdW6x8p0ImTaPuRl3XEmXbolavIq+FTtOSz8vW1PsdD3quO6krrwiQMXKv1ZMjup\nDGIZZ4hUUhN84efjlZyoFRvPRvZ8YvjjrHXLij0vcRxndlicevwl5ezlm0LBOpsT\nkI2uWrbSbxlue/XdgwFCbN0+mXX88fGj6cjhpvd/xnwHaDHfSG9UoU149LJb6ZIZ\nru+07QriQQxK8V7AdPr6bhmKPxbbFenvSQmsmgjAY93qtanbNg==\n-----END CERTIFICATE-----"
    ],
    "keystoreServicePid": "org.eclipse.kura.core.keystore.HttpsKeystore",
    "alias": "localhost",
    "type": "PRIVATE_KEY"
}
```

### Get the CSR for a KeyPair

#### Request

**URL**: https:/<gateway-ip>/services/keystores/entries/csrkeystoreServicePid=org.eclipse.kura.core.keystore.HttpsKeystore&alias=localhost`

#### Request Body:

```json
{ 
    "keystoreServicePid":"org.eclipse.kura.core.keystore.HttpsKeystore",
    "alias":"localhost",
    "signatureAlgorithm" : "SHA256withRSA",
    "attributes" : "CN=Kura, OU=IoT, O=Eclipse, C=US"
}
```

### Store Trusted Certificate

#### Request

**URL**: https:/<gateway-ip>/services/keystores/entries/certificate

#### Request Body:

```json
{
    "keystoreServicePid":"MyKeystore",
    "alias":"myCertTest99",
    "certificate":"-----BEGIN CERTIFICATE-----
        MIIDdzCCAl+gAwIBAgIEQsO0gDANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV
        bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD
        VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du
        MB4XDTIxMDQxNDA4MDIyOFoXDTIxMDcxMzA4MDIyOFowbDEQMA4GA1UEBhMHVW5r
        bm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE
        ChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCC
        ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJSWJDxu8UNC4JGOgK31WCvz
        NKy2ONH+jTVKnBY7Ckb1hljJY0sKO55aG1HNDfkev2lJTsPIz0nJjNsqBvB1flvf
        r6XVCxdN0yxvU5g9SpRxE/iiPX0Qt7463OfzyKW97haJrrhF005RHYNcORMY/Phj
        hFDnZhtAwpbQLzq2UuIZ7okJsx0IgRbjH71ZZuvYCqG7Ct/bp1D7w3tT7gTbIKYH
        ppQyG9rJDEh9+cr9Hyk8Gz7aAbPT/wMH+/vXDjH2j/M1Tmed0ajuGCJumaTQ4eHs
        9xW3B3ugycb6e7Osl/4ESRO5RQL1k2GBONv10OrKDoZ5b66xwSJmC/w3BRWQ1cMC
        AwEAAaMhMB8wHQYDVR0OBBYEFPospETb5HNeD/DmS9mwt+v/AYq/MA0GCSqGSIb3
        DQEBCwUAA4IBAQBxMe1xQVQKt36A5qVlEZyxI9eb6eQRlYzorOgP2tFaOsvDPpRI
        CALhPmxgQl/5QvKFfCXKoxWj1Spg4sF6fJp6jhSjLpmChS9lf5fRaWS20/pxIddM
        10diq3r6HxLKSxCYK7Pf5scOeZquvwfo8Kxye01bvCMFf1s1K3ZEZszk5Oo2MnWU
        U22YnXfZm1C0h2WMUcou35A7CeVAHPWI0Rvefojv1qYlQScJOkCN5lO6C/1qvRhq
        nDQdQN/m1HQbpfh2DD6F33nBjkyLQyMRF8uMnspLrLLj8lecSTJZO4fGJOaIXh3O
        44da9A02FAf5nRRQpwP2x/4IZ5RTRBzrqbqD
        -----END CERTIFICATE-----"
}
```

### Generate KeyPair

#### Request

**URL**: https:/<gateway-ip>/services/keystores/entries/keypair

#### Request Body:

```json
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

#### Request

**URL**: https:/<gateway-ip>/services/keystores/entries

#### Request Body:

```json
{
    "keystoreServicePid" : "MyKeystore",
    "alias" : "mycerttestec"
}
```

## KEYS-V1 Request Handler

Mapping the previously defined REST APIs, the framework exposed to the remote cloud platforms a request handler named KEYS-V1 that allows the remote user to list and manage the keystores, the keys and the certificates in the framework.
The request handler exposes also the capability to generate on the edge a CSR that can be countersigned remotely by a trusted CA.

More details on the KEYS-V1 format are available in the [MQTT Namespace](ref/../../ref/mqtt-namespace.html#remote-keystore-management-via-mqtt) page.