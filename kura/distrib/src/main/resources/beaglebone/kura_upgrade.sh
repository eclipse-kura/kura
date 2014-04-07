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

#set up network helper scripts
cp /opt/eclipse/kura/install/ifup-local.raspbian /etc/network/if-up.d/ifup-local
cp /opt/eclipse/kura/install/ifdown-local /etc/network/if-down.d/ifdown-local
chmod +x /etc/network/if-up.d/ifup-local
chmod +x /etc/network/if-down.d/ifdown-local

#set up logrotate - no need to restart as it is a cronjob
cp /opt/eclipse/kura/install/logrotate.conf /etc/logrotate.conf
cp /opt/eclipse/kura/install/kura.logrotate /etc/logrotate.d/kura
