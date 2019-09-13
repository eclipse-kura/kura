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

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;

public class CallbackAdapter<T, U> implements Adapter<Callback<T, U>> {

    private static final String ON_FAILURE = "onFailure";
    private static final String ON_SUCCESS = "onSuccess";

    private final Adapter<T> successAdapter;
    private final Adapter<U> failureAdapter;

    public CallbackAdapter(final Adapter<T> successAdapter, final Adapter<U> failureAdapter) {
        this.successAdapter = successAdapter;
        this.failureAdapter = failureAdapter;
    }

    @Override
    public JavaScriptObject adaptNonNull(final Callback<T, U> callback) {
        final JsObject result = JavaScriptObject.createObject().cast();

        result.set(ON_SUCCESS, new ConsumerAdapter<>(successAdapter).adaptNullable(callback::onSuccess));
        result.set(ON_FAILURE, new ConsumerAdapter<>(failureAdapter).adaptNullable(callback::onFailure));

        return result;
    }

    @Override
    public Callback<T, U> adaptNonNull(final JavaScriptObject callback) {
        return new Callback<T, U>() {

            @Override
            public void onSuccess(T result) {
                final JsObject obj = callback.cast();
                obj.call(ON_SUCCESS, successAdapter.adaptNullable(result));
            }

            @Override
            public void onFailure(U reason) {
                final JsObject obj = callback.cast();
                obj.call(ON_FAILURE, failureAdapter.adaptNullable(reason));
            }
        };
    }

}
