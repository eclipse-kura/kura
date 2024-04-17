/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.net;

import static java.util.Objects.isNull;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.configuration.NetworkConfigurationConstants;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.modem.ModemConnectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModemConfigurationInterpreter {

    private static final Logger logger = LoggerFactory.getLogger(ModemConfigurationInterpreter.class);

    private static final String NET_INTERFACE = "net.interface.";

    private ModemConfigurationInterpreter() {

    }

    public static List<NetConfig> populateConfiguration(NetInterfaceAddressConfig netInterfaceAddress,
            Map<String, Object> props, String interfaceName) throws KuraException {

        // build the prefixes for all the properties associated with this interface
        StringBuilder sbPrefix = new StringBuilder();
        sbPrefix.append(NET_INTERFACE).append(interfaceName).append(".");

        String netIfPrefix = sbPrefix.append("config.").toString();

        int pppNum = getPPPNum(netIfPrefix, props);
        return populateConfiguration(netInterfaceAddress, props, interfaceName, pppNum);
    }

    public static List<NetConfig> populateConfiguration(NetInterfaceAddressConfig netInterfaceAddress,
            Map<String, Object> props, String interfaceName, int pppNum) throws KuraException {
        List<NetConfig> netConfigs = new ArrayList<>();

        if (isNull(props)) {
            return netConfigs;
        }

        ModemInterfaceAddressConfigImpl modemInterfaceAddressImpl = (ModemInterfaceAddressConfigImpl) netInterfaceAddress;

        // build the prefixes for all the properties associated with this interface
        StringBuilder sbPrefix = new StringBuilder();
        sbPrefix.append(NET_INTERFACE).append(interfaceName).append(".");

        String netIfPrefix = sbPrefix.append("config.").toString();

        // connection type
        String configConnType = netIfPrefix + "connection.type";
        if (props.containsKey(configConnType)) {
            ModemConnectionType connType = ModemConnectionType.PPP;
            String connTypeStr = (String) props.get(configConnType);
            if (connTypeStr != null && !connTypeStr.isEmpty()) {
                connType = ModemConnectionType.valueOf(connTypeStr);
            }

            logger.trace("Adding modem connection type: {}", connType);
            modemInterfaceAddressImpl.setConnectionType(connType);
        }

        // connection status
        String configConnStatus = netIfPrefix + "connection.status";
        if (props.containsKey(configConnStatus)) {
            ModemConnectionStatus connStatus = ModemConnectionStatus.UNKNOWN;
            String connStatusStr = (String) props.get(configConnStatus);
            if (connStatusStr != null && !connStatusStr.isEmpty()) {
                connStatus = ModemConnectionStatus.valueOf(connStatusStr);
            }

            logger.trace("Adding modem connection status: {}", connStatus);
            modemInterfaceAddressImpl.setConnectionStatus(connStatus);
        }

        ModemConfig modemConfig = getModemConfig(netIfPrefix, props);
        modemConfig.setPppNumber(pppNum);
        netConfigs.add(modemConfig);

        return netConfigs;
    }

    private static ModemConfig getModemConfig(String prefix, Map<String, Object> properties) throws KuraException {
        ModemConfig modemConfig = new ModemConfig();

        modemConfig.setApn(getApn(prefix, properties));
        modemConfig.setAuthType(getAuthenticationType(prefix, properties));
        modemConfig.setDataCompression(getDataCompression(prefix, properties));
        modemConfig.setDialString(getDialString(prefix, properties));
        modemConfig.setHeaderCompression(getHeaderCompression(prefix, properties));
        modemConfig.setIpAddress(getIpAddress(prefix, properties));
        modemConfig.setPassword(getPassword(prefix, properties));
        modemConfig.setPdpType(getPdpType(prefix, properties));
        modemConfig.setProfileID(getProfileId(prefix, properties));
        modemConfig.setPersist(isPersist(prefix, properties));
        modemConfig.setHoldoff(getHoldoff(prefix, properties));
        modemConfig.setMaxFail(getMaximumFailures(prefix, properties));
        modemConfig.setResetTimeout(getResetTimeout(prefix, properties));
        modemConfig.setIdle(getIdle(prefix, properties));
        modemConfig.setActiveFilter(getActiveFilter(prefix, properties));
        modemConfig.setLcpEchoInterval(getLcpEchoInterval(prefix, properties));
        modemConfig.setLcpEchoFailure(getLcpEchoFailure(prefix, properties));
        modemConfig.setUsername((String) properties.get(prefix + "username"));
        modemConfig.setEnabled(isEnabled(prefix, properties));
        modemConfig.setGpsEnabled(isGpsEnabled(prefix, properties));
        modemConfig.setDiversityEnabled(isDiversityEnabled(prefix, properties));

        return modemConfig;
    }

    private static boolean isGpsEnabled(String prefix, Map<String, Object> properties) {
        String key = prefix + "gpsEnabled";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_GPS_ENABLED_VALUE);
        return value != null ? (Boolean) value : NetworkConfigurationConstants.DEFAULT_MODEM_GPS_ENABLED_VALUE;
    }

    private static boolean isDiversityEnabled(String prefix, Map<String, Object> properties) {
        String key = prefix + "diversityEnabled";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_DIVERSITY_ENABLED_VALUE);
        return value != null ? (Boolean) value : NetworkConfigurationConstants.DEFAULT_MODEM_DIVERSITY_ENABLED_VALUE;
    }

    private static boolean isEnabled(String prefix, Map<String, Object> properties) {
        String key = prefix + "enabled";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_ENABLED_VALUE);
        return value != null ? (Boolean) value : NetworkConfigurationConstants.DEFAULT_MODEM_ENABLED_VALUE;
    }

    private static int getLcpEchoFailure(String prefix, Map<String, Object> properties) {
        String key = prefix + "lcpEchoFailure";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_LCP_ECHO_FAILURE_VALUE);
        return value != null ? (Integer) value : NetworkConfigurationConstants.DEFAULT_MODEM_LCP_ECHO_FAILURE_VALUE;
    }

    private static int getLcpEchoInterval(String prefix, Map<String, Object> properties) {
        String key = prefix + "lcpEchoInterval";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_LCP_ECHO_INTERVAL_VALUE);
        return value != null ? (Integer) value : NetworkConfigurationConstants.DEFAULT_MODEM_LCP_ECHO_INTERVAL_VALUE;
    }

    private static String getActiveFilter(String prefix, Map<String, Object> properties) {
        String key = prefix + "activeFilter";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_ACTIVE_FILTER_VALUE);
        return value != null ? (String) value : NetworkConfigurationConstants.DEFAULT_MODEM_ACTIVE_FILTER_VALUE;
    }

    private static int getIdle(String prefix, Map<String, Object> properties) {
        String key = prefix + "idle";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_IDLE_VALUE);
        return value != null ? (Integer) value : NetworkConfigurationConstants.DEFAULT_MODEM_IDLE_VALUE;
    }

    private static int getResetTimeout(String prefix, Map<String, Object> properties) {
        String key = prefix + "resetTimeout";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_RESET_TIMEOUT_VALUE);
        return value != null ? (Integer) value : NetworkConfigurationConstants.DEFAULT_MODEM_RESET_TIMEOUT_VALUE;
    }

    private static int getMaximumFailures(String prefix, Map<String, Object> properties) {
        String key = prefix + "maxFail";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_MAXFAIL_VALUE);
        return value != null ? (Integer) value : NetworkConfigurationConstants.DEFAULT_MODEM_MAXFAIL_VALUE;
    }

    private static boolean isPersist(String prefix, Map<String, Object> properties) {
        String key = prefix + "persist";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_PERSIST_VALUE);
        return value != null ? (Boolean) value : NetworkConfigurationConstants.DEFAULT_MODEM_PERSIST_VALUE;
    }

    private static int getHoldoff(String prefix, Map<String, Object> properties) {
        String key = prefix + "holdoff";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_HOLDOFF_VALUE);
        return value != null ? (Integer) value : NetworkConfigurationConstants.DEFAULT_MODEM_HOLDOFF_VALUE;
    }

    private static int getProfileId(String prefix, Map<String, Object> properties) {
        String key = prefix + "profileId";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_PROFILE_ID_VALUE);
        return value != null ? (Integer) value : NetworkConfigurationConstants.DEFAULT_MODEM_PROFILE_ID_VALUE;
    }

    private static PdpType getPdpType(String prefix, Map<String, Object> properties) {
        String key = prefix + "pdpType";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_PDP_TYPE_VALUE.name());
        return value != null ? parsePdpType((String) value) : NetworkConfigurationConstants.DEFAULT_MODEM_PDP_TYPE_VALUE;
    }

    private static PdpType parsePdpType(String pdpTypeString) {
        PdpType pdpType = PdpType.UNKNOWN;
        try {
            pdpType = PdpType.valueOf(pdpTypeString);
        } catch (IllegalArgumentException e) {
            pdpType = NetworkConfigurationConstants.DEFAULT_MODEM_PDP_TYPE_VALUE;
        }
        return pdpType;
    }

    private static Password getPassword(String prefix, Map<String, Object> properties) throws KuraException {
        Password password = null;
        String key = prefix + "password";
        Object psswdObj = properties.get(key);
        if (psswdObj instanceof Password) {
            password = (Password) psswdObj;
        } else if (psswdObj instanceof String) {
            password = new Password((String) psswdObj);
        } else if (psswdObj == null) {
            password = new Password("");
        } else {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, "Invalid password type.", key,
                    psswdObj.getClass());
        }
        return password;
    }

    private static IPAddress getIpAddress(String prefix, Map<String, Object> properties) throws KuraException {
        String ipAddressString = (String) properties.get(prefix + "ipAddress");
        IPAddress ipAddress = null;
        logger.trace("IP address is {}", ipAddressString);
        if (ipAddressString != null && !ipAddressString.isEmpty()) {
            ipAddress = parseIpAddress(ipAddressString);
        } else {
            logger.trace("IP address is null");
        }
        return ipAddress;
    }

    private static IPAddress parseIpAddress(String ipAddressString) throws KuraException {
        try {
            return IPAddress.parseHostAddress(ipAddressString);
        } catch (UnknownHostException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Could not parse ip address " + ipAddressString);
        }
    }

    private static int getHeaderCompression(String prefix, Map<String, Object> properties) {
        String key = prefix + "headerCompression";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_HEADER_COMPRESSION_VALUE);
        return value != null ? (Integer) value : NetworkConfigurationConstants.DEFAULT_MODEM_HEADER_COMPRESSION_VALUE;
    }

    private static String getDialString(String prefix, Map<String, Object> properties) {
        String key = prefix + "dialString";
        return (String) properties.get(key);
    }

    private static int getDataCompression(String prefix, Map<String, Object> properties) {
        String key = prefix + "dataCompression";
        Object value = properties.getOrDefault(key, NetworkConfigurationConstants.DEFAULT_MODEM_DATA_COMPRESSION_VALUE);
        return value != null ? (Integer) value : NetworkConfigurationConstants.DEFAULT_MODEM_DATA_COMPRESSION_VALUE;
    }

    private static AuthType getAuthenticationType(String prefix, Map<String, Object> properties) throws KuraException {
        String key = prefix + "authType";
        String authTypeString = (String) properties.get(key);
        AuthType authType;
        logger.trace("Auth type is {}", authTypeString);
        if (authTypeString != null && !authTypeString.isEmpty()) {
            authType = parseAuthenticationType(authTypeString);
        } else {
            logger.trace("Auth type is null");
            authType = NetworkConfigurationConstants.DEFAULT_MODEM_AUTH_TYPE_VALUE;
        }
        return authType;
    }

    private static AuthType parseAuthenticationType(String authTypeString) throws KuraException {
        AuthType authType = NetworkConfigurationConstants.DEFAULT_MODEM_AUTH_TYPE_VALUE;
        try {
            authType = AuthType.valueOf(authTypeString);
        } catch (IllegalArgumentException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Could not parse auth type " + authTypeString);
        }
        return authType;
    }

    private static String getApn(String prefix, Map<String, Object> properties) {
        String key = prefix + "apn";
        return (String) properties.get(key);
    }

    private static int getPPPNum(String prefix, Map<String, Object> properties) {
        String key = prefix + "pppNum";
        return (int) properties.getOrDefault(key, 0);
    }

}
