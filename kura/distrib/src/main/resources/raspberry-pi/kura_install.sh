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


#create known kura install location
ln -sf /opt/eclipse/kura-* /opt/eclipse/kura

#set up Kura init
cp /opt/eclipse/kura/install/kura.init.raspbian /etc/init.d/kura
chmod +x /etc/init.d/kura
chmod +x /opt/eclipse/kura/bin/*.sh

#set up default networking file
cp /opt/eclipse/kura/install/network.interfaces.raspbian /etc/network/interfaces

#set up network helper scripts
cp /opt/eclipse/kura/install/ifup-local.raspbian /etc/network/if-up.d/ifup-local
cp /opt/eclipse/kura/install/ifdown-local /etc/network/if-down.d/ifdown-local
chmod +x /etc/network/if-up.d/ifup-local
chmod +x /etc/network/if-down.d/ifdown-local

#set up default firewall configuration
cp /opt/eclipse/kura/install/firewall.init /etc/init.d/firewall
chmod +x /etc/init.d/firewall

#set up networking configuration
mac_addr=$(head -1 /sys/class/net/eth0/address | tr '[:lower:]' '[:upper:]')
sed "s/^ssid=kura_gateway.*/ssid=kura_gateway_${mac_addr}/" < /opt/eclipse/kura/install/hostapd.conf > /etc/hostapd.conf
cp /opt/eclipse/kura/install/dhcpd-eth0.conf /etc/dhcpd-eth0.conf
cp /opt/eclipse/kura/install/dhcpd-wlan0.conf /etc/dhcpd-wlan0.conf

#set up kuranet.conf
cp /opt/eclipse/kura/install/kuranet.conf /opt/eclipse/data/kuranet.conf

#set up bind/named
cp /opt/eclipse/kura/install/named.conf /etc/bind/named.conf
mkdir /var/named
chown -R bind /var/named
touch /var/log/named.log
chown -R bind /var/log/named.log
cp /opt/eclipse/kura/install/named.ca /var/named/
cp /opt/eclipse/kura/install/named.rfc1912.zones /etc/
cp /opt/eclipse/kura/install/usr.sbin.named /etc/apparmor.d/
if [ ! -f "/etc/bind/rndc.key" ] ; then
	rndc-confgen -r /dev/urandom -a
fi

#set up monit
if [ -d "/etc/monit/conf.d" ] ; then
    cp /opt/eclipse/kura/install/monitrc.raspbian /etc/monit/conf.d/
fi

# set up /opt/eclipse/recover_dflt_kura_config.sh
cp /opt/eclipse/kura/install/recover_dflt_kura_config.sh /opt/eclipse/kura/recover_dflt_kura_config.sh
chmod +x /opt/eclipse/kura/recover_dflt_kura_config.sh
if [ ! -d /opt/eclipse/kura/.data ]; then
    mkdir /opt/eclipse/kura/.data
fi
# for md5.info should keep the same order as in the /opt/eclipse/kura/recover_dflt_kura_config.sh
echo `md5sum /opt/eclipse/kura/data/kuranet.conf` > /opt/eclipse/kura/.data/md5.info
echo `md5sum /opt/eclipse/kura/data/snapshots/snapshot_0.xml` >> /opt/eclipse/kura/.data/md5.info
tar czf /opt/eclipse/kura/.data/recover_dflt_kura_config.tgz /opt/eclipse/kura/data/kuranet.conf /opt/eclipse/kura/data/snapshots/snapshot_0.xml

#set up runlevels to start/stop Kura by default
update-rc.d firewall defaults
update-rc.d kura defaults
#update-rc.d monit defaults

#set up logrotate - no need to restart as it is a cronjob
cp /opt/eclipse/kura/install/logrotate.conf /etc/logrotate.conf
cp /opt/eclipse/kura/install/kura.logrotate /etc/logrotate.d/kura
