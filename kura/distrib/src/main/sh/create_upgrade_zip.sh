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


ZIP_FILE=$1
KEEP_FILES=$2
BUILD_NAME=$3


# Create an update zip that does not include files that are unchanged
# from the previous version

REMOVE=$(
while read line
do
	# Skip comments
	if [[ $line == "#"* ]] ; then
		continue
	fi
	printf "*/$line "
done < $KEEP_FILES)

# Remove from zip
zip -d $ZIP_FILE $REMOVE

