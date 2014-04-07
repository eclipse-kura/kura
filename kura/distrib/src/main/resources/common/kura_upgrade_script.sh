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

KURA_CORRECT_PREVIOUS_VERSION=2.0.4

# These get replaced with the correct versions during the maven build (e.g. this line becomes "KURA_CORRECT_VERSION=2.1.0")
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

# Fix the f/s path issues in the /opt/eclipse/kura/bin dir and /opt/eclipse/kura/kura/kura.properties
sed "s/^kura\.home.*/kura.home=\/opt\/eclipse\/kura\/kura/" /opt/eclipse/kura/kura/kura.properties > /tmp/kura.properties
sed "s/^kura\.plugins.*/kura.plugins=\/opt\/eclipse\/kura\/kura\/plugins/" /tmp/kura.properties > /tmp/kura.properties1
sed "s/^kura\.packages.*/kura.packages=\/opt\/eclipse\/kura\/kura\/packages/" /tmp/kura.properties1 > /tmp/kura.properties2
mv /tmp/kura.properties2 /opt/eclipse/kura/kura/kura.properties
rm /tmp/kura.properties*

# Fix the Kura start scripts
sed "s/\-Dkura.configuration.*/\-Dkura.configuration=file:\/opt\/eclipse\/kura\/kura\/kura.properties \\\/" /opt/eclipse/kura/bin/start_kura_background.sh > /tmp/start_kura_background.sh
sed "s/\-Ddpa.configuration.*/\-Ddpa.configuration=\/opt\/eclipse\/kura\/kura\/dpa.properties \\\/" /tmp/start_kura_background.sh > /tmp/start_kura_background.sh1
sed "s/\-Dlog4j.configuration.*/\-Dlog4j.configuration=file:\/opt\/eclipse\/kura\/kura\/log4j.properties \\\/" /tmp/start_kura_background.sh1 > /tmp/start_kura_background.sh2
mv /tmp/start_kura_background.sh2 /opt/eclipse/kura/bin/start_kura_background.sh
rm /tmp/start_kura_background.sh*
chmod +x /opt/eclipse/kura/bin/start_kura_background.sh
sed "s/\-Dkura.configuration.*/\-Dkura.configuration=file:\/opt\/eclipse\/kura\/kura\/kura.properties \\\/" /opt/eclipse/kura/bin/start_kura.sh > /tmp/start_kura.sh
sed "s/\-Ddpa.configuration.*/\-Ddpa.configuration=\/opt\/eclipse\/kura\/kura\/dpa.properties \\\/" /tmp/start_kura.sh > /tmp/start_kura.sh1
sed "s/\-Dlog4j.configuration.*/\-Dlog4j.configuration=file:\/opt\/eclipse\/kura\/kura\/log4j.properties \\\/" /tmp/start_kura.sh1 > /tmp/start_kura.sh2
mv /tmp/start_kura.sh2 /opt/eclipse/kura/bin/start_kura.sh
rm /tmp/start_kura.sh*
chmod +x /opt/eclipse/kura/bin/start_kura.sh
sed "s/\-Dkura.configuration.*/\-Dkura.configuration=file:\/opt\/eclipse\/kura\/kura\/kura.properties \\\/" /opt/eclipse/kura/bin/start_kura_debug.sh > /tmp/start_kura_debug.sh
sed "s/\-Ddpa.configuration.*/\-Ddpa.configuration=\/opt\/eclipse\/kura\/kura\/dpa.properties \\\/" /tmp/start_kura_debug.sh > /tmp/start_kura_debug.sh1
sed "s/\-Dlog4j.configuration.*/\-Dlog4j.configuration=file:\/opt\/eclipse\/kura\/kura\/log4j.properties \\\/" /tmp/start_kura_debug.sh1 > /tmp/start_kura_debug.sh2
mv /tmp/start_kura_debug.sh2 /opt/eclipse/kura/bin/start_kura_debug.sh
rm /tmp/start_kura_debug.sh*
chmod +x /opt/eclipse/kura/bin/start_kura_debug.sh

# Return
exit 0
