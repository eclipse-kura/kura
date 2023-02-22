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

file_paths_to_edit = sys.argv[1:] # remove script name from args

if len(file_paths_to_edit) < 1:
    print ('ERROR: invalid arguments length')
    exit(1)

input = subprocess.check_output(['nmcli', 'dev']).decode(sys.stdout.encoding).strip()

# list comprehension to remove empty items
lines = [x.strip() for x in input.split('\n') if len(x.strip()) > 0]

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

#
# interfaces_info example:
#
# [{'device': 'eth0', 'type': 'ethernet', 'state': 'connected', 'connection': 'kura-eth0-connection'},
# {'device': 'wlan0', 'type': 'wifi', 'state': 'connected', 'connection': 'kura-wlan0-connection'},
# {'device': 'p2p-dev-wlan0', 'type': 'wifi-p2p', 'state': 'disconnected', 'connection': '--'},
# {'device': 'lo', 'type': 'loopback', 'state': 'unmanaged', 'connection': '--'}]
#

ethernet_types = ['ethernet', 'eth']
wireless_types = ['wifi', 'wireless']

ethernet_inteface_names = [x['device'] for x in interfaces_info if x['type'] in ethernet_types]
wireless_inteface_names = [x['device'] for x in interfaces_info if x['type'] in wireless_types]

if len(ethernet_inteface_names) < 1:
    print('ERROR: no ethernet interfaces found')
    sys.exit(1)

print('Found ethernet interfaces: ', ethernet_inteface_names)

if len(ethernet_inteface_names) < 1:
    print('ERROR: no wireless interfaces found')
    sys.exit(1)

print('Found wireless interfaces: ', wireless_inteface_names)

#with open('/tmp/eth0', 'wb') as tmp_file:
#    #tmp_file.write(ethernet_inteface_names[0])
#    print(ethernet_inteface_names[0] + ' written to /tmp/eth0')

#with open('/tmp/wlan0', 'wb') as tmp_file:
#    #tmp_file.write(wireless_inteface_names[0])
#    print(wireless_inteface_names[0] + ' written to /tmp/wlan0')

for path in file_paths_to_edit:
    with open(path, 'r+') as file_to_edit:
        print('Editing file ' + path)
        content = file_to_edit.read()
        replaced_content = content.replace('eth0', ethernet_inteface_names[0])
        replaced_content = replaced_content.replace('wlan0', wireless_inteface_names[0])
        file_to_edit.write(replaced_content)
        print('File ' + path + ' successfully edited')