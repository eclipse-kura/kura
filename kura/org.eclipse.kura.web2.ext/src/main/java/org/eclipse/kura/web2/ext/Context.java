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
package org.eclipse.kura.web2.ext;

import java.util.function.Consumer;

import com.google.gwt.core.client.Callback;

public interface Context {

    public void addSidenavComponent(String name, String icon, WidgetFactory widget);

    public void addSettingsComponent(String name, WidgetFactory widget);

    public void addAuthenticationHandler(AuthenticationHandler authenticationHandler);

    public void getXSRFToken(final Callback<String, String> callback);

    public Callback<Void, String> startLongRunningOperation();

    public void showAlertDialog(final String message, final AlertSeverity severity, final Consumer<Boolean> callback);
}
