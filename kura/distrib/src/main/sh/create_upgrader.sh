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


TARGET_DIR=$1
INSTALL_ZIP=$2
UPGRADE_ZIP=$3
OLD_VERSION=$4
BASE_DIR=$5
INSTALL_DIR=$6
REMOVE_LIST=$7
KEEP_LIST=$8
OUTPUT_NAME=$9
BUILD_NAME=${10}

# Files that should NOT be included in an upgrade
EXCLUDE_FILES=("kura/kura_custom.properties" "kura/dpa.properties" "data/kuranet.conf")  

cd $TARGET_DIR

# Create an upgrade zip that does not include files that are unchanged
# from the previous version (files to "keep")
cp $INSTALL_ZIP $TARGET_DIR/$UPGRADE_ZIP

REMOVE="$(printf "*/%s " ${EXCLUDE_FILES[@]})"
REMOVE+=$(
while read line
do
	# Skip comments and blank lines
	if [[ $line == "#"* || -z $line ]] ; then
		continue
	fi
	printf "*/$line "
done < $KEEP_LIST)

# Remove files from zip
zip -d $UPGRADE_ZIP $REMOVE

# Remove excluded files from remove list
UPGRADE_REMOVE="upgrade_${REMOVE_LIST}"
# escape slash
EXCLUDE_ESC=(${EXCLUDE_FILES[@]/\//\\/})
EXCLUDE_SED="$(printf "/%s/d;" "${EXCLUDE_ESC[@]}")"
sed "$EXCLUDE_SED" $REMOVE_LIST > $UPGRADE_REMOVE


#tar the zip...
tar czvf $UPGRADE_ZIP.tar.gz $UPGRADE_ZIP $UPGRADE_REMOVE

# Populate variables in extract script
if [[ $BUILD_NAME == *"edison"* ]]
then
	sed "s/^OLD_VERSION=.*/OLD_VERSION=$OLD_VERSION/;s|^BASE_DIR=.*|BASE_DIR=$BASE_DIR|;s/^INSTALL_DIR=.*/INSTALL_DIR=$INSTALL_DIR/;s/^REMOVE_LIST=.*/REMOVE_LIST=$UPGRADE_REMOVE/;s/^ABSOLUTE_PATH=\`readlink -m \$0\`/ABSOLUTE_PATH=\`readlink -f \$0\`/" ../src/main/sh/extract_upgrade.sh > $TARGET_DIR/extract_upgrade.sh
else
	sed "s/^OLD_VERSION=.*/OLD_VERSION=$OLD_VERSION/;s|^BASE_DIR=.*|BASE_DIR=$BASE_DIR|;s/^INSTALL_DIR=.*/INSTALL_DIR=$INSTALL_DIR/;s/^REMOVE_LIST=.*/REMOVE_LIST=$UPGRADE_REMOVE/" ../src/main/sh/extract_upgrade.sh > $TARGET_DIR/extract_upgrade.sh
fi

cat extract_upgrade.sh $UPGRADE_ZIP.tar.gz > $OUTPUT_NAME
chmod +x $OUTPUT_NAME

#clean up
rm $TARGET_DIR/$UPGRADE_ZIP.tar.gz
