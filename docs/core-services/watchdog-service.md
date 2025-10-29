# Watchdog Service

The WatchdogService provides methods for starting, stopping, and updating a hardware watchdog if it is present on the system. Once started, the watchdog must be updated to prevent the system from rebooting.

To use this service, select the **WatchdogService** option located in the **Services** area as shown in the screen capture below.

![Watchdog Service](./images/watchdog-service.png)

This service provides the following configuration parameters:

- **enabled** - sets whether or not this service is enabled or disabled. (Required field)

- **pingInterval** - defines the maximum time interval (in milliseconds) between two watchdogs' refresh to prevent the system from rebooting. (Required field). The interval should be set to a value inferior to the system's **watchdog timeout**, otherwise the watchdog will never be refreshed on time and the system will be rebooted. On most systems, the watchdog timeout can be inspected with the following command:
    ```bash
    wdctl <watchdog-device>
    ```
    The `wdctl` tool allows to change the default timeout value. Refer to specific [man page](https://man7.org/linux/man-pages/man8/wdctl.8.html) for details.

- **Watchdog device path** - sets the watchdog device path. (Required field)

- **Reboot Cause File Path** - sets the path to the file that will contain the reboot cause information. (Required field)