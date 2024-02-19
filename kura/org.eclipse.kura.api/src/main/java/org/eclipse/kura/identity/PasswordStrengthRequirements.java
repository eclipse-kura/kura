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
 ******************************************************************************/
package org.eclipse.kura.identity;

import java.util.Objects;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a set of password strength requirements that should be enforced by
 * the framework for new passwords.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.7.0
 */
@ProviderType
public class PasswordStrengthRequirements {

    private final int passwordMinimumLength;
    private final boolean digitsRequired;
    private final boolean specialCharactersRequired;
    private final boolean bothCasesRequired;

    /**
     * Creates a new instance.
     * 
     * @param passwordMinimumLength     the minimum allowed password length.
     * @param digitsRequired            a {@code boolean} indicating whether new
     *                                  passwords must contain
     *                                  at least one digit.
     * @param specialCharactersRequired a {@code boolean} indicating whether new
     *                                  passwords must contain
     *                                  at least one non alphanumeric character.
     * @param bothCasesRequired         a {@code boolean} indicating whether new
     *                                  passwords must contain
     *                                  at least one upper case and lower case
     *                                  character.
     */
    public PasswordStrengthRequirements(int passwordMinimumLength, boolean digitsRequired,
            boolean specialCharactersRequired, boolean bothCasesRequired) {
        this.passwordMinimumLength = passwordMinimumLength;
        this.digitsRequired = digitsRequired;
        this.specialCharactersRequired = specialCharactersRequired;
        this.bothCasesRequired = bothCasesRequired;
    }

    /**
     * Returns the minimum allowed password length.
     *
     * @return the minimum allowed password length.
     */
    public int getPasswordMinimumLength() {
        return passwordMinimumLength;
    }

    /**
     * Returns a {@code boolean} indicating whether new passwords must contain
     * at least one digit.
     * 
     * @return a {@code boolean} indicating whether new passwords must contain
     *         at least one digit.
     */
    public boolean digitsRequired() {
        return digitsRequired;
    }

    /**
     * Returns a {@code boolean} indicating whether new passwords must contain
     * at least one non alphanumeric character.
     * 
     * @return a {@code boolean} indicating whether new passwords must contain
     *         at least one non alphanumeric character.
     */
    public boolean specialCharactersRequired() {
        return specialCharactersRequired;
    }

    /**
     * Returns a {@code boolean} indicating whether new passwords must contain
     * at least one upper case and lower case character.
     * 
     * @return a {@code boolean} indicating whether new passwords must contain
     *         at least one upper case and lower case character.
     */
    public boolean bothCasesRequired() {
        return bothCasesRequired;
    }

    @Override
    public int hashCode() {
        return Objects.hash(passwordMinimumLength, bothCasesRequired, digitsRequired,
                specialCharactersRequired);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PasswordStrengthRequirements)) {
            return false;
        }
        PasswordStrengthRequirements other = (PasswordStrengthRequirements) obj;
        return passwordMinimumLength == other.passwordMinimumLength
                && bothCasesRequired == other.bothCasesRequired
                && digitsRequired == other.digitsRequired
                && specialCharactersRequired == other.specialCharactersRequired;
    }

}
