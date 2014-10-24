---
layout: page
title:  "Heater Demo"
date:   2014-08-24 11:29:11
categories: [doc]
---

[Overview](#overview)

-   [Prerequisites](#section)

[Heater Demo Introduction](#_Heater_Demo_Introduction)

[Run the Bundle](#run-the-bundle)

[Configure the MQTT Client](#configure-the-mqtt-client)

[Modify Bundle Configuration in Local Web
UI](#modify-bundle-configuration-in-local-web-ui)

Overview
========

This section describes the prepackaged heater demo bundle that comes
with the Kura development environment and demonstrates how to perform
the following functions:

-   Run the Kura Emulator

-   Connect to the Cloud

-   Gain an understanding of ConfigurableComponents in Kura

-   Modify configurations of custom bundles

Prerequisites
-------------

-   [Setting up Kura Development Environment](kura-setup.html)

-   [Testing and Deploying Bundles](deploying-bundles.html)

-   Using the Kura web UI

<span id="_Hello_World_Using" class="anchor"><span id="_Heater_Demo" class="anchor"><span id="_Heater_Demo_Introduction" class="anchor"></span></span></span>Heater Demo Introduction
=====================================================================================================================================================================================

The org.eclipse.kura.demo.heater bundle is a simple OSGi bundle that
represents a thermostat and heater combination. The application utilizes
the Kura ConfigurableComponent interface to be able to receive
configuration updates through the local Kura web UI. In addition, this
bundle utilizes OSGi declarative services and the Kura
CloudClientListener. This tutorial demonstrates how to modify
configurations of custom bundles and shows how those configuration
changes can dynamically impact the behavior of the bundle through the
Kura web UI.

Run the Bundle
==============

By default, the heater demo bundle does not run automatically. To run
the bundle and Kura in the Emulator, locate the
**org.eclipse.kura.emulator** project. Expand it to show the
src/main/resources folder.

Right-click the correct **Kura_Emulator_*[OS]*.launch** file,
depending on which operating system you are running. In the context
menu, select the **Run As** option, and click on **Run Configurations**.

Under OSGi Framework (Run Configurations window shown below), click on
the **Kura_Emulator_[OS]** entry. In the Bundles tab under Workspace,
enable the **org.eclipse.kura.demo.heater** checkbox to enable it as
shown below:

![]({{ site.baseurl }}/assets/images/heater_demo///media/image1.png)

Click the **Apply** and **Run** buttons to start the Kura Emulator. Once
this setting has been made, you only need to right-click on the launch
file and select **Run As** and the **Kura_Emulator_[OS]** option to
run with the same settings.

This will start Kura running locally and will display a Console window
in Eclipse. The Console window will show the OSGi diagnostics as various
bundles start and execute.

Configure the MQTT Client
=========================

With the heater demo bundle running, open a browser window on the same
computer as the Eclipse development environment and browse to the Kura
web UI at <http://127.0.0.1:8080>. Once connected to the Kura web UI, a
log in window appears prompting you to enter the Name and Password as
shown below:

![]({{ site.baseurl }}/assets/images/heater_demo///media/image2.png)

Enter the appropriate name and password (default is admin/admin) and
click **Log in**. The Kura Admin web UI appears as shown below:

![](../../assets/images/heater_demo///media/image3.png)

From the Kura web UI, click on **MqttDataTransport** in the Services
pane on the lower left of the browser window. You will see a menu
similar to the one shown in the following screen capture:

![]({{ site.baseurl }}/assets/images/heater_demo///media/image4.png)

Fill in the following fields then click the **Apply** button:

|Field|Value|
|------------|-------|
|broker-url:      |             the url for the MQTT broker (this example shows the MQTT broker-url **mqtt://iot.eclipse.org:1883/** hosted by the Eclipse Foundation)|
|topic.context.account-name:|   your [account_name]|
|username:            |         typically [account_name]_broker|
|password:            |        the password for your user|
|client-id            |        the client identifier to be used when connecting to the MQTT broker (optional)|
<br />
Now that the account credentials are set in the MqttDataTransport
service, the DataService needs to be configured to connect by default.
To do so, click **DataService** in the Services area on the left of the
browser window. For the ‘connect.auto-on-startup’ parameter, select
**true** as shown below:

![]({{ site.baseurl }}/assets/images/heater_demo///media/image5.png)

<span id="_View_and_Modify" class="anchor"><span
id="_Modify_Configuration_and" class="anchor"></span></span>

Modify Bundle Configuration in Local Web UI
===========================================

Bundles changes may be made directly in the emulator web UI. Since you
are running an emulated device in Eclipse, you can do this by browsing
to <http://127.0.0.1:8080> (same URL where the MQTT client was
configured in the previous section of this tutorial). If the bundle was
running on a real device and you had network access to it, you would
browse to
[http://[ip_address_of_device]](http://ip_address_of_device/).

From the Kura web UI, select the Heater bundle from the configurable
services on the left and modify the parameters as needed (shown in the
screen capture below). By default, the heater demo is configured
according to the following characteristics and assumptions about its
operational environment:

-   Start operation is at 6:00am (06:00).

-   End operation is at 10:00pm (22:00).

-   It is colder outside than inside the heated chamber (hard-coded to 5
    degrees in the application).

-   Output of the heater is constant at 30 degrees (hard-coded).

-   When in operational mode, the temperature will drop inside if the
    heater is off.

-   The heater turns off when it is about to exceed the setPoint defined
    in the configuration.

-   After the temperature drops to four times the increment point (a
    made-up value to show dropping temperature, hard-coded in the
    application), the heater turns back on, and the temperature starts
    increment at the rate of the ‘temperature.increment’ rate.

![]({{ site.baseurl }}/assets/images/heater_demo///media/image6.png)

Click **Apply** for changes to take affect. The updated() method is
called after settings are applied for the new configuration.

After completing this tutorial, it is highly recommended that you review
the heater demo source code in Eclipse to see how it is put together.
Kura automatically generates the user configuration interface through
implementation of the ConfigurableComponent interface and some small
additions to the component.xml file (called heater.xml). This powerful
feature provides both a local and remote configuration user interface
with no additional development requirements.
