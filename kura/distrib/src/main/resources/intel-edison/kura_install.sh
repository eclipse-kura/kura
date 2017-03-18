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

INSTALL_DIR=/home/root/eclipse

#create known kura install location
ln -sf ${INSTALL_DIR}/kura_* ${INSTALL_DIR}/kura

#create /etc/init.d/ folder
if [ ! -d /etc/init.d ]; then
    mkdir -p /etc/init.d
fi

#set up Kura init
sed "s|^KURA_DIR=.*|KURA_DIR=${INSTALL_DIR}/kura|" ${INSTALL_DIR}/kura/install/kura.init.yocto > /etc/init.d/kura
chmod +x /etc/init.d/kura
chmod +x ${INSTALL_DIR}/kura/bin/*.sh

# setup snapshot_0 recovery folder
if [ ! -d ${INSTALL_DIR}/kura/.data ]; then
    mkdir ${INSTALL_DIR}/kura/.data
fi

# disable intel edison network manager
systemctl disable systemd-networkd
systemctl disable systemd-resolved
systemctl disable systemd-hostnamed
systemctl disable hostapd
systemctl disable wpa_supplicant

#set up default networking file
if [ ! -d /etc/network ]; then
    mkdir /etc/network
fi

cp ${INSTALL_DIR}/kura/install/network.interfaces.intel.edison /etc/network/interfaces
cp ${INSTALL_DIR}/kura/install/network.interfaces.intel.edison ${INSTALL_DIR}/kura/.data/interfaces

#set up network helper scripts
if [ ! -d /etc/network/if-down.d ]; then
    mkdir /etc/network/if-down.d
fi

if [ ! -d /etc/network/if-post-down.d ]; then
    mkdir /etc/network/if-post-down.d
fi

if [ ! -d /etc/network/if-up.d ]; then
    mkdir /etc/network/if-up.d
fi

if [ ! -d /etc/network/if-pre-up.d ]; then
    mkdir /etc/network/if-pre-up.d
fi

if [ ! -d /etc/sysconfig ]; then
    mkdir /etc/sysconfig
fi

cp ${INSTALL_DIR}/kura/install/ifup-local.debian /etc/network/if-up.d/ifup-local
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
mac_addr=$(head /sys/class/net/usb0/address | tr '[:lower:]' '[:upper:]')
sed "s/^ssid=kura_gateway.*/ssid=kura_gateway_${mac_addr}/" < ${INSTALL_DIR}/kura/install/hostapd.conf > /etc/hostapd/hostapd.conf
cp /etc/hostapd/hostapd.conf ${INSTALL_DIR}/kura/.data/hostapd.conf

cp ${INSTALL_DIR}/kura/install/udhcpd-for-hostapd.service /lib/systemd/system/udhcpd-for-hostapd.service

cp ${INSTALL_DIR}/kura/install/udhcpd-usb0.conf /etc/udhcpd-usb0.conf
cp ${INSTALL_DIR}/kura/install/udhcpd-usb0.conf ${INSTALL_DIR}/kura/.data/udhcpd-usb0.conf

cp ${INSTALL_DIR}/kura/install/udhcpd-wlan0.conf /etc/udhcpd-wlan0.conf
cp ${INSTALL_DIR}/kura/install/udhcpd-wlan0.conf ${INSTALL_DIR}/kura/.data/udhcpd-wlan0.conf

#set up kuranet.conf
cp ${INSTALL_DIR}/kura/install/kuranet.conf ${INSTALL_DIR}/kura/data/kuranet.conf
cp ${INSTALL_DIR}/kura/install/kuranet.conf ${INSTALL_DIR}/kura/.data/kuranet.conf

#set up dos2unix
if [ ! -f "/usr/bin/dos2unix" ] ; then
    cp ${INSTALL_DIR}/kura/install/dos2unix.bin /usr/bin/dos2unix
    chmod +x /usr/bin/dos2unix
fi

#set up bind/named
if [ ! -f "/usr/sbin/named" ] ; then
    cp ${INSTALL_DIR}/kura/install/named.bin /usr/sbin/named
    chmod +x /usr/sbin/named
fi

if [ ! -f "/usr/sbin/rndc" ] ; then
    cp ${INSTALL_DIR}/kura/install/rndc.bin /usr/sbin/rndc
    chmod +x /usr/sbin/rndc
fi

if [ ! -f "/usr/sbin/rndc-confgen" ] ; then
    cp ${INSTALL_DIR}/kura/install/rndc-confgen.bin /usr/sbin/rndc-confgen
    chmod +x /usr/sbin/rndc-confgen
fi

cp ${INSTALL_DIR}/kura/install/bind.init /etc/init.d/bind
chmod +x /etc/init.d/bind

#if [ ! -d /etc/bind ]; then
#    mkdir /etc/bind
#fi
cp ${INSTALL_DIR}/kura/install/named.conf /etc/named.conf
if [ ! -d "/var/named" ]; then
   mkdir /var/named
fi
cp ${INSTALL_DIR}/kura/install/named.ca /var/named/
cp ${INSTALL_DIR}/kura/install/named.rfc1912.zones /etc/
if [ ! -f "/etc/rndc.key" ] ; then
	rndc-confgen -r /dev/urandom -a
fi

#set up monit <IAB> commented out for now
#if [ -d "/etc/monit/conf.d" ] ; then
#    cp ${INSTALL_DIR}/kura/install/monitrc.raspbian /etc/monit/conf.d/
#fi

#set up /etc/group
GroupFile=/etc/group
if ! grep -q 'wheel' $GroupFile; then
   echo 'wheel:x:10:root' >> $GroupFile
fi
if ! grep -q 'bind' $GroupFile; then
   echo 'bind:x:10:root' >> $GroupFile
fi

#set up runlevels to start/stop Kura by default
if [ ! -d /etc/rc0.d ]; then
    mkdir -p /etc/rc0.d
fi
cd /etc/rc0.d
#ln -sf ../init.d/monit K08monit
ln -sf ../init.d/kura K09kura
if [ ! -d /etc/rc6.d ]; then
    mkdir -p /etc/rc6.d
fi
cd /etc/rc6.d
#ln -sf ../init.d/monit K08monit
ln -sf ../init.d/kura K09kura

#set up runlevels to start Kura by default
if [ ! -d /etc/rc2.d ]; then
    mkdir -p /etc/rc2.d
fi
cd /etc/rc2.d
ln -sf ../init.d/firewall S25firewall
ln -sf ../init.d/kura S99kura			# this is not needed since monit handles this
#ln -sf ../init.d/monit S99monit
if [ ! -d /etc/rc3.d ]; then
    mkdir -p /etc/rc3.d
fi
cd ../rc3.d
ln -sf ../init.d/firewall S25firewall
ln -sf ../init.d/kura S99kura			# this is not needed since monit handles this
#ln -sf ../init.d/monit S99monit
if [ ! -d /etc/rc5.d ]; then
    mkdir -p /etc/rc5.d
fi
cd ../rc5.d
ln -sf ../init.d/firewall S25firewall
ln -sf ../init.d/kura S99kura			# this is not needed since monit handles this
#ln -sf ../init.d/monit S99monit


#set up logrotate - no need to restart as it is a cronjob
cp ${INSTALL_DIR}/kura/install/logrotate.conf /etc/logrotate.conf
if [ ! -d /etc/logrotate.d/ ]; then
    mkdir -p /etc/logrotate.d/
fi
cp ${INSTALL_DIR}/kura/install/kura.logrotate /etc/logrotate.d/kura

#change ps command in start scripts
sed -i 's/ps ax/ps/g' ${INSTALL_DIR}/kura/bin/start_kura.sh
sed -i 's/ps ax/ps/g' ${INSTALL_DIR}/kura/bin/start_kura_background.sh
sed -i 's/ps ax/ps/g' ${INSTALL_DIR}/kura/bin/start_kura_debug.sh

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