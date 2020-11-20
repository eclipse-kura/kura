package org.eclipse.kura.internal.useradmin.store;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.util.configuration.Property;

public class RoleRepositoryStoreOptions {

    private static final String ROLES_CONFIG_ID = "roles.config";
    private static final String WRITE_DELAY_MS_ID = "write.delay.ms";

    private static final Property<String> ROLES_CONFIG = new Property<>(ROLES_CONFIG_ID, "{}");
    private static final Property<Long> WRITE_DELAY_MS = new Property<>(WRITE_DELAY_MS_ID, 5000L);

    private final String rolesConfig;
    private final long writeDelayMs;

    public RoleRepositoryStoreOptions(String rolesConfig, long writeDelayMs) {
        this.rolesConfig = rolesConfig;
        this.writeDelayMs = writeDelayMs;
    }

    public RoleRepositoryStoreOptions(final Map<String, Object> properties) {
        this.rolesConfig = ROLES_CONFIG.get(properties);
        this.writeDelayMs = WRITE_DELAY_MS.get(properties);
    }

    public String getRolesConfig() {
        return rolesConfig;
    }

    public long getWriteDelayMs() {
        return writeDelayMs;
    }

    public Map<String, Object> toProperties() {
        final Map<String, Object> result = new HashMap<>();

        result.put(ROLES_CONFIG_ID, rolesConfig);
        result.put(WRITE_DELAY_MS_ID, writeDelayMs);

        return result;
    }
}
