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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

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
    private static final String NM_SETTINGS_PATH = "/org/freedesktop/NetworkManager/Settings";

    private static NMDbusConnector instance;
    private DBusConnection dbusConnection;
    private NetworkManager nm;
    private Map<String, Object> configurationCache;

    private NMDbusConnector(DBusConnection dbusConnection) throws DBusException {
        this.dbusConnection = Objects.requireNonNull(dbusConnection);
        this.nm = this.dbusConnection.getRemoteObject(NM_BUS_NAME, NM_BUS_PATH, NetworkManager.class);
    }

    public static NMDbusConnector createInstance() throws DBusException {
        instance = new NMDbusConnector(DBusConnection.getConnection(DBusConnection.DEFAULT_SYSTEM_BUS_ADDRESS));
        return instance;
    }

    public static NMDbusConnector createInstance(DBusConnection dbusConnection) throws DBusException {
        instance = new NMDbusConnector(dbusConnection);
        return instance;
    }

    public void closeConnection() {
        dbusConnection.disconnect();
    }

    public static NMDbusConnector getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "Instance not created yet. Please use NMDbusConnector.createInstance() first");
        }

        return instance;
    }

    public void checkPermissions() {
        Map<String, String> getPermissions = nm.GetPermissions();
        for (Entry<String, String> entry : getPermissions.entrySet()) {
            if (!entry.getValue().equals("yes")) {
                logger.warn("Missing permission for {}", entry.getKey());
            }
        }
    }

    public DBusConnection getDbusConnection() {
        return this.dbusConnection;
    }

    public void apply(Map<String, Object> networkConfiguration) throws DBusException {
        logger.info("Applying configuration using NetworkManager Dbus connector");

        List<String> netInterfaces = NMSettingsConverter
                .splitCommaSeparatedStrings((String) networkConfiguration.get("net.interfaces"));

        for (String iface : netInterfaces) {
            Device device = getDeviceByIpIface(iface); // What if no device matches?
            NMDeviceType deviceType = getDeviceType(device);

            logger.info("Settings iface \"{}\":{}", iface, deviceType);

            if (deviceType == NMDeviceType.NM_DEVICE_TYPE_ETHERNET) {
                Connection connection = getAppliedConnection(device); // What if there's no applied connection?

                Map<String, Variant<?>> connectionMap = copyConnectionSettings(connection.GetSettings());
                Map<String, Variant<?>> ipv4Map = NMSettingsConverter.buildIpv4Settings(networkConfiguration, iface);

                Map<String, Map<String, Variant<?>>> newConnectionSettings = new HashMap<>();
                newConnectionSettings.put("ipv4", ipv4Map);
                newConnectionSettings.put("connection", connectionMap);

                connection.Update(newConnectionSettings);
                connection.Save();

                nm.ActivateConnection(new DBusPath(connection.getObjectPath()), new DBusPath(device.getObjectPath()),
                        new DBusPath("/"));
            } else {
                logger.warn("Device type \"{}\" currently not supported", deviceType);
                return;
            }
        }

    }

    private NMDeviceType getDeviceType(Device device) throws DBusException {
        Properties deviceProperties = dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);

        return NMDeviceType.fromUInt32(deviceProperties.Get("org.freedesktop.NetworkManager.Device", "DeviceType"));
    }

    private Device getDeviceByIpIface(String iface) throws DBusException {
        DBusPath ifaceDevicePath = nm.GetDeviceByIpIface(iface);
        return dbusConnection.getRemoteObject(NM_BUS_NAME, ifaceDevicePath.getPath(), Device.class);
    }

    private Connection getAppliedConnection(Device dev) throws DBusException {
        Map<String, Map<String, Variant<?>>> connectionSettings = dev.GetAppliedConnection(new UInt32(0))
                .getConnection();
        String uuid = String.valueOf(connectionSettings.get("connection").get("uuid")).replaceAll("\\[|\\]", "");

        Settings settings = this.dbusConnection.getRemoteObject(NM_BUS_NAME, NM_SETTINGS_PATH, Settings.class);

        DBusPath connectionPath = settings.GetConnectionByUuid(uuid);
        return dbusConnection.getRemoteObject(NM_BUS_NAME, connectionPath.getPath(), Connection.class);
    }

    private Map<String, Variant<?>> copyConnectionSettings(Map<String, Map<String, Variant<?>>> connectionSettings) {
        Map<String, Variant<?>> connectionMap = new HashMap<>();
        for (String key : connectionSettings.get("connection").keySet()) {
            connectionMap.put(key, connectionSettings.get("connection").get(key));
        }

        return connectionMap;
    }
}