---
layout: page
title:  "Raspberry Pi - Raspbian Quick Start"
categories: [intro]
---

[Overview](#overview)

[Enable SSH Access](#enable-ssh-access)

[Eclipse Kura&trade; Installation](#eclipse-kura-installation)

## Overview

This section provides Eclipse Kura&trade; quick installation procedures for the
Raspberry Pi and the Kura development environment.

{% include alerts.html message="This quickstart will install the version of Kura with the administrative web UI and network  configuration support but not CAN support. For more information on this please visit the [Eclipse Kura download page](https://www.eclipse.org/kura/downloads.php)" %}

This quickstart has been tested using the following image:

<pre>2021-10-30-raspios-bullseye-armhf.zip</pre>

downloaded from

<pre>https://www.raspberrypi.com/software/operating-systems/#raspberry-pi-os-32-bit</pre>

## Enable SSH Access

The ssh server is disabled by default on Raspbian images released after November 2016,
in order to enable it to follow the instructions available at the following URL:

<pre>https://www.raspberrypi.org/documentation/remote-access/ssh/</pre>

## Eclipse Kura&trade; Installation

{% include alerts.html message="The last Raspbian Stretch adopts the new <a href='https://www.freedesktop.org/wiki/Software/systemd/PredictableNetworkInterfaceNames/' target='_blank'>Consistent Network Device Naming</a>. To correctly run Eclipse Kura on the Raspberry Pi, it should be disabled adding the ```net.ifnames=0``` parameter at the end of the /boot/cmdline.txt file." %}

To install Eclipse Kura with its dependencies on the Raspberry Pi, perform the
following steps:

1. Boot the Raspberry Pi with the latest Raspbian image (starting from release 5.1.0 Kura only supports Debian 11 or above).

2. Make sure your device is connected to internet. By default, `eth0` lan network interface is configured in DHCP mode.

3. Upgrade the system:

   <pre>
   sudo apt update
   sudo apt upgrade
   </pre>
   
4. Download the Kura package with:

    <pre>wget http://download.eclipse.org/kura/releases/&lt;version&gt;/kura_&lt;version&gt;_raspberry-pi_installer.deb</pre>

    Note: replace \<version\> in the URL above with the version number of the latest release (e.g. 5.1.0).

5. Install Kura with: 

    <pre>sudo apt-get install ./kura_&lt;version&gt;_raspberry-pi_installer.deb</pre>

6. It could happen that 'wlan' interface is "soft blocked" by default and needs to be enabled. To see if it is blocked run:

    <pre>rfkill list</pre>

    and unblock it with:

    <pre>sudo rfkill unblock wlan</pre>

7. Set the right Wi-Fi regulatory domain based on your current world region following the instructions <a href="https://www.raspberrypi.org/documentation/computers/configuration.html#using-the-desktop" target="_blank">here</a> In case of problems, you could try to edit the `/etc/default/crda` adding the <a href="https://it.wikipedia.org/wiki/ISO_3166-1_alpha-2" target="_blank">ISO 3166-1 alpha-2</a> code of your region

8. Reboot the Raspberry Pi with:

    <pre>sudo reboot</pre>

    Kura starts on the target platform after reboot.

9. Kura setups a local web ui that is available using a browser via:

     <pre>https://&lt;device-ip&gt;</pre>

     The browser will prompt the user to accept the connection to an endpoint with an untrusted certificate:
     ![Untrusted certificate page]({{ site.baseurl }}/assets/images/admin/untrusted_cert1.png)

     ![Untrusted certificate details]({{ site.baseurl }}/assets/images/admin/untrusted_cert2.png)

     ![Proceed trusting the source]({{ site.baseurl }}/assets/images/admin/untrusted_cert3.png)

     Once trusted the source, the user will be redirected to a login page where the default **username** is:
     
     <pre>admin</pre>

     and the default **password** is:

     <pre>admin</pre>

