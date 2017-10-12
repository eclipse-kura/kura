---
layout: page
title:  "Command Service"
categories: [builtin]
---

The CommandService provides methods for running system commands from the web console of a cloud platform. Currently it is supported in Everyware Cloud. In this case, the service also provides the ability for a script to execute using the **File** option of the **Command** tab in the Everyware Cloud Console. This script must be compressed into a zip file with the eventual, associated resource files.

Once the file is selected and **Execute** is clicked, the zip file is sent embedded in an MQTT message on the device. The  Command Service in the device stores the file in /tmp, unzips it, and tries to execute a shell script if one is present in the file. Note that in this case, the Execute parameter cannot be empty; a simple command, such as "ls -l /tmp", may be entered.

To use this service, select the **CommandService** option located in the **Services** area as shown in the screen capture below.

![command_service]({{ site.baseurl }}/assets/images/builtin/command_service.png)

The CommandService provides the following configuration parameters:

- **command.enable** - sets whether this service is enabled or disabled in the cloud platform. (Required field.)

- **command.password.value** - sets a password to protect this service.

- **command.working.directory** - specifies the working directory where the command execution is performed.

- **command.timeout** - sets the timeout (in seconds) for the command execution.

- **command.environment** - supplies a space-separated list of environment variables in the format key=value.

When a command execution is requested in the cloud platform, it sends an MQTT control message to the device requesting that the command be executed. On the device, the Command Service opens a temporary shell as root in the _command.working.directory,_ sets the _command.environment_ variables (if any), and waits  _command.timeout_ seconds to get command response.