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
package org.eclipse.kura.internal.rest.identity.provider.v2.dto;

public class PasswordConfigurationDTO {

    private boolean passwordChangeNeeded;
    private boolean passwordAuthEnabled;
    private String password;

    public boolean isPasswordChangeNeeded() {
        return this.passwordChangeNeeded;
    }

    public void setPasswordChangeNeeded(boolean passwordChangeNeeded) {
        this.passwordChangeNeeded = passwordChangeNeeded;
    }

    public boolean isPasswordAuthEnabled() {
        return this.passwordAuthEnabled;
    }

    public void setPasswordAuthEnabled(boolean passwordAuthEnabled) {
        this.passwordAuthEnabled = passwordAuthEnabled;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String passwordHash) {
        this.password = passwordHash;
    }

    @Override
    public String toString() {
        return "PasswordConfigurationDTO [passwordChangeNeeded=" + this.passwordChangeNeeded + ", passwordAuthEnabled="
                + this.passwordAuthEnabled + ", password=****]";
    }

}
