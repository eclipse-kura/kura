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
import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Describes the the set of permissions currently assigned to a given identity.
 * If the
 * {@link IdentityService#updateIdentityConfiguration(IdentityConfiguration)}
 * receives an {@link IdentityConfiguration} containing this component, it
 * should replace the currently assigned permission set with the specified one.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.7.0
 */
@ProviderType
public class AssignedPermissions implements IdentityConfigurationComponent {

    private final Set<Permission> permissions;

    /**
     * Creates a new instance representing the provided permission set.
     * 
     * @param permissions the permission set.
     */
    public AssignedPermissions(final Set<Permission> permissions) {
        this.permissions = requireNonNull(permissions, "permissions cannot be null");
    }

    /**
     * Returns the permission set.
     * 
     * @return the permission set.
     */
    public Set<Permission> getPermissions() {
        return permissions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AssignedPermissions)) {
            return false;
        }
        AssignedPermissions other = (AssignedPermissions) obj;
        return Objects.equals(permissions, other.permissions);
    }

}
