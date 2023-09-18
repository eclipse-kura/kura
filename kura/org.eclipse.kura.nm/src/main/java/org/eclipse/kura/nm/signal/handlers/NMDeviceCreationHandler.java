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

import java.util.Objects;
import java.util.concurrent.BlockingQueue;

import org.eclipse.kura.nm.NMDbusConnector;
import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMDeviceCreationHandler implements DBusSigHandler<NetworkManager.DeviceAdded> {

    private static final Logger logger = LoggerFactory.getLogger(NMDeviceCreationHandler.class);
    
    private final NMDbusConnector nm;
    private final String deviceToCreate;
    private final BlockingQueue<Device> createdDeviceQueue;

    public NMDeviceCreationHandler(NMDbusConnector connector, String deviceToCreate, BlockingQueue<Device> createdDeviceQueue) {
        this.nm = Objects.requireNonNull(connector);
        this.deviceToCreate = Objects.requireNonNull(deviceToCreate);
        this.createdDeviceQueue = Objects.requireNonNull(createdDeviceQueue);
    }
    
    @Override
    public void handle(NetworkManager.DeviceAdded s) {
        try {
            logger.debug("New network device connected at {}", s.getDevicePath());
            String deviceId = this.nm.getInterfaceIdByDBusPath(s.getDevicePath().getPath());
            if (deviceToCreate.equals(deviceId)) {
                String createdDevicePath = s.getDevicePath().getPath();
                logger.info("Created device \"{}\" at path \"{}\"", deviceId, createdDevicePath);
                Device createdDevice = nm.getDbusConnection().getRemoteObject("org.freedesktop.NetworkManager", createdDevicePath, Device.class);
                createdDeviceQueue.add(createdDevice);
            }
        } catch (DBusException | IllegalStateException e) {
            logger.error("Failed to handle DeviceAdded event for device: {}. Caused by:", s.getDevicePath(), e);
        }
    }
}
