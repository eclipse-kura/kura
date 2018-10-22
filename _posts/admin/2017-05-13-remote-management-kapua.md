---
layout: page
title:  "Remote Management with Eclipse Kapua"
categories: [admin]
---

## Built-in Services Management

This section describes the remote management of devices running Kura via Eclipse Kapua Console. The Eclipse Kapua Web Console provides the administration tools used for the management of the built-in services exposed by Kura.

To remotely manage a device running Kura through the Eclipse Kapua Web Console, select the desired device from the **Devices Table** of the console and open the **Configuration** tab as shown in the screen capture below. Please refer to the **Built-in Services** section for a description of the available **Services** and their configuration parameters.

![]({{ site.baseurl }}/assets/images/admin/KapuaConfiguration.png)

## Installation of a New Application

As described in  [Application Management](application-management.html), a new application embedded in a deployment package can be deployed and configured using Eclipse Kapua Console.

To do so, select a connected device and click on the **Packages** tab. Then, click on **Install/Upgrade**. The **Install New Package** window opens allowing the deployment package to be installed from an URL as shown in the screen capture below. Once installed, the new application parameters may be modified in the same way as the Built-in Services. Click on the **Configuration** tab to see the service that corresponds to your application.

![]({{ site.baseurl }}/assets/images/admin/KapuaPackages.png)

## Snapshots

As described in [Snapshot Management](snapshot-management.html), the overall Kura configuration, including the new installed applications, is stored in a snapshot xml file. The Eclipse Kapua Console also provides options to **Download**, **Upload and Apply**, or **Rollback** snapshots as shown in the screen capture below.

![]({{ site.baseurl }}/assets/images/admin/KapuaSnapshots.png)

## Remote Command Execution from Eclipse Kapua Web Console

The Eclipse Kapua Console provides the ability to run system commands directly on the device. Refer to [Command Service]({{ site.baseurl }}/builtin/command-service.html) for details on how to configure this service in Kura.

It is also possible to send a script to execute using the **File** option of the **Command** tab in Eclipse Kapua Console as shown in the screen capture below. This script must be compressed into a zip file with the eventual associated resource files. Once the file is selected, click **Execute**.

The zip file is sent embedded in an MQTT message on the device. The Command Service in the device stores the file in /tmp, unzips it, and tries to execute a shell script if one is present. Note that in this case, the Execute parameter cannot be empty; a simple command, such as "ls -l /tmp", may be entered.

![]({{ site.baseurl }}/assets/images/admin/KapuaCommand.png)
