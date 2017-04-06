/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     Eurotech
 *     Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

/**
 * The Class GwtWireConfiguration represents a POJO for every Wire Configuration
 * present in the system
 */
public final class GwtWireConfiguration extends GwtBaseModel implements Serializable {

    /** Serial Version */
    private static final long serialVersionUID = 3573302888192836657L;

    private String emitterPid;
    private String receiverPid;
    private String filter;

    public String getEmitterPid() {
        return this.emitterPid;
    }

    public String getReceiverPid() {
        return this.receiverPid;
    }

    public String getFilter() {
        return this.filter;
    }

    public void setEmitterPid(final String emitterPid) {
        this.emitterPid = emitterPid;
    }

    public void setReceiverPid(final String receiverPid) {
        this.receiverPid = receiverPid;
    }

    public void setFilter(final String filter) {
        this.filter = filter;
    }

}
