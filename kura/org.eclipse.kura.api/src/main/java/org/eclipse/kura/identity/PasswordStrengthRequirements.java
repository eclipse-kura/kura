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
    private final boolean passwordRequireDigits;
    private final boolean passwordRequireSpecialChars;
    private final boolean passwordRequireBothCases;

    /**
     * Creates a new instance.
     * 
     * @param passwordMinimumLength       the minimum allowed password length.
     * @param passwordRequireDigits       a {@code boolean} indicating whether new
     *                                    passwords must contain
     *                                    at least one digit.
     * @param passwordRequireSpecialChars a {@code boolean} indicating whether new
     *                                    passwords must contain
     *                                    at least one non alphanumeric character.
     * @param passwordRequireBothCases    a {@code boolean} indicating whether new
     *                                    passwords must contain
     *                                    at least one upper case and lower case
     *                                    character.
     */
    public PasswordStrengthRequirements(int passwordMinimumLength, boolean passwordRequireDigits,
            boolean passwordRequireSpecialChars, boolean passwordRequireBothCases) {
        this.passwordMinimumLength = passwordMinimumLength;
        this.passwordRequireDigits = passwordRequireDigits;
        this.passwordRequireSpecialChars = passwordRequireSpecialChars;
        this.passwordRequireBothCases = passwordRequireBothCases;
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
    public boolean isPasswordRequireDigits() {
        return passwordRequireDigits;
    }

    /**
     * Returns a {@code boolean} indicating whether new passwords must contain
     * at least one non alphanumeric character.
     * 
     * @return
     */
    public boolean isPasswordRequireSpecialChars() {
        return passwordRequireSpecialChars;
    }

    /**
     * Returns a {@code boolean} indicating whether new passwords must contain
     * at least one upper case and lower case character.
     * 
     * @return a {@code boolean} indicating whether new passwords must contain
     *         at least one non alphanumeric character.
     */
    public boolean isPasswordRequireBothCases() {
        return passwordRequireBothCases;
    }

    @Override
    public int hashCode() {
        return Objects.hash(passwordMinimumLength, passwordRequireBothCases, passwordRequireDigits,
                passwordRequireSpecialChars);
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
                && passwordRequireBothCases == other.passwordRequireBothCases
                && passwordRequireDigits == other.passwordRequireDigits
                && passwordRequireSpecialChars == other.passwordRequireSpecialChars;
    }

}
