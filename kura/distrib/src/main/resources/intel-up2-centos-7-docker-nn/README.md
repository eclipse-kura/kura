# Installation of Eclipse Kura™ on Intel Up²

This is a guide about installing Kura on the Intel Up² on CentOS 7.

## Operating system installation

First you need to install CentOS 7.4+. Download the "Minimal ISO" from:

https://www.centos.org/download/

Next you will need to flash this onto a USB stick:

    sudo dd if=CentOS-7-x86_64-Minimal-1708.iso of=/dev/sdXXX bs=8M status=progress oflag=direct

**Note:** Be sure to replace `/dev/sdXXX` with the actual device of the USB stick (e.g. `/dev/sdb`).
Be careful as this will erase all data on this device.

Boot from the USB stick and perform a normal installation of CentOS.

## Install Kura

In order to install Kura you will need to enable EPEL repositories by executing:

    sudo yum install epel-release

Next install Kura itself:

    sudo yum install kura-intel-up2-centos-7-4.0.0-1-x86_64

After the installation is complete, reboot the machine by executing:

    sudo reboot
