# Installation of Eclipse Kura™ on Intel Up² with Ubuntu

This is a guide about installing Kura on the Intel Up² with Ubuntu 16-04.

## Operating system installation

A complete guide on how to install Ubuntu on the Intel Up² can be found [here](https://wiki.up-community.org/Ubuntu).
The main procedure is presented in the following.

### Install Ubuntu on Intel Up² board

- To install the OS, you need a Intel Up² board, an USB stick with at least 8Gb of space, a keyboard, a mouse, a screen and an internet connection.

- Download Ubuntu 16.04 ISO from the Ubuntu download page (works with desktop and server edition)

```
http://old-releases.ubuntu.com/releases/16.04.3/ubuntu-16.04.3-desktop-amd64.iso 
http://old-releases.ubuntu.com/releases/16.04.3/ubuntu-16.04.3-server-amd64.iso
```

- Burn the downloaded image on a USB stick using [Etcher](https://etcher.io)

- Insert the USB installer disk in a empty USB port and proceed with a normal Ubuntu installation.
  > While installing, **do not select** the option "automatic updates"!

- After the reboot you need to add this repository:
```
sudo add-apt-repository ppa:ubilinux/up
```

- Update the repository list
```
sudo apt update
```

- Remove all the generic installed kernel
```
sudo apt-get autoremove --purge 'linux-.*generic'
```

- Install the new kernel:
```
sudo apt-get install linux-image-generic-hwe-16.04-upboard
```

- Install the updates
```
sudo apt dist-upgrade -y
sudo reboot
```
  > Make sure to not upgrade the system to Ubuntu 18.04!

- Install the firmware driver for up core wifi chip
```
sudo apt install firmware-ampak-ap6214a
```

To install Intel graphic card drivers and to enable the HAT functionalities, please refer to [the complete guide](https://wiki.up-community.org/Ubuntu).

### Install Bluez 5.43

Kura uses [TinyB](https://github.com/intel-iot-devkit/tinyb) to support BluetoothLE. In order to use Kura BLE capabilities, the bluez library has to be upgraded to the version 5.43.
Please follow the procedure below.

- Install software needed by the patch
```
sudo  apt-get install debhelper dh-autoreconf flex bison libdbus-glib-1-dev libglib2.0-dev  libcap-ng-dev libudev-dev libreadline-dev libical-dev check dh-systemd libebook1.2-dev devscripts cups haveged
```

- Create a gpg key. Please fill the information requested by the interactive procedure
```
gpg --gen-key
```

- Then
```
wget https://launchpad.net/ubuntu/+archive/primary/+files/bluez_5.43.orig.tar.xz
wget https://launchpad.net/ubuntu/+archive/primary/+files/bluez_5.43-0ubuntu1.debian.tar.xz
wget https://launchpad.net/ubuntu/+archive/primary/+files/bluez_5.43-0ubuntu1.dsc

tar xf bluez_5.43.orig.tar.xz
cd bluez-5.43
tar xf ../bluez_5.43-0ubuntu1.debian.tar.xz
debchange 'Backport to Xenial'
dpkg-buildpackage -uc -us
cd ..
sudo dpkg -i *.deb
```

Check that `hcitool` command report the correct version.

A complete procedure can be found [here](https://askubuntu.com/questions/883713/using-bluez-5-43-on-ubuntu-16-04/884062) and [here](https://serverfault.com/questions/191785/how-can-i-properly-sign-a-package-i-modified-and-recompiled).
 
## Install Kura

The following services must be configured before installing Kura:
```
sudo systemctl disable networking
```
The following packages must be installed before installing Kura:
```
sudo apt-get install gdebi-core
sudo apt-get install openjdk-8-jre-headless
```
Install Kura:
```
sudo gdebi kura_4.0.0-SNAPSHOT_intel-up2-ubuntu_installer.deb
```
After the installation is complete, reboot the machine by executing:
```
sudo reboot
```