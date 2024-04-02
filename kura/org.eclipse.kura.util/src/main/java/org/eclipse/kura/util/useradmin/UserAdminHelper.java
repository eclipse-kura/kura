/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.util.useradmin;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.crypto.CryptoService;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class UserAdminHelper {

    private static final String PERMISSION_ROLE_NAME_PREFIX = "kura.permission.";
    private static final String USER_ROLE_NAME_PREFIX = "kura.user.";
    private static final String PASSWORD_PROPERTY = "kura.password";
    private static final String KURA_NEED_PASSWORD_CHANGE = "kura.need.password.change";

    private final UserAdmin userAdmin;
    private final CryptoService cryptoService;

    public UserAdminHelper(final UserAdmin userAdmin, final CryptoService cryptoService) {
        this.userAdmin = userAdmin;
        this.cryptoService = cryptoService;
    }

    public void verifyUsernamePassword(final String username, final String password) throws AuthenticationException {
        final User user = getUser(username)
                .orElseThrow(() -> new AuthenticationException(AuthenticationException.Reason.USER_NOT_FOUND));

        try {
            String sha256Password = cryptoService.sha256Hash(password);

            if (!Objects.equals(sha256Password, user.getCredentials().get(PASSWORD_PROPERTY))) {
                throw new AuthenticationException(AuthenticationException.Reason.INCORRECT_PASSWORD);
            }
        } catch (final NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new AuthenticationException(AuthenticationException.Reason.ENCRYPTION_ERROR);
        }
    }

    public void requirePermissions(final String username, final String... permissions) throws AuthenticationException {
        final String userRoleName = getUserRoleName(username);
        final Role role = userAdmin.getRole(userRoleName);

        if (!(role instanceof User)) {
            throw new AuthenticationException(AuthenticationException.Reason.USER_NOT_FOUND);
        }

        final Group admin = getOrCreatePermission("kura.admin");

        if (admin.getMembers() != null && Arrays.stream(admin.getMembers()).anyMatch(role::equals)) {
            return;
        }

        for (final String permission : permissions) {
            final String permissionRoleName = getPermissionRoleName(permission);
            final Role permissionRole = userAdmin.getRole(permissionRoleName);

            if (!(permissionRole instanceof Group)) {
                throw new AuthenticationException(AuthenticationException.Reason.USER_NOT_IN_ROLE);
            }

            final Group asGroup = (Group) permissionRole;
            final Role[] members = asGroup.getMembers();

            if (members == null || Arrays.stream(members).noneMatch(r -> r.getName().equals(userRoleName))) {
                throw new AuthenticationException(AuthenticationException.Reason.USER_NOT_IN_ROLE);
            }
        }
    }

    public Set<String> getIdentityPermissions(final String name) {
        final String userRoleName = getUserRoleName(name);
        final Role role = userAdmin.getRole(userRoleName);

        if (!(role instanceof User)) {
            return Collections.emptySet();
        }

        final Set<String> result = new HashSet<>();

        foreachPermission((permission, group) -> {

            final Role[] members = group.getMembers();

            if (members != null && Arrays.stream(members).anyMatch(r -> r.getName().equals(userRoleName))) {

                result.add(getBaseName(group));
            }
        });

        return result;
    }

    public void changeUserPassword(final String username, final String userPassword) throws AuthenticationException {
        final User user = getUser(username)
                .orElseThrow(() -> new AuthenticationException(AuthenticationException.Reason.USER_NOT_FOUND));

        try {
            final String newHash = cryptoService.sha256Hash(userPassword);

            if (Objects.equals(user.getCredentials().get(PASSWORD_PROPERTY), newHash)) {
                throw new AuthenticationException(AuthenticationException.Reason.PASSWORD_CHANGE_WITH_SAME_PASSWORD);
            }

            user.getCredentials().put(PASSWORD_PROPERTY, newHash);
            user.getProperties().remove(KURA_NEED_PASSWORD_CHANGE);

        } catch (final NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new AuthenticationException(AuthenticationException.Reason.ENCRYPTION_ERROR);
        }
    }

    public boolean isPasswordChangeRequired(final String username) {
        final String userRoleName = getUserRoleName(username);
        final Role role = userAdmin.getRole(userRoleName);

        if (!(role instanceof User)) {
            return false;
        }

        final User asUser = (User) role;

        return "true".equals(asUser.getProperties().get(KURA_NEED_PASSWORD_CHANGE));
    }

    public void createUser(final String userName) {
        User createdUser = getOrCreateUser(userName);
        Objects.requireNonNull(createdUser, "Could not create user " + userName);
    }

    public void deleteUser(final String userName) {

        final Optional<User> user = getUser(userName);

        if (!user.isPresent()) {
            return;
        }

        foreachPermission((name, group) -> group.removeMember(user.get()));

        this.userAdmin.removeRole(user.get().getName());
    }

    public Optional<Integer> getCredentialsHash(final String userName) {
        final Optional<User> user = getUser(userName);

        if (!user.isPresent()) {
            return Optional.empty();
        }

        final Dictionary<?, ?> credentials = user.get().getCredentials();

        if (credentials == null) {
            return Optional.empty();
        }

        return Optional.of(credentials.hashCode());
    }

    public Set<String> getDefinedPermissions() {
        final Set<String> result = new HashSet<>();

        foreachPermission((permission, group) -> result.add(permission));

        return result;
    }

    public Optional<User> getUser(final String name) {
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

    public Optional<Group> getPermission(final String name) {
        final String roleName = getPermissionRoleName(name);

        final Role role = userAdmin.getRole(roleName);

        if (!(role instanceof Group)) {
            return Optional.empty();
        }

        return Optional.of((Group) role);
    }

    public Group getOrCreatePermission(final String name) {
        return getOrCreateRole(Group.class, getPermissionRoleName(name));
    }

    public void deletePremission(final String name) {
        final Role role = this.userAdmin.getRole(getPermissionRoleName(name));

        if (role instanceof Group) {
            this.userAdmin.removeRole(role.getName());
        }
    }

    public User getOrCreateUser(final String name) {
        return getOrCreateRole(User.class, getUserRoleName(name));
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

    @SuppressWarnings("unchecked")
    public <R extends Role, E extends Exception> void foreachRole(final Class<R> classz,
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

    public <E extends Exception> void foreachUser(final UserConsumer<E> consumer) throws E {
        foreachRole(User.class, user -> {

            final String name = user.getName();

            if (!name.startsWith(USER_ROLE_NAME_PREFIX)) {
                return;
            }

            consumer.accept(name.substring(USER_ROLE_NAME_PREFIX.length()), user);
        });
    }

    public <E extends Exception> void foreachPermission(final PermissionConsumer<E> consumer) throws E {
        foreachRole(Group.class, group -> {
            final String name = group.getName();

            if (!name.startsWith(PERMISSION_ROLE_NAME_PREFIX)) {
                return;
            }

            consumer.accept(name.substring(PERMISSION_ROLE_NAME_PREFIX.length()), group);
        });
    }

    public static class AuthenticationException extends Exception {

        private static final long serialVersionUID = -8534499595655286448L;

        public enum Reason {
            USER_NOT_FOUND,
            INCORRECT_PASSWORD,
            USER_NOT_IN_ROLE,
            PASSWORD_CHANGE_WITH_SAME_PASSWORD,
            ENCRYPTION_ERROR;
        }

        private final Reason reason;

        public AuthenticationException(final Reason reason) {
            this.reason = reason;
        }

        public Reason getReason() {
            return reason;
        }
    }

    public interface UserConsumer<E extends Exception> {

        public void accept(final String userName, final User user) throws E;
    }

    public interface PermissionConsumer<E extends Exception> {

        public void accept(final String permissionName, final Group group) throws E;
    }

    public interface FallibleConsumer<T, E extends Exception> {

        public void accept(final T item) throws E;
    }
}
