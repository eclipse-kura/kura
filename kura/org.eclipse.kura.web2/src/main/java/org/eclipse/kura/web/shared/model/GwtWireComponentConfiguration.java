/**
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

import org.eclipse.kura.annotation.Nullable;

/**
 * The Class GwtWireComponentConfiguration represents a POJO for every Wire
 * Component present in the system
 */
public final class GwtWireComponentConfiguration extends GwtBaseModel implements Serializable {

    /** Serial Version */
    private static final long serialVersionUID = 50782654510063453L;

    /** Driver Associated to it */
    @Nullable
    private String driverPid;

    /** Factory PID */
    private String factoryPid;

    /** The PID of the Wire Component */
    private String pid;

    /** The Wire Component Type */
    private String type;

    public String getDriverPid() {
        return this.driverPid;
    }

    public String getFactoryPid() {
        return this.factoryPid;
    }

    public String getPid() {
        return this.pid;
    }

    public String getType() {
        return this.type;
    }

    public void setDriverPid(final String driverPid) {
        this.driverPid = driverPid;
    }

    public void setFactoryPid(final String factoryPid) {
        this.factoryPid = factoryPid;
    }

    public void setPid(final String pid) {
        this.pid = pid;
    }

    public void setType(final String type) {
        this.type = type;
    }

}
