#!/bin/sh
#
# Copyright (c) 2016 Red Hat Inc and others
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

#set up recover default configuration script
cp ${INSTALL_DIR}/kura/install/recover_default_config.init ${INSTALL_DIR}/kura/.data/.recoverDefaultConfig.sh
chmod +x ${INSTALL_DIR}/kura/.data/.recoverDefaultConfig.sh

#set up networking configuration
mac_addr=$(head /sys/class/net/eth0/address | tr '[:lower:]' '[:upper:]')

OLD_PATH=$(pwd)
SELINUX_KURA=$(semodule -l | grep selinuxKura)
if [ -z $SELINUX_KURA ]; then
	echo "Applying semodule..."
	cd ${INSTALL_DIR}/kura/install/
    semodule -i selinuxKura.pp
    cd ${OLD_PATH}
fi

#copy snapshot_0.xml
cp ${INSTALL_DIR}/kura/data/snapshots/snapshot_0.xml ${INSTALL_DIR}/kura/.data/snapshot_0.xml

#set up dos2unix
if [ ! -f "/usr/bin/dos2unix" ] ; then
    cp ${INSTALL_DIR}/kura/install/dos2unix.bin /usr/bin/dos2unix
    chmod +x /usr/bin/dos2unix
fi

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
sysctl -p

