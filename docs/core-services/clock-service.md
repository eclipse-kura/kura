# Clock Service

The ClockService handles the date and time management of the system. If enabled, it tries to update the system date and time using a Network Time Protocol (NTP) server. NTP can use NTS as authentication mechanism through chrony.



## Service Configuration

To manage the system date and time, select the **ClockService** option located in the **Services** area as shown in the screen capture below.

![Clock Service](./images/clock-service.png)

The ClockService provides the following configuration parameters:

- **enabled**: sets whether or not this service is enabled or disabled (Required field).

- **clock.set.hwclock**: defines if the hardware clock of the gateway must be synced after the system time is set. If enabled, the service calls the Linux command `hwclock --utc --systohc`.

- **clock.provider**: specifies one among Java NTP client (java-ntp), Linux chrony command (chrony-advanced), Linux ntpdate command (ntpd) (Required field). If chrony-advanced is used, Kura will not change system and/or hardware clock directly, delegating these operations to chrony.

- **clock.ntp.host**: sets a valid NTP server host address.

- **clock.ntp.port**: sets a valid NTP port number.

- **clock.ntp.timeout**: specifies the NTP timeout in milliseconds.

- **clock.ntp.max-retry**: defines the number of retries when a sync fails (retry at every minute). Subsequently, the next retry occurs on the next refresh interval.

- **clock.ntp.retry.interval**: defines the interval in seconds between each retry when a sync fails. If the _clock.ntp.refresh-interval_ parameter is less than zero, there is no update. If the _clock.ntp.refresh-interval_ parameter is equal to zero, there is only one try at startup (Required field).

- **clock.ntp.refresh-interval**: defines the frequency (in seconds) at which the service tries to sync the clock. Note that at the start of Kura, when the ClockService is enabled, it tries to sync the clock every minute until it is successful. After a successful sync, this operation is performed at the frequency defined by this parameter. If the value is less than zero, there is no update. If the value is equal to zero, syncs only once at startup.

- **chrony.advanced.config**: specifies the content of the chrony configuration file. If this field is left blank, the default system configuration will be used. 
To obtain the hardware clock synchronization  the directive rtcsync could be used. The rtcsync directive provides the hardware clock synchronization  made by the linux kernel every 11 minutes. For further information reference the [chrony website](https://chrony.tuxfamily.org/doc/4.1/chrony.conf.html).

Two example configuration are shown below.



## NTS Secure [configuration example](https://fedoramagazine.org/secure-ntp-with-nts)

```shell
server time.cloudflare.com iburst nts
server nts.sth1.ntp.se iburst nts
server nts.sth2.ntp.se iburst nts

sourcedir /etc/chrony/sources.d

driftfile /var/lib/chrony/chrony.drift

logdir /var/log/chrony

maxupdateskew 100.0

rtcsync

makestep 1 -1

leapsectz right/UTC
```

!!! note
    If the system stays disconnected from the network for a long time or if the backup battery is not working properly or is depleted, there is the possibility of a synchronization failure due to the client inability to verify the server certificates. 

    If this happens and no counter action has been taken in the chrony configuration file, the risk is that the gateway will be unable to synchronise again its date and therefore will not be able to connect to the cloud and/or be fully operational.

    A possible way to prevent this issue is to [temporary disable the certificate verification](https://chrony.tuxfamily.org/faq.html#_using_nts) using the directive nocerttimecheck. This directory will **disable the security checks of the activation and expiration times of certificates for the specified number of clock updates** and should be **used with caution** due to the **important security implications**.

As reported by the official [Chrony documentation](https://chrony.tuxfamily.org/doc/devel/chrony.conf.html), disabling the time checks has important security implications and should be used only as a last resort, preferably with a minimal number of trusted certificates. The default value is 0, which means the time checks are always enabled.

An example of the directive is `nocerttimecheck 1`
This would disable the time checks until the clock is updated for the first time, assuming the first update corrects the clock and later checks can work with correct time.



## Simple [configuration example](https://git.tuxfamily.org/chrony/chrony.git/tree/examples/chrony.conf.example1)

```shell
# Use public NTP servers from the pool.ntp.org project.
pool pool.ntp.org iburst

# Record the rate at which the system clock gains/losses time.
driftfile /var/lib/chrony/drift

# Allow the system clock to be stepped in the first three updates
# if its offset is larger than 1 second.
makestep 1 -1

# Enable kernel synchronization of the real-time clock (RTC).
rtcsync
```