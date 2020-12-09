/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.web.client.ui.wires.composer;

import org.eclipse.kura.web.shared.model.GwtWireConfiguration;

import com.google.gwt.core.client.JavaScriptObject;

public final class Wire extends JavaScriptObject {

    protected Wire() {
    }

    public static native Wire create()
    /*-{
        return {
            emitterPort: 0,
            receiverPort: 0
        }
    }-*/;

    public native String getEmitterPid()
    /*-{
        return this.emitterPid
    }-*/;

    public native String getReceiverPid()
    /*-{
        return this.receiverPid
    }-*/;

    public native int getEmitterPort()
    /*-{
        return this.emitterPort
    }-*/;

    public native int getReceiverPort()
    /*-{
        return this.receiverPort
    }-*/;

    public native void setEmitterPid(String emitterPid)
    /*-{
        this.emitterPid = emitterPid
    }-*/;

    public native void setReceiverPid(String receiverPid)
    /*-{
        this.receiverPid = receiverPid
    }-*/;

    public native void setEmitterPort(int emitterPort)
    /*-{
        this.emitterPort = emitterPort
    }-*/;

    public native void setReceiverPort(int receiverPort)
    /*-{
        this.receiverPort = receiverPort
    }-*/;

    public static Wire fromGwt(GwtWireConfiguration config) {

        final Wire result = Wire.create();

        result.setEmitterPid(config.getEmitterPid());
        result.setEmitterPort(config.getEmitterPort());
        result.setReceiverPid(config.getReceiverPid());
        result.setReceiverPort(config.getReceiverPort());

        return result;
    }

    public GwtWireConfiguration toGwt() {
        final GwtWireConfiguration result = new GwtWireConfiguration();

        result.setEmitterPid(getEmitterPid());
        result.setEmitterPort(getEmitterPort());
        result.setReceiverPid(getReceiverPid());
        result.setReceiverPort(getReceiverPort());

        return result;
    }
}
