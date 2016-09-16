---
layout: page
title:  "Raspberry Pi Quick Start"
date:   2014-08-25 12:31:11
categories: [doc]
---

[Overview](#overview)

[Target Platform Installation](#target-platform-installation)

[Development Environment
Installation](#development-environment-installation)

## Overview

This section provides Eclipse Kura quick installation procedures for the
Raspberry Pi and the Kura development environment.

{% include alerts.html message="This quickstart will install the version of Kura with the
administraive web UI and netwowrk configuration support but not CAN support. For more information
on this please visit the [Eclipse Kura download page](https://www.eclipse.org/kura/downloads.php)" %}

## Target Platform Installation

To install Kura with its dependencies on the Raspberry Pi 2, perform the
following steps:

1. Boot the Raspberry Pi with the latest Raspbian image.
2. Eclipse Kura requires Oracle Java VM or Open JDK to be installed on the target device. Verify that Java 7 or above is installed with:

    <pre>java –version</pre>

    If not, install it with:

    <pre>sudo apt-get install java</pre>

3.  Connect to the platform shell (ssh).

4.  Download the Kura package with:

	<pre>wget https://s3.amazonaws.com/kura_downloads/raspbian/release/1.4.0/kura_1.4.0_raspberry-pi-2_installer.deb</pre>

5.  Install Kura with: 

    <pre>sudo apt-get update

    sudo dpkg -i kura_1.4.0_raspberry-pi-2_installer.deb

    sudo apt-get install -f</pre>

6.  Reboot the Raspberry Pi with:
    <pre>sudo reboot</pre>

    Kura starts on the target platform after reboot.

## Development Environment Installation

To set up the development environment for Eclipse Kura, perform the
following steps:

1. Download and install JDK SE 7 as appropriate for your OS (if it is not already installed). For Windows and Linux users, the JDK can be
downloaded from Java SE 7 Downloads. Use the latest version of Java SE Development Kit.
2. Download and install the Eclipse IDE for Java EE Developers from the <a href="http://www.eclipse.org/downloads/" target="_blank">Eclipse download site</a>.
3.  Download the Kura user workspace archive from the <a href="https://www.eclipse.org/kura/downloads.php" target="_blank">Eclipse Kura download site</a>.
4.  Import the Kura workspace zip file into the new Eclipse project development environment.
5.  Begin developing Kura-based applications for your target platform.

