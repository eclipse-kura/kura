/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.configuration.api;

public class PidAndFactoryPid {

    private final String pid;
    private final String factoryPid;

    public PidAndFactoryPid(String pid) {
        this(pid, null);
    }

    public PidAndFactoryPid(String pid, String factoryPid) {
        this.pid = pid;
        this.factoryPid = factoryPid;
    }

    public String getPid() {
        return pid;
    }

    public String getFactoryPid() {
        return factoryPid;
    }

}
