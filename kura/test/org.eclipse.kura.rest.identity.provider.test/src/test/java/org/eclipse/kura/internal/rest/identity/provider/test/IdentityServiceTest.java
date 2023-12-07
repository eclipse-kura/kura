/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.identity.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.rest.identity.provider.IdentityService;
import org.eclipse.kura.internal.rest.identity.provider.dto.UserDTO;
import org.eclipse.kura.util.validation.ValidatorOptions;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

@SuppressWarnings("restriction")
public class IdentityServiceTest {

    private static final String KURA_WEB_CONSOLE_SERVICE_PID = "org.eclipse.kura.web.Console";
    private static final String USER_ROLE_NAME_PREFIX = "kura.user.";
    private static final String PERMISSION_ROLE_NAME_PREFIX = "kura.permission.";

    private IdentityService identityService;
    private Exception occurredException;
    private UserDTO user;
    private UserDTO newUser;

    private CryptoService cryptoService;
    private UserAdmin userAdmin;
    private ConfigurationService configurationService;

    private Set<String> definedPermissions;
    private Set<UserDTO> userConfig;
    private boolean isPasswordValid;
    private ValidatorOptions validatorOptions;

    @Test
    public void shouldCreateAnUser() throws KuraException {
        givenIdentityService();

        whenCreatingUser(new UserDTO("testuser", Collections.emptySet(), true, false, "testpassw"));

        thenNoExceptionOccurred();
        thenUserIsCreated();
    }

    @Test
    public void shouldDeleteExistingUser() {
        givenIdentityService();
        givenExistingUser(new UserDTO("testuser", Collections.emptySet(), true, false, "testpassw"));

        whenDeleting("testuser");

        thenNoExceptionOccurred();
        thenUserIsDeleted();
    }

    @Test
    public void shouldGetDefinedPermissions() throws InvalidSyntaxException {
        givenIdentityService();
        givenExistingPermissions(new PermissionRole("perm1"), new PermissionRole("perm2"));

        whenGettingDefinedPermissions();

        thenNoExceptionOccurred();
        thenPermissionsAre("perm1", "perm2");

    }

    @Test
    public void shoulGetUser() {
        givenIdentityService();
        givenExistingUser(new UserDTO("testuser", Collections.emptySet(), true, false, "testpassw"));

        whenGettingUser("testuser");

        thenNoExceptionOccurred();
        thenUserIs("testuser");

    }

    private void whenGettingUser(String username) {
        try {
            this.user = this.identityService.getUser(username);
        } catch (KuraException e) {
            this.occurredException = e;
        }
    }

    @Test
    public void shouldGetUserConfig() {
        givenIdentityService();
        givenExistingUser(new UserDTO("testuser", Collections.emptySet(), true, false, "testpassw"));

        whenGetUserConfig();

        thenNoExceptionOccurred();
        thenConfigContains(new UserDTO("testuser", Collections.emptySet(), true, false, "testpassw"));
    }

    @Test
    public void shouldGetValidatorOptions() {
        givenIdentityService();

        whenGettingValidatorOptions();

        thenNoExceptionOccurred();
        thenValidatorOptionsAre(8, false, false, false);

    }

    private void thenValidatorOptionsAre(int passwordMinimumLength, boolean passwordRequireDigits,
            boolean passwordRequireBothCases, boolean passwordRequireSpecialChars) {

        assertEquals(passwordMinimumLength, this.validatorOptions.isPasswordMinimumLength());
        assertEquals(passwordRequireDigits, this.validatorOptions.isPasswordRequireDigits());
        assertEquals(passwordRequireBothCases, this.validatorOptions.isPasswordRequireBothCases());
        assertEquals(passwordRequireSpecialChars, this.validatorOptions.isPasswordRequireSpecialChars());

    }

    @Test
    public void shouldUpdatedUser() throws InvalidSyntaxException {
        givenIdentityService();
        givenExistingUser(new UserDTO("testuser", Collections.emptySet(), true, false, "testpassw"));

        whenUpdatingUser(new UserDTO("testuser", Collections.emptySet(), true, true, "testpassw2"));

        thenNoExceptionOccurred();
        thenUserIsUpdated();
    }

    @Test
    public void shouldValidateRightPassword() {
        givenIdentityService();

        whenValidatingPassword("password123");

        thenNoExceptionOccurred();
        thenPasswordValidationIs(true);
    }

    @Test
    public void shouldNotValidateWrongPassword() {
        givenIdentityService();

        whenValidatingPassword("short");

        thenPasswordValidationIs(false);
    }

    private void whenValidatingPassword(String password) {
        try {
            this.identityService.validateUserPassword(password);
            this.isPasswordValid = true;
        } catch (Exception e) {
            this.isPasswordValid = false;
            this.occurredException = e;
        }
    }

    private void givenIdentityService() {
        this.cryptoService = mock(CryptoService.class);
        this.userAdmin = mock(UserAdmin.class);
        this.configurationService = mock(ConfigurationService.class);

        ComponentConfiguration mockWebConsoleConfiguration = mock(ComponentConfiguration.class);
        when(mockWebConsoleConfiguration.getConfigurationProperties()).thenReturn(defaultValidatorProperties());

        try {
            when(this.configurationService.getComponentConfiguration(KURA_WEB_CONSOLE_SERVICE_PID))
                    .thenReturn(mockWebConsoleConfiguration);

            when(this.cryptoService.sha256Hash(anyString())).thenReturn("sha256hash");
        } catch (KuraException | NoSuchAlgorithmException | UnsupportedEncodingException e) {
            fail("fail to setup mocks");
        }

        this.identityService = new IdentityService(this.cryptoService, this.userAdmin, this.configurationService);

    }

    private void givenExistingUser(UserDTO user) {
        this.user = user;

        UserImpl userImpl = new UserImpl(this.user.getUserName(), this.user.getPassword(),
                this.user.isPasswordChangeNeeded().get());

        try {
            when(this.userAdmin.getRole(USER_ROLE_NAME_PREFIX + user.getUserName())).thenReturn(userImpl);
            when(this.userAdmin.getRoles(null)).thenReturn(new Role[] { userImpl });
        } catch (InvalidSyntaxException e) {
            fail("unable to setup mock user admin");
        }
    }

    private void givenExistingPermissions(Role... roles) throws InvalidSyntaxException {
        when(this.userAdmin.getRoles(null)).thenReturn(roles);
    }

    private void whenCreatingUser(UserDTO newUser) {
        try {
            this.newUser = newUser;
            when(this.userAdmin.createRole(USER_ROLE_NAME_PREFIX + newUser.getUserName(), Role.USER)).then(anser -> {

                User userImpl = new UserImpl(newUser.getUserName(), newUser.getPassword(),
                        newUser.isPasswordChangeNeeded().get());

                when(this.userAdmin.getRole(USER_ROLE_NAME_PREFIX + newUser.getUserName())).thenReturn(userImpl);

                return userImpl;
            });
            this.identityService.createUser(this.newUser);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenDeleting(String username) {
        try {
            this.identityService.deleteUser(username);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGettingDefinedPermissions() {
        this.definedPermissions = this.identityService.getDefinedPermissions();
    }

    private void whenGetUserConfig() {
        this.userConfig = this.identityService.getUserConfig();
    }

    private void whenGettingValidatorOptions() {
        try {
            this.validatorOptions = this.identityService.getValidatorOptions();
        } catch (Exception e) {
            this.occurredException = e;
        }

    }

    private void whenUpdatingUser(UserDTO user) {
        this.user = user;
        try {
            this.identityService.updateUser(user);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void thenUserIsCreated() throws KuraException {
        verify(this.userAdmin, times(1)).createRole(USER_ROLE_NAME_PREFIX + this.newUser.getUserName(), Role.USER);
    }

    private void thenUserIsDeleted() {
        verify(this.userAdmin, times(1)).removeRole(USER_ROLE_NAME_PREFIX + this.user.getUserName());
    }

    private void thenPermissionsAre(String... permissions) {
        assertTrue(new HashSet<>(this.definedPermissions).containsAll(Arrays.asList(permissions)));
    }

    private void thenUserIs(String username) {
        assertEquals(username, this.user.getUserName());
    }

    private void thenConfigContains(UserDTO userDTO) {
        assertTrue(this.userConfig.contains(userDTO));
    }

    private void thenUserIsUpdated() throws InvalidSyntaxException {
        verify(this.userAdmin, times(1)).getRole(USER_ROLE_NAME_PREFIX + this.user.getUserName());
        verify(this.userAdmin, times(1)).getRoles(null);
    }

    private void thenPasswordValidationIs(boolean expectedValue) {
        assertEquals(expectedValue, this.isPasswordValid);

    }

    private void thenNoExceptionOccurred() {
        String errorMessage = "Empty message";
        if (Objects.nonNull(this.occurredException)) {
            StringWriter sw = new StringWriter();
            this.occurredException.printStackTrace(new PrintWriter(sw));

            errorMessage = String.format("No exception expected, \"%s\" found. Caused by: %s",
                    this.occurredException.getClass().getName(), sw.toString());
        }

        assertNull(errorMessage, this.occurredException);
    }

    public static class PermissionRole implements Group {

        private final String name;

        public PermissionRole(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return PERMISSION_ROLE_NAME_PREFIX + this.name;

        }

        @Override
        public int getType() {
            return Role.GROUP;
        }

        @Override
        public Dictionary<String, Object> getProperties() {
            return null;
        }

        @Override
        public Dictionary<String, Object> getCredentials() {
            return null;
        }

        @Override
        public boolean hasCredential(String key, Object value) {
            return false;
        }

        @Override
        public boolean addMember(Role role) {
            return false;
        }

        @Override
        public boolean addRequiredMember(Role role) {
            return false;
        }

        @Override
        public boolean removeMember(Role role) {
            return false;
        }

        @Override
        public Role[] getMembers() {
            return null;
        }

        @Override
        public Role[] getRequiredMembers() {
            return null;
        }
    }

    public static class UserImpl implements User {

        private final String name;
        private final String password;
        private final boolean needPasswordChange;

        public UserImpl(String name, String password, boolean needPasswordChange) {
            this.name = name;
            this.password = password;
            this.needPasswordChange = needPasswordChange;
        }

        @Override
        public String getName() {
            return USER_ROLE_NAME_PREFIX + this.name;
        }

        @Override
        public int getType() {
            return Role.USER;
        }

        @Override
        public Dictionary<String, Object> getProperties() {
            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put("kura.need.password.change", this.needPasswordChange);
            return properties;
        }

        @Override
        public boolean hasCredential(String key, Object value) {
            return true;
        }

        @Override
        public Dictionary<String, Object> getCredentials() {
            Dictionary<String, Object> credentials = new Hashtable<>();
            credentials.put("kura.password", this.password);

            return credentials;
        }
    }

    private static Map<String, Object> defaultValidatorProperties() {
        Map<String, Object> properties = new HashMap<>();

        properties.put("new.password.min.length", 8);
        properties.put("new.password.require.digits", false);
        properties.put("new.password.require.special.characters", false);
        properties.put("new.password.require.both.cases", false);

        return properties;
    }

}
