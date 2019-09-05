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
          if (!window.top.extensionRegistry) {
                window.top.extensionRegistry = {
                    extensions: {},
                    consumers: []
                }
          }
          return window.top.extensionRegistry
    }-*/;

    public final native void registerExtension(final JavaScriptObject extension)
    /*-{
          this.extensions[extension.id] = extension
          for (var i = 0; i < this.consumers.length; i++) {
              (this.consumers[i])(extension)
          }
    }-*/;

    public final native void unregisterExtension(final JavaScriptObject extension)
    /*-{
          delete this.extensions[extension.id]
    }-*/;

    public final native void addExtensionConsumer(final Consumer<JavaScriptObject> consumer)
    /*-{
        this.consumers.push(function (ex) {
            consumer.@java.util.function.Consumer::accept(Ljava/lang/Object;)(ex)
        })
        for (var p in this.extensions) {
            consumer.@java.util.function.Consumer::accept(Ljava/lang/Object;)(this.extensions[p])
        }
    }-*/;

    public final native JsArray<JavaScriptObject> getExtensions()
    /*-{
        var result = []
        for (var p in this.extensions) {
            result.push(this.extensions[p])
        }
        return result
    }-*/;

}
