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

import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a permission that can be assigned to Kura identites.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.7.0
 */
@ProviderType
public class Permission {

    private final String name;

    /**
     * Creates a new instance.
     * 
     * @param name the identity name.
     */
    public Permission(String name) {
        this.name = requireNonNull(name, "name cannot be null");

        if (this.name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty");
        }
    }

    /**
     * Creates a new instance.
     * 
     * @return the identity name.
     */
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Permission)) {
            return false;
        }
        Permission other = (Permission) obj;
        return Objects.equals(name, other.name);
    }

}
