/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.useradmin.store.test;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.IntConsumer;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class RoleRepositoryStoreTest {

    private final UserAdmin userAdmin;
    private final ConfigurationService configurationService;

    @BeforeClass
    public static void setUp() {
        try {
            // wait for spurious updates
            WireTestUtil
                    .modified("(kura.service.pid=org.eclipse.kura.internal.useradmin.store.RoleRepositoryStoreImpl)")
                    .get(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            // no need
        }
    }

    public RoleRepositoryStoreTest()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        userAdmin = WireTestUtil.trackService(UserAdmin.class, Optional.empty()).get(30, TimeUnit.SECONDS);
        configurationService = WireTestUtil.trackService(ConfigurationService.class, Optional.empty()).get(30,
                TimeUnit.SECONDS);

        final CompletableFuture<Void> modified = WireTestUtil
                .modified("(kura.service.pid=org.eclipse.kura.internal.useradmin.store.RoleRepositoryStoreImpl)");

        final Role[] roles = userAdmin.getRoles(null);

        if (roles == null) {
            return;
        }

        for (final Role role : roles) {
            userAdmin.removeRole(role.getName());
        }

        modified.get(30, TimeUnit.SECONDS);
    }

    @Test
    public void shouldHaveDefaultConfig() throws KuraException {
        final Options currentOptions = getCurrentRoleRepositoryStoreOptions();

        assertEquals("[]", currentOptions.rolesConfig);
        assertEquals("[]", currentOptions.usersConfig);
        assertEquals("[]", currentOptions.groupsConfig);
        assertEquals(5000L, currentOptions.writeDelayMs);
    }

    @Test
    public void shouldCreateEmptyUser()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        testRoleKindConfig(k -> userAdmin.createRole("foo" + k, k), k -> "[{\"name\":\"foo" + k + "\"}]", Role.USER,
                Role.GROUP);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSerializeRoleProperties()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        testRoleKindConfig(k -> {
            final Role role = userAdmin.createRole("foo" + k, k);
            role.getProperties().put("foo", "bar");
            role.getProperties().put("boo", new byte[] { 1, 2, 3, 4 });
        }, k -> "[{\"name\":\"foo" + k + "\",\"properties\":{\"boo\":[1,2,3,4],\"foo\":\"bar\"}}]", Role.USER,
                Role.GROUP);
    }

    @Test
    public void shouldSupportRemovingRoleProperties()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        shouldSerializeRoleProperties();

        testRoleKindConfig(k -> {
            final Role role = userAdmin.getRole("foo" + k);
            role.getProperties().remove("boo");
        }, k -> "[{\"name\":\"foo" + k + "\",\"properties\":{\"foo\":\"bar\"}}]", Role.USER, Role.GROUP);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSerializeRoleCredentials()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        testRoleKindConfig(k -> {
            final User role = (User) userAdmin.createRole("foo" + k, k);
            role.getCredentials().put("foo", "bar");
            role.getCredentials().put("boo", new byte[] { 1, 2, 3, 4 });
        }, k -> "[{\"name\":\"foo" + k + "\",\"credentials\":{\"boo\":[1,2,3,4],\"foo\":\"bar\"}}]", Role.USER,
                Role.GROUP);
    }

    @Test
    public void shouldSupportRemovingRoleCredentials()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        shouldSerializeRoleCredentials();

        testRoleKindConfig(k -> {
            final User role = (User) userAdmin.getRole("foo" + k);
            role.getCredentials().remove("boo");
        }, k -> "[{\"name\":\"foo" + k + "\",\"credentials\":{\"foo\":\"bar\"}}]", Role.USER, Role.GROUP);
    }

    @Test
    public void shouldSupportBasicMembers()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {

        final Role foo = userAdmin.createRole("foo", Role.USER);
        final Role bar = userAdmin.createRole("bar", Role.USER);
        final Role baz = userAdmin.createRole("baz", Role.GROUP);

        testRoleKindConfig(k -> {
            final Group group = (Group) userAdmin.createRole("group", Role.GROUP);
            group.addMember(foo);
            group.addMember(bar);
            group.addMember(baz);
        }, k -> "[{\"name\":\"baz\"},{\"name\":\"group\",\"basicMembers\":[\"bar\",\"baz\",\"foo\"]}]", Role.GROUP);
    }

    @Test
    public void shouldSupportRemovingBasicMembers()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {

        shouldSupportBasicMembers();

        testRoleKindConfig(k -> {
            final Group group = (Group) userAdmin.getRole("group");
            group.removeMember(userAdmin.getRole("baz"));
        }, k -> "[{\"name\":\"baz\"},{\"name\":\"group\",\"basicMembers\":[\"bar\",\"foo\"]}]", Role.GROUP);
    }

    @Test
    public void shouldSupportRequiredMembers()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {

        final Role foo = userAdmin.createRole("foo", Role.USER);
        final Role bar = userAdmin.createRole("bar", Role.USER);
        final Role baz = userAdmin.createRole("baz", Role.GROUP);

        testRoleKindConfig(k -> {
            final Group group = (Group) userAdmin.createRole("group", Role.GROUP);
            group.addRequiredMember(foo);
            group.addRequiredMember(bar);
            group.addRequiredMember(baz);
        }, k -> "[{\"name\":\"baz\"},{\"name\":\"group\",\"requiredMembers\":[\"bar\",\"baz\",\"foo\"]}]", Role.GROUP);
    }

    @Test
    public void shouldSupportRemovingRequiredMembers()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {

        shouldSupportRequiredMembers();

        testRoleKindConfig(k -> {
            final Group group = (Group) userAdmin.getRole("group");
            group.removeMember(userAdmin.getRole("baz"));
        }, k -> "[{\"name\":\"baz\"},{\"name\":\"group\",\"requiredMembers\":[\"bar\",\"foo\"]}]", Role.GROUP);
    }

    private void testRoleKindConfig(final IntConsumer setup, final Function<Integer, String> expectedConfig,
            final int... kinds)
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        for (final int type : kinds) {

            final CompletableFuture<Void> modified = WireTestUtil
                    .modified("(kura.service.pid=org.eclipse.kura.internal.useradmin.store.RoleRepositoryStoreImpl)");

            setup.accept(type);

            modified.get(30, TimeUnit.SECONDS);

            final Options currentOptions = getCurrentRoleRepositoryStoreOptions();

            final String expected = expectedConfig.apply(type);

            if (type == Role.ROLE) {
                assertEquals(expected, currentOptions.rolesConfig);
            } else if (type == Role.USER) {
                assertEquals(expected, currentOptions.usersConfig);
            } else if (type == Role.GROUP) {
                assertEquals(expected, currentOptions.groupsConfig);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private Options getCurrentRoleRepositoryStoreOptions() throws KuraException {
        final ComponentConfiguration config = configurationService
                .getComponentConfiguration("org.eclipse.kura.internal.useradmin.store.RoleRepositoryStoreImpl");

        return new Options(config.getConfigurationProperties());
    }

    private static final class Options {

        private static final String ROLES_CONFIG_ID = "roles.config";
        private static final String USERS_CONFIG_ID = "users.config";
        private static final String GROUPS_CONFIG_ID = "groups.config";
        private static final String WRITE_DELAY_MS_ID = "write.delay.ms";

        private final String rolesConfig;
        private final String usersConfig;
        private final String groupsConfig;
        private final long writeDelayMs;

        Options(final Map<String, Object> properties) {
            this.rolesConfig = (String) properties.getOrDefault(ROLES_CONFIG_ID, "[]");
            this.usersConfig = (String) properties.getOrDefault(USERS_CONFIG_ID, "[]");
            this.groupsConfig = (String) properties.getOrDefault(GROUPS_CONFIG_ID, "[]");
            this.writeDelayMs = (Long) properties.getOrDefault(WRITE_DELAY_MS_ID, 5000L);
        }
    }

}
