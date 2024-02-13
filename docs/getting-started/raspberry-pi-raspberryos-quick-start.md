# Raspberry Pi - Raspberry Pi OS Quick Start

## Overview

This section provides Eclipse Kura&trade; quick installation procedures for the Raspberry Pi and the Kura development environment.

!!! warning
    This quickstart will install the version of Kura with the administrative web UI and network  configuration support but not [CAN bus](https://en.wikipedia.org/wiki/CAN_bus) support. For more information on this please visit the [Eclipse Kura download page](https://websites.eclipseprojects.io/kura/downloads.php)

This quickstart has been tested using the latest Raspberry Pi OS 32 and 64 bits images which are available for download through [the official Raspberry Pi foundation site](https://www.raspberrypi.com/software/operating-systems/) and the Raspberry Pi Imager.

!!! warning
    Recent versions of Raspberry Pi OS 32 bit on Raspberry PI 4 will use by default a 64 bit kernel with a 32 bit userspace. This can cause issues to applications that use the result of `uname -m` to decide which native libraries to load (see https://github.com/raspberrypi/firmware/issues/1795). This currently affects for example the Kura SQLite database connector. It should be possible to solve by switching to the 32 bit kernel setting `arm_64bit=0` in `/boot/config.txt` and restarting the device.

For additional details on OS compatibility refer to the [Kura&trade; release notes](https://websites.eclipseprojects.io/kura/downloads.php).

## Enable SSH Access

The ssh server is disabled by default on Raspbian images released after November 2016,
in order to enable it follow the instructions available [here](https://www.raspberrypi.org/documentation/remote-access/ssh/).

If you're using the [Raspberry Pi Imager](https://github.com/raspberrypi/rpi-imager) you can directly enable SSH before writing the operating system into the SD card by clicking on the "setting" icon.

![Enable SSH Raspberry Pi Imager](./images/imager-enable-ssh.png)

## Eclipse Kura&trade; Installation

To install Eclipse Kura with its dependencies on the Raspberry Pi, perform the
following steps:

1. Boot the Raspberry Pi with the latest Raspbian image (starting from release 5.1.0 Kura is tested with Raspbian 11).

2. Make sure your device is connected to the internet. The best installation experience can be obtained when the device is cabled to the local network and the Internet. By default, the Raspberry Pi OS configures the ethernet interface `eth0` in DHCP mode.

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
    sudo apt-get install ./kura-<kura-version>_generic-<arch>_installer.deb
    ```

6. For a correct configuration of the Wlan interface, it is necessary to set the **Locale** and the **WLAN Country** through the `raspi-config` command:

    ```
    sudo raspi-config
    ```

    ![raspi-config menu](./images/raspi-config.png)

    From the raspi-config main menu select `Localisation Options`:

    ![raspi-config menu](./images/raspi-localisation-menu.png)

    Then modify the **Locale** and **WLAN Country** with with the proper settings for your location. For example, an user located in Italy could set the values as the ones in the table:

    | Setting         	| Value             	|
    |-----------------	|-------------------	|
    | L1 Locale       	| it_IT.UTF-8 UTF-8 	|
    | L4 WLAN Country 	| IT Italy          	|

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

    You can check your GPIO device configuration executing the command `pinout`

8. Reboot the Raspberry Pi with:

    ```
    sudo reboot
    ```

    Kura starts on the target platform after reboot.

9. Kura setups a local web ui that is available using a browser via:

    ```
    https://<device-ip>
    ```

    The browser will prompt the user to accept the connection to an endpoint with an untrusted certificate:

    ![Proceed trusting the source](./images/untrusted_cert.png)

    Once trusted the source, the user will be redirected to a login page where the following credentianls:
    **username**: `admin`
    **password**: `admin`
