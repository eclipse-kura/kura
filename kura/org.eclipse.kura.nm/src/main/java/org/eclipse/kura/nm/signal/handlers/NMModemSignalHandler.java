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

import org.eclipse.kura.nm.ModemTaskScheduler;
import org.eclipse.kura.nm.enums.NMDeviceState;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMModemSignalHandler implements DBusSigHandler<Device.StateChanged> {

    private static final Logger logger = LoggerFactory.getLogger(NMModemSignalHandler.class);
    private final ModemTaskScheduler modemTaskScheduler;

    public NMModemSignalHandler(ModemTaskScheduler modemTaskScheduler) {
        this.modemTaskScheduler = Objects.requireNonNull(modemTaskScheduler);
    }

    @Override
    public void handle(Device.StateChanged s) {
        if (!s.getPath().equals(this.modemTaskScheduler.getDevice().getObjectPath())) {
            // Ignore signals coming from other devices
            return;
        }

        NMDeviceState oldState = NMDeviceState.fromUInt32(s.getOldState());
        NMDeviceState newState = NMDeviceState.fromUInt32(s.getNewState());

        logger.debug("Modem state change detected: {} -> {}, for device {}", oldState, newState, s.getPath());

        if (oldState == NMDeviceState.NM_DEVICE_STATE_FAILED
                && newState == NMDeviceState.NM_DEVICE_STATE_DISCONNECTED) {
            this.modemTaskScheduler.scheduleConnection();
            this.modemTaskScheduler.scheduleReset();
        } else if (newState == NMDeviceState.NM_DEVICE_STATE_ACTIVATED) {

            logger.info("Modem reconnected. Cancelling scheduled modem connection...");
            this.modemTaskScheduler.cancel();
        }
    }

    public ModemTaskScheduler getModemConnectionScheduler() {
        return this.modemTaskScheduler;
    }

}
