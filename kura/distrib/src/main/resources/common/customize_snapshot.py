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
        format='[customize_snapshot.py] %(asctime)s %(levelname)s %(message)s',
        level=logging.INFO,
        datefmt='%Y-%m-%d %H:%M:%S',
        handlers=[
            logging.StreamHandler()
        ]
    )
    
    snapshot_filename = "/opt/eclipse/kura/user/snapshots/snapshot_0.xml"
    
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
            firewall_configuration_template = "/opt/eclipse/kura/install/template_firewall_wlan"
    
    
    logging.info("%s : starting editing", snapshot_filename)
    snapshot = open(snapshot_filename, 'r+', encoding='utf-8')
    network_template = open(network_configuration_template, 'r', encoding='utf-8')
    firewall_template = open(firewall_configuration_template, 'r', encoding='utf-8')

    snapshot_content = snapshot.read()
    network_template_content = network_template.read()
    firewall_template_content = firewall_template.read()
    snapshot_content = snapshot_content.replace('NETWORK_CONFIGURATION', network_template_content)
    snapshot_content = snapshot_content.replace('FIREWALL_CONFIGURATION', firewall_template_content)
    
    interfaces_list = "lo"

    for i, eth_name in enumerate(eth_names):
        if i > 1:
            break
        snapshot_content = snapshot_content.replace('ETH_INTERFACE_' + str(i), eth_name)
        interfaces_list += "," + eth_name
        logging.info("%s : replaced ETH_INTERFACE_%s with %s", snapshot_filename, str(i), eth_name)

    for i, wlan_name in enumerate(wlan_names):
        if i > 0:
            break
        snapshot_content = snapshot_content.replace('WIFI_INTERFACE_' + str(i), wlan_name)
        interfaces_list += "," + wlan_name
        logging.info("%s : replaced WIFI_INTERFACE_%s with %s", snapshot_filename, str(i), wlan_name)
        
    snapshot_content = snapshot_content.replace('INTERFACES_LIST', interfaces_list)
    
    snapshot.seek(0)
    snapshot.truncate()
    snapshot.write(snapshot_content)
    snapshot.close()
        
    logging.info("%s : successfully edited", snapshot_filename)
            
if __name__ == "__main__":
    main()