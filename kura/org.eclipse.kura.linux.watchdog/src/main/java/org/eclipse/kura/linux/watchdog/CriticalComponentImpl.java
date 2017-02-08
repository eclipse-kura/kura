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
package org.eclipse.kura.linux.watchdog;

import org.eclipse.kura.watchdog.CriticalComponent;

public class CriticalComponentImpl {

    private CriticalComponent criticalComponent;
    private final String name;
    private final long timeout;
    private long updated;

    /**
     *
     * @param criticalComponent
     */
    public CriticalComponentImpl(CriticalComponent criticalComponent) {
        this.criticalComponent = criticalComponent;
        this.name = criticalComponent.getCriticalComponentName();
        this.timeout = criticalComponent.getCriticalComponentTimeout();
        this.updated = System.currentTimeMillis();
    }

    public CriticalComponent getCriticalComponent() {
        return criticalComponent;
    }

    public void setCriticalComponent(CriticalComponent criticalComponent) {
        this.criticalComponent = criticalComponent;
    }

    public String getName() {
        return this.name;
    }

    public long getTimeout() {
        return this.timeout;
    }

    public boolean isTimedOut() {
        long current = System.currentTimeMillis();
        return this.timeout < current - this.updated;
    }

    public void update() {
        this.updated = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "Service Name:  " + this.name + ", Timeout(ms):  " + this.timeout;
    }
}
