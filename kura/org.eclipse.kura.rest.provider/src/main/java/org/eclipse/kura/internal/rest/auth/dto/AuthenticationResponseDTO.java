/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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

public class AuthenticationResponseDTO {

    private final boolean passwordChangeNeeded;
    private final String message;

    public AuthenticationResponseDTO(final boolean passwordChangeNeeded) {
        this.passwordChangeNeeded = passwordChangeNeeded;
        this.message = null;
    }

    public AuthenticationResponseDTO(final boolean passwordChangeNeeded, final String message) {
        this.passwordChangeNeeded = passwordChangeNeeded;
        this.message = message;
    }

    public boolean isPasswordChangeNeeded() {
        return passwordChangeNeeded;
    }

    public String getMessage() {
        return message;
    }
}