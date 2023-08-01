# Install Kura

Kura is provided using a DEB Linux package. Visit the [Kura download page](https://www.eclipse.org/kura/downloads.php) to find the correct installation file for the target system.



## Installer types

Several installers can be found on such page, and they fall into one of the following categories:

1. specific device profiles, like `kura_5.3.0_raspberry-pi-armhf_installer.deb` (identified with the device name in the [downloads](https://www.eclipse.org/kura/downloads.php) page);
2. generic profiles, like `kura-5.3.0_generic-arm32_installer.deb`; and
3. profiles with suffix **nn**, like `kura_5.3.0_raspberry-pi-nn_installer.deb`

Profiles of types (1) and (2) ship a Kura version with networking functionalities. In particular, the installers of kind (1) use **Kura Networking** for leveraging network interface configurations and are made for a specific device.

Installers of type (2) can be installed on targets with [NetworkManager](https://networkmanager.dev); a commonly available tool for managing Linux networking. Kura leverages this tool for networking functionalities. Refer to the [Generic installers](#generic-installers) section for further information.

Installers of type (3) with the suffix `nn` are **No Networking** profiles that do not bundle the Kura Network Manager: all the network configurations need to be done outside of Kura. Functionalities **missing** in **NN profiles** compared to the full Kura profiles:

- Networking interfaces management
- Firewall configuration management
- Network Threat management



## Generic installers

Kura can be installed using the **generic profiles**

```
kura-<kura-version>_generic-<arch>_installer.deb/rpm
```

where `<arch>` is one of the **supported architectures**: *x86_64*, *arm32*, and *arm64*. A generic Kura profile can work on systems that have available the dependencies listed in the [Kura dependencies](#kura-dependencies) section, and that have **at least one** physical ethernet interface.

If installing the generic package on a non supported device (see [Supported Devices](#supported-devices)), only one ethernet interface and one wifi interface (if present) are configured. The firewall will be set up as follows:

![](./images/firewall-generic.png)

Where `eth0` and `wlan0` will be replaced with the detected primary ethernet and wireless interfaces.

On unsupported devices, Kura will install an empty `jdk.dio.properties` file. Hence, for having the complete set of Kura features, further configurations are recommended after installation:

- other network interfaces, if any, from the web UI;
- the system firewall from the web UI;
- editing of `/opt/eclipse/kura/framework/jdk.dio.properties` for correct GPIO mappings.

### Kura dependencies

On generic profiles (`generic-aarch64`, `generic-arm32`, `generic-x86_64`), to have all the Kura features working, the following dependencies are required:

- General: `setserial`, `zip`, `gzip`, `unzip`, `procps`, `usbutils`, `socat`, `gawk`, `sed`, `inetutils-telnet`, `telnet`.
- Security: `polkit` or `policykit-1`, `ssh` or `openssh`, `openssl`, `busybox`, `openvpn`.
- Bluetooth: `bluez` or `bluez5`, `bluez-hcidump` or `bluez5-noinst-tools`.
- Time: `ntpdate`, `chrony`, `chronyc`, `cron` or `cronie`.
- Networking: `network-manager` or `networkmanager`, `bind9` or `bind`, `dnsmasq` or `isc-dhcp-server` or (`dhcp-server` and `dhcp-client`), `iw`, `iptables`, `modemmanager`.
- Logs: `logrotate`.
- Gps: `gpsd`.
- Python: `python3`.
- Java: `openjdk-8-jre-headless` or `temurin-8-jdk` or `openjdk-17-jre-headless` or `temurin-17-jdk`.
- Others: `dos2unix`

In addition, some devices may require particular dependencies that are not included in the the list above. So, if you're not using a generic version of Kura, it is recomended to get a look of the device-specific page in this documentation section.

### Supported devices

Kura generic has been tested on the following devices and provides full configuration of all the available interfaces and GPIO mappings.

| Device | Architecture | OS |
| - | - | - |
| **Raspberry Pi 3/4** | *arm32* | Raspbian "Bullseye" |
| **Raspberry Pi 3/4** | *arm64* | Ubuntu 20.04 |
| **Intel Up²** | *x86_64* | Ubuntu 20.04 |
| **NVIDIA Jetson Nano&trade;** | *arm64* | Ubuntu 20.04 |

Check out the quick start guides for the detailed installation steps and set-up procedures:

- [Raspberry Pi - Raspberry Pi OS Quick Start](./raspberry-pi-raspberryos-quick-start.md)
- [Raspberry Pi - Ubuntu 20 Quick Start](./raspberry-pi-ubuntu-20-quick-start.md)
- [Docker Quick Start](./docker-quick-start.md)
- [Intel Up² Quick Start](./intel-up-2-quick-start.md)
- [NVIDIA Jetson Nano&trade;](./nvidia-jetson-nano-quick-start.md)