#!/bin/sh
#
# Copyright (c) 2011, 2015 Eurotech and/or its affiliates
#
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   Eurotech
#

INSTALL_DIR=/opt/eurotech
KURA_SYMLINK=esf

#create known kura install location
ln -sf ${INSTALL_DIR}/${KURA_SYMLINK}_* ${INSTALL_DIR}/${KURA_SYMLINK}

#set up logrotate - no need to restart as it is a cronjob
cp ${INSTALL_DIR}/${KURA_SYMLINK}/install/logrotate.conf /etc/logrotate.conf
cp ${INSTALL_DIR}/${KURA_SYMLINK}/install/kura.logrotate /etc/logrotate.d/kura