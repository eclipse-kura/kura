# ZimaBoard/Blade Quick Start

## Overview

TBD

## Eclipse Kura&trade; Installation

TBD

## Known Issues

### `dhclient` apparmor profile

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

### `named` failing to start


On Ubuntu 24.04 with NetworkManager and DHCP (IPv4) you might see the following error in the Kura logs:

```
2026-06-19T10:00:09,741 [DnsMonitorServiceImpl] WARN  o.e.k.n.c.m.DnsServerMonitor - Command 'Failed to start named' exited with code {1}.
org.eclipse.kura.KuraException: Command 'Failed to start named' exited with code {1}.
	at org.eclipse.kura.linux.net.dns.LinuxDnsServer.start(LinuxDnsServer.java:181)
	at org.eclipse.kura.nm.configuration.monitor.DnsServerMonitor.reconfigureDNSProxy(DnsServerMonitor.java:254)
	at org.eclipse.kura.nm.configuration.monitor.DnsServerMonitor.manageDnsProxies(DnsServerMonitor.java:229)
	at org.eclipse.kura.nm.configuration.monitor.DnsServerMonitor.monitorDnsServerStatus(DnsServerMonitor.java:133)
	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:539)
	at java.base/java.util.concurrent.FutureTask.runAndReset(FutureTask.java:305)
	at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:305)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
	at java.base/java.lang.Thread.run(Thread.java:840)
```

To fix it, the `AppArmor` configuration has to be updated as follows:

- edit the `/etc/apparmor.d/local/usr.sbin.named` file and add the following lines in the main section:

```
# Site-local additions for /usr/sbin/named.
# This file is included by /etc/apparmor.d/usr.sbin.named.

# Allow BIND/named worker threads to set/read their thread names.
owner /proc/*/task/*/comm rw,

# Allow BIND/named to read the kernel ephemeral port range.
@{PROC}/sys/net/ipv4/ip_local_port_range r,

# Allow BIND/named to notify systemd that it is ready / running.
@{run}/systemd/notify w,

# Allow BIND/named to read Ubuntu kernel version metadata.
@{PROC}/version_signature r,

# Only enable this if named still fails to start/run without it.
# capability sys_admin,
```
