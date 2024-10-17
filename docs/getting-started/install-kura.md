# Install Kura

Eclipse Kura&trade; is provided using a Debian Linux package. Visit the [Kura download page](https://www.eclipse.org/kura/downloads.php) to find the correct installation file for the target system.



## Installer types

Several installers can be found on such page, and they fall into one of the following categories:

1. standard installers, like `kura-6.0.0_generic-arm32_installer.deb`; and
2. installers with suffix **nn**, like `kura-6.0.0_generic-arm32-nn_installer.deb`

Profiles of types (1) ship a Kura version with networking functionalities. In particular, they can be installed on targets with [NetworkManager](https://networkmanager.dev), a commonly available tool for managing Linux networking. Kura leverages this tool for networking functionalities. Refer to the [Kura installers](#kura-installers) section for further information.

Installers of type (2) with the suffix `nn` are **No Networking** profiles that do not bundle the Kura Network Manager: all the network configurations need to be done outside of Kura. Functionalities **missing** in **NN profiles** compared to the full Kura profiles:

- Networking interfaces management
- Firewall configuration management
- Network Threat management



## Kura installers

A user can deploy Kura on a target system using the installer tailored for the device architecture. The installer file looks like:

```
kura-<kura-version>_generic-<arch>_installer.deb/rpm
```

where `<arch>` is one of the **supported architectures**: *x86_64*, *arm32*, and *arm64*. Kura can work on systems that have available the dependencies listed in the [Kura dependencies](#kura-dependencies) section, and that have **at least one** physical ethernet interface.

### Java Heap Memory Assignment
The Eclipse Kura&trade;'s installer incorporates an adaptive Heap Memory allocation system during installation. The allocation follows a formula based on your gateway's available memory. If your gateway has less than 1024MB of RAM, Kura will set -Xms and -Xmx to 256MB. For gateways with more than 1024MB, one quarter of the total RAM will be assigned to -Xms and -Xmx.

### Initial network configuration

During the installation of Eclipse Kura with network management support, the initial network configuration will be generated dynamically. The existing wired and wireless network interface names are detected and sorted in ascending lexicographic order at the installation.

If only one ethernet interface is detected (e.g. eth0), it will be configured as follows:

| Interface | Configuration |
|-----------|---------------|
| Ethernet interface (e.g. eth0) | - **Status**: `Enabled for WAN`<br>- **Configure**: `Using DHCP`|

If multiple ethernet interfaces are detected, instead, the first two interfaces will be configured as presented in the table below. The other ethernet interfaces will be disabled.

| Interface | Configuration |
|-----------|---------------|
| First Ethernet interface (e.g. eth0) | - **Status**: `Enabled for LAN`<br>- **Configure**: `Manually`<br>- **IP address**: `172.16.0.1`<br>- **Router Mode**: DHCP and NAT|
| Second Ethernet interface (e.g. eth1) | - **Status**: `Enabled for WAN`<br>- **Configure**: `Using DHCP`|

Finally, if a wireless interface (e.g. wlan0) is detected, it will configured as shown below. The other wireless interfaces will be disabled.

| Interface | Configuration |
|-----------|---------------|
| Wireless interface (e.g. wlan0) | - **Status**: `Enabled for LAN`<br>- **Configure**: `Manually`<br>- **IP address**: `172.16.1.1`<br>- **Passphrase**: `testKEYS`<br>- **Router Mode**: DHCP and NAT |

For example, if the system contains the following interfaces: `wlp2s0`, `wlp3s0`, `enp3s0`, `eno1`, `ens2`; then `eno1` will be
enabled for LAN with a DHCP server, `enp3s0` will be enabled for WAN in DHCP client mode, `wlp2s0` will be configured as an AP, and all other network interfaces will be disabled.

!!! warning
    On systems that do not use systemd's predictable interface naming scheme (see [Freedesktop reference](https://www.freedesktop.org/wiki/Software/systemd/PredictableNetworkInterfaceNames/)) the primary network interface name might change whenever a re-enumeration is triggered (for example, after a reboot or after plugging in an external network adapter).

    The advice is to install Kura on systems that use a reliable naming convention for network interfaces.

    Systemd consistent network interface naming assigns the name prefix based on the physical location of the device, see [Understanding the Predictable Network Interface Device Names](https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/7/html/networking_guide/sec-understanding_the_predictable_network_interface_device_names) for further reference.

### Initial firewall configuration

Similarly to the initial network configuration, the initial firewall setup is adapted based on the network interface detected on the system. In case of multiple ethernet and wireless interfaces, the configuration will be as shown in the screenshot below.

![](./images/firewall-generic.png)

If the wireless interface is not present, the firewall entries for the `wlan0` are dropped.

Please note that installing Eclipse Kura with network configuration support will replace the current network and firewall configuration with the one shown above.

### Other Kura services

Eclipse Kura&trade; do not contain gateway specific customizations, this implies that the values of some configuration parameters may be incorrect and/or missing and must be manually filled after installation. For example the user might want to:

- Configure the other network interfaces, if any.
- Setup additional firewall rules.
- Edit the `/opt/eclipse/kura/framework/jdk.dio.properties` with the correct GPIO mappings. By default this file is empty.

### Kura dependencies

To have all the Kura features working, the following dependencies are required:

- General: `setserial`, `zip`, `gzip`, `unzip`, `procps`, `usbutils`, `socat`, `gawk`, `sed`, `inetutils-telnet`.
- Security: `polkit` or `policykit-1`, `ssh` or `openssh`, `openssl`, `busybox`, `openvpn`.
- Bluetooth: `bluez` or `bluez5`, `bluez-hcidump` or `bluez5-noinst-tools`.
- Time: `ntpdate`, `chrony`, `chronyc`, `cron` or `cronie`.
- Networking: `network-manager` or `networkmanager`, `bind9` or `bind`, `dnsmasq` or `isc-dhcp-server` or (`dhcp-server` and `dhcp-client`), `iw`, `iptables`, `modemmanager`, `hostapd`, `wpa-supplicant`, `ppp`, `iproute2`.
- Logs: `logrotate`.
- Gps: `gpsd`.
- Python: `python3`.
- Java: `openjdk-17-jre-headless` or `temurin-17-jdk` or `openjdk-8-jre-headless` or `temurin-8-jdk`.
- Others: `dos2unix`

### Reference devices

Eclipse Kura&trade; has been tested on the following devices and provides full configuration of all the available interfaces and GPIO mappings.

| Device | Architecture | OS |
| - | - | - |
| **Raspberry Pi 3/4** | *arm32* | Raspberry Pi OS "Bookworm" |
| **Raspberry Pi 3/4** | *arm64* | Raspberry Pi OS "Bookworm" |
| **Raspberry Pi 3/4** | *arm64* | Ubuntu 20.04 |
| **ZimaBoard/Blade** | *x86_64* | TBD |
| **NVIDIA Jetson AGX Orin&trade;** | *arm64* | TBD |

Check out the quick start guides for the detailed installation steps and set-up procedures:

- [Raspberry Pi - Raspberry Pi OS Quick Start](./raspberry-pi-raspberryos-quick-start.md)
- [Raspberry Pi - Ubuntu 20 Quick Start](./raspberry-pi-ubuntu-20-quick-start.md)
- [Docker Quick Start](./docker-quick-start.md)
- [Nvidia Jetson AGX Orin Quick Start](./nvidia-jetson-orin-quick-start.md)
- [ZimaBoard/Blade Quick Start](./zima-board-quick-start.md)
