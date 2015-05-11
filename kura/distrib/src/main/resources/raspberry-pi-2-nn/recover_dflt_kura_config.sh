#!/bin/bash
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

cd /tmp
tar xzvf ${INSTALL_DIR}/${KURA_SYMLINK}/.data/recover_dflt_kura_config.tgz
sum=`md5sum /tmp/${INSTALL_DIR}/${KURA_SYMLINK}/data/kuranet.conf`
echo ${sum/\/tmp\/\//\/} > /tmp/${INSTALL_DIR}/${KURA_SYMLINK}/data/md5.info
sum=`md5sum /tmp/${INSTALL_DIR}/${KURA_SYMLINK}/data/snapshots/snapshot_0.xml`
echo ${sum/\/tmp\/\//\/} >> /tmp/${INSTALL_DIR}/${KURA_SYMLINK}/data/md5.info

MD5_1=`md5sum /tmp/${INSTALL_DIR}/${KURA_SYMLINK}/data/md5.info | cut -d ' ' -f 1`
MD5_2=`md5sum ${INSTALL_DIR}/${KURA_SYMLINK}/.data/md5.info | cut -d ' ' -f 1`
if [ $MD5_1 == $MD5_2 ]; then
    cd /
    tar xzvf ${INSTALL_DIR}/${KURA_SYMLINK}/.data/recover_dflt_kura_config.tgz
    # clean up
    cd /tmp
    rm -rf opt
    echo "Default Kura configuration has been recovered."
else
    echo "MD5 sum doesn't match, Kura configuration recovery failed."
fi
