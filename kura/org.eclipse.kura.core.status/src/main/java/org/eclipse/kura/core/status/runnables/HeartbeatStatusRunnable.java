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

public class HeartbeatStatusRunnable implements StatusRunnable {

    private final LedManager ledManager;

    private boolean enabled;

    public HeartbeatStatusRunnable(LedManager ledManager) {
        this.ledManager = ledManager;
        this.enabled = true;
    }

    @Override
    public void run() {
        while (this.enabled) {
            try {
                this.ledManager.writeLed(true);
                Thread.sleep(CloudConnectionStatusEnum.HEARTBEAT_SYSTOLE_DURATION);
                this.ledManager.writeLed(false);
                Thread.sleep(CloudConnectionStatusEnum.HEARTBEAT_SYSTOLE_DURATION);
                this.ledManager.writeLed(true);
                Thread.sleep(CloudConnectionStatusEnum.HEARTBEAT_DIASTOLE_DURATION);
                this.ledManager.writeLed(false);
                Thread.sleep(CloudConnectionStatusEnum.HEARTBEAT_PAUSE_DURATION);
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
