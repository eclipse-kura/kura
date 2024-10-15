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
import argparse
from network_tools import get_eth_wlan_interfaces_names

def main():
    logging.basicConfig(
        format='[customize_snapshot.py] %(asctime)s %(levelname)s %(message)s',
        level=logging.INFO,
        datefmt='%Y-%m-%d %H:%M:%S',
        handlers=[
            logging.StreamHandler()
        ]
    )
    
    SNAPSHOT_FILENAME = "/opt/eclipse/kura/user/snapshots/snapshot_0.xml"
    
    parser = argparse.ArgumentParser(description="Customize snapshot_0.xml file", usage='%(prog)s is_networking_profile')
    parser.add_argument('--networking_profile', action='store_true', help='Specifies if this is a profile with or without networking')
    parser.set_defaults(networking_profile=False)
    args = parser.parse_args()
    
    if not args.networking_profile:
        with open(SNAPSHOT_FILENAME, 'r', encoding='utf-8') as snapshot:
            snapshot_content = snapshot.read()
        
        snapshot_content_updated = ""
        for line in snapshot_content.split("\n"):
            if 'NETWORK_CONFIGURATION' not in line and 'FIREWALL_CONFIGURATION' not in line:
                snapshot_content_updated += (line + "\n")
                
        with open(SNAPSHOT_FILENAME, 'w', encoding='utf-8') as snapshot:
            snapshot.write(snapshot_content_updated)  
        logging.info("%s : successfully edited", SNAPSHOT_FILENAME)     
        
    else:
        (eth_names, wlan_names) = get_eth_wlan_interfaces_names()
        
        eth_number = len(eth_names)
        wlan_number = len(wlan_names)
        
        if eth_number == 0:
            logging.info("ERROR: no ethernet interface found")
            exit(1)
        
        network_configuration_template = "/opt/eclipse/kura/install/template_one_eth_no_wlan"
        firewall_configuration_template = "/opt/eclipse/kura/install/template_firewall_eth"
        if eth_number == 1:
            if wlan_number == 0:
                network_configuration_template = "/opt/eclipse/kura/install/template_one_eth_no_wlan"
                firewall_configuration_template = "/opt/eclipse/kura/install/template_firewall_eth"
            else:
                network_configuration_template = "/opt/eclipse/kura/install/template_one_eth_one_wlan"
                firewall_configuration_template = "/opt/eclipse/kura/install/template_firewall_eth_wlan"
        else:
            if wlan_number == 0:
                network_configuration_template = "/opt/eclipse/kura/install/template_multiple_eth_no_wlan"
                firewall_configuration_template = "/opt/eclipse/kura/install/template_firewall_eth"
            else:
                network_configuration_template = "/opt/eclipse/kura/install/template_multiple_eth_one_wlan"
                firewall_configuration_template = "/opt/eclipse/kura/install/template_firewall_eth_wlan"
        
        
        logging.info("%s : starting editing", SNAPSHOT_FILENAME)
        with open(SNAPSHOT_FILENAME, 'r', encoding='utf-8') as snapshot:
            snapshot_content = snapshot.read()
            
        with open(network_configuration_template, 'r', encoding='utf-8') as network_template:
            network_template_content = network_template.read()
        
        with open(firewall_configuration_template, 'r', encoding='utf-8') as firewall_template:
            firewall_template_content = firewall_template.read()
            
        snapshot_content = snapshot_content.replace('NETWORK_CONFIGURATION', network_template_content)
        snapshot_content = snapshot_content.replace('FIREWALL_CONFIGURATION', firewall_template_content)
        
        interfaces_list = "lo"
    
        for i, eth_name in enumerate(eth_names[:2]):
            snapshot_content = snapshot_content.replace('ETH_INTERFACE_' + str(i), eth_name)
            interfaces_list += "," + eth_name
            logging.info("%s : replaced ETH_INTERFACE_%s with %s", SNAPSHOT_FILENAME, str(i), eth_name)
    
        for i, wlan_name in enumerate(wlan_names[:1]):
            snapshot_content = snapshot_content.replace('WIFI_INTERFACE_' + str(i), wlan_name)
            interfaces_list += "," + wlan_name
            logging.info("%s : replaced WIFI_INTERFACE_%s with %s", SNAPSHOT_FILENAME, str(i), wlan_name)
            
        snapshot_content = snapshot_content.replace('INTERFACES_LIST', interfaces_list)
        
        with open(SNAPSHOT_FILENAME, 'w', encoding='utf-8') as snapshot:
            snapshot.write(snapshot_content)
            
        logging.info("%s : successfully edited", SNAPSHOT_FILENAME)
            
if __name__ == "__main__":
    main()