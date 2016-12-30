#!/bin/bash -e
#
# Copyright (c) 2016 Eurotech and/or its affiliates
#
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
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
