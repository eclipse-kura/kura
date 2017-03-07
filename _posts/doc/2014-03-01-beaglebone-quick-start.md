---
layout: page
title:  "BeagleBone Quick Start"
categories: [doc]
---

[Overview](#overview)

[Eclipse Kura&trade; Installation](#eclipse-kuratrade-installation)

[Development Environment Installation](#development-environment-installation)

## Overview

This section provides Eclipse Kura&trade; quick installation procedures for the BeagleBone Black rev C on Debian and the
Kura development environment.

{% include alerts.html message="This quickstart will install the version of  Kura with the administraive web UI and netwowrk configuration support but not CAN support. For more information on this please visit https://www.eclipse.org/kura/downloads.php" %}

This quickstart has been written basing upon on the following Debian 8 image:

<pre>bone-debian-8.6-lxqt-4gb-armhf-2016-11-06-4gb.img</pre>

downloaded from

<pre>https://beagleboard.org/latest-images</pre>

## Eclipse Kura&trade; Installation

To install Kura with its dependencies on the BeagleBone, perform the
following steps:

1.  Boot the BeagleBone Black with the latest Debian image (starting from release 2.1.0 Kura only supports Debian 8 or above).

2.  Connect to the platform shell (ssh).

3. BeagleBone ships with several web services enabled to assist in setting
   up the device and doing example projects. If the Kura web UI is to be
   used, these services must be disabled to avoid interference. To disable
   the BeagleBone web services, perform the following commands:

    <pre>
    sudo systemctl disable cloud9.service
    sudo systemctl disable apache2.service
    sudo systemctl disable bonescript.service
    sudo systemctl disable bonescript.socket
    sudo systemctl disable bonescript-autorun.service
    sudo systemctl disable avahi-daemon.service</pre>

4. The BeagleBone default image provides a desktop environment which is auto started at boot, it needs to be disabled using the following command:

    <pre>sudo systemctl disable lightdm.service</pre>

5. The Connman service conflicts with Kura network management and needs to be disabled
   performing the following command:

    <pre>sudo systemctl disable connman.service</pre>

6. The tyme sync service conflicts with Kura Clock Service and needs to be disabled:

    <pre>sudo timedatectl set-ntp false</pre>

7.  Install the gdebi command line tool:

    <pre>
    sudo apt-get update
    sudo apt-get install gdebi-core</pre>

8.  Download the Kura package with:

    <pre>
    wget http://download.eclipse.org/kura/releases/&lt;version&gt;/kura_&lt;version&gt;_beaglebone_debian_installer.deb
    </pre>

    Note: replace \<version\> in the URL above with the version number of the latest release (e.g. 2.1.0).

9.  Install Kura with: 

    <pre>sudo gdebi kura_&lt;version&gt;_beaglebone_debian_installer.deb</pre>

    Note: The BeagleBone Kura distribution has been tested on Java 8, but the official Debian 8 repositories do not contain a free Java 8 implementation. The Kura Debian
    package therefore depends on OpenJDK 7, which will be installed automatically by gdebi if no other Java installation is found on the system.

    In order to use Java 8 it is possible to manually install the following non-free package before performing the Kura package installation:

    <pre>sudo apt-get install oracle-java8-installer</pre>

10. Reboot the BeagleBone Black with:

    <pre>sudo reboot</pre>

    Kura starts on the target platform after reboot.

11. Kura setups a local web ui that is available using a browser via:

    <pre>http://&lt;device-ip&gt;</pre>

    Default **username** is:

    <pre>admin</pre>

    Default **password** is:

    <pre>admin</pre>

## Development Environment Installation

### User Workspace

To set up the development environment for Eclipse Kura, perform the
following steps:

1. Download and install JDK SE 8 as appropriate for your OS (if it is not already installed). For Windows and Linux users, the JDK can be
downloaded from Java SE 8 Downloads. Use the latest version of Java SE Development Kit.
2. Download and install the Eclipse IDE for Java EE Developers from the <a href="http://www.eclipse.org/downloads/" target="_blank">Eclipse download site</a>.
3.  Download the Kura user workspace archive from the <a href="https://www.eclipse.org/kura/downloads.php" target="_blank">Eclipse Kura download site</a>.
4.  Import the Kura workspace zip file into the new Eclipse project development environment.
5.  Begin developing Kura-based applications for your target platform.

### Oomph Installer
1. Start the Eclipse Installer
1. Switch to advanced mode (in simple mode you cannot add the custom installer)
1. Select "Eclipse for Committers" and select a JRE 1.8+ -> Next
1. Add a new installer by URL: https://raw.githubusercontent.com/eclipse/kura/develop/kura/setups/kura.setup -> Check and next
1. Update Eclipse Kura Git repository's username (HTTPS, link to your fork) and customize further settings if you like (e.g. Root install folder, Installation folder name) -> Next
1. Leave all Bootstrap Tasks selected -> Finish
1. Accept the licenses and unsigned content
1. Wait for the installation to finish, a few additional plugins will be installed
1. At first startup Eclipse IDE will checkout the code and perform a full build
1. A few Working Sets will be prepared