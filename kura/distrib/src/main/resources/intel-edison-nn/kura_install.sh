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
mkdir -p /etc/init.d

#set up Kura init
sed "s|^KURA_DIR=.*|KURA_DIR=${INSTALL_DIR}/kura|" ${INSTALL_DIR}/kura/install/kura.init.yocto > /etc/init.d/kura
chmod +x /etc/init.d/kura
chmod +x ${INSTALL_DIR}/kura/bin/*.sh

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
