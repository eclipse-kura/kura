/*******************************************************************************
 * Copyright (c) 2020, 2023 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.web;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.util.useradmin.UserAdminHelper;
import org.eclipse.kura.util.useradmin.UserAdminHelper.AuthenticationException;
import org.eclipse.kura.util.useradmin.UserAdminHelper.FallibleConsumer;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtUserConfig;
import org.eclipse.kura.web.shared.model.GwtUserData;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

@SuppressWarnings("restriction")
public class UserManager {

    private static final String PERMISSION_ROLE_NAME_PREFIX = "kura.permission.";
    private static final String USER_ROLE_NAME_PREFIX = "kura.user.";
    private static final String PASSWORD_PROPERTY = "kura.password";
    private static final String KURA_NEED_PASSWORD_CHANGE = "kura.need.password.change";

    private final CryptoService cryptoService;

    private final UserAdminHelper userAdminHelper;

    public UserManager(final UserAdmin userAdmin, final CryptoService cryptoService) {
        this.cryptoService = cryptoService;
        this.userAdminHelper = new UserAdminHelper(userAdmin, cryptoService);
    }

    public void update(final ConsoleOptions consoleOptions)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, KuraException, InvalidSyntaxException {
        initializeUserAdmin(consoleOptions);
    }

    public void authenticateWithPassword(final String username, final String password) throws KuraException {
        try {
            this.userAdminHelper.verifyUsernamePassword(username, password);
        } catch (final AuthenticationException e) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION, e);
        }
    }

    public void requirePermissions(final String username, final String... permissions) throws KuraException {
        try {
            this.userAdminHelper.requirePermissions(username, permissions);
        } catch (final AuthenticationException e) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION, e);
        }
    }

    public boolean isPasswordChangeRequired(final String username) {
        return this.userAdminHelper.isPasswordChangeRequired(username);
    }

    public void createUser(final String userName) {
        this.userAdminHelper.createUser(userName);
    }

    public void deleteUser(final String userName) {

        this.userAdminHelper.deleteUser(userName);
    }

    public boolean setUserPassword(final String userName, final String userPassword) throws KuraException {
        try {
            this.userAdminHelper.changeUserPassword(userName, userPassword);
            return true;
        } catch (final AuthenticationException e) {
            if (e.getReason() == AuthenticationException.Reason.PASSWORD_CHANGE_WITH_SAME_PASSWORD) {
                return false;
            }

            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
        }
    }

    public Set<String> getDefinedPermissions() {
        return this.userAdminHelper.getDefinedPermissions();
    }

    public Set<GwtUserConfig> getUserConfig() {
        final Map<String, GwtUserConfig> result = new HashMap<>();

        this.userAdminHelper.foreachUser((name, user) -> {

            final GwtUserConfig userData = initUserConfig(user);

            result.put(user.getName(), userData);
        });

        fillPermissions(result);

        return new HashSet<>(result.values());
    }

    public Optional<GwtUserConfig> getUserConfig(final String userName) {

        final Optional<User> user = this.userAdminHelper.getUser(userName);

        if (!user.isPresent()) {
            return Optional.empty();
        }

        final GwtUserConfig userConfig = initUserConfig(user.get());

        fillPermissions(Collections.singletonMap(user.get().getName(), userConfig));

        return Optional.of(userConfig);

    }

    public Optional<Integer> getCredentialsHash(final String userName) {
        return this.userAdminHelper.getCredentialsHash(userName);
    }

    @SuppressWarnings("unchecked")
    public void setUserConfig(final Set<GwtUserConfig> userData) throws KuraException {
        this.userAdminHelper.foreachUser((name, user) -> {
            if (userData.stream().noneMatch(data -> data.getUserName().equals(name))) {
                deleteUser(name);
            }
        });

        this.userAdminHelper.foreachPermission((permissionName, permissionGroup) -> {
            for (final GwtUserData data : userData) {

                final User user = this.userAdminHelper.getOrCreateUser(data.getUserName());

                if (data.getPermissions().contains(permissionName)) {
                    permissionGroup.addMember(user);
                } else {
                    permissionGroup.removeMember(user);
                }
            }
        });

        for (final GwtUserConfig config : userData) {
            final User user = this.userAdminHelper.getOrCreateUser(config.getUserName());

            @SuppressWarnings("rawtypes")
            final Dictionary credentials = user.getCredentials();

            if (config.isPasswordAuthEnabled()) {
                final Optional<String> password = config.getNewPassword();

                if (password.isPresent()) {
                    try {
                        credentials.put(PASSWORD_PROPERTY, cryptoService.sha256Hash(password.get()));
                    } catch (final Exception e) {
                        throw new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE, e);
                    }
                }
            } else {
                credentials.remove(PASSWORD_PROPERTY);
            }

            @SuppressWarnings("rawtypes")
            final Dictionary properties = user.getProperties();

            if (config.isPasswordChangeNeeded()) {
                properties.put(KURA_NEED_PASSWORD_CHANGE, "true");
            } else {
                properties.remove(KURA_NEED_PASSWORD_CHANGE);
            }
        }
    }

    private GwtUserConfig initUserConfig(final User user) {

        final boolean isPasswordEnabled = user.getCredentials().get(PASSWORD_PROPERTY) instanceof String;
        final boolean isPasswordChangeRequired = Objects.equals("true",
                user.getProperties().get(KURA_NEED_PASSWORD_CHANGE));

        return new GwtUserConfig(getBaseName(user), new HashSet<>(), isPasswordEnabled, isPasswordChangeRequired);
    }

    private void fillPermissions(final Map<String, ? extends GwtUserData> userData) {
        this.userAdminHelper.foreachPermission((permission, group) -> {

            forEach(group.getMembers(), member -> {
                final GwtUserData data = userData.get(member.getName());

                if (data != null) {
                    data.getPermissions().add(permission);
                }
            });
        });
    }

    private static boolean isKuraUser(final Role role) {
        return role.getName().startsWith(USER_ROLE_NAME_PREFIX);
    }

    private static boolean isKuraPermission(final Role role) {
        return role.getName().startsWith(PERMISSION_ROLE_NAME_PREFIX);
    }

    private static String getBaseName(final Role role) {
        final String name = role.getName();

        if (isKuraUser(role)) {
            return name.substring(USER_ROLE_NAME_PREFIX.length());
        } else if (isKuraPermission(role)) {
            return name.substring(PERMISSION_ROLE_NAME_PREFIX.length());
        } else {
            throw new IllegalArgumentException("not a Kura role");
        }
    }

    private static <T, E extends Exception> void forEach(final T[] items, final FallibleConsumer<T, E> consumer)
            throws E {
        if (items != null) {
            for (final T item : items) {
                consumer.accept(item);
            }
        }
    }

    private void initializeUserAdmin(final ConsoleOptions options)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, KuraException, InvalidSyntaxException {

        for (final String defaultPermission : KuraPermission.DEFAULT_PERMISSIONS) {
            this.userAdminHelper.getOrCreatePermission(defaultPermission);
        }
    }

}
