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
package org.eclipse.kura.nm;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.nm.signal.handlers.NMModemSignalHandler;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModemTaskManager {

    private static final Logger logger = LoggerFactory.getLogger(ModemTaskManager.class);

    private final DBusConnection dbusConnection;
    private final Map<String, NMModemSignalHandler> modemTaskHandlers = new HashMap<>();

    public ModemTaskManager(DBusConnection dbusConnection) {
        this.dbusConnection = dbusConnection;
    }

    protected void modemTaskHandlerEnable(String deviceId, Device device, NetworkProperties properties)
            throws DBusException {
        int resetDelayMinutes = properties.get(Integer.class, "net.interface.%s.config.resetTimeout", deviceId);
        boolean autoconnect = properties.get(Boolean.class, "net.interface.%s.config.persist", deviceId);
        if (isModemTaskAlreadyActivated(deviceId, resetDelayMinutes, autoconnect)) {
            return;
        }
        modemTaskHandlerDisable(deviceId);
        if (autoconnect || resetDelayMinutes > 0) {

            logger.info("Modem {} activated. Starting monitoring task...", deviceId);
            ModemTaskScheduler connectionScheduler = new ModemTaskScheduler(deviceId, device, properties);
            if (autoconnect) {
                connectionScheduler.scheduleConnection();
            }
            if (resetDelayMinutes > 0) {
                connectionScheduler.scheduleReset();
            }
            NMModemSignalHandler modemConnectionHandler = new NMModemSignalHandler(connectionScheduler);

            this.modemTaskHandlers.put(deviceId, modemConnectionHandler);

            this.dbusConnection.addSigHandler(org.freedesktop.networkmanager.Device.StateChanged.class,
                    modemConnectionHandler);
        }
    }

    private boolean isModemTaskAlreadyActivated(String deviceId, int resetDelayMinutes, boolean autoconnect) {
        return isModemTaskHandlerPresent(deviceId) && (isModemTaskHandlerResetActivated(deviceId, resetDelayMinutes)
                || isModemTaskHandlerAutoconnectActivated(deviceId, autoconnect));
    }

    protected boolean isModemTaskHandlerPresent(String deviceId) {
        return this.modemTaskHandlers.containsKey(deviceId);
    }

    private boolean isModemTaskHandlerResetActivated(String deviceId, int resetDelayMinutes) {
        return resetDelayMinutes > 0
                && this.modemTaskHandlers.get(deviceId).getModemConnectionScheduler().isResetScheduled();
    }

    private boolean isModemTaskHandlerAutoconnectActivated(String deviceId, boolean autoconnect) {
        return autoconnect
                && this.modemTaskHandlers.get(deviceId).getModemConnectionScheduler().isConnectionScheduled();
    }

    protected void modemTaskHandlerDisable(String deviceId) {
        if (this.modemTaskHandlers.containsKey(deviceId)) {
            NMModemSignalHandler handler = this.modemTaskHandlers.get(deviceId);
            handler.getModemConnectionScheduler().cancelAndShutdown();
            try {
                this.dbusConnection.removeSigHandler(org.freedesktop.networkmanager.Device.StateChanged.class, handler);
            } catch (DBusException e) {
                logger.warn("Couldn't remove signal handler for: {}. Caused by:", deviceId, e);
            }
            this.modemTaskHandlers.remove(deviceId);
        }
    }

    protected void modemTaskHandlerDisable() {
        for (String deviceId : this.modemTaskHandlers.keySet()) {
            modemTaskHandlerDisable(deviceId);
        }
        this.modemTaskHandlers.clear();
    }

}
