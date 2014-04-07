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

cd /tmp
tar xzvf /opt/eclipse/.data/recover_dflt_kura_config.tgz
sum=`md5sum /tmp/opt/eclipse/data/snapshots/snapshot_0.xml`                   
echo ${sum/\/tmp\//\/} >> /tmp/opt/eclipse/data/md5.info                      
                                                                        
MD5_1=`md5sum /tmp/opt/eclipse/data/md5.info | cut -d ' ' -f 1`
MD5_2=`md5sum /opt/eclipse/.data/md5.info | cut -d ' ' -f 1`   
if [ $MD5_1 == $MD5_2 ]; then                                   
    cd /                                                      
    tar xzvf /opt/eclipse/.data/recover_dflt_kura_config.tgz
    # clean up
    cd /tmp                                                    
    rm -rf opt
    echo "Default Kura configuration has been recovered."
else
    echo "MD5 sum doesn't match, Kura configuration recovery failed."
fi

