/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.configuration;

import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;

public final class NetworkConfigurationConstants {

    // Modem properties default values
    public static final boolean DEFAULT_MODEM_PERSIST_VALUE = true;
    public static final int DEFAULT_MODEM_HOLDOFF_VALUE = 1;
    public static final int DEFAULT_MODEM_MAXFAIL_VALUE = 5;
    public static final int DEFAULT_MODEM_RESET_TIMEOUT_VALUE = 5;
    public static final int DEFAULT_MODEM_IDLE_VALUE = 95;
    public static final String DEFAULT_MODEM_ACTIVE_FILTER_VALUE = "inbound";
    public static final int DEFAULT_MODEM_LCP_ECHO_FAILURE_VALUE = 0;
    public static final int DEFAULT_MODEM_LCP_ECHO_INTERVAL_VALUE = 0;
    public static final boolean DEFAULT_MODEM_GPS_ENABLED_VALUE = false;
    public static final boolean DEFAULT_MODEM_DIVERSITY_ENABLED_VALUE = false;
    public static final boolean DEFAULT_MODEM_ENABLED_VALUE = false;
    public static final int DEFAULT_MODEM_PROFILE_ID_VALUE = 0;
    public static final int DEFAULT_MODEM_DATA_COMPRESSION_VALUE = 0;
    public static final int DEFAULT_MODEM_HEADER_COMPRESSION_VALUE = 0;
    public static final PdpType DEFAULT_MODEM_PDP_TYPE_VALUE = PdpType.IP;
    public static final AuthType DEFAULT_MODEM_AUTH_TYPE_VALUE = AuthType.NONE;
    public static final int DEFAULT_MODEM_PPP_NUMBER_VALUE = 0;

    // Wifi properties default values
    public static final boolean DEFAULT_WIFI_BROADCAST_VALUE = false;
    public static final WifiRadioMode DEFAULT_WIFI_RADIO_MODE_VALUE = WifiRadioMode.RADIO_MODE_80211b;
    public static final WifiSecurity DEFAULT_WIFI_SECURITY_VALUE = WifiSecurity.NONE;
    public static final String DEFAULT_WIFI_CHANNEL_VALUE = "1";
    public static final boolean DEFAULT_WIFI_IGNORE_SSID_VALUE = false;
    public static final WifiCiphers DEFAULT_WIFI_PAIRWISE_CIPHERS_VALUE = WifiCiphers.CCMP_TKIP;
    public static final WifiCiphers DEFAULT_WIFI_GROUP_CIPHERS_VALUE = WifiCiphers.CCMP_TKIP;
    public static final String DEFAULT_WIFI_BGSCAN_VALUE = "";
    public static final boolean DEFAULT_WIFI_PING_AP_VALUE = false;
    public static final WifiMode DEFAULT_WIFI_MODE = WifiMode.UNKNOWN;

    // IP properties default values
    public static final boolean DEFAULT_IPV4_DHCP_SERVER_ENABLED_VALUE = false;
    public static final int DEFAULT_IPV4_DHCP_SERVER_DEFAULT_LEASE_TIME_VALUE = -1;
    public static final int DEFAULT_IPV4_DHCP_SERVER_MAX_LEASE_TIME_VALUE = -1;
    public static final Short DEFAULT_IPV4_DHCP_SERVER_PREFIX_VALUE = -1;
    public static final boolean DEFAULT_IPV4_DHCP_PASS_DNS_VALUE = false;
    public static final boolean DEFAULT_IPV4_DHCP_SERVER_NAT_ENABLED_VALUE = false;
    public static final boolean DEFAULT_IPV4_DHCP_CLIENT_ENABLED_VALUE = false;
    public static final NetInterfaceType DEFAULT_INTERFACE_TYPE_VALUE = NetInterfaceType.UNKNOWN;
    public static final NetInterfaceStatus DEFAULT_IPV4_STATUS_VALUE = NetInterfaceStatus.netIPv4StatusDisabled;
    public static final boolean DEFAULT_AUTOCONNECT_VALUE = false;
    public static final int DEFAULT_PROMISC_VALUE = -1;
    public static final short DEFAULT_IPV4_PREFIX_VALUE = -1;
    public static final short DEFAULT_IPV6_PREFIX_VALUE = -1;
    public static final NetInterfaceStatus DEFAULT_IPV6_STATUS_VALUE = NetInterfaceStatus.netIPv6StatusDisabled;
    public static final String DEFAULT_IPV6_ADDRESS_METHOD_VALUE = "netIPv6MethodAuto";

    // VLAN properties default values
    public static final int DEFAULT_VLAN_FLAGS_VALUE = 1;

    private NetworkConfigurationConstants() {
        // Do nothing...
    }

}
