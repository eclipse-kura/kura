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

public class BooleanAdapter implements Adapter<Boolean> {

    @Override
    public native JavaScriptObject adaptNullable(final Boolean value)
    /*-{
        if (value === null) {
            return null
        }
        return value.@java.lang.Boolean::booleanValue()()
    }-*/;

    @Override
    public native Boolean adaptNullable(final JavaScriptObject value)
    /*-{
         if (value == null) {
             return null
         }
         return @java.lang.Boolean::valueOf(Z)(value)
    }-*/;

    @Override
    public JavaScriptObject adaptNonNull(final Boolean value) {
        return adaptNullable(value);
    }

    @Override
    public Boolean adaptNonNull(JavaScriptObject value) {
        return adaptNullable(value);
    }

}
