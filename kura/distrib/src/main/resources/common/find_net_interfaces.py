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
import sys
import logging
import os

def get_interface_names():
    if not os.path.exists("/sys/class/net"):
        logging.error("'/sys/class/net' does not exists, unable to read interface names. Exiting.")
        sys.exit(1)

    return os.listdir("/sys/class/net")

def get_eth_wlan_interfaces_names():
    """Reads the network interface names present on the device.

    It is assumed that at least one ethernet interface is present.

    Requirements:
        "/sys/class/net" directory must exist

    Returns:
        tuple of lists (eth_names, wlan_names) where:
            'eth_names' are the found ethernet interface names sorted by name;
            'wlan_names' are the found wireless interface names sorted by name, might be an empty list.
    """
    ethernet_interface_names = list()
    wireless_interface_names = list()

    interface_names = get_interface_names()
    
    for ifname in interface_names:
        if ifname.startswith("en") or ifname.startswith("et"):
            ethernet_interface_names.append(ifname)
        if ifname.startswith("wl"):
            wireless_interface_names.append(ifname)

    ethernet_interface_names.sort()
    wireless_interface_names.sort()

    if len(ethernet_interface_names) < 1:
        logging.error("No ethernet interfaces found, exiting.")
        sys.exit(1)

    logging.info("Found ethernet interfaces: %s", ethernet_interface_names)
    logging.info("Found wireless interfaces: %s", wireless_interface_names)
    return (ethernet_interface_names, wireless_interface_names)

def main():
    logging.basicConfig(
        format='[find_net_interfaces.py] %(asctime)s %(levelname)s %(message)s',
        level=logging.INFO,
        datefmt='%Y-%m-%d %H:%M:%S',
        handlers=[
            logging.StreamHandler()
        ]
    )
    
    file_paths_to_edit = sys.argv[1:] # remove script name from args
    
    if len(file_paths_to_edit) < 1:
        logging.info("ERROR: invalid arguments length")
        exit(1)
    
    (eth_names, wlan_names) = get_eth_wlan_interfaces_names()
    
    for path in file_paths_to_edit:
        with open(path, 'r+', encoding='utf-8') as file_to_edit:
            logging.info("%s : starting editing", path)
    
            content = file_to_edit.read()
    
            for i, eth_name in enumerate(eth_names):
                content = content.replace('eth' + str(i), eth_name)
                logging.info("%s : replaced eth%s with %s", path, str(i), eth_name)
    
            for i, wlan_name in enumerate(wlan_names):
                content = content.replace('wlan' + str(i), wlan_name)
                logging.info("%s : replaced wlan%s with %s", path, str(i), wlan_name)
            
            file_to_edit.seek(0)
            file_to_edit.truncate()
            file_to_edit.write(content)
            
            logging.info("%s : successfully edited", path)
            
if __name__ == "__main__":
    main()