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

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.w1.wpa_supplicant1.Interface;

public class WPAScanLock {

    private static final Logger logger = LoggerFactory.getLogger(WPAScanLock.class);

    private final CountDownLatch latch = new CountDownLatch(1);
    private final WPAScanDoneHandler scanHandler;
    private final DBusConnection dbusConnection;

    public WPAScanLock(DBusConnection dbusConnection, String dbusPath) throws DBusException {
        if (Objects.isNull(dbusPath) || dbusPath.isEmpty() || dbusPath.equals("/")) {
            throw new IllegalArgumentException(String.format("Illegal DBus path for WPAScanLock \"%s\"", dbusPath));
        }
        this.dbusConnection = Objects.requireNonNull(dbusConnection);
        this.scanHandler = new WPAScanDoneHandler(this.latch, dbusPath);

        this.dbusConnection.addSigHandler(Interface.ScanDone.class, this.scanHandler);
    }

    public void waitForSignal(long scanTimeoutSeconds) throws DBusException {
        try {
            boolean countdownCompleted = this.latch.await(scanTimeoutSeconds, TimeUnit.SECONDS);
            if (!countdownCompleted) {
                logger.warn("Timeout elapsed. Exiting anyway");
            }
        } catch (InterruptedException e) {
            logger.warn("Wait interrupted because of:", e);
            Thread.currentThread().interrupt();
        } finally {
            this.dbusConnection.removeSigHandler(Interface.ScanDone.class, this.scanHandler);
        }
    }

}