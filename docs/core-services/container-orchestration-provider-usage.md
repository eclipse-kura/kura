# Container Orchestration Provider Usage



## Before Starting

For this bundle to function appropriately, the gateway must have a supported container engine installed and running. Currently, the only officially supported engine is Docker.



## Starting the Service

To use this service select the **ContainerOrchestrationService** option located in the **Services** area. The ContainerOrchestrationService provides the following parameters:

- **Enabled**--activates the service when set to true
- **Container Engine Host URL**--provides a string that tells the service where to find the container engine (best left to the default value).
- **Allowlist Enforcement Enabled**--activates the container enforcement of the service, which let only the allowed containers to run
- **Container Image Allowlist Content**--the comma-separated list of conainer's digests allowed to be run

![Container Orchestration Provider](./images/container-orchestration-provider.png)



## Creating your first container.

To create a container, select the `+` icon (Create a new component) under **services**. A popup dialogue box will appear. In the field **Factory** select **org.eclipse.kura.container.provider.ContainerInstance** from the drop-down. Then, use the **Name** field to specify a name for the container.

!!! note
    The name specified in the 'Name' field will also be the name of the container when it is spun up by the orchestrator.

After pressing submit, a new component will be added under the **services** tab, with the name that was selected in the dialogue. Select this component to finish configuring the container.



## Configuring the container

To begin configuring the container, look under **Services** and select the item which has the name set in the previous step. Containers may be configured using the following fields:

- **Enabled** - When true, the service will create the defined container. When false the API will not create the container or will destroy the container if already running.
  
- **Image Name** - Describes the image that will be used to create the container. Remember to ensure that the selected image supports the architecture of the host machine, or else the container will not be able to start.
  
- **Image Tag** - Describes the version of the container image that will be used to create the container.

- **Trust Anchor** - Trust anchor used to verify the container image [Signature Verification](./container-orchestration-image-auth.md#container-signature-verification)

- **Verify in transparency log** - Sets the transparency log verification, to be used when a container image signature has been uploaded to the transparency log.

- **Container Image Enforcement Digest** - A string representing the digest for the image that will be allowed to run by this container instance (eg: `sha256:0000000000000000000000000000000000000000000000000000000000000000`). It is used in the [Container Enforcement](./container-orchestration-image-auth.md) service provided by the Container Orchestration Service.

- **Authentication Registry URL** - URL for an alternative registry to pull images from. (If the field is left blank, credentials will be applied to Docker-Hub). Please see the [Authenticated Registries](./container-orchestration-provider-authenticated-registries.md) document for more information about connecting to different popular registries.

- **Authentication Username** - Describes the username to access the container registry entered above.

- **Password** - Describes the password to access the alternative container registry.

- **Image Download Retries** - Describes the number of retries the framework will attempt to pull the image before giving up.

- **Image Download Retry Interval** - Describes the amount of time the framework will wait before attempting to pull the image again.

- **Image Download Timeout** - Describes the amount of time the framework will let the image download before timeout.
  
- **Internal Ports** - This field accepts a comma-separated list of ports that will be internally exposed on the spun-up container. In this field, you can also specify which protocol to run at the port by appending a port with a colon and typing in the name of the network protocol. Example: `80, 443:tcp, 8080:udp`.
  
- **External Ports** - This field accepts a comma-separated list of ports that will be externally exposed on the host machine.
  
- **Privileged Mode** - This flag if enabled will give the container root capabilities to all devices on the host system. Please be aware that setting this flag can be dangerous, and must only be used in exceptional situations.
  
- **Environment Variables (optional)** - This field accepts a comma-separated list of environment variables, which will be set inside the container when spun up.
  
- **Entrypoint Override (optional)** - This field accepts a comma-separated list which is used to override the command used to start a container. Example: ```./test.sh,-v,-d,--human-readable```.

- **Memory (optional)** - This field allows the configuration of the maximum amount of memory the container can use in bytes. The value is a positive integer, optionally followed by a suffix of b, k, m, g, to indicate bytes, kilobytes, megabytes, or gigabytes. The minimum and default values depends by the native container orchestrator. If left empty, the memory assigned to the container will be set to a default value.

- **CPUs (optional)** - This value specifies how many CPUs a container can use. Decimal values are allowed, so if set to 1.5, the container will use at most one and a half cpu resource.

- **GPUs (optional)** - This field configures how many Nvidia GPUs a container can use. Allowed values are `all` or an integer number. If there's no Nvidia GPU installed, leave it empty. The Nvidia Container Toolkit must be installed on the system to correctly configure the service, otherwise the container will not start. If the Nvidia Container Runtime is used, leave the field empty.

- **Volume Mount (optional)** - This field accepts a comma-separated list of system-to-container file mounts. This allows for the container to access files on the host machine.
  
- **Peripheral Device (optional)** - This field accepts a comma-separated list of device paths. This parameter allows devices to be passed through from the host to the container.

- **Runtime (optional)**: Specifies the fully qualified name of an alternate OCI-compatible runtime, which is used to run commands specified by the 'run' instruction. Example: `nvidia` corresponds to `--runtime=nvidia`. Note:  when using the Nvidia Container Runtime, leave the **GPUs** field empty. The GPUs available on the system will be accessible from the container by default.

- **Networking Mode (optional)** - Use this field to specify what networking mode the container will use. Possible Drivers include: bridge, none, container:{container id}, host. Please note that this field is case-sensitive. This field can also be used to connect to any of the networks listed by the cli command ```docker network ls```.

- **Logger Type** - This field provides a drop-down selection of supported container logging drivers.

- **Logger Parameters (optional)** - This field accepts a comma-separated list of logging parameters. More information can be found in the container-engine logger documentation, for instance [here](https://docs.docker.com/config/containers/logging/configure/).

- **Restart Container On Failure** - A boolean that tells the container engine to automatically restart the container when it has failed or shut down.

- **Identity Integration Enabled** - When enabled, Kura automatically creates a temporary identity with the specified permissions and provides the container with authentication credentials to access Kura's REST APIs. See [Container Identity Integration](#container-identity-integration) for more details.

- **Container Permissions (optional)** - A comma-separated list of permission names to grant to the container's temporary identity (e.g., `rest.system,rest.configuration`). This field is only used when **Identity Integration Enabled** is set to true. See [Container Identity Integration](#container-identity-integration) for available permissions and usage examples.

After specifying container parameters, ensure to set **Enabled** to **true** and press **Apply**. The container engine will then pull the respective image, spin up and start the container. If the gateway or the framework is power cycled, and the container and Container Orchestration Service are set to **enabled**, the framework will automatically start the container again upon startup.

![Container Orchestration Provider Container Configuration](./images/container-orchestration-provider-container-configuration.png)

## Manage container resources
Memory and CPU settings only take effect if the host system’s kernel has the corresponding cgroup v2 features enabled.
To see which cgroup subsystems are enabled, inspect the `/proc/cgroup` file.
```shell
cat /proc/cgroups
```
You should see output similar to this:
```shell
#subsys_name    hierarchy       num_cgroups     enabled
cpuset  0       97      1
cpu     0       97      1
cpuacct 0       97      1
blkio   0       97      1
devices 0       97      1
freezer 0       97      1
net_cls 0       97      1
perf_event      0       97      1
net_prio        0       97      1
pids    0       97      1
```

**enabled**: Indicates whether the subsystem is enabled (1) or disabled (0) in the kernel.

To enable the subsystem in Raspberry Pi OS, for example, you need to add the following entries to the bootloader options:

 - `cgroup_enable=memory`
 - `cgroup_memory=1`
 - `swapaccount=1`
 - `cgroup_enable=cpuset`
 
 and reboot the system.

For example, on Raspberry Pi 5 with Debian Bookworm 12.2, you can add them to the `/boot/firmware/cmdline.txt` file, whose original content is:
 
```shell
 console=serial0,115200 console=tty1 root=PARTUUID=6312077f-02 rootfstype=ext4 fsck.repair=yes rootwait cfg80211.ieee80211_regdom=IT
```

The result should be a single line with all the existing options plus the new ones.

```shell
 console=serial0,115200 console=tty1 root=PARTUUID=6312077f-02 rootfstype=ext4 fsck.repair=yes rootwait cfg80211.ieee80211_regdom=IT cgroup_enable=memory cgroup_memory=1 swapaccount=1 cgroup_enable=cpuset`
```

!!! warning
    Modifying the bootloader options incorrectly may prevent the system from booting. Please ensure to back up any important data before making changes to these settings.

## Container Identity Integration

The Container Identity Integration feature allows containers to securely authenticate and interact with Kura's REST APIs using temporary credentials. When enabled, Kura automatically provisions a temporary identity and delivers its credentials to the container through a read-only file mounted from an in-memory filesystem (tmpfs), eliminating the need for manual credential configuration and keeping the password out of the container's environment.

### Overview

When Identity Integration is enabled for a container instance, Kura performs the following operations:

1. **Creates a Temporary Identity**: A temporary, non-persistent identity is created specifically for the container with a unique name based on the container name (e.g., `container_myapp` for a container named `myapp`).

2. **Assigns Permissions**: The temporary identity is granted the permissions specified in the **Container Permissions** field. These are the same permissions used by Kura identities (for example `rest.system` or `rest.configuration`), so each name must reference a permission that already exists in the gateway.

3. **Provides Credentials**: The container receives the following environment variables:
    - `KURA_IDENTITY_NAME`: The temporary identity name for accessing Kura's REST APIs
    - `KURA_TOKEN_FILE`: The in-container path of a read-only file containing the temporary password (always `/run/secrets/kura-token`)
    - `KURA_REST_BASE_URL`: The complete base URL for Kura's REST API endpoints (e.g., `http://172.17.0.1:8080/services` or `https://172.17.0.1:443/services`)

4. **Mounts a Secure Token File**: The temporary password is written to a file on an in-memory filesystem (tmpfs) on the host — `<base>/kura-tokens/<uuid>/kura-token`, where `<base>` defaults to `/dev/shm` — with owner-read-only permissions (`400`), and is mounted **read-only** into the container at `/run/secrets/kura-token`. Because the password is never placed in an environment variable, it does not appear in `docker inspect` or in `/proc/<pid>/environ`.

5. **Automatic Cleanup**: When the container stops or is deleted, Kura automatically removes the temporary identity, invalidates its credentials, and deletes the token file and its parent directory from tmpfs.

### Features

- **Zero Configuration**: Containers automatically receive the correct REST API URL based on the gateway's HTTPS configuration and network mode.
- **Network-Aware**: The REST base URL is automatically adjusted based on the container's networking mode (bridge, host, etc.).
- **Secure**: Credentials are temporary and automatically invalidated when containers stop. The password is delivered through a read-only tmpfs file instead of an environment variable, so it is not exposed via `docker inspect` or `/proc/<pid>/environ`.
- **Non-Persistent**: Temporary identities exist only in memory and are never persisted to disk. The token file lives on tmpfs (RAM) and is cleared on reboot.
- **Permission-Based**: Fine-grained access control using Kura's existing permission system.

### Configuration

To enable Identity Integration for a container:

1. Set **Identity Integration Enabled** to `true`
2. Specify the required permissions in **Container Permissions** field (comma-separated)
3. Apply the configuration

To use the temporary credentials with REST APIs, ensure **Basic Authentication Enabled** is set to `true` in the **RestService** configuration.

The framework will create the temporary identity when the container starts and clean it up when the container stops.

!!! note
    The token file is written to an in-memory filesystem (tmpfs). The base directory is `/dev/shm` by default and can be changed with the `kura.tmpfs.base` system property. The platform must provide a writable tmpfs at that location: if it is missing, the container fails to start with a clear error message.

### Available Permissions

For a complete list of available permissions, use the [REST Identity API](/references/rest-apis/rest-identity-api-v2/#get-defined-permissions) to query defined permissions in your system.

### Usage Example

#### Example: Container with Read-Only System Access

A monitoring container that needs to read system information but cannot modify configuration:

**Container Configuration:**
- **Identity Integration Enabled**: `true`
- **Container Permissions**: `rest.system`

**Container Code (Java):**
```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class SystemInfoExample {

    public static void main(String[] args) throws Exception {
        // Read the identity name and the REST base URL from the environment
        String identityName = System.getenv("KURA_IDENTITY_NAME");
        String baseUrl = System.getenv("KURA_REST_BASE_URL");

        // Read the password from the read-only token file
        String password = new String(Files.readAllBytes(Path.of(System.getenv("KURA_TOKEN_FILE"))));

        // Make an authenticated request to get system information
        String basicAuth = Base64.getEncoder()
                .encodeToString((identityName + ":" + password).getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/system/info"))
                .header("Authorization", "Basic " + basicAuth)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.println("System info: " + response.body());
        } else {
            System.out.println("Failed to get system info: " + response.statusCode());
        }
    }
}
```

The same flow can be implemented in other languages. In each case the identity name and REST base URL are read from the environment, while the password is read from the file referenced by `KURA_TOKEN_FILE`.

!!! note
    When Kura's REST endpoint uses HTTPS with a self-signed certificate, the client must either trust that certificate or disable TLS verification (the examples below disable verification for brevity). Prefer trusting the certificate in production.

**Container Code (Python):**
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

**Container Code (Shell):**
```bash
#!/bin/sh
# KURA_IDENTITY_NAME, KURA_TOKEN_FILE and KURA_REST_BASE_URL are provided by Kura
curl -k -u "${KURA_IDENTITY_NAME}:$(cat "${KURA_TOKEN_FILE}")" \
  "${KURA_REST_BASE_URL}/system/info"
```

**Container Code (Node.js):**
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

### Best Practices

1. **Principle of Least Privilege**: Only grant permissions that are absolutely necessary for the container's functionality.

2. **Validate Environment Variables and Token File**: Always check that `KURA_IDENTITY_NAME`, `KURA_TOKEN_FILE`, and `KURA_REST_BASE_URL` are present, and that the file referenced by `KURA_TOKEN_FILE` exists and is non-empty, before making API calls.

3. **Handle Credential Lifecycle**: Be prepared for credentials to become invalid when the container is stopping or restarting.

4. **Error Handling**: Implement proper error handling for API calls, as permissions may be denied if the container doesn't have the required permission.

5. **Network Mode Considerations**: The REST base URL is automatically adjusted based on network mode:
   - **bridge mode** (default): Uses the Docker bridge gateway IP (typically `172.17.0.1`)
   - **host mode**: Uses `localhost`

6. **HTTPS Support**: The REST base URL automatically uses HTTPS if enabled in Kura's HTTP Service configuration.

### Troubleshooting

**Container cannot access Kura APIs:**
- Verify that **Identity Integration Enabled** is set to `true`
- Check that the container has been granted the necessary permissions in **Container Permissions**
- Ensure the container is reading the environment variables and the token file correctly
- If Kura firewall is installed and enabled, allow traffic from container networks (for example `docker0` or user-defined Docker bridges) to the Kura REST API port
- Check container logs for authentication errors

**Token file missing or empty:**
- Verify that `KURA_TOKEN_FILE` is set and points to `/run/secrets/kura-token`
- Confirm the file is mounted read-only and readable by the process inside the container
- Check the Kura logs for token file creation errors (for example a missing or non-writable tmpfs base directory)

**Basic authentication fails:**
- Verify the request includes valid Basic credentials (`KURA_IDENTITY_NAME` as the username and the content of the file referenced by `KURA_TOKEN_FILE` as the password)
- Check that the temporary identity was created successfully in Kura logs
- Ensure the container is using the correct REST base URL
- Verify **Basic Authentication Enabled** is set to `true` in **RestService**

**Permission denied errors:**
- Verify the permission name is correct (case-sensitive)
- Ensure the permission exists in the system (use the REST Identity API to list defined permissions)
- Check that the permission was correctly added to the **Container Permissions** field

## Stopping the container

!!! warning
    Stopping a container will delete it in an irreversible way. Please be sure to only use stateless containers and/or save their data in external volumes.

To stop the container without deleting the component, set the **Enabled** field to **false**, and then press **Apply**. This will delete the running container, but leave this component available for running the container again in the future. If you want to completely remove the container and component, press the **Delete** button to the top right of the screen, and press **Yes** on the confirmation dialogue.



## Container Management Dashboard

The Container Orchestration service also provides the user with an intuitive container dashboard. This dashboard shows all containers running on a gateway, including containers created with the framework and those created manually through the command-line interface. To utilize this dashboard the `org.eclipse.container.orchestration.provider` (ContainerOrchestrationService) must be enabled, and the dashboard can be opened by navigating to Device > Containers.
