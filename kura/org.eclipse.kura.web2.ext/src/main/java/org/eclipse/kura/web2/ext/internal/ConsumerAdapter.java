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

import java.util.function.Consumer;

import com.google.gwt.core.client.JavaScriptObject;

public class ConsumerAdapter<T> implements Adapter<Consumer<T>> {

    private final Adapter<T> adapter;

    public ConsumerAdapter(final Adapter<T> adapter) {
        this.adapter = adapter;
    }

    @Override
    public native JavaScriptObject adaptNonNull(final Consumer<T> consumer)
    /*-{
        var self = this
        return function (v) {
            var adapter = self.@org.eclipse.kura.web2.ext.internal.ConsumerAdapter::adapter
            var adapted = adapter.@org.eclipse.kura.web2.ext.internal.Adapter::adaptNullable(Lcom/google/gwt/core/client/JavaScriptObject;)(v)
            consumer.@java.util.function.Consumer::accept(Ljava/lang/Object;)(adapted)
        }
    }-*/;

    @Override
    public Consumer<T> adaptNonNull(final JavaScriptObject jsConsumer) {
        return new Consumer<T>() {

            @Override
            public native void accept(T t)
            /*-{
                var adapter = this.@org.eclipse.kura.web2.ext.internal.ConsumerAdapter::adapter
                var adapted = adapter.@org.eclipse.kura.web2.ext.internal.Adapter::adaptNullable(Ljava/lang/Object;)(t)
                jsConsumer(adapted)
            }-*/;
        };
    }

}
