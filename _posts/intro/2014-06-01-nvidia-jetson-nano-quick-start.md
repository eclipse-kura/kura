---
layout: page
title:  "NVIDIA Jetson Nano&trade; - Quick Start"
categories: [intro]
---

[Overview](#overview)

[Eclipse Kura&trade; Installation](#eclipse-kura-installation)

## Overview

This section provides Eclipse Kura&trade; quick installation procedures for the NVIDIA Jetson Nano&trade;.

{% include alerts.html message="This quickstart will install the version of Kura with the administrative web UI and network  configuration support but not CAN support. For more information on this please visit the [Eclipse Kura download page](https://www.eclipse.org/kura/downloads.php)" %}

This quickstart has been tested using the following image:

<pre>jetson-nano-jp46-sd-card-image.zip</pre>

downloaded from

<pre>https://developer.nvidia.com/jetson-nano-sd-card-image</pre>

and with the image burned on a SD card with [Etcher](https://www.balena.io/etcher/)

The official images can be found on the [Jetson Nano Developer Kit Getting Starteg Guide](https://developer.nvidia.com/embedded/learn/get-started-jetson-nano-devkit#write). Further information on the Ubuntu installation for the NVIDIA Jetson Nano&trade; can be found [here](https://developer.nvidia.com/embedded/learn/get-started-jetson-nano-devkit#intro).

## Eclipse Kura&trade; Installation

To install Eclipse Kura with its dependencies on the NVIDIA Jetson Nano&trade;, perform the
following steps:

1. Boot the NVIDIA Jetson Nano&trade; with the latest Jetson Nano Developer Kit SD Card image.

2. Make sure your device is connected to internet. By default, `eth0` lan network interface is configured in DHCP mode.

3. Upgrade the system:

   <pre>
   sudo apt update
   sudo apt upgrade
   </pre>

4. Download the Kura package with:

    <pre> wget http://download.eclipse.org/kura/releases/&lt;version&gt;/kura_&lt;version&gt;_nvidia-jetson-nano_installer.deb</pre>

    Note: replace \<version\> in the URL above with the version number of the latest release (e.g. 5.1.0).

5. Install Kura with:

    <pre> sudo apt install ./kura_&lt;version&gt;_nvidia-jetson-nano_installer.deb</pre>

    All the required dependencies will be downloaded and installed.

6. Reboot the NVIDIA Jetson Nano&trade; with:

    <pre>sudo reboot</pre>

    Kura starts on the target platform after reboot.

7. Kura setups a local web ui that is available using a browser via:

    <pre>https://&lt;device-ip&gt;</pre>

    The browser will prompt the user to accept the connection to an endpoint with an untrusted certificate:
    ![Untrusted certificate page]({{ site.baseurl }}/assets/images/admin/untrusted_cert1.png)

    ![Untrusted certificate details]({{ site.baseurl }}/assets/images/admin/untrusted_cert2.png)

    ![Proceed trusting the source]({{ site.baseurl }}/assets/images/admin/untrusted_cert3.png)

    Once trusted the source, the user will be redirected to a login page where the default **username** is:

    <pre>admin</pre>

    and the default **password** is:

    <pre>admin</pre>
