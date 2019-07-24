---
layout: page
title:  "Raspberry Pi Quick Start"
categories: [intro]
---

[Overview](#overview)

[Enable SSH Access](#enable-ssh-access)

[Eclipse Kura&trade; Installation](#eclipse-kuratrade-installation)

[Development Environment Installation](#development-environment-installation)


## Overview

This section provides Eclipse Kura&trade; quick installation procedures for the
Raspberry Pi and the Kura development environment.

{% include alerts.html message="This quickstart will install the version of Kura with the administraive web UI and netwowrk configuration support but not CAN support. For more information on this please visit the [Eclipse Kura download page](https://www.eclipse.org/kura/downloads.php)" %}

This quickstart has been tested using the following image:

<pre>2016-11-25-raspbian-jessie-lite.img</pre>

downloaded from

<pre>https://www.raspberrypi.org/downloads/raspbian/</pre>

and with the NOOBS image installed on the sdcard bundled with the Raspberry Pi 3.

## Enable SSH Access

The ssh server is disabled by default on Raspbian images released after November 2016,
in order to enable it follow the instructions available at the following URL:

<pre>https://www.raspberrypi.org/documentation/remote-access/ssh/</pre>

## Direct Connection of the Raspberry Pi to the Computer

If you don't use Raspberry Pi with a monitor and keyvboard, you can direct connection of the Raspberry Pi to the Computer.

{% include alerts.html message="This solution is for Raspian Jessie and later version (8+)" %}

1. Insert the SD card on a reader of your Computer.
2. Access the **boot** partition of the card and create a file called **ssh**.
3. Access the **rootfs** partition of the card and edit the file **/etc/dhcpcd.conf**

   <pre>
   interface 'interface'
   static ip_address='ip to assign'/'subnet'
   static routers='ip router'
   static domain_name_server='server DNS'
   </pre>

   for example
   <pre>
   interface eth0
   static ip_address=10.42.0.27/24
   static routers=10.42.0.1
   static domain_name_server=8.8.8.8
   </pre>

{% include alerts.html message="You can access the ```/etc/dhcpcd.conf``` file also with ssh. In the Terminal type ```ssh pi@raspberrypi.local```, the defalut password is : ```raspberry```" %}

4. Assign a static IP to the computer, the values must be compatible with those of the Raspberry Pi.

5. Reboot the Raspberry Pi
   <pre> sudo reboot </pre>

## Eclipse Kura&trade; Installation

{% include alerts.html message="The last Raspbian Stretch adopts the new <a href='https://www.freedesktop.org/wiki/Software/systemd/PredictableNetworkInterfaceNames/' target='_blank'>Consistent Network Device Naming</a>. To correctly run Eclipse Kura on the Raspberry Pi, it should be disabled adding the ```net.ifnames=0``` parameter at the end of the /boot/cmdline.txt file." %}

To install Kura with its dependencies on the Raspberry Pi, perform the
following steps:

1. Boot the Raspberry Pi with the latest Raspbian image (starting from release 2.1.0 Kura only supports Debian 8 or above).

2. The dhcpcd5 package is not compatible with Kura and needs to be removed
    performing the following command:

    <pre>sudo apt-get purge dhcpcd5</pre>

3. NetworkManager conflicts with Kura network management, make sure that it is
    not installed performing the following command:

    <pre>sudo apt-get remove network-manager</pre>

    In the latest Raspbian releases, type the following command to disable the network manager:

    <pre>sudo systemctl disable networking</pre>

4. In the latest raspbian releases, the wireless interface is disabled by default. Type the following command to enabled it:

    <pre>sudo rfkill unblock all</pre>

5. Install the gdebi command line tool:

    <pre>sudo apt-get update
    sudo apt-get install gdebi-core
    </pre>

6. Make sure that Java 8 is installed with

    <pre>java -version</pre>

    if not install OpenJDK 8 performing the following command:

    <pre>sudo apt-get install openjdk-8-jre-headless</pre>

7. Download the Kura package with:

    <pre>wget http://download.eclipse.org/kura/releases/&lt;version&gt;/kura_&lt;version&gt;_raspberry-pi-2-3_installer.deb</pre>

    Note: replace \<version\> in the URL above with the version number of the latest release (e.g. 2.1.0).

8. Install Kura with: 

    <pre>sudo gdebi kura_&lt;version&gt;_raspberry-pi-2-3_installer.deb</pre>

9. Reboot the Raspberry Pi with:

    <pre>sudo reboot</pre>

    Kura starts on the target platform after reboot.

10. Kura setups a local web ui that is available using a browser via:

    <pre>https://&lt;device-ip&gt;</pre>
    
    The browser will prompt the user to accept the connection to an endpoint with an untrusted certificate:
    ![Untrusted certificate page]({{ site.baseurl }}/kura/assets/images/admin/untrusted_cert1.png)
    
    ![Untrusted certificate details]({{ site.baseurl }}/kura/assets/images/admin/untrusted_cert2.png)
    
    ![Proceed trusting the source]({{ site.baseurl }}/kura/assets/images/admin/untrusted_cert3.png)
    
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



