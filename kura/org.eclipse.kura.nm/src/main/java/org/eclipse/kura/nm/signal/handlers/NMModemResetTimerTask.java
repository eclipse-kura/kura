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
import java.util.TimerTask;

import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.modemmanager1.Modem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMModemResetTimerTask extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(NMModemResetTimerTask.class);

    private final Modem modem;
    private boolean expired = false;

    public NMModemResetTimerTask(Modem modem) {
        this.modem = Objects.requireNonNull(modem);
    }

    public boolean expired() {
        return this.expired;
    }

    @Override
    public void run() {
        logger.info("Modem reset timer expired. Resetting modem {} ...", this.modem.getObjectPath());
        this.expired = true;
        try {
            this.modem.Reset();
        } catch (DBusExecutionException e) {
            logger.warn("Could not perform modem reset for {} because: ", this.modem.getObjectPath(), e);
        }
    }

    public String getModemDbusPath() {
        return this.modem.getObjectPath();
    }

}
