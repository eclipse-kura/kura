/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.auth.dto;

public class JwtDTO {

    private final String token;
    private final long expiresAtEpochMillis;

    public JwtDTO(final String token, final long expiresAtEpochMillis) {
        this.token = token;
        this.expiresAtEpochMillis = expiresAtEpochMillis;
    }

    public String getToken() {
        return this.token;
    }

    public long getExpiresAtEpochMillis() {
        return this.expiresAtEpochMillis;
    }

}
