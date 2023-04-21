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
import shutil
import logging
from os.path import exists

ETHERNET_TYPES = ['ethernet', 'eth', 'wired']
WIRELESS_TYPES = ['wifi', 'wireless']
INTERFACES_PATH = "/etc/network/interfaces"
INTERFACES_OLD_PATH = INTERFACES_PATH + ".old"
INTERFACES_TMP_PATH = INTERFACES_PATH + ".tmp"

def get_eth_wlan_interfaces_names():
    """Reads the network interface names using 'nmcli dev' command.

    It is assumed that at least one ethernet interface is present.

    Returns:
        tuple of lists (eth_names, wlan_names) where:
            'eth_names' are the found ethernet interface names ordered by name;
            'wlan_names' are the found wireless interface names ordered by name, might be an empty list.
    """
    cmd_output = subprocess.check_output(['nmcli', 'dev']).decode(sys.stdout.encoding).strip()
    # list comprehension to remove empty items
    lines = [x.strip() for x in cmd_output.split('\n') if len(x.strip()) > 0]

    # removing header
    del lines[0]

    inteface_names = list()

    for line in lines:
        row = [x.strip() for x in line.split(' ') if len(x.strip()) > 0]
        
        interface_name = row[0].lower()
        interface_type = row[1].lower()

        if interface_type in ETHERNET_TYPES or interface_type in WIRELESS_TYPES:
            inteface_names.append(interface_name)
        
    inteface_names.sort()

    logging.info("Found interfaces: %s", inteface_names)

    return (inteface_names)

def comment_paragraph(paragraph):
    """Comment a paragraph

    Returns:
        a paragraph whose lines start with #
    """
    commented = ""
    for line in paragraph.split("\n"):
        if (not line.startswith("#")):
            commented += "#" + line + "\n" 
        else:
            commented += line + "\n" 
    commented += "\n"
    return (commented)

def main():
    logging.basicConfig(
        format='[comment_interfaces_file.py] %(asctime)s %(levelname)s %(message)s',
        level=logging.INFO,
        datefmt='%Y-%m-%d %H:%M:%S',
        handlers=[
            logging.StreamHandler()
        ]
    )

    if (not exists(INTERFACES_PATH)):
        logging.info("File %s does not exist.", INTERFACES_PATH)
        exit(0)
    
    logging.info("Backup %s file.", INTERFACES_PATH)
    shutil.copyfile(INTERFACES_PATH, INTERFACES_OLD_PATH)
    shutil.copyfile(INTERFACES_PATH, INTERFACES_TMP_PATH)
    
    logging.info("Search for interfaces...")
    interface_names = get_eth_wlan_interfaces_names()
    
    for name in interface_names:
        with open(INTERFACES_TMP_PATH, 'r+', encoding='utf-8') as file_to_edit:
            content_paragraph = file_to_edit.read().split("\n\n")
        output = ""
        for paragraph in content_paragraph:
            if name in paragraph:
                logging.info("Comment %s configuration.", name)
                output += comment_paragraph(paragraph)
            else:
                output += paragraph + "\n\n"
        f = open(INTERFACES_TMP_PATH, "w")
        f.write(output)
        f.close()

    logging.info("Replace %s file.", INTERFACES_PATH)
    shutil.move(INTERFACES_TMP_PATH, INTERFACES_PATH)
    
if __name__ == "__main__":
    main()