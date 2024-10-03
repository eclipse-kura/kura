#!/usr/bin/env python3
#
# Copyright (c) 2024 Eurotech and/or its affiliates and others
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
from network_tools import get_eth_wlan_interfaces_names

def main():
    logging.basicConfig(
        format='[customize_iptables.py] %(asctime)s %(levelname)s %(message)s',
        level=logging.INFO,
        datefmt='%Y-%m-%d %H:%M:%S',
        handlers=[
            logging.StreamHandler()
        ]
    )
    
    iptables_filename = "/opt/eclipse/kura/.data/iptables"
    
    (eth_names, wlan_names) = get_eth_wlan_interfaces_names()
    
    eth_number = len(eth_names)
    wlan_number = len(wlan_names)
    
    if eth_number == 0:
        logging.info("ERROR: no ethernet interface found")
        exit(1)
    
    logging.info("%s : starting editing", iptables_filename)
    iptables = open(iptables_filename, 'r+', encoding='utf-8')
    iptables_content = ""
    if wlan_number == 0:
        for line in iptables:
            if 'WIFI_INTERFACE_0' not in line:
                iptables_content += line.replace('ETH_INTERFACE_0', eth_names[0])
    else:
        iptables_content = iptables.read()
        iptables_content = iptables_content.replace('ETH_INTERFACE_0', eth_names[0])
        iptables_content = iptables_content.replace('WIFI_INTERFACE_0', wlan_names[0])
    
    iptables.seek(0)
    iptables.truncate()
    iptables.write(iptables_content)
    iptables.close()
        
    logging.info("%s : successfully edited", iptables_filename)
            
if __name__ == "__main__":
    main()