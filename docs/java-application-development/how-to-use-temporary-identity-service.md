# How to Use Temporary Identities

Temporary identities are short-lived, non-persistent identities created in memory. They are intended for scenarios such as containerized applications that need limited access to Kura REST APIs without storing long-term credentials.

## Overview

Temporary identities are managed through the `IdentityService` using an `IdentityConfiguration` and a lifetime:

- A temporary identity is created with an `IdentityConfiguration` and assigned permissions.
- The identity exists only in memory and is removed explicitly or when the lifetime expires.
- REST access can use password-based or certificate-based authentication, depending on RestService configuration.

## API Reference

### Create Temporary Identity

```java
void createTemporaryIdentity(IdentityConfiguration configuration, Duration lifetime)
```

Creates a temporary identity with the given configuration and lifetime.

### Delete Identity

```java
boolean deleteIdentity(String identityName)
```

Deletes a temporary identity by name (also works for regular identities).

## Usage Example

```java
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.identity.AssignedPermissions;
import org.eclipse.kura.identity.IdentityConfiguration;
import org.eclipse.kura.identity.IdentityService;
import org.eclipse.kura.identity.Permission;
import org.eclipse.kura.identity.PasswordConfiguration;

public class TemporaryIdentityExample {

    private IdentityService identityService;

    public void createTemporaryIdentity() throws Exception {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(new Permission("rest.system"));
        permissions.add(new Permission("rest.configuration"));

        String identityName = "container_myapp";
        char[] password = "temporary-password".toCharArray();

        PasswordConfiguration passwordConfiguration = new PasswordConfiguration(
            false,
            true,
            Optional.of(password),
            Optional.empty()
        );

        AssignedPermissions assignedPermissions = new AssignedPermissions(permissions);
        IdentityConfiguration configuration = new IdentityConfiguration(
            identityName,
            Arrays.asList(passwordConfiguration, assignedPermissions)
        );

        identityService.createTemporaryIdentity(configuration, Duration.ofHours(1));

        // Use identityName and password to access REST APIs via Basic auth
    }

    public void cleanupTemporaryIdentity() throws Exception {
        identityService.deleteIdentity("container_myapp");
    }
}
```

## REST Authentication Example

```bash
curl -k -u "${KURA_IDENTITY_NAME}:${KURA_IDENTITY_PASSWORD}" \
  "${KURA_REST_BASE_URL}/system/info"
```

Basic authentication must be enabled in the **RestService** configuration for password-based access. Certificate-based access requires certificate authentication to be enabled and a client certificate whose CN matches the temporary identity name.

## Best Practices

1. **Least Privilege**: Grant only the permissions required by the application.
2. **Short Lifetimes**: Use the minimum lifetime needed for the use case.
3. **Cleanup**: Delete temporary identities when they are no longer needed.
4. **Secrets Handling**: Do not log or persist temporary passwords.
