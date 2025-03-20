/*******************************************************************************
 * Copyright (c) 2023, 2025 Eurotech and/or its affiliates and others
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.nm.enums.MMModemLocationSource;
import org.eclipse.kura.nm.enums.MMModemState;
import org.eclipse.kura.nm.signal.handlers.NMModemConnectionHandler;
import org.eclipse.kura.nm.signal.handlers.NMModemResetHandler;
import org.eclipse.kura.nm.signal.handlers.NMModemResetTimerTask;
import org.eclipse.kura.nm.status.SimProperties;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.modemmanager1.modem.Location;
import org.freedesktop.networkmanager.Device;
import org.freedesktop.networkmanager.settings.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModemManagerDbusWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ModemManagerDbusWrapper.class);

    private static final String MM_BUS_NAME = "org.freedesktop.ModemManager1";
    private static final String MM_MODEM_NAME = "org.freedesktop.ModemManager1.Modem";
    private static final String MM_SIM_NAME = "org.freedesktop.ModemManager1.Sim";
    private static final String MM_LOCATION_BUS_NAME = "org.freedesktop.ModemManager1.Modem.Location";
    private static final String MM_MODEM_PROPERTY_STATE = "State";

    private final DBusConnection dbusConnection;

    private final Map<String, NMModemResetHandler> modemHandlers = new HashMap<>();
    private final Map<String, MMFailedModemResetTimer> failedModemResetTimers = new HashMap<>();
    private final Map<String, NMModemConnectionHandler> modemConnectionHandlers = new HashMap<>();

    public ModemManagerDbusWrapper(DBusConnection dbusConnection) {
        this.dbusConnection = dbusConnection;
    }

    protected void setGPS(Optional<String> modemDevicePath, Optional<Boolean> enableGPS, Optional<String> gpsModeString)
            throws DBusException {
        if (!modemDevicePath.isPresent()) {
            logger.warn("Cannot retrieve MM.Modem from NM.Modem. Skipping GPS configuration.");
            return;
        }

        enableModem(modemDevicePath.get());

        boolean isGPSSourceEnabled = enableGPS.isPresent() && enableGPS.get();
        KuraModemGPSMode desiredGPSMode = gpsModeString.isPresent() ? KuraModemGPSMode.fromString(gpsModeString.get())
                : KuraModemGPSMode.KURA_MODEM_GPS_MODE_UNMANAGED;

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
            desiredLocationSources = KuraModemGPSMode.toMMModemLocationSources(desiredGPSMode);

            if (!availableLocationSources.containsAll(desiredLocationSources)) {
                logger.warn("Cannot setup Modem.Location, {} not supported for {}", desiredLocationSources,
                        modemLocationProperties.getObjectPath());
                return;
            }
        }

        logger.debug("Modem location setup {} for modem {}", currentLocationSources, modemDevicePath.get());

        if (!currentLocationSources.equals(desiredLocationSources)) {
            if (!EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE).equals(desiredLocationSources)) {
                modemLocation.Setup(MMModemLocationSource.toBitMaskFromMMModemLocationSource(
                        EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE)), false);
            }
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

    public Modem getModem(String modemPath) throws DBusException {
        return dbusConnection.getRemoteObject(MM_BUS_NAME, modemPath, Modem.class);
    }

    public Location getModemManagerLocation(String modemPath) throws DBusException {
        return dbusConnection.getRemoteObject(MM_BUS_NAME, modemPath, Location.class);
    }

    public Properties getLocationProperties(Location location) throws DBusException {
        return dbusConnection.getRemoteObject(MM_BUS_NAME, location.getObjectPath(), Properties.class);
    }

    public MMModemState getMMModemState(Properties modemProperties) {
        return MMModemState.toMMModemState(modemProperties.Get(MM_MODEM_NAME, MM_MODEM_PROPERTY_STATE));
    }

    public MMModemState getMMModemState(String modemPath) throws DBusException {
        Optional<Properties> properties = getModemProperties(modemPath);
        if (properties.isPresent()) {
            return getMMModemState(properties.get());
        } else {
            return MMModemState.MM_MODEM_STATE_UNKNOWN;
        }
    }

    public Optional<Properties> getModemProperties(String modemPath) throws DBusException {
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

        resetHandlersDisable(deviceId);

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
            this.modemHandlers.remove(deviceId);
        }
    }

    protected void failedModemResetTimerSchedule(String deviceId, Optional<String> modemManagerDbusPath,
            int delayMinutes) throws DBusException {
        if (!modemManagerDbusPath.isPresent()) {
            logger.warn("Cannot retrieve modem device for {}. Skipping modem reset monitor setup.", deviceId);
            return;
        }

        Modem mmModemDevice = this.dbusConnection.getRemoteObject(MM_BUS_NAME, modemManagerDbusPath.get(), Modem.class);

        MMFailedModemResetTimer resetTimer = new MMFailedModemResetTimer(mmModemDevice, delayMinutes);
        resetTimer.schedule();

        this.failedModemResetTimers.put(deviceId, resetTimer);
    }

    protected void failedModemResetTimerCancel() {
        for (String deviceId : this.failedModemResetTimers.keySet()) {
            failedModemResetTimerCancel(deviceId);
        }
        this.modemHandlers.clear();
    }

    protected void failedModemResetTimerCancel(String deviceId) {
        if (this.failedModemResetTimers.containsKey(deviceId)) {
            MMFailedModemResetTimer timer = this.failedModemResetTimers.get(deviceId);
            timer.cancel();
        }
    }

    protected boolean isMMFailedModemResetTimerArmed(String deviceId) {
        return this.failedModemResetTimers.containsKey(deviceId);
    }

    private class MMFailedModemResetTimerTask extends NMModemResetTimerTask {

        public MMFailedModemResetTimerTask(Modem modem) {
            super(modem);
        }

        @Override
        public void run() {
            try {
                MMModemState modemState = getMMModemState(this.getModemDbusPath());
                if (MMModemState.MM_MODEM_STATE_FAILED.equals(modemState)) {
                    super.run();
                } else {
                    NMModemResetTimerTask.logger.info("Modem state changed. Reset skipped.");
                }
            } catch (DBusException e) {
                NMModemResetTimerTask.logger.warn("Couldn't get state of modem interface, caused by:", e);
            }
        }

    }

    private class MMFailedModemResetTimer {

        private final Timer timer = new Timer("FailedModemResetTimer");
        private final MMFailedModemResetTimerTask task;
        private final long delay;

        public MMFailedModemResetTimer(Modem modem, long delayMinutes) {
            this.delay = delayMinutes * 60L * 1000L;
            this.task = new MMFailedModemResetTimerTask(modem);
        }

        public void schedule() {
            this.timer.schedule(this.task, this.delay);
        }

        public void cancel() {
            if (this.task != null) {
                this.task.cancel();
            }
            this.timer.cancel();
        }
    }

    protected void modemConnectionTask(String deviceId, ModemManagerDbusWrapper modemManager,
            NetworkManagerDbusWrapper networkManager, Connection connection, Device device, int maxFail, int holdoff,
            boolean autoconnect, int resetDelayMinutes) throws DBusException {
        connectionHandlersDisable(deviceId);
        MMModemConnectionScheduler connectionScheduler = new MMModemConnectionScheduler(networkManager, modemManager,
                connection, device, maxFail, holdoff, autoconnect, resetDelayMinutes);
        if (autoconnect) {
            connectionScheduler.scheduleConnection();
        }
        if (resetDelayMinutes > 0) {
            connectionScheduler.scheduleReset();
        }
        NMModemConnectionHandler modemConnectionHandler = new NMModemConnectionHandler(connectionScheduler);

        this.modemConnectionHandlers.put(deviceId, modemConnectionHandler);

        if (autoconnect || resetDelayMinutes > 0) {
            this.dbusConnection.addSigHandler(org.freedesktop.networkmanager.Device.StateChanged.class,
                    modemConnectionHandler);
        }
    }

    protected void connectionHandlersDisable(String deviceId) {
        // use modemConnectionTaskCancel?
        if (this.modemConnectionHandlers.containsKey(deviceId)) {
            NMModemConnectionHandler handler = this.modemConnectionHandlers.get(deviceId);
            handler.getModemConnectionScheduler().cancelAndShutdown();
            try {
                this.dbusConnection.removeSigHandler(org.freedesktop.networkmanager.Device.StateChanged.class, handler);
            } catch (DBusException e) {
                logger.warn("Couldn't remove signal handler for: {}. Caused by:", deviceId, e);
            }
            this.modemConnectionHandlers.remove(deviceId);
        }
    }

    protected void modemModemConnectionTaskCancel() {
        this.modemConnectionHandlers.keySet().forEach(this::modemConnectionTaskCancel);
        this.modemConnectionHandlers.clear();
    }

    protected void modemConnectionTaskCancel(String deviceId) {
        if (this.modemConnectionHandlers.containsKey(deviceId)) {
            this.modemConnectionHandlers.get(deviceId).getModemConnectionScheduler().cancelAndShutdown();
            this.modemConnectionHandlers.remove(deviceId);
        }
    }

    public class MMModemConnectionScheduler {

        private static final int DELAY = 90;

        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
        private final NetworkManagerDbusWrapper networkManager;
        private final ModemManagerDbusWrapper modemManager;
        private final Device device;
        private final Connection connection;
        private final int maxFail;
        private final int holdoff;
        private final boolean autoconnect;
        private final int resetDelayMinutes;

        private ScheduledFuture<?> connectionHandler;
        private ScheduledFuture<?> resetHandler;
        private AtomicBoolean isConnectionScheduled = new AtomicBoolean(false);
        private AtomicBoolean isResetScheduled = new AtomicBoolean(false);

        public MMModemConnectionScheduler(NetworkManagerDbusWrapper networkManager,
                ModemManagerDbusWrapper modemManager, Connection connection, Device device, int maxFail, int holdoff,
                boolean autoconnect, int resetDelayMinutes) {
            this.maxFail = maxFail;
            this.holdoff = holdoff;
            this.networkManager = Objects.requireNonNull(networkManager);
            this.modemManager = Objects.requireNonNull(modemManager);
            this.device = Objects.requireNonNull(device);
            this.connection = Objects.requireNonNull(connection);
            this.autoconnect = autoconnect;
            this.resetDelayMinutes = resetDelayMinutes;
        }

        public void scheduleConnection() {
            if (isConnectionScheduled.get() || !this.autoconnect) {
                return;
            }
            logger.info("Schedule connection for modem {}", this.device.getObjectPath());
            this.isConnectionScheduled.set(true);
            this.connectionHandler = this.scheduler.schedule(() -> tryConnection(1), 0, TimeUnit.SECONDS);
        }

        public void scheduleReset() {
            if (isResetScheduled.get() || this.resetDelayMinutes <= 0) {
                return;
            }
            logger.info("Schedule reset for modem {}", this.device.getObjectPath());
            this.isResetScheduled.set(true);
            this.resetHandler = this.scheduler.schedule(() -> {
                try {
                    if (!isModemConnected()) {
                        if (this.connectionHandler != null) {
                            this.connectionHandler.cancel(true);
                        }
                        Optional<String> mmDbusPath = this.networkManager
                                .getModemManagerDbusPath(device.getObjectPath());
                        Modem modem = ModemManagerDbusWrapper.this.dbusConnection.getRemoteObject(MM_BUS_NAME,
                                mmDbusPath.get(), Modem.class);
                        modem.Reset();
                        logger.info("Modem reset successful for modem {}", this.device.getObjectPath());
                    }
                } catch (DBusException | DBusExecutionException e) {
                    logger.warn("Could not reset modem {} because: ", this.device.getObjectPath(), e);
                }
                this.isResetScheduled.set(false);
            }, this.resetDelayMinutes, TimeUnit.MINUTES);
        }

        // remove handlers and get polling...
        // disable reset if 0

        private void tryConnection(int attemptNumber) {
            try {
                logger.debug("Connection attempt {} for modem {} ...", attemptNumber, this.device.getObjectPath());
                this.networkManager.activateConnection(this.connection, this.device);
                if (isModemConnected()) {
                    logger.info("Connection attempt {} for modem {} successful", attemptNumber,
                            this.device.getObjectPath());
                    if (this.connectionHandler != null) {
                        this.connectionHandler.cancel(true);
                    }
                    this.isConnectionScheduled.set(false);
                } else {
                    logger.warn("Could not activate connection for modem {}", this.device.getObjectPath());
                    if (attemptNumber < this.maxFail) {
                        this.connectionHandler = this.scheduler.schedule(() -> this.tryConnection(attemptNumber + 1),
                                this.holdoff, TimeUnit.SECONDS);
                    } else {
                        this.connectionHandler = this.scheduler.schedule(() -> tryConnection(1), DELAY,
                                TimeUnit.SECONDS);
                    }
                }
            } catch (DBusException | DBusExecutionException e) {
                logger.warn("Could not activate connection for modem {} because: ", this.device.getObjectPath(), e);
                if (attemptNumber < this.maxFail) {
                    this.connectionHandler = this.scheduler.schedule(() -> this.tryConnection(attemptNumber + 1),
                            this.holdoff, TimeUnit.SECONDS);
                } else {
                    this.connectionHandler = this.scheduler.schedule(() -> tryConnection(1), DELAY, TimeUnit.SECONDS);
                }
            }
        }

        public void cancelAndShutdown() {
            cancel();
            this.scheduler.shutdownNow();
        }

        public void cancel() {
            if (this.connectionHandler != null) {
                this.connectionHandler.cancel(true);
            }
            if (this.resetHandler != null) {
                this.resetHandler.cancel(true);
            }
            this.isConnectionScheduled.set(false);
            this.isResetScheduled.set(false);
        }

        public Device getDevice() {
            return this.device;
        }

        public boolean isScheduled() {
            return this.isConnectionScheduled.get();
        }

        private boolean isModemFailed() throws DBusException {
            Optional<String> mmDbusPath = this.networkManager.getModemManagerDbusPath(this.device.getObjectPath());
            if (!mmDbusPath.isPresent()) {
                return false;
            }
            MMModemState modemState = this.modemManager.getMMModemState(mmDbusPath.get());
            return MMModemState.MM_MODEM_STATE_FAILED.equals(modemState);
        }

        private boolean isModemConnected() throws DBusException {
            Optional<String> mmDbusPath = this.networkManager.getModemManagerDbusPath(this.device.getObjectPath());
            if (!mmDbusPath.isPresent()) {
                return false;
            }
            MMModemState modemState = this.modemManager.getMMModemState(mmDbusPath.get());
            return MMModemState.MM_MODEM_STATE_CONNECTED.equals(modemState);
        }
    }

}
