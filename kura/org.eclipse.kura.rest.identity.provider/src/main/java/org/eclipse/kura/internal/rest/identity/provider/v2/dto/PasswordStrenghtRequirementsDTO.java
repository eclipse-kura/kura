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

import java.util.Objects;

public class PasswordStrenghtRequirementsDTO {

    private final int passwordMinimumLength;
    private final Boolean digitsRequired;
    private final Boolean specialCharactersRequired;
    private final Boolean bothCasesRequired;

    public PasswordStrenghtRequirementsDTO(int passwordMinimumLength, Boolean digitsRequired,
            Boolean specialCharactersRequired, Boolean bothCasesRequired) {

        this.passwordMinimumLength = passwordMinimumLength;
        this.digitsRequired = digitsRequired;
        this.specialCharactersRequired = specialCharactersRequired;
        this.bothCasesRequired = bothCasesRequired;
    }

    public int getPasswordMinimumLength() {
        return this.passwordMinimumLength;
    }

    public Boolean isDigitsRequired() {
        return this.digitsRequired;
    }

    public Boolean isSpecialCharactersRequired() {
        return this.specialCharactersRequired;
    }

    public Boolean isBothCasesRequired() {
        return this.bothCasesRequired;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.bothCasesRequired, this.digitsRequired, this.passwordMinimumLength,
                this.specialCharactersRequired);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        PasswordStrenghtRequirementsDTO other = (PasswordStrenghtRequirementsDTO) obj;
        return this.bothCasesRequired == other.bothCasesRequired && this.digitsRequired == other.digitsRequired
                && this.passwordMinimumLength == other.passwordMinimumLength
                && this.specialCharactersRequired == other.specialCharactersRequired;
    }

    @Override
    public String toString() {
        return "PasswordStrenghtRequirementsDTO [passwordMinimumLength=" + this.passwordMinimumLength
                + ", digitsRequired=" + this.digitsRequired + ", specialCharactersRequired="
                + this.specialCharactersRequired + ", bothCasesRequired=" + this.bothCasesRequired + "]";
    }

}
