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

import java.util.function.Consumer;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public final class JsExtensionRegistry extends JavaScriptObject {

    protected JsExtensionRegistry() {
    }

    public static native JsExtensionRegistry get()
    /*-{
          return window.top.extensionRegistry
    }-*/;

    public final native void registerExtension(final JavaScriptObject extension)
    /*-{
          this.registerExtension(extension)
    }-*/;

    public final native void unregisterExtension(final JavaScriptObject extension)
    /*-{
          this.unregisterExtension(extension)
    }-*/;

    public final native void addExtensionConsumer(final Consumer<JavaScriptObject> consumer)
    /*-{
        this.addExtensionConsumer(function (ex) {
            consumer.@java.util.function.Consumer::accept(Ljava/lang/Object;)(ex)
        })
    }-*/;

    public final native JsArray<JavaScriptObject> getExtensions()
    /*-{
        return this.getExtensions()
    }-*/;

}
