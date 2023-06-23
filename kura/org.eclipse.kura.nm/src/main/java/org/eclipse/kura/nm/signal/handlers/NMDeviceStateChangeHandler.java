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
package org.eclipse.kura.nm.signal.handlers;

import java.util.concurrent.CountDownLatch;

import org.eclipse.kura.nm.enums.NMDeviceState;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMDeviceStateChangeHandler implements DBusSigHandler<Device.StateChanged> {

    private static final Logger logger = LoggerFactory.getLogger(NMDeviceStateChangeHandler.class);

    private final CountDownLatch latch;
    private final String path;
    private final NMDeviceState expectedState;

    public NMDeviceStateChangeHandler(CountDownLatch latch, String path, NMDeviceState expectedNmDeviceState) {
        this.latch = latch;
        this.path = path;
        this.expectedState = expectedNmDeviceState;
    }

    @Override
    public void handle(Device.StateChanged s) {

        NMDeviceState oldState = NMDeviceState.fromUInt32(s.getOldState());
        NMDeviceState newState = NMDeviceState.fromUInt32(s.getNewState());

        logger.trace("Device state change detected: {} -> {}, for {}", oldState, newState, s.getPath());
        if (s.getPath().equals(this.path) && newState == this.expectedState) {
            logger.debug("Notify waiting thread");
            this.latch.countDown();
        }
    }
}
