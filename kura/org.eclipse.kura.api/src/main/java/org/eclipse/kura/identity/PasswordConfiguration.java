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

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Describes the password related configuration for an identity.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.7.0
 */
@ProviderType
public class PasswordConfiguration implements IdentityConfigurationComponent {

    private final boolean passwordChangeNeeded;
    private final boolean passwordAuthEnabled;
    private final Optional<char[]> newPassword;
    private final Optional<PasswordHash> passwordHash;

    /**
     * Creates a new password configuration.
     * 
     * @param passwordChangeNeeded a {@code boolean} indicating whether a password
     *                             change for the given
     *                             identity is required at next login.
     * @param passwordAuthEnabled  a {@code boolean} indicating whether a password
     *                             authentication is
     *                             enabled for the given identity.
     * @param newPassword          a new password that should be set for the given
     *                             identity, setting this parameter to empty will
     *                             not change the current identity password during a
     *                             configuration update.
     * @param passwordHash         the password hash. This value is ignored by the
     *                             {@link IdentityService} in case of identity
     *                             configuration update.
     */
    public PasswordConfiguration(boolean passwordChangeNeeded, boolean passwordAuthEnabled,
            Optional<char[]> newPassword, Optional<PasswordHash> passwordHash) {
        this.passwordChangeNeeded = passwordChangeNeeded;
        this.passwordAuthEnabled = passwordAuthEnabled;
        this.newPassword = requireNonNull(newPassword, "newPassword cannot be null");
        this.passwordHash = requireNonNull(passwordHash, "password hash cannot be null");
    }

    /**
     * Defines whether a password change is required for the given identity at next
     * login.
     * 
     * @return a {@code boolean} indicating whether a password change for the given
     *         identity is required at next login.
     */
    public boolean isPasswordChangeNeeded() {
        return passwordChangeNeeded;
    }

    /**
     * Defines whether a password authentication is enabled for the given identity.
     * 
     * @return a {@code boolean} indicating whether a password authentication is
     *         enabled for the given identity.
     */
    public boolean isPasswordAuthEnabled() {
        return passwordAuthEnabled;
    }

    /**
     * Returns the hash of the password currently associated with the given
     * identity, if any.
     * 
     * @return the password hash.
     */
    public Optional<PasswordHash> getPasswordHash() {
        return passwordHash;
    }

    /**
     * Returns the new password that should be set for the given identity as part of
     * a configuration update. The {@link IdentityService} will always return an
     * empty optional when the password data for an existing identity is retrieved.
     * 
     * @return the new password.
     */
    public Optional<char[]> getNewPassword() {
        return newPassword;
    }

    @Override
    public int hashCode() {
        return Objects.hash(newPassword, passwordAuthEnabled, passwordChangeNeeded, passwordHash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PasswordConfiguration)) {
            return false;
        }
        PasswordConfiguration other = (PasswordConfiguration) obj;
        return Objects.equals(newPassword, other.newPassword) && passwordAuthEnabled == other.passwordAuthEnabled
                && passwordChangeNeeded == other.passwordChangeNeeded
                && Objects.equals(passwordHash, other.passwordHash);
    }

}
