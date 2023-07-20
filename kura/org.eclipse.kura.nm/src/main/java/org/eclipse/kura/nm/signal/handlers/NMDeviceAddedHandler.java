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

import org.eclipse.kura.nm.NMDbusConnector;
import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMDeviceAddedHandler implements DBusSigHandler<NetworkManager.DeviceAdded> {

    private static final Logger logger = LoggerFactory.getLogger(NMDeviceAddedHandler.class);
    private final NMDbusConnector nm;

    public NMDeviceAddedHandler(NMDbusConnector connector) {
        this.nm = Objects.requireNonNull(connector);
    }

    @Override
    public void handle(NetworkManager.DeviceAdded s) {
        try {
            logger.info("New network device connected at {}", s.getDevicePath());
            String deviceId = this.nm.getInterfaceIdByDBusPath(s.getDevicePath().getPath());
            this.nm.apply(deviceId);
        } catch (DBusException e) {
            logger.error("Failed to handle DeviceAdded event for device: {}. Caused by:", s.getDevicePath(), e);
        }
    }
}