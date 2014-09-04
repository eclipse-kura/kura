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
KURA_ZIP_FILE_NAME=$2
OUTPUT_NAME=$3
BUILD_NAME=$4
TMP_DIR="tmp_versions"
KURA_FILE_NAME="${KURA_ZIP_FILE_NAME%.zip}"

# Check for existence of md5sum or md5 (OSX)
MD5=md5sum
if ! command -v $MD5 >&-
then
	MD5="md5 -r "
	if ! command -v $MD5 >&-
	then
		echo "Could not find md5sum or md5.  Exiting..."
		exit 1
	fi
fi


# unpack the zip...
cd $TARGET_DIR
rm -rf $TMP_DIR
mkdir $TMP_DIR
unzip $KURA_ZIP_FILE_NAME -d $TMP_DIR


# Get version from MANIFEST.MF for jar files, use md5 for others
echo "# $KURA_FILE_NAME" > $OUTPUT_NAME

FILES=`find $TMP_DIR -type f`
for file in $FILES
do
	# Remove the first two directories from name
	filename=`echo ${file#$TMP_DIR/$KURA_FILE_NAME/}`

	version=`$MD5 $file | awk '{ print $1 }' `
	if [[ $file == *.jar ]]
	then
		# extract the manifest and get the bundle version
		jar xf $file META-INF/MANIFEST.MF
		version=`grep Bundle-Version META-INF/MANIFEST.MF |awk '{print $2}'`
	fi

	printf "$filename	$version\n" >> $OUTPUT_NAME
done

#clean up
rm -rf $TMP_DIR
