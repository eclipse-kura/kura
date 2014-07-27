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


TARGET_DIR=$1
INSTALL_ZIP=$2
UPGRADE_ZIP=$3
OLD_VERSION=$4
INSTALL_DIR=$5
REMOVE_LIST=$6
KEEP_LIST=$7
OUTPUT_NAME=$8
BUILD_NAME=$9


cd $TARGET_DIR

# Create an upgrade zip that does not include files that are unchanged
# from the previous version (files to "keep")
cp $INSTALL_ZIP $TARGET_DIR/$UPGRADE_ZIP

REMOVE=$(
while read line
do
	# Skip comments
	if [[ $line == "#"* ]] ; then
		continue
	fi
	printf "*/$line "
done < $KEEP_LIST)

# Remove files from zip
zip -d $UPGRADE_ZIP $REMOVE



#tar the zip...
tar czvf $UPGRADE_ZIP.tar.gz $UPGRADE_ZIP $REMOVE_LIST

# Populate variables in extract script
sed "s/^OLD_VERSION=$/OLD_VERSION=$OLD_VERSION/;s/^INSTALL_DIR=$/INSTALL_DIR=$INSTALL_DIR/;s/^REMOVE_LIST=$/REMOVE_LIST=$REMOVE_LIST/" ../src/main/sh/extract_upgrade.sh > $TARGET_DIR/extract_upgrade.sh

cat extract_upgrade.sh $UPGRADE_ZIP.tar.gz > $OUTPUT_NAME
chmod +x $OUTPUT_NAME

#clean up
rm $TARGET_DIR/$UPGRADE_ZIP.tar.gz
