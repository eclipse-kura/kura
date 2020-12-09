#!/bin/bash
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


TARGET_DIR=$1
KURA_ZIP_FILE_NAME=$2
OUTPUT_NAME=$3
BUILD_NAME=$4
INSTALL_DIR=$5

# Select the no networking extract file if necessary
if [[ $BUILD_NAME =~ .*-nn$ ]]
then
	EXTRACT_NAME="extract_nn.sh"
else
	EXTRACT_NAME="extract.sh"
fi

#tar the zip...
cd $TARGET_DIR
tar czvf $KURA_ZIP_FILE_NAME.tar.gz $KURA_ZIP_FILE_NAME

sed "s|^INSTALL_DIR=.*|INSTALL_DIR=${INSTALL_DIR}|" $TARGET_DIR/../src/main/sh/$EXTRACT_NAME > tmp_file
cat tmp_file $KURA_ZIP_FILE_NAME.tar.gz > $OUTPUT_NAME
chmod +x $OUTPUT_NAME

#clean up
rm $TARGET_DIR/$KURA_ZIP_FILE_NAME.tar.gz
rm $TARGET_DIR/tmp_file
