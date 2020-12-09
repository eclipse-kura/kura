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

import com.google.gwt.core.client.JavaScriptObject;

public final class WireComponentRenderingProperties extends JavaScriptObject {

    protected WireComponentRenderingProperties() {
    }

    public static native WireComponentRenderingProperties create()
    /*-{
        return {}
    }-*/;

    public native Point getPosition()
    /*-{
        return this.position
    }-*/;

    public native void setPosition(Point position)
    /*-{
        this.position = position
    }-*/;

    public native PortNames getInputPortNames()
    /*-{
        return this.inputPortNames
    }-*/;

    public native void setInputPortNames(PortNames portNames)
    /*-{
        this.inputPortNames = portNames
    }-*/;

    public native PortNames getOutputPortNames()
    /*-{
        return this.outputPortNames
    }-*/;

    public native void setOutputPortNames(PortNames portNames)
    /*-{
        this.outputPortNames = portNames
    }-*/;

}
