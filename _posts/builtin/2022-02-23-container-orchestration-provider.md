---
layout: page
title:  "Container Orchestration Provider"
categories: [builtin]
---

# What is the Container Orchestration Provider?
The Container Orchestration Provider allows Kura to manage Docker. With this tool you can arbitrarily pull and deploy containerized software packages and run them on your gateway. This Provider allows the user to create, configure, start, and stop containers all from the browser. The bundle will also restart containers, if the gateway is restarted.

The Container Orchestration service is composed of two bundles, one that exposes APIs for container management and one that implements those APIs. This API is exposed so that you can leverage it to implement containerization in your own Kura plugins.

***

# How to use the Container Orchestration Provider

## Before Starting
For this bundle to function appropriately, the gateway must have docker installed and the docker daemon must be started and enabled.

## Starting the Service

To use this service select the **ContainerOrchestrationService** option located in the **Services** area. The ContainerOrchestrationService provides the following parameters: 
- **Enabled**--activates the service when set to true
- **Docker Host URL**--provides a string that tells the service where to find the docker engine (best left to the default value).

![Container-API]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/enable_api.png)

## Creating your first container.

To create a container, select the + icon (Create a new component) under **services**. A popup dialogue box will appear. In the field **Factory** select **org.eclipse.kura.container.provider.ContainerInstance** from the drop-down. Then, using the **Name** field, enter the name of the container you wish to create and Finally press submit to create the component.

![Container-API]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/create_container.png)

After pressing submit, a new component will be added under the **services** tab, with the name that was selected in the dialogue. Select this component to finish configuring the container.

##Configuring the container

To begin configuring the container, look under **Services** and select the item which has the name set in the previous step.

Containers may be configured using the following fields:

- **Enabled** - When true, API will create the described container. When false the API will not create the container or will destroy the container if already running.
  
- **Image Name** - Describes the image that will be used to create the container. Remember to ensure that the selected image supports the architecture of the host machine, or else the container will not be able to start.
  
- **Image Tag** - Describes the version of the docker image that will be used to create the container.

- **Authentication Registry URL** - URL for an alternative registry to pull images from. (If the field is left blank, credentials will be applied to Docker-Hub). Please see the [Authenticated Containers ](#authenticated-containers) document for more information about connecting to different popular repositories.

- **Authentication Username** - Describes the username to access the Docker repository entered above.

- **Password** - Describes the password to access the alternative Docker repository.

- **Image Download Retries** - Describes the number of retries the framework will attempt to pull the image before giving up.

- **Image Download Retry Interval** - Describes the amount of time the framework will wait before attempting to pull the image again.

- **Image Download Timeout** - Describes the amount of time the framework will let the image download before timeout.
  
- **Internal Ports** - This field accepts a comma-separated list of ports that will be internally exposed on the spun-up docker container.
  
- **External Ports** - This field accepts a comma-separated list of ports that will be externally exposed on the host machine.
  
- **Privileged Mode** - This flag if enabled will give the container root capabilities to all devices on the host system. Please be aware that setting this flag can be dangerous, and must only be used in exceptional situations.
  
- **Environment Variables (optional)** - This field accepts a comma-separated list of environment variables, which will be set inside the container when spun up.
  
- **Volume Mount (optional)** - This field accepts a comma-separated list of system-to-container file mounts. This allows for the container to access files on the host machine.
  
- **Peripheral Device (optional)** - This field accepts a comma-separated list of device paths. This parameter allows for devices to be passed through from the host to the container.

- **Logger Type** - This field provides a drop-down selection of supported container logging drivers.

- **Logger Parameters (optional)** - This field accepts a comma-separated list of logging parameters. More information can be found in the Docker logger documentation.

After specifying container parameters, ensure to set **Enabled** to **true** and press **Apply**. Docker will then pull the respective image, spin up and start the container. If the gateway is reset (power cycled), and the container and Docker-service are set to **enabled**, ESF will automatically start the container again upon startup. 

![Supported-Container-Configuration]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/container_configuration.png)

## Stopping the container

{% include alerts.html message="Stopping a container will delete it in irreversible way. Please be sure to use only stateless container and/or save their data in external volumes." %}

To stop the container without deleting the component, set the **Enabled** field to **false**, and then press **Apply**. This will delete the running container, but leave this component available for running the container again in the future. If you want to completely remove the container and component, press the **Delete** button to the top right of the screen, and press **Yes** on the confirmation dialog.
![Stop-Container]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/stop_container.png)

## Container Management Dashboard

The Container Orchestration service also provides the user with an intuitive container dashboard. This dashboard shows all containers running on a gateway, including containers created with Kura and those created manually through the command line interface. To utilize this dashboard the **org.eclipse.container.orchestration.provider** (Docker-API) must be enabled, and the dashboard can be opened by navigating to **Device > Docker Containers**.

![Stop-Container]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/container_inventory.png)

***

# Authenticated Containers
The Container Orchestrator provider allows the user to pull images from private and password-protected registries. The following document will provide examples of how to connect to some popular registries.


{% include alerts.html 
message="
These guides make the following two assumptions.

1) That you have already configured the Container Orchestrator and have a container instance already created. Please see the [usage](#how-to-use-the-container-orchestration-provider) doc, to learn the basics of the orchestrator.

2) That the image you are trying to pull supports the architecture of the gateway.
" %}

### Private Docker-Hub Registries

#### Preparation:
 - have a Docker Hub account (its credentials), and a private image ready to pull. 

#### Procedure:

1) Populate the image name field. The username containing the private image must be placed before the image name separated by a forward slash. This is demonstrated below:
- **Image Name: ** ```<Docker-Hub username>/<image name>``` for exmaple```eurotech/esf```.

2) Populate the credential fields: 
- **Authentication Registry URL:** This field should be left blank.
- **Authentication Username:** Your Docker Hub username.
- **Password:** Your Docker Hub password.
![Authenticated-Docker-Hub]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/authenticated-docker-hub.png)

### Amazon Web Services - Elastic Container Registries (AWS-ECR)
#### Preparation:
- Have access to an Amazon ECR instance.
- Have the [AWS-CLI](https://aws.amazon.com/cli/) tool installed and appropriately configured on your computer. 
- Have access to your AWS ECR web console.

#### Procedure:

1) Sign in to your amazon web console, navigate to ECR and identify which container you will like to pull onto the gateway. Copy the URI of the container. This URI will reveal the information required for the following steps. Here is how to decode the URI ```<identifier>.dkr.ecr.<ecr-region>.amazonaws.com/<directory>/<image name>:<image tag>```.

2) Generating an AWS-ECR access password. Open a terminal window on the machine with aws-cli installed and enter the following command ```aws ecr get-login-password --region <ecr-region>```. Your ECR region can be found by inspecting the container URI string copied in the previous step. This command will return a long string which will be used as the repo password in the gateway.

3) Populating information on the gateway. 
 - **Image Name: ** enter the full URI without the tag.```<identifier>.dkr.ecr.<ecr-region>.amazonaws.com/<directory>/<image name>```
- **Image Tag:** enter only the image tag found at the end of the URI  ```<image tag>```
- **Authentication Registry URL:** Paste only the part of the URI before the image name   ```<identifier>.dkr.ecr.<ecr-region>.amazonaws.com/<directory>/```
- **Authentication Username:** will be ```AWS```
- **Password:** will be the string created in step two.

A fully configured container set to pull AWS will look like the following.
![Authenticated-AWS-ECR]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/authenticated-aws-ecr.png)

***

# How to leverage the Container Orchestration Provider API?

The Container Orchestration Provider exposes an API that can be leveraged by other framework plugins. The following is an overview of how these APIs work together.

![API-Overview]({{ site.baseurl }}/assets/images/builtin/container_orchestrator/api-overview.png)

##ContainerOrchestrationService
The ContainerOrchestrationService is used to directly communicate with the running container engine. It exposes methods for listing, creating, and stopping containers. This class utilizes an instantiated ContainerConfiguration object as a parameter for container creation.

##ContainerConfiguration
The ContainerConfiguration class, allows you to define a container to create. Using the embedded builder class, one can define many container-related parameters such as name, image, ports and volume mounts.

##ContainerInstanceDescriptor
The ContainerInstanceDescriptor class is used to describe a container that has already been created. This class contains runtime information such as the ID of the container.

##ContainerState
The ContainerState is a class that exposes an enum of container states tracked by the framework.

##PasswordRegistryCredentials
The PasswordRegistryCredentials class stores password credentials when provisioning a container to pull from an alternative password-protected registry.

##PasswordRegistryCredentials
The PasswordRegistryCredentials class stores password credentials when provisioning a container to pull from an alternative password-protected registry.

{% include alerts.html 
message="
The Container Orchestration Provider exports an MQTT-Namespace API. This API can be used to manage containers via MQTT requests from external applications such as Everywhere Cloud. Please visit the  [Remote Gateway Inventory via MQTT](../ref/mqtt-namespace.html) documentation for more information.
" %}