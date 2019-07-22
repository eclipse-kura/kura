---
layout: page
title:  "Docker Quick Start"
categories: [intro]
---

[Installation](#installation)

[Command Toolbox](#command-toolbox)

## Installation

Eclipse Kura is also available as a Docker container available in [Docker Hub](https://hub.docker.com/r/eclipse/kura/)

To download and run, use the following command:

```
docker run -d -p 8080:8080 -t eclipse/kura
```

This command will start Kura in background and the Kura Web Ui will be available through port 8080.

Once the image is started you can navigate your browser to [http://localhost:8080](http://localhost:8080) and log in using the credentials admin : admin.


## Command Toolbox

Following, a set of useful Docker command that can be used to list and manage Docker containers.
For more details on Docker commands, please reference the official [Docker documentation](https://docs.docker.com/get-started/) 

### List Docker Images

To list all the installed Docker images run:

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
