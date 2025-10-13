#!/bin/sh
#
#  Copyright (c) 2011, 2024 Eurotech and/or its affiliates and others
#
#  This program and the accompanying materials are made
#  available under the terms of the Eclipse Public License 2.0
#  which is available at https://www.eclipse.org/legal/epl-2.0/
#
#  SPDX-License-Identifier: EPL-2.0
#
#  Contributors:
#   Eurotech
#   Cavium
#

setup_libudev() {
    # create soft link for libudev.so.0 to make it retrocompatible
    # https://unix.stackexchange.com/questions/156776/arch-ubuntu-so-whats-the-deal-with-libudev-so-0
    if [ ! -f /lib/libudev.so.0 ] && [ -f /lib/libudev.so.1 ]; then
        ln -sf /lib/libudev.so.1 /lib/libudev.so.0
    fi

    if [ ! -f /usr/lib/aarch64-linux-gnu/libudev.so.0 ] && [ -f /usr/lib/aarch64-linux-gnu/libudev.so.1 ]; then
       ln -sf /usr/lib/aarch64-linux-gnu/libudev.so.1 /usr/lib/aarch64-linux-gnu/libudev.so.0
    fi
}

INSTALL_DIR=/opt/eclipse

setup_libudev

#create known kura install location
ln -sf ${INSTALL_DIR}/kura_* ${INSTALL_DIR}/kura

#set up Kura init
sed "s|INSTALL_DIR|${INSTALL_DIR}|" ${INSTALL_DIR}/kura/install/kura.service > /lib/systemd/system/kura.service
systemctl daemon-reload
systemctl enable kura
chmod +x ${INSTALL_DIR}/kura/bin/*.sh

mkdir -p ${INSTALL_DIR}/kura/data

# disable systemd watchdog
# https://manpages.debian.org/testing/systemd/systemd-system.conf.5.en.html
#
# Order of application of conf files:
# 1. /usr/lib/systemd/system.conf.d/
# 2. /usr/local/lib/systemd/system.conf.d/
# 3. /etc/systemd/system.conf.d/
# docs suggest to use 10-40 priority for drop-ins in /usr/, and 50-90 for /etc/
# we use zz since some OSs do not respect that convention (raspbian, ubuntu jammy)
#
chmod 644 ${INSTALL_DIR}/kura/install/zz-kura-disable-watchdog.conf
if [ -d /usr/lib/systemd/system.conf.d/ ]; then
    echo "Installing /usr/lib/systemd/system.conf.d/zz-kura-disable-watchdog.conf"
    cp ${INSTALL_DIR}/kura/install/zz-kura-disable-watchdog.conf /usr/lib/systemd/system.conf.d/
elif [ -d /usr/local/lib/systemd/system.conf.d/ ]; then
    echo "Installing /usr/local/lib/systemd/system.conf.d/zz-kura-disable-watchdog.conf"
    cp ${INSTALL_DIR}/kura/install/zz-kura-disable-watchdog.conf /usr/local/lib/systemd/system.conf.d/
elif [ -d /etc/systemd/system.conf.d/ ]; then
    echo "Installing /etc/systemd/system.conf.d/zz-kura-disable-watchdog.conf"
    cp ${INSTALL_DIR}/kura/install/zz-kura-disable-watchdog.conf /etc/systemd/system.conf.d/
else
    echo "No systemd drop-in directory found, watchdog not disabled"
fi

#set up systemd-tmpfiles
cp ${INSTALL_DIR}/kura/install/kura-tmpfiles.conf /etc/tmpfiles.d/kura.conf

# setup snapshot_0 recovery folder
if [ ! -d ${INSTALL_DIR}/kura/.data ]; then
    mkdir ${INSTALL_DIR}/kura/.data
fi

#set up users and grant permissions to them
cp ${INSTALL_DIR}/kura/install/manage_kura_users.sh ${INSTALL_DIR}/kura/.data/manage_kura_users.sh
chmod 700 ${INSTALL_DIR}/kura/.data/manage_kura_users.sh
${INSTALL_DIR}/kura/.data/manage_kura_users.sh -i -nn

#copy snapshot_0.xml
cp ${INSTALL_DIR}/kura/user/snapshots/snapshot_0.xml ${INSTALL_DIR}/kura/.data/snapshot_0.xml

# set up kura files permissions
chmod 700 ${INSTALL_DIR}/kura/bin/*.sh
chown -R kurad:kurad /opt/eclipse
chmod -R go-rwx /opt/eclipse
chmod a+rx /opt/eclipse    
find /opt/eclipse/kura -type d -exec chmod u+x "{}" \;

keytool -genkey -alias localhost -keyalg RSA -keysize 2048 -keystore /opt/eclipse/kura/user/security/httpskeystore.ks -deststoretype pkcs12 -dname "CN=Kura, OU=Kura, O=Eclipse Foundation, L=Ottawa, S=Ontario, C=CA" -ext ku=digitalSignature,nonRepudiation,keyEncipherment,dataEncipherment,keyAgreement,keyCertSign -ext eku=serverAuth,clientAuth,codeSigning,timeStamping -validity 1000 -storepass changeit -keypass changeit  
