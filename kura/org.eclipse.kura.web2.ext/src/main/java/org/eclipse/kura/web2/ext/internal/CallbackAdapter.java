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
