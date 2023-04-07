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

import java.util.Objects;
import java.util.Timer;

import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMModemResetHandler implements DBusSigHandler<Device.StateChanged> {

    private static final Logger logger = LoggerFactory.getLogger(NMModemResetHandler.class);
    private final Timer modemResetTimer = new Timer("ModemResetTimer");
    private NMModemResetTimerTask scheduledTasks = null;

    private final String nmDevicePath;
    private final Modem mmModemDevice;
    private final long delay;

    public NMModemResetHandler(String nmDeviceDBusPath, Modem mmModem, long modemResetDelayMilliseconds) {
        this.nmDevicePath = nmDeviceDBusPath;
        this.mmModemDevice = Objects.requireNonNull(mmModem);
        this.delay = modemResetDelayMilliseconds;
        logger.debug("ModemStateHandler armed for {}", this.nmDevicePath);
    }

    public String getNMDevicePath() {
        return this.nmDevicePath;
    }

    @Override
    public void handle(Device.StateChanged s) {
        if (!s.getPath().equals(this.nmDevicePath)) {
            // Ignore signals coming from other devices
            return;
        }

        NMDeviceState oldState = NMDeviceState.fromUInt32(s.getOldState());
        NMDeviceState newState = NMDeviceState.fromUInt32(s.getNewState());

        logger.debug("Modem state change detected: {} -> {}, for device {}", oldState, newState, s.getPath());

        if (oldState == NMDeviceState.NM_DEVICE_STATE_FAILED
                && newState == NMDeviceState.NM_DEVICE_STATE_DISCONNECTED) {
            if (timerAlreadyScheduled()) {
                logger.debug("Modem {} already scheduled for reset. Ignoring event...", s.getPath());
                return;
            }

            logger.info("Modem {} disconnected. Scheduling modem reset in {} minutes ...", s.getPath(),
                    this.delay / (60 * 1000));

            this.scheduledTasks = new NMModemResetTimerTask(this.mmModemDevice);
            this.modemResetTimer.schedule(this.scheduledTasks, this.delay);
        } else if (newState == NMDeviceState.NM_DEVICE_STATE_ACTIVATED) {

            if (!timerAlreadyScheduled()) {
                return;
            }

            logger.info("Modem {} reconnected. Cancelling scheduled modem reset...", this.nmDevicePath);
            this.scheduledTasks.cancel();
            this.scheduledTasks = null;
        }
    }

    private boolean timerAlreadyScheduled() {
        return Objects.nonNull(this.scheduledTasks) && !this.scheduledTasks.expired();
    }

    public void clearTimer() {
        if (timerAlreadyScheduled()) {
            logger.info("Clearing timer for {}. Cancelling scheduled modem reset...", this.nmDevicePath);
            this.scheduledTasks.cancel();
            this.scheduledTasks = null;
        }
        logger.debug("ModemStateHandler disarmed for {}", this.nmDevicePath);
    }

}
