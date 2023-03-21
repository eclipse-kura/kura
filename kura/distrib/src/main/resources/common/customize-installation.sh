#!/bin/bash
#
#  Copyright (c) 2023 Eurotech and/or its affiliates and others
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

KURA_PLATFORM=$( uname -m )
sed -i "s/kura_platform/${KURA_PLATFORM}/g" "/opt/eclipse/kura/framework/kura.properties"

BOARD="generic-device"

if uname -a | grep -q 'raspberry' > /dev/null 2>&1
then
    BOARD="raspberry"
    echo "Customizing installation for Raspberry PI"
fi

mv "/opt/eclipse/kura/install/jdk.dio.properties-${BOARD}" "/opt/eclipse/kura/framework/jdk.dio.properties"
sed -i "s/device_name/${BOARD}/g" "/opt/eclipse/kura/framework/kura.properties"