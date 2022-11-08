#!/bin/sh
#
#  Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
#
#  This program and the accompanying materials are made
#  available under the terms of the Eclipse Public License 2.0
#  which is available at https://www.eclipse.org/legal/epl-2.0/
#
#  SPDX-License-Identifier: EPL-2.0
#
#  Contributors:
#   Eurotech
#

INSTALL_DIR=/opt/eurotech
KURA_SYMLINK=esf

#create known kura install location
ln -sf ${INSTALL_DIR}/${KURA_SYMLINK}_* ${INSTALL_DIR}/${KURA_SYMLINK}

#set up logrotate - no need to restart as it is a cronjob
cp ${INSTALL_DIR}/${KURA_SYMLINK}/install/logrotate.conf /etc/logrotate.conf
cp ${INSTALL_DIR}/${KURA_SYMLINK}/install/kura.logrotate /etc/logrotate.d/kura