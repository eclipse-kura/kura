Manual steps to install Kura 3.0 on Fedora 25 / Raspberry Pi
==

1. Download Fedora Minimal image from [here](https://arm.fedoraproject.org/).
1. Copy downloaded image to your sdcard. There are many references on how to do this for your development platform,
such as the [Raspberry Pi installation guide](https://www.raspberrypi.org/documentation/installation/installing-images/)
      The command for the Fedora 25 image on Linux or Mac is something like:
      ```
      xzcat ~/Downloads/fedora/Fedora-Minimal-armhfp-25-1.3-sda.raw.xz | sudo dd bs=1m of=/dev/rdisk2
      ```
1. Load the sdcard above and boot the RPi.
1. Scan your network for the RPi (if using DHCP).
      ```
      sudo nmap -sP 192.168.0.0/24 | awk '/^Nmap/{ip=$NF}/B8:27:EB/{print ip}'
      ```
1. SSH into the RPi. You will need to be plugged into a wired ethernet network at this point,
but after this step you could configure wifi and repeat the discovery step.
1. Grow the root file system as described [here](https://fedoraproject.org/wiki/Architectures/ARM/F25/Installation#Resize_the_Root_Filesystem)
1. Install pre-requisites:
      ```
      dnf -y install unzip psmisc tar telnet bind iptables iptables-services dhcp hostapd dos2unix wireless-tools net-tools usbutils java-1.8.0-openjdk-headless
      ``` 
1. Copy `kura_3.0.0_fedora25_installer.sh` to the Fedora machine
1. Run `kura_3.0.0_fedora25_installer.sh` as root to install
1. `systemctl start kura`
1. Connect browser to host on standard http port
