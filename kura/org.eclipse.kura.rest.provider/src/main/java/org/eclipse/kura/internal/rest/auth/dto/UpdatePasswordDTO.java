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

import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;

public class UpdatePasswordDTO {

    private final String currentPassword;
    private final String newPassword;

    public UpdatePasswordDTO(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void validate() {
        if (currentPassword == null || currentPassword.trim().isEmpty() || newPassword == null
                || newPassword.trim().isEmpty()) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                    "currentPassword or newPassword have not been provided");
        }
    }
}
