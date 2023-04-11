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

INSTALL_DIR=/opt/eclipse

# NetworkManager cannot modify connection settings that are from /etc/network/interfaces
if test -f /etc/network/interfaces; then
    mv /etc/network/interfaces /etc/network/interfaces.old
fi

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

# manage running services
systemctl daemon-reload
systemctl stop systemd-timesyncd
systemctl disable systemd-timesyncd
systemctl stop chrony
systemctl disable chrony
systemctl enable NetworkManager
systemctl start NetworkManager
systemctl enable ModemManager

# set up users and grant permissions
cp ${INSTALL_DIR}/kura/install/manage_kura_users.sh ${INSTALL_DIR}/kura/.data/manage_kura_users.sh
chmod 700 ${INSTALL_DIR}/kura/.data/manage_kura_users.sh
${INSTALL_DIR}/kura/.data/manage_kura_users.sh -i

bash "${INSTALL_DIR}/kura/install/customize-installation.sh"

# copy snapshot_0.xml
cp ${INSTALL_DIR}/kura/user/snapshots/snapshot_0.xml ${INSTALL_DIR}/kura/.data/snapshot_0.xml

# set up default firewall configuration
if [ ! -d /etc/sysconfig ]; then
    mkdir /etc/sysconfig
fi
chmod 644 ${INSTALL_DIR}/kura/.data/iptables
cp ${INSTALL_DIR}/kura/.data/iptables /etc/sysconfig/iptables
cp ${INSTALL_DIR}/kura/install/firewall.init ${INSTALL_DIR}/kura/bin/firewall
chmod 755 ${INSTALL_DIR}/kura/bin/firewall
cp ${INSTALL_DIR}/kura/install/firewall.service /lib/systemd/system/firewall.service
chmod 644 /lib/systemd/system/firewall.service
sed -i "s|/bin/sh KURA_DIR|/bin/bash ${INSTALL_DIR}/kura|" /lib/systemd/system/firewall.service
systemctl daemon-reload
systemctl enable firewall

# disables cloud-init network management if exists, sets netplan network renderer to NetworkManager allowing interface management to NetworkManager
if [ -d /etc/cloud/cloud.cfg.d ]; then
    echo "network: {config: disabled}" | sudo tee -a /etc/cloud/cloud.cfg.d/99-disable-network-config.cfg > /dev/null
fi
if [ -d /etc/netplan/ ]; then
    cp /etc/netplan/00-installer-config.yaml /etc/netplan/00-installer-config.yaml.BAK
    cat << EOF >> /etc/netplan/00-installer-config.yaml
# This file describes the network interfaces available on your system
# For more information, see netplan(5).
network:
  version: 2
  renderer: NetworkManager
EOF
fi
if [ -d /usr/lib/NetworkManager/conf.d/ ]; then
    TO_REMOVE=$( find /usr/lib/NetworkManager/conf.d/ -type f -name  "*-globally-managed-devices.conf" | awk 'NR==1{print $1}' )

    if [ -f "${TO_REMOVE}" ]; then
        rm "${TO_REMOVE}"
    fi
fi

# disable NTP service
if command -v timedatectl > /dev/null ;
  then
    timedatectl set-ntp false
fi

# set up logrotate - no need to restart as it is a cronjob
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

keytool -genkey -alias localhost -keyalg RSA -keysize 2048 -keystore /opt/eclipse/kura/user/security/httpskeystore.ks -deststoretype pkcs12 -dname "CN=Kura, OU=Kura, O=Eclipse Foundation, L=Ottawa, S=Ontario, C=CA" -ext ku=digitalSignature,nonRepudiation,keyEncipherment,dataEncipherment,keyAgreement,keyCertSign -ext eku=serverAuth,clientAuth,codeSigning,timeStamping -validity 1000 -storepass changeit -keypass changeit
