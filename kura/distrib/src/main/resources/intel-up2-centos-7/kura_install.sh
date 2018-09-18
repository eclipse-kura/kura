#!/bin/bash
#
# Copyright (c) 2016, 2018 Red Hat Inc and others
#
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     Red Hat Inc
#	  Eurotech
#

set -e

INSTALL_DIR=/opt/eclipse

#create known kura install location
ln -sf ${INSTALL_DIR}/kura_* ${INSTALL_DIR}/kura

#set up Kura init
sed "s|INSTALL_DIR|${INSTALL_DIR}|" ${INSTALL_DIR}/kura/install/kura.service > /lib/systemd/system/kura.service
systemctl daemon-reload
systemctl enable kura
chmod +x ${INSTALL_DIR}/kura/bin/*.sh

# setup snapshot_0 recovery folder
if [ ! -d ${INSTALL_DIR}/kura/.data ]; then
    mkdir ${INSTALL_DIR}/kura/.data
fi

# disable network manager
systemctl stop NetworkManager.service
systemctl disable NetworkManager.service || true
systemctl disable systemd-networkd || true
systemctl disable systemd-resolved || true
systemctl disable systemd-hostnamed || true
systemctl disable hostapd || true
systemctl disable wpa_supplicant || true

# remove existing /etc/resolv.conf file (it should be a link)
rm /etc/resolv.conf

#set up recover default configuration script
cp ${INSTALL_DIR}/kura/install/recover_default_config.init ${INSTALL_DIR}/kura/bin/.recoverDefaultConfig.sh
chmod +x ${INSTALL_DIR}/kura/bin/.recoverDefaultConfig.sh

cp ${INSTALL_DIR}/kura/install/iptables.init /etc/sysconfig/iptables
cp /etc/sysconfig/iptables ${INSTALL_DIR}/kura/.data/iptables

# Mask firewalld and enable/start iptables
systemctl mask firewalld
systemctl enable iptables
systemctl stop firewalld
systemctl start iptables

#set up networking configuration
mac_addr=$(head /sys/class/net/enp2s0/address | tr '[:lower:]' '[:upper:]')
sed "s/^ssid=kura_gateway.*/ssid=kura_gateway_${mac_addr}/" < ${INSTALL_DIR}/kura/install/hostapd.conf > /etc/hostapd-wlp4s0.conf
sed -i "s/^interface=wlan0/interface=wlp4s0/" /etc/hostapd-wlp4s0.conf
cp /etc/hostapd-wlp4s0.conf ${INSTALL_DIR}/kura/.data/hostapd-wlp4s0.conf

cp ${INSTALL_DIR}/kura/install/dhcpd-enp2s0.conf /etc/dhcpd-enp2s0.conf
cp ${INSTALL_DIR}/kura/install/dhcpd-enp2s0.conf ${INSTALL_DIR}/kura/.data/dhcpd-enp2s0.conf

cp ${INSTALL_DIR}/kura/install/dhcpd-wlp4s0.conf /etc/dhcpd-wlan0.conf
cp ${INSTALL_DIR}/kura/install/dhcpd-wlp4s0.conf ${INSTALL_DIR}/kura/.data/dhcpd-wlan0.conf

#set up kuranet.conf
cp ${INSTALL_DIR}/kura/install/kuranet.conf ${INSTALL_DIR}/kura/user/kuranet.conf
cp ${INSTALL_DIR}/kura/install/kuranet.conf ${INSTALL_DIR}/kura/.data/kuranet.conf

OLD_PATH=$(pwd)
cd ${INSTALL_DIR}/kura/install/
semodule -i selinuxKura.pp
cd ${OLD_PATH}

mkdir -p ${INSTALL_DIR}/kura/data

#copy snapshot_0.xml
cp ${INSTALL_DIR}/kura/user/snapshots/snapshot_0.xml ${INSTALL_DIR}/kura/.data/snapshot_0.xml

#set up ifcfg files
cp ${INSTALL_DIR}/kura/install/ifcfg-enp2s0 /etc/sysconfig/network-scripts/ifcfg-enp2s0
cp ${INSTALL_DIR}/kura/install/ifcfg-enp2s0 ${INSTALL_DIR}/kura/.data/ifcfg-enp2s0

cp ${INSTALL_DIR}/kura/install/ifcfg-enp3s0 /etc/sysconfig/network-scripts/ifcfg-enp3s0
cp ${INSTALL_DIR}/kura/install/ifcfg-enp3s0 ${INSTALL_DIR}/kura/.data/ifcfg-enp3s0

cp ${INSTALL_DIR}/kura/install/ifcfg-wlp4s0 /etc/sysconfig/network-scripts/ifcfg-wlp4s0
cp ${INSTALL_DIR}/kura/install/ifcfg-wlp4s0 ${INSTALL_DIR}/kura/.data/ifcfg-wlp4s0

# Set up logrotate
cp ${INSTALL_DIR}/kura/install/logrotate.conf /etc/logrotate.conf
if [ ! -d /etc/logrotate.d/ ]; then
    mkdir -p /etc/logrotate.d/
fi
cp ${INSTALL_DIR}/kura/install/kura.logrotate /etc/logrotate.d/kura

# Setup tmpfiles.d
cp ${INSTALL_DIR}/kura/install/kura-tmpfiles.conf /usr/lib/tmpfiles.d/kura.conf

# execute patch_sysctl.sh from installer install folder
chmod 700 ${INSTALL_DIR}/kura/install/patch_sysctl.sh 
${INSTALL_DIR}/kura/install/patch_sysctl.sh ${INSTALL_DIR}/kura/install/sysctl.kura.conf /etc/sysctl.conf

if ! [ -d /sys/class/net ]
then
 sysctl -p || true
else
 sysctl -w net.ipv6.conf.all.disable_ipv6=1
 sysctl -w net.ipv6.conf.default.disable_ipv6=1
 for INTERFACE in $(ls /sys/class/net)
 do
 	sysctl -w net.ipv6.conf.${INTERFACE}.disable_ipv6=1
 done
fi
