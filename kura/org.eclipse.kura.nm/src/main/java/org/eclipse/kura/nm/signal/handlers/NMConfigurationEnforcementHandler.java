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
import org.eclipse.kura.nm.enums.NMDeviceState;
import org.eclipse.kura.nm.enums.NMDeviceStateReason;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMConfigurationEnforcementHandler implements DBusSigHandler<Device.StateChanged> {

    private static final Logger logger = LoggerFactory.getLogger(NMConfigurationEnforcementHandler.class);
    private final NMDbusConnector nm;

    public NMConfigurationEnforcementHandler(NMDbusConnector nmDbusConnector) {
        this.nm = Objects.requireNonNull(nmDbusConnector);
    }

    @Override
    public void handle(Device.StateChanged s) {

        NMDeviceState oldState = NMDeviceState.fromUInt32(s.getOldState());
        NMDeviceState newState = NMDeviceState.fromUInt32(s.getNewState());
        NMDeviceStateReason reason = NMDeviceStateReason.fromUInt32(s.getReason());

        logger.debug("Device state change detected: {} -> {} (reason: {}), for device {}", oldState, newState, reason,
                s.getPath());

        boolean deviceDisconnectedBecauseOfConfigurationEvent = oldState != NMDeviceState.NM_DEVICE_STATE_FAILED
                && oldState != NMDeviceState.NM_DEVICE_STATE_UNAVAILABLE
                && newState == NMDeviceState.NM_DEVICE_STATE_DISCONNECTED;
        boolean deviceIsConnectingToANewNetwork = newState == NMDeviceState.NM_DEVICE_STATE_CONFIG;

        if (deviceIsConnectingToANewNetwork || deviceDisconnectedBecauseOfConfigurationEvent) {
            try {
                logger.info("Network change detected on interface {}. Roll-back to cached configuration", s.getPath());
                String deviceId = this.nm.getInterfaceIdByDBusPath(s.getPath());
                this.nm.apply(deviceId);
            } catch (DBusException e) {
                logger.error("Failed to handle network configuration change event for device: {}. Caused by:",
                        s.getPath(), e);
            }
        }
    }
}