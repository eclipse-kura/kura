/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web2.ext.internal;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

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

    public final native JavaScriptObject call(final String key, final JsArray<JavaScriptObject> values)
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

    public static final native JavaScriptObject call(final JavaScriptObject func)
    /*-{
         return func()
    }-*/;

    public static final native JavaScriptObject call(final JavaScriptObject func, final JavaScriptObject value)
    /*-{
         return func(value)
    }-*/;

    public static final native JavaScriptObject call(final JavaScriptObject func,
            final JsArray<JavaScriptObject> values)
    /*-{
         return func.apply(null, values)
    }-*/;

    @SafeVarargs
    public static final <T extends JavaScriptObject> JsArray<T> toArray(T... args) {
        final JsArray<T> result = JavaScriptObject.createArray().cast();

        for (T val : args) {
            result.push(val);
        }

        return result;
    }
}
