---
layout: page
title:  "Built-in Services Configuration"
categories: [config]
---

This document describes the administration tools available using the [Kura Gateway Administration Console](console.html). This web interface provides the ability to configure all services and applications that are installed and running on the gateway and is shown in the following screen capture:

![]({{ site.baseurl }}/assets/images/config/adminConsole.png)

This section describes the functionality and configuration of the standard services included in Kura. Other sections describe the management of the applications that may be installed on top of Kura.

## Clock Service

The ClockService handles the date and time management of the system. If enabled, it tries to update the system date and time using a Network Time Protocol (NTP) server.

To manage the system date and time, select the **ClockService** option located in the **Services** area as shown in the screen capture below.

![]({{ site.baseurl }}/assets/images/config/clock.png)

The ClockService provides the following configuration parameters:

- **enabled** - sets whether or not this service is enabled or disabled. (Required field.)

- **clock.set.hwclock** - defines if the hardware clock of the gateway must be synced after the system time is set. If enabled, the service calls the Linux command "hwclock --utc --systohc".

- **clock.provider** - specifies either Java NTP client (java-ntp), or a direct call to the native Linux ntpdate command (ntpd). (Required field.)

- **clock.ntp.host** - sets a valid NTP server host address.

- **clock.ntp.port** - sets a valid NTP port number.

- **clock.ntp.timeout** - specifies the NTP timeout in milliseconds.

- **clock.ntp.max-retry** - defines the number of retries when a sync fails (retry at every minute). Subsequently, the next retry occurs on the next refresh interval.

- **clock.ntp.retry.interval** - defines the interval in seconds between each retry when a sync fails. If the _clock.ntp.refresh-interval_ parameter is less than zero, there is no update. If the _clock.ntp.refresh-interval_ parameter is equal to zero, there is only one try at startup. (Required field.)

- **clock.ntp.refresh-interval** - defines the frequency (in seconds) at which the service tries to sync the clock. Note that at the start of ESF, when the ClockService is enabled, it tries to sync the clock every minute until it is successful. After a successful sync, this operation is performed at the frequency defined by this parameter. If the value is less than zero, there is no update. If the value is equal to zero, syncs only once at startup.

## Position Service

The PositionService provides the geographic position of the gateway if a GPS component is available and enabled.

When this service is enabled and provides a valid geographic position, this position is published in the gateway birth certificate.

The GPS connection parameters must be defined in order to allow the service to receive the GPS frames.

For a device that is not connected to a GPS, it is possible to define a _static_ position by entering _latitude,_ _longitude,_ and _altitude._ In this case, the position is returned by the PositionService as if it were an actual GPS position. This may be useful when a gateway is installed in a known place and does not move.

To use this service, select the **PositionService** option located in the **Services** area as shown in the screen capture below.

![]({{ site.baseurl }}/assets/images/config/positionService.png)

This service provides the following configuration parameters:

- **enabled** - defines whether or not this service is enabled or disabled. (Required field.)

- **static** - specifies true or false whether to use a static position instead of a GPS. (Required field.)

- **latitude** - provides the static latitude value in degrees.

- **longitude** - provides the static longitude value in degrees.

- **altitude** - provides the static altitude value in meters.

- **port** - supplies the USB or serial port of the GPS device.

- **baudRate** - supplies the baud rate of the GPS device.

- **bitsPerWord** - sets the number of bits per word (databits) for the serial communication to the GPS device.

- **stopbits** - sets the number of stop bits for the serial communication to the GPS device.

- **parity** - sets the parity for the serial communication to the GPS device.

## Watchdog Service

The WatchdogService provides methods for starting, stopping, and updating a hardware watchdog if it is present on the system. Once started, the watchdog must be updated to prevent the system from rebooting.

To use this service, select the **WatchdogService** option located in the **Services** area as shown in the screen capture below.

![]({{ site.baseurl }}/assets/images/config/watchdogService.png)

This service provides the following configuration parameters:

- **Watchdog enabled** - sets whether or not this service is enabled or disabled. (Required field.)

- **Watchdog refresh interval** - defines the maximum time interval between two watchdogs' refresh to prevent the system from rebooting. (Required field.)

- **Watchdog device path** - set the device path of the watchdog resource i.e. /dev/watchdog

## Command Service

The CommandService provides methods for running system commands from the web console of a cloud platform. Currently it is supported in Everyware Cloud. In this case, the service also provides the ability for a script to execute using the **File** option of the **Command** tab in the Everyware Cloud Console. This script must be compressed into a zip file with the eventual, associated resource files.

Once the file is selected and **Execute** is clicked, the zip file is sent embedded in an MQTT message on the device. The  Command Service in the device stores the file in /tmp, unzips it, and tries to execute a shell script if one is present in the file. Note that in this case, the Execute parameter cannot be empty; a simple command, such as "ls -l /tmp", may be entered.

To use this service, select the **CommandService** option located in the **Services** area as shown in the screen capture below.

![]({{ site.baseurl }}/assets/images/config/commandService.png)

The CommandService provides the following configuration parameters:

- **command.enable** - sets whether this service is enabled or disabled in the cloud platform. (Required field.)

- **command.password.value** - sets a password to protect this service.

- **command.working.directory** - specifies the working directory where the command execution is performed.

- **command.timeout** - sets the timeout (in seconds) for the command execution.

- **command.environment** - supplies a space-separated list of environment variables in the format key=value.

When a command execution is requested in the cloud platform, it sends an MQTT control message to the device requesting that the command be executed. On the device, the Command Service opens a temporary shell as root in the _command.working.directory,_ sets the _command.environment_ variables (if any), and waits  _command.timeout_ seconds to get command response.
