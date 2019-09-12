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

public class IdentityAdapter<T> implements Adapter<T> {

    @Override
    public native JavaScriptObject adaptNonNull(final T value)
    /*-{
        return value
    }-*/;

    @Override
    public native T adaptNonNull(JavaScriptObject value)
    /*-{
         return value
    }-*/;

}
