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

public final class BlinkEffect extends JavaScriptObject {

    protected BlinkEffect() {
    }

    public static native BlinkEffect create(WireComposer composer)
    /*-{
        return new parent.window.BlinkEffect(composer)
    }-*/;

    public native void setEnabled(boolean enabled)
    /*-{
        this.setEnabled(enabled)
    }-*/;
}
