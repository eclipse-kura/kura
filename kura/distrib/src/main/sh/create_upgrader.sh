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
KURA_ZIP_FILE_NAME=$2
OLD_VERSION=$3
INSTALL_DIR=$4
REMOVE_LIST=$5
OUTPUT_NAME=$6
BUILD_NAME=$7


#tar the zip...
cd $TARGET_DIR
tar czvf $KURA_ZIP_FILE_NAME.tar.gz $KURA_ZIP_FILE_NAME $REMOVE_LIST

# Populate variables in extract script
sed "s/^OLD_VERSION=$/OLD_VERSION=$OLD_VERSION/;s/^INSTALL_DIR=$/INSTALL_DIR=$INSTALL_DIR/;s/^REMOVE_LIST=$/REMOVE_LIST=$REMOVE_LIST/" ../src/main/sh/extract_upgrade.sh > $TARGET_DIR/extract_upgrade.sh

cat extract_upgrade.sh $KURA_ZIP_FILE_NAME.tar.gz > $OUTPUT_NAME
chmod +x $OUTPUT_NAME

#clean up
rm $TARGET_DIR/$KURA_ZIP_FILE_NAME.tar.gz
