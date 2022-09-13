---
layout: page
title:  "User guide"
categories: [cloud-api]
---

This guide will illustrate the steps required for configuring an application that uses the new Cloud Connection APIs to publish messages to the Kapua platform.

The involved steps are the following

1. [Instantiation and configuration of the Cloud Connection](#creating-a-new-cloud-connection).
2. [Instantiation and configuration of a Publisher](#creating-and-configuring-a-new-publisher).
3. [Binding an application to the Publisher](#binding-an-application-to-a-publisher).

## Creating a new Cloud Connection

1. Open the Cloud Connections section of the Web UI:

![cloud-connections](https://s3-us-west-2.amazonaws.com/kura-repo/kura-github-wiki-images/generic-cloud-services/cloud-connections-user-1.png)

2. Create a new Cloud Connection

  1. Click on the **New Connection** button

  ![cloud-connections](https://s3-us-west-2.amazonaws.com/kura-repo/kura-github-wiki-images/generic-cloud-services/cloud-connections-user-2.png)

  2. Enter a new unique identifier in the **Cloud Connection Service** PID field. The identifier must be a valid `kura.service.pid` and, in case of a Kapua Cloud Connection, it must start with the `org.eclipse.kura.cloud.CloudService-` prefix. A valid identifier can be `org.eclipse.kura.cloud.CloudService-KAPUA`. As an alternative it is possible to reconfigure the existing `org.eclipse.kura.cloud.CloudService` Cloud Connection.

  3. Configure the `MQTTDataTrasport` service.

  ![cloud-connections](https://s3-us-west-2.amazonaws.com/kura-repo/kura-github-wiki-images/generic-cloud-services/cloud-connections-user-3.png)

  Click on the `MQTTDataTrasport-KAPUA` tab and fill the parameters required for establishing the MQTT connection:

  * `Broker-url`
  * `Topic Context Account-Name`
  * `Username`
  * `Password`

  4. Configure the `DataService-KAPUA` service.

  ![cloud-connections](https://s3-us-west-2.amazonaws.com/kura-repo/kura-github-wiki-images/generic-cloud-services/cloud-connections-user-4.png)

   In order to enable automatic connection, set the `Connect Auto-on-startup` parameter to `true`

## Creating and configuring a new Publisher

  1. Select to the connection to be used from the list.

  2. Click on the **New Pub/Sub** button.

  ![cloud-connections](https://s3-us-west-2.amazonaws.com/kura-repo/kura-github-wiki-images/generic-cloud-services/cloud-connections-user-5.png)

  3. Select the type of component to be created, from the **Available Publisher/Subscriber factories** drop down list, in order to create a Publisher
  select the `org.eclipse.kura.cloud.publisher.CloudPublisher` entry.

  4. Enter an unique kura.service.pid identifier in the **New Publisher/Subscriber PID** field.

  5. Click **Apply**, you should see the publisher configuration

  ![cloud-connections](https://s3-us-west-2.amazonaws.com/kura-repo/kura-github-wiki-images/generic-cloud-services/cloud-connections-user-6.png)

  6. Select and configure the newly created publisher instance, and then click **Apply**

## Binding an application to a publisher

  1. Select the application instance configuration

  2. Find the configuration entry that represents a Publisher reference.

  3. Click on the **Select available targets** link and select the desired Publisher instance to bind to.

  ![cloud-connections](https://s3-us-west-2.amazonaws.com/kura-repo/kura-github-wiki-images/generic-cloud-services/cloud-connections-user-7.png)

  4. Click on **Apply**
