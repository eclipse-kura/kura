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
package org.eclipse.kura.core.watchdog;

public class CriticalServiceImpl {

    private final String name;
    private final long timeout;
    private long updated;

    /**
     *
     * @param name
     * @param timeout
     *            timeout for reporting interval in seconds
     */
    public CriticalServiceImpl(String name, long timeout) {
        this.name = name;
        this.timeout = timeout;
        this.updated = System.currentTimeMillis();
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
