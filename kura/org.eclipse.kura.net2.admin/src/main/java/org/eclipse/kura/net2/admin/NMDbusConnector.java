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
package org.eclipse.kura.net2.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Pattern;

import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.networkmanager.Device;
import org.freedesktop.networkmanager.Settings;
import org.freedesktop.networkmanager.settings.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMDbusConnector {

    private static final Logger logger = LoggerFactory.getLogger(NMDbusConnector.class);
    private static final String NM_BUS_NAME = "org.freedesktop.NetworkManager";
    private static final String NM_BUS_PATH = "/org/freedesktop/NetworkManager";

    private DBusConnection dbusConnection;
    private NetworkManager nm;
    private Map<String, Object> configurationCache;

    public NMDbusConnector(DBusConnection dbusConnection) throws DBusException {
        this.dbusConnection = Objects.requireNonNull(dbusConnection);
        this.nm = this.dbusConnection.getRemoteObject(NM_BUS_NAME, NM_BUS_PATH, NetworkManager.class);

        Map<String, String> getPermissions = nm.GetPermissions();
        for (Entry<String, String> entry : getPermissions.entrySet()) {
            if (!entry.getValue().equals("yes")) {
                logger.warn("Missing permission for {}", entry.getKey());
            }
        }
    }

    public void apply(Map<String, Object> networkConfiguration) throws DBusException {
        logger.info("Applying configuration using NetworkManager Dbus connector");

        List<String> modifiedInterfaces = getModifiedInterfaces(
                (String) networkConfiguration.get("modified.interface.names"));

        for (String iface : modifiedInterfaces) {
            logger.info("Modified: {}", iface);

            NMDeviceType deviceType = getDeviceTypeByIpIface(iface);

            logger.info("DeviceType: {}", deviceType);

            if (deviceType == NMDeviceType.NM_DEVICE_TYPE_ETHERNET) {
                Device ifaceDevice = getDeviceByIpIface(iface);

                String connectionUuid = getAppliedConnectionUuid(ifaceDevice); // What if there's no applied connection?
                Connection connection = getConnectionByUuid(connectionUuid);

                Map<String, Map<String, Variant<?>>> currentConnectionSettings = connection.GetSettings();

                Map<String, Variant<?>> connectionMap = buildConnectionSettings(currentConnectionSettings);
                Map<String, Variant<?>> ipv4Map = buildIpv4Settings(networkConfiguration, iface);

                Map<String, Map<String, Variant<?>>> newConnectionSettings = new HashMap<>();
                newConnectionSettings.put("ipv4", ipv4Map);
                newConnectionSettings.put("connection", connectionMap);

                connection.Update(newConnectionSettings);
                connection.Save();

                nm.ActivateConnection(new DBusPath(connection.getObjectPath()),
                        new DBusPath(ifaceDevice.getObjectPath()), new DBusPath("/"));
            } else {
                logger.warn("Device type \"{}\" currently not supported", deviceType);
                return;
            }
        }

    }

    private List<String> getModifiedInterfaces(String modifiedInterfaces) {
        List<String> modifiedInterfaceNames = new ArrayList<>();
        Pattern comma = Pattern.compile(",");
        if (modifiedInterfaces != null) {
            comma.splitAsStream(modifiedInterfaces).filter(s -> !s.trim().isEmpty())
                    .forEach(modifiedInterfaceNames::add);
        }

        return modifiedInterfaceNames;
    }

    private List<String> getDNSServers(String dnsServersString) {
        List<String> dnsServers = new ArrayList<>();
        Pattern comma = Pattern.compile(",");
        if (dnsServersString != null) {
            comma.splitAsStream(dnsServersString).filter(s -> !s.trim().isEmpty()).forEach(dnsServers::add);
        }

        return dnsServers;
    }

    private NMDeviceType getDeviceTypeByIpIface(String iface) throws DBusException {
        DBusPath ifaceDevicePath = nm.GetDeviceByIpIface(iface);
        Properties deviceProperties = dbusConnection.getRemoteObject(NM_BUS_NAME, ifaceDevicePath.getPath(),
                Properties.class);

        return NMDeviceType.fromUInt32(deviceProperties.Get("org.freedesktop.NetworkManager.Device", "DeviceType"));

    }

    private Device getDeviceByIpIface(String iface) throws DBusException {
        DBusPath ifaceDevicePath = nm.GetDeviceByIpIface(iface);
        return dbusConnection.getRemoteObject(NM_BUS_NAME, ifaceDevicePath.getPath(), Device.class);
    }

    private Connection getConnectionByUuid(String uuid) throws DBusException {
        Settings settings = this.dbusConnection.getRemoteObject(NM_BUS_NAME, "/org/freedesktop/NetworkManager/Settings",
                Settings.class);

        DBusPath connectionPath = settings.GetConnectionByUuid(uuid);
        return dbusConnection.getRemoteObject(NM_BUS_NAME, connectionPath.getPath(), Connection.class);
    }

    private String getAppliedConnectionUuid(Device dev) {
        Map<String, Map<String, Variant<?>>> connectionSettings = dev.GetAppliedConnection(new UInt32(0))
                .getConnection();
        return String.valueOf(connectionSettings.get("connection").get("uuid")).replaceAll("\\[|\\]", "");
    }

    private Map<String, Variant<?>> buildConnectionSettings(Map<String, Map<String, Variant<?>>> connectionSettings) {
        Map<String, Variant<?>> connectionMap = new HashMap<>();
        for (String key : connectionSettings.get("connection").keySet()) {
            connectionMap.put(key, connectionSettings.get("connection").get(key));
        }

        return connectionMap;
    }

    private Map<String, Variant<?>> buildIpv4Settings(Map<String, Object> networkConfiguration, String iface) {
        Map<String, Variant<?>> ipv4Map = new HashMap<>();

        String dhcpClient4EnabledProperty = String.format("net.interface.%s.config.dhcpClient4.enabled", iface);
        Boolean dhcpClient4Enabled = (Boolean) networkConfiguration.get(dhcpClient4EnabledProperty);

        if (Boolean.FALSE.equals(dhcpClient4Enabled)) {
            ipv4Map.put("method", new Variant<>("manual"));

            String dhcpClient4AddressProperty = String.format("net.interface.%s.config.ip4.address", iface);
            String dhcpClient4Address = (String) networkConfiguration.get(dhcpClient4AddressProperty);

            String dhcpClient4PrefixProperty = String.format("net.interface.%s.config.ip4.prefix", iface);
            Short dhcpClient4Prefix = (Short) networkConfiguration.get(dhcpClient4PrefixProperty);

            Map<String, Variant<?>> address = new HashMap<>();
            address.put("address", new Variant<>(dhcpClient4Address));
            address.put("prefix", new Variant<>(new UInt32(dhcpClient4Prefix)));

            List<Map<String, Variant<?>>> addressData = Arrays.asList(address);
            ipv4Map.put("address-data", new Variant<>(addressData, "aa{sv}"));

            String dhcpClient4DNSProperty = String.format("net.interface.%s.config.ip4.dnsServers", iface);
            if (networkConfiguration.containsKey(dhcpClient4DNSProperty)) {
                String dhcpClient4DNS = (String) networkConfiguration.get(dhcpClient4DNSProperty);
                ipv4Map.put("ignore-auto-dns", new Variant<>(true));
                ipv4Map.put("dns-search", new Variant<>(getDNSServers(dhcpClient4DNS)));
            } else {
                ipv4Map.put("ignore-auto-dns", new Variant<>(false));
            }

            String dhcpClient4GatewayProperty = String.format("net.interface.%s.config.ip4.gateway", iface);
            if (networkConfiguration.containsKey(dhcpClient4GatewayProperty)) {
                String dhcpClient4Gateway = (String) networkConfiguration.get(dhcpClient4GatewayProperty);
                ipv4Map.put("gateway", new Variant<>(dhcpClient4Gateway));
            }

        } else {
            ipv4Map.put("method", new Variant<>("auto"));
        }

        return ipv4Map;
    }

}
