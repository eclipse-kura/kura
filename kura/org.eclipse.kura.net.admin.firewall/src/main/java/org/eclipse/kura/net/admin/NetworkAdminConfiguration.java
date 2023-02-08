/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin;

public enum NetworkAdminConfiguration {
    PLATFORM_INTERFACES,
    CONFIG_MTU,
    CONFIG_AUTOCONNECT,
    CONFIG_DRIVER,
    CONFIG_IPV4_DHCP_CLIENT_ENABLED,
    CONFIG_IPV4_ADDRESS,
    CONFIG_IPV4_PREFIX,
    CONFIG_IPV4_GATEWAY,
    CONFIG_DNS_SERVERS,
    CONFIG_WINS_SERVERS,
    CONFIG_IPV4_DHCP_SERVER_ENABLED,
    CONFIG_IPV4_DHCP_SERVER_DEFAULT_LEASE_TIME,
    CONFIG_IPV4_DHCP_SERVER_MAX_LEASE_TIME,
    CONFIG_IPV4_DHCP_SERVER_PREFIX,
    CONFIG_IPV4_DHCP_SERVER_RANGE_START,
    CONFIG_IPV4_DHCP_SERVER_RANGE_END,
    CONFIG_IPV4_DHCP_SERVER_PASS_DNS,
    CONFIG_IPV4_DHCP_SERVER_NAT_ENABLED,
    CONFIG_WIFI_MODE,
    CONFIG_WIFI_ADHOC_SSID,
    CONFIG_WIFI_ADHOC_HARDWARE_MODE,
    CONFIG_WIFI_ADHOC_RADIO_MODE,
    CONFIG_WIFI_ADHOC_SECURITY_TYPE,
    CONFIG_WIFI_ADHOC_PASSPHRASE,
    CONFIG_WIFI_ADHOC_CHANNEL,
    CONFIG_WIFI_ADHOC_BGSCAN,
    CONFIG_WIFI_INFRA_SSID,
    CONFIG_WIFI_INFRA_HARDWARE_MODE,
    CONFIG_WIFI_INFRA_RADIO_MODE,
    CONFIG_WIFI_INFRA_SECURITY_TYPE,
    CONFIG_WIFI_INFRA_PASSPHRASE,
    CONFIG_WIFI_INFRA_PAIRWISE_CIPHERS,
    CONFIG_WIFI_INFRA_GROUP_CIPHERS,
    CONFIG_WIFI_INFRA_CHANNEL,
    CONFIG_WIFI_INFRA_BGSCAN,
    CONFIG_WIFI_MASTER_SSID,
    CONFIG_WIFI_MASTER_BROADCAST_ENABLED,
    CONFIG_WIFI_MASTER_HARDWARE_MODE,
    CONFIG_WIFI_MASTER_RADIO_MODE,
    CONFIG_WIFI_MASTER_SECURITY_TYPE,
    CONFIG_WIFI_MASTER_PASSPHRASE,
    CONFIG_WIFI_MASTER_CHANNEL,
    CONFIG_WIFI_MASTER_BGSCAN,
    USB_PORT,
    USB_MANUFACTURER,
    USB_PRODUCT,
    USB_MANUFACTURER_ID,
    USB_PRODUCT_ID,
    WIFI_CAPABILITIES
}
