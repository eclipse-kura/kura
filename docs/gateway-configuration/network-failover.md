# Network Failover

For devices configured to use NetworkManager, it is possible to configure multiple WAN interfaces and a network failover basic functionality.

As in picture below, the Kura UI allows defining multiple WAN interfaces. Each WAN interface can be configured with a **WAN Priority** configuration. This priority is used to determine which interface select as primary WAN. In case the primary WAN interface loses connection, then the next highest priority interface is selected.

![](images/net-failover.png)

Kura uses NetworkManager's implementation to achieve network failover (see [NetworkManager](https://www.digi.com/resources/documentation/digidocs/90001548/reference/yocto/r_network_failover.htm?TocPath=Digi%20Embedded%20Yocto%7CSystem%20development%7CSoftware%20extensions%7C_____3)). Lower values correspond to higher priority. Allowed values range from -1 to 2147483647. Value -1 means that the metric is chosen automatically based on the device type (see [NetworkManager DBUS properties](https://developer-old.gnome.org/NetworkManager/unstable/nm-settings-dbus.html)).

To observe changes to the applied configuration, use the following command on your device's shell:

```bash
route -n

Kernel IP routing table
Destination     Gateway         Genmask         Flags Metric Ref    Use Iface
0.0.0.0         192.168.2.1     0.0.0.0         UG    100    0        0 eth0
172.16.1.0      0.0.0.0         255.255.255.0   U     600    0        0 wlan0
172.17.0.0      0.0.0.0         255.255.0.0     U     0      0        0 docker0
192.168.2.0     0.0.0.0         255.255.255.0   U     100    0        0 eth0
```

The `metric` flag will correspond to the set **WAN Priority**.

The default URI used to perform the connectivity check may differ across operating systems. To set a specific URI edit `/etc/NetworkManager/NetworkManager.conf` (reference [NetworkManager](https://www.digi.com/resources/documentation/digidocs/90001548/reference/yocto/r_network_failover.htm?TocPath=Digi%20Embedded%20Yocto%7CSystem%20development%7CSoftware%20extensions%7C_____3)):

```
[connectivity]
uri=http://network-test.debian.org/nm
interval=...
response=...
```