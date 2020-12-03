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
package org.eclipse.kura.example.web.extension.client;

import org.eclipse.kura.example.web.extension.shared.service.DummyAuthenticationService;
import org.eclipse.kura.example.web.extension.shared.service.DummyAuthenticationServiceAsync;
import org.eclipse.kura.web2.ext.AuthenticationHandler;
import org.eclipse.kura.web2.ext.Context;
import org.eclipse.kura.web2.ext.Extension;
import org.eclipse.kura.web2.ext.WidgetFactory;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.InputGroup;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class ExampleAuthenticationHandler implements Extension {

    private static final DummyAuthenticationServiceAsync DUMMY_AUTH = GWT.create(DummyAuthenticationService.class);

    @Override
    public void onLoad(final Context context) {
        context.addAuthenticationHandler(new AlwaysFails());
        context.addAuthenticationHandler(new AlwaysSucceeds());
    }

    private class AlwaysSucceeds implements AuthenticationHandler {

        private final Input usernameInput = new Input();

        public AlwaysSucceeds() {
            usernameInput.setPlaceholder("Enter user name");
        }

        @Override
        public String getName() {
            return "Always Succeeds";
        }

        @Override
        public WidgetFactory getLoginDialogElement() {
            return () -> usernameInput;
        }

        @Override
        public void authenticate(final Callback<String, String> callback) {
            DUMMY_AUTH.login(usernameInput.getText(), new AsyncCallback<String>() {

                @Override
                public void onSuccess(String result) {
                    callback.onSuccess(result);
                }

                @Override
                public void onFailure(Throwable caught) {
                    callback.onFailure(caught.getMessage());
                }
            });
        }
    }

    private class ExampleWidget extends InputGroup {

        private final Input exampleInput = new Input();
        private final Button exampleButton = new Button();

        public ExampleWidget() {

            this.exampleInput.setPlaceholder("Enter failure message here");
            this.exampleButton.setText("Example button");

            this.exampleButton.addClickHandler(e -> exampleInput.setText("button clicked"));

            this.add(this.exampleInput);
            this.add(this.exampleButton);
        }

        public String getText() {
            return exampleInput.getValue();
        }
    }

    private class AlwaysFails implements AuthenticationHandler {

        private ExampleWidget exampleWidget;

        @Override
        public String getName() {
            return "Always Fails";
        }

        @Override
        public WidgetFactory getLoginDialogElement() {
            return () -> {
                this.exampleWidget = new ExampleWidget();
                return this.exampleWidget;
            };
        }

        @Override
        public void authenticate(final Callback<String, String> callback) {
            callback.onFailure("not authorized, message: " + this.exampleWidget.getText());
        }
    }

}
