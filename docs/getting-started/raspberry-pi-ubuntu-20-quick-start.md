# Raspberry Pi - Ubuntu 20 Quick Start

## Overview

This section provides Eclipse Kura&trade; quick installation procedures for the
Raspberry Pi.

!!! warning
    This quickstart will install the version of Kura with the administrative web UI and network configuration support but not [CAN bus](https://en.wikipedia.org/wiki/CAN_bus) support. For more information on this please visit the [Eclipse Kura download page](https://websites.eclipseprojects.io/kura/downloads.php)

This quickstart has been tested using the latest Ubuntu 20.04.3 LTS Live Server for arm64 architecture flashed on the SD card through [Raspberry Pi Imager](https://www.raspberrypi.com/software/).

The official images can be also found on the [Project Page](https://ubuntu.com/download/raspberry-pi). Further information on the Ubuntu installation for Raspberry Pi can be found [here](https://ubuntu.com/tutorials/how-to-install-ubuntu-on-your-raspberry-pi).

!!! warning
    Please note that, at the time of this writing, only 64 bit OS image is supported.

## Enable SSH Access

On Ubuntu 20.04.3 the ssh access is enabled only for the standard **ubuntu** user. If you desire to remote login as root user, edit the file `/etc/ssh/sshd_config` (using the root permission) adding the line `PermitRootLogin yes`

## Eclipse Kura&trade; Installation

To install Eclipse Kura with its dependencies on the Raspberry Pi, perform the
following steps:

1. Boot the Raspberry Pi with the latest Ubuntu 20.04.3 LTS Server image.

2. Make sure your device is connected to internet. By default, `eth0` lan network interface is configured in DHCP mode.

3. Upgrade the system:

    ```bash
    sudo apt update
    ```
    ```bash
    sudo apt upgrade
    ```

    !!! tip
        **Optional**: Since version 5.3.0 Kura also supports [Eclipse Temurin&trade;](https://adoptium.net/en-GB/) as an alternative JVM. To install it you need to perform these additional steps:

        ```bash
        sudo apt-get install -y wget apt-transport-https gnupg
        ```
        ```bash
        sudo wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
        ```
        ```bash
        sudo echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
        ```
        ```bash
        sudo apt-get update
        ```
        ```bash
        sudo apt-get install temurin-17-jdk
        ```

4. Download the Kura package with:

    ```
    wget http://download.eclipse.org/kura/releases/<version>/kura-<kura-version>_generic-<arch>_installer.deb
    ```

    Note: replace `<version>` in the URL above with the version number of the latest release (e.g. 5.5.0) and `<arch>` with your device architecture 


5. Install Kura with:

    ```
    sudo apt install ./kura-<kura-version>_generic-<arch>_installer.deb
    ```

    All the required dependencies will be downloaded and installed.

6. Set the right Wi-Fi regulatory domain based on your current world region editing the `/etc/default/crda` and adding the [ISO 3166-1 alpha-2](https://it.wikipedia.org/wiki/ISO_3166-1_alpha-2) code of your region.

7. (Optional) Configure the GPIO replacing the content of the file `/opt/eclise/kura/framework/jdk.dio.properties` with the following text:

    ```
    0 = deviceType: gpio.GPIOPin, pinNumber:0, name:GPIO0
    1 = deviceType: gpio.GPIOPin, pinNumber:1, name:GPIO1
    2 = deviceType: gpio.GPIOPin, pinNumber:2, name:GPI02
    3 = deviceType: gpio.GPIOPin, pinNumber:3, name:GPIO3
    4 = deviceType: gpio.GPIOPin, pinNumber:4, name:GPIO4
    5 = deviceType: gpio.GPIOPin, pinNumber:5, name:GPIO5
    6 = deviceType: gpio.GPIOPin, pinNumber:6, name:GPIO6
    7 = deviceType: gpio.GPIOPin, pinNumber:7, name:GPIO7
    8 = deviceType: gpio.GPIOPin, pinNumber:8, name:GPIO8
    9 = deviceType: gpio.GPIOPin, pinNumber:9, name:GPIO9
    10 = deviceType: gpio.GPIOPin, pinNumber:10, name:GPIO10
    11 = deviceType: gpio.GPIOPin, pinNumber:11, name:GPIO11
    12 = deviceType: gpio.GPIOPin, pinNumber:12, name:GPIO12
    13 = deviceType: gpio.GPIOPin, pinNumber:13, name:GPIO13
    14 = deviceType: gpio.GPIOPin, pinNumber:14, name:GPIO14
    15 = deviceType: gpio.GPIOPin, pinNumber:14, name:GPIO15
    16 = deviceType: gpio.GPIOPin, pinNumber:16, name:GPIO16
    17 = deviceType: gpio.GPIOPin, pinNumber:17, name:GPIO17
    18 = deviceType: gpio.GPIOPin, pinNumber:18, name:GPIO18
    19 = deviceType: gpio.GPIOPin, pinNumber:19, name:GPIO19
    20 = deviceType: gpio.GPIOPin, pinNumber:20, name:GPIO20
    21 = deviceType: gpio.GPIOPin, pinNumber:21, name:GPIO21
    22 = deviceType: gpio.GPIOPin, pinNumber:22, name:GPIO22
    23 = deviceType: gpio.GPIOPin, pinNumber:23, name:GPIO23
    24 = deviceType: gpio.GPIOPin, pinNumber:24, name:GPIO24
    25 = deviceType: gpio.GPIOPin, pinNumber:25, name:GPIO25
    26 = deviceType: gpio.GPIOPin, pinNumber:26, name:GPIO26
    27 = deviceType: gpio.GPIOPin, pinNumber:27, name:GPIO27

    gpio.GPIOPin = initValue:0, deviceNumber:0, direction:3, mode:-1, trigger:3
    uart.UART = baudRate:19200, parity:0, dataBits:8, stopBits:1, flowControl:0
    ```

8. Reboot the Raspberry Pi with:

    ```
    sudo reboot
    ```

    Kura starts on the target platform after reboot.

9. Kura setups a local web ui that is available using a browser via:

    ```
    https://<device-ip>
    ```

    The browser will prompt the user to accept the connection to an endpoint with a self signed certificate, select `Accept the risk and continue`:
    
    ![Proceed trusting the source](./images/untrusted_cert.png)

    Once trusted the source, the user will be redirected to a login page where the following credentianls:
    **username**: `admin`
    **password**: `admin`
