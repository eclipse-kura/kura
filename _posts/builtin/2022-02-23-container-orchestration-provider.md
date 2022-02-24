---
layout: page
title:  "Container Orchestration Provider"
categories: [builtin]
---

# What is the Container Orchestration Provider?
The Container Orchestration Provider allows Kura to manage Docker. With this tool you can arbitrarily pull and deploy containerized software packages and run them on your gateway. This Provider allows the user to create, configure, start, and stop containers all from the browser. The bundle will also restart containers, if the gateway is restarted.

The Container Orchestration service is composed by two bundles, one that exposes APIs for container management and one that implements those APIs. This API is exposed so that you can leverage it to implement containerization in your own Kura plugins.

***

# How to use the Container Orchestration Provider

## Before Starting
For this bundle to function appropriately, the gateway must have docker installed and the docker daemon must be started and enabled.

## Starting the Service

To use this service select the **Docker-API** option located in the **Services** area. The docker-api service provides the following parameters: **Enabled**--activates the service when set to true, and **Docker Host URL**--provides a string which tells the service where to find the docker engine (best left to the default value). Optionally a user can provide a Repository/Registery URL and respective credentials so that containers will be pulled from a alternative source such as AWS-ECR.

![Container-API]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/enable_api.png)

## Creating your first container.

To create a container, select the + icon (Create a new component) under **services**. A pop up dialog box will appear. In the field **Factory** select **org.eclipse.kura.container.provider.ConfigurableGenericDockerService** from the drop-down. Then, using the **Name** field, enter the name of the container you wish to create and Finally press submit to create the component.

![Container-API]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/create_container.png)

After pressing submit, a new component will be added under the **services** tab, with the name that was selected in the dialog. Select this component to finish configuring the container.

## Configuring the container

To begin configuring the container, look under **Services** and select the item which has the name set in the previous step.

Containers may be configured using the following fields:

- **Enabled** - When true, api will create the described container. When false api will not create the container, or will destroy the container if already running.
  
- **Image Name** - Describes the docker image that will be used to create the container. Remember to ensure that the selected image supports the architecture of the host machine, or else the container will not be able to start.
  
- **Image Tag** - Describes the version of the docker image that will be used to create the container.
  
- **Internal Ports** - This field accepts a comma separated list of ports which will be internally exposed on the spun up docker container.
  
- **External Ports** - This field accepts a comma separated list of ports which will be externally exposed on the host machine.
  
- **Privileged Mode** - This flag if enabled will give the container root capabilities to all devices on the host system. Please be aware that setting this flag can be dangerous, and must only be used in exceptional situations.
  
- **Environment Variables (optional)** - This field accepts a comma separated list of environment variables, which will be set inside the container when spun up.
  
- **Volume Mount (optional)** - This field accepts a comma separated list of system-to-container file mounts. This allows for the container to access files on the host machine.
  
- **Peripheral Device (optional)** - This field accepts a comma separated list of device paths. This parameter allows for devices to be passed though from the host to the container.

- **Logger Type** - This field provides a dropdown selection of supported container logging drivers.
  
- **Logger Parameters (optional)** - This field accepts a comma separated list of logging parameters. More information can be found in the [Docker logger documentation](https://docs.docker.com/config/containers/logging/configure/#supported-logging-drivers).
  

After specifying container parameters, ensure to set **Enabled** to **true** and press **Apply**. Docker will then pull the respective image locally, or through docker hub, spin up and start the container. If the gateway is reset (power cycled), and the container and Docker-service is set to **enabled**, Kura will automatically start the container again upon startup. 
![Supported-Container-Configuration]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/container_configuration.png)

## Stopping the container

To stop the container without deleting the component, set the **Enabled** field to **false**, and then press **Apply**. This will delete the running container, but leave this component available for running the container again in the future. If you want to completely remove the container and component, press the **Delete** button to the top right of the screen, and press **Yes** on the confirmation dialog.
![Stop-Container]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/stop_container.png)

## Container Management Dashboard

The Container Orchestration service also provides the user with a intuitive container dashboard. This dashboard shows all containers running on a gateway, including containers created with Kura and those created manually though the command line interface. To utilize this dashboard the **org.eclipse.container.orchestration.provider** (Docker-API) must be enabled, and the dashboard can be opened by navigating to **Device > Docker Containers**.

![Stop-Container]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/container_inventory.png)

***

# How to leverage the Container Orchestration Provider API?

The Kura Container Orchestration Provider bundle exposes two main classes: 1) DockerService, 2) ContainerDescriptor.

![API-Overview]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/api-overview.png)

## DockerService Interface
This interface is used to directly communicate with the running docker instance. It exposes methods for listing, creating, and stopping containers, and utilizes an instantiated ContainerDescriptor object as a parameter.

## ContainerDescriptor
The ContainerDescriptor class, allows you to define a container to create. Using the embedded builder class, one can define many container related parameters such as name, image, ports and volume mounts.

# Inventory-V1 API
Using the API exposed by Inventory-V1, one can start and stop docker containers via external applications such as Everywhere Cloud.

## List All Containers

This operation lists all the containers installed in the gateway.

* Request Topic:
  * **$EDC/account_name/client_id/INVENTORY-V1/GET/containers**
* Request Payload:
  * Nothing application-specific beyond the request ID and requester client ID
* Response Payload:
  * Installed containers serialized in JSON format

The following JSON message is an example what this request outputs:
    
    {
      "containers":
      [
        {
          "name":"docker_container_1",
          "version":"nginx:latest",
          "type":"DOCKER"
        }
      ]
    }
    
The container JSON message is comprised of the following elements:
    - Name: The name of the docker container.
    - Version: describes both the container's respective image and tag separated by a colon.
    - Type: denotes the type of inventory payload

## Start Container

This operation allows to start a container installed on the gateway.
* Request Topic
  * $EDC/account_name/client_id/INVENTORY-V1/EXEC/containers/_start
* Request Payload
  * A JSON object that identifies the target container must be specified in payload body. This payload will be described in the following section
* Response Payload
  * Nothing application specific

## Stop Container

* Request Topic
  * $EDC/account_name/client_id/INVENTORY-V1/EXEC/containers/_stop
* Request Payload
  * A JSON object that identifies the target container must be specified in payload body. This payload will be described in the following section.
* Response Payload
  *Nothing application specific

## JSON identifier/payload for start and stop requests

The requests for starting and stopping a container require the application to include a JSON object in request payload for selecting the target container. Docker enforces unique container names on a gateway, and thus they can reliably be used as a identifier.

Example 1:
    
    {
    "name":"docker_container_1",
    "version":"nginx:latest",
    "type":"DOCKER"
    }


Example 2:
    
    {
    "name":"docker_container_1",
    }

