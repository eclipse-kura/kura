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
package org.eclipse.kura.linux.watchdog;

import org.eclipse.kura.watchdog.CriticalComponent;

public class CriticalComponentRegistration {

    private final CriticalComponent criticalComponent;
    private long updated;

    public CriticalComponentRegistration(CriticalComponent criticalComponent) {
        this.criticalComponent = criticalComponent;
        this.updated = System.nanoTime();
    }

    public boolean isTimedOut() {
        long now = System.nanoTime();
        return this.criticalComponent.getCriticalComponentTimeout() * 1000000L < now - this.updated;
    }

    public void update() {
        this.updated = System.nanoTime();
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
