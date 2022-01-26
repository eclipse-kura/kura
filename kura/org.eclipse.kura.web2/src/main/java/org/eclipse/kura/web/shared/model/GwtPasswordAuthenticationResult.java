/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtPasswordAuthenticationResult implements IsSerializable {

    private boolean isPasswordUpdateRequired;
    private String redirectPath;

    public GwtPasswordAuthenticationResult() {
    }

    public GwtPasswordAuthenticationResult(final boolean isPasswordUpdateRequired, final String redirectPath) {
        this.isPasswordUpdateRequired = isPasswordUpdateRequired;
        this.redirectPath = redirectPath;
    }

    public boolean isPasswordUpdateRequired() {
        return isPasswordUpdateRequired;
    }

    public String redirectPath() {
        return redirectPath;
    }
}
