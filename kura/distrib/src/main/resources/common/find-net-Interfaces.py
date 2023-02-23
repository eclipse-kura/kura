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

def get_eth_wlan_interfaces_names():
    """Reads the network interface names using 'nmcli dev' command.

    It is assumed that interfaces are sorted according to priority and that at least one
    ethernet interface is present.

    Returns:
         of lists (eth_names, wlan_names) where:
            'eth_names' are the found ethernet interface names,
            'wlan_names' are the found wireless interface names, might be an empty list.
    """
    cmd_output = subprocess.check_output(['nmcli', 'dev']).decode(sys.stdout.encoding).strip()
    # list comprehension to remove empty items
    lines = [x.strip() for x in cmd_output.split('\n') if len(x.strip()) > 0]

    header = list()
    for column in [x.strip() for x in lines[0].split(' ') if len(x.strip()) > 0]:
        header.append(column.lower())

    del lines[0]
    interfaces_info = list()
    for line in lines:
        interface_info = dict()
        row = [x.strip() for x in line.split(' ') if len(x.strip()) > 0]
        for i in range(len(row)):
            interface_info[header[i]] = row[i].lower()
        interfaces_info.append(interface_info)
    
    ethernet_types = ['ethernet', 'eth']
    wireless_types = ['wifi', 'wireless']

    ethernet_inteface_names = [x['device'] for x in interfaces_info if x['type'] in ethernet_types]
    wireless_inteface_names = [x['device'] for x in interfaces_info if x['type'] in wireless_types]

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
    with open(path, 'r+') as file_to_edit:
        print(LOG_MSG_PREFIX + '- ' + path + ': starting editing')

        for i, eth_name in enumerate(eth_names):
            content = file_to_edit.read()
            replaced_content = content.replace('eth' + str(i), eth_name)
            print(LOG_MSG_PREFIX + '- ' + path + ': replaced eth' + str(i) + ' with ' + eth_name)

        for i, wlan_name in enumerate(wlan_names):
            replaced_content = replaced_content.replace('wlan' + str(i), wlan_name)
            print(LOG_MSG_PREFIX + '- ' + path + ': replaced wlan' + str(i) + ' with ' + wlan_name)
        
        file_to_edit.write(replaced_content)
        print(LOG_MSG_PREFIX + '- ' + path + ': successfully edited')