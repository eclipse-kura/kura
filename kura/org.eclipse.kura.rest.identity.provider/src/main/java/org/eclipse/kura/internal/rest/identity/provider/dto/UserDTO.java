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

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class UserDTO {

    private String userName;
    private Boolean passwordAuthEnabled;
    private Boolean passwordChangeNeeded;
    private Set<String> permissions;
    private String password;

    public UserDTO() {

    }

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
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Optional<Boolean> isPasswordAuthEnabled() {
        return Optional.ofNullable(this.passwordAuthEnabled);
    }

    public void setPasswordAuthEnabled(boolean passwordAuthEnabled) {
        this.passwordAuthEnabled = passwordAuthEnabled;
    }

    public Optional<Boolean> isPasswordChangeNeeded() {
        return Optional.ofNullable(this.passwordChangeNeeded);
    }

    public void setPasswordChangeNeeded(boolean passwordChangeNeeded) {
        this.passwordChangeNeeded = passwordChangeNeeded;
    }

    public Set<String> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.userName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        UserDTO other = (UserDTO) obj;
        return Objects.equals(this.userName, other.userName);
    }

}
