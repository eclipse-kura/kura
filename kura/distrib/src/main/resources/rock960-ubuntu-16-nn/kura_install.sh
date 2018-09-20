#!/bin/sh
#
# Copyright (c) 2011, 2017 Eurotech and/or its affiliates
#
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   Eurotech
#   Cavium
#

INSTALL_DIR=/opt/eclipse

#create known kura install location
ln -sf ${INSTALL_DIR}/kura_* ${INSTALL_DIR}/kura

#set up Kura init
cp ${INSTALL_DIR}/kura/install/kura.init.raspbian /etc/init.d/kura
chmod +x /etc/init.d/kura
chmod +x ${INSTALL_DIR}/kura/bin/*.sh

mkdir -p ${INSTALL_DIR}/kura/data

#set up runlevels to start/stop Kura by default
update-rc.d kura defaults

#set up logrotate - no need to restart as it is a cronjob
cp ${INSTALL_DIR}/kura/install/logrotate.conf /etc/logrotate.conf
cp ${INSTALL_DIR}/kura/install/kura.logrotate /etc/logrotate.d/kura

# setup snapshot_0 recovery folder
if [ ! -d ${INSTALL_DIR}/kura/.data ]; then
    mkdir ${INSTALL_DIR}/kura/.data
fi

#copy snapshot_0.xml
cp ${INSTALL_DIR}/kura/user/snapshots/snapshot_0.xml ${INSTALL_DIR}/kura/.data/snapshot_0.xml

#set up recover default configuration script
cp ${INSTALL_DIR}/kura/install/recover_default_config.init ${INSTALL_DIR}/kura/bin/.recoverDefaultConfig.sh
chmod +x ${INSTALL_DIR}/kura/bin/.recoverDefaultConfig.sh

