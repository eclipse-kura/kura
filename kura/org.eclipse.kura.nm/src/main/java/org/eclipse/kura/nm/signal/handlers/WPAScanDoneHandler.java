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

import java.util.concurrent.CountDownLatch;

import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.w1.wpa_supplicant1.Interface;

public class WPAScanDoneHandler implements DBusSigHandler<Interface.ScanDone> {

    private static final Logger logger = LoggerFactory.getLogger(WPAScanDoneHandler.class);

    private final CountDownLatch latch;
    private final String path;

    public WPAScanDoneHandler(CountDownLatch latch, String path) {
        this.latch = latch;
        this.path = path;
    }

    @Override
    public void handle(Interface.ScanDone s) {

        logger.trace("AP scan done signal received for {}", s.getPath());
        if (s.getPath().equals(this.path)) {
            logger.debug("Notify waiting thread");
            this.latch.countDown();
        }
    }
}
