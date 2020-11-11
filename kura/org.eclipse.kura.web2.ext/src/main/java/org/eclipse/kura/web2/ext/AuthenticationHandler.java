/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web2.ext;

import com.google.gwt.core.client.Callback;

public interface AuthenticationHandler {

    public String getName();

    public WidgetFactory getLoginDialogElement();

    public void authenticate(final Callback<String, String> callback);
}
