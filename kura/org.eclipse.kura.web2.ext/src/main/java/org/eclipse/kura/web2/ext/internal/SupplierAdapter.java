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

import java.util.function.Supplier;

import com.google.gwt.core.client.JavaScriptObject;

public class SupplierAdapter<T> implements Adapter<Supplier<T>> {

    private final Adapter<T> adapter;

    public SupplierAdapter(final Adapter<T> adapter) {
        this.adapter = adapter;
    }

    @Override
    public native JavaScriptObject adaptNonNull(final Supplier<T> value)
    /*-{
        var self = this
        return function () {
            var adapter = self.@org.eclipse.kura.web2.ext.internal.SupplierAdapter::adapter
            var v = value.@java.util.function.Supplier::get()()
            return adapter.@org.eclipse.kura.web2.ext.internal.Adapter::adaptNullable(Ljava/lang/Object;)(v)
        }
    }-*/;

    @Override
    public Supplier<T> adaptNonNull(JavaScriptObject jsSupplier) {

        return () -> adapter.adaptNullable(JsObject.call(jsSupplier));

    }
}
