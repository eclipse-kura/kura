# Installation of Eclipse Kura™ on Intel Up²

This is a guide for installing Kura on the Intel Up² with Ubuntu 16.04.

## Install Kura

The following services must be configured before installing Kura:

    sudo systemctl disable networking

The following packages must be installed before installing Kura:

    sudo apt-get install gdebi-core
    sudo apt-get install openjdk-8-jre-headless

Install Kura:

    sudo gdebi kura_3.3.0-SNAPSHOT_intel-up2-ubuntu_installer.deb

After the installation is complete, reboot the machine by executing:

    sudo reboot
