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

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModemTaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ModemTaskScheduler.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private final Device device;
    private final int maxFail;
    private final int holdoff;
    private final boolean autoconnect;
    private final int resetDelayMinutes;
    private final String deviceId;

    private Optional<NMDbusConnector> nmDbusConnector = Optional.empty();
    private ScheduledFuture<?> connectionHandler;
    private ScheduledFuture<?> resetHandler;
    private AtomicBoolean isConnectionScheduled = new AtomicBoolean(false);
    private AtomicBoolean isResetScheduled = new AtomicBoolean(false);
    private int delay = 0;

    public ModemTaskScheduler(String deviceId, Device device, NetworkProperties properties) {

        try {
            this.nmDbusConnector = Optional.of(NMDbusConnector.getInstance());
        } catch (DBusExecutionException | DBusException e) {
            logger.error("Cannot initialize NMDbusConnector due to: ", e);
        }

        this.device = Objects.requireNonNull(device);
        this.resetDelayMinutes = properties.get(Integer.class, "net.interface.%s.config.resetTimeout", deviceId);
        this.autoconnect = properties.get(Boolean.class, "net.interface.%s.config.persist", deviceId);
        this.holdoff = properties.get(Integer.class, "net.interface.%s.config.holdoff", deviceId);
        this.maxFail = properties.get(Integer.class, "net.interface.%s.config.maxFail", deviceId);
        this.deviceId = deviceId;

        this.delay = this.holdoff != 0 && this.maxFail != 0 ? this.holdoff * this.maxFail : 90;
    }

    public void scheduleConnection() {
        if (isModemConnectedAndConnectionActivated()) {
            logger.info("Connection for modem {} successful", this.deviceId);
            return;
        }
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
            if (this.nmDbusConnector.isPresent()) {
                this.nmDbusConnector.get().apply(deviceId);
            } else {
                logger.warn("Could not attempt reconnection since the NMDbusConnector is not available.");
            }
            if (isModemConnectedAndConnectionActivated()) {
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
        if (isResetScheduled.get() || this.resetDelayMinutes <= 0 || isModemConnectedAndConnectionActivated()) {
            return;
        }
        logger.info("Schedule reset for modem {} with path {}", this.deviceId, this.device.getObjectPath());
        this.isResetScheduled.set(true);
        this.resetHandler = this.scheduler.schedule(this::resetModem, this.resetDelayMinutes, TimeUnit.MINUTES);
    }

    private void resetModem() {
        try {
            if (!isModemConnectedAndConnectionActivated()) {
                if (this.connectionHandler != null) {
                    this.connectionHandler.cancel(true);
                }
                this.isConnectionScheduled.set(false);
                Optional<Modem> modem = Optional.empty();
                if (this.nmDbusConnector.isPresent()) {
                    modem = this.nmDbusConnector.get().getModem(device);
                } else {
                    logger.warn("Could get modem since the NMDbusConnector is not available.");
                }
                if (modem.isPresent()) {
                    modem.get().Reset();
                    logger.info("Modem reset successful for modem {} with path {}", this.deviceId,
                            this.device.getObjectPath());
                }
            }
        } catch (DBusException | DBusExecutionException e) {
            logger.warn("Could not reset modem {} with path {} because: ", this.deviceId, this.device.getObjectPath(),
                    e);
        }
        this.isResetScheduled.set(false);
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

    private boolean isModemConnectedAndConnectionActivated() {
        boolean isConnected = false;
        boolean isActivated = false;
        try {
            if (this.nmDbusConnector.isPresent()) {
                isConnected = this.nmDbusConnector.get().isModemConnected(this.device);
                isActivated = this.nmDbusConnector.get().isConnectionActivated(this.device);
            }
        } catch (DBusException e) {
            logger.warn("Could not get modem connection status for modem {} with path {} because: ", this.deviceId,
                    this.device.getObjectPath(), e);
        }
        return isConnected && isActivated;
    }

    protected void setNMDBusConnector(NMDbusConnector nmDbusConnector) {
        this.nmDbusConnector = Optional.of(nmDbusConnector);
    }
}
