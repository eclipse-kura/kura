/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.status.runnables;

import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogStatusRunnable implements StatusRunnable {

    private static final Logger logger = LoggerFactory.getLogger(LogStatusRunnable.class);

    private final CloudConnectionStatusEnum status;

    public LogStatusRunnable(CloudConnectionStatusEnum status) {
        this.status = status;
    }

    @Override
    public void run() {
        switch (this.status) {
        case ON:
            logger.info("Notification LED on");
            break;
        case SLOW_BLINKING:
            logger.info("Notification LED slow blinking");
            break;
        case FAST_BLINKING:
            logger.info("Notification LED fast blinking");
            break;
        case HEARTBEAT:
            logger.info("Notification LED heartbeating");
            break;
        default:
            logger.info("Notification LED off");
        }
    }

    @Override
    public void stopRunnable() {
        return;
    }

}
