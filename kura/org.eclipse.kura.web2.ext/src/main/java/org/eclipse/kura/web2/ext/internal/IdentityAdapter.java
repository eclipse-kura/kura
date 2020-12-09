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
