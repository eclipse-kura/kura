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

backup_files() {
    SUFFIX="${1}"

    shift

    for file in "${@}"
    do
        if [ -f "${file}" ]
        then
            mv "${file}" "${file}.${SUFFIX}"
        fi
    done
}

disable_netplan() {
    # disable netplan configuration files
    backup_files kurasave /lib/netplan/*.yaml /etc/netplan/*.yaml

    if [ -d /etc/netplan  ]
    then

    # use NM renderer
        cat > /etc/netplan/zz-kura-use-nm.yaml <<EOF
network:
  version: 2
  renderer: NetworkManager
EOF
    fi
}

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

# manage running services
systemctl daemon-reload
systemctl stop systemd-timesyncd
systemctl disable systemd-timesyncd
systemctl stop chrony
systemctl disable chrony
systemctl enable NetworkManager
systemctl enable ModemManager
systemctl stop dnsmasq
systemctl disable dnsmasq
systemctl stop dhcpcd
systemctl disable dhcpcd
systemctl disable systemd-networkd

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

disable_netplan

if [ -d /usr/lib/NetworkManager/conf.d/ ]; then
    TO_REMOVE=$( find /usr/lib/NetworkManager/conf.d/ -type f -name  "*-globally-managed-devices.conf" | awk 'NR==1{print $1}' )

    if [ -f "${TO_REMOVE}" ]; then
        rm "${TO_REMOVE}"
    fi
fi
# comment network interface configurations in interfaces file
if python3 -V > /dev/null 2>&1
then
    python3 /opt/eclipse/kura/install/comment_interfaces_file.py
else
    echo "python3 not found. Please manually review the /etc/network/interfaces file and comment configured network interfaces."
fi

# install dnsmasq default configuration
if [ -f /etc/default/dnsmasq ]; then
    mv /etc/default/dnsmasq /etc/default/dnsmasq.old
fi
cp ${INSTALL_DIR}/kura/install/dnsmasq /etc/default/dnsmasq

# disable NTP service
if command -v timedatectl > /dev/null ;
  then
    timedatectl set-ntp false
fi

#set up bind/named
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
