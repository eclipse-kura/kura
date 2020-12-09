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

import java.util.function.Consumer;

import org.eclipse.kura.web2.ext.AlertSeverity;
import org.eclipse.kura.web2.ext.AuthenticationHandler;
import org.eclipse.kura.web2.ext.Context;
import org.eclipse.kura.web2.ext.WidgetFactory;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;

public class ContextAdapter implements Adapter<Context> {

    private static final String SHOW_ALERT_DIALOG = "showAlertDialog";
    private static final String START_LONG_RUNNING_OPERATION = "startLongRunningOperation";
    private static final String GET_XSRF_TOKEN = "getXSRFToken";
    private static final String ADD_AUTHENTICATION_HANDLER = "addAuthenticationHandler";
    private static final String ADD_SETTINGS_COMPONENT = "addSettingsComponent";
    private static final String ADD_SIDENAV_COMPONENT = "addSidenavComponent";

    private static final Adapter<Callback<String, String>> XSRF_TOKEN_ADAPTER = new CallbackAdapter<>(
            new IdentityAdapter<>(), new IdentityAdapter<>());
    private static final Adapter<Callback<Void, String>> OPERATION_ADAPTER = new CallbackAdapter<>(
            new IdentityAdapter<>(), new IdentityAdapter<>());
    private static final Adapter<Consumer<Boolean>> ALERT_CALLBACK_ADAPTER = new ConsumerAdapter<>(
            new BooleanAdapter());

    @Override
    public JavaScriptObject adaptNonNull(final Context context) {
        final JsObject obj = JavaScriptObject.createObject().cast();

        obj.set(ADD_SIDENAV_COMPONENT, new TriConsumerAdapter<String, String, WidgetFactory>(new IdentityAdapter<>(),
                new IdentityAdapter<>(), new WidgetFactoryAdapter()).adaptNullable(context::addSidenavComponent));
        obj.set(ADD_SETTINGS_COMPONENT,
                new BiConsumerAdapter<String, WidgetFactory>(new IdentityAdapter<>(), new WidgetFactoryAdapter())
                        .adaptNullable(context::addSettingsComponent));
        obj.set(ADD_AUTHENTICATION_HANDLER, new ConsumerAdapter<>(new AuthenticationHandlerAdapter())
                .adaptNullable(context::addAuthenticationHandler));
        obj.set(GET_XSRF_TOKEN, new ConsumerAdapter<>(XSRF_TOKEN_ADAPTER).adaptNullable(context::getXSRFToken));
        obj.set(START_LONG_RUNNING_OPERATION,
                new SupplierAdapter<>(OPERATION_ADAPTER).adaptNullable(context::startLongRunningOperation));
        obj.set(SHOW_ALERT_DIALOG,
                new TriConsumerAdapter<String, AlertSeverity, Consumer<Boolean>>(new IdentityAdapter<>(),
                        new EnumAdapter<>(AlertSeverity.class), ALERT_CALLBACK_ADAPTER)
                                .adaptNullable(context::showAlertDialog));

        return obj;
    }

    @Override
    public Context adaptNonNull(final JavaScriptObject jsContext) {
        return new Context() {

            @Override
            public void addSidenavComponent(final String name, final String icon, final WidgetFactory factory) {
                final JsObject obj = jsContext.cast();

                final JavaScriptObject jsName = new IdentityAdapter<>().adaptNullable(name);
                final JavaScriptObject jsIcon = new IdentityAdapter<>().adaptNullable(icon);
                final JavaScriptObject jsElement = new WidgetFactoryAdapter().adaptNullable(factory);

                obj.call(ADD_SIDENAV_COMPONENT, JsObject.toArray(jsName, jsIcon, jsElement));
            }

            @Override
            public void addSettingsComponent(final String name, final WidgetFactory factory) {
                final JsObject obj = jsContext.cast();

                final JavaScriptObject jsName = new IdentityAdapter<>().adaptNullable(name);
                final JavaScriptObject jsElement = new WidgetFactoryAdapter().adaptNullable(factory);

                obj.call(ADD_SETTINGS_COMPONENT, JsObject.toArray(jsName, jsElement));
            }

            @Override
            public void addAuthenticationHandler(final AuthenticationHandler authenticationHandler) {
                final JsObject obj = jsContext.cast();

                final JavaScriptObject jsAuthenticationHandler = new AuthenticationHandlerAdapter()
                        .adaptNullable(authenticationHandler);

                obj.call(ADD_AUTHENTICATION_HANDLER, jsAuthenticationHandler);
            }

            @Override
            public void getXSRFToken(Callback<String, String> callback) {
                final JsObject obj = jsContext.cast();

                final JavaScriptObject jsCallback = XSRF_TOKEN_ADAPTER.adaptNullable(callback);

                obj.call(GET_XSRF_TOKEN, jsCallback);

            }

            @Override
            public Callback<Void, String> startLongRunningOperation() {
                final JsObject obj = jsContext.cast();

                final JavaScriptObject jsCallback = obj.call(START_LONG_RUNNING_OPERATION);

                return OPERATION_ADAPTER.adaptNullable(jsCallback);
            }

            @Override
            public void showAlertDialog(String message, AlertSeverity severity, Consumer<Boolean> callback) {
                final JsObject obj = jsContext.cast();

                final JavaScriptObject jsMessage = new IdentityAdapter<>().adaptNullable(message);
                final JavaScriptObject jsSeverity = new EnumAdapter<>(AlertSeverity.class).adaptNullable(severity);
                final JavaScriptObject jsCallback = ALERT_CALLBACK_ADAPTER.adaptNullable(callback);

                obj.call(SHOW_ALERT_DIALOG, JsObject.toArray(jsMessage, jsSeverity, jsCallback));
            }
        };
    }

}
