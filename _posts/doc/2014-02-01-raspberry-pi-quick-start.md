---
layout: page
title:  "Raspberry Pi Quick Start"
categories: [doc]
---

[Overview](#overview)

[Enable SSH Access](#enable-ssh-access)

[Eclipse Kura&trade; Installation](#eclipse-kuratrade-installation)

[Development Environment Installation](#development-environment-installation)

## Overview

This section provides Eclipse Kura&trade; quick installation procedures for the
Raspberry Pi and the Kura development environment.

{% include alerts.html message="This quickstart will install the version of Kura with the
administraive web UI and netwowrk configuration support but not CAN support. For more information
on this please visit the [Eclipse Kura download page](https://www.eclipse.org/kura/downloads.php)" %}

This quickstart has been tested using the following image:

<pre>2016-11-25-raspbian-jessie-lite.img</pre>

downloaded from

<pre>https://www.raspberrypi.org/downloads/raspbian/</pre>

and with the NOOBS image installed on the sdcard bundled with the Raspberry Pi 3.

## Enable SSH Access

The ssh server is disabled by default on Raspbian images released after November 2016,
in order to enable it follow the instructions available at the following URL:

<pre>https://www.raspberrypi.org/documentation/remote-access/ssh/</pre>

## Eclipse Kura&trade; Installation

To install Kura with its dependencies on the Raspberry Pi, perform the
following steps:

1.  Boot the Raspberry Pi with the latest Raspbian image (starting from release 2.1.0 Kura only supports Debian 8 or above).

2.  The dhcpcd5 package is not compatible with Kura and needs to be removed
    performing the following command:

    <pre>sudo apt-get purge dhcpcd5</pre>

3.  NetworkManager conflicts with Kura network management, make sure that it is
    not installed performing the following command:

    <pre>sudo apt-get remove network-manager</pre>

4.  Install the gdebi command line tool:

    <pre>sudo apt-get update
    sudo apt-get install gdebi-core</pre>

5.  Make sure that Java 8 is installed with

    <pre>java -version</pre>

    if not install OpenJDK 8 performing the following command:

    <pre>sudo apt-get install openjdk-8-jre-headless</pre>

6.  Download the Kura package with:

    <pre>wget http://download.eclipse.org/kura/releases/&lt;version&gt;/kura_&lt;version&gt;_raspberry-pi-2-3_installer.deb</pre>

    Note: replace \<version\> in the URL above with the version number of the latest release (e.g. 2.1.0).

7.  Install Kura with: 

    <pre>sudo gdebi kura_&lt;version&gt;_raspberry-pi-2-3_installer.deb</pre>

8.  Reboot the Raspberry Pi with:

    <pre>sudo reboot</pre>

    Kura starts on the target platform after reboot.

9.  Kura setups a local web ui that is available using a browser via:

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
