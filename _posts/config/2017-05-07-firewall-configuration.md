---
layout: page
title:  "Firewall Configuration"
categories: [config]
---

Kura offers easy management of the Linux firewall iptables included in an IoT Gateway. Additionally, Kura provides the ability to manage network access security to an IoT Gateway through the following:

- Open Ports (local service rules)

- Port Forwarding

- IP Forwarding and Masquerading (NAT service rules)

- 'Automatic' NAT service rules

Open Ports, Port Forwarding, and IP Forwarding and Masquerading are configured via respective **Firewall** configuration tabs. 'Automatic' NAT is enabled for each local (LAN) interface using the **DHCP & NAT** tab of the respective interface configuration.

## Firewall Linux Configuration

This section describes the changes applied by Kura at the Linux networking configuration. Please read the following note before proceeding with manual changes of the Linux networking configuration.

{% include alerts.html message='It is NOT recommended performing manual editing of the Linux networking configuration files when the gateway configuration is being managed through Kura. While Linux may correctly accept manual changes, Kura may not be able to interpret the new configuration resulting in an inconsistent state.' %}

When a new firewall configuration is submitted, the iptables command is executed accordingly the desired configuration and the rules are saved into the /etc/sysconfig/iptables file.

## Open Ports

If Kura is running on a gateway, all TCP/UDP ports are closed by default unless custom rules are added to the /etc/sysconfig/iptables file. . Therefore, if a user needs to connect to a specific port on a gateway, it is insufficient to have an application listening on the desired port; the port also needs to be opened in the firewall.

To open a port using the Administration Console, select the **Firewall** option located in the **System** area. The **Firewall** configuration display appears in the main window. With the **Open Ports** tab selected, click the **New** button. The **New Open Port Entry** form appears.

The **New Open Port Entry** form contains the following configuration parameters:

- **Port or Port Range** - specifies the port or port range (<high>:<low>) to be opened. (Required field.)

- **Protocol** - defines the protocol (tcp or udp). (Required field.)

- **Permitted Network** - only allows packets originated by a host on this network in CIDR notation (e.g. 172.16.1.0/24)

- **Permitted Interface Name** - only allows packets arrived on this interface.

- **Unpermitted Interface Name** - blocks packets arrived on this interface.

- **Permitted MAC Address** - only allows packets originated by this host in the format XX:XX:XX:XX:XX:XX.

- **Source Port Range** - only allows packets with source port in the defined range (<high>:<low>).

Complete the **New Open Port Entry** form and click the **Submit** button when finished. Once the form is submitted, a new port entry will appear. Click the **Apply** button for the change to take effect.

![]({{ site.baseurl }}/assets/images/config/NetFirewall.png)

## Port Forwarding

Port forwarding rules are needed to establish connectivity from the WAN side to a specific port on a host that resides on a LAN behind the gateway. In this case, a routing solution may be avoided since the connection is made to a specified _external_ port on a gateway, and packets are forwarded to an _internal_ port on the destination host; therefore, it is not necessary to add the _external_ port to the list of open ports.

To add a port forwarding rule, select the **Port Forwarding** tab on the **Firewall** display and click the **New** button. The **Port Forward Entry** form appears.

The **Port Forward Entry** form contains the following configuration parameters:

- **Input Interface** - specifies the interface through which a packet is going to be received. (Required field.)

- **Output Interface** - specifies the interface through which a packet is going to be forwarded to its destination. (Required field.)

- **LAN Address** - supplies the IP address of destination host. (Required field.)

- **Protocol** - defines the protocol (tcp or udp). (Required field.)

- **External Port** - provides the external destination port on gateway unit. (Required field.)

- **Internal Port** - provides the port on a destination host. (Required field.)

- **Enable Masquerading** - defines whether masquerading is used (yes or no). If enabled, the gateway replaces the IP address of the originating host with the IP address of its own output (LAN) interface. This is needed when the destination host does not have a back route to the originating host (or default gateway route) via the gateway unit. The masquerading option is provided with port forwarding to limit gateway forwarding only to the destination port. (Required field.)

- **Permitted Network** - only forwards if the packet is originated from a host on this network.

- **Permitted MAC Address** - only forwards if the packet is originated by this host.

- **Source Port Range** - only forwards if the packet's source port is within the defined range.

Complete the **Port Forward Entry** form and click the **Apply** button for the desired port forwarding rules to take effect.

### Port Forwarding example

This section describes an example on port forwarding rules. The initial setup is described below.

- A couple of RaspberryPi 3 or Beaglebone that shares the same LAN over Ethernet.

- The first RaspberryPi running Kura is configured as follows:
  - The eth0 interface static with IP address of 172.16.0.5.
  - There is no default gateway.

- The second RaspberryPi running Kura is configured as follows:
  - The eth0 interface LAN/static with IP address of 172.16.0.1/24 and no NAT.
  - The wlan0 interface is WAN/DHCP client.

- A laptop is connected to the same network of the wlan0 of the second RaspberryPi and can ping its wlan0 interface.

The purpose of the second RaspberryPi configuration is to enable access to the Administration Console running on the first one (port 80) by connecting to the second RaspberryPi's port 8080 over the wlan. This scenario assumes that IP addresses are assigned as follows:

- Second RaspberryPi wlan0 - 10.200.12.6

- Laptop wlan0 - 10.200.12.10

The following port forwarding entries are added to the second RaspberryPi configuration as described above using the **Port Forward Entry** form:

- Input Interface - wlan0

- Output Interface - eth0

- LAN Address - 172.16.0.5

- Protocol - tcp

- External Port - 8080

- Internal Port - 80

- Masquerade - yes

The _Permitted Network_, _Permited MAC Address_, and _Source Port Range_ fields are left blank.

The following iptables rules are added to the /etc/sysconfig/iptables file:

```
-A FORWARD -d 172.16.0.5/32 -i wlan0 -o eth0 -p tcp -m tcp -j ACCEPT
-A FORWARD -s 172.16.0.5/32 -i eth0 -o wlan0 -p tcp -m state --state RELATED,ESTABLISHED -j ACCEPT
-A PREROUTING -i wlan0 -p tcp -m tcp --dport 8080 -j DNAT --to-destination 172.16.0.5:80
-A POSTROUTING -d 172.16.0.5/32 -o eth0 -p tcp -j MASQUERADE
```

The following iptables commands may be used to verify that the new rules have been applied:

```
sudo iptables -v -n -L
sudo iptables -v -n -L -t nat
```

At this point, it is possible to try to connect to  http://10.200.12.6 and to http://10.200.12.6:8080 from the laptop. Note that when a connection is made to the device on port 80, it is to the Kura configuration page on the device itself (the second RaspberryPi). When the gateway is connected on port 8080, you are forwarded to the Kura Gateway Administration Console on the first RaspberryPi. The destination host can only be reached by connecting to the gateway on port 8080.

Another way to connect to the Kura Gateway Administration Console on the first RaspberryPi would be to add an IP Forwarding/Masquerading entry as described in the next section. In this case, the following additional iptables rules need to be added to the system:

```
#custom port forward service rules
sudo iptables -t nat -A PREROUTING -i wlan0 -p tcp -s 0.0.0.0/0 --dport 8080 -j DNAT --to-destination 172.16.0.5:80
sudo iptables -A FORWARD -i wlan0 -o eth0 -p tcp -s 0.0.0.0/0 --dport 80 -d 172.16.0.5 -j ACCEPT
sudo iptables -A FORWARD -i eth0 -o wlan0 -p tcp -s 172.16.0.5 -m state --state RELATED,ESTABLISHED -j ACCEPT

#custom automatic NAT service rules (if NAT option is enabled for LAN interface)

#custom NAT service rules
sudo iptables -t nat -A POSTROUTING -p tcp -s 0.0.0.0/0 -d 172.16.0.5/32 -o eth0 -j MASQUERADE
sudo iptables -A FORWARD -p tcp -s 172.16.0.5/32 -d 0.0.0.0/0 -i eth0 -o wlan0 -m state --state RELATED,ESTABLISHED -j ACCEPT
sudo iptables -A FORWARD -p tcp -s 0.0.0.0/0 -d 172.16.0.5/32 -i wlan0 -o eth0 -j ACCEPT
```

This solution is less desirable for the following reasons:

- The IP Forwarding solution is no longer self-contained.

- Extra FORWARDING rules are defined.

- It allows the gateway to forward/masquerade all traffic to the destination host; therefore, if a connecting laptop has a route to the destination host, it provides a way to ssh to the RaspberryPi as well and may present a security problem.

## IP Forwarding/Masquerading

The advantage of the _Automatic NAT_ method is its simplicity. However, this approach does not handle reverse NATing, and it cannot be used for interfaces that are not listed in the Gateway Administration Console. To set up generic (one-to-many) NATing, select the **IP Forwarding/Masquerading** tab on the **Firewall** display. The **IP Forwarding/Masquerading** form appears.

The **IP Forwarding/Masquerading** form contains the following configuration parameters:

- **Input Interface** - specifies the interface through which a packet is going to be received. (Required field.)

- **Output Interface** - specifies the interface through which a packet is going to be forwarded to its destination. (Required field.)

- **Protocol** - defines the protocol of the rule to check (all, tcp, or udp). (Required field.)

- **Source Network/Host** - identifies the source network or host name (CIDR notation). Set to 0.0.0.0/0 if empty.

- **Destination Network/Host** - identifies the destination network or host name (CIDR notation). Set to 0.0.0.0/0 if empty.

- **Enable Masquerading** - defines whether masquerading is used (yes or no). If set to 'yes', masquerading is enabled. If set to 'no', only FORWARDING rules are be added. (Required field.)

As a use-case scenario, consider the same setup as in port forwarding. In this case, the interfaces of the gateway are configured as follows:

- eth0 - LAN/Static/No NAT 172.16.0.1/24

- wlan0 - WAN/DHCP 10.200.12.6/24

To reach the RaspberryPi unit sitting on the 172.16.0.5/24 from a specific host on the 10.200.0.0/16 network, set up the following _Reverse NAT_ entry:

- Input Interface - wlan0 (WAN interface)

- Output Interface - eth0 (LAN interface)

- Protocol - tcp

- Source Network/Host - 10.200.12.10/32

- Destination Network/Host - 172.16.0.5/32

- Enable Masquerading - yes

This case adds the following iptables rules to the /etc/sysconfig/iptables file:

```
#custom NAT service rules
-A POSTROUTING -p tcp -s 10.200.12.10/32 -d 172.16.0.5/32 -o eth0 -j MASQUERADE
-A FORWARD -p tcp -s 172.16.0.5/32 -i eth0 -o wlan0 -m state --state RELATED,ESTABLISHED -j ACCEPT
-A FORWARD -p tcp -s 10.200.12.10/32 -d 172.16.0.5/32 -i wlan0 -o eth0 -j ACCEPT
```

Additionally, a route to the 172.16.0.0/24 network needs to be configured on a connecting laptop as shown below:

```
sudo route add -net 172.16.0.0 netmask 255.255.255.0 gw 10.200.12.6
```

Since _masquerading_ is enabled, there is no need to specify the back route on the destination host. Note that with this setup, the RaspberryPi only forwards packets originating on the 10.200.12.10 laptop to the 172.16.0.5 destination.

If the _Source Network/Host_ and _Destination Network/Host_ fields are empty, iptables rules appear as follows:

```
#custom NAT service rules
-A POSTROUTING -p tcp -s 0.0.0.0/0 -d 0.0.0.0/0 -o eth0 -j MASQUERADE
-A FORWARD -p tcp -s 0.0.0.0/0 -d 0.0.0.0/0 -i eth0 -o wlan0 -m state --state RELATED,ESTABLISHED -j ACCEPT
-A FORWARD -p tcp -s 0.0.0.0/0 -d 0.0.0.0/0 -i wlan0 -o eth0 -j ACCEPT
```

The RaspberryPi forwards packets from any external host (connected to wlan0) to any destination on the local network (eth0 interface).
