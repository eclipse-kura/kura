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
package org.eclipse.kura.web.client;

import java.util.List;

import org.eclipse.kura.web.client.ui.login.LoginUi;
import org.eclipse.kura.web.shared.model.GwtClientExtensionBundle;
import org.eclipse.kura.web.shared.service.GwtExtensionService;
import org.eclipse.kura.web.shared.service.GwtExtensionServiceAsync;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;

public class Login implements EntryPoint {

    private final GwtExtensionServiceAsync gwtExtensionService = GWT.create(GwtExtensionService.class);

    @Override
    public void onModuleLoad() {
        this.gwtExtensionService.getLoginExtensions(new AsyncCallback<List<GwtClientExtensionBundle>>() {

            @Override
            public void onFailure(Throwable caught) {
                // do nothing
            }

            @Override
            public void onSuccess(List<GwtClientExtensionBundle> result) {

                for (final GwtClientExtensionBundle extension : result) {
                    ScriptInjector.fromUrl(extension.getEntryPointUrl()).inject();
                }
            }
        });

        RootPanel.get().add(GWT.create(LoginUi.class));
    }

}
