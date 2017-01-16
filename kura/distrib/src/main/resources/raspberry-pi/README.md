Manual steps to install Kura 3.0 on Raspbian Jessie / Raspberry Pi
==

Download and install Raspbian Jessie from https://www.raspberrypi.org/downloads/raspbian/

Enable the SSH daemon on the Pi by executing:

    sudo systemctl enable ssh
    sudo systemctl start ssh

Install the `gdebi` command line tool:

    sudo apt-get install gdebi-core

Then bootstrap Kura with the `kura_*.deb` file:

    sudo gdebi kura_*.deb