#!/bin/bash
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


##############################################
# PRE-INSTALL SCRIPT
##############################################
echo ""
echo "Upgrading Kura..."
echo "Upgrading Kura..." > /tmp/kura_upgrade.log 2>&1

# Save DPs
mkdir /tmp/dps
cp /opt/eclipse/kura/kura/packages/*.dp /tmp/dps/

# Save DPA file wiping out the old Kura version
sed "s/.*kura=file.*/KURA_VERSION_DP/" /opt/eclipse/kura/kura/dpa.properties > /tmp/dpa.properties
sed "s/:kura\/packages/:\/opt\/eclipse\/kura\/kura\/packages/" /tmp/dpa.properties > /tmp/dpa1.properties

# kill JVM and monit for upgrade
killall monit java >> /tmp/kura_upgrade.log 2>&1

# clean up old installation if present
rm /opt/eclipse/kura >> /tmp/kura_upgrade.log 2>&1
rm -fr /opt/eclipse/kura-* >> /tmp/kura_upgrade.log 2>&1

echo ""
##############################################
# END PRE-INSTALL SCRIPT
##############################################

SKIP=`awk '/^__TARFILE_FOLLOWS__/ { print NR + 1; exit 0; }' $0`

# take the tarfile and pipe it into tar and redirect the output
tail -n +$SKIP $0 | tar -xz


##############################################
# POST INSTALL SCRIPT
##############################################
unzip -o kura-*.zip -d /opt/eclipse >> /tmp/kura_upgrade.log 2>&1

# install Kura files
sh /opt/eclipse/kura-*/install/kura_upgrade.sh >> /tmp/kura_upgrade.log 2>&1

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

echo ""
echo "Finished.  Kura has been upgraded in /opt/eclipse/kura and system will now reboot"  >> /opt/eclipse/kura/configuration/kura_upgrade.log 2>&1
reboot
#############################################
# END POST INSTALL SCRIPT
##############################################

# NOTE: Don't place any newline characters after the last line below.
__TARFILE_FOLLOWS__
