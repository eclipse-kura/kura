#!/usr/bin/env python3
#
# Copyright (c) 2023 Eurotech and/or its affiliates and others
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#  Eurotech
#
import subprocess
import sys

LOG_MSG_PREFIX = "[find-net-interfaces.py] "
ETHERNET_TYPES = ['ethernet', 'eth', 'wired']
WIRELESS_TYPES = ['wifi', 'wireless']

def get_eth_wlan_interfaces_names():
    """Reads the network interface names using 'nmcli dev' command.

    It is assumed that interfaces are sorted according to priority and that at least one
    ethernet interface is present.

    Returns:
        tuple of lists (eth_names, wlan_names) where:
            'eth_names' are the found ethernet interface names;
            'wlan_names' are the found wireless interface names, might be an empty list.
    """
    cmd_output = subprocess.check_output(['nmcli', 'dev']).decode(sys.stdout.encoding).strip()
    # list comprehension to remove empty items
    lines = [x.strip() for x in cmd_output.split('\n') if len(x.strip()) > 0]

    # removing header
    del lines[0]

    ethernet_inteface_names = list()
    wireless_inteface_names = list()

    for line in lines:
        row = [x.strip() for x in line.split(' ') if len(x.strip()) > 0]
        
        interface_name = row[0].lower()
        interface_type = row[1].lower()

        if interface_type in ETHERNET_TYPES:
            ethernet_inteface_names.append(interface_name)
        
        if  interface_type in WIRELESS_TYPES:
            wireless_inteface_names.append(interface_name)
    
    if len(ethernet_inteface_names) < 1:
        print(LOG_MSG_PREFIX + 'ERROR: no ethernet interfaces found')
        sys.exit(1)

    print(LOG_MSG_PREFIX + 'Found ethernet interfaces: ', ethernet_inteface_names)
    print(LOG_MSG_PREFIX + 'Found wireless interfaces: ', wireless_inteface_names)

    return (ethernet_inteface_names, wireless_inteface_names)



file_paths_to_edit = sys.argv[1:] # remove script name from args

if len(file_paths_to_edit) < 1:
    print(LOG_MSG_PREFIX + 'ERROR: invalid arguments length')
    exit(1)

(eth_names, wlan_names) = get_eth_wlan_interfaces_names()

for path in file_paths_to_edit:
    with open(path, 'r+', encoding='utf-8') as file_to_edit:
        print(LOG_MSG_PREFIX + '- ' + path + ': starting editing')

        content = file_to_edit.read()

        for i, eth_name in enumerate(eth_names):
            content = content.replace('eth' + str(i), eth_name)
            print(LOG_MSG_PREFIX + '- ' + path + ': replaced eth' + str(i) + ' with ' + eth_name)

        for i, wlan_name in enumerate(wlan_names):
            content = content.replace('wlan' + str(i), wlan_name)
            print(LOG_MSG_PREFIX + '- ' + path + ': replaced wlan' + str(i) + ' with ' + wlan_name)
        
        file_to_edit.seek(0)
        file_to_edit.truncate()
        file_to_edit.write(content)
        
        print(LOG_MSG_PREFIX + '- ' + path + ': successfully edited')