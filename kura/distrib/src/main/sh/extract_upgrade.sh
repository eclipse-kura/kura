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


OLD_VERSION=
INSTALL_DIR=
REMOVE_LIST=

DIR=`pwd`
LOG=/tmp/kura_upgrade.log

##############################################
# PRE-INSTALL SCRIPT
##############################################
echo "Upgrading Kura..." > $LOG

# Check currently installed version
CURRENT_VERSION=`grep -e "^kura.version=" /opt/eclipse/kura/kura/kura.properties |cut -d'=' -f2`
if [ "$CURRENT_VERSION" != "$OLD_VERSION" ]; then
    echo "Could not update - Currently installed version is not $OLD_VERSION" | tee $LOG 2>&1
    exit 1
fi

# Check that upgraded version does not already exist
if [ -d "/opt/eclipse/$INSTALL_DIR" ]; then
    echo "Could not update - Updated version already exists in /opt/eclipse/$INSTALL_DIR" | tee $LOG 2>&1
    exit 1
fi

## Save DPs
#mkdir /tmp/dps
#cp /opt/eclipse/kura/kura/packages/*.dp /tmp/dps/

# Save DPA file wiping out the old Kura version
sed "s/.*kura=file.*/KURA_VERSION_DP/" /opt/eclipse/kura/kura/dpa.properties > /tmp/dpa.properties 2> $LOG
sed "s/:kura\/packages/:\/opt\/eclipse\/kura\/kura\/packages/" /tmp/dpa.properties > /tmp/dpa1.properties 2> $LOG

# kill JVM and monit for upgrade
killall monit java >> $LOG 2>&1

## clean up old installation if present
#rm /opt/eclipse/kura >> $LOG 2>&1
#rm -fr /opt/eclipse/kura-* >> $LOG 2>&1


# Make a copy of the previous installation using hard links
echo "Creating hard link copy of previous version into $INSTALL_DIR" >> $LOG 2>&1
mkdir "/opt/eclipse/$INSTALL_DIR"
cd "/opt/eclipse/kura" && find . -type d | cpio -dp /opt/eclipse/$INSTALL_DIR >> $LOG 2>&1
cd "/opt/eclipse/kura" && find . -type f -exec ln {} /opt/eclipse/$INSTALL_DIR/{} \; >> $LOG 2>&1

# Replace hard links with real copies for certain files
FILES=" \
	bin/* \
	data/* \
	kura/config.ini \
	kura/dpa.properties \
	kura/kura_install.log \
	kura/kura.properties \
	kura/kura_custom.properties \
	kura/log4j.properties
"
for f in $FILES
do
	target="/opt/eclipse/$INSTALL_DIR/$f"
	echo "Creating a real copy of $target" >> $LOG 2>&1
	rm -rf $target >> $LOG 2>&1
	# copy file, removing the filename from the target to support wildcards
	cp -r /opt/eclipse/kura/$f ${target%/*} >> $LOG 2>&1
done

echo "" >> $LOG 2>&1
##############################################
# END PRE-INSTALL SCRIPT
##############################################

SKIP=`cd $DIR && awk '/^__TARFILE_FOLLOWS__/ { print NR + 1; exit 0; }' $0`

# take the tarfile and pipe it into tar and redirect the output
cd $DIR && tail -n +$SKIP $0 | tar -xz >> $LOG 2>&1


##############################################
# POST INSTALL SCRIPT
##############################################

# Remove files not needed in the new version
echo "Removing old files..." >> $LOG 2>&1
while read line
do
	# Skip comments
	if [[ $line == "#"* ]] ; then
		continue
	fi

	# TODO - remove files outside of kura directory
	rmfile="/opt/eclipse/$INSTALL_DIR/$line"
	echo "Removing $rmfile" >> $LOG 2>&1
	rm -f $rmfile
done < $REMOVE_LIST

# Extract new files
unzip -o kura-*.zip -d /opt/eclipse >> $LOG 2>&1

## install Kura files
#if [ -f /opt/eclipse/$INSTALL_DIR/install/kura_upgrade.sh ]; then
#	sh /opt/eclipse/$INSTALL_DIR/install/kura_upgrade.sh >> $LOG 2>&1
#fi

# Point symlink to new version
rm -f /opt/eclipse/kura
ln -s /opt/eclipse/$INSTALL_DIR /opt/eclipse/kura

# clean up
rm -rf /opt/eclipse/kura/install >> $LOG 2>&1
rm kura-*.zip >> $LOG 2>&1

# move the log file
mkdir -p /opt/eclipse/kura/log
#mv $LOG /opt/eclipse/kura/log/
#LOG=/opt/eclipse/kura/log/kura_upgrade.log

## restore dps except old Kura version
#rm /tmp/dps/kura-*.dp
#mv /tmp/dps/* /opt/eclipse/kura/kura/packages/

# restore dpa
if [ -d /opt/eclipse/kura/kura/packages ]; then
	cd /opt/eclipse/kura/kura/packages
	KURA=`ls kura-*.dp 2>> $LOG`
fi
if [ -s /tmp/dpa1.properties ]; then
	sed "s/KURA_VERSION_DP/kura=file\\\:\/opt\/eclipse\/kura\/kura\/packages\/${KURA}/" /tmp/dpa1.properties > /opt/eclipse/kura/kura/dpa.properties
fi

# set permissions
chmod +x /opt/eclipse/kura/bin/*.sh >> $LOG 2>&1

# flush all cached filesystem to disk
sync

echo "" >> $LOG 2>&1
echo "Finished.  Kura has been upgraded in /opt/eclipse/kura and system will now reboot" >> $LOG 2>&1
cp $LOG /opt/eclipse/kura/log
reboot

#############################################
# END POST INSTALL SCRIPT
##############################################
exit 0

# NOTE: Don't place any newline characters after the last line below.
__TARFILE_FOLLOWS__
