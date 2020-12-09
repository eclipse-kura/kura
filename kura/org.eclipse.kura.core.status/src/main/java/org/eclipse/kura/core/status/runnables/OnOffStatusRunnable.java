/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.status.runnables;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.status.LedManager;
import org.eclipse.kura.status.CloudConnectionStatusEnum;

public class OnOffStatusRunnable implements StatusRunnable {

    private final LedManager ledManager;
    private boolean ledEnabled = false;

    private boolean enabled;

    public OnOffStatusRunnable(LedManager ledManager, boolean ledEnabled) {
        this.ledManager = ledManager;
        this.ledEnabled = ledEnabled;
        this.enabled = true;
    }

    @Override
    public void run() {
        while (this.enabled) {
            try {
                this.ledManager.writeLed(this.ledEnabled);
                Thread.sleep(CloudConnectionStatusEnum.PERIODIC_STATUS_CHECK_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                this.enabled = false;
            } catch (KuraException e) {
                this.enabled = false;
            }
        }
    }

    @Override
    public void stopRunnable() {
        this.enabled = false;
    }

}
