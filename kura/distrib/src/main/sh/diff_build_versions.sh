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


OLD_FILES=$1
NEW_FILES=$2
OUTPUT_NAME=$3
BUILD_NAME=$4


# Make a list of files that were in the old version but not the new
# and files whose versions have changed

echo "# Files that were removed or changed from $OLD_FILES to $NEW_FILES" > $OUTPUT_NAME

while read line
do
	# Skip comments
	if [[ $line == "#"* ]] ; then
		continue
	fi

	filename=${line%%	*}
	#version=${line#*	}

	if ! grep -q "$line" $NEW_FILES ; then
		echo $filename >> $OUTPUT_NAME
	fi

	#printf "${filename}	${version}\n" >> $OUTPUT_NAME
done < $OLD_FILES

#clean up
