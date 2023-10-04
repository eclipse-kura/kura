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
package org.eclipse.kura.internal.rest.identity.provider.dto;

import java.util.Set;

public class UserDTO {

    private final String userName;
    private final boolean passwordAuthEnabled;
    private final boolean passwordChangeNeeded;
    private final Set<String> permissions;
    private String password;

    public UserDTO(final String userName, final Set<String> permissions, final boolean passwordAuthEnabled,
            final boolean passwordChangeNeeded, final String password) {

        this.userName = userName;
        this.passwordAuthEnabled = passwordAuthEnabled;
        this.passwordChangeNeeded = passwordChangeNeeded;
        this.permissions = permissions;
        this.password = password;
    }

    public UserDTO(final String userName, final Set<String> permissions, final boolean passwordAuthEnabled,
            final boolean passwordChangeNeeded) {

        this(userName, permissions, passwordAuthEnabled, passwordChangeNeeded, null);

    }

    public String getUserName() {
        return userName;
    }

    public boolean isPasswordAuthEnabled() {
        return passwordAuthEnabled;
    }

    public boolean isPasswordChangeNeeded() {
        return passwordChangeNeeded;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}