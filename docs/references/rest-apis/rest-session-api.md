# Session V1 Rest APIs

The session management REST APIs allow to authenticate and establish an HTTP session.
Using this APIs and creating a session is the recommended way for interact with Kura rest APIs from a browser based application.

The supported workflows are the following:

### Login and resource access workflow

1. Try calling the [GET/xsrfToken](#getxsrftoken) to get an XSRF token, if the request succeeds a vaild session is already available, it is possible to proceed to step 4.

2. Call the [POST/login/password](#postloginpassword) or [POST/login/certificate](#postlogincertificate) providing the credentials to create a new session.
  
  The device will return a session cookie with the response, make sure to provide it in successive requests. If the request is performed by a browser, this should be done automatically.

  If password authentication has been used and the response object reports that a password change is needed, perform the [Update password workflow](#update-password-workflow)

3. Repeat step 1. to get an XSRF token.

4. Access the desired resources, set the XSRF token previously obtained as the value of the `X-XSRF-Token` HTTP header on each request. If a reuqest fails with error code 401, proceed to step 2.

### Update password workflow

1. Get an XSRF token using the [GET/xsrfToken](#getxsrftoken) endpoint.

2. Call the [POST/changePassword](#postchangepassword), providing both the new and old password, make sure to include the `X-XSRF-Token` HTTP header.

3. Repeat the [Authentication and resource access workflow](#authentication-and-resource-access-workflow), starting from step 1.

* Sessions will expire after an inactivity interval that can be configured using the **Session Inactivity Interval (Seconds)** RestService configuration parameter. After session expiration, a new login will be necessary.

* In order to add protection against XSRF attacks, it is necessary to provide an token using the `X-XSRF-Token` HTTP header in all requests. The token can be obtained using the [GET/xsrfToken](#getxsrftoken) endpoint after a successful login.

* Session will be invalidated if the current identity credentials are changed, in this case a new login will be necessary.

* If a password change is required for the current identity, it will be necessary to perform the [Update password workflow](#update-password-workflow) before being able to access the other resources.

## Reference

  * [Request definitions](#request-definitions)
    * [POST/login/password](#postloginpassword)
    * [POST/login/certificate](#postlogincertificate)
    * [GET/xsrfToken](#getxsrftoken)
    * [POST/logout](#postlogout)
    * [POST/changePassword](#postchangepassword)
  * [JSON definitions](#json-definitions)
    * [PasswordAuthenticationResponse](#passwordauthenticationresponse)
    * [UsernamePassword](#usernamepassword)
    * [XSRFToken](#xsrftoken)
    * [PasswordChangeRequest](#passwordchangerequest)
    * [GenericFailureReport](#genericfailurereport)

## Request definitions
### POST/login/password
  * **REST API path** : /services/session/v1/login/password
  * **description** : Creates a new session by providing identity name and password.
  * **request body** :
      * [UsernamePassword](#usernamepassword)
  * **responses** :
      * **200**
          * **description** : Request succeeded.
          * **response body** :
              * [PasswordAuthenticationResponse](#passwordauthenticationresponse)
      * **401**
          * **description** : The provided credentials are not correct
      * **500**
          * **description** : An unexpected error occurred.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### POST/login/certificate
  * **REST API path** : /services/session/v1/login/certificate
  * **description** : Creates a new session using certificate based authentication. The user must call this endpoint on an HTTPS connection opened by presenting a client certificate which is trusted by the framework and whose common name matches the name of a Kura identity.
  * **responses** :
      * **204**
          * **description** : The status of the network interfaces in the system.
      * **401**
          * **description** : The provided credentials are not correct
      * **500**
          * **description** : An unexpected error occurred.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### GET/xsrfToken
  * **REST API path** : /services/session/v1/xsrfToken
  * **description** : Gets the XSRF token associated with the current session.
  * **responses** :
      * **200**
          * **description** : Request succeeded, the XSRF token is returned in response body.
          * **response body** :
              * [XSRFToken](#xsrftoken)
      * **401**
          * **description** : The current session is not valid
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
  * **description** : Changes the password associated with the current identity.
  * **request body** :
      * [PasswordChangeRequest](#passwordchangerequest)
  * **responses** :
      * **204**
          * **description** : Request succeeded. The current password has been changed. The session is no longer valid, a new login is required.
      * **400**
          * **description** : The new password is not valid.
      * **401**
          * **description** : The current session is not valid.
      * **500**
          * **description** : An unexpected error occurred.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

## JSON definitions
### PasswordAuthenticationResponse
Represents the response for a successful password authentication request.

<br>**Properties**:

  * **passwordChangeNeeded**: `bool` 
      Determines whether the user needs to change its current password before bein able to access REST APIs.

  

```json
{
  "passwordChangeNeeded": true
}
```
### UsernamePassword
Contains an username and password

<br>**Properties**:

  * **username**: `string` 
      The user name.

  
  * **password**: `string` 
      The user password

  

```json
{
  "password": "bar",
  "username": "foo"
}
```
### XSRFToken
An object containing an XSRF token

<br>**Properties**:

  * **xsrfToken**: `string` 
      The XSRF token.

  

```json
{
  "xsrfToken": "d2b68613-152f-41d5-8b5b-a19448ed0e4e"
}
```
### GenericFailureReport
An object reporting a failure message.

<br>**Properties**:

  * **message**: `string` 
      A message describing the failure.