---
layout: page
title:  "Connection Monitors in DataService"
categories: [config]
---

The DataService offers methods and configuration options to monitor the connection to the remote server and, eventually, cause a system reboot to recover from transient network problems.

This feature, if enabled, leverages the watchdog service and reboots the gateway if the maximum number of configured connection attempts has been made.

A reboot is not requested if the connection to the remote broker succeeds but an _authentication error_, an _invalid client id_ or an _authorization error_ is thrown by the remote cloud platform and causes a connection drop.

The image below shows the parameters that need to be tuned in order to enable this connection monitor feature.

![kura_cloud_stack]({{ site.baseurl }}/assets/images/config/Kura_connection-monitor.png)

To configure this functionality, the System Administrator needs to specify the following configuration elements:

* **enable.recovery.on.connection.failure** - when enabled, activates the recovery feature on connection failure: if the device is not able to connect to a remote cloud platform, the service will wait for a specified amount of connection retries. If the recovery fails, the device will be rebooted. Being based on the Watchdog service, it needs to be activated as well.

* **connection.recovery.max.failures** - related to the previous parameter. It specifies the number of failures before a reboot is requested.

{% include alerts.html message='To be fully working, this feature needs the enabling of the Watchdog Service.' %}