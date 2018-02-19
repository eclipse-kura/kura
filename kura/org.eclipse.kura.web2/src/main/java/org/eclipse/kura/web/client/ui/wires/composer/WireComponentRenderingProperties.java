/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
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
