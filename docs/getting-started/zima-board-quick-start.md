# ZimaBoard/Blade Quick Start

## Overview

## Eclipse Kura&trade; Installation

## Notes

On Ubuntu 24.04 with NetworkManager and DHCP (IPv4), a network interface fails to obtain an IPv4 address automatically. The issue is due to this [bug](https://bugs.launchpad.net/ubuntu/+source/network-manager/+bug/2116733).

To fix it, the `AppArmor` configuration has to be updated as follows:

- edit the `/etc/apparmor.d/sbin.dhclient` file and add the following lines in the main section:

```
  # Added to fix dhclient
  /usr/libexec/nm-dhcp-helper                     Pxrm,
  signal (receive) peer=/usr/libexec/nm-dhcp-helper,
```

- add the following at the end of the file:

```
# Added to fix dhclient
/usr/libexec/nm-dhcp-helper {
  #include <abstractions/base>
  #include <abstractions/dbus>
  /usr/libexec/nm-dhcp-helper mr,

  /run/NetworkManager/private-dhcp rw,
  signal (send) peer=/sbin/dhclient,

  /var/lib/NetworkManager/*lease r,
  signal (receive) peer=/usr/sbin/NetworkManager,
  ptrace (readby) peer=/usr/sbin/NetworkManager,
  network inet dgram,
  network inet6 dgram,
}
```



