---
layout: page
title:  "Cloud Configuration"
date:   2014-08-22 11:31:11
categories: [doc]
---

[Overview](#_Overview)

-   [Prerequisites](#prerequisites)

[Connecting to the Cloud](#_Connecting_to_the)

-   [Local Emulation Mode](#local-emulation-mode)

-   [Run Kura in Emulator Mode](#_Run_ESF_in)

-   [Run Kura on a Remote Target Device](#_List_OSGi_Bundles)

-   [Configuring the Cloud Service](#configuring-the-cloud-service)

<span id="_Overview_1" class="anchor"><span id="_Overview" class="anchor"></span></span>Overview
================================================================================================

Eclipse Kura offers Data Services, where store and forward functionality
for the telemetry data is collected by the gateway and published to
remote servers. Eclipse Paho and its MQTT (MQ Telemetry Transport)
client provide the default-messaging library used. Designed for
constrained devices and low-bandwidth, high-latency or unreliable
networks, MQTT’s publish/subscribe, lightweight messaging protocol,
minimizes network bandwidth and device resource requirements while also
attempting to ensure reliability and delivery assurance in M2M / IoT
solutions.\
\
Kura also offers Cloud Services with an easy-to-use API layer for the
M2M application to communicate with a remote server. In addition to
simple publish/subscribe, the Cloud Service API simplifies the
implementation of more complex interaction flows like request/response
or remote resource management. It allows for a single connection to a
remote server to be shared across more than one application in the
gateway providing the necessary topic partitioning.

This section provides information on how to connect your Kura-enabled
device to an MQTT broker-url, which handles receiving published messages
and sending them on to clients who have subscribed.

Prerequisites
-------------

-   Requires Kura development environment set-up ([Setting up Kura
    Development Environment](kura-setup.html))

-   Implements the use of Kura web user-interface (UI)

<span id="_Hello_World_Using" class="anchor"><span id="_Testing_the_Plug-in" class="anchor"><span id="_Testing_the_OSGi" class="anchor"><span id="_Connecting_to_the" class="anchor"></span></span></span></span>Connecting to the Cloud
========================================================================================================================================================================================================================================

With the Eclipse Kura development environment set up, you can either:

-   Configure the emulator to connect to the Cloud (see [Local Emulation
    Mode](#local-emulation-mode))

-   Configure a physical device running Kura to connect to the Cloud
    (see [Run Kura on a Remote Target Device](#_List_OSGi_Bundles))

Local Emulation Mode
--------------------

The Kura user workspace can be used in Eclipse in local emulation mode
(Linux/OS X only; this feature is not currently supported under
Windows). To deploy the code to a running system, refer to the section
[Run Kura on a Remote Target Device](#_List_OSGi_Bundles).

<span id="_Running_ESF_in" class="anchor"><span id="_Run_ESF_in" class="anchor"></span></span>Run Kura in Emulator Mode
-----------------------------------------------------------------------------------------------------------------------

In the Eclipse workspace, locate the **org.eclipse.kura.emulator**
project. Expand it to show the src/main/resources folder.

Right-click the correct **Kura_Emulator_*[OS]*.launch** file,
depending on which operating system you are running (where “*[OS]*” is
“Linux” for a Linux system or “OSX” for a OS X system). In the context
menu, select the **Run as** option, and select the
**Kura_Emulator_*[OS]***. With Kura running locally, the OSGi
diagnostics will display in the Eclipse Console window as various
bundles start and execute. The output indicates that Kura has started
and will appear similar to the following:

![]({{ site.baseurl }}/assets/images/cloud_configuration/media/image1.png)

At this point, the Kura web UI, which starts a Web service on the local
computer, is available at <http://127.0.0.1:8080/>.<span
id="_List_OSGi_Bundles" class="anchor"><span id="_Run_ESF_on"
class="anchor"><span id="_Run_ESF_on_1" class="anchor"><span
id="_Run_ESF_on_2" class="anchor"></span></span></span></span> Refer to
the section [Configuring the Cloud
Service](#configuring-the-cloud-service) to set the Cloud credentials.

Run Kura on a Remote Target Device
----------------------------------

Kura is designed to start automatically on remote target devices. With
it preinstalled, simply power-on the device and wait for it to fully
boot. Once the device is up and running, connect to it with a browser at
http://*ip_address*, where “*ip_address*” should be replaced by the IP
address of the device. For information on the default IP settings for
various devices, see [Kura Default
Configuration](4.01-ESF-Default-Configuration.asp).

Configuring the Cloud Service
-----------------------------

Once connected to the Kura web UI, a log in window appears prompting you
to enter the Name and Password as shown below:

![]({{ site.baseurl }}/assets/images/cloud_configuration/media/image2.png)

Enter the appropriate name and password (default is admin/admin) and
click **Log in**. The Kura Admin web UI appears as shown below:

![]({{ site.baseurl }}/assets/images/cloud_configuration/media/image3.png)

To configure the Cloud credentials, click **MqttDataTransport** in the
Services area on the left side of the browser window. The Kura web UI
displays the MQTT connection parameters and allows you to configure them
as needed (shown below).

![]({{ site.baseurl }}/assets/images/cloud_configuration/media/image4.png)

Configure the Cloud credentials as needed. The most pertinent fields
include:

  broker-url:                   the url for the MQTT broker (this example shows the MQTT broker-url **mqtt://iot.eclipse.org:1883/** hosted by the Eclipse Foundation)
  ----------------------------- ----------------------------------------------------------------------------------------------------------------------------------------
  topic.context.account-name:   your *[account_name]*
  username:                     typically *[account_name]*_broker
  password:                     the password for your user

Once these credentials are set, click the **Apply** button near the top
left of the configuration pane.

Now that the account credentials are set in the MqttDataTransport
service, the DataService needs to be configured to connect by default.
To do so, click **DataService** in the Services area on the left of the
browser window. For the ‘connect.auto-on-startup’ parameter, select
**true** as shown below:

![]({{ site.baseurl }}/assets/images/cloud_configuration/media/image5.png)

Click **Apply**. At this point, the MQTT client in Kura is configured to
automatically connect to the Cloud.

<span id="_See_the_Device" class="anchor"><span id="_Verify_Device_is"
class="anchor"></span></span>
