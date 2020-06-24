/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.web.client.util;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class FormData extends JavaScriptObject {

    protected FormData() {
    }

    public static native FormData create()
    /*-{
        return new FormData()
    }-*/;

    public final native void append(String key, String value)
    /*-{
        this.append(key, value)
    }-*/;

    public final native void append(String key, File file)
    /*-{
        this.append(key, file)
    }-*/;

    public final native void submit(String relativePath, AsyncCallback<Void> callback)
    /*-{
        var request = new XMLHttpRequest()
        request.open("POST", relativePath, true)
        request.onload = function(event) {
            if (request.status == 200) {
              callback.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;)(null)
            } else {
              var exception = @java.lang.Exception::new(Ljava/lang/String;)("Upload failed, status: " + request.status)
              callback.@com.google.gwt.user.client.rpc.AsyncCallback::onFailure(Ljava/lang/Throwable;)(exception)
            }
        }
    
        request.send(this)
    }-*/;
}
