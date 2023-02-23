#!/bin/sh
#
# Copyright (c) 2023 Eurotech and/or its affiliates and others
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#  Eurotech
#

# manage running services
systemctl daemon-reload
systemctl stop systemd-timesyncd
systemctl disable systemd-timesyncd
systemctl stop chrony
systemctl disable chrony
systemctl enable NetworkManager
systemctl start NetworkManager
systemctl enable firewall

INSTALL_DIR=/opt/eclipse

# create known kura install location
ln -sf ${INSTALL_DIR}/kura_* ${INSTALL_DIR}/kura

# set up kura init
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

# set up users and grant permissions
cp ${INSTALL_DIR}/kura/install/manage_kura_users.sh ${INSTALL_DIR}/kura/.data/manage_kura_users.sh
chmod 700 ${INSTALL_DIR}/kura/.data/manage_kura_users.sh
${INSTALL_DIR}/kura/.data/manage_kura_users.sh -i

# replace snapshot_0 and iptables.init with correct interface names
if python3 -V > /dev/null 2>&1
then
    python3 ${INSTALL_DIR}/kura/install/find-net-interfaces.py ${INSTALL_DIR}/kura/user/snapshots/snapshot_0.xml ${INSTALL_DIR}/kura/install/iptables.init ${INSTALL_DIR}/kura/framework/kura.properties
elif python -V > /dev/null 2>&1
then
    python ${INSTALL_DIR}/kura/install/find-net-interfaces.py ${INSTALL_DIR}/kura/user/snapshots/snapshot_0.xml ${INSTALL_DIR}/kura/install/iptables.init ${INSTALL_DIR}/kura/framework/kura.properties
else
    echo "python/python3 not found. ${INSTALL_DIR}/kura/user/snapshots/snapshot_0.xml, ${INSTALL_DIR}/kura/install/iptables.init, and ${INSTALL_DIR}/kura/framework/kura.properties may have wrong interface names. Default is eth0 and wlan0. Please correct them manually if they mismatch."
fi

# copy snapshot_0.xml
cp ${INSTALL_DIR}/kura/user/snapshots/snapshot_0.xml ${INSTALL_DIR}/kura/.data/snapshot_0.xml

# set up default firewall configuration
cp ${INSTALL_DIR}/kura/install/iptables.init ${INSTALL_DIR}/kura/.data/iptables
chmod 644 ${INSTALL_DIR}/kura/.data/iptables
cp ${INSTALL_DIR}/kura/.data/iptables /etc/sysconfig/iptables
cp ${INSTALL_DIR}/kura/install/firewall.init ${INSTALL_DIR}/kura/bin/firewall
chmod 755 ${INSTALL_DIR}/kura/bin/firewall
cp ${INSTALL_DIR}/kura/install/firewall.service /lib/systemd/system/firewall.service
chmod 644 /lib/systemd/system/firewall.service
sed -i "s|/bin/sh KURA_DIR|/bin/bash ${INSTALL_DIR}/kura|" /lib/systemd/system/firewall.service

# disable NTP service
if command -v timedatectl > /dev/null ;
  then
    timedatectl set-ntp false
fi

#set up logrotate - no need to restart as it is a cronjob
cp ${INSTALL_DIR}/kura/install/kura.logrotate /etc/logrotate-kura.conf

if [ ! -f /etc/cron.d/logrotate-kura ]; then
    test -d /etc/cron.d || mkdir -p /etc/cron.d
    touch /etc/cron.d/logrotate-kura
    echo "*/5 * * * * root /usr/sbin/logrotate --state /var/log/logrotate-kura.status /etc/logrotate-kura.conf" >> /etc/cron.d/logrotate-kura
fi

# set up systemd-tmpfiles
cp ${INSTALL_DIR}/kura/install/kura-tmpfiles.conf /etc/tmpfiles.d/kura.conf

# set up kura files permissions
chmod 700 ${INSTALL_DIR}/kura/bin/*.sh
chown -R kurad:kurad /opt/eclipse
chmod -R go-rwx /opt/eclipse
chmod a+rx /opt/eclipse    
find /opt/eclipse/kura -type d -exec chmod u+x "{}" \;

# execute patch_sysctl.sh (required for disabling ipv6))
chmod 700 ${INSTALL_DIR}/kura/install/patch_sysctl.sh
${INSTALL_DIR}/kura/install/patch_sysctl.sh ${INSTALL_DIR}/kura/install/sysctl.kura.conf /etc/sysctl.conf

# disables IPv6 on all network interfaces in the system if the "/sys/class/net" directory exists, or applies the system-wide configuration specified in the "/etc/sysctl.conf" file using the "sysctl -p" command otherwise.
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

# install custom jdk.dio based on board
BOARD="generic-device"
if uname -a | grep -q 'raspberry' > /dev/null 2>&1
then
    BOARD="raspberry"
    echo "Installing custom jdk.dio.properties for Raspberry PI"
else
    echo "Installing generic-device jdk.dio.properties. Please review it with correct GPIO mappings."
fi

mv ${INSTALL_DIR}/kura/install/jdk.dio.properties-${BOARD} ${INSTALL_DIR}/kura/framework/jdk.dio.properties

# customizing kura.properties

KURA_PLATFORM=$( uname -m )
sed -i "s/kura_platform/${KURA_PLATFORM}/" ${INSTALL_DIR}/kura/framework/kura.properties
sed -i "s/device_name/${BOARD}/" ${INSTALL_DIR}/kura/framework/kura.properties