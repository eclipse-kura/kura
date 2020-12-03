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
package org.eclipse.kura.cloud.app.command;

import java.util.TimerTask;

/**
 * Just a simple TimerTask that interrupts the specified thread when run.
 */
class InterruptTimerTask extends TimerTask {

    private final Thread thread;

    public InterruptTimerTask(Thread t) {
        this.thread = t;
    }

    @Override
    public void run() {
        this.thread.interrupt();
    }
}