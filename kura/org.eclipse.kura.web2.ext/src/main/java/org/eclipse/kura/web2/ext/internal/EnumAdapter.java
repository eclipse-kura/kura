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

public class EnumAdapter<T extends Enum<T>> implements Adapter<T> {

    private final Class<T> type;

    public EnumAdapter(final Class<T> type) {
        this.type = type;
    }

    @Override
    public JavaScriptObject adaptNonNull(final T value) {
        return JsObject.fromString(value.name());
    }

    @Override
    public T adaptNonNull(JavaScriptObject value) {
        final JsObject obj = value.cast();
        return Enum.valueOf(type, obj.asString());
    }

}
