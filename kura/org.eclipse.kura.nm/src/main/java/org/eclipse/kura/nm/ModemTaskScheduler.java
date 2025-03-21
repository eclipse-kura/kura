/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.nm.enums.MMModemState;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.networkmanager.Device;
import org.freedesktop.networkmanager.settings.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModemTaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ModemTaskScheduler.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final NetworkManagerDbusWrapper networkManager;
    private final ModemManagerDbusWrapper modemManager;
    private final Device device;
    private final Connection connection;
    private final int maxFail;
    private final int holdoff;
    private final boolean autoconnect;
    private final int resetDelayMinutes;
    private final String deviceId;

    private ScheduledFuture<?> connectionHandler;
    private ScheduledFuture<?> resetHandler;
    private AtomicBoolean isConnectionScheduled = new AtomicBoolean(false);
    private AtomicBoolean isResetScheduled = new AtomicBoolean(false);
    private Optional<String> mmDbusPath;
    private int delay = 0;

    public ModemTaskScheduler(NetworkManagerDbusWrapper networkManager, ModemManagerDbusWrapper modemManager,
            Connection connection, Device device, String deviceId, NetworkProperties properties) {
        this.networkManager = Objects.requireNonNull(networkManager);
        this.modemManager = Objects.requireNonNull(modemManager);
        this.device = Objects.requireNonNull(device);
        this.connection = Objects.requireNonNull(connection);
        this.resetDelayMinutes = properties.get(Integer.class, "net.interface.%s.config.resetTimeout", deviceId);
        this.autoconnect = properties.get(Boolean.class, "net.interface.%s.config.persist", deviceId);
        this.holdoff = properties.get(Integer.class, "net.interface.%s.config.holdoff", deviceId);
        this.maxFail = properties.get(Integer.class, "net.interface.%s.config.maxFail", deviceId);
        this.deviceId = deviceId;

        this.delay = this.holdoff != 0 && this.maxFail != 0 ? this.holdoff * this.maxFail : 90;
        try {
            this.mmDbusPath = this.networkManager.getModemManagerDbusPath(this.device.getObjectPath());
        } catch (DBusException e) {
            logger.warn("Could not get ModemManager dbus path for device {} because: ", this.device.getObjectPath(), e);
            this.mmDbusPath = Optional.empty();
        }
    }

    public void scheduleConnection() {
        if (isConnectionScheduled.get() || !this.autoconnect) {
            return;
        }
        logger.info("Schedule connection for modem {} with path {}", this.deviceId, this.device.getObjectPath());
        this.isConnectionScheduled.set(true);
        this.connectionHandler = this.scheduler.schedule(() -> tryConnection(1), 0, TimeUnit.SECONDS);
    }

    private void tryConnection(int attemptNumber) {
        try {
            logger.debug("Connection attempt {} for modem {} with path {} ...", attemptNumber, this.deviceId,
                    this.device.getObjectPath());
            this.networkManager.activateConnection(this.connection, this.device);
            if (isModemConnected()) {
                logger.info("Connection for modem {} successful", this.deviceId);
                if (this.connectionHandler != null) {
                    this.connectionHandler.cancel(true);
                }
                this.isConnectionScheduled.set(false);
            } else {
                logger.warn("Could not activate connection for modem {} with path {}", this.deviceId,
                        this.device.getObjectPath());
                scheduleConnectInternal(attemptNumber);
            }
        } catch (DBusException | DBusExecutionException e) {
            logger.warn("Could not activate connection for modem {} with path {} because: ", this.deviceId,
                    this.device.getObjectPath(), e);
            scheduleConnectInternal(attemptNumber);
        }
    }

    private void scheduleConnectInternal(int attemptNumber) {
        if (attemptNumber < this.maxFail) {
            this.connectionHandler = this.scheduler.schedule(() -> this.tryConnection(attemptNumber + 1), this.holdoff,
                    TimeUnit.SECONDS);
        } else {
            this.connectionHandler = this.scheduler.schedule(() -> tryConnection(1), this.delay, TimeUnit.SECONDS);
        }
    }

    public void scheduleReset() {
        if (isResetScheduled.get() || this.resetDelayMinutes <= 0) {
            return;
        }
        logger.info("Schedule reset for modem {} with path {}", this.deviceId, this.device.getObjectPath());
        this.isResetScheduled.set(true);
        this.resetHandler = this.scheduler.schedule(() -> {
            try {
                if (!isModemConnected()) {
                    if (this.connectionHandler != null) {
                        this.connectionHandler.cancel(true);
                    }
                    this.isConnectionScheduled.set(false);
                    if (this.mmDbusPath.isPresent()) {
                        Modem modem = this.modemManager.getModem(mmDbusPath.get());
                        modem.Reset();
                        logger.info("Modem reset successful for modem {} with path {}", this.deviceId,
                                this.device.getObjectPath());
                    }
                }
            } catch (DBusException | DBusExecutionException e) {
                logger.warn("Could not reset modem {} with path {} because: ", this.deviceId,
                        this.device.getObjectPath(), e);
            }
            this.isResetScheduled.set(false);
        }, this.resetDelayMinutes, TimeUnit.MINUTES);
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

    protected boolean isConnectionScheduled() {
        return this.isConnectionScheduled.get();
    }

    protected boolean isResetScheduled() {
        return this.isResetScheduled.get();
    }

    private boolean isModemConnected() throws DBusException {
        if (!this.mmDbusPath.isPresent()) {
            return false;
        }
        MMModemState modemState = this.modemManager.getMMModemState(mmDbusPath.get());
        return MMModemState.MM_MODEM_STATE_CONNECTED.equals(modemState);
    }
}
