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
package org.eclipse.kura.nm.signal.handlers;

import java.util.Objects;

import org.eclipse.kura.nm.ModemManagerDbusWrapper.MMModemConnectionScheduler;
import org.eclipse.kura.nm.enums.NMDeviceState;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMModemConnectionHandler implements DBusSigHandler<Device.StateChanged> {

    private static final Logger logger = LoggerFactory.getLogger(NMModemConnectionHandler.class);
    private final MMModemConnectionScheduler modemConnectionScheduler;

    public NMModemConnectionHandler(MMModemConnectionScheduler modemConnectionScheduler) {
        this.modemConnectionScheduler = Objects.requireNonNull(modemConnectionScheduler);
    }

    @Override
    public void handle(Device.StateChanged s) {
        if (!s.getPath().equals(this.modemConnectionScheduler.getDevice().getObjectPath())) {
            // Ignore signals coming from other devices
            return;
        }

        NMDeviceState oldState = NMDeviceState.fromUInt32(s.getOldState());
        NMDeviceState newState = NMDeviceState.fromUInt32(s.getNewState());

        logger.debug("Modem state change detected: {} -> {}, for device {}", oldState, newState, s.getPath());

        if (oldState == NMDeviceState.NM_DEVICE_STATE_FAILED
                && newState == NMDeviceState.NM_DEVICE_STATE_DISCONNECTED) {
            // if (modemConnectionScheduler.isScheduled()) {
            // logger.debug("Modem {} already scheduled for connection. Ignoring event...", s.getPath());
            // return;
            // }

            // logger.info("Modem {} disconnected. Scheduling modem connection in {} minutes ...", s.getPath(),
            // this.delay / (60 * 1000));

            // this.scheduledTasks = new NMModemResetTimerTask(this.mmModemDevice);
            // this.modemResetTimer.schedule(this.scheduledTasks, this.delay);
            this.modemConnectionScheduler.scheduleConnection();
            this.modemConnectionScheduler.scheduleReset();
        } else if (newState == NMDeviceState.NM_DEVICE_STATE_ACTIVATED) {

            // if (!modemConnectionScheduler.isScheduled()) {
            // return;
            // }

            logger.info("Modem reconnected. Cancelling scheduled modem connection...");
            this.modemConnectionScheduler.cancel();
            // this.scheduledTasks.cancel();
            // this.scheduledTasks = null;
        }
    }

    // private boolean timerAlreadyScheduled() {
    // return Objects.nonNull(this.scheduledTasks) && !this.scheduledTasks.expired();
    // }
    //
    // public void clearTimer() {
    // if (timerAlreadyScheduled()) {
    // logger.info("Clearing timer for {}. Cancelling scheduled modem reset...", this.nmDevicePath);
    // this.scheduledTasks.cancel();
    // this.scheduledTasks = null;
    // this.modemResetTimer.cancel();
    // }
    // logger.debug("ModemStateHandler disarmed for {}", this.nmDevicePath);
    // }

    public MMModemConnectionScheduler getModemConnectionScheduler() {
        return this.modemConnectionScheduler;
    }

}
