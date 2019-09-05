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
        return new BiConsumer<T, U>() {

            @Override
            public native void accept(T t, U u)
            /*-{
                var firstAdapter = this.@org.eclipse.kura.web2.ext.internal.BiConsumerAdapter::firstAdapter
                var secondAdapter = this.@org.eclipse.kura.web2.ext.internal.BiConsumerAdapter::secondAdapter
                var first = firstAdapter.@org.eclipse.kura.web2.ext.internal.Adapter::adaptNullable(Ljava/lang/Object;)(t)
                var second = secondAdapter.@org.eclipse.kura.web2.ext.internal.Adapter::adaptNullable(Ljava/lang/Object;)(u)
                jsConsumer(first, second)
            }-*/;
        };
    }

}
