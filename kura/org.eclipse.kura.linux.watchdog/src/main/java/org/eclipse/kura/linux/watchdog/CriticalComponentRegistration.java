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
package org.eclipse.kura.linux.watchdog;

import org.eclipse.kura.watchdog.CriticalComponent;

public class CriticalComponentRegistration {

    private final CriticalComponent criticalComponent;
    private long updated;

    public CriticalComponentRegistration(CriticalComponent criticalComponent) {
        this.criticalComponent = criticalComponent;
        this.updated = System.currentTimeMillis();
    }

    public boolean isTimedOut() {
        long now = System.currentTimeMillis();
        return this.criticalComponent.getCriticalComponentTimeout() < now - this.updated;
    }

    public void update() {
        this.updated = System.currentTimeMillis();
    }

    public String getCriticalComponentName() {
        return this.criticalComponent.getCriticalComponentName();
    }

    public int getCriticalComponentTimeout() {
        return this.criticalComponent.getCriticalComponentTimeout();
    }

    public CriticalComponent getCriticalComponent() {
        return this.criticalComponent;
    }
}
