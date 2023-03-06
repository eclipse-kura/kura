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

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMConfigurationEnforcementHandler implements DBusSigHandler<Device.StateChanged> {

    private static final Logger logger = LoggerFactory.getLogger(NMConfigurationEnforcementHandler.class);

    @Override
    public void handle(Device.StateChanged s) {

        NMDeviceState oldState = NMDeviceState.fromUInt32(s.getOldState());
        NMDeviceState newState = NMDeviceState.fromUInt32(s.getNewState());

        logger.debug("Device state change detected: {} -> {}, for {}", oldState, newState, s.getPath());
        if ((oldState != NMDeviceState.NM_DEVICE_STATE_FAILED && newState == NMDeviceState.NM_DEVICE_STATE_DISCONNECTED)
                || newState == NMDeviceState.NM_DEVICE_STATE_CONFIG) {

            try {
                NMDbusConnector nm = NMDbusConnector.getInstance();
                logger.info("Network change detected on interface {}. Roll-back to cached configuration", s.getPath());
                nm.apply();
            } catch (DBusException e) {
                logger.error("Failed to handle network configuration change event for device: {}. Caused by:",
                        s.getPath(), e);
            }
        }
    }
}