# Installation of Kura on Fedora / Raspberry Pi

1. Download Fedora Minimal image from [here](https://arm.fedoraproject.org/).
1. Copy downloaded image to your sdcard. There are many references on how to do this for your development platform,
such as the [Raspberry Pi installation guide](https://www.raspberrypi.org/documentation/installation/installing-images/)
      The command for the Fedora 27 image on Linux or Mac is something like:
      ```
      xzcat ~/Downloads/fedora/Fedora-Minimal-armhfp-27-1.6-sda.raw.xz | sudo dd bs=1M of=/dev/rdisk2
      ```
1. Load the sdcard above and boot the RPi.
1. Scan your network for the RPi (if using DHCP).
      ```
      sudo nmap -sP 192.168.0.0/24 | awk '/^Nmap/{ip=$NF}/B8:27:EB/{print ip}'
      ```
1. SSH into the RPi. You will need to be plugged into a wired ethernet network at this point,
but after this step you could configure wifi and repeat the discovery step.
1. Grow the root file system as described [here](https://fedoraproject.org/wiki/Architectures/ARM/F25/Installation#Resize_the_Root_Filesystem),
also see section "How do I enlarge the root partition?" in the [FAQ](https://fedoraproject.org/wiki/Architectures/ARM/F25/Installation#FAQ)
1. Install pre-requisites:
      ```
      dnf -y install which unzip psmisc tar telnet bind iptables iptables-services dhcp hostapd dos2unix wireless-tools net-tools usbutils java-1.8.0-openjdk-headless
      ```
1. Install a version of Java. See [Installing Java](#installing-java) below.
1. Copy `kura_3.*_fedora_installer.sh` to the Fedora machine
1. Run `kura_3.*_fedora_installer.sh` as root to install
1. `systemctl start kura`
1. Connect browser to host on standard http port

## Installing Java

Fedora brings OpenJDK by default, which can be installed using:

    dnf -y install java-1.8.0-openjdk-headless

This is working fine on x86 based architectures. However on ARM it is missing the JIT
and thus performance is really bad as the JVM has to switch back to interpreted mode.

The solution to this can be to install the Oracle JVM for ARM (please note that it might require
a separate license) or using some alternative JVM such as Eclipse OpenJ9 or Azul.

In order for the Kura installer to properly run, you will need to ensure that the `java`
executable can be located in the "PATH" during the installation. Later on it can start up
properly when found in `/opt/jvm`.
