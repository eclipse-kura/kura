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
    