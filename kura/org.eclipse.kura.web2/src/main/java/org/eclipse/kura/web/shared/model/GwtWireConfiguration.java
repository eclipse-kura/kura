/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
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
    private int emitterPort;
    private int receiverPort;

    public String getEmitterPid() {
        return this.emitterPid;
    }

    public String getReceiverPid() {
        return this.receiverPid;
    }

    public void setEmitterPid(final String emitterPid) {
        this.emitterPid = emitterPid;
    }

    public void setReceiverPid(final String receiverPid) {
        this.receiverPid = receiverPid;
    }

    public int getEmitterPort() {
        return this.emitterPort;
    }

    public void setEmitterPort(int emitterPort) {
        this.emitterPort = emitterPort;
    }

    public int getReceiverPort() {
        return this.receiverPort;
    }

    public void setReceiverPort(int receiverPort) {
        this.receiverPort = receiverPort;
    }

}
