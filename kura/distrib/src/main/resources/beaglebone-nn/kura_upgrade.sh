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

#set up logrotate - no need to restart as it is a cronjob
cp /opt/eclipse/kura/install/logrotate.conf /etc/logrotate.conf
cp /opt/eclipse/kura/install/kura.logrotate /etc/logrotate.d/kura
