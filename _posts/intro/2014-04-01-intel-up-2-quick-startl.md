---
layout: page
title:  "Intel Up² Quick Start"
categories: [intro]
---

[Overview](#overview)

[Enable SSH Access](#enable-ssh-access)

[Eclipse Kura&trade; Installation](#eclipse-kuratrade-installation)

[Development Environment Installation](#development-environment-installation)

## Overview

This section provides Eclipse Kura&trade; quick installation procedures for the
Intel Up² and the Kura development environment.

{% include alerts.html message="This quickstart will install the version of Kura with the administrative web UI and network  configuration support but not CAN support. For more information on this please visit the [Eclipse Kura download page](https://www.eclipse.org/kura/downloads.php)" %}

This quickstart has been tested using the following image:

<pre>ubuntu-18.04.5-live-server-amd64.iso</pre>

downloaded from

<pre>https://releases.ubuntu.com/bionic/ubuntu-18.04.5-live-server-amd64.iso</pre>

and with the image burned on an USB stick with [balenaEtcher](https://www.balena.io/etcher/)

A complete guide on how to install Ubuntu on the Intel Up² can be found [here](https://wiki.up-community.org/Ubuntu).

It is important, in order to access the HAT, Bluetooth, Wifi functionality, to follow the relative steps provided in the complete guide. Make sure to assign the right execute permissions to `kurad` user created by the installer as described here: [Add Groups](https://github.com/up-board/up-community/wiki/Ubuntu_18.04#add-groups)

It is high raccomanded to install the custom Intel kernel provided in the guide.

## Eclipse Kura&trade; Installation

To install Kura with its dependencies on the Intel Up², perform the
following steps:

1. Boot the Raspberry Pi with the Ubuntu Image 18.04.5.

2. The following packages  must be installed before installing Kura: 

   <pre>sudo apt install hostapd, isc-dhcp-server, iw, dos2unix, bind9, unzip, ethtool, telnet, bluez-hcidump,wireless-tools, chrony</pre>.

3. Assign a static ip to the primary ethernet network interface:
   
   <pre></pre>
   
5. Make sure that Java 8 is installed with

    <pre>java -version</pre>

    if not install OpenJDK 8 performing the following command:

    <pre>sudo apt-get install openjdk-8-jre-headless</pre>

6. Download the Kura package with:

    <pre>wget http://download.eclipse.org/kura/releases/&lt;version&gt;/kura_&lt;version&gt;_intel-up2-ubuntu-18_installer.deb</pre>

    Note: replace \<version\> in the URL above with the version number of the latest release (e.g. 5.0.0).

8. Install Kura with: 

    <pre>apt install kura_&lt;version&gt;_intel-up2-ubuntu-18_installer.deb</pre>

10. Reboot the Intel Up² with:

    <pre>sudo reboot</pre>

    Kura starts on the target platform after reboot.

11. Kura setups a local web ui that is available using a browser via:

      <pre>https://&lt;device-ip&gt;</pre>

      The browser will prompt the user to accept the connection to an endpoint with an untrusted certificate:
      ![Untrusted certificate page]({{ site.baseurl }}/assets/images/admin/untrusted_cert1.png)

      ![Untrusted certificate details]({{ site.baseurl }}/assets/images/admin/untrusted_cert2.png)

      ![Proceed trusting the source]({{ site.baseurl }}/assets/images/admin/untrusted_cert3.png)

      Once trusted the source, the user will be redirected to a login page where the default **username** is:

      <pre>admin</pre>

      and the default **password** is:

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