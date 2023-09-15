# REST Service

Kura provides a built-in REST Service based on the [osgi-jax-rs-connector](https://github.com/hstaudacher/osgi-jax-rs-connector) project.

By default, REST service providers register their services using the context path `/services`. The REST service provides the **BASIC** Authentication support and HTTPS client certificate authentication support.

Starting from Kura 5.4.0, REST service provides built in session management support, the [Session REST APIs](doc:rest-session-api) can be used to establish a session. Using sessions is the recommended way for interacting with Kura REST APIs from a browser based application, for this use case it is also possible to disable the default session-less BASIC and certificate based authentication (see the **Basic Authentication Enabled** and **Enable Certificate Authentication Whitout Session Management** configuration parameters below).

REST API access is available on all HTTP ports defined in the [HTTP/HTTPS Configuration](doc:httphttps-configuration) section, unless access is restricted to dedicated ports using the corresponding configuration parameter (see below).

Certificate authentication support is only available on the **HTTPS With Certificate Authentication Ports** configured in [HTTP/HTTPS Configuration](doc:httphttps-configuration) section.

Kura Identity names and passwords can be used for **BASIC** Authentication. Certificate authentication follows the same rules as [Gateway Administration Console Authentication](../gateway-configuration/gateway-administration-console-authentication.md).

!!! warning
    If the [forced password change](doc:security-configuration#forced-password-change-on-login) feature for a given identity is enabled, REST API password authentication will be blocked for that identity until the password is updated by the user or the feature is manually disabled. Certificate authentication will continue to be allowed even if the forced password change feature is enabled.

JAX-RS roles are mapped to Kura permissions, the name of a permission associated with a JAX-RS role is the _rest._ prefix followed by the role name. For example the _assets_ role is mapped to the _rest.assets_ permission. REST related permissions can be assigned to an identity using the Gateway Administration Console in the **Identities** section.



## Rest Service configuration

The available configuration parameters are the following:

* **Allowed Ports**: If set to a non empty list, REST API access will be allowed only on the specified ports. If set to an empty list, access will be allowed on all ports. Please make sure that the allowed ports are open in HttpService and Firewall configuration. (Default: empty)

* **Password Authentication Enabled**: Enables or disables the built-in password authentication support. (Default: true)

* **Certificate Authentication Enabled**: Enables or disables the built-in certificate authentication support. (Default: true)

* **Session Based Authentication Enabled**: If set to true, enables authentication using the dedicated `/services/session/v1` endpoints and cookie based session management. (Default: true)

* **Session Inactivity Interval (Seconds)**: The session inactivity interval, sessions will expire if no request is performed for the amount of time specified by this parameter in seconds. This parameter is ignored if Session Based Authentication Enabled is set to false. (Default: 900)

* **Basic Authentication Enabled**: Allows to perform authentication by providing identity name and password as BASIC credentials in the request to any resource endpoint. Requires that the **Password Authentication Enabled parameter** is set to true. (Default: true)

* **Enable Certificate Authentication Without Session Management**: If set to true, calling `/services/session/v1/certificate` to create a session will not be necessary in order to perform certificate based authentication. Presenting a valid HTTPS client certificate and accessing resource endpoint directly is enough for authentication to succeed. Requires that the **Certificate Authentication Enabled** parameter is set to true. (Default: true)

## Custom authentication methods

Starting from Kura 5.2.0 it is also possible to develop custom REST authentication method providers by registering an implementation of the `org.eclipse.kura.rest.auth.AuthenticationProvider` interface as an OSGi service. The `org.eclipse.kura.example.rest.authentication.provider` bundle in Kura repository provides an example on how to implement a custom authentication method.



## Assets REST APIs

Kura exposes REST APIs for the Asset instances instantiated in the framework. Assets REST APIs are available in the context path ```/services/assets```. Following, the supported REST endpoints.

| Method | Path | Allowed roles | Encoding | Request parameters | Description |
| ------ | ---- | ------------- | -------- | ------------------ | ----------- |
| GET | `/` | assets | JSON | None | Returns the list of available assets |
| GET | `/{pid}` | assets | JSON | None | Returns the list of available channels for the selected asset (specified by the corresponding PID) |
| GET | `/{pid}/_read` | assets | JSON | None | Returns the read for all the READ channels in the selected Asset |
| POST | `/{pid}/_read` | assets | JSON | The list of channels where the READ operation should be performed. | Returns the result of the read operation for the specified channels |
| POST | `/{pid}/_write` | assets | JSON | The list of channels and associated values that will be used for the WRITE operation. | Performs the write operation for the specified channels returning the result of the operation. |