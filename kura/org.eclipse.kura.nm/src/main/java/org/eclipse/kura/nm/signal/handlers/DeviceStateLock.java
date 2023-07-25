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

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.nm.enums.NMDeviceState;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceStateLock {

    private static final Logger logger = LoggerFactory.getLogger(DeviceStateLock.class);

    private final CountDownLatch latch = new CountDownLatch(1);
    private final NMDeviceStateChangeHandler stateHandler;
    private final DBusConnection dbusConnection;

    public DeviceStateLock(DBusConnection dbusConnection, String dbusPath, NMDeviceState expectedNmDeviceState)
            throws DBusException {
        if (Objects.isNull(dbusPath) || dbusPath.isEmpty() || dbusPath.equals("/")) {
            throw new IllegalArgumentException(String.format("Illegal DBus path for DeviceSateLock \"%s\"", dbusPath));
        }
        this.dbusConnection = Objects.requireNonNull(dbusConnection);
        this.stateHandler = new NMDeviceStateChangeHandler(this.latch, dbusPath, expectedNmDeviceState);

        this.dbusConnection.addSigHandler(Device.StateChanged.class, this.stateHandler);
    }

    public void waitForSignal() throws DBusException {
        try {
            boolean countdownCompleted = this.latch.await(5, TimeUnit.SECONDS);
            if (!countdownCompleted) {
                logger.warn("Timeout elapsed. Exiting anyway");
            }
        } catch (InterruptedException e) {
            logger.warn("Wait interrupted because of:", e);
            Thread.currentThread().interrupt();
        } finally {
            this.dbusConnection.removeSigHandler(Device.StateChanged.class, this.stateHandler);
        }
    }

}
