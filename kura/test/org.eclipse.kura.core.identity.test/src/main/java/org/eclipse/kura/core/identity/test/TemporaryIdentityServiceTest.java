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
package org.eclipse.kura.core.identity.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.identity.Permission;
import org.eclipse.kura.identity.TemporaryIdentityService;
import org.junit.Test;
import org.osgi.service.useradmin.UserAdmin;

/**
 * Test class for TemporaryIdentityService functionality following BDD patterns
 */
public class TemporaryIdentityServiceTest extends IdentityServiceTestBase {

    private final UserAdmin userAdmin;
    private final CryptoService cryptoService;
    private final TemporaryIdentityService temporaryIdentityService;

    private Optional<Exception> exception = Optional.empty();
    private Optional<Object> result = Optional.empty();

    private String identityName;
    private Set<Permission> permissions;
    private String temporaryToken;

    public TemporaryIdentityServiceTest() {
        super();
        try {
            this.userAdmin = ServiceUtil.trackService(UserAdmin.class, Optional.empty()).get(30, TimeUnit.SECONDS);
            this.cryptoService = ServiceUtil.trackService(CryptoService.class, Optional.empty()).get(30,
                    TimeUnit.SECONDS);
            this.temporaryIdentityService = ServiceUtil.trackService(TemporaryIdentityService.class, Optional.empty())
                    .get(30, TimeUnit.SECONDS);

            givenNoUserAdminRoles();
        } catch (Exception e) {
            fail("failed to setup test environment");
            throw new IllegalStateException("unreachable");
        }
    }

    @Test
    public void shouldCreateTemporaryIdentity() {
        givenIdentityName("container_test");
        givenPermissions("rest.asset.read", "rest.configuration.write");
        givenExistingPermissions("rest.asset.read", "rest.configuration.write");

        whenTemporaryIdentityIsCreated();

        thenNoExceptionIsThrown();
        thenTemporaryTokenIsNotNull();
        thenTemporaryTokenStartsWithPrefix("kura-temp-");
        thenTemporaryTokenCanBeValidated();
    }

    @Test
    public void shouldValidateTemporaryToken() {
        givenIdentityName("container_test");
        givenPermissions("rest.asset.read");
        givenExistingPermissions("rest.asset.read");
        givenExistingTemporaryIdentity();

        whenTemporaryTokenIsValidated();

        thenNoExceptionIsThrown();
        thenValidatedIdentityNameMatches();
    }

    @Test
    public void shouldCheckTemporaryPermission() {
        givenIdentityName("container_test");
        givenPermissions("rest.asset.read");
        givenExistingPermissions("rest.asset.read");
        givenExistingTemporaryIdentity();

        whenTemporaryPermissionIsChecked("rest.asset.read");

        thenNoExceptionIsThrown();
    }

    @Test
    public void shouldFailToCheckNonAssignedPermission() {
        givenIdentityName("container_test");
        givenPermissions("rest.asset.read");
        givenExistingPermissions("rest.asset.read", "rest.configuration.write");
        givenExistingTemporaryIdentity();

        whenTemporaryPermissionIsChecked("rest.configuration.write");

        thenExceptionIsThrown(KuraException.class);
    }

    @Test
    public void shouldDeleteTemporaryIdentity() {
        givenIdentityName("container_test");
        givenPermissions("rest.asset.read");
        givenExistingPermissions("rest.asset.read");
        givenExistingTemporaryIdentity();

        whenTemporaryIdentityIsDeleted();

        thenNoExceptionIsThrown();
        thenTemporaryIdentityServiceReportsDeleted(true);
        thenTemporaryTokenIsNoLongerValid();
    }

    @Test
    public void shouldReturnFalseWhenDeletingNonExistentTemporaryIdentity() {
        whenTemporaryIdentityIsDeleted("invalid-token");

        thenNoExceptionIsThrown();
        thenTemporaryIdentityServiceReportsDeleted(false);
    }

    @Test
    public void shouldFailToCreateTemporaryIdentityWithNonexistentPermission() {
        givenIdentityName("container_test");
        givenPermissions("nonexistent.permission");

        whenTemporaryIdentityIsCreated();

        thenExceptionIsThrown(KuraException.class);
    }

    @Test
    public void shouldFailToValidateInvalidToken() {
        whenTemporaryTokenIsValidated("invalid-token");

        thenExceptionIsThrown(KuraException.class);
    }

    // Given methods
    private void givenIdentityName(final String name) {
        this.identityName = name;
    }

    private void givenPermissions(final String... permissionNames) {
        this.permissions = new HashSet<>();
        for (String permissionName : permissionNames) {
            this.permissions.add(new Permission(permissionName));
        }
    }

    private void givenExistingPermissions(final String... permissionNames) {
        for (String permissionName : permissionNames) {
            this.userAdmin.createRole("kura.permission." + permissionName, org.osgi.service.useradmin.Role.GROUP);
        }
    }

    private void givenExistingTemporaryIdentity() {
        try {
            this.temporaryToken = this.temporaryIdentityService.createTemporaryIdentity(this.identityName,
                    this.permissions);
        } catch (KuraException e) {
            fail("Failed to create temporary identity for test setup");
        }
    }

    private void givenNoUserAdminRoles() {
        try {
            final org.osgi.service.useradmin.Role[] roles = this.userAdmin.getRoles(null);

            if (roles != null) {
                for (final org.osgi.service.useradmin.Role role : roles) {
                    this.userAdmin.removeRole(role.getName());
                }
            }
        } catch (Exception e) {
            fail("failed to get existing roles");
        }
    }

    // When methods
    private void whenTemporaryIdentityIsCreated() {
        call(() -> this.temporaryIdentityService.createTemporaryIdentity(this.identityName, this.permissions));
    }

    private void whenTemporaryIdentityIsDeleted() {
        call(() -> this.temporaryIdentityService.deleteTemporaryIdentity(this.temporaryToken));
    }

    private void whenTemporaryIdentityIsDeleted(final String token) {
        call(() -> this.temporaryIdentityService.deleteTemporaryIdentity(token));
    }

    private void whenTemporaryTokenIsValidated() {
        call(() -> this.temporaryIdentityService.validateTemporaryToken(this.temporaryToken));
    }

    private void whenTemporaryTokenIsValidated(final String token) {
        call(() -> this.temporaryIdentityService.validateTemporaryToken(token));
    }

    private void whenTemporaryPermissionIsChecked(final String permissionName) {
        callVoid(() -> {
            this.temporaryIdentityService.checkTemporaryPermission(this.temporaryToken, new Permission(permissionName));
            return null;
        });
    }

    // Then methods
    private void thenNoExceptionIsThrown() {
        assertEquals(Optional.empty(), this.exception);
    }

    private void thenExceptionIsThrown(final Class<? extends Exception> clazz) {
        assertEquals(Optional.of(clazz), this.exception.map(Object::getClass));
    }

    private void thenTemporaryTokenIsNotNull() {
        assertNotNull("Token should not be null", expectResult(String.class));
        this.temporaryToken = expectResult(String.class);
    }

    private void thenTemporaryTokenStartsWithPrefix(final String prefix) {
        assertTrue("Token should start with prefix " + prefix, expectResult(String.class).startsWith(prefix));
    }

    private void thenTemporaryTokenCanBeValidated() {
        try {
            String validatedName = this.temporaryIdentityService.validateTemporaryToken(expectResult(String.class));
            assertEquals("Identity name should match", this.identityName, validatedName);
        } catch (KuraException e) {
            fail("Token validation should not fail: " + e.getMessage());
        }
    }

    private void thenValidatedIdentityNameMatches() {
        assertEquals("Validated identity name should match", this.identityName, expectResult(String.class));
    }

    private void thenTemporaryIdentityServiceReportsDeleted(final boolean deleted) {
        assertEquals(Optional.of(deleted), this.result);
    }

    private void thenTemporaryTokenIsNoLongerValid() {
        try {
            this.temporaryIdentityService.validateTemporaryToken(this.temporaryToken);
            fail("Token should no longer be valid after deletion");
        } catch (KuraException e) {
            // Expected - token should be invalid
        }
    }

    // Helper methods
    private <T> T expectResult(final Class<T> ty) {
        return this.result.filter(ty::isInstance).map(ty::cast)
                .orElseThrow(() -> new IllegalStateException("unexpected return type"));
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
}
