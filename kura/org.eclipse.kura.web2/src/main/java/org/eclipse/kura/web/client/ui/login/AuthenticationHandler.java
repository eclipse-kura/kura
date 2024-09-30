/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.web.client.ui.login;

import java.util.function.Supplier;

import com.google.gwt.core.client.Callback;
import com.google.gwt.user.client.ui.Widget;

public interface AuthenticationHandler {

    public String getName();

    public Supplier<Widget> getLoginDialogElement();

    public void authenticate(final Callback<String, String> callback);
}
