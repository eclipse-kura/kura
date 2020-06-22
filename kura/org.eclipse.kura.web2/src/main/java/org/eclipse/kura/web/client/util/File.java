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

public class File extends JavaScriptObject {

    protected File() {
    }

    public final native String getName()
    /*-{
        return this.name
    }-*/;

    public final native String getType()
    /*-{
        return this.type
    }-*/;
}
