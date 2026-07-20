# Container Identity Credentials Migration Guide

This guide explains how containerized applications must read their Kura REST API credentials when [Container Identity Integration](./container-orchestration-provider-usage.md#container-identity-integration) is enabled, and how to migrate from the previous environment-variable mechanism.

## Why this changed

Previously, the temporary identity password was passed to the container in the `KURA_IDENTITY_PASSWORD` environment variable. Environment variables are exposed through `docker inspect` and `/proc/<pid>/environ`, so any process able to inspect the container (or the host) could read the password.

The password is now delivered through a file mounted **read-only** from an in-memory filesystem (tmpfs):

- On the host the file is written to `<base>/kura-tokens/<uuid>/kura-token` (with `<base>` defaulting to `/dev/shm`, configurable via the `kura.tmpfs.base` system property), with owner-read-only permissions (`400`).
- Inside the container it is mounted read-only at `/run/secrets/kura-token`, and its path is exported in the `KURA_TOKEN_FILE` environment variable.
- Because the password is never placed in an environment variable, it no longer appears in `docker inspect` or `/proc/<pid>/environ`. The file lives in RAM and is cleared on reboot.

## What changed

### Environment variables

| Variable | Before | After |
| -------- | ------ | ----- |
| `KURA_IDENTITY_NAME` | Provided (identity name) | Provided (unchanged) |
| `KURA_REST_BASE_URL` | Provided (REST base URL) | Provided (unchanged) |
| `KURA_IDENTITY_PASSWORD` | Provided (password value) | **Removed** |
| `KURA_TOKEN_FILE` | — | **Added** — path of the read-only file containing the password (always `/run/secrets/kura-token`) |

### Mechanism

| Aspect | Before | After |
| ------ | ------ | ----- |
| Where the password lives | `KURA_IDENTITY_PASSWORD` environment variable | Read-only file at `/run/secrets/kura-token` (tmpfs) |
| Visible in `docker inspect` / `/proc/<pid>/environ` | Yes | No |
| How the container reads it | `os.environ` / `getenv` | Read the file referenced by `KURA_TOKEN_FILE` |
| Cleanup on container stop | Identity invalidated | Identity invalidated **and** token file deleted |

!!! warning
    This is a breaking change. `KURA_IDENTITY_PASSWORD` is no longer provided to containers. Applications that read the password from that environment variable must be updated to read it from the file referenced by `KURA_TOKEN_FILE`, otherwise authentication will fail.

## Before / After

The only change required is where the password comes from — the username, base URL, and the authenticated request stay the same.

**Before (environment variable):**
```java
String identityName = System.getenv("KURA_IDENTITY_NAME");
String identityPassword = System.getenv("KURA_IDENTITY_PASSWORD");   // no longer provided
String baseUrl = System.getenv("KURA_REST_BASE_URL");
```

**After (read-only token file):**
```java
String identityName = System.getenv("KURA_IDENTITY_NAME");
String identityPassword = new String(
        Files.readAllBytes(Path.of(System.getenv("KURA_TOKEN_FILE"))));   // /run/secrets/kura-token
String baseUrl = System.getenv("KURA_REST_BASE_URL");
```

## Reading the token (new approach)

Each example reads the identity name and REST base URL from the environment, reads the password from the file referenced by `KURA_TOKEN_FILE`, and performs a Basic-authenticated request to Kura's REST APIs.

!!! note
    When Kura's REST endpoint uses HTTPS with a self-signed certificate, the client must either trust that certificate or disable TLS verification (the examples below disable verification for brevity, as in the rest of this documentation). Prefer trusting the certificate in production.

**Python:**
```python
import os
import requests

identity_name = os.environ['KURA_IDENTITY_NAME']
base_url = os.environ['KURA_REST_BASE_URL']

with open(os.environ['KURA_TOKEN_FILE']) as token_file:
    identity_password = token_file.read()

response = requests.get(
    f'{base_url}/system/info',
    auth=(identity_name, identity_password),
    verify=False
)
response.raise_for_status()
print(response.json())
```

**Java:**
```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class KuraRestExample {

    public static void main(String[] args) throws Exception {
        String identityName = System.getenv("KURA_IDENTITY_NAME");
        String baseUrl = System.getenv("KURA_REST_BASE_URL");
        String password = new String(Files.readAllBytes(Path.of(System.getenv("KURA_TOKEN_FILE"))));

        String basicAuth = Base64.getEncoder()
                .encodeToString((identityName + ":" + password).getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/system/info"))
                .header("Authorization", "Basic " + basicAuth)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.statusCode() + ": " + response.body());
    }
}
```

**Shell (curl):**
```bash
#!/bin/sh
# KURA_IDENTITY_NAME, KURA_TOKEN_FILE and KURA_REST_BASE_URL are provided by Kura
curl -k -u "${KURA_IDENTITY_NAME}:$(cat "${KURA_TOKEN_FILE}")" \
  "${KURA_REST_BASE_URL}/system/info"
```

**Node.js:**
```javascript
const fs = require('fs');

const identityName = process.env.KURA_IDENTITY_NAME;
const baseUrl = process.env.KURA_REST_BASE_URL;
const password = fs.readFileSync(process.env.KURA_TOKEN_FILE, 'utf8');

const auth = Buffer.from(`${identityName}:${password}`).toString('base64');

// Allow self-signed certificates (development only)
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

fetch(`${baseUrl}/system/info`, {
  headers: { Authorization: `Basic ${auth}` }
})
  .then((response) => response.json())
  .then((info) => console.log(info))
  .catch((error) => console.error(error));
```

## Backward compatibility

If you need a single container image that works with both old and new Kura releases, read the password from `KURA_TOKEN_FILE` when it is set and fall back to `KURA_IDENTITY_PASSWORD` otherwise:

```java
String tokenFile = System.getenv("KURA_TOKEN_FILE");
String identityPassword;
if (tokenFile != null && !tokenFile.isEmpty()) {
    identityPassword = new String(Files.readAllBytes(Path.of(tokenFile)));
} else {
    identityPassword = System.getenv("KURA_IDENTITY_PASSWORD");
}
```

## Notes

- The token file is **read-only**: the container cannot modify or overwrite it.
- Each container gets its own token file in a unique per-container directory.
- The password (and its file) are invalidated and removed when the container stops, is deleted, or its configuration changes.
- The file contains only the password, with no trailing newline.
