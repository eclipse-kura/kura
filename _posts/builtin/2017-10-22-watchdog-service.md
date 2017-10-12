---
layout: page
title:  "Watchdog Service"
categories: [builtin]
---

The WatchdogService provides methods for starting, stopping, and updating a hardware watchdog if it is present on the system. Once started, the watchdog must be updated to prevent the system from rebooting.

To use this service, select the **WatchdogService** option located in the **Services** area as shown in the screen capture below.

![watchdog_service]({{ site.baseurl }}/assets/images/builtin/watchdog_service.png)

This service provides the following configuration parameters:

- **enabled** - sets whether or not this service is enabled or disabled. (Required field.)

- **pingInterval** - defines the maximum time interval between two watchdogs' refresh to prevent the system from rebooting. (Required field.)

- **Watchdog device path** - sets the watchdog device path. (Required field.)

- **Reboot Cause File Path** - sets the path to the file that will contain the reboot cause information. (Required field.)