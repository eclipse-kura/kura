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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A service interface that allows to manage Kura identities.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.7.0
 */
@ProviderType
public interface IdentityService {

    /**
     * Creates a new identity with the given name.
     * 
     * @param identityName the name of the identity to be created.
     * @return {@code true} if the identity with the given name has been created as
     *         part of the method call or {@code false} if the identity already
     *         exist.
     * @throws KuraException if a failure occurs in creating the identity.
     */
    public boolean createIdentity(final String identityName) throws KuraException;

    /**
     * Deletes the identity with the given name.
     * 
     * @param identityName the name of the identity to be deleted.
     * @return {@code true} if the identity with the given name has been deleted as
     *         part of the method call or {@code false} if the identity does not
     *         exist.
     * @throws KuraException if a failure occurs in deleting the identity.
     */
    public boolean deleteIdentity(final String identityName) throws KuraException;

    /**
     * Returns the configuration of all existing identities.
     * 
     * @param componentsToReturn the set of {@link IdentityConfigurationComponent}
     *                           types to be
     *                           returned.
     * @return the
     * @throws KuraException if a failure occurs in retrieving identity
     *                       configurations.
     */
    public List<IdentityConfiguration> getIdentitiesConfiguration(
            Set<Class<? extends IdentityConfigurationComponent>> componentsToReturn)
            throws KuraException;

    /**
     * Returns the configuration of the identity with the given name.
     * 
     * @param identityName       the identity name.
     * @param componentsToReturn the set of {@link IdentityConfigurationComponent}
     *                           types to be
     *                           returned.
     * @return the configuration of the requested identity or an empty optional if
     *         the identity does not exist.
     * @throws KuraException if a failure occurs in retrieving identity
     *                       configuration.
     */
    public Optional<IdentityConfiguration> getIdentityConfiguration(final String identityName,
            Set<Class<? extends IdentityConfigurationComponent>> componentsToReturn)
            throws KuraException;

    /**
     * Returns the default configuration for the identity with the given name, this
     * method should succeed even if the identity does not exist. The result should
     * be the same configuration returned by the
     * {@link IdentityService#getIdentityConfiguration(String, List)}
     * method for an identity that has just been created with the
     * {@link IdentityService#createIdentity(String)} method.
     * 
     * This
     * method can be useful for example to allow a user interface to show the
     * initial identity configuration to the user before creating it.
     *
     * @param identityName       the identity name.
     * @param componentsToReturn the set of {@link IdentityConfigurationComponent}
     *                           types to be
     *                           returned.
     * @return the default configuration for the requested identity
     * @throws KuraException if a failure occurs in retrieving identity
     *                       configuration.
     */
    public IdentityConfiguration getIdentityDefaultConfiguration(final String identityName,
            Set<Class<? extends IdentityConfigurationComponent>> componentsToReturn) throws KuraException;

    /**
     * Updates the configuration of the given identity for the provided
     * {@link IdentityConfigurationComponent} types.
     * The configuration of the
     * components that have not been provided will not be modified.
     * 
     * @param identityConfiguration the new identity configuration.
     * @throws KuraException if a failure occurs updating identity
     *                       configuration.
     */
    public void updateIdentityConfiguration(final IdentityConfiguration identityConfiguration) throws KuraException;

    /**
     * Defines a new permission.
     * 
     * @param permission the permission to be created.
     * @return {@code true} if the permission has been created as
     *         part of the method call or {@code false} if the permission already
     *         exist.
     * @throws KuraException if a failure occurs creating the permission.
     */
    public boolean createPermission(final Permission permission) throws KuraException;

    /**
     * Removes an existing permission. The permission will also be removed from all
     * identities assigned to it.
     * 
     * @param permission the permission to be deleted.
     * @return {@code true} if the permission has been deleted as
     *         part of the method call or {@code false} if the permission does not
     *         exist.
     * @throws KuraException if a failure occurs deleting the permission.
     */
    public boolean deletePermission(final Permission permission) throws KuraException;

    /**
     * Returns the set of permissions that are currently defined within the
     * framework.
     * 
     * @return the set of permissions that are currently defined within the
     *         framework.
     * @throws KuraException if a failure occurs retrieving the permission set.
     */
    public Set<Permission> getPermissions() throws KuraException;

    /**
     * Returns the password strength requirements that the framework should enforce
     * for new passwords.
     * 
     * @return the password strength requirements.
     * @throws KuraException if a failure occurs while retrieving the password
     *                       strength requirements.
     */
    public PasswordStrengthRequirements getPasswordStrengthRequirements() throws KuraException;

    /**
     * Computes a {@link PasswordHash} for the given plaintext password. The
     * password array will be overwritten at the end of the operation.
     * 
     * @param password the plaintext password.
     * @return the computed password hash.
     * @throws KuraException if a failure occurs computing the password hash
     */
    public PasswordHash computePasswordHash(final char[] password) throws KuraException;
}
