/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.status.LedManager;
import org.eclipse.kura.status.CloudConnectionStatusEnum;

public class BlinkStatusRunnable implements Runnable {

    private final LedManager ledManager;

    public BlinkStatusRunnable(LedManager ledManager) {
        this.ledManager = ledManager;
    }

    @Override
    public void run() {
        while (true) {
            try {
                this.ledManager.writeLed(true);
                Thread.sleep(CloudConnectionStatusEnum.SLOW_BLINKING_ON_TIME);
                this.ledManager.writeLed(false);
                Thread.sleep(CloudConnectionStatusEnum.SLOW_BLINKING_OFF_TIME);
            } catch (InterruptedException | KuraException ex) {
                break;
            }
        }
    }
}
