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

public class UsernamePasswordDTO {

    private final String username;
    private final String password;

    public UsernamePasswordDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void validate() {
        if (username == null || username.trim().isEmpty() || password == null
                || password.trim().isEmpty()) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                    "username and or password fields have not been provided");
        }
    }

}
