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

public final class Point extends JavaScriptObject {

    protected Point() {
    }

    public static native Point create(double x, double y)
    /*-{
        return {x: x, y: y}
    }-*/;

    public native double getX()
    /*-{
        return this.x
    }-*/;

    public native int getY()
    /*-{
        return this.y
    }-*/;

    public native void setX(double x)
    /*-{
        this.x = x;
    }-*/;

    public native int getY(double y)
    /*-{
        this.y = y
    }-*/;
}
