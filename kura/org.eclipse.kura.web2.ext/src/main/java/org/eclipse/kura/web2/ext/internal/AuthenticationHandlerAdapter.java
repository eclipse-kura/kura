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

import org.eclipse.kura.web2.ext.AuthenticationHandler;
import org.eclipse.kura.web2.ext.WidgetFactory;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;

public class AuthenticationHandlerAdapter implements Adapter<AuthenticationHandler> {

    private static final String AUTHENTICATE = "authenticate";
    private static final String GET_LOGIN_DIALOG_ELEMENT = "getLoginDialogElement";
    private static final String GET_NAME = "getName";

    private static final CallbackAdapter<String, String> CALLBACK_ADAPTER = new CallbackAdapter<>(
            new IdentityAdapter<>(), new IdentityAdapter<>());

    @Override
    public JavaScriptObject adaptNonNull(final AuthenticationHandler authenticationHandler) {
        final JsObject obj = JavaScriptObject.createObject().cast();

        obj.set(GET_NAME, new SupplierAdapter<>(new IdentityAdapter<>()).adaptNullable(authenticationHandler::getName));
        obj.set(GET_LOGIN_DIALOG_ELEMENT, new SupplierAdapter<>(new WidgetFactoryAdapter())
                .adaptNullable(authenticationHandler::getLoginDialogElement));
        obj.set(AUTHENTICATE,
                new ConsumerAdapter<>(CALLBACK_ADAPTER).adaptNullable(authenticationHandler::authenticate));

        return obj;
    }

    @Override
    public AuthenticationHandler adaptNonNull(final JavaScriptObject jsAuthenticationHandler) {
        return new AuthenticationHandler() {

            @Override
            public WidgetFactory getLoginDialogElement() {
                final JsObject obj = jsAuthenticationHandler.cast();

                return new WidgetFactoryAdapter().adaptNullable(obj.call(GET_LOGIN_DIALOG_ELEMENT));
            }

            @Override
            public String getName() {
                final JsObject obj = jsAuthenticationHandler.cast();

                return new IdentityAdapter<String>().adaptNullable(obj.call(GET_NAME));
            }

            @Override
            public void authenticate(final Callback<String, String> callback) {

                final JsObject obj = jsAuthenticationHandler.cast();

                final JavaScriptObject jsCallback = CALLBACK_ADAPTER.adaptNullable(callback);

                obj.call(AUTHENTICATE, JsObject.toArray(jsCallback));
            }
        };
    }

}
