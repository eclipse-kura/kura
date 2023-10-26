#!/bin/bash
#
#  Copyright (c) 2023 Eurotech and/or its affiliates and others
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

KURA_PLATFORM=$( uname -m )
sed -i "s/kura_platform/${KURA_PLATFORM}/g" "/opt/eclipse/kura/framework/kura.properties"

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
    
    # dynamic ram assignment
    RAM_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
    RAM_MB=$(expr $RAM_KB / 1024)
    RAM_MB_FOR_KURA=$(expr $RAM_MB / 4)
    RAM_REPLACEMENT_STRING="-Xms${RAM_MB_FOR_KURA}m -Xmx${RAM_MB_FOR_KURA}m"

    if RAM_MB > 1024
    then
        echo "Setting kura ram -Xms and -Xmx to ${RAM_MB_FOR_KURA}m"
        sed -i "s/-Xms[0-9]*m -Xmx[0-9]*m/$RAM_REPLACEMENT_STRING/g" /opt/eclipse/kura/bin/start_kura.sh
        sed -i "s/-Xms[0-9]*m -Xmx[0-9]*m/$RAM_REPLACEMENT_STRING/g" /opt/eclipse/kura/bin/start_kura_debug.sh
        sed -i "s/-Xms[0-9]*m -Xmx[0-9]*m/$RAM_REPLACEMENT_STRING/g" /opt/eclipse/kura/bin/start_kura_background.sh
    else
        echo "Leaving kura ram as the default set in installer profile"
    fi
fi
