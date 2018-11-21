---
layout: page
title:  "Docker Quick Start"
categories: [intro]
---

[Installation](#installtion)

[Command Toolbox](#command-toolbox)

[Eclipse Kura&trade; Installation](#eclipse-kuratrade-installation)

[Development Environment Installation](#development-environment-installation)

## Installation

Eclipse Kura is also available as a Docker container available in [Docker Hub](https://hub.docker.com/r/eclipse/kura/)

To download and run, run the following command:

```
docker run -d -p 8080:8080 eclipse/kura
```

This command will start Kura in background and the Kura Web Ui will be available through port 8080.


## Command Toolbox

Following a set of useful Docker command that can be used to list and manage Docker containers.
For more details on Docker commands, please reference the official [Docker documentation](https://docs.docker.com/get-started/) 

### List Docker Images

To list all the installed docker images run:

```
docker images
```

### List Running Docker Containers

To list all the available instances (both running and powered off) run:

```
docker ps -a
```

### Start/Stop a Docker Container

```
docker stop <container id>
docker start <container id>
```

where `<container id>` is the instance identification number.
