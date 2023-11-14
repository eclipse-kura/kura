# Session V1 Rest APIs

The session management REST APIs allow to authenticate and establish an HTTP session.
Using this APIs and creating a session is the recommended way for interact with Kura rest APIs from a browser based application.

The supported workflows are the following:

### Login and resource access workflow

1. Try calling the [GET/xsrfToken](#getxsrftoken) to get an XSRF token, if the request succeeds a vaild session is already available, it is possible to proceed to step 4.
  
    - It is not necessary to call [GET/xsrfToken](#getxsrftoken) again until the current session expires, the obtained token is valid as long as the current session is valid.

2. Call the [POST/login/password](#postloginpassword) or [POST/login/certificate](#postlogincertificate) providing the credentials to create a new session.
  
    - This request will return a session cookie with the response. The session cookie name is currently `JSESSIONID`. It is important to provide the received cookies in successive requests using the [Cookie](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cookie) HTTP request header. If this is not done, requests will fail with 401 code. If the request is performed by a browser, cookie management should be performed automatically.

    - If password authentication has been used and the response object reports that a password change is needed, perform the [Update password workflow](#update-password-workflow)

3. Repeat step 1. to get an XSRF token.

4. Access the desired resources, set the XSRF token previously obtained as the value of the `X-XSRF-Token` HTTP header on each request. If a reuqest fails with error code 401, proceed to step 2.

#### Login example with `curl`

1. Login with username/password and collect the session cookie

```bash
curl -k -X POST \
    -H 'Content-Type: application/json' \
    https://$ADDRESS/services/session/v1/login/password \
    -d '{"password": "$KURA_PASS", "username": "$KURA_USER"}' -v
```

where:
- `$ADDRESS`: is the address of the Kura instance
- `$KURA_USER`: is the Kura username
- `$KURA_PASS`: is the Kura password

in the log you should find a `JSESSIONID` you'll use in subsequent reqeusts

```
...
< HTTP/1.1 200 OK
< Date: Tue, 14 Nov 2023 08:17:26 GMT
< Set-Cookie: JSESSIONID=myawesomecookie; Path=/; Secure; HttpOnly
< Expires: Thu, 01 Jan 1970 00:00:00 GMT
< Content-Type: application/json
< Content-Length: 30
<
* Connection #0 to host 192.168.1.111 left intact
{"passwordChangeNeeded":false}%
```

2. Retrieve the XSRF token

```bash
curl -k -X GET \
    -b "JSESSIONID=myawesomecookie" \
    https://$ADDRESS/services/session/v1/xsrfToken
```

in the response you'll find you token

```
{"xsrfToken":"myawesometoken"}%
```

3. Access the resource

```bash
curl -k -X GET \
    -H 'X-XSRF-Token: myawesometoken' \
    -b "JSESSIONID=myawesomecookie" \
    https://$ADDRESS/services/deploy/v2/
```

### Update password workflow

1. Get an XSRF token using the [GET/xsrfToken](#getxsrftoken) endpoint.

2. Call the [POST/changePassword](#postchangepassword), providing both the new and old password, make sure to include the `X-XSRF-Token` HTTP header.

3. Repeat the [Authentication and resource access workflow](#authentication-and-resource-access-workflow), starting from step 1.

Sessions will expire after an inactivity interval that can be configured using the **Session Inactivity Interval (Seconds)** RestService configuration parameter. After session expiration, a new login will be necessary.

In order to add protection against XSRF attacks, it is necessary to provide an token using the `X-XSRF-Token` HTTP header in all requests. The token can be obtained using the [GET/xsrfToken](#getxsrftoken) endpoint after a successful login.

Session will be invalidated if the current identity credentials are changed, in this case a new login will be necessary.

If a password change is required for the current identity, it will be necessary to perform the [Update password workflow](#update-password-workflow) before being able to access the other resources.

## Reference

  * [Request definitions](#request-definitions)
    * [POST/login/password](#postloginpassword)
    * [POST/login/certificate](#postlogincertificate)
    * [GET/xsrfToken](#getxsrftoken)
    * [POST/logout](#postlogout)
    * [POST/changePassword](#postchangepassword)
    * [GET/currentIdentity](#getcurrentidentity)
    * [GET/authenticationMethods](#getauthenticationmethods)
  * [JSON definitions](#json-definitions)
    * [AuthenticationResponse](#authenticationresponse)
    * [UsernamePassword](#usernamepassword)
    * [XSRFToken](#xsrftoken)
    * [PasswordChangeRequest](#passwordchangerequest)
    * [IdentityInfo](#identityinfo)
    * [AuthenticationInfo](#authenticationinfo)
    * [GenericFailureReport](#genericfailurereport)

## Request definitions
### POST/login/password
  * **REST API path** : /services/session/v1/login/password
  * **description** : Creates a new session by providing identity name and password. If the response reports that a password change is needed, it is necessary to update the current password in order to be able to access other REST APIs.
  * **request body** :
      * [UsernamePassword](#usernamepassword)
  * **responses** :
      * **200**
          * **description** : Request succeeded.
          * **response body** :
              * [AuthenticationResponse](#authenticationresponse)
      * **401**
          * **description** : The provided credentials are not correct.
      * **500**
          * **description** : An unexpected error occurred.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### POST/login/certificate
  * **REST API path** : /services/session/v1/login/certificate
  * **description** : Creates a new session using certificate based authentication. The response will report if the current identity needs a password change for informational purposes only. 
        If authentication is performed using this endpoint, access to other REST APIs will be allowd even if password change is required for the current identity.
  * **responses** :
      * **200**
          * **description** : Request succeeded.
          * **response body** :
              * [AuthenticationResponse](#authenticationresponse)
      * **401**
          * **description** : The provided credentials are not correct.
      * **500**
          * **description** : An unexpected error occurred.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### GET/xsrfToken
  * **REST API path** : /services/session/v1/xsrfToken
  * **description** : Gets the XSRF token associated with the current session. It is not necessary to call this method again until the current session expires, the obtained token is valid as long as the current session is valid.
  * **responses** :
      * **200**
          * **description** : Request succeeded, the XSRF token is returned in response body.
          * **response body** :
              * [XSRFToken](#xsrftoken)
      * **401**
          * **description** : The current session is not valid.
      * **500**
          * **description** : An unexpected error occurred.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### POST/logout
  * **REST API path** : /services/session/v1/logout
  * **description** : Terminates the current session.
  * **responses** :
      * **204**
          * **description** : Request succeeded, the session is no longer valid.
      * **401**
          * **description** : The current session is not valid
      * **500**
          * **description** : An unexpected error occurred.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### POST/changePassword
  * **REST API path** : /services/session/v1/changePassword
  * **description** : Changes the password associated with the current identity. The new password will be validated against the currently configured password strength requirements. 
        The current password strenght requirements can be retrieved using the `identity/v1/passwordRequirements` endpoint.
  * **request body** :
      * [PasswordChangeRequest](#passwordchangerequest)
  * **responses** :
      * **204**
          * **description** : Request succeeded. The current password has been changed. The session is no longer valid, a new login is required.
      * **400**
          * **description** : The new password is not valid. This can be due to the fact that it does not fullfill the current password strenght requirements.
      * **401**
          * **description** : The current session is not valid.
      * **500**
          * **description** : An unexpected error occurred.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### GET/currentIdentity
  * **REST API path** : /services/session/v1/currentIdentity
  * **description** : Provides information about the currently authenticated identity.
  * **responses** :
      * **200**
          * **description** : Request succeeded
          * **response body** :
              * [IdentityInfo](#identityinfo)
      * **401**
          * **description** : The current session is not valid.
      * **500**
          * **description** : An unexpected error occurred.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### GET/authenticationMethods
  * **REST API path** : /services/session/v1/authenticationInfo
  * **description** : Provides information about the available authentication methods.
  * **responses** :
      * **200**
          * **description** : Request succeeded
          * **response body** :
              * [AuthenticationInfo](#authenticationinfo)
      * **401**
          * **description** : The current session is not valid.
      * **500**
          * **description** : An unexpected error occurred.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

## JSON definitions
### AuthenticationResponse
Represents the response for a successful authentication request.

<br>**Properties**:

  * **passwordChangeNeeded**: `bool` 
      Determines whether a password change is required for the current identity.

  

```json
{
  "passwordChangeNeeded": true
}
```
### UsernamePassword
Contains an username and password.

<br>**Properties**:

  * **username**: `string` 
      The user name.

  
  * **password**: `string` 
      The user password.

  

```json
{
  "password": "bar",
  "username": "foo"
}
```
### XSRFToken
An object containing an XSRF token.

<br>**Properties**:

  * **xsrfToken**: `string` 
      The XSRF token.

  

```json
{
  "xsrfToken": "d2b68613-152f-41d5-8b5b-a19448ed0e4e"
}
```
### PasswordChangeRequest
An object containing the current password and a new password.

<br>**Properties**:

  * **currentPassword**: `string` 
      The current password.

  
  * **newPassword**: `string` 
      The new password.

  

```json
{
  "currentPassword": "foo",
  "newPassword": "bar"
}
```
### IdentityInfo
An object containing information about the current identity

<br>**Properties**:

  * **name**: `string` 
      The name of the current identity.

  
  * **passwordChangeNeeded**: `bool` 
      Determines whether a password change is required for the current identity.

  
  * **permissions**: `array` 
      The list of permissions assigned to the current identity.

      * array element type: `string`
          The permission name.

  

```json
{
  "name": "foo",
  "passwordChangeNeeded": false,
  "permissions": [
    "rest.bar",
    "rest.assets",
    "rest.foo"
  ]
}
```
### AuthenticationInfo
An object containing information about the enabled authentication methods.

<br>**Properties**:

  * **passwordAuthenticationEnabled**: `bool` 
      Reports whether authentication using the `login/password` endpoint is enabled.

  
  * **certificateAuthenticationEnabled**: `bool` 
      Reports whether authentication using the `login/certificate` endpoint is enabled.

  
  * **certificateAuthenticationPorts**: `array` (**optional**)
      The list of ports available for certificate based authentication. This property will be present only if `certificateAuthenticationEnabled` is true

      * array element type: `string`
          A port that can be used for certificate based authentication.

  
  * **message**: `string` (**optional**)
      Reports the content of the Login Banner, if configured on the device. A browser based application should display this message to the user before login if this property is set.
          This property will be missing if the login banner is not enabled.

  

```json
{
  "certificateAuthenticationEnabled": false,
  "passwordAuthenticationEnabled": true
}
```
```json
{
  "certificateAuthenticationEnabled": true,
  "certificateAuthenticationPorts": [
    4443,
    4444
  ],
  "passwordAuthenticationEnabled": true
}
```
```json
{
  "certificateAuthenticationEnabled": false,
  "message": "login banner content",
  "passwordAuthenticationEnabled": true
}
```
### GenericFailureReport
An object reporting a failure message.

<br>**Properties**:

  * **message**: `string` 
      A message describing the failure.
