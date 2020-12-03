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

import java.util.function.BiConsumer;

import com.google.gwt.core.client.JavaScriptObject;

public class BiConsumerAdapter<T, U> implements Adapter<BiConsumer<T, U>> {

    private final Adapter<T> firstAdapter;
    private final Adapter<U> secondAdapter;

    public BiConsumerAdapter(final Adapter<T> firstAdapter, final Adapter<U> secondAdapter) {
        this.firstAdapter = firstAdapter;
        this.secondAdapter = secondAdapter;
    }

    @Override
    public native JavaScriptObject adaptNonNull(final BiConsumer<T, U> consumer)
    /*-{
        var self = this
        return function (t, u) {
            var firstAdapter = self.@org.eclipse.kura.web2.ext.internal.BiConsumerAdapter::firstAdapter
            var secondAdapter = self.@org.eclipse.kura.web2.ext.internal.BiConsumerAdapter::secondAdapter
            var first = firstAdapter.@org.eclipse.kura.web2.ext.internal.Adapter::adaptNullable(Lcom/google/gwt/core/client/JavaScriptObject;)(t)
            var second = secondAdapter.@org.eclipse.kura.web2.ext.internal.Adapter::adaptNullable(Lcom/google/gwt/core/client/JavaScriptObject;)(u)
            consumer.@java.util.function.BiConsumer::accept(Ljava/lang/Object;Ljava/lang/Object;)(first, second)
        }
    }-*/;

    @Override
    public BiConsumer<T, U> adaptNonNull(final JavaScriptObject jsConsumer) {

        return (t, u) -> JsObject.call(jsConsumer,
                JsObject.toArray(firstAdapter.adaptNullable(t), secondAdapter.adaptNullable(u)));

    }

}
