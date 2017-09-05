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

public class OnOffStatusRunnable implements Runnable {

    private final LedManager ledManager;
    private boolean enabled = false;

    public OnOffStatusRunnable(LedManager ledManager, boolean enabled) {
        this.ledManager = ledManager;
        this.enabled = enabled;
    }

    @Override
    public void run() {
        while (true) {
            try {
                this.ledManager.writeLed(this.enabled);
                Thread.sleep(CloudConnectionStatusEnum.PERIODIC_STATUS_CHECK_DELAY);
            } catch (InterruptedException | KuraException e) {
                break;
            }
        }
    }

}
