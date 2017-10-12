---
layout: page
title:  "Clock Service"
categories: [builtin]
---

The ClockService handles the date and time management of the system. If enabled, it tries to update the system date and time using a Network Time Protocol (NTP) server.

To manage the system date and time, select the **ClockService** option located in the **Services** area as shown in the screen capture below.

![clock_service]({{ site.baseurl }}/assets/images/builtin/clock_service.png)

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