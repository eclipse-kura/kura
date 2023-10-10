# Firewall Configuration

Kura offers easy management of the Linux firewall iptables and ip6tables included in an IoT Gateway. Additionally, Kura provides the ability to manage network access security to an IoT Gateway for both IPv4 and IPv6 through the following:

- Open Ports (local service rules)
- Port Forwarding
- IP Forwarding and Masquerading (NAT service rules)
- 'Automatic' NAT service rules

Open Ports, Port Forwarding, and IP Forwarding and Masquerading are configured via respective **Firewall** configuration tabs. 'Automatic' NAT is enabled for each local (LAN) interface using the **DHCP & NAT** tab of the respective interface configuration.
While the IPv4 Firewall configuration capability is present on all IoT Gateways, the IPv6 Firewall is present only on devices that support IPv6 Networking.

## Firewall Linux Configuration

This section describes the changes applied by Kura at the Linux networking configuration. Please read the following note before proceeding with manual changes of the Linux networking configuration.

!!! warning
    It is **NOT** recommended performing manual editing of the Linux networking configuration files when the gateway configuration is being managed through Kura. While Linux may correctly accept manual changes, Kura may not be able to interpret the new configuration resulting in an inconsistent state.

When a new firewall configuration is submitted, Kura immediately applies it using the iptables service provided by the OS. Moreover, the rules are stored in the filesystem and a new Kura snapshot is generated containing the new configuration. At the next startup, the **firewall** service in the OS will re-apply them and Kura will check the firewall configuration against the one contained in the last snapshot. In this way, the user can update the snapshot with the needed rules and apply them to the system using the webUI or modify the `snapshot_0.xml` before the first start of Kura.

In order to allow a better coexistence between Kura and external applications that need to modify firewall rules, Kura writes its rules to a set of custom iptables chains. They are *input-kura*, *output-kura*, *forward-kura*, *forward-kura-pf* and *forward-kura-ipf* for the filter table and *input-kura*, *output-kura*, *prerouting-kura*, *prerouting-kura-pf*, *postrouting-kura*, *postrouting-kura-pf* and *postrouting-kura-ipf* for the nat table. The custom chains are then put in their respective standard iptables chains, as shown in the following:

```shell
iptables -t filter -I INPUT -j input-kura
iptables -t filter -I OUTPUT -j output-kura
iptables -t filter -I FORWARD -j forward-kura
iptables -t filter -I forward-kura -j forward-kura-pf
iptables -t filter -I forward-kura -j forward-kura-ipf
iptables -t nat -I PREROUTING -j prerouting-kura
iptables -t nat -I prerouting-kura -j prerouting-kura-pf
iptables -t nat -I INPUT -j input-kura
iptables -t nat -I OUTPUT -j output-kura
iptables -t nat -I POSTROUTING -j postrouting-kura
iptables -t nat -I postrouting-kura -j postrouting-kura-pf
iptables -t nat -I postrouting-kura -j postrouting-kura-ipf
```

The same custom chains are used in the IPv6 case.

Even if many firewall rules can be handled by Kura, it could be that some rules cannot be filled through the Web Console. In this case, custom firewall rules may be added to the /etc/init.d/firewall_cust script manually. These rules are applied/reapplied every time the **firewall** service starts, that is at the gateway startup. These custom rules should **not** be applied to the Kura custom chains, but to the standard ones.

## Open Ports

If Kura is running on a gateway, all TCP/UDP ports are closed by default unless they are listed in the **Open Ports IPv4** or **Open Ports IPv6** tab of the **Firewall** section in the Gateway Administration Console, or in the `/etc/sysconfig/iptables` script. Therefore, if a user needs to connect to a specific port on a gateway, it is insufficient to have an application listening on the desired port; the port also needs to be opened in the firewall.

To open a port using the Gateway Administration Console, select the **Firewall** option located in the **System** area. The **Firewall** configuration display appears in the main window. With the **Open Ports IPv4** or **Open Ports IPV6** tab selected, click the **New** button. The **New Open Port Entry** form appears.

The **New Open Port Entry** form contains the following configuration parameters:

- **Port**: specifies the port to be opened. (Required field.)
- **Protocol**: defines the protocol (tcp or udp). (Required field.)
- **Permitted Network**: only allows packets originated by a host on this network.
- **Permitted Interface Name**: only allows packets arrived on this interface.
- **Unpermitted Interface Name**: blocks packets arrived on this interface.
- **Permitted MAC Address**: only allows packets originated by this host.
- **Source Port Range**: only allows packets with source port in the defined range.

Complete the **New Open Port Entry** form and click the **Submit** button when finished. Once the form is submitted, a new port entry will appear. Click the **Apply** button for the change to take effect.

The firewall rules related to the open ports section are stored in the *input-kura* custom chain of the IPv4/IPv6 filter table.

![Firewall Open Ports](./images/firewall-open-ports.png)

## Port Forwarding

Port forwarding rules are needed to establish connectivity from the WAN side to a specific port on a host that resides on a LAN behind the gateway. In this case, a routing solution may be avoided since the connection is made to a specified _external_ port on a gateway, and packets are forwarded to an _internal_ port on the destination host; therefore, it is not necessary to add the _external_ port to the list of open ports.

To add a port forwarding rule, select the **Port Forwarding IPv4** or **Port Forwarding IPv6** tab on the **Firewall** display and click the **New** button. The **Port Forward Entry** form appears.

The **Port Forward Entry** form contains the following configuration parameters:

- **Input Interface**: specifies the interface through which a packet is going to be received. (Required field).

- **Output Interface**: specifies the interface through which a packet is going to be forwarded to its destination. (Required field).

- **LAN Address**: supplies the IP address of destination host. (Required field).

- **Protocol**: defines the protocol (tcp or udp). (Required field).

- **External Port**: provides the external destination port on gateway unit. (Required field).

- **Internal Port**: provides the port on a destination host. (Required field).

- **Enable Masquerading**: defines whether masquerading is used (yes or no). If enabled, the gateway replaces the IP address of the originating host with the IP address of its own output (LAN) interface. This is needed when the destination host does not have a back route to the originating host (or default gateway route) via the gateway unit. The masquerading option is provided with port forwarding to limit gateway forwarding only to the destination port. (Required field).

- **Permitted Network**: only forwards if the packet is originated from a host on this network.

- **Permitted MAC Address**: only forwards if the packet is originated by this host.

- **Source Port Range**: only forwards if the packet's source port is within the defined range.

Complete the **Port Forward Entry** form and click the **Apply** button for the desired port forwarding rules to take effect.

The firewall rules related to the port forwarding section are stored in the *forward-kura-pf* custom chain of the IPv4/IPv6 filter table and in the *postrouting-kura-pf* and *prerouting-kura-pf* chains of the IPv4/IPv6  nat table.

### Port Forwarding example

This section describes an example of port forwarding rules applied to the IPv4 case. The initial setup is described below.

- A couple of RaspberryPi that shares the same LAN over Ethernet.

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

The _Permitted Network_, _Permitted MAC Address_, and _Source Port Range_ fields are left blank.

The following iptables rules are applied and added to the /etc/sysconfig/iptables file:

```shell
iptables -t nat -A prerouting-kura-pf -i wlan0 -p tcp -s 0.0.0.0/0 --dport 8080 -j DNAT --to 172.16.0.5:80
iptables -t nat -A postrouting-kura-pf -o eth0 -p tcp -d 172.16.0.5 -j MASQUERADE
iptables -A forward-kura-pf -i wlan0 -o eth0 -p tcp -s 0.0.0.0/0 --dport 80 -d 172.16.0.5 -j ACCEPT
iptables -A forward-kura-pf -i eth0 -o wlan0 -p tcp -s 172.16.0.5 -m state --state RELATED,ESTABLISHED -j ACCEPT
```

The following iptables commands may be used to verify that the new rules have been applied:

```shell
sudo iptables -v -n -L
sudo iptables -v -n -L -t nat
```

At this point, it is possible to try to connect to  http://10.200.12.6 and to http://10.200.12.6:8080 from the laptop. Note that when a connection is made to the device on port 80, it is to the Kura configuration page on the device itself (the second RaspberryPi). When the gateway is connected on port 8080, you are forwarded to the Kura Gateway Administration Console on the first RaspberryPi. The destination host can only be reached by connecting to the gateway on port 8080.

Another way to connect to the Kura Gateway Administration Console on the first RaspberryPi would be to add an IP Forwarding/Masquerading entry as described in the next section.

## IP Forwarding/Masquerading

The advantage of the _Automatic NAT_ method is its simplicity. However, this approach does not handle reverse NATing, and it cannot be used for interfaces that are not listed in the Gateway Administration Console (such as VPN tun0 interface). To set up generic (one-to-many) NATing, select the **IP Forwarding/Masquerading IPv4** or **IP Forwarding/Masquerading IPv6** tab on the **Firewall** display. Press the **New** button and the **IP Forwarding/Masquerading** form appears.

The **IP Forwarding/Masquerading** form contains the following configuration parameters:

- **Input Interface**: specifies the interface through which a packet is going to be received. (Required field).

- **Output Interface**: specifies the interface through which a packet is going to be forwarded to its destination. (Required field).

- **Protocol**: defines the protocol of the rule to check (all, tcp, or udp). (Required field).

- **Source Network/Host**: identifies the source network or host name (CIDR notation). Set to IPv4 0.0.0.0/0 or IPv6 ::/0 if empty.

- **Destination Network/Host**: identifies the destination network or host name (CIDR notation). Set to IPv4 0.0.0.0/0 or IPv6 ::/0 if empty.

- **Enable Masquerading**: defines whether masquerading is used (yes or no). If set to 'yes', masquerading is enabled. If set to 'no', only FORWARDING rules are be added. (Required field).

The rules will be added to the *forward-kura-ipf* chain in the IPv4/IPv6 filter table and in the *postrouting-kura-ipf* one in the IPv4/IPv6 nat table.

As a use-case scenario, consider the same setup as in port forwarding, but with cellular interface disabled and eth1 interface configured as WAN/DHCP client. In this case, the interfaces of the gateway are configured as follows:

- `eth0`: LAN/Static/No NAT 172.16.0.1/24

- `eth1`: WAN/DHCP 10.11.5.4/24

To reach the gateway sitting on the 172.16.0.5/24 from a specific host on the 10.11.0.0/16 network, set up the following _Reverse NAT_ entry:

- Input Interface: eth1 (WAN interface)

- Output Interface: eth0 (LAN interface)

- Protocol: all

- Source Network/Host: 10.11.5.21/32

- Destination Network/Host: 172.16.0.5/32

- Enable Masquerading: yes

This case applies the following iptables rules:

```shell
iptables -t nat -A postrouting-kura-ipf -p tcp -s 10.11.5.21/32 -d 172.16.0.5/32 -o eth0 -j MASQUERADE
iptables -A forward-kura-ipf -p tcp -s 172.16.0.5/32 -i eth0 -o eth1 -m state --state RELATED,ESTABLISHED -j ACCEPT
iptables -A forward-kura-ipf -p tcp -s 10.11.5.21/32 -d 172.16.0.5/32 -i eth1 -o eth0 -m tcp -j ACCEPT
```

Additionally, a route to the 172.16.0.0/24 network needs to be configured on a connecting laptop as shown below:

```shell
sudo route add -net 172.16.0.0 netmask 255.255.255.0 gw 10.11.5.4
```

Since _masquerading_ is enabled, there is no need to specify the back route on the destination host. Note that with this setup, the gateway only forwards packets originating on the 10.11.5.21 laptop to the 172.16.0.5 destination.

If the _Source Network/Host_ and _Destination Network/Host_ fields are empty, iptables rules appear as follows:

```shell
iptables -t nat -A postrouting-kura-ipf -p tcp -s 0.0.0.0/0 -d 0.0.0.0/0 -o eth0 -j MASQUERADE
iptables -A forward-kura-ipf -p tcp -s 0.0.0.0/0 -i eth0 -o eth1 -m state --state RELATED,ESTABLISHED -j ACCEPT
iptables -A forward-kura-ipf -p tcp -s 0.0.0.0/0 -d 0.0.0.0/0 -i eth1 -o eth0 -j ACCEPT
```

The gateway forwards packets from any external host (connected to eth1) to any destination on the local network (eth0 interface).