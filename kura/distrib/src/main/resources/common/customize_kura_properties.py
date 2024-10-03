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
        format='[customize_kura_properties.py] %(asctime)s %(levelname)s %(message)s',
        level=logging.INFO,
        datefmt='%Y-%m-%d %H:%M:%S',
        handlers=[
            logging.StreamHandler()
        ]
    )
    
    board_name = sys.argv[1:] # remove script name from args
    
    if len(board_name) < 1:
        logging.info("ERROR: invalid arguments length")
        exit(1)
        
    kura_properties_filename = "/opt/eclipse/kura/framework/kura.properties"
    
    (eth_names, wlan_names) = get_eth_wlan_interfaces_names()
    
    if len(eth_names) == 0:
        logging.info("ERROR: no ethernet interface found")
        exit(1)
    
    logging.info("%s : starting editing", kura_properties_filename)
    kura_properties = open(kura_properties_filename, 'r+', encoding='utf-8')

    kura_properties_content = kura_properties.read()
    kura_properties_content = kura_properties_content.replace('device_name', board_name[0])
    kura_properties_content = kura_properties_content.replace('eth0', eth_names[0])
    
    kura_properties.seek(0)
    kura_properties.truncate()
    kura_properties.write(kura_properties_content)
    kura_properties.close()
        
    logging.info("%s : successfully edited", kura_properties_filename)
            
if __name__ == "__main__":
    main()