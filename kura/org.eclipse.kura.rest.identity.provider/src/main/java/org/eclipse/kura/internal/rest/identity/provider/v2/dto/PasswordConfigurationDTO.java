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

    private Boolean passwordChangeNeeded;
    private Boolean passwordAuthEnabled;
    private String password;

    public Boolean isPasswordChangeNeeded() {
        return this.passwordChangeNeeded;
    }

    public void setPasswordChangeNeeded(Boolean passwordChangeNeeded) {
        this.passwordChangeNeeded = passwordChangeNeeded;
    }

    public Boolean isPasswordAuthEnabled() {
        return this.passwordAuthEnabled;
    }

    public void setPasswordAuthEnabled(Boolean passwordAuthEnabled) {
        this.passwordAuthEnabled = passwordAuthEnabled;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "PasswordConfigurationDTO [passwordChangeNeeded=" + this.passwordChangeNeeded + ", passwordAuthEnabled="
                + this.passwordAuthEnabled + "]";
    }

}
