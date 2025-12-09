/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.identity;

import java.util.Set;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A service interface that allows to manage temporary Kura identities for containers.
 * Temporary identities are not persisted and are automatically removed when no longer needed.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.8.0
 */
@ProviderType
public interface TemporaryIdentityService {

    /**
     * Creates a temporary identity with the given name and permissions.
     * The identity will not be persisted and will exist only in memory.
     * 
     * @param identityName the name of the temporary identity to be created.
     * @param permissions the set of permissions to be assigned to this temporary identity.
     * @return a temporary authentication token that can be used to authenticate as this identity.
     * @throws KuraException if a failure occurs in creating the temporary identity.
     */
    public String createTemporaryIdentity(final String identityName, final Set<Permission> permissions) throws KuraException;

    /**
     * Deletes a temporary identity identified by its authentication token.
     * 
     * @param token the authentication token of the temporary identity to be deleted.
     * @return {@code true} if the temporary identity was deleted as part of the method call 
     *         or {@code false} if the identity does not exist.
     * @throws KuraException if a failure occurs in deleting the temporary identity.
     */
    public boolean deleteTemporaryIdentity(final String token) throws KuraException;

    /**
     * Validates a temporary identity authentication token and returns the identity name.
     * 
     * @param token the authentication token to validate.
     * @return the identity name if the token is valid.
     * @throws KuraException if the token is invalid or if a failure occurs during validation.
     */
    public String validateTemporaryToken(final String token) throws KuraException;

    /**
     * Checks if the specified permission is currently assigned to the temporary identity
     * identified by the given token.
     * 
     * @param token the authentication token of the temporary identity.
     * @param permission the permission to check.
     * @throws KuraException if the provided permission is not currently assigned to
     *                       the given temporary identity or if a failure occurs while performing the check.
     */
    public void checkTemporaryPermission(final String token, final Permission permission) throws KuraException;
}