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
package org.eclipse.kura.nm.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
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
    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";
    private static final String NM_SETTINGS_BUS_PATH = "/org/freedesktop/NetworkManager/Settings";

    private static final List<NMDeviceType> SUPPORTED_DEVICES = Arrays.asList(NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
            NMDeviceType.NM_DEVICE_TYPE_WIFI);
    private static final List<KuraInterfaceStatus> SUPPORTED_STATUSES = Arrays.asList(KuraInterfaceStatus.DISABLED,
            KuraInterfaceStatus.ENABLEDLAN, KuraInterfaceStatus.ENABLEDWAN, KuraInterfaceStatus.UNMANAGED);

    private static NMDbusConnector instance;
    private DBusConnection dbusConnection;
    private NetworkManager nm;

    private Map<String, Object> cachedConfiguration = null;

    private NMDbusConnector(DBusConnection dbusConnection) throws DBusException {
        this.dbusConnection = Objects.requireNonNull(dbusConnection);
        this.nm = this.dbusConnection.getRemoteObject(NM_BUS_NAME, NM_BUS_PATH, NetworkManager.class);

        this.dbusConnection.addSigHandler(NetworkManager.DeviceAdded.class, new NMDeviceAddedHandler());
    }

    public static synchronized NMDbusConnector getInstance() throws DBusException {
        return getInstance(DBusConnection.getConnection(DBusConnection.DEFAULT_SYSTEM_BUS_ADDRESS));
    }

    public static synchronized NMDbusConnector getInstance(DBusConnection dbusConnection) throws DBusException {
        if (Objects.isNull(instance)) {
            instance = new NMDbusConnector(dbusConnection);
        }

        return instance;
    }

    public DBusConnection getDbusConnection() {
        return this.dbusConnection;
    }

    public void closeConnection() {
        dbusConnection.disconnect();
    }

    public void checkPermissions() {
        Map<String, String> getPermissions = nm.GetPermissions();
        for (Entry<String, String> entry : getPermissions.entrySet()) {
            logger.info("Permission for {}: {}", entry.getKey(), entry.getValue());
        }
    }

    public void checkVersion() throws DBusException {
        Properties nmProperties = dbusConnection.getRemoteObject(NM_BUS_NAME, NM_BUS_PATH, Properties.class);

        String nmVersion = nmProperties.Get(NM_BUS_NAME, "Version");

        logger.info("NM Version: {}", nmVersion);
    }

    public synchronized void apply(Map<String, Object> networkConfiguration) throws DBusException {
        doApply(networkConfiguration);
        this.cachedConfiguration = networkConfiguration;
    }

    public synchronized void apply() throws DBusException {
        if (Objects.isNull(this.cachedConfiguration)) {
            logger.warn("No cached network configuration found.");
            return;
        }
        doApply(this.cachedConfiguration);
    }

    private synchronized void doApply(Map<String, Object> networkConfiguration) throws DBusException {
        logger.info("Applying configuration using NetworkManager Dbus connector");

        NetworkProperties properties = new NetworkProperties(networkConfiguration);

        List<String> configuredInterfaces = properties.getStringList("net.interfaces");
        manageConfiguredInterfaces(configuredInterfaces, properties);

        List<Device> availableInterfaces = getAllDevices();
        manageNonConfiguredInterfaces(configuredInterfaces, availableInterfaces);
    }

    private synchronized void manageConfiguredInterfaces(List<String> configuredInterfaces,
            NetworkProperties properties) throws DBusException {
        List<String> availableInterfaces = getAllDeviceInterfaceNames();

        for (String iface : configuredInterfaces) {
            if (!availableInterfaces.contains(iface)) {
                logger.debug("Configured device \"{}\" not available on the system. Ignoring configuration.", iface);
                continue;
            }

            Device device = getDeviceByIpIface(iface);
            NMDeviceType deviceType = getDeviceType(device);

            KuraInterfaceStatus ip4Status = KuraInterfaceStatus
                    .fromString(properties.get(String.class, "net.interface.%s.config.ip4.status", iface));
            KuraInterfaceStatus ip6Status = ip4Status == KuraInterfaceStatus.UNMANAGED ? KuraInterfaceStatus.UNMANAGED
                    : KuraInterfaceStatus.DISABLED; // Temporary solution while we wait to add ipv6 support.
            NMDeviceEnable deviceStatus = NMDeviceEnable.fromKuraInterfaceStatus(ip4Status, ip6Status);

            if (!SUPPORTED_DEVICES.contains(deviceType) || !SUPPORTED_STATUSES.contains(ip4Status)
                    || !SUPPORTED_STATUSES.contains(ip6Status)) {
                logger.warn("Device \"{}\" of type \"{}\" with status \"{}\"/\"{}\" currently not supported", iface,
                        deviceType, ip4Status, ip6Status);
                continue;
            }

            logger.info("Settings iface \"{}\":{}", iface, deviceType);

            if (deviceStatus == NMDeviceEnable.DISABLED) {
                disable(device);
            } else if (deviceStatus == NMDeviceEnable.UNMANAGED) {
                setDeviceManaged(device, false);
            } else { // NMDeviceEnable.ENABLED
                if (!isDeviceManaged(device)) {
                    setDeviceManaged(device, true);
                }

                Optional<Connection> connection = getAppliedConnection(device);
                Map<String, Map<String, Variant<?>>> newConnectionSettings = NMSettingsConverter
                        .buildSettings(properties, connection, iface, deviceType);

                logger.info("New settings: {}", newConnectionSettings);

                if (connection.isPresent()) {
                    logger.info("Current settings: {}", connection.get().GetSettings());

                    connection.get().Update(newConnectionSettings);
                    nm.ActivateConnection(new DBusPath(connection.get().getObjectPath()),
                            new DBusPath(device.getObjectPath()), new DBusPath("/"));
                } else {
                    nm.AddAndActivateConnection(newConnectionSettings, new DBusPath(device.getObjectPath()),
                            new DBusPath("/"));
                }
            }
        }
    }

    private synchronized void manageNonConfiguredInterfaces(List<String> configuredInterfaces,
            List<Device> availableInterfaces) throws DBusException {
        for (Device device : availableInterfaces) {
            NMDeviceType deviceType = getDeviceType(device);
            String ipInterface = getDeviceIpInterface(device);

            if (!SUPPORTED_DEVICES.contains(deviceType)) {
                logger.warn("Device \"{}\" of type \"{}\" currently not supported", ipInterface, deviceType);
                continue;
            }

            if (!isDeviceManaged(device)) {
                setDeviceManaged(device, true);
            }

            if (!configuredInterfaces.contains(ipInterface)) {
                logger.warn("Device \"{}\" of type \"{}\" not configured. Disabling...", ipInterface, deviceType);
                disable(device);
            }
        }
    }

    private void disable(Device device) throws DBusException {
        NMDeviceState deviceState = getDeviceState(device);
        if (NMDeviceState.isConnected(deviceState)) {
            device.Disconnect();
        }

        Optional<Connection> connection = getAppliedConnection(device);
        if (connection.isPresent()) {
            connection.get().Delete();
        }
    }

    private List<Device> getAllDevices() throws DBusException {
        List<DBusPath> devicePaths = nm.GetAllDevices();

        List<Device> devices = new ArrayList<>();
        for (DBusPath path : devicePaths) {
            devices.add(dbusConnection.getRemoteObject(NM_BUS_NAME, path.getPath(), Device.class));
        }

        return devices;
    }

    private List<String> getAllDeviceInterfaceNames() throws DBusException {
        List<DBusPath> devicePaths = nm.GetAllDevices();

        List<String> devices = new ArrayList<>();
        for (DBusPath path : devicePaths) {
            Properties deviceProperties = dbusConnection.getRemoteObject(NM_BUS_NAME, path.getPath(), Properties.class);
            devices.add(deviceProperties.Get(NM_DEVICE_BUS_NAME, "Interface"));
        }

        return devices;
    }

    private NMDeviceState getDeviceState(Device device) throws DBusException {
        Properties deviceProperties = dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);

        return NMDeviceState.fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, "State"));
    }

    private void setDeviceManaged(Device device, Boolean manage) throws DBusException {
        Properties deviceProperties = dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);

        deviceProperties.Set(NM_DEVICE_BUS_NAME, "Managed", manage);
    }

    private Boolean isDeviceManaged(Device device) throws DBusException {
        Properties deviceProperties = dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);

        return deviceProperties.Get(NM_DEVICE_BUS_NAME, "Managed");
    }

    private NMDeviceType getDeviceType(Device device) throws DBusException {
        Properties deviceProperties = dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);

        return NMDeviceType.fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, "DeviceType"));
    }

    private String getDeviceIpInterface(Device device) throws DBusException {
        Properties deviceProperties = dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);

        return deviceProperties.Get(NM_DEVICE_BUS_NAME, "Interface");
    }

    private Device getDeviceByIpIface(String iface) throws DBusException {
        DBusPath ifaceDevicePath = nm.GetDeviceByIpIface(iface);
        return dbusConnection.getRemoteObject(NM_BUS_NAME, ifaceDevicePath.getPath(), Device.class);
    }

    private Optional<Connection> getAppliedConnection(Device dev) throws DBusException {
        try {
            Map<String, Map<String, Variant<?>>> connectionSettings = dev.GetAppliedConnection(new UInt32(0))
                    .getConnection();
            String uuid = String.valueOf(connectionSettings.get("connection").get("uuid")).replaceAll("\\[|\\]", "");

            Settings settings = this.dbusConnection.getRemoteObject(NM_BUS_NAME, NM_SETTINGS_BUS_PATH, Settings.class);

            DBusPath connectionPath = settings.GetConnectionByUuid(uuid);
            return Optional.of(dbusConnection.getRemoteObject(NM_BUS_NAME, connectionPath.getPath(), Connection.class));
        } catch (DBusExecutionException e) {
            logger.debug("Could not find applied connection for {}, caused by", dev.getObjectPath(), e);
            return Optional.empty();
        }
    }
}