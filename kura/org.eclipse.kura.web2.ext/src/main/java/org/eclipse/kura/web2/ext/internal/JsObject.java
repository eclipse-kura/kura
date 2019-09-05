/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web2.ext.internal;

import com.google.gwt.core.client.JavaScriptObject;

public final class JsObject extends JavaScriptObject {

    protected JsObject() {
    }

    public final native <T extends JavaScriptObject> T get(final String key)
    /*-{
        return this[key]
    }-*/;

    public final native void set(final String key, final JavaScriptObject object)
    /*-{
        this[key] = object
    }-*/;

    public final native JavaScriptObject call(final String key)
    /*-{
        return this[key]()
    }-*/;

    public final native JavaScriptObject call(final String key, final JavaScriptObject value)
    /*-{
        return this[key](value)
    }-*/;

    public final native JavaScriptObject call(final String key, final JavaScriptObject[] values)
    /*-{
        return this[key].apply(this, values)
    }-*/;

    public final native String asString()
    /*-{
        return this
    }-*/;

    public static native JsObject fromString(final String string)
    /*-{
        return string
    }-*/;
}
