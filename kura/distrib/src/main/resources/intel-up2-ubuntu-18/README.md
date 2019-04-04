# Installation of Eclipse Kura™ on Intel Up² with Ubuntu

This is a guide about installing Kura on the Intel Up² with Ubuntu 18-04.

## Operating system installation

A complete guide on how to install Ubuntu on the Intel Up² can be found [here](https://wiki.up-community.org/Ubuntu).
The main procedure is presented in the following.

### Install Ubuntu on Intel Up² board

- To install the OS, you need a Intel Up² board, an USB stick with at least 8Gb of space, a keyboard, a mouse, a screen and an internet connection.

- Download Ubuntu 18.04.2 ISO from the Ubuntu download page (works with desktop and server edition)

```
http://releases.ubuntu.com/18.04/ubuntu-18.04.2-desktop-amd64.iso 
http://releases.ubuntu.com/18.04/ubuntu-18.04.2-live-server-amd64.iso
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
sudo apt-get install linux-image-generic-hwe-18.04-upboard
```

- Install the updates
```
sudo apt dist-upgrade -y
sudo reboot
```

- Install the firmware driver for up core wifi chip
```
sudo apt install firmware-ampak
```

To install Intel graphic card drivers and to enable the HAT functionalities, please refer to [the complete guide](https://wiki.up-community.org/Ubuntu).

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
sudo gdebi kura_4.1.0_intel-up2-ubuntu_installer.deb
```
After the installation is complete, reboot the machine by executing:
```
sudo reboot
```