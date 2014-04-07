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


# remove the original update file
rm /tmp/kura-update-*.zip

# make the upgrade executable
chmod +x /tmp/kura-*_upgrader.sh

# execute the upgrade
sh /tmp/kura-*_upgrader.sh &

echo "Installing Kura and rebooting"
sleep 90

# clean up
rm -rf /opt/eclipse/kura/install >> /tmp/kura_upgrade.log 2>&1
rm kura-*.zip >> /tmp/kura_upgrade.log 2>&1

# move the log file
mv /tmp/kura_upgrade.log /opt/eclipse/kura/configuration/

# restore dps except old Kura version
rm /tmp/dps/kura-*.dp
mv /tmp/dps/* /opt/eclipse/kura/kura/packages/

# restore dpa
cd /opt/eclipse/kura/kura/packages
KURA=`ls kura-*.dp`
sed "s/KURA_VERSION_DP/kura=file\\\:\/opt\/eclipse\/kura\/kura\/packages\/${KURA}/" /tmp/dpa1.properties > /opt/eclipse/kura/kura/dpa.properties

# set permissions
chmod +x /opt/eclipse/kura/bin/*.sh

# flush all cached filesystem to disk
sync                          

# finally reboot
reboot
