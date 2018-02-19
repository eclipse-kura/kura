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

import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.core.client.JavaScriptObject;

public final class PortNames extends JavaScriptObject {

    protected PortNames() {
    }

    public native String getPortName(int index)
    /*-{
        return this["" + index]
    }-*/;

    public native void setPortName(String name, int index)
    /*-{
        if (name) {
            this["" + index] = name
        } else {
            delete this["" + index]
        }
    }-*/;

    public static native PortNames create()
    /*-{
        return {}
    }-*/;

    public static PortNames fromMap(Map<Integer, String> portNames) {
        final PortNames result = PortNames.create();
        if (portNames == null) {
            return result;
        }
        for (Entry<Integer, String> e : portNames.entrySet()) {
            result.setPortName(e.getValue(), e.getKey());
        }
        return result;
    }
}
