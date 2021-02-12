---
layout: page
title:  "Network Threat Manager"
categories: [config]
---

Kura provides a set of features to detect and prevent network attacks. The **Security** section in the Gateway Administration Console shows the **Network Threat Manager** tab where is it possible to activate and configure these functions.

{% include alerts.html message='The Network Threat Manager tab is not available for the No Network version of Kura.' %}

The following functions are supported:

- Flooding protection

## Flooding protection

The flooding protection function is used to prevent DDos (Distributed Denial-of-Service) attacks using a set of firewall rules. The rules affect the **mangle** firewall table. The following rules are added to the **mangle** table and they are implemented to block invalid or malicious network packets:

```
iptables -A prerouting-kura -m conntrack --ctstate INVALID -j DROP
iptables -A prerouting-kura -p tcp ! --syn -m conntrack --ctstate NEW -j DROP
iptables -A prerouting-kura -p tcp -m conntrack --ctstate NEW -m tcpmss ! --mss 536:65535 -j DROP
iptables -A prerouting-kura -p tcp --tcp-flags FIN,SYN FIN,SYN -j DROP
iptables -A prerouting-kura -p tcp --tcp-flags SYN,RST SYN,RST -j DROP
iptables -A prerouting-kura -p tcp --tcp-flags FIN,RST FIN,RST -j DROP
iptables -A prerouting-kura -p tcp --tcp-flags FIN,ACK FIN -j DROP
iptables -A prerouting-kura -p tcp --tcp-flags ACK,URG URG -j DROP
iptables -A prerouting-kura -p tcp --tcp-flags ACK,FIN FIN -j DROP
iptables -A prerouting-kura -p tcp --tcp-flags ACK,PSH PSH -j DROP
iptables -A prerouting-kura -p tcp --tcp-flags ALL ALL -j DROP
iptables -A prerouting-kura -p tcp --tcp-flags ALL NONE -j DROP
iptables -A prerouting-kura -p tcp --tcp-flags ALL FIN,PSH,URG -j DROP
iptables -A prerouting-kura -p tcp --tcp-flags ALL SYN,FIN,PSH,URG -j DROP
iptables -A prerouting-kura -p tcp --tcp-flags ALL SYN,RST,ACK,FIN,URG -j DROP
iptables -A prerouting-kura -p icmp -j DROP", "-A prerouting-kura -f -j DROP
```

The following parameters are available:

- *flooding.protection.enabled* : enables the application of the firewall rules for flooding protection.

