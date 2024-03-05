#!/bin/bash
#
#  Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
#
#  This program and the accompanying materials are made
#  available under the terms of the Eclipse Public License 2.0
#  which is available at https://www.eclipse.org/legal/epl-2.0/
#
#  SPDX-License-Identifier: EPL-2.0
#
#  Contributors:
#   Eurotech
#

setup_libudev() {
    # create soft link for libudev.so.0 to make it retrocompatible
    # https://unix.stackexchange.com/questions/156776/arch-ubuntu-so-whats-the-deal-with-libudev-so-0
    if [ ! -f /lib/libudev.so.0 ] && [ -f /lib/libudev.so.1 ]; then
        ln -sf /lib/libudev.so.1 /lib/libudev.so.0
    fi

    if uname -m | grep -q arm ; then
        destination="/usr/lib/arm-linux-gnueabihf/libudev.so.1"
        link_name="/usr/lib/arm-linux-gnueabihf/libudev.so.0"
    fi
    if uname -m | grep -q aarch ; then
        destination="/usr/lib/aarch64-linux-gnu/libudev.so.1"
        link_name="/usr/lib/aarch64-linux-gnu/libudev.so.0"
    fi
    if uname -m | grep -q x86_64 ; then
         destination="/usr/lib/x86_64-linux-gnu/libudev.so.1"
        link_name="/usr/lib/x86_64-linux-gnu/libudev.so.0"
    fi

    if [ -f "${destination}" ] && [ ! -f "${link_name}" ]; then
        echo "Setting up symlink ${link_name} -> ${destination}"
        ln -sf "${destination}" "${link_name}"
    fi
}

KURA_PLATFORM=$( uname -m )
sed -i "s/kura_platform/${KURA_PLATFORM}/g" "/opt/eclipse/kura/framework/kura.properties"

setup_libudev

BOARD="generic-device"

if uname -a | grep -q 'raspberry' > /dev/null 2>&1
then
    BOARD="raspberry"
    echo "Customizing installation for Raspberry PI"
fi

if uname -a | grep -q 'up' > /dev/null 2>&1
then
    if uname -n | grep 'intel' > /dev/null 2>&1
    then
        BOARD="intelup2"
        echo "Customizing installation for Intel UP2"
    fi
fi

if uname -a | grep -q 'nvidia' > /dev/null 2>&1
then
    BOARD="jetson-nano"
    echo "Customizing installation for NVIDIA Jetson Nano"
fi

if [ ! -d "/opt/eclipse/kura/user/snapshots/" ]; then
    mkdir /opt/eclipse/kura/user/snapshots/
fi

mv "/opt/eclipse/kura/install/jdk.dio.properties-${BOARD}" "/opt/eclipse/kura/framework/jdk.dio.properties"
mv "/opt/eclipse/kura/install/snapshot_0.xml-${BOARD}" "/opt/eclipse/kura/user/snapshots/snapshot_0.xml"
mv "/opt/eclipse/kura/install/iptables-${BOARD}" "/opt/eclipse/kura/.data/iptables"
sed -i "s/device_name/${BOARD}/g" "/opt/eclipse/kura/framework/kura.properties"
if python3 -V > /dev/null 2>&1
then
    python3 /opt/eclipse/kura/install/find_net_interfaces.py /opt/eclipse/kura/framework/kura.properties
else
    echo "python3 not found. Could not edit the primary netowrk interface name in /opt/eclipse/kura/framework/kura.properties. Defaulted to eth0."
fi

if [ ${BOARD} = "generic-device" ]; then
    # replace snapshot_0, iptables.init, and kura.properties with correct interface names
    if python3 -V > /dev/null 2>&1
    then
        python3 /opt/eclipse/kura/install/find_net_interfaces.py /opt/eclipse/kura/user/snapshots/snapshot_0.xml /opt/eclipse/kura/.data/iptables
    else
        echo "python3 not found. snapshot_0.xml, and iptables.init files may have wrong interface names. Default is eth0 and wlan0. Please correct them manually if they mismatch."
    fi
    
    # dynamic RAM assignment
    RAM_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
    RAM_MB=$(expr $RAM_KB / 1024)
    RAM_MB_FOR_KURA=$(expr $RAM_MB / 4)

    if [ "$RAM_MB" -lt 1024 ]; then
        RAM_REPLACEMENT_STRING="-Xms256m -Xmx256m"
    fi

    echo "Setting kura RAM to ${RAM_REPLACEMENT_STRING}"
    start_scripts_to_change=("start_kura.sh" "start_kura_debug.sh" "start_kura_background.sh")

    RAM_REPLACEMENT_STRING="-Xms${RAM_MB_FOR_KURA}m -Xmx${RAM_MB_FOR_KURA}m"
    for installer_name in "${start_scripts_to_change[@]}"; do
        echo "Updating RAM values for $installer_name"
        sed -i "s/-Xms[0-9]*m -Xmx[0-9]*m/$RAM_REPLACEMENT_STRING/g" "/opt/eclipse/kura/bin/$installer_name"
    done
    
fi
