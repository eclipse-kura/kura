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

KURA_CORRECT_PREVIOUS_VERSION=0.1.0

# These get replaced with the correct versions during the maven build (e.g. this line becomes "KURA_CORRECT_VERSION=0.2.0")
KURA_CORRECT_VERSION=UNKNOWN

# Make sure the Kura version is correct
KURA_VERSION_LINE=`cat /opt/eclipse/kura/kura/kura.properties | grep kura.version`

if [ "kura.version=${KURA_CORRECT_VERSION}" != "${KURA_VERSION_LINE}" ] ; then
        echo "Kura version is not correct - was ${KURA_VERSION_LINE}"

	# Get the previous version
	KURA_PREVIOUS_VERSION=${KURA_VERSION_LINE:12}
	if [ "${KURA_CORRECT_PREVIOUS_VERSION}" != "${KURA_PREVIOUS_VERSION}" ] ; then
		echo "CAN NOT UPDATE - Kura is currently at version ${KURA_PREVIOUS_VERSION} but this update can only apply to ${KURA_CORRECT_PREVIOUS_VERSION}"
		exit -1
	else
		echo "Kura version updated in kura.properties to ${KURA_CORRECT_VERSION}"
	        sed "s/^kura\.version.*/kura.version=${KURA_CORRECT_VERSION}/" /opt/eclipse/kura/kura/kura.properties > /tmp/kura.properties
	        mv /tmp/kura.properties /opt/eclipse/kura/kura/kura.properties

		# Note: we can't exit yet because we have to fix some other issues
	fi
else
    	echo "Kura version is correct in kura.properties file: ${KURA_CORRECT_VERSION}"
	# This is not a 'real' update - just the starting of Kura.  So, no need to continue
	exit 0
fi

# Return
exit 0
