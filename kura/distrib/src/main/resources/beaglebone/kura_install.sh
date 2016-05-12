#!/bin/sh
#
# Copyright (c) 2011, 2014 Eurotech and/or its affiliates
#
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   Eurotech
#

INSTALL_DIR=/opt/eclipse

#create known kura install location
ln -sf ${INSTALL_DIR}/kura_* ${INSTALL_DIR}/kura

#set up Kura init
cp ${INSTALL_DIR}/kura/install/kura.init.raspbian /etc/init.d/kura
chmod +x /etc/init.d/kura
chmod +x ${INSTALL_DIR}/kura/bin/*.sh

# setup snapshot_0 recovery folder
if [ ! -d ${INSTALL_DIR}/kura/.data ]; then
    mkdir ${INSTALL_DIR}/kura/.data
fi

#set up default networking file
cp ${INSTALL_DIR}/kura/install/network.interfaces.raspbian /etc/network/interfaces
cp ${INSTALL_DIR}/kura/install/network.interfaces.raspbian ${INSTALL_DIR}/kura/.data/interfaces

#set up network helper scripts
cp ${INSTALL_DIR}/kura/install/ifup-local.raspbian /etc/network/if-up.d/ifup-local
cp ${INSTALL_DIR}/kura/install/ifdown-local /etc/network/if-down.d/ifdown-local
chmod +x /etc/network/if-up.d/ifup-local
chmod +x /etc/network/if-down.d/ifdown-local

#set up recover default configuration script
cp ${INSTALL_DIR}/kura/install/recover_default_config.init ${INSTALL_DIR}/kura/.data/.recoverDefaultConfig.sh
chmod +x ${INSTALL_DIR}/kura/.data/.recoverDefaultConfig.sh

#set up default firewall configuration
cp ${INSTALL_DIR}/kura/install/firewall.init /etc/init.d/firewall
chmod +x /etc/init.d/firewall
cp ${INSTALL_DIR}/kura/install/iptables.init /etc/sysconfig/iptables
cp /etc/sysconfig/iptables ${INSTALL_DIR}/kura/.data/iptables

#set up networking configuration
mac_addr=$(head -1 /sys/class/net/eth0/address | tr '[:lower:]' '[:upper:]')
sed "s/^ssid=kura_gateway.*/ssid=kura_gateway_${mac_addr}/" < ${INSTALL_DIR}/kura/install/hostapd.conf > /etc/hostapd-wlan0.conf
cp /etc/hostapd-wlan0.conf ${INSTALL_DIR}/kura/.data/hostapd-wlan0.conf

cp ${INSTALL_DIR}/kura/install/dhcpd-eth0.conf /etc/dhcpd-eth0.conf
cp ${INSTALL_DIR}/kura/install/dhcpd-eth0.conf ${INSTALL_DIR}/kura/.data/dhcpd-eth0.conf

#set up kuranet.conf
cp ${INSTALL_DIR}/kura/install/kuranet.conf ${INSTALL_DIR}/kura/data/kuranet.conf
cp ${INSTALL_DIR}/kura/install/kuranet.conf ${INSTALL_DIR}/kura/.data/kuranet.conf

#set up bind/named
cp ${INSTALL_DIR}/kura/install/named.conf /etc/bind/named.conf
cp ${INSTALL_DIR}/kura/install/named.ca /var/named/
cp ${INSTALL_DIR}/kura/install/named.rfc1912.zones /etc/
if [ ! -f "/etc/bind/rndc.key" ] ; then
	rndc-confgen -r /dev/urandom -a
fi

#set up monit
cp ${INSTALL_DIR}/kura/install/monit.init.raspbian /etc/init.d/monit
chmod +x /etc/init.d/monit
cp ${INSTALL_DIR}/kura/install/monitrc.raspbian /etc/monitrc
chmod 700 /etc/monitrc

#set up runlevels to start/stop Kura by default
update-rc.d firewall defaults
update-rc.d kura defaults

#set up logrotate - no need to restart as it is a cronjob
cp ${INSTALL_DIR}/kura/install/logrotate.conf /etc/logrotate.conf
cp ${INSTALL_DIR}/kura/install/kura.logrotate /etc/logrotate.d/kura
