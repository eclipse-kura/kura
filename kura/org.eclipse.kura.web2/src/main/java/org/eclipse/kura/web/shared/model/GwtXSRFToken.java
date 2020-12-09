/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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

import java.util.Date;

public class GwtXSRFToken implements java.io.Serializable {

    private static final long serialVersionUID = 6731819179007021824L;
    private static final long TOKEN_VALIDITY_PERIOD_MS = 300000;

    private String token;

    private Date expiresOn;

    public GwtXSRFToken() {
        setExpiresOn(new Date(System.currentTimeMillis() + TOKEN_VALIDITY_PERIOD_MS));
    }

    public GwtXSRFToken(String tokenString) {
        this.token = tokenString;
        setExpiresOn(new Date(System.currentTimeMillis() + TOKEN_VALIDITY_PERIOD_MS));
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getExpiresOn() {
        return this.expiresOn;
    }

    public void setExpiresOn(Date expiresOn) {
        this.expiresOn = expiresOn;
    }
}
