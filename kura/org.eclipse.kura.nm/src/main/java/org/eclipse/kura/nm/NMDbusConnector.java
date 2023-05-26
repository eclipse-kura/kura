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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.util.IwCapabilityTool;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.wifi.WifiChannel;
import org.eclipse.kura.nm.configuration.NMSettingsConverter;
import org.eclipse.kura.nm.status.AccessPointsProperties;
import org.eclipse.kura.nm.status.DevicePropertiesWrapper;
import org.eclipse.kura.nm.status.NMStatusConverter;
import org.eclipse.kura.nm.status.SimProperties;
import org.eclipse.kura.nm.status.SupportedChannelsProperties;
import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.modemmanager1.modem.Location;
import org.freedesktop.networkmanager.Device;
import org.freedesktop.networkmanager.Settings;
import org.freedesktop.networkmanager.device.Generic;
import org.freedesktop.networkmanager.device.Wired;
import org.freedesktop.networkmanager.device.Wireless;
import org.freedesktop.networkmanager.settings.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMDbusConnector {

    private static final Logger logger = LoggerFactory.getLogger(NMDbusConnector.class);

    private static final String NM_BUS_NAME = "org.freedesktop.NetworkManager";
    private static final String NM_BUS_PATH = "/org/freedesktop/NetworkManager";
    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";
    private static final String NM_DEVICE_WIRELESS_BUS_NAME = "org.freedesktop.NetworkManager.Device.Wireless";
    private static final String NM_GENERIC_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device.Generic";
    private static final String NM_SETTINGS_BUS_PATH = "/org/freedesktop/NetworkManager/Settings";
    private static final String MM_BUS_NAME = "org.freedesktop.ModemManager1";
    private static final String MM_BUS_PATH = "/org/freedesktop/ModemManager1";
    private static final String MM_MODEM_NAME = "org.freedesktop.ModemManager1.Modem";
    private static final String MM_SIM_NAME = "org.freedesktop.ModemManager1.Sim";
    private static final String MM_LOCATION_BUS_NAME = "org.freedesktop.ModemManager1.Modem.Location";

    private static final String NM_PROPERTY_VERSION = "Version";

    private static final String NM_DEVICE_PROPERTY_INTERFACE = "Interface";
    private static final String NM_DEVICE_PROPERTY_MANAGED = "Managed";
    private static final String NM_DEVICE_PROPERTY_DEVICETYPE = "DeviceType";
    private static final String NM_DEVICE_PROPERTY_STATE = "State";
    private static final String NM_DEVICE_PROPERTY_IP4CONFIG = "Ip4Config";
    private static final String NM_SETTING_CONNECTION_KEY = "connection";
    private static final String NM_DEVICE_GENERIC_PROPERTY_TYPEDESCRIPTION = "TypeDescription";

    private static final String MM_MODEM_PROPERTY_STATE = "State";

    private static final List<NMDeviceType> CONFIGURATION_SUPPORTED_DEVICE_TYPES = Arrays.asList(
            NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceType.NM_DEVICE_TYPE_MODEM);
    private static final List<KuraIpStatus> CONFIGURATION_SUPPORTED_STATUSES = Arrays.asList(KuraIpStatus.DISABLED,
            KuraIpStatus.ENABLEDLAN, KuraIpStatus.ENABLEDWAN, KuraIpStatus.UNMANAGED);

    private static final List<NMDeviceType> STATUS_SUPPORTED_DEVICE_TYPES = Arrays.asList(
            NMDeviceType.NM_DEVICE_TYPE_MODEM, NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceType.NM_DEVICE_TYPE_WIFI,
            NMDeviceType.NM_DEVICE_TYPE_LOOPBACK);

    private static NMDbusConnector instance;
    private final DBusConnection dbusConnection;
    private final NetworkManager nm;

    private Map<String, Object> cachedConfiguration = null;

    private NMConfigurationEnforcementHandler configurationEnforcementHandler = null;
    private NMDeviceAddedHandler deviceAddedHandler = null;

    private boolean configurationEnforcementHandlerIsArmed = false;

    private final Map<String, NMModemResetHandler> modemHandlers = new HashMap<>();

    private NMDbusConnector(DBusConnection dbusConnection) throws DBusException {
        this.dbusConnection = Objects.requireNonNull(dbusConnection);
        this.nm = this.dbusConnection.getRemoteObject(NM_BUS_NAME, NM_BUS_PATH, NetworkManager.class);
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

    public boolean configurationEnforcementIsActive() {
        return Objects.nonNull(this.configurationEnforcementHandler) && Objects.nonNull(this.deviceAddedHandler)
                && this.configurationEnforcementHandlerIsArmed;
    }

    public void checkPermissions() {
        Map<String, String> getPermissions = this.nm.GetPermissions();
        if (logger.isDebugEnabled()) {
            for (Entry<String, String> entry : getPermissions.entrySet()) {
                logger.debug("Permission for {}: {}", entry.getKey(), entry.getValue());
            }
        }
    }

    public void checkVersion() throws DBusException {
        Properties nmProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, NM_BUS_PATH, Properties.class);

        String nmVersion = nmProperties.Get(NM_BUS_NAME, NM_PROPERTY_VERSION);

        logger.debug("NM Version: {}", nmVersion);
    }

    public synchronized List<String> getDeviceIds() throws DBusException {
        List<Device> availableDevices = getAllDevices();

        List<String> supportedDeviceNames = new ArrayList<>();
        for (Device device : availableDevices) {
            NMDeviceType deviceType = getDeviceType(device.getObjectPath());
            if (STATUS_SUPPORTED_DEVICE_TYPES.contains(deviceType)) {
                supportedDeviceNames.add(getDeviceIdByDBusPath(device.getObjectPath()));
            }

        }

        return supportedDeviceNames;
    }

    public synchronized String getInterfaceName(String interfaceId) throws DBusException {
        Optional<Device> device = getDeviceByInterfaceId(interfaceId);
        if (device.isPresent()) {
            NMDeviceType deviceType = getDeviceType(device.get().getObjectPath());
            if (!NMDeviceType.NM_DEVICE_TYPE_MODEM.equals(deviceType)) {
                return interfaceId;
            } else {
                Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME,
                        device.get().getObjectPath(), Properties.class);
                try {
                    String ipInterface = deviceProperties.Get(NM_DEVICE_BUS_NAME, "IpInterface");
                    if (Objects.nonNull(ipInterface) && !ipInterface.isEmpty()) {
                        return ipInterface;
                    } else {
                        return "";
                    }
                } catch (DBusExecutionException e) {
                    logger.debug("Cannot retrieve IpInterface for {} interface Id", interfaceId, e);
                    return "";
                }
            }
        }
        return "";
    }

    public synchronized NetworkInterfaceStatus getInterfaceStatus(String interfaceId,
            CommandExecutorService commandExecutorService) throws DBusException, KuraException {
        NetworkInterfaceStatus networkInterfaceStatus = null;
        Optional<Device> device = getDeviceByInterfaceId(interfaceId);
        if (device.isPresent()) {
            NMDeviceType deviceType = getDeviceType(device.get().getObjectPath());
            Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.get().getObjectPath(),
                    Properties.class);

            DBusPath ip4configPath = deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_IP4CONFIG);
            Optional<Properties> ip4configProperties = Optional.empty();

            if (!ip4configPath.getPath().equals("/")) {
                ip4configProperties = Optional.of(
                        this.dbusConnection.getRemoteObject(NM_BUS_NAME, ip4configPath.getPath(), Properties.class));
            }

            if (!STATUS_SUPPORTED_DEVICE_TYPES.contains(deviceType)) {
                logger.warn("Device \"{}\" of type \"{}\" currently not supported", interfaceId, deviceType);
                return null;
            }

            switch (deviceType) {
                case NM_DEVICE_TYPE_ETHERNET:
                    Wired wiredDevice = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.get().getObjectPath(),
                            Wired.class);
                    Properties wiredDeviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME,
                            wiredDevice.getObjectPath(), Properties.class);

                    DevicePropertiesWrapper ethernetPropertiesWrapper = new DevicePropertiesWrapper(deviceProperties,
                            Optional.of(wiredDeviceProperties), NMDeviceType.NM_DEVICE_TYPE_ETHERNET);

                    networkInterfaceStatus = NMStatusConverter.buildEthernetStatus(interfaceId,
                            ethernetPropertiesWrapper,
                            ip4configProperties);
                    break;
                case NM_DEVICE_TYPE_LOOPBACK:
                    DevicePropertiesWrapper loopbackPropertiesWrapper = new DevicePropertiesWrapper(deviceProperties,
                            Optional.empty(), NMDeviceType.NM_DEVICE_TYPE_LOOPBACK);

                    networkInterfaceStatus = NMStatusConverter.buildLoopbackStatus(interfaceId,
                            loopbackPropertiesWrapper,
                            ip4configProperties);
                    break;
                case NM_DEVICE_TYPE_WIFI:
                    networkInterfaceStatus = createWirelessStatus(interfaceId, commandExecutorService, device.get(),
                            deviceProperties, ip4configProperties);
                    break;
                case NM_DEVICE_TYPE_MODEM:
                    networkInterfaceStatus = createModemStatus(interfaceId, device.get(), deviceProperties,
                            ip4configProperties);
                    break;
                default:
                    break;
            }
        }
        return networkInterfaceStatus;
    }

    private NetworkInterfaceStatus createModemStatus(String interfaceId, Device device, Properties deviceProperties,
            Optional<Properties> ip4configProperties) throws DBusException {
        NetworkInterfaceStatus networkInterfaceStatus;
        Optional<String> modemPath = getModemPathFromMM(device.getObjectPath());
        Optional<Properties> modemDeviceProperties = Optional.empty();
        List<SimProperties> simProperties = Collections.emptyList();
        List<Properties> bearerProperties = Collections.emptyList();
        if (modemPath.isPresent()) {
            modemDeviceProperties = getModemProperties(modemPath.get());
            if (modemDeviceProperties.isPresent()) {
                simProperties = getModemSimProperties(modemDeviceProperties.get());
                bearerProperties = getModemBearersProperties(modemPath.get(), modemDeviceProperties.get());
            }
        }
        DevicePropertiesWrapper modemPropertiesWrapper = new DevicePropertiesWrapper(deviceProperties,
                modemDeviceProperties, NMDeviceType.NM_DEVICE_TYPE_MODEM);
        networkInterfaceStatus = NMStatusConverter.buildModemStatus(interfaceId, modemPropertiesWrapper,
                ip4configProperties, simProperties, bearerProperties);
        return networkInterfaceStatus;
    }

    private NetworkInterfaceStatus createWirelessStatus(String interfaceId,
            CommandExecutorService commandExecutorService, Device device, Properties deviceProperties,
            Optional<Properties> ip4configProperties) throws DBusException, KuraException {
        NetworkInterfaceStatus networkInterfaceStatus = null;
        Wireless wirelessDevice = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Wireless.class);
        Properties wirelessDeviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME,
                wirelessDevice.getObjectPath(), Properties.class);

        List<Properties> accessPoints = getAllAccessPoints(wirelessDevice);

        DBusPath activeAccessPointPath = wirelessDeviceProperties.Get(NM_DEVICE_WIRELESS_BUS_NAME, "ActiveAccessPoint");
        Optional<Properties> activeAccessPoint = Optional.empty();
        String countryCode = IwCapabilityTool.getWifiCountryCode(commandExecutorService);
        List<WifiChannel> supportedChannels = IwCapabilityTool.probeChannels(interfaceId, commandExecutorService);

        if (!activeAccessPointPath.getPath().equals("/")) {
            activeAccessPoint = Optional.of(this.dbusConnection.getRemoteObject(NM_BUS_NAME,
                    activeAccessPointPath.getPath(), Properties.class));
        }
        DevicePropertiesWrapper wirelessPropertiesWrapper = new DevicePropertiesWrapper(deviceProperties,
                Optional.of(wirelessDeviceProperties), NMDeviceType.NM_DEVICE_TYPE_WIFI);

        networkInterfaceStatus = NMStatusConverter.buildWirelessStatus(interfaceId, wirelessPropertiesWrapper,
                ip4configProperties, new AccessPointsProperties(activeAccessPoint, accessPoints),
                new SupportedChannelsProperties(countryCode, supportedChannels));
        return networkInterfaceStatus;
    }

    public synchronized void apply(Map<String, Object> networkConfiguration) throws DBusException {
        try {
            configurationEnforcementDisable();
            modemResetHandlersDisable();
            doApply(networkConfiguration);
            this.cachedConfiguration = networkConfiguration;
        } finally {
            configurationEnforcementEnable();
        }
    }

    public synchronized void apply() throws DBusException {
        if (Objects.isNull(this.cachedConfiguration)) {
            logger.warn("No cached network configuration found.");
            return;
        }
        try {
            configurationEnforcementDisable();
            modemResetHandlersDisable();
            doApply(this.cachedConfiguration);
        } finally {
            configurationEnforcementEnable();
        }
    }

    public synchronized void apply(String deviceId) throws DBusException {
        if (Objects.isNull(deviceId) || deviceId.isEmpty()) {
            throw new IllegalArgumentException("DeviceId cannot be null or empty.");
        }
        if (Objects.isNull(this.cachedConfiguration)) {
            logger.warn("No cached network configuration found.");
            return;
        }
        try {
            configurationEnforcementDisable();
            modemResetHandlersDisable(deviceId);
            doApply(deviceId, this.cachedConfiguration);
        } finally {
            configurationEnforcementEnable();
        }
    }

    private synchronized void doApply(Map<String, Object> networkConfiguration) throws DBusException {
        logger.info("Applying configuration using NetworkManager Dbus connector");
        List<Device> availableDevices = getAllDevices();
        availableDevices.forEach(device -> {
            try {
                String deviceId = getDeviceIdByDBusPath(device.getObjectPath());
                doApply(deviceId, networkConfiguration);
            } catch (DBusException | DBusExecutionException | IllegalArgumentException | NoSuchElementException e) {
                logger.error("Unable to apply configuration to the device path {}", device.getObjectPath(), e);
            }
        });
    }

    private synchronized void doApply(String deviceIdToBeConfigured, Map<String, Object> networkConfiguration)
            throws DBusException {
        NetworkProperties properties = new NetworkProperties(networkConfiguration);
        List<String> configuredInterfaceIds = properties.getStringList("net.interfaces");

        Optional<Device> device = getDeviceByInterfaceId(deviceIdToBeConfigured);
        if (device.isPresent()) {
            if (configuredInterfaceIds.contains(deviceIdToBeConfigured)) {
                manageConfiguredInterface(device.get(), deviceIdToBeConfigured, properties);
            } else {
                manageNonConfiguredInterface(device.get(), deviceIdToBeConfigured);
            }
        }
    }

    private synchronized void manageConfiguredInterface(Device device, String deviceId, NetworkProperties properties)
            throws DBusException {
        NMDeviceType deviceType = getDeviceType(device.getObjectPath());

        KuraIpStatus ip4Status = KuraIpStatus
                .fromString(properties.get(String.class, "net.interface.%s.config.ip4.status", deviceId));
        // Temporary solution while we wait to add complete IPv6 support
        KuraIpStatus ip6Status = ip4Status == KuraIpStatus.UNMANAGED ? KuraIpStatus.UNMANAGED : KuraIpStatus.DISABLED;
        KuraInterfaceStatus interfaceStatus = KuraInterfaceStatus.fromKuraIpStatus(ip4Status, ip6Status);

        if (!CONFIGURATION_SUPPORTED_DEVICE_TYPES.contains(deviceType)
                || !CONFIGURATION_SUPPORTED_STATUSES.contains(ip4Status)
                || !CONFIGURATION_SUPPORTED_STATUSES.contains(ip6Status)) {
            logger.warn("Device \"{}\" of type \"{}\" with status \"{}\"/\"{}\" currently not supported", deviceId,
                    deviceType, ip4Status, ip6Status);
            return;
        }

        logger.info("Settings iface \"{}\":{}", deviceId, deviceType);

        if (interfaceStatus == KuraInterfaceStatus.DISABLED) {
            disable(device);
        } else if (interfaceStatus == KuraInterfaceStatus.UNMANAGED) {
            logger.info("Iface \"{}\" set as UNMANAGED in Kura. Skipping configuration.", deviceId);
        } else { // NMDeviceEnable.ENABLED
            enableInterface(deviceId, properties, device, deviceType);
        }

        // Manage GPS independently of device ip status
        if (deviceType == NMDeviceType.NM_DEVICE_TYPE_MODEM) {
            Optional<Boolean> enableGPS = properties.getOpt(Boolean.class, "net.interface.%s.config.gpsEnabled",
                    deviceId);
            handleModemManagerGPSSetup(device, enableGPS);
        }

    }

    private void enableInterface(String deviceId, NetworkProperties properties, Device device, NMDeviceType deviceType)
            throws DBusException {
        if (Boolean.FALSE.equals(isDeviceManaged(device))) {
            setDeviceManaged(device, true);
        }
        String interfaceName = getDeviceInterface(device);

        Optional<Connection> connection = getAssociatedConnection(device);
        Map<String, Map<String, Variant<?>>> newConnectionSettings = NMSettingsConverter.buildSettings(properties,
                connection, deviceId, interfaceName, deviceType);

        DeviceStateLock dsLock = new DeviceStateLock(this.dbusConnection, device.getObjectPath(),
                NMDeviceState.NM_DEVICE_STATE_CONFIG);

        if (connection.isPresent()) {
            connection.get().Update(newConnectionSettings);
        } else {
            Settings settings = this.dbusConnection.getRemoteObject(NM_BUS_NAME, NM_SETTINGS_BUS_PATH, Settings.class);
            DBusPath createdConnectionPath = settings.AddConnection(newConnectionSettings);
            Connection createdConnection = this.dbusConnection.getRemoteObject(NM_BUS_NAME,
                    createdConnectionPath.getPath(), Connection.class);
            connection = Optional.of(createdConnection);
        }

        try {
            this.nm.ActivateConnection(new DBusPath(connection.get().getObjectPath()),
                    new DBusPath(device.getObjectPath()), new DBusPath("/"));
            dsLock.waitForSignal();
        } catch (DBusExecutionException e) {
            logger.warn("Couldn't complete activation of {} interface, caused by:", deviceId, e);
        }

        // Housekeeping
        List<Connection> availableConnections = getAvaliableConnections(device);
        for (Connection availableConnection : availableConnections) {
            if (!connection.get().getObjectPath().equals(availableConnection.getObjectPath())) {
                availableConnection.Delete();
            }
        }

        if (deviceType == NMDeviceType.NM_DEVICE_TYPE_MODEM) {
            int delayMinutes = properties.get(Integer.class, "net.interface.%s.config.resetTimeout", deviceId);

            if (delayMinutes != 0) {
                modemResetHandlerEnable(deviceId, delayMinutes, device);
            }
        }

    }

    private void manageNonConfiguredInterface(Device device, String deviceId) throws DBusException {
        NMDeviceType deviceType = getDeviceType(device.getObjectPath());

        if (!CONFIGURATION_SUPPORTED_DEVICE_TYPES.contains(deviceType)) {
            logger.warn("Device \"{}\" of type \"{}\" currently not supported", deviceId, deviceType);
            return;
        }

        if (Boolean.FALSE.equals(isDeviceManaged(device))) {
            setDeviceManaged(device, true);
        }

        logger.warn("Device \"{}\" of type \"{}\" not configured. Disabling...", deviceId, deviceType);

        disable(device);

        if (deviceType == NMDeviceType.NM_DEVICE_TYPE_MODEM) {
            handleModemManagerGPSSetup(device, Optional.of(false));
        }
    }

    private void handleModemManagerGPSSetup(Device device, Optional<Boolean> enableGPS) throws DBusException {
        Optional<String> modemDevicePath = getModemPathFromMM(device.getObjectPath());

        if (!modemDevicePath.isPresent()) {
            logger.warn("Cannot retrieve MM.Modem from NM.Modem at path: {}. Skipping GPS configuration.",
                    device.getObjectPath());
            return;
        }

        enableModem(modemDevicePath.get());

        boolean isGPSSourceEnabled = enableGPS.isPresent() && enableGPS.get();

        Location modemLocation = this.dbusConnection.getRemoteObject(MM_BUS_NAME, modemDevicePath.get(),
                Location.class);
        Properties modemLocationProperties = this.dbusConnection.getRemoteObject(MM_BUS_NAME,
                modemLocation.getObjectPath(), Properties.class);

        Set<MMModemLocationSource> availableLocationSources = EnumSet
                .of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE);
        Set<MMModemLocationSource> currentLocationSources = EnumSet
                .of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE);
        Set<MMModemLocationSource> desiredLocationSources = EnumSet
                .of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE);

        try {
            availableLocationSources = MMModemLocationSource.toMMModemLocationSourceFromBitMask(
                    modemLocationProperties.Get(MM_LOCATION_BUS_NAME, "Capabilities"));
            currentLocationSources = MMModemLocationSource
                    .toMMModemLocationSourceFromBitMask(modemLocationProperties.Get(MM_LOCATION_BUS_NAME, "Enabled"));
        } catch (DBusExecutionException e) {
            logger.warn("Cannot retrive Modem.Location capabilities for {}. Caused by: ",
                    modemLocationProperties.getObjectPath(), e);
            return;
        }

        if (isGPSSourceEnabled) {
            if (!availableLocationSources.contains(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED)) {
                logger.warn("Cannot setup Modem.Location, MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED not supported for {}",
                        modemLocationProperties.getObjectPath());
                return;
            }
            desiredLocationSources = EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED);
        }

        logger.debug("Modem location setup {} for modem {}", currentLocationSources, modemDevicePath.get());

        if (!currentLocationSources.equals(desiredLocationSources)) {
            modemLocation.Setup(MMModemLocationSource.toBitMaskFromMMModemLocationSource(desiredLocationSources),
                    false);
        }
    }

    private void enableModem(String modemDevicePath) throws DBusException {
        Modem modem = this.dbusConnection.getRemoteObject(MM_BUS_NAME, modemDevicePath, Modem.class);
        Properties modemProperties = this.dbusConnection.getRemoteObject(MM_BUS_NAME, modemDevicePath,
                Properties.class);

        MMModemState currentModemState = MMModemState
                .toMMModemState(modemProperties.Get(MM_MODEM_NAME, MM_MODEM_PROPERTY_STATE));

        if (currentModemState.getValue() < MMModemState.MM_MODEM_STATE_ENABLED.getValue()) {
            logger.info("Modem {} not enabled. Enabling modem...", modemDevicePath);
            modem.Enable(true);
        }
    }

    protected String getDeviceIdByDBusPath(String dbusPath) throws DBusException {
        NMDeviceType deviceType = getDeviceType(dbusPath);
        if (deviceType.equals(NMDeviceType.NM_DEVICE_TYPE_MODEM)) {
            Optional<String> modemPath = getModemPathFromMM(dbusPath);
            if (!modemPath.isPresent()) {
                throw new IllegalStateException(String.format("Cannot retrieve modem path for: %s.", dbusPath));
            }
            Optional<Properties> modemDeviceProperties = getModemProperties(modemPath.get());
            if (!modemDeviceProperties.isPresent()) {
                throw new IllegalStateException(String.format("Cannot retrieve modem properties for: %s.", dbusPath));

            }
            return NMStatusConverter.getModemDeviceHwPath(modemDeviceProperties.get());
        } else {
            Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, dbusPath, Properties.class);
            return deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_INTERFACE);
        }
    }

    private void disable(Device device) throws DBusException {
        Optional<Connection> appliedConnection = getAppliedConnection(device);

        NMDeviceState deviceState = getDeviceState(device);
        if (Boolean.TRUE.equals(NMDeviceState.isConnected(deviceState))) {
            DeviceStateLock dsLock = new DeviceStateLock(this.dbusConnection, device.getObjectPath(),
                    NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
            device.Disconnect();
            dsLock.waitForSignal();
        }

        // Housekeeping
        if (appliedConnection.isPresent()) {
            appliedConnection.get().Delete();
        }

        List<Connection> availableConnections = getAvaliableConnections(device);
        for (Connection connection : availableConnections) {
            connection.Delete();
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

    private NMDeviceType getDeviceType(String deviceDbusPath) throws DBusException {
        Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, deviceDbusPath,
                Properties.class);

        NMDeviceType deviceType = NMDeviceType
                .fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_DEVICETYPE));

        // Workaround to identify Loopback interface for NM versions prior to 1.42
        if (deviceType == NMDeviceType.NM_DEVICE_TYPE_GENERIC) {
            Generic genericDevice = this.dbusConnection.getRemoteObject(NM_BUS_NAME, deviceDbusPath, Generic.class);
            Properties genericDeviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME,
                    genericDevice.getObjectPath(), Properties.class);
            String genericDeviceType = genericDeviceProperties.Get(NM_GENERIC_DEVICE_BUS_NAME,
                    NM_DEVICE_GENERIC_PROPERTY_TYPEDESCRIPTION);
            if (genericDeviceType.equals("loopback")) {
                return NMDeviceType.NM_DEVICE_TYPE_LOOPBACK;
            }
        }

        return deviceType;
    }

    private String getDeviceInterface(Device device) throws DBusException {
        Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);

        return deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_INTERFACE);
    }

    private Optional<Device> getDeviceByInterfaceId(String interfaceId) throws DBusException {
        for (Device d : getAllDevices()) {
            Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, d.getObjectPath(),
                    Properties.class);
            NMDeviceType deviceType = NMDeviceType
                    .fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_DEVICETYPE));
            if (deviceType.equals(NMDeviceType.NM_DEVICE_TYPE_MODEM)) {
                Optional<String> modemPath = getModemPathFromMM(d.getObjectPath());
                if (modemPath.isPresent()) {
                    Optional<Properties> modemDeviceProperties = getModemProperties(modemPath.get());
                    if (modemDeviceProperties.isPresent() && NMStatusConverter
                            .getModemDeviceHwPath(modemDeviceProperties.get()).equals(interfaceId)) {
                        return Optional.of(d);
                    }
                }
            } else {
                if (deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_INTERFACE).equals(interfaceId)) {
                    return Optional.of(d);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Connection> getAssociatedConnection(Device dev) throws DBusException {
        Optional<Connection> appliedConnection = getAppliedConnection(dev);
        if (appliedConnection.isPresent()) {
            return appliedConnection;
        } else {
            logger.debug("Active connection not found, looking for avaliable connections.");

            List<Connection> availableConnections = getAvaliableConnections(dev);

            if (!availableConnections.isEmpty()) {
                return Optional.of(availableConnections.get(0));

            } else {
                return Optional.empty();
            }
        }
    }

    private Optional<Connection> getAppliedConnection(Device dev) throws DBusException {
        try {
            Map<String, Map<String, Variant<?>>> connectionSettings = dev.GetAppliedConnection(new UInt32(0))
                    .getConnection();
            String uuid = String.valueOf(connectionSettings.get(NM_SETTING_CONNECTION_KEY).get("uuid").getValue());

            Settings settings = this.dbusConnection.getRemoteObject(NM_BUS_NAME, NM_SETTINGS_BUS_PATH, Settings.class);

            DBusPath connectionPath = settings.GetConnectionByUuid(uuid);
            return Optional
                    .of(this.dbusConnection.getRemoteObject(NM_BUS_NAME, connectionPath.getPath(), Connection.class));
        } catch (DBusExecutionException e) {
            logger.debug("Could not find applied connection for {}, caused by", dev.getObjectPath(), e);
        }

        return Optional.empty();
    }

    private List<Connection> getAvaliableConnections(Device dev) throws DBusException {
        List<Connection> connections = new ArrayList<>();

        try {
            Settings settings = this.dbusConnection.getRemoteObject(NM_BUS_NAME, NM_SETTINGS_BUS_PATH, Settings.class);

            List<DBusPath> connectionPath = settings.ListConnections();

            Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, dev.getObjectPath(),
                    Properties.class);
            String interfaceName = deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_INTERFACE);
            String expectedConnectionName = String.format("kura-%s-connection", interfaceName);

            for (DBusPath path : connectionPath) {

                Connection availableConnection = this.dbusConnection.getRemoteObject(NM_BUS_NAME, path.getPath(),
                        Connection.class);

                Map<String, Map<String, Variant<?>>> availableConnectionSettings = availableConnection.GetSettings();
                String availableConnectionId = (String) availableConnectionSettings.get(NM_SETTING_CONNECTION_KEY)
                        .get("id").getValue();

                if (availableConnectionId.equals(expectedConnectionName)) {
                    connections.add(availableConnection);
                }

            }

        } catch (DBusExecutionException e) {
            logger.debug("Could not find applied connection for {}, caused by", dev.getObjectPath(), e);
        }

        return connections;
    }

    private void configurationEnforcementEnable() throws DBusException {
        if (Objects.isNull(this.configurationEnforcementHandler) && Objects.isNull(this.deviceAddedHandler)) {
            this.configurationEnforcementHandler = new NMConfigurationEnforcementHandler(this);
            this.deviceAddedHandler = new NMDeviceAddedHandler(this);
        }

        this.dbusConnection.addSigHandler(Device.StateChanged.class, this.configurationEnforcementHandler);
        this.dbusConnection.addSigHandler(NetworkManager.DeviceAdded.class, this.deviceAddedHandler);
        this.configurationEnforcementHandlerIsArmed = true;
    }

    private void configurationEnforcementDisable() throws DBusException {
        if (Objects.nonNull(this.configurationEnforcementHandler) && Objects.nonNull(this.deviceAddedHandler)) {
            this.dbusConnection.removeSigHandler(Device.StateChanged.class, this.configurationEnforcementHandler);
            this.dbusConnection.removeSigHandler(NetworkManager.DeviceAdded.class, this.deviceAddedHandler);
        }
        this.configurationEnforcementHandlerIsArmed = false;
    }

    private void modemResetHandlerEnable(String deviceId, int delayMinutes, Device device) throws DBusException {
        Optional<String> mmDBusPath = getModemPathFromMM(device.getObjectPath());
        if (!mmDBusPath.isPresent()) {
            logger.warn("Cannot retrieve modem device for {}. Skipping modem reset monitor setup.", deviceId);
            return;
        }

        Modem mmModemDevice = this.dbusConnection.getRemoteObject(MM_BUS_NAME, mmDBusPath.get(), Modem.class);

        NMModemResetHandler resetHandler = new NMModemResetHandler(device.getObjectPath(), mmModemDevice,
                delayMinutes * 60L * 1000L);

        this.modemHandlers.put(deviceId, resetHandler);
        this.dbusConnection.addSigHandler(Device.StateChanged.class, resetHandler);
    }

    private void modemResetHandlersDisable() {
        for (String deviceId : this.modemHandlers.keySet()) {
            modemResetHandlersDisable(deviceId);
        }
        this.modemHandlers.clear();
    }

    private void modemResetHandlersDisable(String deviceId) {
        if (this.modemHandlers.containsKey(deviceId)) {
            NMModemResetHandler handler = this.modemHandlers.get(deviceId);
            handler.clearTimer();
            try {
                this.dbusConnection.removeSigHandler(Device.StateChanged.class, handler);
            } catch (DBusException e) {
                logger.warn("Couldn't remove signal handler for: {}. Caused by:", handler.getNMDevicePath(), e);
            }
        }
    }

    private String getDeviceIdFromNM(String deviceDbusPath) throws DBusException {
        Properties nmModemProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, deviceDbusPath,
                Properties.class);
        String deviceId = (String) nmModemProperties.Get(NM_DEVICE_BUS_NAME + ".Modem", "DeviceId");
        logger.debug("Found DeviceId {} for device {}", deviceId, deviceDbusPath);
        return deviceId;
    }

    private Map<DBusPath, Map<String, Map<String, Variant<?>>>> getManagedObjectsFromMM() throws DBusException {
        ObjectManager objectManager = this.dbusConnection.getRemoteObject(MM_BUS_NAME, MM_BUS_PATH,
                ObjectManager.class);
        Map<DBusPath, Map<String, Map<String, Variant<?>>>> managedObjects = objectManager.GetManagedObjects();
        logger.debug("Found Managed Objects {}", managedObjects.keySet());
        return managedObjects;
    }

    private Optional<String> getModemPathFromManagedObjects(
            Map<DBusPath, Map<String, Map<String, Variant<?>>>> managedObjects, String deviceId) {
        Optional<String> modemPath = Optional.empty();
        Optional<Entry<DBusPath, Map<String, Map<String, Variant<?>>>>> modemEntry = managedObjects.entrySet().stream()
                .filter(entry -> {
                    String modemDeviceId = (String) entry.getValue().get(MM_MODEM_NAME).get("DeviceIdentifier")
                            .getValue();
                    return modemDeviceId.equals(deviceId);
                }).findFirst();
        if (modemEntry.isPresent()) {
            modemPath = Optional.of(modemEntry.get().getKey().getPath());
        }
        return modemPath;
    }

    private Optional<String> getModemPathFromMM(String devicePath) throws DBusException {
        String deviceId = getDeviceIdFromNM(devicePath);
        Map<DBusPath, Map<String, Map<String, Variant<?>>>> managedObjects = getManagedObjectsFromMM();
        return getModemPathFromManagedObjects(managedObjects, deviceId);
    }

    private Optional<Properties> getModemProperties(String modemPath) throws DBusException {
        Optional<Properties> modemProperties = Optional.empty();
        Properties properties = this.dbusConnection.getRemoteObject(MM_BUS_NAME, modemPath, Properties.class);
        if (Objects.nonNull(properties)) {
            modemProperties = Optional.of(properties);
        }
        return modemProperties;
    }

    private List<SimProperties> getModemSimProperties(Properties modemProperties) throws DBusException {
        List<SimProperties> simProperties = new ArrayList<>();
        try {
            UInt32 primarySimSlot = modemProperties.Get(MM_MODEM_NAME, "PrimarySimSlot");

            if (primarySimSlot.intValue() == 0) {
                // Multiple SIM slots aren't supported
                DBusPath simPath = modemProperties.Get(MM_MODEM_NAME, "Sim");
                if (!simPath.getPath().equals("/")) {
                    Properties simProp = this.dbusConnection.getRemoteObject(MM_BUS_NAME, simPath.getPath(),
                            Properties.class);
                    simProperties.add(new SimProperties(simProp, true, true));
                }
            } else {
                List<DBusPath> simPaths = modemProperties.Get(MM_MODEM_NAME, "SimSlots");
                for (int index = 0; index < simPaths.size(); index++) {
                    String dbusPath = simPaths.get(index).getPath();

                    if (dbusPath.equals("/")) {
                        // SIM slot doesn't contain a SIM
                        continue;
                    }

                    Properties simProp = this.dbusConnection.getRemoteObject(MM_BUS_NAME, dbusPath, Properties.class);
                    boolean isActive = simProp.Get(MM_SIM_NAME, "Active");
                    boolean isPrimary = index == (primarySimSlot.intValue() - 1);

                    simProperties.add(new SimProperties(simProp, isActive, isPrimary));
                }
            }
        } catch (DBusExecutionException e) {
            // Fallback for ModemManager version prior to 1.16
            DBusPath simPath = modemProperties.Get(MM_MODEM_NAME, "Sim");
            if (!simPath.getPath().equals("/")) {
                Properties simProp = this.dbusConnection.getRemoteObject(MM_BUS_NAME, simPath.getPath(),
                        Properties.class);
                simProperties.add(new SimProperties(simProp, true, true));
            }

        }
        return simProperties;
    }

    private List<Properties> getModemBearersProperties(String modemPath, Properties modemProperties)
            throws DBusException {
        List<Properties> bearerProperties = new ArrayList<>();
        try {
            Modem modem = this.dbusConnection.getRemoteObject(MM_BUS_NAME, modemPath, Modem.class);
            if (Objects.nonNull(modem)) {
                List<DBusPath> bearerPaths = modem.ListBearers();
                bearerProperties = getBearersPropertiesFromPaths(bearerPaths);
            }
        } catch (DBusExecutionException e) {
            try {
                List<DBusPath> bearerPaths = modemProperties.Get(MM_BUS_NAME, "Bearers");
                bearerProperties = getBearersPropertiesFromPaths(bearerPaths);
            } catch (DBusExecutionException e1) {
                logger.warn("Cannot get bearers for modem {}", modemPath, e1);
            }
        }
        return bearerProperties;
    }

    private List<Properties> getBearersPropertiesFromPaths(List<DBusPath> bearerPaths) throws DBusException {
        List<Properties> bearerProperties = new ArrayList<>();
        for (DBusPath bearerPath : bearerPaths) {
            if (!bearerPath.getPath().equals("/")) {
                bearerProperties
                        .add(this.dbusConnection.getRemoteObject(MM_BUS_NAME, bearerPath.getPath(), Properties.class));
            }
        }
        return bearerProperties;
    }
}
