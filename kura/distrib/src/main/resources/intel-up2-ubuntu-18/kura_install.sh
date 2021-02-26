#!/bin/sh
#
#  Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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

mkdir -p ${INSTALL_DIR}/kura/data

#copy snapshot_0.xml
cp ${INSTALL_DIR}/kura/user/snapshots/snapshot_0.xml ${INSTALL_DIR}/kura/.data/snapshot_0.xml

# setup /etc/sysconfig folder for iptables configuration file
if [ ! -d /etc/sysconfig ]; then
    mkdir /etc/sysconfig
fi

#set up users and grant permissions to them
cp ${INSTALL_DIR}/kura/install/manage_kura_users.sh ${INSTALL_DIR}/kura/.data/manage_kura_users.sh
chmod 700 ${INSTALL_DIR}/kura/.data/manage_kura_users.sh
${INSTALL_DIR}/kura/.data/manage_kura_users.sh -i 

systemctl stop apparmor
systemctl disable apparmor

#set up default networking file
cp ${INSTALL_DIR}/kura/install/network.interfaces /etc/network/interfaces
cp ${INSTALL_DIR}/kura/install/network.interfaces ${INSTALL_DIR}/kura/.data/interfaces

#set up network helper scripts
cp ${INSTALL_DIR}/kura/install/ifup-local.raspbian /etc/network/if-up.d/ifup-local
cp ${INSTALL_DIR}/kura/install/ifdown-local /etc/network/if-down.d/ifdown-local
chmod +x /etc/network/if-up.d/ifup-local
chmod +x /etc/network/if-down.d/ifdown-local

#set up recover default configuration script
cp ${INSTALL_DIR}/kura/install/recover_default_config.init ${INSTALL_DIR}/kura/bin/.recoverDefaultConfig.sh
chmod +x ${INSTALL_DIR}/kura/bin/.recoverDefaultConfig.sh

#set up default firewall configuration
cp /etc/sysconfig/iptables /etc/sysconfig/iptables.esfsave
cp ${BASE_DIR}/${KURA_SYMLINK}/.data/iptables /etc/sysconfig/iptables
sed -i "s|/bin/sh KURA_DIR|/bin/bash ${BASE_DIR}/${KURA_SYMLINK}|" /lib/systemd/system/firewall.service
systemctl daemon-reload
systemctl enable firewall

#set up networking configuration
mac_addr=$(head -1 /sys/class/net/enp2s0/address | tr '[:lower:]' '[:upper:]')
sed "s/^ssid=kura_gateway.*/ssid=kura_gateway_${mac_addr}/" < ${INSTALL_DIR}/kura/install/hostapd.conf > /etc/hostapd-wlp4s0.conf
cp /etc/hostapd-wlp4s0.conf ${INSTALL_DIR}/kura/.data/hostapd-wlp4s0.conf

cp ${INSTALL_DIR}/kura/install/dhcpd-enp2s0.conf /etc/dhcpd-enp2s0.conf
cp ${INSTALL_DIR}/kura/install/dhcpd-enp2s0.conf ${INSTALL_DIR}/kura/.data/dhcpd-enp2s0.conf

cp ${INSTALL_DIR}/kura/install/dhcpd-wlp4s0.conf /etc/dhcpd-wlp4s0.conf
cp ${INSTALL_DIR}/kura/install/dhcpd-wlp4s0.conf ${INSTALL_DIR}/kura/.data/dhcpd-wlp4s0.conf

#set up kuranet.conf
cp ${INSTALL_DIR}/kura/install/kuranet.conf ${INSTALL_DIR}/kura/user/kuranet.conf
cp ${INSTALL_DIR}/kura/install/kuranet.conf ${INSTALL_DIR}/kura/.data/kuranet.conf

#set up bind/named
cp ${INSTALL_DIR}/kura/install/named.conf /etc/bind/named.conf
mkdir -p /var/named
chown -R bind /var/named
cp ${INSTALL_DIR}/kura/install/named.ca /var/named/
cp ${INSTALL_DIR}/kura/install/named.rfc1912.zones /etc/
if [ ! -f "/etc/bind/rndc.key" ] ; then
	rndc-confgen -r /dev/urandom -a
fi

#set up logrotate - no need to restart as it is a cronjob
cp ${INSTALL_DIR}/kura/install/logrotate.conf /etc/logrotate.conf
cp ${INSTALL_DIR}/kura/install/kura.logrotate /etc/logrotate.d/kura

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

keytool -genkey -alias localhost -keyalg RSA -keysize 2048 -keystore /opt/eclipse/kura/user/security/httpskeystore.ks -deststoretype pkcs12 -dname "CN=Kura, OU=Kura, O=Eclipse Foundation, L=Ottawa, S=Ontario, C=CA" -ext ku=digitalSignature,nonRepudiation,keyEncipherment,dataEncipherment,keyAgreement,keyCertSign -ext eku=serverAuth,clientAuth,codeSigning,timeStamping -validity 1000 -storepass changeit -keypass changeit  