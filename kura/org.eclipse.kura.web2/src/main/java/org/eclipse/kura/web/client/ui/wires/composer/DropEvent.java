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

public final class DropEvent extends JavaScriptObject {

    protected DropEvent() {
    }

    public native String getAttachment()
    /*-{
        return this.getAttachment()
    }-*/;

    public native void complete(WireComponent component)
    /*-{
        this.complete(component)
    }-*/;

    public native void cancel()
    /*-{
        this.cancel()
    }-*/;
}
