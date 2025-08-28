/*******************************************************************************
 * Copyright (c) 2024, 2025 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A service interface that allows to retrieve and verify the password strength
 * requirements that the framework should enforce for new password.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.7.0
 */
@ProviderType
public interface PasswordStrengthVerificationService {

    /**
     * Checks whether the provided password satisfies the password strength
     * requirements currently configured on the system.
     * 
     * @param password
     *            the password to be verified.
     * @throws KuraException
     *             if the password does not satisfy the current password
     *             strength requirements.
     */
    public void checkPasswordStrength(final char[] password) throws KuraException;

    /**
     * Similar to {@link #checkPasswordStrength(char[])} checks whether
     * the provided password satisfies the password strength requirements currently configured
     * on the system and in addition verifies that the password
     * does not match the identityName ignoring the characters case.
     * 
     * @param identityName
     *            the name of the identity
     * @param password
     *            the password to be verified.
     * @since 3.0
     * @throws KuraException
     *             if the password does not satisfy the current password
     *             strength requirements.
     */
    public void checkPasswordStrength(String identityName, final char[] password) throws KuraException;

    /**
     * Returns the password strength requirements that the framework should enforce
     * for new passwords.
     * 
     * @return the password strength requirements.
     * @throws KuraException
     *             if a failure occurs while retrieving the password
     *             strength requirements.
     */
    public PasswordStrengthRequirements getPasswordStrengthRequirements() throws KuraException;
}
