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
