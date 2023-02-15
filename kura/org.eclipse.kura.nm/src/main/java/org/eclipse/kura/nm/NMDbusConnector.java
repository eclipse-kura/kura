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
package org.eclipse.kura.nm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.nm.configuration.NMSettingsConverter;
import org.eclipse.kura.nm.status.NMStatusConverter;
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
import org.freedesktop.networkmanager.device.Generic;
import org.freedesktop.networkmanager.device.Wireless;
import org.freedesktop.networkmanager.settings.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMDbusConnector {

    private static final Logger logger = LoggerFactory.getLogger(NMDbusConnector.class);

    private static final String NM_BUS_NAME = "org.freedesktop.NetworkManager";
    private static final String NM_BUS_PATH = "/org/freedesktop/NetworkManager";
    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";
    private static final String NM_GENERIC_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device.Generic";
    private static final String NM_SETTINGS_BUS_PATH = "/org/freedesktop/NetworkManager/Settings";

    private static final String NM_PROPERTY_VERSION = "Version";

    private static final String NM_DEVICE_PROPERTY_INTERFACE = "Interface";
    private static final String NM_DEVICE_PROPERTY_MANAGED = "Managed";
    private static final String NM_DEVICE_PROPERTY_DEVICETYPE = "DeviceType";
    private static final String NM_DEVICE_PROPERTY_STATE = "State";
    private static final String NM_DEVICE_PROPERTY_IP4CONFIG = "Ip4Config";

    private static final String NM_DEVICE_GENERIC_PROPERTY_TYPEDESCRIPTION = "TypeDescription";

    private static final List<NMDeviceType> CONFIGURATION_SUPPORTED_DEVICE_TYPES = Arrays
            .asList(NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceType.NM_DEVICE_TYPE_WIFI);
    private static final List<KuraIpStatus> CONFIGURATION_SUPPORTED_STATUSES = Arrays.asList(KuraIpStatus.DISABLED,
            KuraIpStatus.ENABLEDLAN, KuraIpStatus.ENABLEDWAN, KuraIpStatus.UNMANAGED);

    private static final List<NMDeviceType> STATUS_SUPPORTED_DEVICE_TYPES = Arrays.asList(
            NMDeviceType.NM_DEVICE_TYPE_MODEM, NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceType.NM_DEVICE_TYPE_WIFI,
            NMDeviceType.NM_DEVICE_TYPE_PPP, NMDeviceType.NM_DEVICE_TYPE_GENERIC);

    private static NMDbusConnector instance;
    private final DBusConnection dbusConnection;
    private final NetworkManager nm;

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
        this.dbusConnection.disconnect();
    }

    public void checkPermissions() {
        Map<String, String> getPermissions = this.nm.GetPermissions();
        for (Entry<String, String> entry : getPermissions.entrySet()) {
            logger.info("Permission for {}: {}", entry.getKey(), entry.getValue());
        }
    }

    public void checkVersion() throws DBusException {
        Properties nmProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, NM_BUS_PATH, Properties.class);

        String nmVersion = nmProperties.Get(NM_BUS_NAME, NM_PROPERTY_VERSION);

        logger.info("NM Version: {}", nmVersion);
    }

    public synchronized List<String> getInterfaces() throws DBusException {
        List<Device> availableDevices = getAllDevices();

        List<String> supportedDeviceNames = new ArrayList<>();
        for (Device device : availableDevices) {
            NMDeviceType deviceType = getDeviceType(device);
            if (STATUS_SUPPORTED_DEVICE_TYPES.contains(deviceType)) {
                Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                        Properties.class);
                supportedDeviceNames.add(deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_INTERFACE));
            }

        }

        return supportedDeviceNames;
    }

    public synchronized NetInterface<NetInterfaceAddress> getInterfaceStatus(String interfaceName)
            throws DBusException {
        Device device = getDeviceByIpIface(interfaceName);
        NMDeviceType deviceType = getDeviceType(device);
        Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);

        DBusPath ip4configPath = deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_IP4CONFIG);
        Optional<Properties> ip4configProperties = Optional.empty();

        if (!ip4configPath.getPath().equals("/")) {
            ip4configProperties = Optional
                    .of(this.dbusConnection.getRemoteObject(NM_BUS_NAME, ip4configPath.getPath(), Properties.class));
        }

        if (!STATUS_SUPPORTED_DEVICE_TYPES.contains(deviceType)) {
            logger.warn("Device \"{}\" of type \"{}\" currently not supported", interfaceName, deviceType);
            return null;
        }

        if (deviceType == NMDeviceType.NM_DEVICE_TYPE_ETHERNET) {
            return NMStatusConverter.buildEthernetStatus(interfaceName, deviceProperties, ip4configProperties);
        } else if (deviceType == NMDeviceType.NM_DEVICE_TYPE_GENERIC) {
            Generic genericDevice = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                    Generic.class);
            Properties genericDeviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME,
                    genericDevice.getObjectPath(), Properties.class);
            String genericDeviceType = genericDeviceProperties.Get(NM_GENERIC_DEVICE_BUS_NAME,
                    NM_DEVICE_GENERIC_PROPERTY_TYPEDESCRIPTION);
            if (genericDeviceType.equals("loopback")) {
                return NMStatusConverter.buildLoopbackStatus(interfaceName, deviceProperties, ip4configProperties);
            }
        } else if (deviceType == NMDeviceType.NM_DEVICE_TYPE_WIFI) {
            Wireless wirelessDevice = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                    Wireless.class);
            Properties wirelessDeviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME,
                    wirelessDevice.getObjectPath(), Properties.class);

            List<Properties> accessPoints = getAllAccessPoints(wirelessDevice);

            return NMStatusConverter.buildWirelessStatus(interfaceName, deviceProperties, ip4configProperties,
                    wirelessDeviceProperties, accessPoints);
        }

        return null;
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

            manageConfiguredInterface(iface, properties);
        }
    }

    private synchronized void manageConfiguredInterface(String iface, NetworkProperties properties)
            throws DBusException {
        Device device = getDeviceByIpIface(iface);
        NMDeviceType deviceType = getDeviceType(device);

        KuraIpStatus ip4Status = KuraIpStatus
                .fromString(properties.get(String.class, "net.interface.%s.config.ip4.status", iface));
        // Temporary solution while we wait to add complete IPv6 support
        KuraIpStatus ip6Status = ip4Status == KuraIpStatus.UNMANAGED ? KuraIpStatus.UNMANAGED : KuraIpStatus.DISABLED;
        KuraInterfaceStatus interfaceStatus = KuraInterfaceStatus.fromKuraIpStatus(ip4Status, ip6Status);

        if (!CONFIGURATION_SUPPORTED_DEVICE_TYPES.contains(deviceType)
                || !CONFIGURATION_SUPPORTED_STATUSES.contains(ip4Status)
                || !CONFIGURATION_SUPPORTED_STATUSES.contains(ip6Status)) {
            logger.warn("Device \"{}\" of type \"{}\" with status \"{}\"/\"{}\" currently not supported", iface,
                    deviceType, ip4Status, ip6Status);
            return;
        }

        logger.info("Settings iface \"{}\":{}", iface, deviceType);

        if (interfaceStatus == KuraInterfaceStatus.DISABLED) {
            disable(device);
        } else if (interfaceStatus == KuraInterfaceStatus.UNMANAGED) {
            setDeviceManaged(device, false);
        } else { // NMDeviceEnable.ENABLED
            if (Boolean.FALSE.equals(isDeviceManaged(device))) {
                setDeviceManaged(device, true);
            }

            Optional<Connection> connection = getAppliedConnection(device);
            Map<String, Map<String, Variant<?>>> newConnectionSettings = NMSettingsConverter.buildSettings(properties,
                    connection, iface, deviceType);

            logger.info("New settings: {}", newConnectionSettings);

            if (connection.isPresent()) {
                logger.info("Current settings: {}", connection.get().GetSettings());

                connection.get().Update(newConnectionSettings);
                this.nm.ActivateConnection(new DBusPath(connection.get().getObjectPath()),
                        new DBusPath(device.getObjectPath()), new DBusPath("/"));
            } else {
                this.nm.AddAndActivateConnection(newConnectionSettings, new DBusPath(device.getObjectPath()),
                        new DBusPath("/"));
            }
        }
    }

    private synchronized void manageNonConfiguredInterfaces(List<String> configuredInterfaces,
            List<Device> availableInterfaces) throws DBusException {
        for (Device device : availableInterfaces) {
            NMDeviceType deviceType = getDeviceType(device);
            String ipInterface = getDeviceIpInterface(device);

            if (!CONFIGURATION_SUPPORTED_DEVICE_TYPES.contains(deviceType)) {
                logger.warn("Device \"{}\" of type \"{}\" currently not supported", ipInterface, deviceType);
                continue;
            }

            if (Boolean.FALSE.equals(isDeviceManaged(device))) {
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
        if (Boolean.TRUE.equals(NMDeviceState.isConnected(deviceState))) {
            device.Disconnect();
        }

        Optional<Connection> connection = getAppliedConnection(device);
        if (connection.isPresent()) {
            connection.get().Delete();
        }
    }

    private List<Device> getAllDevices() throws DBusException {
        List<DBusPath> devicePaths = this.nm.GetAllDevices();

        List<Device> devices = new ArrayList<>();
        for (DBusPath path : devicePaths) {
            devices.add(this.dbusConnection.getRemoteObject(NM_BUS_NAME, path.getPath(), Device.class));
        }

        return devices;
    }

    private List<String> getAllDeviceInterfaceNames() throws DBusException {
        List<DBusPath> devicePaths = this.nm.GetAllDevices();

        List<String> devices = new ArrayList<>();
        for (DBusPath path : devicePaths) {
            Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, path.getPath(),
                    Properties.class);
            devices.add(deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_INTERFACE));
        }

        return devices;
    }

    private List<Properties> getAllAccessPoints(Wireless wirelessDevice) throws DBusException {
        List<DBusPath> accessPointPaths = wirelessDevice.GetAllAccessPoints();

        List<Properties> accessPointProperties = new ArrayList<>();

        for (DBusPath path : accessPointPaths) {
            Properties apProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, path.getPath(),
                    Properties.class);
            accessPointProperties.add(apProperties);

        }

        return accessPointProperties;
    }

    private NMDeviceState getDeviceState(Device device) throws DBusException {
        Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);

        return NMDeviceState.fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_STATE));
    }

    private void setDeviceManaged(Device device, Boolean manage) throws DBusException {
        Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);

        deviceProperties.Set(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_MANAGED, manage);
    }

    private Boolean isDeviceManaged(Device device) throws DBusException {
        Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);

        return deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_MANAGED);
    }

    private NMDeviceType getDeviceType(Device device) throws DBusException {
        Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);

        return NMDeviceType.fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_DEVICETYPE));
    }

    private String getDeviceIpInterface(Device device) throws DBusException {
        Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);

        return deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_INTERFACE);
    }

    private Device getDeviceByIpIface(String iface) throws DBusException {
        DBusPath ifaceDevicePath = this.nm.GetDeviceByIpIface(iface);
        return this.dbusConnection.getRemoteObject(NM_BUS_NAME, ifaceDevicePath.getPath(), Device.class);
    }

    private Optional<Connection> getAppliedConnection(Device dev) throws DBusException {
        try {
            Map<String, Map<String, Variant<?>>> connectionSettings = dev.GetAppliedConnection(new UInt32(0))
                    .getConnection();
            String uuid = String.valueOf(connectionSettings.get("connection").get("uuid").getValue());

            Settings settings = this.dbusConnection.getRemoteObject(NM_BUS_NAME, NM_SETTINGS_BUS_PATH, Settings.class);

            DBusPath connectionPath = settings.GetConnectionByUuid(uuid);
            return Optional
                    .of(this.dbusConnection.getRemoteObject(NM_BUS_NAME, connectionPath.getPath(), Connection.class));
        } catch (DBusExecutionException e) {
            logger.debug("Could not find applied connection for {}, caused by", dev.getObjectPath(), e);
            return Optional.empty();
        }
    }
}