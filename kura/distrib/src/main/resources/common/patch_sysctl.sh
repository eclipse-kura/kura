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
