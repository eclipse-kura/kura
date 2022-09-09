# Intel Up² Quick Start

## Overview

This section provides Eclipse Kura&trade; quick installation procedures for the
Intel Up² and the Kura development environment.

!!! warn ""
    This quickstart will install the version of Kura with the administrative web UI and network configuration support but not CAN support. For more information on this please visit the [Eclipse Kura download page](https://www.eclipse.org/kura/downloads.php).

This quickstart has been tested using the following image:

```
ubuntu-20.04.4-live-server-amd64.iso
```

downloaded from

```
https://releases.ubuntu.com/20.04/ubuntu-20.04.4-live-server-amd64.iso
```

and with the image burned on an USB stick with [balenaEtcher](https://www.balena.io/etcher/).

A complete guide on how to install Ubuntu on the Intel Up² can be found [here](https://wiki.up-community.org/Ubuntu).

It is important, in order to access the HAT, Bluetooth, Wifi functionality, to follow the relative steps provided in the complete guide. Make sure to assign the right execute permissions to `kurad` user created by the installer as described here: [Add Groups](https://github.com/up-board/up-community/wiki/Ubuntu_20.04#add-groups).

It is high raccomanded to install the custom Intel kernel provided in the guide.

## Eclipse Kura&trade; Installation

To install Kura with its dependencies on the Intel Up², perform the
following steps:

1. Boot the Intel Up² with the Ubuntu Image 20.04.3.

2. Make sure your device is connected to internet.

3. Upgrade the system:

    ```
    sudo apt update
    sudo apt upgrade
    ```

3. Download the Kura package with:

    ```
    wget http://download.eclipse.org/kura/releases/<version>/kura_<version>_intel-up2-ubuntu-20_installer.deb
    ```

    Note: replace `<version>` in the URL above with the version number of the latest release (e.g. 5.1.0).

6. Install Kura with: 

    ```
    apt-get install./ kura_<version>_intel-up2-ubuntu-20_installer.deb
    ```

7. Set the right Wi-Fi regulatory domain based on your current world region editing the `/etc/default/crda` and adding the [ISO 3166-1 alpha-2](https://it.wikipedia.org/wiki/ISO_3166-1_alpha-2) code of your region.
   
8. Reboot the Intel Up² with:

    ```
    sudo reboot
    ```

    Kura starts on the target platform after reboot.

9. Kura setups a local web ui that is available using a browser via:

    ```
    https://<device-ip>
    ```

    The browser will prompt the user to accept the connection to an endpoint with an untrusted certificate:
    
    ![Untrusted certificate page](./images/untrusted_cert1.png)

    ![Untrusted certificate details](./images/untrusted_cert2.png)

    ![Proceed trusting the source](./images/untrusted_cert3.png)

    Once trusted the source, the user will be redirected to a login page where the default **username** is:

    ```
    admin
    ```

    and the default **password** is:

    ```
    admin
    ```
