# How to Use the Temporary Identity Service

The Temporary Identity Service provides APIs for creating and managing temporary, non-persistent identities with specific permissions. This service is primarily used by the Container Orchestration Provider to provision authentication credentials for containers, but can also be leveraged in custom OSGi bundles.

## Overview

The `TemporaryIdentityService` allows you to:

- Create temporary identities with specific permissions
- Generate authentication tokens for temporary identities
- Validate temporary tokens
- Check permissions for temporary identities
- Delete temporary identities when no longer needed

Temporary identities are stored only in memory and are not persisted to disk, making them ideal for short-lived authentication scenarios such as containerized applications.

## API Reference

The `TemporaryIdentityService` interface is located in the `org.eclipse.kura.identity` package and provides the following methods:

### Create Temporary Identity

```java
public String createTemporaryIdentity(
    final String identityName,
    final Set<Permission> permissions
) throws KuraException
```

Creates a temporary identity with the given name and permissions. Returns an authentication token that can be used to authenticate as this identity.

**Parameters:**
- `identityName`: The name of the temporary identity (e.g., `container_myapp`)
- `permissions`: A set of `Permission` objects representing the permissions to grant

**Returns:** An authentication token (String) that can be used to authenticate REST API requests

**Throws:** `KuraException` if the identity creation fails

### Delete Temporary Identity

```java
public boolean deleteTemporaryIdentity(final String token) throws KuraException
```

Deletes a temporary identity identified by its authentication token.

**Parameters:**
- `token`: The authentication token of the temporary identity to delete

**Returns:** `true` if the identity was deleted, `false` if the identity doesn't exist

**Throws:** `KuraException` if the deletion fails

### Validate Temporary Token

```java
public String validateTemporaryToken(final String token) throws KuraException
```

Validates a temporary identity authentication token and returns the identity name.

**Parameters:**
- `token`: The authentication token to validate

**Returns:** The identity name if the token is valid

**Throws:** `KuraException` if the token is invalid or validation fails

### Check Temporary Permission

```java
public void checkTemporaryPermission(
    final String token,
    final Permission permission
) throws KuraException
```

Checks if the specified permission is assigned to the temporary identity.

**Parameters:**
- `token`: The authentication token of the temporary identity
- `permission`: The permission to check

**Throws:** `KuraException` if the permission is not assigned or the check fails

## Usage Examples

### Example 1: Basic Usage in an OSGi Bundle

```java
package com.example.kura.app;

import java.util.Set;
import java.util.HashSet;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.identity.Permission;
import org.eclipse.kura.identity.TemporaryIdentityService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyApplication {

    private static final Logger logger = LoggerFactory.getLogger(MyApplication.class);

    private TemporaryIdentityService temporaryIdentityService;
    private String currentToken;

    public void setTemporaryIdentityService(TemporaryIdentityService service) {
        this.temporaryIdentityService = service;
    }

    public void unsetTemporaryIdentityService(TemporaryIdentityService service) {
        this.temporaryIdentityService = null;
    }

    protected void activate(ComponentContext context) {
        logger.info("Activating MyApplication...");

        try {
            // Create a temporary identity with specific permissions
            Set<Permission> permissions = new HashSet<>();
            permissions.add(new Permission("rest.system"));
            permissions.add(new Permission("rest.configuration"));

            this.currentToken = this.temporaryIdentityService.createTemporaryIdentity(
                "myapp_temp_identity",
                permissions
            );

            logger.info("Created temporary identity with token");

            // Validate the token
            String identityName = this.temporaryIdentityService.validateTemporaryToken(
                this.currentToken
            );
            logger.info("Token validated for identity: {}", identityName);

            // Check if a specific permission is granted
            this.temporaryIdentityService.checkTemporaryPermission(
                this.currentToken,
                new Permission("rest.system")
            );
            logger.info("Permission check passed");

        } catch (KuraException e) {
            logger.error("Failed to create temporary identity", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        logger.info("Deactivating MyApplication...");

        // Clean up temporary identity
        if (this.currentToken != null && this.temporaryIdentityService != null) {
            try {
                boolean deleted = this.temporaryIdentityService.deleteTemporaryIdentity(
                    this.currentToken
                );
                if (deleted) {
                    logger.info("Successfully deleted temporary identity");
                } else {
                    logger.warn("Temporary identity was already deleted");
                }
            } catch (KuraException e) {
                logger.error("Failed to delete temporary identity", e);
            }
            this.currentToken = null;
        }
    }
}
```

### Example 2: OSGi Declarative Services Configuration

Create a component definition file (`OSGI-INF/MyApplication.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.1.0"
    name="com.example.kura.app.MyApplication"
    activate="activate"
    deactivate="deactivate">

    <implementation class="com.example.kura.app.MyApplication"/>

    <reference
        bind="setTemporaryIdentityService"
        unbind="unsetTemporaryIdentityService"
        cardinality="1..1"
        name="TemporaryIdentityService"
        interface="org.eclipse.kura.identity.TemporaryIdentityService"
        policy="static"/>
</scr:component>
```

### Example 3: Dynamic Permission Management

```java
public class DynamicPermissionManager {

    private TemporaryIdentityService temporaryIdentityService;
    private final Map<String, String> activeTokens = new ConcurrentHashMap<>();

    /**
     * Creates a temporary identity with read-only permissions
     */
    public String createReadOnlyIdentity(String name) throws KuraException {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(new Permission("rest.system"));
        permissions.add(new Permission("rest.network.status"));
        permissions.add(new Permission("rest.inventory"));

        String token = this.temporaryIdentityService.createTemporaryIdentity(
            "readonly_" + name,
            permissions
        );

        this.activeTokens.put(name, token);
        return token;
    }

    /**
     * Creates a temporary identity with administrative permissions
     */
    public String createAdminIdentity(String name) throws KuraException {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(new Permission("rest.configuration"));
        permissions.add(new Permission("rest.deploy"));
        permissions.add(new Permission("rest.identity"));

        String token = this.temporaryIdentityService.createTemporaryIdentity(
            "admin_" + name,
            permissions
        );

        this.activeTokens.put(name, token);
        return token;
    }

    /**
     * Validates and checks if a token has a specific permission
     */
    public boolean hasPermission(String token, String permissionName) {
        try {
            // First validate the token
            this.temporaryIdentityService.validateTemporaryToken(token);

            // Then check the specific permission
            this.temporaryIdentityService.checkTemporaryPermission(
                token,
                new Permission(permissionName)
            );
            return true;
        } catch (KuraException e) {
            return false;
        }
    }

    /**
     * Cleanup all active tokens
     */
    public void cleanupAll() {
        for (Map.Entry<String, String> entry : this.activeTokens.entrySet()) {
            try {
                this.temporaryIdentityService.deleteTemporaryIdentity(entry.getValue());
                logger.info("Cleaned up temporary identity: {}", entry.getKey());
            } catch (KuraException e) {
                logger.error("Failed to cleanup identity: {}", entry.getKey(), e);
            }
        }
        this.activeTokens.clear();
    }
}
```

### Example 4: Integration with REST Client

```java
public class KuraRestClient {

    private final String baseUrl;
    private final String token;

    public KuraRestClient(String baseUrl, String token) {
        this.baseUrl = baseUrl;
        this.token = token;
    }

    /**
     * Make an authenticated GET request to Kura REST API
     */
    public String get(String endpoint) throws IOException {
        URL url = new URL(this.baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + this.token);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            } else {
                throw new IOException("HTTP error code: " + responseCode);
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Factory method to create a client with a temporary identity
     */
    public static KuraRestClient createWithTemporaryIdentity(
            TemporaryIdentityService service,
            String baseUrl,
            String identityName,
            Set<Permission> permissions) throws KuraException {

        String token = service.createTemporaryIdentity(identityName, permissions);
        return new KuraRestClient(baseUrl, token);
    }
}

// Usage example
public void useRestClient() {
    try {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(new Permission("rest.system"));

        KuraRestClient client = KuraRestClient.createWithTemporaryIdentity(
            this.temporaryIdentityService,
            "http://localhost:8080/services",
            "rest_client_temp",
            permissions
        );

        String systemInfo = client.get("/system/info");
        logger.info("System info: {}", systemInfo);

    } catch (Exception e) {
        logger.error("Failed to use REST client", e);
    }
}
```

## Best Practices

### 1. Always Clean Up Temporary Identities

Temporary identities should be explicitly deleted when no longer needed to prevent token accumulation in memory:

```java
@Override
protected void deactivate(ComponentContext context) {
    if (this.token != null) {
        try {
            this.temporaryIdentityService.deleteTemporaryIdentity(this.token);
        } catch (KuraException e) {
            logger.error("Failed to cleanup temporary identity", e);
        }
    }
}
```

### 2. Use Descriptive Identity Names

Use meaningful names that indicate the purpose and owner of the temporary identity:

```java
// Good
String token = service.createTemporaryIdentity(
    "container_monitoring_app",
    permissions
);

// Less clear
String token = service.createTemporaryIdentity(
    "temp_id_1",
    permissions
);
```

### 3. Apply Principle of Least Privilege

Only grant the minimum permissions required for the task:

```java
// Good - only required permissions
Set<Permission> permissions = new HashSet<>();
permissions.add(new Permission("rest.system"));  // Only what's needed

// Bad - excessive permissions
Set<Permission> permissions = new HashSet<>();
permissions.add(new Permission("kura.admin"));  // Full admin access
```

### 4. Handle Exceptions Appropriately

Always handle `KuraException` when using the service:

```java
try {
    String token = this.temporaryIdentityService.createTemporaryIdentity(
        identityName,
        permissions
    );
    // Use the token
} catch (KuraException e) {
    logger.error("Failed to create temporary identity", e);
    // Implement appropriate fallback logic
}
```

### 5. Validate Tokens Before Use

Validate tokens before making critical operations:

```java
public void performOperation(String token) {
    try {
        // Validate token first
        String identityName = this.temporaryIdentityService.validateTemporaryToken(token);
        logger.info("Performing operation for identity: {}", identityName);

        // Proceed with operation
        // ...

    } catch (KuraException e) {
        logger.error("Invalid or expired token", e);
        throw new IllegalStateException("Authentication failed", e);
    }
}
```

## Common Use Cases

### Container Authentication

The primary use case is providing authentication credentials to containerized applications:

```java
// In container orchestration code
String token = temporaryIdentityService.createTemporaryIdentity(
    "container_" + containerName.replace("-", "_"),
    containerPermissions
);

// Pass token to container via environment variable
envVars.add("KURA_IDENTITY_TOKEN=" + token);
```

### Temporary Service Access

Granting temporary access to external services or applications:

```java
public String grantTemporaryAccess(String serviceName, Duration validFor)
        throws KuraException {

    Set<Permission> permissions = getServicePermissions(serviceName);
    String token = temporaryIdentityService.createTemporaryIdentity(
        "service_" + serviceName,
        permissions
    );

    // Schedule automatic cleanup
    scheduler.schedule(
        () -> cleanupToken(token),
        validFor.toMillis(),
        TimeUnit.MILLISECONDS
    );

    return token;
}
```

### Testing and Development

Creating temporary identities for integration tests:

```java
@Before
public void setUp() throws KuraException {
    Set<Permission> testPermissions = new HashSet<>();
    testPermissions.add(new Permission("rest.configuration"));

    this.testToken = temporaryIdentityService.createTemporaryIdentity(
        "test_identity",
        testPermissions
    );
}

@After
public void tearDown() throws KuraException {
    temporaryIdentityService.deleteTemporaryIdentity(this.testToken);
}
```

## Security Considerations

1. **Token Storage**: Never persist temporary tokens to disk or logs. They should only exist in memory.

2. **Token Transmission**: When passing tokens to containers or external services, use secure channels (environment variables, in-memory files, etc.).

3. **Permission Scope**: Always use the minimum required permissions. Avoid granting `kura.admin` unless absolutely necessary.

4. **Token Lifecycle**: Implement proper cleanup logic to ensure tokens are deleted when no longer needed.

5. **Validation**: Always validate tokens before performing privileged operations.

## Related Documentation

- [Container Identity Integration](../core-services/container-orchestration-provider-usage.md#container-identity-integration)
- [REST Identity API](../references/rest-apis/rest-identity-api-v2.md)
- [Authentication and Authorization](../gateway-configuration/authentication-and-authorization.md)
