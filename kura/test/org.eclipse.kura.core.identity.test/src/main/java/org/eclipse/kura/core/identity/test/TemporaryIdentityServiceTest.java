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

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.identity.IdentityServiceImpl;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.identity.Permission;
import org.eclipse.kura.identity.PasswordStrengthVerificationService;
import org.eclipse.kura.identity.TemporaryIdentityService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

import static org.mockito.Mockito.*;

/**
 * Test class for TemporaryIdentityService functionality
 */
public class TemporaryIdentityServiceTest {

    @Mock
    private UserAdmin userAdmin;
    
    @Mock
    private CryptoService cryptoService;
    
    @Mock
    private PasswordStrengthVerificationService passwordStrengthVerificationService;
    
    @Mock
    private Group permissionGroup;
    
    private IdentityServiceImpl identityService;
    private TemporaryIdentityService temporaryIdentityService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        
        identityService = new IdentityServiceImpl();
        identityService.setUserAdmin(userAdmin);
        identityService.setCryptoService(cryptoService);
        identityService.setPasswordStrengthVerificationService(passwordStrengthVerificationService);
        identityService.activate();
        
        temporaryIdentityService = identityService;
        
        // Mock permission existence
        when(userAdmin.getRole("kura.permission.rest.asset.read")).thenReturn(permissionGroup);
        when(userAdmin.getRole("kura.permission.rest.configuration.write")).thenReturn(permissionGroup);
    }

    @Test
    public void testCreateTemporaryIdentity() throws KuraException {
        String identityName = "container-test";
        Set<Permission> permissions = new HashSet<>();
        permissions.add(new Permission("rest.asset.read"));
        permissions.add(new Permission("rest.configuration.write"));
        
        String token = temporaryIdentityService.createTemporaryIdentity(identityName, permissions);
        
        assertNotNull("Token should not be null", token);
        assertTrue("Token should start with prefix", token.startsWith("kura-temp-"));
        
        // Verify the token can be validated
        String validatedName = temporaryIdentityService.validateTemporaryToken(token);
        assertEquals("Identity name should match", identityName, validatedName);
    }

    @Test
    public void testCheckTemporaryPermission() throws KuraException {
        String identityName = "container-test";
        Set<Permission> permissions = new HashSet<>();
        permissions.add(new Permission("rest.asset.read"));
        
        String token = temporaryIdentityService.createTemporaryIdentity(identityName, permissions);
        
        // This should succeed
        temporaryIdentityService.checkTemporaryPermission(token, new Permission("rest.asset.read"));
        
        // This should fail
        try {
            temporaryIdentityService.checkTemporaryPermission(token, new Permission("rest.configuration.write"));
            fail("Should have thrown exception for missing permission");
        } catch (KuraException e) {
            // Expected
        }
    }

    @Test
    public void testDeleteTemporaryIdentity() throws KuraException {
        String identityName = "container-test";
        Set<Permission> permissions = new HashSet<>();
        permissions.add(new Permission("rest.asset.read"));
        
        String token = temporaryIdentityService.createTemporaryIdentity(identityName, permissions);
        
        // Should be able to validate initially
        temporaryIdentityService.validateTemporaryToken(token);
        
        // Delete the identity
        boolean deleted = temporaryIdentityService.deleteTemporaryIdentity(token);
        assertTrue("Identity should have been deleted", deleted);
        
        // Should no longer be able to validate
        try {
            temporaryIdentityService.validateTemporaryToken(token);
            fail("Should have thrown exception for invalid token");
        } catch (KuraException e) {
            // Expected
        }
    }

    @Test(expected = KuraException.class)
    public void testInvalidPermission() throws KuraException {
        String identityName = "container-test";
        Set<Permission> permissions = new HashSet<>();
        permissions.add(new Permission("nonexistent.permission"));
        
        // This should fail because the permission doesn't exist
        temporaryIdentityService.createTemporaryIdentity(identityName, permissions);
    }
}