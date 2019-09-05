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

public class TriConsumerAdapter<T, U, V> implements Adapter<TriConsumer<T, U, V>> {

    private final Adapter<T> firstAdapter;
    private final Adapter<U> secondAdapter;
    private final Adapter<V> thirdAdapter;

    public TriConsumerAdapter(final Adapter<T> firstAdapter, final Adapter<U> secondAdapter,
            final Adapter<V> thirdAdapter) {
        this.firstAdapter = firstAdapter;
        this.secondAdapter = secondAdapter;
        this.thirdAdapter = thirdAdapter;
    }

    @Override
    public native JavaScriptObject adaptNonNull(final TriConsumer<T, U, V> consumer)
    /*-{
        var self = this
        return function (t, u, v) {
            var firstAdapter = self.@org.eclipse.kura.web2.ext.internal.TriConsumerAdapter::firstAdapter
            var secondAdapter = self.@org.eclipse.kura.web2.ext.internal.TriConsumerAdapter::secondAdapter
            var thirdAdapter = self.@org.eclipse.kura.web2.ext.internal.TriConsumerAdapter::thirdAdapter
            var first = firstAdapter.@org.eclipse.kura.web2.ext.internal.Adapter::adaptNullable(Lcom/google/gwt/core/client/JavaScriptObject;)(t)
            var second = secondAdapter.@org.eclipse.kura.web2.ext.internal.Adapter::adaptNullable(Lcom/google/gwt/core/client/JavaScriptObject;)(u)
            var third = thirdAdapter.@org.eclipse.kura.web2.ext.internal.Adapter::adaptNullable(Lcom/google/gwt/core/client/JavaScriptObject;)(v)
            consumer.@org.eclipse.kura.web2.ext.internal.TriConsumer::accept(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)(first, second, third)
        }
    }-*/;

    @Override
    public TriConsumer<T, U, V> adaptNonNull(final JavaScriptObject jsConsumer) {
        return new TriConsumer<T, U, V>() {

            @Override
            public native void accept(T t, U u, V v)
            /*-{
                var firstAdapter = this.@org.eclipse.kura.web2.ext.internal.TriConsumerAdapter::firstAdapter
                var secondAdapter = this.@org.eclipse.kura.web2.ext.internal.TriConsumerAdapter::secondAdapter
                var thirdAdapter = this.@org.eclipse.kura.web2.ext.internal.TriConsumerAdapter::thirdAdapter
                var first = firstAdapter.@org.eclipse.kura.web2.ext.internal.Adapter::adaptNullable(Ljava/lang/Object;)(t)
                var second = secondAdapter.@org.eclipse.kura.web2.ext.internal.Adapter::adaptNullable(Ljava/lang/Object;)(u)
                var third = thirdAdapter.@org.eclipse.kura.web2.ext.internal.Adapter::adaptNullable(Ljava/lang/Object;)(v)
                jsConsumer(first, second, third)
            }-*/;
        };
    }

}
