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


OLD_FILES=$1
NEW_FILES=$2
REMOVED_NAME="${3}.removed"
KEEP_NAME="${3}.keep"
ADDED_NAME="${3}.added"
BUILD_NAME=$4

# filenames without path or extension
OLD=${OLD_FILES##*/}
OLD=${OLD%.versions}
NEW=${NEW_FILES##*/}
NEW=${NEW%.versions}



# Make a list of files that were in the old version but not the new
# and files whose versions have changed

echo "# Files that were removed or changed from $OLD to $NEW" > $REMOVED_NAME

while read line
do
	# Skip comments
	if [[ $line == "#"* ]] ; then
		continue
	fi

	# remove version from line
	filename=${line%%	*}

	if ! grep -q "$line" $NEW_FILES ; then
		echo $filename >> $REMOVED_NAME
	fi
done < $OLD_FILES


# Make a list of files in the new version that are unchanged, and a list
# of files that were not in the old version

echo "# Files that were unchanged from $OLD to $NEW" > $KEEP_NAME
echo "# Files that were added or changed from $OLD to $NEW" > $ADDED_NAME

while read line
do
	# Skip comments
	if [[ $line == "#"* ]] ; then
		continue
	fi

	# remove version from line
	filename=${line%%	*}

	if grep -q "$line" $OLD_FILES ; then
		echo $filename >> $KEEP_NAME
	else
		echo $filename >> $ADDED_NAME
	fi
done < $NEW_FILES


#clean up
