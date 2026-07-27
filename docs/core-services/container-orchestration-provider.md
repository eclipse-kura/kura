# Container Orchestration Provider

The Container Orchestration Provider allows Kura to manage Docker. With this tool, you can arbitrarily pull and deploy containerized software packages and run them on your gateway. This Service allows the user to create, configure, start, and stop containers all from the browser. The bundle will also restart containers if the gateway is restarted.

The feature is composed of two bundles, one that exposes APIs for container management and one that implements those APIs. This API is exposed so that you can leverage it to implement containerization in your own Kura plugins.

## Key Features

- **Container Lifecycle Management**: Create, start, stop, and manage Docker containers through Kura's web interface
- **Image Management**: Pull images from Docker Hub or authenticated registries with automatic retry and timeout configuration
- **Resource Control**: Configure CPU, memory, GPU allocation, and cgroup settings for containers
- **Network Configuration**: Support for various Docker networking modes (bridge, host, custom networks)
- **Security**: Container image signature verification and allowlist enforcement
- **Identity Integration**: Automatic provisioning of temporary credentials for containers to securely access Kura's REST APIs, delivered through a read-only token file on an in-memory filesystem (tmpfs).
- **Persistence**: Automatic container restart after gateway reboot if configured