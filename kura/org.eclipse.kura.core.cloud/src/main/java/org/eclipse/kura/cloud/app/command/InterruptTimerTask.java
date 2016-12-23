/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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