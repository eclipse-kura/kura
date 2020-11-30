/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
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
import java.util.Arrays;
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
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtUserConfig;
import org.eclipse.kura.web.shared.model.GwtUserData;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class UserManager {

    private static final String PERMISSION_ROLE_NAME_PREFIX = "kura.permission.";
    private static final String USER_ROLE_NAME_PREFIX = "kura.user.";
    private static final String PASSWORD_PROPERTY = "kura.password";

    private final UserAdmin userAdmin;
    private final CryptoService cryptoService;

    public UserManager(final UserAdmin userAdmin, final CryptoService cryptoService) {
        this.userAdmin = userAdmin;
        this.cryptoService = cryptoService;
    }

    public void update(final ConsoleOptions consoleOptions)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, KuraException, InvalidSyntaxException {
        initializeUserAdmin(consoleOptions);
    }

    public void authenticateWithPassword(final String username, final String password) throws KuraException {
        final Role role = userAdmin.getRole(getUserRoleName(username));

        if (!(role instanceof User)) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
        }

        final User asUser = (User) role;

        try {
            String sha1Password = cryptoService.sha1Hash(password);

            if (!Objects.equals(sha1Password, asUser.getCredentials().get(PASSWORD_PROPERTY))) {
                throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
            }
        } catch (final KuraException e) {
            throw e;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
        }
    }

    public void requirePermissions(final String username, final String... permissions) throws KuraException {
        final String userRoleName = getUserRoleName(username);
        final Role role = userAdmin.getRole(userRoleName);

        if (!(role instanceof User)) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
        }

        final Group admin = getOrCreatePermission(KuraPermission.ADMIN);

        if (admin.getMembers() != null && Arrays.stream(admin.getMembers()).anyMatch(role::equals)) {
            return;
        }

        for (final String permission : permissions) {
            final String permissionRoleName = getPermissionRoleName(permission);
            final Role permissionRole = userAdmin.getRole(permissionRoleName);

            if (!(permissionRole instanceof Group)) {
                throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
            }

            final Group asGroup = (Group) permissionRole;
            final Role[] members = asGroup.getMembers();

            if (members != null && !Arrays.stream(members).anyMatch(r -> r.getName().equals(permissionRoleName))) {
                throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
            }
        }
    }

    public void createUser(final String userName) {
        getOrCreateUser(getUserRoleName(userName));
    }

    public void deleteUser(final String userName) {

        final Optional<User> user = getUser(userName);

        if (!user.isPresent()) {
            return;
        }

        foreachPermission((name, group) -> group.removeMember(user.get()));

        this.userAdmin.removeRole(user.get().getName());
    }

    @SuppressWarnings("unchecked")
    public void setUserPassword(final String userName, final String userPassword) throws KuraException {
        final User user = getUser(userName).orElseThrow(() -> new KuraException(KuraErrorCode.NOT_FOUND));

        try {
            user.getCredentials().put(PASSWORD_PROPERTY, cryptoService.sha1Hash(userPassword));
        } catch (final Exception e) {
            throw new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    public Set<String> getDefinedPermissions() {
        final Set<String> result = new HashSet<>();

        foreachPermission((permission, group) -> result.add(permission));

        return result;
    }

    public Set<GwtUserConfig> getUserConfig() {
        final Map<String, GwtUserConfig> result = new HashMap<>();

        foreachUser((name, user) -> {

            final GwtUserConfig userData = initUserConfig(user);

            result.put(user.getName(), userData);
        });

        fillPermissions(result);

        return new HashSet<>(result.values());
    }

    public Optional<GwtUserConfig> getUserConfig(final String userName) {

        final Optional<User> user = getUser(userName);

        if (!user.isPresent()) {
            return Optional.empty();
        }

        final GwtUserConfig userConfig = initUserConfig(user.get());

        fillPermissions(Collections.singletonMap(user.get().getName(), userConfig));

        return Optional.of(userConfig);

    }

    @SuppressWarnings("unchecked")
    public void setUserConfig(final Set<GwtUserConfig> userData) throws KuraException {
        foreachUser((name, user) -> {
            if (!userData.stream().anyMatch(data -> data.getUserName().equals(name))) {
                deleteUser(name);
            }
        });

        foreachPermission((permissionName, permissionGroup) -> {
            for (final GwtUserData data : userData) {

                final User user = getOrCreateUser(data.getUserName());

                if (data.getPermissions().contains(permissionName)) {
                    permissionGroup.addMember(user);
                } else {
                    permissionGroup.removeMember(user);
                }
            }
        });

        for (final GwtUserConfig config : userData) {
            final User user = getOrCreateUser(config.getUserName());

            @SuppressWarnings("rawtypes")
            final Dictionary credentials = user.getCredentials();

            if (config.isPasswordAuthEnabled()) {
                final Optional<String> password = config.getNewPassword();

                if (password.isPresent()) {
                    try {
                        credentials.put(PASSWORD_PROPERTY, cryptoService.sha1Hash(password.get()));
                    } catch (final Exception e) {
                        throw new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE, e);
                    }
                }
            } else {
                credentials.remove(PASSWORD_PROPERTY);
            }
        }
    }

    private Optional<User> getUser(final String name) {
        final String roleName = getUserRoleName(name);

        final Role role = userAdmin.getRole(roleName);

        if (!(role instanceof User)) {
            return Optional.empty();
        }

        if (!getBaseName(role).equals(name)) {
            return Optional.empty();
        }

        return Optional.of((User) role);
    }

    private GwtUserConfig initUserConfig(final User user) {

        final boolean isPasswordEnabled = user.getCredentials().get(PASSWORD_PROPERTY) instanceof String;

        return new GwtUserConfig(getBaseName(user), new HashSet<>(), isPasswordEnabled);
    }

    private void fillPermissions(final Map<String, ? extends GwtUserData> userData) {
        foreachPermission((permission, group) -> {

            forEach(group.getMembers(), member -> {
                final GwtUserData data = userData.get(member.getName());

                if (data != null) {
                    data.getPermissions().add(permission);
                }
            });
        });
    }

    private static String getUserRoleName(final String name) {
        return USER_ROLE_NAME_PREFIX + name;
    }

    private static String getPermissionRoleName(final String name) {
        return PERMISSION_ROLE_NAME_PREFIX + name;
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

    private Group getOrCreatePermission(final String name) {
        return getOrCreateRole(Group.class, getPermissionRoleName(name));
    }

    private User getOrCreateUser(final String name) {
        return getOrCreateRole(User.class, getUserRoleName(name));
    }

    private void initializeUserAdmin(final ConsoleOptions options)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, KuraException, InvalidSyntaxException {

        for (final String defaultPermission : KuraPermission.DEFAULT_PERMISSIONS) {
            getOrCreatePermission(defaultPermission);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Role> T getOrCreateRole(final Class<T> classz, final String name) {

        final int type;

        if (classz == Role.class) {
            type = Role.ROLE;
        } else if (classz == User.class) {
            type = Role.USER;
        } else if (classz == Group.class) {
            type = Role.GROUP;
        } else {
            throw new IllegalArgumentException("unknown role type");
        }

        final Role result = userAdmin.getRole(name);

        if (result != null && result.getType() == type) {
            return (T) result;
        } else if (result == null) {

            return (T) userAdmin.createRole(name, type);

        } else {
            throw new IllegalArgumentException("role exists but has different type");
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

    @SuppressWarnings("unchecked")
    private <R extends Role, E extends Exception> void foreachRole(final Class<R> classz,
            final FallibleConsumer<R, E> consumer) throws E {
        try {
            final Role[] existingRoles = userAdmin.getRoles(null);

            if (existingRoles != null) {
                for (final Role role : existingRoles) {
                    if (!classz.isInstance(role)) {
                        continue;
                    }

                    consumer.accept((R) role);
                }
            }
        } catch (InvalidSyntaxException e) {
            // no need
        }
    }

    private <E extends Exception> void foreachUser(final UserConsumer<E> consumer) throws E {
        foreachRole(User.class, user -> {

            final String name = user.getName();

            if (!name.startsWith(USER_ROLE_NAME_PREFIX)) {
                return;
            }

            consumer.accept(name.substring(USER_ROLE_NAME_PREFIX.length()), user);
        });
    }

    private <E extends Exception> void foreachPermission(final PermissionConsumer<E> consumer) throws E {
        foreachRole(Group.class, group -> {
            final String name = group.getName();

            if (!name.startsWith(PERMISSION_ROLE_NAME_PREFIX)) {
                return;
            }

            consumer.accept(name.substring(PERMISSION_ROLE_NAME_PREFIX.length()), group);
        });
    }

    private interface UserConsumer<E extends Exception> {

        public void accept(final String userName, final User user) throws E;
    }

    private interface PermissionConsumer<E extends Exception> {

        public void accept(final String permissionName, final Group group) throws E;
    }

    private interface FallibleConsumer<T, E extends Exception> {

        public void accept(final T item) throws E;
    }
}
