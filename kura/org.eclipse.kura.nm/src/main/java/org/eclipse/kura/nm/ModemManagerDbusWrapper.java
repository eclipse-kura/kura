package org.eclipse.kura.nm;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.nm.enums.MMModemLocationSource;
import org.eclipse.kura.nm.enums.MMModemState;
import org.eclipse.kura.nm.signal.handlers.NMModemResetHandler;
import org.eclipse.kura.nm.status.SimProperties;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.modemmanager1.modem.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModemManagerDbusWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ModemManagerDbusWrapper.class);

    private static final String MM_BUS_NAME = "org.freedesktop.ModemManager1";
    private static final String MM_MODEM_NAME = "org.freedesktop.ModemManager1.Modem";
    private static final String MM_SIM_NAME = "org.freedesktop.ModemManager1.Sim";
    private static final String MM_MODEM_PROPERTY_STATE = "State";
    private static final String MM_LOCATION_BUS_NAME = "org.freedesktop.ModemManager1.Modem.Location";

    private final DBusConnection dbusConnection;

    private final Map<String, NMModemResetHandler> modemHandlers = new HashMap<>();

    protected ModemManagerDbusWrapper(DBusConnection dbusConnection) {
        this.dbusConnection = dbusConnection;
    }

    protected void setGPS(Optional<String> modemDevicePath, Optional<Boolean> enableGPS) throws DBusException {
        if (!modemDevicePath.isPresent()) {
            logger.warn("Cannot retrieve MM.Modem from NM.Modem. Skipping GPS configuration.");
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

    protected void enableModem(String modemDevicePath) throws DBusException {
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

    protected Optional<Properties> getModemProperties(String modemPath) throws DBusException {
        Optional<Properties> modemProperties = Optional.empty();
        Properties properties = this.dbusConnection.getRemoteObject(MM_BUS_NAME, modemPath, Properties.class);
        if (Objects.nonNull(properties)) {
            modemProperties = Optional.of(properties);
        }
        return modemProperties;
    }

    protected List<SimProperties> getModemSimProperties(Properties modemProperties) throws DBusException {
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
                    boolean isPrimary = index == primarySimSlot.intValue() - 1;

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

    protected String getHardwareSysfsPath(Optional<String> dbusPath) throws DBusException {
        if (!dbusPath.isPresent()) {
            throw new IllegalStateException(String.format("Cannot retrieve modem path for: %s.", dbusPath));
        }
        Optional<Properties> modemDeviceProperties = getModemProperties(dbusPath.get());
        if (!modemDeviceProperties.isPresent()) {
            throw new IllegalStateException(String.format("Cannot retrieve modem properties for: %s.", dbusPath));
        }
        String modemDeviceProperty = (String) modemDeviceProperties.get().Get(MM_MODEM_NAME, "Device");
        return modemDeviceProperty.substring(modemDeviceProperty.lastIndexOf("/") + 1);
    }

    protected List<Properties> getModemBearersProperties(String modemPath, Properties modemProperties)
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

    protected void resetHandlerEnable(String deviceId, Optional<String> modemManagerDbusPath, int delayMinutes,
            String networkManagerDbusPath) throws DBusException {
        if (!modemManagerDbusPath.isPresent()) {
            logger.warn("Cannot retrieve modem device for {}. Skipping modem reset monitor setup.", deviceId);
            return;
        }

        Modem mmModemDevice = this.dbusConnection.getRemoteObject(MM_BUS_NAME, modemManagerDbusPath.get(), Modem.class);

        NMModemResetHandler resetHandler = new NMModemResetHandler(networkManagerDbusPath, mmModemDevice,
                delayMinutes * 60L * 1000L);

        this.modemHandlers.put(deviceId, resetHandler);
        this.dbusConnection.addSigHandler(org.freedesktop.networkmanager.Device.StateChanged.class, resetHandler);
    }

    protected void resetHandlersDisable() {
        for (String deviceId : this.modemHandlers.keySet()) {
            resetHandlersDisable(deviceId);
        }
        this.modemHandlers.clear();
    }

    protected void resetHandlersDisable(String deviceId) {
        if (this.modemHandlers.containsKey(deviceId)) {
            NMModemResetHandler handler = this.modemHandlers.get(deviceId);
            handler.clearTimer();
            try {
                this.dbusConnection.removeSigHandler(org.freedesktop.networkmanager.Device.StateChanged.class, handler);
            } catch (DBusException e) {
                logger.warn("Couldn't remove signal handler for: {}. Caused by:", handler.getNMDevicePath(), e);
            }
        }
    }
}
