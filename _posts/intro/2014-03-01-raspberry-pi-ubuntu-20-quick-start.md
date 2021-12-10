---
layout: page
title:  "Raspberry Pi  - Ubuntu 20 Quick Start"
categories: [intro]
---

[Overview](#overview)

[Enable SSH Access](#enable-ssh-access)

[Eclipse Kura&trade; Installation](#eclipse-kura-installation)

## Overview

This section provides Eclipse Kura&trade; quick installation procedures for the
Raspberry Pi.

{% include alerts.html message="This quickstart will install the version of Kura with the administrative web UI and network  configuration support but not CAN support. For more information on this please visit the [Eclipse Kura download page](https://www.eclipse.org/kura/downloads.php)" %}

This quickstart has been tested using the Ubuntu 20.04.3 LTS Live Server for arm64 architecture flashed on the sd card through [Raspberry Pi Imager](https://www.raspberrypi.com/software/)

The official images could be also found on the [Project Page](https://ubuntu.com/download/raspberry-pi) . Further information on the Ubuntu installation for Raspberry Pi could be found [here](https://ubuntu.com/tutorials/how-to-install-ubuntu-on-your-raspberry-pi).

## Enable SSH Access

On Ubuntu 20.04.3 the ssh access is enabled only for the standard **ubuntu** user. If you desire to remote login as root user, edit the file `/etc/ssh/sshd_config` (using the root permission) adding the line `PermitRootLogin yes`

## Eclipse Kura&trade; Installation

To install Eclipse Kura with its dependencies on the Raspberry Pi, perform the
following steps:

1. Boot the Raspberry Pi with the latest Ubuntu 20.04.3 LTS Server image.

2. Make sure your device is connected to internet. By default, `eth0` lan network interface is configured in DHCP mode.

3. Upgrade the system:

   <pre>
   sudo apt update
   sudo apt upgrade
   </pre>

4. Download the Kura package with:

    <pre> wget http://download.eclipse.org/kura/releases/&lt;version&gt;/kura_&lt;version&gt;_raspberry-pi-ubuntu-20_installer.deb</pre>

    Note: replace \<version\> in the URL above with the version number of the latest release (e.g. 5.1.0).

5. Install Kura with:

    <pre> sudo apt install ./kura_&lt;version&gt;_raspberry-pi-ubuntu-20_installer.deb</pre>

    All the required dependencies will be downloaded and installed.

6. It could happen that 'wlan' interface is "soft blocked" by default and needs to be enabled. To see if it is blocked run:

    <pre>rfkill list</pre>

    and unblock it with:

    <pre>sudo rfkill unblock wlan</pre>


7. Set the right Wi-Fi regulatory domain based on your current world region editing the `/etc/default/crda` and adding the [ISO 3166-1 alpha-2](https://it.wikipedia.org/wiki/ISO_3166-1_alpha-2) code of your region.

8. Reboot the Raspberry Pi with:

    <pre>sudo reboot</pre>

    Kura starts on the target platform after reboot.

9. Kura setups a local web ui that is available using a browser via:

     <pre>https://&lt;device-ip&gt;</pre>

     The browser will prompt the user to accept the connection to an endpoint with a self signed certificate, select `Accept the risk and continue`:
     ![Self Signed Certificate]({{ site.baseurl }}/assets/images/admin/self-signed-certificate-firefox-94.png)

     Once trusted the source, the user will be redirected to a login page where the default **username** is:

     <pre>admin</pre>

     and the default **password** is:

     <pre>admin</pre>
