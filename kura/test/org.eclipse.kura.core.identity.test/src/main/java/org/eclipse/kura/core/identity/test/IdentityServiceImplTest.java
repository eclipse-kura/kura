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
 *******************************************************************************/
package org.eclipse.kura.core.identity.test;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.identity.AdditionalConfigurations;
import org.eclipse.kura.identity.AssignedPermissions;
import org.eclipse.kura.identity.IdentityConfiguration;
import org.eclipse.kura.identity.IdentityConfigurationComponent;
import org.eclipse.kura.identity.IdentityService;
import org.eclipse.kura.identity.PasswordConfiguration;
import org.eclipse.kura.identity.PasswordHash;
import org.eclipse.kura.identity.Permission;
import org.eclipse.kura.identity.configuration.extension.IdentityConfigurationExtension;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class IdentityServiceImplTest {

    @Test
    public void shouldCreateIdentity() {
        givenNoUserAdminRoleWithName("kura.user.foo");
        whenIdentityIsCreated("foo");

        thenNoExceptionIsThrown();
        thenIdentityServiceReportsUserCreated(true);

        thenUserAdminRoleExists("kura.user.foo", Role.USER);
    }

    @Test
    public void shouldReportIdentityAlreadyExistingOnCreation() {
        givenUserAdminRole("kura.user.foo", Role.USER);
        whenIdentityIsCreated("foo");

        thenNoExceptionIsThrown();
        thenIdentityServiceReportsUserCreated(false);
        thenUserAdminRoleExists("kura.user.foo", Role.USER);
    }

    @Test
    public void shouldDeleteIdentity() {
        givenUserAdminRole("kura.user.foo", Role.USER);

        whenIdentityIsDeleted("foo");

        thenIdentityServiceReportsUserDeleted(true);
        thenNoExceptionIsThrown();
        thenUserAdminRoleDoesNotExists("kura.user.foo");
    }

    @Test
    public void shouldReportIdentityNotExistingOnDeletion() {
        givenNoUserAdminRoleWithName("kura.user.foo");

        whenIdentityIsDeleted("foo");

        thenNoExceptionIsThrown();
        thenIdentityServiceReportsUserDeleted(false);
        thenUserAdminRoleDoesNotExists("kura.user.foo");
    }

    @Test
    public void shouldCreatePermission() {
        givenNoUserAdminRoleWithName("kura.permission.foo");
        whenPermissionIsCreated("foo");

        thenNoExceptionIsThrown();
        thenIdentityServiceReportsPermissionCreated(true);

        thenUserAdminRoleExists("kura.permission.foo", Role.GROUP);
    }

    @Test
    public void shouldReportPermissionAlreadyExistingOnCreation() {
        givenUserAdminRole("kura.permission.foo", Role.GROUP);
        whenPermissionIsCreated("foo");

        thenNoExceptionIsThrown();
        thenIdentityServiceReportsPermissionCreated(false);
        thenUserAdminRoleExists("kura.permission.foo", Role.GROUP);
    }

    @Test
    public void shouldDeletePermission() {
        givenUserAdminRole("kura.permission.foo", Role.GROUP);

        whenPermissionIsDeleted("foo");

        thenIdentityServiceReportsPermissionDeleted(true);
        thenNoExceptionIsThrown();
        thenUserAdminRoleDoesNotExists("kura.permission.foo");
    }

    @Test
    public void shouldReportPermissionNotExistingOnDeletion() {
        givenNoUserAdminRoleWithName("kura.permission.foo");

        whenPermissionIsDeleted("foo");

        thenNoExceptionIsThrown();
        thenIdentityServiceReportsPermissionDeleted(false);
        thenUserAdminRoleDoesNotExists("kura.permission.foo");
    }

    @Test
    public void shouldListExistingIdentityNames() {
        givenUserAdminUsers("kura.user.foo", "kura.user.bar", "other");
        givenUserAdminGroups("kura.permission.foo", "kura.permission.baz", "othergroup");

        whenIdentityNamesAreRetrieved();

        thenNoExceptionIsThrown();
        thenReturnedIdentityNamesAre("foo", "bar");
    }

    @Test
    public void shouldListExistingPermissionNames() {
        givenUserAdminUsers("kura.user.foo", "kura.user.bar", "other");
        givenUserAdminGroups("kura.permission.foo", "kura.permission.baz", "othergroup");

        whenPermissionNamesAreRetrieved();

        thenNoExceptionIsThrown();
        thenReturnedPermissionNamesAre("foo", "baz");
    }

    @Test
    public void shouldSetPermissions() {
        givenUserAdminUsers("kura.user.foo");
        givenUserAdminGroups("kura.permission.foo", "kura.permission.bar", "kura.permission.baz",
                "kura.permission.other", "other");

        whenIdentityConfigurationIsUpdated(new IdentityConfiguration("foo",
                singletonList(new AssignedPermissions(set(new Permission("foo"), new Permission("bar"))))));

        thenNoExceptionIsThrown();
        thenUserAdminGroupContainsMember("kura.permission.foo", "kura.user.foo");
        thenUserAdminGroupContainsMember("kura.permission.bar", "kura.user.foo");
        thenUserAdminGroupDoesNotContainMember("kura.permission.baz", "kura.user.foo");
        thenUserAdminGroupDoesNotContainMember("kura.permission.other", "kura.user.foo");
    }

    @Test
    public void shouldFailToSetPermissionsIfPermissionDoesNotExist() {
        givenUserAdminUsers("kura.user.foo");
        givenUserAdminGroups("kura.permission.foo", "kura.permission.bar", "kura.permission.baz",
                "kura.permission.other", "other");

        whenIdentityConfigurationIsUpdated(new IdentityConfiguration("foo",
                singletonList(new AssignedPermissions(set(new Permission("foo"), new Permission("notexisting"))))));

        thenExceptionThrown(KuraException.class);
    }

    @Test
    public void shouldConsiderConfigurationInvalidIfPermissionDoesNotExist() {
        givenUserAdminUsers("kura.user.foo");
        givenUserAdminGroups("kura.permission.foo", "kura.permission.bar", "kura.permission.baz",
                "kura.permission.other", "other");

        whenIdentityConfigurationIsValidated(new IdentityConfiguration("foo",
                singletonList(new AssignedPermissions(set(new Permission("foo"), new Permission("notexisting"))))));

        thenExceptionThrown(KuraException.class);
    }

    @Test
    public void shouldRemovePermissions() {
        givenUserAdminUsers("kura.user.foo");
        givenUserAdminGroups("kura.permission.foo", "kura.permission.bar", "kura.permission.baz",
                "kura.permission.other", "other");
        givenUserAdminBasicMember("kura.permission.baz", "kura.user.foo");

        whenIdentityConfigurationIsUpdated(new IdentityConfiguration("foo",
                singletonList(new AssignedPermissions(set(new Permission("foo"), new Permission("bar"))))));

        thenNoExceptionIsThrown();
        thenUserAdminGroupContainsMember("kura.permission.foo", "kura.user.foo");
        thenUserAdminGroupContainsMember("kura.permission.bar", "kura.user.foo");
        thenUserAdminGroupDoesNotContainMember("kura.permission.baz", "kura.user.foo");
        thenUserAdminGroupDoesNotContainMember("kura.permission.other", "kura.user.foo");
    }

    @Test
    public void shouldReturnAssignedPermissions() {
        givenUserAdminUsers("kura.user.foo");
        givenUserAdminGroups("kura.permission.foo", "kura.permission.bar", "kura.permission.baz",
                "kura.permission.other", "other");
        givenUserAdminBasicMember("kura.permission.foo", "kura.user.foo");
        givenUserAdminBasicMember("kura.permission.bar", "kura.user.foo");

        whenIdentityConfigurationIsRetrieved("foo", set(AssignedPermissions.class));

        thenNoExceptionIsThrown();
        thenIdentityConfigurationEquals(new IdentityConfiguration("foo",
                singletonList(new AssignedPermissions(set(new Permission("foo"), new Permission("bar"))))));
    }

    @Test
    public void shouldSetUserPassword() {
        givenUserAdminUsers("kura.user.foo");
        givenPasswordHash("foobar");

        whenIdentityConfigurationIsUpdated(new IdentityConfiguration("foo",
                singletonList(new PasswordConfiguration(false, true, Optional.of(lastPasswordHash())))));

        thenNoExceptionIsThrown();
        thenUserAdminCredentialIs("kura.user.foo", "kura.password", sha256("foobar"));
    }

    @Test
    public void shouldNotChangeUserPassword() {
        givenUserAdminUsers("kura.user.foo");
        givenUserAdminCredential("kura.user.foo", "kura.password", sha256("foobar"));

        whenIdentityConfigurationIsUpdated(new IdentityConfiguration("foo",
                singletonList(new PasswordConfiguration(false, true, Optional.empty()))));

        thenNoExceptionIsThrown();
        thenUserAdminCredentialIs("kura.user.foo", "kura.password", sha256("foobar"));
    }

    @Test
    public void shouldRemoveUserPassword() {
        givenUserAdminUsers("kura.user.foo");
        givenUserAdminCredential("kura.user.foo", "kura.password", sha256("foobar"));

        whenIdentityConfigurationIsUpdated(new IdentityConfiguration("foo",
                singletonList(new PasswordConfiguration(false, false, Optional.empty()))));

        thenNoExceptionIsThrown();
        thenUserAdminCredentialIsNotSet("kura.user.foo", "kura.password");
    }

    @Test
    public void shouldSetNeedPasswordChange() {
        givenUserAdminUsers("kura.user.foo");
        givenPasswordHash("foobar");

        whenIdentityConfigurationIsUpdated(new IdentityConfiguration("foo",
                singletonList(new PasswordConfiguration(true, true, Optional.of(lastPasswordHash())))));

        thenNoExceptionIsThrown();
        thenUserAdminCredentialIs("kura.user.foo", "kura.password", sha256("foobar"));
        thenUserAdminPropertyIs("kura.user.foo", "kura.need.password.change", "true");
    }

    @Test
    public void shouldUnsetNeedPasswordChange() {
        givenUserAdminUsers("kura.user.foo");
        givenUserAdminProperty("kura.user.foo", "kura.need.password.change", "true");
        givenPasswordHash("foobar");

        whenIdentityConfigurationIsUpdated(new IdentityConfiguration("foo",
                singletonList(new PasswordConfiguration(false, true, Optional.of(lastPasswordHash())))));

        thenNoExceptionIsThrown();
        thenUserAdminCredentialIs("kura.user.foo", "kura.password", sha256("foobar"));
        thenUserAdminPropertyIsNotSet("kura.user.foo", "kura.need.password.change");
    }

    @Test
    public void shouldReturnNeedPasswordChangeTrue() {
        givenUserAdminUsers("kura.user.foo");
        givenPasswordHash("foobar");
        givenUserAdminProperty("kura.user.foo", "kura.need.password.change", "true");

        whenIdentityConfigurationIsRetrieved("foo", set(PasswordConfiguration.class));

        thenNoExceptionIsThrown();
        thenIdentityConfigurationEquals(new IdentityConfiguration("foo",
                singletonList(new PasswordConfiguration(true, false, Optional.empty()))));
    }

    @Test
    public void shouldReturnNeedPasswordChangeFalse() {
        givenUserAdminUsers("kura.user.foo");
        givenPasswordHash("foobar");

        whenIdentityConfigurationIsRetrieved("foo", set(PasswordConfiguration.class));

        thenNoExceptionIsThrown();
        thenIdentityConfigurationEquals(new IdentityConfiguration("foo",
                singletonList(new PasswordConfiguration(false, false, Optional.empty()))));
    }

    @Test
    public void shouldReturnPasswordHash() {
        givenUserAdminUsers("kura.user.foo");
        givenUserAdminCredential("kura.user.foo", "kura.password", sha256("foobar"));
        givenPasswordHash("foobar");

        whenIdentityConfigurationIsRetrieved("foo", set(PasswordConfiguration.class));

        thenNoExceptionIsThrown();
        thenIdentityConfigurationEquals(new IdentityConfiguration("foo",
                singletonList(new PasswordConfiguration(false, true, Optional.of(lastPasswordHash())))));
    }

    @Test
    public void shouldReturnDefaultConfiguationFromExtension() {
        givenMockIdentityConfigurationExtension("test.extension");
        givenExtensionDefaultConfiguration("test.extension", "foo",
                TestComponentConfiguration.forPid("test.extension"));

        whenIdentityDefaultConfigurationIsRetrieved("foo", set(AdditionalConfigurations.class));

        thenNoExceptionIsThrown();
        thenIdentityDefaultConfigurationEquals(new IdentityConfiguration("foo", singletonList(
                new AdditionalConfigurations(singletonList(TestComponentConfiguration.forPid("test.extension"))))));
    }

    @Test
    public void shouldReturnConfiguationFromExtension() {
        givenMockIdentityConfigurationExtension("test.extension");
        givenExtensionConfiguration("test.extension", "foo", TestComponentConfiguration.forPid("test.extension"));
        givenUserAdminUsers("kura.user.foo");

        whenIdentityConfigurationIsRetrieved("foo", set(AdditionalConfigurations.class));

        thenNoExceptionIsThrown();
        thenIdentityConfigurationEquals(new IdentityConfiguration("foo", singletonList(
                new AdditionalConfigurations(singletonList(TestComponentConfiguration.forPid("test.extension"))))));
    }

    @Test
    public void shouldConsiderConfigurationInvalidIfExtensionIsNotRegistered() {
        givenUserAdminUsers("kura.user.foo");

        whenIdentityConfigurationIsValidated(new IdentityConfiguration("foo", singletonList(
                new AdditionalConfigurations(singletonList(TestComponentConfiguration.forPid("nonexisting"))))));

        thenExceptionThrown(KuraException.class);
    }

    @Test
    public void shouldConsiderConfigurationInvalidIfExtensionValidationFails() {
        givenMockIdentityConfigurationExtension("test.extension");
        givenExtensionReturningValidationFailure("test.extension", "foobar");

        whenIdentityConfigurationIsValidated(new IdentityConfiguration("foobar", singletonList(
                new AdditionalConfigurations(singletonList(TestComponentConfiguration.forPid("test.extension"))))));

        thenExceptionThrown(KuraException.class);
    }

    @Test
    public void shouldSupportUpdatingAdditionaConfiguration() {
        givenMockIdentityConfigurationExtension("test.extension");
        givenUserAdminUsers("kura.user.foo");

        whenIdentityConfigurationIsUpdated(new IdentityConfiguration("foo", singletonList(
                new AdditionalConfigurations(singletonList(TestComponentConfiguration.forPid("test.extension"))))));
        whenIdentityConfigurationIsRetrieved("foo", set(AdditionalConfigurations.class));

        thenNoExceptionIsThrown();
        thenIdentityConfigurationEquals(new IdentityConfiguration("foo", singletonList(
                new AdditionalConfigurations(singletonList(TestComponentConfiguration.forPid("test.extension"))))));
    }

    private final UserAdmin userAdmin;
    private final CryptoService cryptoService;
    private final IdentityService identityService;

    private Optional<Exception> exception = Optional.empty();
    private Optional<Object> result = Optional.empty();
    private Optional<PasswordHash> passwordHash = Optional.empty();
    private final Map<String, MockExtensionHolder> extensions = new HashMap<>();

    public IdentityServiceImplTest() {
        try {
            this.userAdmin = ServiceUtil.trackService(UserAdmin.class, Optional.empty()).get(30, TimeUnit.SECONDS);
            this.cryptoService = ServiceUtil.trackService(CryptoService.class, Optional.empty()).get(30,
                    TimeUnit.SECONDS);
            this.identityService = ServiceUtil.trackService(IdentityService.class, Optional.empty()).get(30,
                    TimeUnit.SECONDS);

            givenNoUserAdminRoles();
        } catch (Exception e) {
            fail("failed to setup test environment");
            throw new IllegalStateException("unreachable");
        }
    }

    @After
    public void cleanup() {
        for (final MockExtensionHolder reg : this.extensions.values()) {
            reg.registration.unregister();
        }
    }

    private void givenMockIdentityConfigurationExtension(final String pid) {
        this.extensions.put(pid, new MockExtensionHolder(pid));
    }

    private void givenExtensionConfiguration(final String extensionPid, final String identity,
            final ComponentConfiguration config) {
        this.extensions.get(extensionPid).configurations.put(identity, config);
    }

    private void givenExtensionDefaultConfiguration(final String extensionPid, final String identity,
            final ComponentConfiguration config) {
        this.extensions.get(extensionPid).defaultConfigurations.put(identity, config);
    }

    private void givenExtensionReturningValidationFailure(final String extensionPid, final String identity) {
        this.extensions.get(extensionPid).throwOnIdentityValidation(identity);
    }

    private void givenUserAdminProperty(final String role, final String key, final String value) {
        this.userAdmin.getRole(role).getProperties().put(key, value);
    }

    private void givenUserAdminCredential(final String user, final String key, final String value) {
        ((User) this.userAdmin.getRole(user)).getCredentials().put(key, value);
    }

    private void givenUserAdminBasicMember(final String group, final String member) {
        ((Group) this.userAdmin.getRole(group)).addMember(this.userAdmin.getRole(member));
    }

    private void givenNoUserAdminRoleWithName(final String name) {
        this.userAdmin.removeRole(name);
    }

    private void givenUserAdminRole(final String name, final int type) {
        this.userAdmin.createRole(name, type);
    }

    private void givenNoUserAdminRoles() {
        try {
            final Role[] roles = this.userAdmin.getRoles(null);

            if (roles != null) {
                for (final Role role : roles) {
                    this.userAdmin.removeRole(role.getName());
                }
            }
        } catch (InvalidSyntaxException e) {
            fail("failed to get existing roles");
        }
    }

    private void givenUserAdminUsers(final String... users) {
        for (final String user : users) {
            this.userAdmin.createRole(user, Role.USER);
        }
    }

    private void givenUserAdminGroups(final String... users) {
        for (final String user : users) {
            this.userAdmin.createRole(user, Role.GROUP);
        }
    }

    private void givenPasswordHash(final String password) {
        try {
            this.passwordHash = Optional.of(this.identityService.computePasswordHash(password.toCharArray()));
        } catch (Exception e) {
            fail("failed to compute password hash");
        }
    }

    private void whenIdentityIsCreated(final String name) {
        call(() -> this.identityService.createIdentity(name));
    }

    private void whenIdentityIsDeleted(final String name) {
        call(() -> this.identityService.deleteIdentity(name));
    }

    private void whenPermissionIsCreated(final String name) {
        call(() -> this.identityService.createPermission(new Permission(name)));
    }

    private void whenPermissionIsDeleted(final String name) {
        call(() -> this.identityService.deletePermission(new Permission(name)));
    }

    private void whenIdentityNamesAreRetrieved() {
        call(() -> this.identityService.getIdentitiesConfiguration(Collections.emptySet()));
    }

    private void whenPermissionNamesAreRetrieved() {
        call(() -> this.identityService.getPermissions());
    }

    private void whenIdentityConfigurationIsUpdated(final IdentityConfiguration identityConfiguration) {
        callVoid(() -> {
            this.identityService.updateIdentityConfigurations(Collections.singleton(identityConfiguration));
            return null;
        });
    }

    private void whenIdentityConfigurationIsValidated(final IdentityConfiguration identityConfiguration) {
        callVoid(() -> {
            this.identityService.validateIdentityConfigurations(singletonList(identityConfiguration));
            return null;
        });
    }

    private void whenIdentityConfigurationIsRetrieved(final String identity,
            final Set<Class<? extends IdentityConfigurationComponent>> components) {
        call(() -> this.identityService.getIdentityConfiguration(identity, components));
    }

    private void whenIdentityDefaultConfigurationIsRetrieved(final String identity,
            final Set<Class<? extends IdentityConfigurationComponent>> components) {
        call(() -> this.identityService.getIdentityDefaultConfiguration(identity, components));
    }

    private void thenUserAdminPropertyIs(final String role, final String key, final String value) {
        assertEquals(value, this.userAdmin.getRole(role).getProperties().get(key));
    }

    private void thenUserAdminPropertyIsSet(final String role, final String key) {
        assertNotNull(this.userAdmin.getRole(role).getProperties().get(key));
    }

    private void thenUserAdminPropertyIsNotSet(final String role, final String key) {
        assertNull(this.userAdmin.getRole(role).getProperties().get(key));
    }

    private void thenUserAdminCredentialIs(final String user, final String key, final String value) {
        assertEquals(value, ((User) this.userAdmin.getRole(user)).getCredentials().get(key));
    }

    private void thenUserAdminCredentialIsSet(final String user, final String key) {
        assertNotNull(((User) this.userAdmin.getRole(user)).getCredentials().get(key));
    }

    private void thenUserAdminCredentialIsNotSet(final String user, final String key) {
        assertNull(((User) this.userAdmin.getRole(user)).getCredentials().get(key));
    }

    private void thenUserAdminGroupContainsMember(final String group, final String member) {
        final List<Role> members = getBasicMembers(group);

        assertTrue(members.stream().filter(m -> Objects.equals(member, m.getName())).findAny().isPresent());
    }

    private void thenUserAdminGroupDoesNotContainMember(final String group, final String member) {
        final List<Role> members = getBasicMembers(group);

        assertFalse(members.stream().filter(m -> Objects.equals(member, m.getName())).findAny().isPresent());
    }

    private void thenIdentityServiceReportsUserDeleted(final boolean deleted) {
        assertEquals(Optional.of(deleted), this.result);
    }

    private void thenIdentityServiceReportsPermissionDeleted(final boolean deleted) {
        assertEquals(Optional.of(deleted), this.result);
    }

    private void thenIdentityServiceReportsUserCreated(final boolean created) {
        assertEquals(Optional.of(created), this.result);
    }

    private void thenIdentityServiceReportsPermissionCreated(final boolean created) {
        assertEquals(Optional.of(created), this.result);
    }

    private void thenIdentityConfigurationEquals(final IdentityConfiguration identityConfiguration) {
        assertEquals(Optional.of(identityConfiguration), expectResult(Optional.class));
    }

    private void thenIdentityDefaultConfigurationEquals(final IdentityConfiguration identityConfiguration) {
        assertEquals(identityConfiguration, expectResult(IdentityConfiguration.class));
    }

    @SuppressWarnings("unchecked")
    private void thenReturnedIdentityNamesAre(final String... names) {
        assertEquals(Arrays.stream(names).collect(Collectors.toSet()),
                ((List<IdentityConfiguration>) expectResult(List.class)).stream().map(IdentityConfiguration::getName)
                        .collect(Collectors.toSet()));
    }

    @SuppressWarnings("unchecked")
    private void thenReturnedPermissionNamesAre(final String... names) {
        assertEquals(Arrays.stream(names).collect(Collectors.toSet()), ((Set<Permission>) expectResult(Set.class))
                .stream().map(Permission::getName).collect(Collectors.toSet()));
    }

    private void thenUserAdminRoleExists(final String name, final int type) {
        assertNotNull("role " + name + " does not exist", this.userAdmin.getRole(name));
        assertEquals(type, this.userAdmin.getRole(name).getType());
    }

    private void thenUserAdminRoleDoesNotExists(final String name) {
        assertNull("user " + name + " does exists", this.userAdmin.getRole(name));
    }

    private void thenNoExceptionIsThrown() {
        assertEquals(Optional.empty(), this.exception);
    }

    private void thenExceptionThrown(final Class<? extends Exception> clazz) {
        assertEquals(Optional.of(clazz), this.exception.map(Object::getClass));
    }

    public <T> T expectResult(final Class<T> ty) {
        return this.result.filter(ty::isInstance).map(ty::cast)
                .orElseThrow(() -> new IllegalStateException("unexpected return type"));
    }

    private List<Role> getBasicMembers(final String group) {
        return Optional.ofNullable(((Group) this.userAdmin.getRole(group)).getMembers()).map(Arrays::asList)
                .orElseGet(Collections::emptyList);
    }

    private PasswordHash lastPasswordHash() {
        return this.passwordHash.orElseThrow(() -> new IllegalStateException("no password hash has been computed"));
    }

    private String sha256(final String key) {
        try {
            return this.cryptoService.sha256Hash(key);
        } catch (Exception e) {
            fail("cannot compute sha256 with cryptoservice");
            throw new IllegalStateException("unreachable");
        }
    }

    @SafeVarargs
    private final <T> Set<T> set(final T... items) {
        return new HashSet<>(Arrays.asList(items));
    }

    private void call(final Callable<?> callable) {
        try {
            this.result = Optional.of(callable.call());
        } catch (Exception e) {
            this.exception = Optional.of(e);
        }
    }

    private void callVoid(final Callable<Void> callable) {
        try {
            callable.call();
        } catch (Exception e) {
            this.exception = Optional.of(e);
        }
    }

    private static class MockExtensionHolder {

        private final ServiceRegistration<IdentityConfigurationExtension> registration;
        private final IdentityConfigurationExtension extension;

        private final Map<String, ComponentConfiguration> defaultConfigurations = new HashMap<>();
        private final Map<String, ComponentConfiguration> configurations = new HashMap<>();

        public MockExtensionHolder(final String pid) {
            this.extension = Mockito.mock(IdentityConfigurationExtension.class);

            try {
                Mockito.when(this.extension.getConfiguration(ArgumentMatchers.anyString())).thenAnswer(a -> {
                    final String name = a.getArgument(0, String.class);

                    return Optional.ofNullable(this.configurations.get(name));
                });

                Mockito.when(this.extension.getDefaultConfiguration(ArgumentMatchers.anyString())).thenAnswer(a -> {
                    final String name = a.getArgument(0, String.class);

                    return Optional.ofNullable(this.defaultConfigurations.get(name));
                });

                Mockito.doAnswer(i -> {
                    final String identityName = i.getArgument(0, String.class);
                    final ComponentConfiguration connfig = i.getArgument(1, ComponentConfiguration.class);

                    this.configurations.put(identityName, connfig);

                    return (Void) null;
                }).when(this.extension).updateConfiguration(ArgumentMatchers.anyString(), ArgumentMatchers.any());

            } catch (KuraException e) {
                // no need
            }

            final Dictionary<String, Object> properties = new Hashtable<>();
            properties.put("kura.service.pid", pid);

            this.registration = FrameworkUtil.getBundle(IdentityServiceImplTest.class).getBundleContext()
                    .registerService(IdentityConfigurationExtension.class, this.extension, properties);
        }

        void throwOnIdentityValidation(final String identityName) {
            try {
                Mockito.doThrow(new KuraException(KuraErrorCode.INVALID_PARAMETER)).when(this.extension)
                        .validateConfiguration(ArgumentMatchers.eq(identityName), ArgumentMatchers.any());
            } catch (KuraException e) {
                // no need
            }
        }
    }

    private static class TestComponentConfiguration implements ComponentConfiguration {

        private final String pid;
        private final OCD ocd;
        private final Map<String, Object> properties;

        public TestComponentConfiguration(String pid, OCD ocd, Map<String, Object> properties) {
            this.pid = pid;
            this.ocd = ocd;
            this.properties = properties;
        }

        @Override
        public String getPid() {
            return this.pid;
        }

        @Override
        public OCD getDefinition() {
            return this.ocd;
        }

        @Override
        public Map<String, Object> getConfigurationProperties() {
            return this.properties;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.ocd, this.pid, this.properties);
        }

        static TestComponentConfiguration forPid(final String pid) {
            return new TestComponentConfiguration("test.extension", null, Collections.singletonMap("foo", "bar"));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TestComponentConfiguration other = (TestComponentConfiguration) obj;
            return Objects.equals(this.ocd, other.ocd) && Objects.equals(this.pid, other.pid)
                    && Objects.equals(this.properties, other.properties);
        }

        @Override
        public String toString() {
            return "TestComponentConfiguration [pid=" + this.pid + ", ocd=" + this.ocd + ", properties="
                    + this.properties + "]";
        }

    }
}
