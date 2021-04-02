/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.useradmin.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.util.configuration.Property;

public class RoleRepositoryStoreOptions {

    private static final String ROLES_CONFIG_ID = "roles.config";
    private static final String USERS_CONFIG_ID = "users.config";
    private static final String GROUPS_CONFIG_ID = "groups.config";
    private static final String WRITE_DELAY_MS_ID = "write.delay.ms";

    private static final Property<String> ROLES_CONFIG = new Property<>(ROLES_CONFIG_ID, "[]");
    private static final Property<String> USERS_CONFIG = new Property<>(USERS_CONFIG_ID, "[]");
    private static final Property<String> GROUPS_CONFIG = new Property<>(GROUPS_CONFIG_ID, "[]");
    private static final Property<Long> WRITE_DELAY_MS = new Property<>(WRITE_DELAY_MS_ID, 5000L);

    private final String rolesConfig;
    private final String usersConfig;
    private final String groupsConfig;
    private final long writeDelayMs;

    public RoleRepositoryStoreOptions(String rolesConfig, String usersConfig, String gropusConfig, long writeDelayMs) {
        this.rolesConfig = rolesConfig;
        this.usersConfig = usersConfig;
        this.groupsConfig = gropusConfig;
        this.writeDelayMs = writeDelayMs;
    }

    public RoleRepositoryStoreOptions(final Map<String, Object> properties) {
        this.rolesConfig = ROLES_CONFIG.get(properties);
        this.usersConfig = USERS_CONFIG.get(properties);
        this.groupsConfig = GROUPS_CONFIG.get(properties);
        this.writeDelayMs = WRITE_DELAY_MS.get(properties);
    }

    public String getRolesConfig() {
        return this.rolesConfig;
    }

    public String getUsersConfig() {
        return this.usersConfig;
    }

    public String getGroupsConfig() {
        return this.groupsConfig;
    }

    public long getWriteDelayMs() {
        return this.writeDelayMs;
    }

    public Map<String, Object> toProperties() {
        final Map<String, Object> result = new HashMap<>();

        result.put(ROLES_CONFIG_ID, this.rolesConfig);
        result.put(USERS_CONFIG_ID, this.usersConfig);
        result.put(GROUPS_CONFIG_ID, this.groupsConfig);
        result.put(WRITE_DELAY_MS_ID, this.writeDelayMs);

        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupsConfig, rolesConfig, usersConfig, writeDelayMs);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RoleRepositoryStoreOptions other = (RoleRepositoryStoreOptions) obj;
        return Objects.equals(groupsConfig, other.groupsConfig) && Objects.equals(rolesConfig, other.rolesConfig)
                && Objects.equals(usersConfig, other.usersConfig) && writeDelayMs == other.writeDelayMs;
    }
}
