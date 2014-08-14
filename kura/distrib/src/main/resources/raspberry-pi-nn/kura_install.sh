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

# set up /opt/eclipse/kura/recover_dflt_kura_config.sh
cp /opt/eclipse/kura/install/recover_dflt_kura_config.sh /opt/eclipse/kura/recover_dflt_kura_config.sh
chmod +x /opt/eclipse/kura/recover_dflt_kura_config.sh
if [ ! -d /opt/eclipse/kura/.data ]; then
    mkdir /opt/eclipse/kura/.data
fi
# for md5.info should keep the same order as in the /opt/eclipse/kura/recover_dflt_kura_config.sh
echo `md5sum /opt/eclipse/kura/data/snapshots/snapshot_0.xml` > /opt/eclipse/kura/.data/md5.info
tar czf /opt/eclipse/kura/.data/recover_dflt_kura_config.tgz /opt/eclipse/kura/data/snapshots/snapshot_0.xml

#set up runlevels to start/stop Kura by default
update-rc.d kura defaults

#set up logrotate - no need to restart as it is a cronjob
cp /opt/eclipse/kura/install/logrotate.conf /etc/logrotate.conf
cp /opt/eclipse/kura/install/kura.logrotate /etc/logrotate.d/kura
