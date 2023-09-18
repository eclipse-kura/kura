/*******************************************************************************
 * Copyright (c) 2023 Areti and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Areti
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.nm.signal.handlers;


import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.nm.NMDbusConnector;
import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceCreationLock {

    private static final Logger logger = LoggerFactory.getLogger(DeviceCreationLock.class);
    
    private final BlockingQueue<Device> deviceQueue;
    private final NMDeviceCreationHandler handler;
    private final DBusConnection dbusConnection;
    
    public DeviceCreationLock(NMDbusConnector nm, String deviceId) throws DBusException {
        this.deviceQueue = new ArrayBlockingQueue<>(1);
        this.dbusConnection = nm.getDbusConnection();
        this.handler = new NMDeviceCreationHandler(nm, deviceId, this.deviceQueue);

        this.dbusConnection.addSigHandler(NetworkManager.DeviceAdded.class, this.handler);
    }
    
    public Optional<Device> waitForDeviceCreation() throws DBusException, TimeoutException {
        return waitForDeviceCreation(5L);
    }

    public Optional<Device> waitForDeviceCreation(long timeoutSeconds) throws DBusException, TimeoutException {
        try {
            return Optional.ofNullable(deviceQueue.poll(timeoutSeconds, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            logger.warn("Wait interrupted because of:", e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            this.dbusConnection.removeSigHandler(NetworkManager.DeviceAdded.class, this.handler);
        }
    }
}
