/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
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
