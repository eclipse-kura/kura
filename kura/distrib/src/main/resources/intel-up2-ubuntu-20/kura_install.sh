#!/bin/sh
#
#  Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
cp ${INSTALL_DIR}/kura/install/iptables.init ${INSTALL_DIR}/kura/.data/iptables
chmod 644 ${INSTALL_DIR}/kura/.data/iptables
cp ${INSTALL_DIR}/kura/.data/iptables /etc/sysconfig/iptables
cp ${INSTALL_DIR}/kura/install/firewall.init ${INSTALL_DIR}/kura/bin/firewall
chmod 755 ${INSTALL_DIR}/kura/bin/firewall
cp ${INSTALL_DIR}/kura/install/firewall.service /lib/systemd/system/firewall.service
chmod 644 /lib/systemd/system/firewall.service
sed -i "s|/bin/sh KURA_DIR|/bin/bash ${INSTALL_DIR}/kura|" /lib/systemd/system/firewall.service
systemctl daemon-reload
systemctl enable firewall

#copy snapshot_0.xml
cp ${INSTALL_DIR}/kura/user/snapshots/snapshot_0.xml ${INSTALL_DIR}/kura/.data/snapshot_0.xml

#disable NTP service
if command -v timedatectl > /dev/null ;
  then
    timedatectl set-ntp false
fi

# Prevent time sync services from starting
systemctl stop systemd-timesyncd
systemctl disable systemd-timesyncd
# Prevent time sync with chrony from starting.
systemctl stop chrony
systemctl disable chrony

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

#assigning kuranet.conf files ownership to kurad
chown kurad:kurad ${INSTALL_DIR}/kura/user/kuranet.conf
chown kurad:kurad ${INSTALL_DIR}/kura/.data/kuranet.conf

#set up bind/named
cp ${INSTALL_DIR}/kura/install/named.conf /etc/bind/named.conf
mkdir -p /var/named
chown -R bind /var/named
cp ${INSTALL_DIR}/kura/install/named.ca /var/named/
cp ${INSTALL_DIR}/kura/install/named.rfc1912.zones /etc/
cp ${INSTALL_DIR}/kura/install/usr.sbin.named /etc/apparmor.d/
if [ ! -f "/etc/bind/rndc.key" ] ; then
    rndc-confgen -r /dev/urandom -a
fi
chown bind:bind /etc/bind/rndc.key
chmod 600 /etc/bind/rndc.key

#set up logrotate - no need to restart as it is a cronjob
cp ${INSTALL_DIR}/kura/install/logrotate.conf /etc/logrotate.conf
cp ${INSTALL_DIR}/kura/install/kura.logrotate /etc/logrotate.d/kura

# disable dhcpcd service - kura is the network manager
systemctl stop dhcpcd
systemctl disable dhcpcd

# disable isc-dhcp-server service - kura is the network manager
systemctl stop isc-dhcp-server
systemctl disable isc-dhcp-server

#disable netplan
systemctl disable systemd-networkd.socket 
systemctl disable systemd-networkd
systemctl disable networkd-dispatcher
systemctl disable systemd-networkd-wait-online
systemctl mask systemd-networkd.socket 
systemctl mask systemd-networkd
systemctl mask networkd-dispatcher
systemctl mask systemd-networkd-wait-online

#disable wpa_supplicant
systemctl stop wpa_supplicant
systemctl disable wpa_supplicant

#assigning possible .conf files ownership to kurad
PATTERN="/etc/dhcpd*.conf* /etc/resolv.conf* /etc/wpa_supplicant*.conf* /etc/hostapd*.conf*"
for FILE in $(ls $PATTERN 2>/dev/null)
do
  chown kurad:kurad $FILE
done

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