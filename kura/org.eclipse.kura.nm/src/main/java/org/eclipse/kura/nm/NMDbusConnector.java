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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.util.IwCapabilityTool;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.wifi.WifiChannel;
import org.eclipse.kura.nm.configuration.NMSettingsConverter;
import org.eclipse.kura.nm.enums.NMDeviceState;
import org.eclipse.kura.nm.enums.NMDeviceType;
import org.eclipse.kura.nm.signal.handlers.DeviceStateLock;
import org.eclipse.kura.nm.signal.handlers.NMConfigurationEnforcementHandler;
import org.eclipse.kura.nm.signal.handlers.NMDeviceAddedHandler;
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
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.networkmanager.Device;
import org.freedesktop.networkmanager.Settings;
import org.freedesktop.networkmanager.device.Wired;
import org.freedesktop.networkmanager.device.Wireless;
import org.freedesktop.networkmanager.settings.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMDbusConnector {

    private static final Logger logger = LoggerFactory.getLogger(NMDbusConnector.class);

    private static final String NM_BUS_NAME = "org.freedesktop.NetworkManager";
    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";
    private static final String NM_DEVICE_WIRELESS_BUS_NAME = "org.freedesktop.NetworkManager.Device.Wireless";
    private static final String NM_SETTINGS_BUS_PATH = "/org/freedesktop/NetworkManager/Settings";

    private static final String NM_DEVICE_PROPERTY_INTERFACE = "Interface";
    private static final String NM_DEVICE_PROPERTY_IP4CONFIG = "Ip4Config";
    private static final String NM_DEVICE_PROPERTY_IP6CONFIG = "Ip6Config";

    private static final List<NMDeviceType> CONFIGURATION_SUPPORTED_DEVICE_TYPES = Arrays.asList(
            NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceType.NM_DEVICE_TYPE_MODEM);
    private static final List<KuraIpStatus> CONFIGURATION_SUPPORTED_STATUSES = Arrays.asList(KuraIpStatus.DISABLED,
            KuraIpStatus.ENABLEDLAN, KuraIpStatus.ENABLEDWAN, KuraIpStatus.UNMANAGED);

    private static final List<NMDeviceType> STATUS_SUPPORTED_DEVICE_TYPES = Arrays.asList(
            NMDeviceType.NM_DEVICE_TYPE_MODEM, NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceType.NM_DEVICE_TYPE_WIFI,
            NMDeviceType.NM_DEVICE_TYPE_LOOPBACK);

    private static final long MAX_SCAN_TIME_SECONDS = 30L;

    private static NMDbusConnector instance;
    private final DBusConnection dbusConnection;
    private final NetworkManagerDbusWrapper networkManager;
    private final ModemManagerDbusWrapper modemManager;
    private final WpaSupplicantDbusWrapper wpaSupplicant;

    private Map<String, Object> cachedConfiguration = null;

    private NMConfigurationEnforcementHandler configurationEnforcementHandler = null;
    private NMDeviceAddedHandler deviceAddedHandler = null;

    private boolean configurationEnforcementHandlerIsArmed = false;

    private NMDbusConnector(DBusConnection dbusConnection) throws DBusException {
        this.dbusConnection = Objects.requireNonNull(dbusConnection);
        this.networkManager = new NetworkManagerDbusWrapper(this.dbusConnection);
        this.modemManager = new ModemManagerDbusWrapper(this.dbusConnection);
        this.wpaSupplicant = new WpaSupplicantDbusWrapper(this.dbusConnection);
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
        Map<String, String> getPermissions = this.networkManager.getPermissions();
        if (logger.isDebugEnabled()) {
            for (Entry<String, String> entry : getPermissions.entrySet()) {
                logger.debug("Permission for {}: {}", entry.getKey(), entry.getValue());
            }
        }
    }

    public void checkVersion() throws DBusException {
        String nmVersion = this.networkManager.getVersion();
        logger.debug("NM Version: {}", nmVersion);
    }

    public synchronized List<String> getInterfaceIds() throws DBusException {
        List<Device> availableDevices = this.networkManager.getAllDevices();

        List<String> supportedDeviceNames = new ArrayList<>();
        for (Device device : availableDevices) {
            NMDeviceType deviceType = this.networkManager.getDeviceType(device.getObjectPath());
            if (STATUS_SUPPORTED_DEVICE_TYPES.contains(deviceType)) {
                supportedDeviceNames.add(getInterfaceIdByDBusPath(device.getObjectPath()));
            }

        }

        return supportedDeviceNames;
    }

    public synchronized String getInterfaceName(String interfaceId) throws DBusException {
        Optional<Device> device = getNetworkManagerDeviceByInterfaceId(interfaceId);
        if (device.isPresent()) {
            NMDeviceType deviceType = this.networkManager.getDeviceType(device.get().getObjectPath());
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

    private Optional<Device> getNetworkManagerDeviceByInterfaceId(String interfaceId) throws DBusException {
        for (Device nmDevice : this.networkManager.getAllDevices()) {
            String deviceInterfaceId = getInterfaceIdByDBusPath(nmDevice.getObjectPath());
            if (deviceInterfaceId.equals(interfaceId)) {
                return Optional.of(nmDevice);
            }
        }
        return Optional.empty();
    }

    public String getInterfaceIdByDBusPath(String dbusPath) throws DBusException {
        NMDeviceType deviceType = this.networkManager.getDeviceType(dbusPath);
        if (deviceType.equals(NMDeviceType.NM_DEVICE_TYPE_MODEM)) {
            Optional<String> modemPath = this.networkManager.getModemManagerDbusPath(dbusPath);
            return this.modemManager.getHardwareSysfsPath(modemPath);
        } else {
            Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, dbusPath, Properties.class);
            return deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_INTERFACE);
        }
    }

    public synchronized NetworkInterfaceStatus getInterfaceStatus(String interfaceId, boolean recompute,
            CommandExecutorService commandExecutorService) throws DBusException, KuraException {
        NetworkInterfaceStatus networkInterfaceStatus = null;

        Optional<Device> device = getNetworkManagerDeviceByInterfaceId(interfaceId);
        if (device.isPresent()) {
            NMDeviceType deviceType = this.networkManager.getDeviceType(device.get().getObjectPath());
            Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.get().getObjectPath(),
                    Properties.class);

            DBusPath ip4configPath = deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_IP4CONFIG);
            Optional<Properties> ip4configProperties = Optional.empty();

            if (!ip4configPath.getPath().equals("/")) {
                ip4configProperties = Optional.of(
                        this.dbusConnection.getRemoteObject(NM_BUS_NAME, ip4configPath.getPath(), Properties.class));
            }

            DBusPath ip6configPath = deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_IP6CONFIG);
            Optional<Properties> ip6configProperties = Optional.empty();

            if (!ip6configPath.getPath().equals("/")) {
                ip6configProperties = Optional.of(
                        this.dbusConnection.getRemoteObject(NM_BUS_NAME, ip6configPath.getPath(), Properties.class));
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

                networkInterfaceStatus = NMStatusConverter.buildEthernetStatus(interfaceId, ethernetPropertiesWrapper,
                        ip4configProperties, ip6configProperties);
                break;
            case NM_DEVICE_TYPE_LOOPBACK:
                DevicePropertiesWrapper loopbackPropertiesWrapper = new DevicePropertiesWrapper(deviceProperties,
                        Optional.empty(), NMDeviceType.NM_DEVICE_TYPE_LOOPBACK);

                networkInterfaceStatus = NMStatusConverter.buildLoopbackStatus(interfaceId, loopbackPropertiesWrapper,
                        ip4configProperties, ip6configProperties);
                break;
            case NM_DEVICE_TYPE_WIFI:
                if (recompute) {
                    wpaSupplicant.syncScan(interfaceId, MAX_SCAN_TIME_SECONDS);
                }

                networkInterfaceStatus = createWirelessStatus(interfaceId, commandExecutorService, device.get(),
                        deviceProperties, ip4configProperties, ip6configProperties);
                break;
            case NM_DEVICE_TYPE_MODEM:
                networkInterfaceStatus = createModemStatus(interfaceId, device.get(), deviceProperties,
                        ip4configProperties, ip6configProperties);
                break;
            default:
                break;
            }
        }
        return networkInterfaceStatus;
    }

    private NetworkInterfaceStatus createModemStatus(String interfaceId, Device device, Properties deviceProperties,
            Optional<Properties> ip4configProperties, Optional<Properties> ip6configProperties) throws DBusException {
        NetworkInterfaceStatus networkInterfaceStatus;
        Optional<String> modemPath = this.networkManager.getModemManagerDbusPath(device.getObjectPath());
        Optional<Properties> modemDeviceProperties = Optional.empty();
        List<SimProperties> simProperties = Collections.emptyList();
        List<Properties> bearerProperties = Collections.emptyList();
        if (modemPath.isPresent()) {
            modemDeviceProperties = this.modemManager.getModemProperties(modemPath.get());
            if (modemDeviceProperties.isPresent()) {
                simProperties = this.modemManager.getModemSimProperties(modemDeviceProperties.get());
                bearerProperties = this.modemManager.getModemBearersProperties(modemPath.get(),
                        modemDeviceProperties.get());
            }
        }
        DevicePropertiesWrapper modemPropertiesWrapper = new DevicePropertiesWrapper(deviceProperties,
                modemDeviceProperties, NMDeviceType.NM_DEVICE_TYPE_MODEM);
        networkInterfaceStatus = NMStatusConverter.buildModemStatus(interfaceId, modemPropertiesWrapper,
                ip4configProperties, ip6configProperties, simProperties, bearerProperties);
        return networkInterfaceStatus;
    }

    private NetworkInterfaceStatus createWirelessStatus(String interfaceId,
            CommandExecutorService commandExecutorService, Device device, Properties deviceProperties,
            Optional<Properties> ip4configProperties, Optional<Properties> ip6configProperties)
            throws DBusException, KuraException {
        NetworkInterfaceStatus networkInterfaceStatus = null;
        Wireless wirelessDevice = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Wireless.class);
        Properties wirelessDeviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME,
                wirelessDevice.getObjectPath(), Properties.class);

        List<Properties> accessPoints = this.networkManager.getAllAccessPoints(wirelessDevice);

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
                ip4configProperties, ip6configProperties, new AccessPointsProperties(activeAccessPoint, accessPoints),
                new SupportedChannelsProperties(countryCode, supportedChannels));
        return networkInterfaceStatus;
    }

    public synchronized void apply(Map<String, Object> networkConfiguration) throws DBusException {
        try {
            configurationEnforcementDisable();
            this.modemManager.resetHandlersDisable();
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
            this.modemManager.resetHandlersDisable();
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
            this.modemManager.resetHandlersDisable(deviceId);
            doApply(deviceId, this.cachedConfiguration);
        } finally {
            configurationEnforcementEnable();
        }
    }

    private synchronized void doApply(Map<String, Object> networkConfiguration) throws DBusException {
        logger.info("Applying configuration using NetworkManager Dbus connector");
        List<Device> availableDevices = this.networkManager.getAllDevices();
        availableDevices.forEach(device -> {
            try {
                String deviceId = getInterfaceIdByDBusPath(device.getObjectPath());
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

        Optional<Device> device = getNetworkManagerDeviceByInterfaceId(deviceIdToBeConfigured);
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
        NMDeviceType deviceType = this.networkManager.getDeviceType(device.getObjectPath());

        KuraIpStatus ip4Status = KuraIpStatus
                .fromString(properties.get(String.class, "net.interface.%s.config.ip4.status", deviceId));

        Optional<KuraIpStatus> ip6OptStatus = KuraIpStatus
                .fromString(properties.getOpt(String.class, "net.interface.%s.config.ip6.status", deviceId));
        KuraIpStatus ip6Status;

        if (!ip6OptStatus.isPresent()) {
            ip6Status = ip4Status == KuraIpStatus.UNMANAGED ? KuraIpStatus.UNMANAGED : KuraIpStatus.DISABLED;
        } else {
            ip6Status = ip6OptStatus.get();
        }

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
            Optional<String> mmDbusPath = this.networkManager.getModemManagerDbusPath(device.getObjectPath());
            this.modemManager.setGPS(mmDbusPath, enableGPS);
        }

    }

    private void enableInterface(String deviceId, NetworkProperties properties, Device device, NMDeviceType deviceType)
            throws DBusException {
        if (Boolean.FALSE.equals(this.networkManager.isDeviceManaged(device))) {
            this.networkManager.setDeviceManaged(device, true);
        }
        String interfaceName = this.networkManager.getDeviceInterface(device);

        Optional<Connection> connection = this.networkManager.getAssociatedConnection(device);
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
            this.networkManager.activateConnection(connection.get(), device);
            dsLock.waitForSignal();
        } catch (DBusExecutionException e) {
            logger.warn("Couldn't complete activation of {} interface, caused by:", deviceId, e);
        }

        // Housekeeping
        List<Connection> availableConnections = this.networkManager.getAvaliableConnections(device);
        for (Connection availableConnection : availableConnections) {
            if (!connection.get().getObjectPath().equals(availableConnection.getObjectPath())) {
                availableConnection.Delete();
            }
        }

        if (deviceType == NMDeviceType.NM_DEVICE_TYPE_MODEM) {
            int delayMinutes = properties.get(Integer.class, "net.interface.%s.config.resetTimeout", deviceId);

            if (delayMinutes != 0) {
                Optional<String> mmDbusPath = this.networkManager.getModemManagerDbusPath(device.getObjectPath());
                this.modemManager.resetHandlerEnable(deviceId, mmDbusPath, delayMinutes, device.getObjectPath());
            }
        }

    }

    private void manageNonConfiguredInterface(Device device, String deviceId) throws DBusException {
        NMDeviceType deviceType = this.networkManager.getDeviceType(device.getObjectPath());

        if (!CONFIGURATION_SUPPORTED_DEVICE_TYPES.contains(deviceType)) {
            logger.warn("Device \"{}\" of type \"{}\" currently not supported", deviceId, deviceType);
            return;
        }

        if (Boolean.FALSE.equals(this.networkManager.isDeviceManaged(device))) {
            this.networkManager.setDeviceManaged(device, true);
        }

        logger.warn("Device \"{}\" of type \"{}\" not configured. Disabling...", deviceId, deviceType);

        disable(device);

        if (deviceType == NMDeviceType.NM_DEVICE_TYPE_MODEM) {
            Optional<String> mmDbusPath = this.networkManager.getModemManagerDbusPath(device.getObjectPath());
            this.modemManager.setGPS(mmDbusPath, Optional.of(false));
        }
    }

    private void disable(Device device) throws DBusException {
        Optional<Connection> appliedConnection = this.networkManager.getAppliedConnection(device);

        NMDeviceState deviceState = this.networkManager.getDeviceState(device);
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

        List<Connection> availableConnections = this.networkManager.getAvaliableConnections(device);
        for (Connection connection : availableConnections) {
            connection.Delete();
        }
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
}
