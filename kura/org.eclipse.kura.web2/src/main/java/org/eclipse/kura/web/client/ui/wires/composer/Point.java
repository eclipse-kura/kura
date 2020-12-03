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
