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
