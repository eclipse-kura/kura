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
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.networkmanager.Settings;
import org.freedesktop.networkmanager.settings.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class NMConnectionChangedHandler implements DBusSigHandler {

    private static final Logger logger = LoggerFactory.getLogger(NMConnectionChangedHandler.class);

    @Override
    public void handle(DBusSignal signal) {
        if (signal instanceof Settings.NewConnection || signal instanceof Settings.ConnectionRemoved
                || signal instanceof Connection.Updated) {

            logger.info("Detected external network change, rollbacking to the cached configuration!");
            try {
                NMDbusConnector nmDbusConnector = NMDbusConnector.getInstance();
                nmDbusConnector.apply();
            } catch (DBusException e) {
                logger.error("Couldn't apply network configuration settings due to: ", e);
            }

        } else {
            throw new IllegalArgumentException("Signal " + signal.getClass().getName() + " not supported");
        }

    }

}
