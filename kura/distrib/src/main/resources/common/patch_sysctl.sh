#!/bin/bash -e
#
#  Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
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
# sysctl updater

if ! [ x"$1" != "x" ] || ! [ x"$2" != "x" ] || ! [ -a $1 ] || ! [ -a $2 ]; then
	echo "Usage $0 <sysctl.kura.conf> <sysctl.system.conf>"
	exit 1
fi

if [ -e /etc/fedora-release ]; then
	touch /etc/modules-load.d/ipv6.conf
	sed -i '/ipv6.*/d' /etc/modules-load.d/ipv6.conf
	echo "ipv6 " >> /etc/modules-load.d/ipv6.conf
fi

eval sed -i `grep -v "^#" $1 | sed "s|\(.*\)=.*|-e '\/\1\/d'|"` $2
grep -v "^#" $1 >> $2

# This is a bash script that performs the following actions:
#
# 1) Checks if the first and second command line arguments are provided and if they exist in the file system. If any of these conditions are not met, the script prints a usage message and exits with an error code.
#
# 2) If the script is running on a system with the file "/etc/fedora-release", the script creates a file called "ipv6.conf" in the directory "/etc/modules-load.d/", removes any existing lines containing "ipv6" from the "ipv6.conf" file, and appends a new line with "ipv6".
#
# 3) The script removes any commented lines and extracts the non-commented lines containing kernel parameters from the first file, and uses them as patterns to delete matching lines in the second file.
#
# Finally, the script appends the non-commented lines from the first file to the second file.
#
# In summary, the script updates the second file with the kernel parameters specified in the first file and ensures that the "ipv6" kernel module is loaded if the script is running on a Fedora-based system.