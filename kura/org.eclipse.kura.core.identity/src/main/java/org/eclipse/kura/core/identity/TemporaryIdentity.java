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
package org.eclipse.kura.core.identity;

import java.util.Objects;
import java.util.Set;

import org.eclipse.kura.identity.Permission;

/**
 * Represents a temporary identity that exists only in memory.
 * Used for container authentication without persistent storage.
 */
public class TemporaryIdentity {

    private final String name;
    private final Set<Permission> permissions;
    private final long creationTime;
    private final String token;

    public TemporaryIdentity(String name, Set<Permission> permissions, String token) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.permissions = Objects.requireNonNull(permissions, "permissions cannot be null");
        this.token = Objects.requireNonNull(token, "token cannot be null");
        this.creationTime = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getToken() {
        return token;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, token);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TemporaryIdentity)) {
            return false;
        }
        TemporaryIdentity other = (TemporaryIdentity) obj;
        return Objects.equals(name, other.name) && Objects.equals(token, other.token);
    }

    @Override
    public String toString() {
        return "TemporaryIdentity{" + "name='" + name + '\'' + ", permissions=" + permissions.size() + ", creationTime="
                + creationTime + '}';
    }
}